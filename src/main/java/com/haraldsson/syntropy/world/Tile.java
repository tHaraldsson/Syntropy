package com.haraldsson.syntropy.world;

import com.haraldsson.syntropy.entities.Building;
import com.haraldsson.syntropy.entities.Item;
import com.haraldsson.syntropy.entities.ItemType;

import java.util.ArrayList;
import java.util.List;

public class Tile {
    private final int x;
    private final int y;
    private TerrainType terrainType;
    private Building building;
    private boolean stockpile;
    private final List<Item> groundItems = new ArrayList<>();

    public Tile(int x, int y, TerrainType terrainType) {
        this.x = x;
        this.y = y;
        this.terrainType = terrainType;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public TerrainType getTerrainType() {
        return terrainType;
    }

    public void setTerrainType(TerrainType terrainType) {
        this.terrainType = terrainType;
    }

    public Building getBuilding() {
        return building;
    }

    public void setBuilding(Building building) {
        this.building = building;
    }

    public boolean isStockpile() {
        return stockpile;
    }

    public void setStockpile(boolean stockpile) {
        this.stockpile = stockpile;
    }

    public List<Item> getGroundItems() {
        return groundItems;
    }

    public boolean hasItem(ItemType type) {
        for (Item item : groundItems) {
            if (item.getType() == type) {
                return true;
            }
        }
        return false;
    }

    public int countItems(ItemType type) {
        int count = 0;
        for (Item item : groundItems) {
            if (item.getType() == type) {
                count++;
            }
        }
        return count;
    }

    public void addItem(Item item) {
        groundItems.add(item);
    }

    public Item takeFirstItem(ItemType type) {
        for (int i = 0; i < groundItems.size(); i++) {
            Item item = groundItems.get(i);
            if (item.getType() == type) {
                groundItems.remove(i);
                return item;
            }
        }
        return null;
    }
}
