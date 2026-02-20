package com.haraldsson.syntropy.core;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonWriter;
import com.haraldsson.syntropy.entities.Colonist;
import com.haraldsson.syntropy.entities.FoodGrower;
import com.haraldsson.syntropy.entities.Item;
import com.haraldsson.syntropy.entities.ItemType;
import com.haraldsson.syntropy.entities.Miner;
import com.haraldsson.syntropy.entities.TaskType;
import com.haraldsson.syntropy.world.TerrainType;
import com.haraldsson.syntropy.world.Tile;
import com.haraldsson.syntropy.world.World;

public final class SaveLoadSystem {
    private SaveLoadSystem() {
    }

    public static void save(World world, String fileName) {
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
                data.tiles.add(td);
            }
        }

        // Stockpile location
        Tile sp = world.getStockpileTile();
        if (sp != null) {
            data.stockpileX = sp.getX();
            data.stockpileY = sp.getY();
        }

        // Colonists
        for (Colonist c : world.getColonists()) {
            SaveData.ColonistData cd = new SaveData.ColonistData();
            cd.name = c.getName();
            cd.age = c.getAge();
            cd.x = c.getX();
            cd.y = c.getY();
            cd.hunger = c.getHunger();
            cd.energy = c.getEnergy();
            cd.mood = c.getMood();
            cd.dead = c.isDead();
            cd.aiDisabled = c.isAiDisabled();
            cd.carriedItemType = c.getCarriedItem() != null ? c.getCarriedItem().getType().name() : null;
            cd.taskType = c.getTaskType().name();
            cd.targetX = c.getTargetX();
            cd.targetY = c.getTargetY();
            data.colonists.add(cd);
        }

        // Miners
        for (Miner m : world.getMiners()) {
            SaveData.MinerData md = new SaveData.MinerData();
            md.x = m.getX();
            md.y = m.getY();
            md.timer = m.getTimer();
            for (Item item : m.getOutputBuffer()) {
                md.outputBuffer.add(item.getType().name());
            }
            data.miners.add(md);
        }

        // FoodGrowers
        for (FoodGrower fg : world.getFoodGrowers()) {
            SaveData.FoodGrowerData fgd = new SaveData.FoodGrowerData();
            fgd.x = fg.getX();
            fgd.y = fg.getY();
            fgd.timer = fg.getTimer();
            for (Item item : fg.getOutputBuffer()) {
                fgd.outputBuffer.add(item.getType().name());
            }
            data.foodGrowers.add(fgd);
        }

        Json json = new Json();
        json.setOutputType(JsonWriter.OutputType.json);
        String jsonStr = json.prettyPrint(data);

        FileHandle file = Gdx.files.local(fileName);
        file.writeString(jsonStr, false);
    }

    public static World load(String fileName) {
        FileHandle file = Gdx.files.local(fileName);
        if (!file.exists()) {
            throw new RuntimeException("Save file not found: " + fileName);
        }

        Json json = new Json();
        SaveData data = json.fromJson(SaveData.class, file.readString());

        Tile[][] tiles = new Tile[data.worldWidth][data.worldHeight];
        for (SaveData.TileData td : data.tiles) {
            TerrainType terrain = TerrainType.valueOf(td.terrain);
            Tile tile = new Tile(td.x, td.y, terrain);
            tile.setStockpile(td.stockpile);
            for (String itemName : td.groundItems) {
                tile.addItem(new Item(ItemType.valueOf(itemName)));
            }
            tiles[td.x][td.y] = tile;
        }

        World world = new World(data.worldWidth, data.worldHeight, tiles);

        // Stockpile
        if (data.stockpileX >= 0 && data.stockpileY >= 0) {
            world.setStockpileTile(tiles[data.stockpileX][data.stockpileY]);
        }

        // Miners
        for (SaveData.MinerData md : data.miners) {
            Miner miner = new Miner(md.x, md.y);
            miner.setTimer(md.timer);
            for (String itemName : md.outputBuffer) {
                miner.getOutputBuffer().add(new Item(ItemType.valueOf(itemName)));
            }
            world.addMiner(miner);
            tiles[md.x][md.y].setBuilding(miner);
        }

        // FoodGrowers
        for (SaveData.FoodGrowerData fgd : data.foodGrowers) {
            FoodGrower grower = new FoodGrower(fgd.x, fgd.y);
            grower.setTimer(fgd.timer);
            for (String itemName : fgd.outputBuffer) {
                grower.getOutputBuffer().add(new Item(ItemType.valueOf(itemName)));
            }
            world.addFoodGrower(grower);
            tiles[fgd.x][fgd.y].setBuilding(grower);
        }

        // Colonists
        for (SaveData.ColonistData cd : data.colonists) {
            Colonist colonist = new Colonist(cd.name, cd.age, cd.x, cd.y);
            colonist.setHunger(cd.hunger);
            colonist.setEnergy(cd.energy);
            colonist.setMood(cd.mood);
            colonist.setDead(cd.dead);
            colonist.setAiDisabled(cd.aiDisabled);
            if (cd.carriedItemType != null) {
                colonist.setCarriedItem(new Item(ItemType.valueOf(cd.carriedItemType)));
            }
            colonist.setTask(TaskType.valueOf(cd.taskType), cd.targetX, cd.targetY);
            world.addColonist(colonist);
        }

        return world;
    }
}

