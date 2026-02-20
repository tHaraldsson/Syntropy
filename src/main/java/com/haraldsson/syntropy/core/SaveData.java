package com.haraldsson.syntropy.core;

import java.util.ArrayList;
import java.util.List;

/**
 * Plain data-holder for JSON serialization. No circular references.
 */
public class SaveData {
    public int worldWidth;
    public int worldHeight;
    public List<TileData> tiles = new ArrayList<>();
    public List<ColonistData> colonists = new ArrayList<>();
    public List<MinerData> miners = new ArrayList<>();
    public List<FoodGrowerData> foodGrowers = new ArrayList<>();
    public int stockpileX = -1;
    public int stockpileY = -1;

    public static class TileData {
        public int x, y;
        public String terrain;
        public boolean stockpile;
        public List<String> groundItems = new ArrayList<>();
    }

    public static class ColonistData {
        public String name;
        public int age;
        public float x, y;
        public float hunger, energy, mood;
        public boolean dead;
        public boolean aiDisabled;
        public String carriedItemType; // null if none
        public String taskType;
        public int targetX, targetY;
    }

    public static class MinerData {
        public int x, y;
        public float timer;
        public List<String> outputBuffer = new ArrayList<>();
    }

    public static class FoodGrowerData {
        public int x, y;
        public float timer;
        public List<String> outputBuffer = new ArrayList<>();
    }
}


