package com.haraldsson.syntropy.entities;

import com.haraldsson.syntropy.world.World;

import java.util.ArrayList;
import java.util.List;

public class Miner extends Building {
    private static final float PRODUCTION_INTERVAL = 5f;
    private static final int MAX_OUTPUT = 5;

    private final List<Item> outputBuffer = new ArrayList<>();
    private float timer;

    public Miner(int x, int y) {
        super(x, y);
    }

    @Override
    public void update(float delta, World world) {
        timer += delta;
        if (timer >= PRODUCTION_INTERVAL && outputBuffer.size() < MAX_OUTPUT) {
            timer = 0f;
            outputBuffer.add(new Item(ItemType.STONE));
        }
    }

    public boolean hasOutput() {
        return !outputBuffer.isEmpty();
    }

    public Item takeOutput() {
        if (outputBuffer.isEmpty()) {
            return null;
        }
        return outputBuffer.remove(0);
    }

    public int getOutputCount() {
        return outputBuffer.size();
    }
}

