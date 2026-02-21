package com.haraldsson.syntropy.world;

import com.haraldsson.syntropy.ecs.ECSWorld;
import com.haraldsson.syntropy.ecs.Entity;
import com.haraldsson.syntropy.ecs.components.*;
import com.haraldsson.syntropy.entities.ColonistRole;
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
        int[] m1 = findValidTile(tiles, width, height, 4, 4, world);
        createBuilding(ecsWorld, world, tiles, m1[0], m1[1], "MINER", 5f, 5, ItemType.STONE);

        // Miner 2
        int[] m2 = findValidTile(tiles, width, height, width - 5, 5, world);
        createBuilding(ecsWorld, world, tiles, m2[0], m2[1], "MINER", 5f, 5, ItemType.STONE);

        // Food Grower
        int[] fg = findValidTile(tiles, width, height, width / 2, height / 2, world);
        createBuilding(ecsWorld, world, tiles, fg[0], fg[1], "FOOD_GROWER", 6f, 5, ItemType.FOOD);

        // Woodcutter
        int[] wc = findValidTile(tiles, width, height, width / 2 - 4, height / 2, world);
        createBuilding(ecsWorld, world, tiles, wc[0], wc[1], "WOODCUTTER", 8f, 5, ItemType.WOOD);

        // Stockpile
        int[] sp = findValidTile(tiles, width, height, width - 4, height - 4, world);
        Tile stockpileTile = tiles[sp[0]][sp[1]];
        stockpileTile.setStockpile(true);
        for (int i = 0; i < 5; i++) stockpileTile.addItem(new Item(ItemType.FOOD));
        for (int i = 0; i < 5; i++) stockpileTile.addItem(new Item(ItemType.STONE));
        for (int i = 0; i < 5; i++) stockpileTile.addItem(new Item(ItemType.WOOD));
        world.setStockpileTile(stockpileTile);

        // Leader (player-controlled)
        int[] c1 = findValidTile(tiles, width, height, width / 2, height / 2 + 2, world);
        createLeader(ecsWorld, "Commander Kael", 30, c1[0] + 0.5f, c1[1] + 0.5f);

        // NPC Colonists
        int[] c2 = findValidTile(tiles, width, height, width / 2 - 1, height / 2 + 2, world);
        createColonist(ecsWorld, "Ari", 28, c2[0] + 0.5f, c2[1] + 0.5f, ColonistRole.HAULER);

        int[] c3 = findValidTile(tiles, width, height, width / 2 + 1, height / 2 + 2, world);
        createColonist(ecsWorld, "Bela", 34, c3[0] + 0.5f, c3[1] + 0.5f, ColonistRole.FARMER);

        int[] c4 = findValidTile(tiles, width, height, width / 2, height / 2 + 3, world);
        createColonist(ecsWorld, "Dax", 22, c4[0] + 0.5f, c4[1] + 0.5f, ColonistRole.MINER);

        assignBedsToColonists(ecsWorld);

        return new GenerationResult(world, ecsWorld);
    }

    private static void assignBedsToColonists(ECSWorld ecsWorld) {
        java.util.List<Entity> unownedBeds = new java.util.ArrayList<>();
        for (Entity bedEntity : ecsWorld.getEntitiesWith(BedComponent.class)) {
            BedComponent bed = bedEntity.get(BedComponent.class);
            if (bed.ownerEntityId == -1) {
                unownedBeds.add(bedEntity);
            }
        }
        if (unownedBeds.isEmpty()) return;

        int bedIndex = 0;
        for (Entity colonist : ecsWorld.getEntitiesWith(AIComponent.class)) {
            if (bedIndex >= unownedBeds.size()) break;
            AIComponent ai = colonist.get(AIComponent.class);
            if (!ai.aiDisabled) {
                Entity bedEntity = unownedBeds.get(bedIndex++);
                bedEntity.get(BedComponent.class).ownerEntityId = colonist.getId();
            }
        }
    }

    private static Entity createBuilding(ECSWorld ecsWorld, World world, Tile[][] tiles,
                                          int x, int y, String type, float interval, int max, ItemType produced) {
        Entity entity = ecsWorld.createEntity();
        entity.add(new PositionComponent(x, y));
        entity.add(new BuildingComponent(type, interval, max, produced));
        tiles[x][y].setBuildingEntity(entity);
        return entity;
    }

    private static Entity createLeader(ECSWorld ecsWorld, String name, int age, float x, float y) {
        Random rng = new Random();
        Entity entity = ecsWorld.createEntity();
        entity.add(new PositionComponent(x, y));
        entity.add(new IdentityComponent(name, age));
        entity.add(new NeedsComponent());
        entity.add(new HealthComponent());
        entity.add(new AIComponent());
        entity.add(new InventoryComponent());
        entity.add(new SkillsComponent());
        entity.add(new LeaderComponent());
        entity.add(new AgingComponent(age, 65 + rng.nextInt(20)));
        entity.add(new RoleComponent(ColonistRole.IDLE));
        entity.add(new MoodComponent());
        entity.add(new SleepQualityComponent());
        entity.add(new WorkSettingsComponent());
        entity.add(new SleepQualityComponent());
        // Leader AI is disabled — player controls directly
        entity.get(AIComponent.class).aiDisabled = true;
        return entity;
    }

    private static Entity createColonist(ECSWorld ecsWorld, String name, int age, float x, float y, ColonistRole role) {
        Random rng = new Random();
        Entity entity = ecsWorld.createEntity();
        entity.add(new PositionComponent(x, y));
        entity.add(new IdentityComponent(name, age));
        entity.add(new NeedsComponent());
        entity.add(new HealthComponent());
        entity.add(new AIComponent());
        entity.add(new InventoryComponent());
        entity.add(new SkillsComponent());
        entity.add(new AgingComponent(age, 60 + rng.nextInt(25)));
        entity.add(new RoleComponent(role));
        entity.add(new MoodComponent());
        entity.add(new SleepQualityComponent());
        WorkSettingsComponent ws = new WorkSettingsComponent();
        ws.setPriority(role, 3); // default priority for assigned role
        ws.setPriority(ColonistRole.HAULER, 2); // everyone hauls at low priority
        entity.add(ws);
        entity.add(new SleepQualityComponent());
        return entity;
    }

    private static int[] findValidTile(Tile[][] tiles, int w, int h, int hintX, int hintY, World world) {
        // FIX BUG2a: only spawn buildings/colonists on passable tiles (2026-02-20)
        hintX = Math.max(0, Math.min(w - 1, hintX));
        hintY = Math.max(0, Math.min(h - 1, hintY));
        if (world.isPassable(hintX, hintY)
                && tiles[hintX][hintY].getBuildingEntity() == null) {
            return new int[]{hintX, hintY};
        }
        for (int r = 1; r < Math.max(w, h); r++) {
            for (int dy = -r; dy <= r; dy++) {
                for (int dx = -r; dx <= r; dx++) {
                    int nx = hintX + dx;
                    int ny = hintY + dy;
                    if (nx >= 0 && ny >= 0 && nx < w && ny < h
                            && world.isPassable(nx, ny)
                            && tiles[nx][ny].getBuildingEntity() == null) {
                        return new int[]{nx, ny};
                    }
                }
            }
        }
        return new int[]{hintX, hintY};
    }
}
