package com.haraldsson.syntropy.ecs.components;

import com.haraldsson.syntropy.ecs.Component;
import com.haraldsson.syntropy.entities.Item;
import com.haraldsson.syntropy.entities.ItemType;

import java.util.ArrayList;
import java.util.List;

public class BuildingComponent implements Component {
    public String buildingType;  // "MINER" or "FOOD_GROWER"
    public boolean built = true;
    public final List<Item> outputBuffer = new ArrayList<>();
    public float timer;
    public float productionInterval;
    public int maxOutput;
    public ItemType producedItemType;

    public BuildingComponent(String type, float interval, int max, ItemType produced) {
        this.buildingType = type;
        this.productionInterval = interval;
        this.maxOutput = max;
        this.producedItemType = produced;
    }

    public boolean hasOutput() {
        return !outputBuffer.isEmpty();
    }

    public Item takeOutput() {
        return outputBuffer.isEmpty() ? null : outputBuffer.remove(0);
    }

    public int getOutputCount() {
        return outputBuffer.size();
    }
}

