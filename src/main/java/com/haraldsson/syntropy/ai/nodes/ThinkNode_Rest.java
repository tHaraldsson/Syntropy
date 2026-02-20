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
 */
public class ThinkNode_Rest extends ThinkNode {
    private static final float REST_DURATION = 3f;

    @Override
    public float getPriority(Entity entity, ECSWorld ecsWorld, World world) {
        NeedsComponent needs = entity.get(NeedsComponent.class);
        AIComponent ai = entity.get(AIComponent.class);
        if (needs == null) return 0f;
        if (ai != null && ai.taskType == TaskType.RESTING) return 80f; // keep resting
        return switch (needs.getEnergyCategory()) {
            case COLLAPSED -> 80f;
            case EXHAUSTED -> 60f;
            case TIRED -> 15f;
            case RESTED -> 0f;
        };
    }

    @Override
    public boolean execute(Entity entity, ECSWorld ecsWorld, World world, float delta) {
        NeedsComponent needs = entity.get(NeedsComponent.class);
        AIComponent ai = entity.get(AIComponent.class);
        PositionComponent pos = entity.get(PositionComponent.class);
        if (needs == null || ai == null || pos == null) return false;

        if (ai.taskType != TaskType.RESTING) {
            ai.setTask(TaskType.RESTING, (int) pos.x, (int) pos.y);
            ai.resetWanderCooldown(REST_DURATION);
        }
        if (ai.shouldPickNewWanderTarget(delta)) {
            needs.rest();
            ai.clearTask();
            return false;
        }
        return true;
    }
}
