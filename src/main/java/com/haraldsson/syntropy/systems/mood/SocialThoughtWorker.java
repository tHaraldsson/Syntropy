package com.haraldsson.syntropy.systems.mood;

import com.haraldsson.syntropy.ecs.ECSWorld;
import com.haraldsson.syntropy.ecs.Entity;
import com.haraldsson.syntropy.ecs.components.*;
import com.haraldsson.syntropy.entities.TaskType;

/**
 * Pattern 2 — SocialThoughtWorker.
 * Grants a mood boost when a colonist is socializing (WANDER task near another colonist).
 */
public class SocialThoughtWorker implements ThoughtWorker {
    private static final float SOCIAL_RANGE = 3f;
    private static final float MOOD_BOOST = 8f;

    private final ECSWorld ecsWorld;

    public SocialThoughtWorker(ECSWorld ecsWorld) {
        this.ecsWorld = ecsWorld;
    }

    @Override
    public float getMoodOffset(Entity entity) {
        AIComponent ai = entity.get(AIComponent.class);
        if (ai == null) return 0f;

        // Mood boost while actively socializing (FIX 3)
        if (ai.taskType == TaskType.SOCIALIZING) return +15f;

        // Mood boost when wandering near another colonist (original behavior)
        if (ai.taskType != TaskType.WANDER) return 0f;

        PositionComponent pos = entity.get(PositionComponent.class);
        if (pos == null) return 0f;

        for (Entity other : ecsWorld.getEntitiesWith(PositionComponent.class, HealthComponent.class, IdentityComponent.class)) {
            if (other == entity) continue;
            HealthComponent health = other.get(HealthComponent.class);
            if (health.dead) continue;
            if (other.has(LeaderComponent.class)) continue;
            PositionComponent otherPos = other.get(PositionComponent.class);
            float dx = pos.x - otherPos.x;
            float dy = pos.y - otherPos.y;
            if (dx * dx + dy * dy <= SOCIAL_RANGE * SOCIAL_RANGE) {
                return MOOD_BOOST;
            }
        }
        return 0f;
    }
}
