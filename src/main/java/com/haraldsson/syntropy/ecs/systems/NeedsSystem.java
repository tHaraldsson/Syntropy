package com.haraldsson.syntropy.ecs.systems;

import com.haraldsson.syntropy.ecs.ECSWorld;
import com.haraldsson.syntropy.ecs.Entity;
import com.haraldsson.syntropy.ecs.GameSystem;
import com.haraldsson.syntropy.ecs.components.HealthComponent;
import com.haraldsson.syntropy.ecs.components.NeedsComponent;
import com.haraldsson.syntropy.world.World;

public class NeedsSystem extends GameSystem {
    @Override
    public void update(ECSWorld ecsWorld, World world, float delta) {
        for (Entity e : ecsWorld.getEntitiesWith(NeedsComponent.class, HealthComponent.class)) {
            HealthComponent health = e.get(HealthComponent.class);
            if (health.dead) continue;

            NeedsComponent needs = e.get(NeedsComponent.class);
            needs.hunger = Math.max(0f, needs.hunger - NeedsComponent.HUNGER_DECAY * delta);
            needs.energy = Math.max(0f, needs.energy - NeedsComponent.ENERGY_DECAY * delta);

            if (needs.hunger < 20f || needs.energy < 20f) {
                needs.mood = Math.max(0f, needs.mood - NeedsComponent.MOOD_DECAY * 3f * delta);
            } else if (needs.hunger > 60f && needs.energy > 60f) {
                needs.mood = Math.min(100f, needs.mood + NeedsComponent.MOOD_DECAY * 0.5f * delta);
            } else {
                needs.mood = Math.max(0f, needs.mood - NeedsComponent.MOOD_DECAY * delta);
            }

            if (needs.hunger <= 0f) {
                health.dead = true;
            }
        }
    }
}

