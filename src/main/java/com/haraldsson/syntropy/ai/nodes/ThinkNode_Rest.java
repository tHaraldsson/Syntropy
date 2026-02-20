package com.haraldsson.syntropy.ai.nodes;

import com.haraldsson.syntropy.ai.ThinkNode;
import com.haraldsson.syntropy.ecs.ECSWorld;
import com.haraldsson.syntropy.ecs.Entity;
import com.haraldsson.syntropy.ecs.components.*;
import com.haraldsson.syntropy.entities.EnergyCategory;
import com.haraldsson.syntropy.entities.TaskType;
import com.haraldsson.syntropy.world.World;

/**
 * Rest when energy is low. Priority scales with exhaustion.
 * Walks to an owned bed first; falls back to sleeping on the ground.
 */
public class ThinkNode_Rest extends ThinkNode {
    private static final float REST_DURATION = 4f;
    private static final float MOVE_SPEED = 2.2f;

    @Override
    public float getPriority(Entity entity, ECSWorld ecsWorld, World world) {
        NeedsComponent needs = entity.get(NeedsComponent.class);
        AIComponent ai = entity.get(AIComponent.class);
        if (needs == null) return 0f;
        if (ai != null && ai.taskType == TaskType.RESTING) return 90f; // keep resting
        return switch (needs.getEnergyCategory()) {
            case COLLAPSED -> 95f;
            case EXHAUSTED -> 75f;
            case TIRED -> 20f;
            case RESTED -> 0f;
        };
    }

    @Override
    public boolean execute(Entity entity, ECSWorld ecsWorld, World world, float delta) {
        NeedsComponent needs = entity.get(NeedsComponent.class);
        AIComponent ai = entity.get(AIComponent.class);
        PositionComponent pos = entity.get(PositionComponent.class);
        if (needs == null || ai == null || pos == null) return false;

        SleepQualityComponent sq = entity.get(SleepQualityComponent.class);

        // Find owned bed
        Entity ownedBed = null;
        for (Entity bedEntity : ecsWorld.getEntitiesWith(BedComponent.class)) {
            BedComponent bed = bedEntity.get(BedComponent.class);
            if (bed.ownerEntityId == entity.getId()) {
                ownedBed = bedEntity;
                break;
            }
        }

        if (ownedBed != null) {
            PositionComponent bedPos = ownedBed.get(PositionComponent.class);
            if (bedPos != null) {
                // Set or maintain RESTING task targeting the bed
                if (ai.taskType != TaskType.RESTING) {
                    ai.setTask(TaskType.RESTING, (int) bedPos.x, (int) bedPos.y);
                }

                if (!ai.isAtTarget(pos.x, pos.y)) {
                    // Walk toward the bed
                    ai.moveTowardTarget(pos, delta, MOVE_SPEED);
                    return true;
                }

                // At bed: start sleep timer on arrival
                if (ai.wanderCooldown <= 0f) {
                    ai.resetWanderCooldown(REST_DURATION);
                }

                if (!ai.shouldPickNewWanderTarget(delta)) {
                    return true; // sleeping in bed
                }

                // Sleep timer expired — apply full rest
                needs.rest();
                if (sq != null) sq.lastSleepQuality = SleepQualityComponent.Quality.IN_BED;
                ai.clearTask();
                ai.resetWanderCooldown(0f); // reset so next sleep cycle starts cleanly
                return false;
            }
        }

        // No owned bed: sleep on ground
        if (ai.taskType != TaskType.RESTING) {
            ai.setTask(TaskType.RESTING, (int) pos.x, (int) pos.y);
            ai.resetWanderCooldown(REST_DURATION);
        }

        if (ai.shouldPickNewWanderTarget(delta)) {
            needs.restPartial(NeedsComponent.REST_AMOUNT * 0.5f);
            if (sq != null) sq.lastSleepQuality = SleepQualityComponent.Quality.ON_GROUND;
            ai.clearTask();
            ai.resetWanderCooldown(0f);
            return false;
        }
        return true;
    }
}

