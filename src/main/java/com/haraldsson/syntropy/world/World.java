package com.haraldsson.syntropy.world;

import com.haraldsson.syntropy.entities.Colonist;
import com.haraldsson.syntropy.entities.FoodGrower;
import com.haraldsson.syntropy.entities.ItemType;
import com.haraldsson.syntropy.entities.Miner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class World {
    private final int width;
    private final int height;
    private final Tile[][] tiles;
    private final List<Colonist> colonists = new ArrayList<>();
    private final List<Miner> miners = new ArrayList<>();
    private final List<FoodGrower> foodGrowers = new ArrayList<>();
    private Tile stockpileTile;

    public World(int width, int height, Tile[][] tiles) {
        this.width = width;
        this.height = height;
        this.tiles = tiles;
    }

    public void update(float delta) {
        for (Miner miner : miners) {
            miner.update(delta, this);
        }
        for (FoodGrower grower : foodGrowers) {
            grower.update(delta, this);
        }
        for (Colonist colonist : colonists) {
            colonist.updateNeeds(delta);
            clampColonist(colonist);
        }
    }

    private void clampColonist(Colonist colonist) {
        float clampedX = Math.max(0.1f, Math.min(width - 0.1f, colonist.getX()));
        float clampedY = Math.max(0.1f, Math.min(height - 0.1f, colonist.getY()));
        colonist.setPosition(clampedX, clampedY);
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public Tile getTile(int x, int y) {
        if (x < 0 || y < 0 || x >= width || y >= height) {
            return null;
        }
        return tiles[x][y];
    }

    public Tile[][] getTiles() {
        return tiles;
    }

    public List<Colonist> getColonists() {
        return Collections.unmodifiableList(colonists);
    }

    public void addColonist(Colonist colonist) {
        colonists.add(colonist);
    }

    public List<Miner> getMiners() {
        return Collections.unmodifiableList(miners);
    }

    public void addMiner(Miner miner) {
        miners.add(miner);
    }

    public Miner getMiner() {
        return miners.isEmpty() ? null : miners.get(0);
    }

    public List<FoodGrower> getFoodGrowers() {
        return Collections.unmodifiableList(foodGrowers);
    }

    public void addFoodGrower(FoodGrower grower) {
        foodGrowers.add(grower);
    }

    public Tile getStockpileTile() {
        return stockpileTile;
    }

    public void setStockpileTile(Tile stockpileTile) {
        this.stockpileTile = stockpileTile;
    }

    public Tile findNearestFoodTile(Colonist colonist) {
        Tile best = null;
        float bestDist = Float.MAX_VALUE;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Tile tile = tiles[x][y];
                if (tile.hasItem(ItemType.FOOD)) {
                    float dx = colonist.getX() - (x + 0.5f);
                    float dy = colonist.getY() - (y + 0.5f);
                    float dist = dx * dx + dy * dy;
                    if (dist < bestDist) {
                        bestDist = dist;
                        best = tile;
                    }
                }
            }
        }
        return best;
    }
}
