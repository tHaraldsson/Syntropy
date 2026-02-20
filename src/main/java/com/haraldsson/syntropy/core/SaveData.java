package com.haraldsson.syntropy.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Flat serializable data structure for save/load.
 */
public class SaveData {
    public int worldWidth;
    public int worldHeight;
    public int stockpileX = -1;
    public int stockpileY = -1;
    public List<TileData> tiles = new ArrayList<>();
    public List<EntityData> entities = new ArrayList<>();

    public static class TileData {
        public int x, y;
        public String terrain;
        public boolean stockpile;
        public List<String> groundItems = new ArrayList<>();
        public int buildingEntityId = -1;
    }

    public static class EntityData {
        public int id;

        // Position
        public boolean hasPosition;
        public float posX, posY;

        // Identity
        public boolean hasIdentity;
        public String name;
        public int age;

        // Needs
        public boolean hasNeeds;
        public float hunger, energy, needsHealth;
        public float mood;

        // Health
        public boolean hasHealth;
        public boolean dead;
        public boolean deathEventFired;

        // AI
        public boolean hasAI;
        public String taskType;
        public int targetX, targetY;
        public boolean aiDisabled;
        public float wanderTimer;
        public float wanderCooldown;

        // Inventory
        public boolean hasInventory;
        public String carriedItem;

        // Skills
        public boolean hasSkills;
        public Map<String, Integer> skillsMap;

        // Building
        public boolean hasBuilding;
        public String buildingType;
        public float buildingTimer;
        public float productionInterval;
        public int maxOutput;
        public String producedItemType;
        public List<String> outputBuffer;
        public float pollutionRate;

        // Leader
        public boolean hasLeader;
        public float charisma, engineering, science, combat;

        // Aging
        public boolean hasAging;
        public float ageYears, maxAge, yearAccumulator;

        // Role
        public boolean hasRole;
        public String roleName;
    }
}
