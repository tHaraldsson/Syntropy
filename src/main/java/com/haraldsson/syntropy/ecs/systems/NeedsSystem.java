package com.haraldsson.syntropy.ecs.systems;

import com.haraldsson.syntropy.ecs.ECSWorld;
import com.haraldsson.syntropy.ecs.Entity;
import com.haraldsson.syntropy.ecs.GameSystem;
import com.haraldsson.syntropy.ecs.components.HealthComponent;
import com.haraldsson.syntropy.ecs.components.NeedsComponent;
import com.haraldsson.syntropy.world.World;

/**
 * Ticks hunger/energy decay. Health damage from starvation. Health regen when well-fed.
 * Mood is handled by MoodSystem (Pattern 2) — NOT here.
 */
public class NeedsSystem extends GameSystem {
    @Override
    public void update(ECSWorld ecsWorld, World world, float delta) {
        for (Entity e : ecsWorld.getEntitiesWith(NeedsComponent.class, HealthComponent.class)) {
            HealthComponent health = e.get(HealthComponent.class);
            if (health.dead) continue;

            NeedsComponent needs = e.get(NeedsComponent.class);
            needs.hunger = Math.max(0f, needs.hunger - NeedsComponent.HUNGER_DECAY * delta);
            needs.energy = Math.max(0f, needs.energy - NeedsComponent.ENERGY_DECAY * delta);

            // Starvation damage
            if (needs.hunger <= 0f) {
                needs.damage(0.05f * delta);
            }

            // Health regen when well-fed and rested
            if (needs.hunger > 0.5f && needs.energy > 0.5f) {
                needs.heal(NeedsComponent.HEALTH_REGEN * delta);
            }

            if (needs.health <= 0f) {
                health.dead = true;
            }
        }
    }
}
