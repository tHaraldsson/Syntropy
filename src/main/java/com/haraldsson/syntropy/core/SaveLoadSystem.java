package com.haraldsson.syntropy.core;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonWriter;
import com.haraldsson.syntropy.ecs.Component;
import com.haraldsson.syntropy.ecs.ECSWorld;
import com.haraldsson.syntropy.ecs.Entity;
import com.haraldsson.syntropy.ecs.components.*;
import com.haraldsson.syntropy.entities.Item;
import com.haraldsson.syntropy.entities.ItemType;
import com.haraldsson.syntropy.world.TerrainType;
import com.haraldsson.syntropy.world.Tile;
import com.haraldsson.syntropy.world.World;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Saves and loads full game state (World + ECSWorld) as JSON.
 */
public final class SaveLoadSystem {
    private SaveLoadSystem() {}

    // ── Save ─────────────────────────────────────────────────────────

    public static void save(World world, ECSWorld ecsWorld, String fileName) {
        SaveData data = new SaveData();
        data.worldWidth = world.getWidth();
        data.worldHeight = world.getHeight();

        // Tiles
        for (int y = 0; y < world.getHeight(); y++) {
            for (int x = 0; x < world.getWidth(); x++) {
                Tile tile = world.getTile(x, y);
                SaveData.TileData td = new SaveData.TileData();
                td.x = x;
                td.y = y;
                td.terrain = tile.getTerrainType().name();
                td.stockpile = tile.isStockpile();
                for (Item item : tile.getGroundItems()) {
                    td.groundItems.add(item.getType().name());
                }
                // Track building entity id
                if (tile.getBuildingEntity() != null) {
                    td.buildingEntityId = tile.getBuildingEntity().getId();
                }
                data.tiles.add(td);
            }
        }

        // Stockpile
        Tile sp = world.getStockpileTile();
        if (sp != null) {
            data.stockpileX = sp.getX();
            data.stockpileY = sp.getY();
        }

        // Entities
        for (Entity entity : ecsWorld.getAll()) {
            SaveData.EntityData ed = new SaveData.EntityData();
            ed.id = entity.getId();

            // Serialize each component by type
            for (Map.Entry<Class<? extends Component>, Component> entry : entity.getComponents().entrySet()) {
                Component comp = entry.getValue();

                if (comp instanceof PositionComponent) {
                    PositionComponent p = (PositionComponent) comp;
                    ed.posX = p.x; ed.posY = p.y; ed.hasPosition = true;
                }
                if (comp instanceof IdentityComponent) {
                    IdentityComponent ic = (IdentityComponent) comp;
                    ed.name = ic.name; ed.age = ic.age; ed.hasIdentity = true;
                }
                if (comp instanceof NeedsComponent) {
                    NeedsComponent n = (NeedsComponent) comp;
                    ed.hunger = n.hunger; ed.energy = n.energy;
                    ed.needsHealth = n.health;
                    ed.hasNeeds = true;
                }
                if (comp instanceof HealthComponent) {
                    HealthComponent h = (HealthComponent) comp;
                    ed.dead = h.dead; ed.hasHealth = true;
                }
                if (comp instanceof AIComponent) {
                    AIComponent ai = (AIComponent) comp;
                    ed.taskType = ai.taskType.name();
                    ed.targetX = ai.targetX; ed.targetY = ai.targetY;
                    ed.aiDisabled = ai.aiDisabled;
                    ed.hasAI = true;
                }
                if (comp instanceof InventoryComponent) {
                    InventoryComponent inv = (InventoryComponent) comp;
                    ed.carriedItem = inv.carriedItem != null ? inv.carriedItem.getType().name() : null;
                    ed.hasInventory = true;
                }
                if (comp instanceof SkillsComponent) {
                    ed.hasSkills = true;
                }
                if (comp instanceof BuildingComponent) {
                    BuildingComponent bc = (BuildingComponent) comp;
                    ed.buildingType = bc.buildingType;
                    ed.buildingTimer = bc.timer;
                    ed.productionInterval = bc.productionInterval;
                    ed.maxOutput = bc.maxOutput;
                    ed.producedItemType = bc.producedItemType.name();
                    ed.outputBuffer = new ArrayList<>();
                    for (Item item : bc.outputBuffer) {
                        ed.outputBuffer.add(item.getType().name());
                    }
                    ed.hasBuilding = true;
                }
                if (comp instanceof LeaderComponent) {
                    LeaderComponent lc = (LeaderComponent) comp;
                    ed.charisma = lc.charisma;
                    ed.engineering = lc.engineering;
                    ed.science = lc.science;
                    ed.combat = lc.combat;
                    ed.hasLeader = true;
                }
                if (comp instanceof AgingComponent) {
                    AgingComponent ac = (AgingComponent) comp;
                    ed.ageYears = ac.ageYears;
                    ed.maxAge = ac.maxAge;
                    ed.yearAccumulator = ac.yearAccumulator;
                    ed.hasAging = true;
                }
                if (comp instanceof RoleComponent) {
                    RoleComponent rc = (RoleComponent) comp;
                    ed.roleName = rc.role.name();
                    ed.hasRole = true;
                }
            }
            data.entities.add(ed);
        }

        Json json = new Json();
        json.setOutputType(JsonWriter.OutputType.json);
        FileHandle file = Gdx.files.local(fileName);
        file.writeString(json.prettyPrint(data), false);
    }

    // ── Load ─────────────────────────────────────────────────────────

    public static LoadResult load(String fileName) {
        FileHandle file = Gdx.files.local(fileName);
        if (!file.exists()) throw new RuntimeException("Save not found: " + fileName);

        Json json = new Json();
        SaveData data = json.fromJson(SaveData.class, file.readString());

        // Rebuild tiles
        Tile[][] tiles = new Tile[data.worldWidth][data.worldHeight];
        for (SaveData.TileData td : data.tiles) {
            Tile tile = new Tile(td.x, td.y, TerrainType.valueOf(td.terrain));
            tile.setStockpile(td.stockpile);
            for (String itemName : td.groundItems) {
                tile.addItem(new Item(ItemType.valueOf(itemName)));
            }
            tiles[td.x][td.y] = tile;
        }

        World world = new World(data.worldWidth, data.worldHeight, tiles);
        if (data.stockpileX >= 0 && data.stockpileY >= 0) {
            world.setStockpileTile(tiles[data.stockpileX][data.stockpileY]);
        }

        // Rebuild entities
        Entity.resetIdCounter();
        ECSWorld ecsWorld = new ECSWorld();
        for (SaveData.EntityData ed : data.entities) {
            Entity entity = ecsWorld.createEntity();

            if (ed.hasPosition) {
                entity.add(new PositionComponent(ed.posX, ed.posY));
            }
            if (ed.hasIdentity) {
                entity.add(new IdentityComponent(ed.name, ed.age));
            }
            if (ed.hasNeeds) {
                NeedsComponent n = new NeedsComponent();
                n.hunger = ed.hunger; n.energy = ed.energy;
                n.health = ed.needsHealth;
                entity.add(n);
                entity.add(new MoodComponent()); // mood calculated by MoodSystem
            }
            if (ed.hasHealth) {
                HealthComponent h = new HealthComponent();
                h.dead = ed.dead;
                entity.add(h);
            }
            if (ed.hasAI) {
                AIComponent ai = new AIComponent();
                ai.taskType = com.haraldsson.syntropy.entities.TaskType.valueOf(ed.taskType);
                ai.targetX = ed.targetX; ai.targetY = ed.targetY;
                ai.aiDisabled = ed.aiDisabled;
                entity.add(ai);
            }
            if (ed.hasInventory) {
                InventoryComponent inv = new InventoryComponent();
                if (ed.carriedItem != null) {
                    inv.carriedItem = new Item(ItemType.valueOf(ed.carriedItem));
                }
                entity.add(inv);
            }
            if (ed.hasSkills) {
                entity.add(new SkillsComponent());
            }
            if (ed.hasBuilding) {
                BuildingComponent bc = new BuildingComponent(
                        ed.buildingType, ed.productionInterval, ed.maxOutput,
                        ItemType.valueOf(ed.producedItemType));
                bc.timer = ed.buildingTimer;
                if (ed.outputBuffer != null) {
                    for (String itemName : ed.outputBuffer) {
                        bc.outputBuffer.add(new Item(ItemType.valueOf(itemName)));
                    }
                }
                entity.add(bc);

                // Re-link tile → building entity
                if (ed.hasPosition) {
                    int tx = (int) ed.posX;
                    int ty = (int) ed.posY;
                    Tile tile = world.getTile(tx, ty);
                    if (tile != null) tile.setBuildingEntity(entity);
                }
            }
            if (ed.hasLeader) {
                LeaderComponent lc = new LeaderComponent();
                lc.charisma = ed.charisma;
                lc.engineering = ed.engineering;
                lc.science = ed.science;
                lc.combat = ed.combat;
                entity.add(lc);
            }
            if (ed.hasAging) {
                AgingComponent ac = new AgingComponent(ed.ageYears, ed.maxAge);
                ac.yearAccumulator = ed.yearAccumulator;
                entity.add(ac);
            }
            if (ed.hasRole) {
                RoleComponent rc = new RoleComponent(
                        com.haraldsson.syntropy.entities.ColonistRole.valueOf(ed.roleName));
                entity.add(rc);
            }
        }

        return new LoadResult(world, ecsWorld);
    }

    public static class LoadResult {
        public final World world;
        public final ECSWorld ecsWorld;
        public LoadResult(World world, ECSWorld ecsWorld) {
            this.world = world;
            this.ecsWorld = ecsWorld;
        }
    }
}
