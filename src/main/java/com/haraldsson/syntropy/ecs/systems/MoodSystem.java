package com.haraldsson.syntropy.ecs.systems;

import com.haraldsson.syntropy.ecs.ECSWorld;
import com.haraldsson.syntropy.ecs.Entity;
import com.haraldsson.syntropy.ecs.GameSystem;
import com.haraldsson.syntropy.ecs.components.HealthComponent;
import com.haraldsson.syntropy.ecs.components.MoodComponent;
import com.haraldsson.syntropy.ecs.components.NeedsComponent;
import com.haraldsson.syntropy.systems.mood.*;
import com.haraldsson.syntropy.world.World;

import java.util.List;

/**
 * Pattern 2 — Decoupled mood system.
 * Mood is the SUM of all ThoughtWorker offsets + a base value.
 * Needs never touch mood directly.
 */
public class MoodSystem extends GameSystem {
    private static final float BASE_MOOD = 50f;

    private final List<ThoughtWorker> workers = List.of(
            new HungerThoughtWorker(),
            new SleepThoughtWorker(),
            new HealthThoughtWorker()
    );

    @Override
    public void update(ECSWorld ecsWorld, World world, float delta) {
        for (Entity e : ecsWorld.getEntitiesWith(NeedsComponent.class, MoodComponent.class, HealthComponent.class)) {
            HealthComponent health = e.get(HealthComponent.class);
            if (health.dead) continue;

            float totalOffset = 0f;
            for (ThoughtWorker worker : workers) {
                totalOffset += worker.getMoodOffset(e);
            }

            MoodComponent mood = e.get(MoodComponent.class);
            float target = Math.max(0f, Math.min(100f, BASE_MOOD + totalOffset));
            // Smooth transition toward target mood
            float speed = 5f * delta;
            if (mood.mood < target) {
                mood.mood = Math.min(target, mood.mood + speed);
            } else if (mood.mood > target) {
                mood.mood = Math.max(target, mood.mood - speed);
            }
        }
    }
}

