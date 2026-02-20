package com.haraldsson.syntropy.ai.nodes;

import com.haraldsson.syntropy.ai.ThinkNode;
import com.haraldsson.syntropy.ecs.ECSWorld;
import com.haraldsson.syntropy.ecs.Entity;
import com.haraldsson.syntropy.ecs.components.*;
import com.haraldsson.syntropy.entities.TaskType;
import com.haraldsson.syntropy.world.World;

/**
 * Lowest priority: wander randomly when nothing else to do.
 */
public class ThinkNode_Wander extends ThinkNode {
    private static final float MOVE_SPEED = 1.3f;

    @Override
    public float getPriority(Entity entity, ECSWorld ecsWorld, World world) {
        return 1f; // always available as fallback
    }

    @Override
    public boolean execute(Entity entity, ECSWorld ecsWorld, World world, float delta) {
        AIComponent ai = entity.get(AIComponent.class);
        PositionComponent pos = entity.get(PositionComponent.class);
        if (ai == null || pos == null) return false;

        if (ai.shouldPickNewWanderTarget(delta) || ai.taskType == TaskType.IDLE) {
            int tx, ty;
            int attempts = 0;
            do {
                tx = (int) (Math.random() * world.getWidth());
                ty = (int) (Math.random() * world.getHeight());
                attempts++;
            } while (!world.isPassable(tx, ty) && attempts < 20);
            // If no passable tile found, stay in place
            if (world.isPassable(tx, ty)) {
                ai.setTask(TaskType.WANDER, tx, ty);
            } else {
                ai.clearTask();
            }
            ai.resetWanderCooldown(2f + (float) Math.random() * 2f);
        }
        ai.moveTowardTarget(pos, delta, MOVE_SPEED, world);
        if (ai.isAtTarget(pos.x, pos.y)) {
            ai.clearTask();
        }
        return true;
    }
}
