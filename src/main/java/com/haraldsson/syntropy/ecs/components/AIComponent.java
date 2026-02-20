package com.haraldsson.syntropy.ecs.components;

import com.haraldsson.syntropy.ecs.Component;
import com.haraldsson.syntropy.entities.TaskType;
import com.haraldsson.syntropy.world.World;

public class AIComponent implements Component {
    public TaskType taskType = TaskType.IDLE;
    public int targetX = -1;
    public int targetY = -1;
    public float wanderTimer;
    public float wanderCooldown;
    public boolean aiDisabled;

    // Per-colonist stuck detection (FIX 1)
    public float stuckTimer = 0f;
    public int stuckTargetX = -1;
    public int stuckTargetY = -1;

    public void setTask(TaskType type, int tx, int ty) {
        this.taskType = type;
        this.targetX = tx;
        this.targetY = ty;
    }

    public void clearTask() {
        this.taskType = TaskType.IDLE;
        this.targetX = -1;
        this.targetY = -1;
    }

    public boolean isAtTarget(float x, float y) {
        if (targetX < 0 || targetY < 0) return false;
        float dx = x - (targetX + 0.5f);
        float dy = y - (targetY + 0.5f);
        return dx * dx + dy * dy < 0.02f;
    }

    public void moveTowardTarget(PositionComponent pos, float delta, float speed) {
        moveTowardTarget(pos, delta, speed, null);
    }

    public void moveTowardTarget(PositionComponent pos, float delta, float speed, World world) {
        if (targetX < 0 || targetY < 0) return;
        float tcx = targetX + 0.5f;
        float tcy = targetY + 0.5f;
        float dx = tcx - pos.x;
        float dy = tcy - pos.y;
        float dist = (float) Math.sqrt(dx * dx + dy * dy);
        if (dist < 0.05f) {
            pos.x = tcx;
            pos.y = tcy;
            return;
        }
        float step = speed * delta;
        float nx = pos.x + (dx / dist) * step;
        float ny = pos.y + (dy / dist) * step;
        if (world != null) {
            // FIX BUG4b: corrected NPC collision to match player fix (2026-02-20)
            boolean canX = world.isPassable((int) Math.floor(nx), (int) Math.floor(pos.y));
            boolean canY = world.isPassable((int) Math.floor(pos.x), (int) Math.floor(ny));
            if (!canX && !canY) return;
            if (canX) pos.x = nx;
            if (canY) pos.y = ny;
        } else {
            pos.x = nx;
            pos.y = ny;
        }
    }

    public void recoverFromStuck(PositionComponent pos, World world) {
        // FIX BUG4d: stuck NPC teleports to nearest passable tile before clearing task (2026-02-20)
        int[] nearest = world.findNearestPassableTile(pos.x, pos.y);
        if (nearest != null) {
            pos.x = nearest[0] + 0.5f;
            pos.y = nearest[1] + 0.5f;
        }
        clearTask();
        stuckTimer = 0f;
    }

    public boolean shouldPickNewWanderTarget(float delta) {
        wanderTimer += delta;
        return wanderCooldown <= 0f || wanderTimer >= wanderCooldown || taskType == TaskType.IDLE;
    }

    public void resetWanderCooldown(float seconds) {
        wanderCooldown = seconds;
        wanderTimer = 0f;
    }
}

