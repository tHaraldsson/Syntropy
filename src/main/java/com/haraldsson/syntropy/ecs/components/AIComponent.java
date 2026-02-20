package com.haraldsson.syntropy.ecs.components;

import com.haraldsson.syntropy.ecs.Component;
import com.haraldsson.syntropy.entities.TaskType;

public class AIComponent implements Component {
    public TaskType taskType = TaskType.IDLE;
    public int targetX = -1;
    public int targetY = -1;
    public float wanderTimer;
    public float wanderCooldown;
    public boolean aiDisabled;

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
        pos.x += (dx / dist) * step;
        pos.y += (dy / dist) * step;
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

