package com.haraldsson.syntropy.systems;

import com.haraldsson.syntropy.entities.Colonist;
import com.haraldsson.syntropy.world.World;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Periodically triggers random events that affect colonists or the colony.
 */
public class EventSystem {
    private static final float MIN_INTERVAL = 30f;
    private static final float MAX_INTERVAL = 60f;

    private final Random random = new Random();
    private float timer;
    private float nextEventAt;
    private final List<String> eventLog = new ArrayList<>();

    public EventSystem() {
        nextEventAt = randomInterval();
    }

    public void update(World world, float delta) {
        timer += delta;
        if (timer >= nextEventAt) {
            timer = 0f;
            nextEventAt = randomInterval();
            triggerRandomEvent(world);
        }
    }

    private void triggerRandomEvent(World world) {
        List<Colonist> alive = new ArrayList<>();
        for (Colonist c : world.getColonists()) {
            if (!c.isDead()) alive.add(c);
        }
        if (alive.isEmpty()) return;

        int roll = random.nextInt(5);
        switch (roll) {
            case 0: eventFoodBlessing(world, alive); break;
            case 1: eventHeatWave(alive); break;
            case 2: eventMoralBoost(alive); break;
            case 3: eventFoodSpoilage(world); break;
            case 4: eventExhaustion(alive); break;
        }
    }

    private void eventFoodBlessing(World world, List<Colonist> alive) {
        if (world.getStockpileTile() != null) {
            for (int i = 0; i < 3; i++) {
                world.getStockpileTile().addItem(
                        new com.haraldsson.syntropy.entities.Item(
                                com.haraldsson.syntropy.entities.ItemType.FOOD));
            }
        }
        log("EVENT: Bountiful harvest! 3 food added to stockpile.");
    }

    private void eventHeatWave(List<Colonist> alive) {
        for (Colonist c : alive) {
            c.setHunger(Math.max(0, c.getHunger() - 15f));
        }
        log("EVENT: Heat wave! All colonists lost 15 hunger.");
    }

    private void eventMoralBoost(List<Colonist> alive) {
        for (Colonist c : alive) {
            c.setMood(Math.min(100, c.getMood() + 20f));
        }
        log("EVENT: Beautiful sunset. All colonists gained 20 mood.");
    }

    private void eventFoodSpoilage(World world) {
        if (world.getStockpileTile() != null) {
            world.getStockpileTile().takeFirstItem(
                    com.haraldsson.syntropy.entities.ItemType.FOOD);
        }
        log("EVENT: Food spoilage! 1 food lost from stockpile.");
    }

    private void eventExhaustion(List<Colonist> alive) {
        Colonist target = alive.get(random.nextInt(alive.size()));
        target.setEnergy(Math.max(0, target.getEnergy() - 25f));
        log("EVENT: " + target.getName() + " feels exhausted! Lost 25 energy.");
    }

    private void log(String message) {
        eventLog.add(message);
        if (eventLog.size() > 5) {
            eventLog.remove(0);
        }
    }

    public List<String> getEventLog() {
        return eventLog;
    }

    public String getLatestEvent() {
        return eventLog.isEmpty() ? "" : eventLog.get(eventLog.size() - 1);
    }

    private float randomInterval() {
        return MIN_INTERVAL + random.nextFloat() * (MAX_INTERVAL - MIN_INTERVAL);
    }
}

