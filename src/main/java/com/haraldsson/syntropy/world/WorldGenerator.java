package com.haraldsson.syntropy.world;

import com.haraldsson.syntropy.entities.Colonist;
import com.haraldsson.syntropy.entities.Item;
import com.haraldsson.syntropy.entities.ItemType;
import com.haraldsson.syntropy.entities.Miner;

public final class WorldGenerator {
    private WorldGenerator() {
    }

    public static World generate(int width, int height) {
        Tile[][] tiles = new Tile[width][height];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                tiles[x][y] = new Tile(x, y, TerrainType.GRASS);
            }
        }

        World world = new World(width, height, tiles);

        // Miner 1
        int minerX = 2;
        int minerY = 2;
        Miner miner1 = new Miner(minerX, minerY);
        world.addMiner(miner1);
        tiles[minerX][minerY].setBuilding(miner1);

        // Miner 2
        int miner2X = 8;
        int miner2Y = 3;
        Miner miner2 = new Miner(miner2X, miner2Y);
        world.addMiner(miner2);
        tiles[miner2X][miner2Y].setBuilding(miner2);

        // Stockpile
        int stockpileX = 7;
        int stockpileY = 7;
        Tile stockpileTile = tiles[stockpileX][stockpileY];
        stockpileTile.setStockpile(true);
        stockpileTile.addItem(new Item(ItemType.FOOD));
        stockpileTile.addItem(new Item(ItemType.FOOD));
        stockpileTile.addItem(new Item(ItemType.FOOD));
        world.setStockpileTile(stockpileTile);

        // Colonists
        Colonist ari = new Colonist("Ari", 28, 5.5f, 5.5f);
        world.addColonist(ari);

        Colonist Bela = new Colonist("Bela", 34, 4.5f, 6.5f);
        world.addColonist(Bela);

        return world;
    }
}
