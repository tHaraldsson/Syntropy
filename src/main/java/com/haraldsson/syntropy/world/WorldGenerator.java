package com.haraldsson.syntropy.world;

import com.haraldsson.syntropy.ecs.ECSWorld;
import com.haraldsson.syntropy.ecs.Entity;
import com.haraldsson.syntropy.ecs.components.*;
import com.haraldsson.syntropy.entities.Item;
import com.haraldsson.syntropy.entities.ItemType;

import java.util.Random;

public final class WorldGenerator {
    private WorldGenerator() {
    }

    /**
     * Result holder for world generation — returns both the spatial grid and ECS world.
     */
    public static class GenerationResult {
        public final World world;
        public final ECSWorld ecsWorld;
        public GenerationResult(World world, ECSWorld ecsWorld) {
            this.world = world;
            this.ecsWorld = ecsWorld;
        }
    }

    public static GenerationResult generate(int width, int height) {
        long seed = new Random().nextLong();
        double freq = 0.08;

        Tile[][] tiles = new Tile[width][height];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                double n = SimplexNoise.noise2(seed, x * freq, y * freq);
                TerrainType terrain;
                if (n < -0.35) terrain = TerrainType.WATER;
                else if (n < -0.1) terrain = TerrainType.SAND;
                else if (n < 0.3) terrain = TerrainType.GRASS;
                else if (n < 0.5) terrain = TerrainType.DIRT;
                else terrain = TerrainType.STONE;
                tiles[x][y] = new Tile(x, y, terrain);
            }
        }

        World world = new World(width, height, tiles);
        ECSWorld ecsWorld = new ECSWorld();

        // Miner 1
        int[] m1 = findValidTile(tiles, width, height, 4, 4);
        createBuilding(ecsWorld, world, tiles, m1[0], m1[1], "MINER", 5f, 5, ItemType.STONE);

        // Miner 2
        int[] m2 = findValidTile(tiles, width, height, width - 5, 5);
        createBuilding(ecsWorld, world, tiles, m2[0], m2[1], "MINER", 5f, 5, ItemType.STONE);

        // Food Grower
        int[] fg = findValidTile(tiles, width, height, width / 2, height / 2);
        createBuilding(ecsWorld, world, tiles, fg[0], fg[1], "FOOD_GROWER", 10f, 5, ItemType.FOOD);

        // Stockpile
        int[] sp = findValidTile(tiles, width, height, width - 4, height - 4);
        Tile stockpileTile = tiles[sp[0]][sp[1]];
        stockpileTile.setStockpile(true);
        stockpileTile.addItem(new Item(ItemType.FOOD));
        stockpileTile.addItem(new Item(ItemType.FOOD));
        stockpileTile.addItem(new Item(ItemType.FOOD));
        world.setStockpileTile(stockpileTile);

        // Colonists
        int[] c1 = findValidTile(tiles, width, height, width / 2, height / 2 + 2);
        createColonist(ecsWorld, "Ari", 28, c1[0] + 0.5f, c1[1] + 0.5f);

        int[] c2 = findValidTile(tiles, width, height, width / 2 - 1, height / 2 + 2);
        createColonist(ecsWorld, "Bela", 34, c2[0] + 0.5f, c2[1] + 0.5f);

        return new GenerationResult(world, ecsWorld);
    }

    private static Entity createBuilding(ECSWorld ecsWorld, World world, Tile[][] tiles,
                                          int x, int y, String type, float interval, int max, ItemType produced) {
        Entity entity = ecsWorld.createEntity();
        entity.add(new PositionComponent(x, y));
        entity.add(new BuildingComponent(type, interval, max, produced));
        tiles[x][y].setBuildingEntity(entity);
        return entity;
    }

    private static Entity createColonist(ECSWorld ecsWorld, String name, int age, float x, float y) {
        Entity entity = ecsWorld.createEntity();
        entity.add(new PositionComponent(x, y));
        entity.add(new IdentityComponent(name, age));
        entity.add(new NeedsComponent());
        entity.add(new HealthComponent());
        entity.add(new AIComponent());
        entity.add(new InventoryComponent());
        entity.add(new SkillsComponent());
        return entity;
    }

    private static int[] findValidTile(Tile[][] tiles, int w, int h, int hintX, int hintY) {
        hintX = Math.max(0, Math.min(w - 1, hintX));
        hintY = Math.max(0, Math.min(h - 1, hintY));
        if (tiles[hintX][hintY].getTerrainType() != TerrainType.WATER
                && tiles[hintX][hintY].getBuildingEntity() == null) {
            return new int[]{hintX, hintY};
        }
        for (int r = 1; r < Math.max(w, h); r++) {
            for (int dy = -r; dy <= r; dy++) {
                for (int dx = -r; dx <= r; dx++) {
                    int nx = hintX + dx;
                    int ny = hintY + dy;
                    if (nx >= 0 && ny >= 0 && nx < w && ny < h
                            && tiles[nx][ny].getTerrainType() != TerrainType.WATER
                            && tiles[nx][ny].getBuildingEntity() == null) {
                        return new int[]{nx, ny};
                    }
                }
            }
        }
        return new int[]{hintX, hintY};
    }
}
