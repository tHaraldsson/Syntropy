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
    public float stuckTimer = 0f; // used by ThinkNodes to detect navigation deadlock

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
        if (world != null && !world.isPassable((int) nx, (int) ny)) {
            return; // blocked by impassable terrain
        }
        pos.x = nx;
        pos.y = ny;
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

