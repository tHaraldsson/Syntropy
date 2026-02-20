package com.haraldsson.syntropy.ai.nodes;

import com.haraldsson.syntropy.ai.ThinkNode;
import com.haraldsson.syntropy.ecs.ECSWorld;
import com.haraldsson.syntropy.ecs.Entity;
import com.haraldsson.syntropy.ecs.components.*;
import com.haraldsson.syntropy.entities.TaskType;
import com.haraldsson.syntropy.world.World;

/**
 * Socialize with nearby colonists when all needs are met.
 * Priority 10 — low priority, only when nothing else to do.
 * Walking toward another colonist and standing near them boosts mood
 * (via future SocialThoughtWorker).
 */
public class ThinkNode_Socialize extends ThinkNode {
    private static final float MOVE_SPEED = 1.5f;
    private static final float SOCIAL_RANGE = 2f;

    @Override
    public float getPriority(Entity entity, ECSWorld ecsWorld, World world) {
        NeedsComponent needs = entity.get(NeedsComponent.class);
        if (needs == null) return 0f;
        // Only socialize when not hungry or tired
        if (needs.isHungry() || needs.isTired()) return 0f;

        // Check if there are other living colonists to socialize with
        for (Entity other : ecsWorld.getEntitiesWith(PositionComponent.class, HealthComponent.class, IdentityComponent.class)) {
            if (other == entity) continue;
            HealthComponent otherHealth = other.get(HealthComponent.class);
            if (otherHealth.dead) continue;
            if (other.has(LeaderComponent.class)) continue; // don't chase the leader
            return 10f;
        }
        return 0f;
    }

    @Override
    public boolean execute(Entity entity, ECSWorld ecsWorld, World world, float delta) {
        AIComponent ai = entity.get(AIComponent.class);
        PositionComponent pos = entity.get(PositionComponent.class);
        if (ai == null || pos == null) return false;

        // Find nearest living non-leader colonist
        Entity nearest = null;
        float nearestDist = Float.MAX_VALUE;
        for (Entity other : ecsWorld.getEntitiesWith(PositionComponent.class, HealthComponent.class, IdentityComponent.class)) {
            if (other == entity) continue;
            HealthComponent otherHealth = other.get(HealthComponent.class);
            if (otherHealth.dead) continue;
            if (other.has(LeaderComponent.class)) continue;
            PositionComponent otherPos = other.get(PositionComponent.class);
            float dx = pos.x - otherPos.x;
            float dy = pos.y - otherPos.y;
            float dist = dx * dx + dy * dy;
            if (dist < nearestDist) {
                nearestDist = dist;
                nearest = other;
            }
        }

        if (nearest == null) return false;

        PositionComponent targetPos = nearest.get(PositionComponent.class);
        float dx = pos.x - targetPos.x;
        float dy = pos.y - targetPos.y;
        float dist = (float) Math.sqrt(dx * dx + dy * dy);

        if (dist < SOCIAL_RANGE) {
            // We're close enough — "socializing" (standing near them)
            // Mood boost comes from SocialThoughtWorker (to be added)
            ai.clearTask();
            return true;
        }

        // Move toward the other colonist
        ai.setTask(TaskType.WANDER, (int) targetPos.x, (int) targetPos.y);
        ai.moveTowardTarget(pos, delta, MOVE_SPEED, world);
        return true;
    }
}

