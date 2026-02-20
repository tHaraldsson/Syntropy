package com.haraldsson.syntropy.world;

import com.haraldsson.syntropy.entities.Colonist;
import com.haraldsson.syntropy.entities.FoodGrower;
import com.haraldsson.syntropy.entities.Item;
import com.haraldsson.syntropy.entities.ItemType;
import com.haraldsson.syntropy.entities.Miner;

import java.util.Random;

public final class WorldGenerator {
    private WorldGenerator() {
    }

    public static World generate(int width, int height) {
        long seed = new Random().nextLong();
        double freq = 0.08;

        Tile[][] tiles = new Tile[width][height];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                double n = SimplexNoise.noise2(seed, x * freq, y * freq);
                TerrainType terrain;
                if (n < -0.35) {
                    terrain = TerrainType.WATER;
                } else if (n < -0.1) {
                    terrain = TerrainType.SAND;
                } else if (n < 0.3) {
                    terrain = TerrainType.GRASS;
                } else if (n < 0.5) {
                    terrain = TerrainType.DIRT;
                } else {
                    terrain = TerrainType.STONE;
                }
                tiles[x][y] = new Tile(x, y, terrain);
            }
        }

        World world = new World(width, height, tiles);

        // Miner 1
        int[] m1 = findValidTile(tiles, width, height, 4, 4);
        Miner miner1 = new Miner(m1[0], m1[1]);
        world.addMiner(miner1);
        tiles[m1[0]][m1[1]].setBuilding(miner1);

        // Miner 2
        int[] m2 = findValidTile(tiles, width, height, width - 5, 5);
        Miner miner2 = new Miner(m2[0], m2[1]);
        world.addMiner(miner2);
        tiles[m2[0]][m2[1]].setBuilding(miner2);

        // Food Grower
        int[] fg = findValidTile(tiles, width, height, width / 2, height / 2);
        FoodGrower foodGrower = new FoodGrower(fg[0], fg[1]);
        world.addFoodGrower(foodGrower);
        tiles[fg[0]][fg[1]].setBuilding(foodGrower);

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
        world.addColonist(new Colonist("Ari", 28, c1[0] + 0.5f, c1[1] + 0.5f));

        int[] c2 = findValidTile(tiles, width, height, width / 2 - 1, height / 2 + 2);
        world.addColonist(new Colonist("Bela", 34, c2[0] + 0.5f, c2[1] + 0.5f));

        return world;
    }

    /** Find a walkable (non-WATER) tile nearest to the hint position. */
    private static int[] findValidTile(Tile[][] tiles, int w, int h, int hintX, int hintY) {
        hintX = Math.max(0, Math.min(w - 1, hintX));
        hintY = Math.max(0, Math.min(h - 1, hintY));
        if (tiles[hintX][hintY].getTerrainType() != TerrainType.WATER
                && tiles[hintX][hintY].getBuilding() == null) {
            return new int[]{hintX, hintY};
        }
        for (int r = 1; r < Math.max(w, h); r++) {
            for (int dy = -r; dy <= r; dy++) {
                for (int dx = -r; dx <= r; dx++) {
                    int nx = hintX + dx;
                    int ny = hintY + dy;
                    if (nx >= 0 && ny >= 0 && nx < w && ny < h
                            && tiles[nx][ny].getTerrainType() != TerrainType.WATER
                            && tiles[nx][ny].getBuilding() == null) {
                        return new int[]{nx, ny};
                    }
                }
            }
        }
        return new int[]{hintX, hintY};
    }
}
