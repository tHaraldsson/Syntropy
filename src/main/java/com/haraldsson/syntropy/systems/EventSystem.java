package com.haraldsson.syntropy.systems;

import com.haraldsson.syntropy.ecs.ECSWorld;
import com.haraldsson.syntropy.ecs.Entity;
import com.haraldsson.syntropy.ecs.components.HealthComponent;
import com.haraldsson.syntropy.ecs.components.IdentityComponent;
import com.haraldsson.syntropy.ecs.components.NeedsComponent;
import com.haraldsson.syntropy.entities.Item;
import com.haraldsson.syntropy.entities.ItemType;
import com.haraldsson.syntropy.world.World;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

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

    public void update(ECSWorld ecsWorld, World world, float delta) {
        timer += delta;
        if (timer >= nextEventAt) {
            timer = 0f;
            nextEventAt = randomInterval();
            triggerRandomEvent(ecsWorld, world);
        }
    }

    private void triggerRandomEvent(ECSWorld ecsWorld, World world) {
        List<Entity> alive = new ArrayList<>();
        for (Entity e : ecsWorld.getEntitiesWith(NeedsComponent.class, HealthComponent.class)) {
            if (!e.get(HealthComponent.class).dead) alive.add(e);
        }
        if (alive.isEmpty()) return;

        int roll = random.nextInt(5);
        switch (roll) {
            case 0: eventFoodBlessing(world); break;
            case 1: eventHeatWave(alive); break;
            case 2: eventMoralBoost(alive); break;
            case 3: eventFoodSpoilage(world); break;
            case 4: eventExhaustion(alive); break;
        }
    }

    private void eventFoodBlessing(World world) {
        if (world.getStockpileTile() != null) {
            for (int i = 0; i < 3; i++) {
                world.getStockpileTile().addItem(new Item(ItemType.FOOD));
            }
        }
        log("EVENT: Bountiful harvest! 3 food added to stockpile.");
    }

    private void eventHeatWave(List<Entity> alive) {
        for (Entity e : alive) {
            NeedsComponent n = e.get(NeedsComponent.class);
            n.hunger = Math.max(0, n.hunger - 0.15f);
        }
        log("EVENT: Heat wave! All colonists lost hunger.");
    }

    private void eventMoralBoost(List<Entity> alive) {
        // With decoupled mood (Pattern 2), this now provides a health/energy bump
        for (Entity e : alive) {
            NeedsComponent n = e.get(NeedsComponent.class);
            n.energy = Math.min(1f, n.energy + 0.1f);
        }
        log("EVENT: Beautiful sunset. All colonists feel refreshed.");
    }

    private void eventFoodSpoilage(World world) {
        if (world.getStockpileTile() != null) {
            world.getStockpileTile().takeFirstItem(ItemType.FOOD);
        }
        log("EVENT: Food spoilage! 1 food lost from stockpile.");
    }

    private void eventExhaustion(List<Entity> alive) {
        Entity target = alive.get(random.nextInt(alive.size()));
        NeedsComponent n = target.get(NeedsComponent.class);
        n.energy = Math.max(0, n.energy - 0.25f);
        IdentityComponent id = target.get(IdentityComponent.class);
        String name = id != null ? id.name : "A colonist";
        log("EVENT: " + name + " feels exhausted! Lost 25 energy.");
    }

    private void log(String message) {
        eventLog.add(message);
        if (eventLog.size() > 5) eventLog.remove(0);
    }

    public List<String> getEventLog() {
        return eventLog;
    }

    private float randomInterval() {
        return MIN_INTERVAL + random.nextFloat() * (MAX_INTERVAL - MIN_INTERVAL);
    }
}
