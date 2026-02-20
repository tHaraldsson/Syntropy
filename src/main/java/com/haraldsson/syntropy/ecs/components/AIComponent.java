package com.haraldsson.syntropy.ecs.components;

import com.haraldsson.syntropy.ecs.Component;
import com.haraldsson.syntropy.entities.TaskType;
import com.haraldsson.syntropy.world.Pathfinder;
import com.haraldsson.syntropy.world.World;

import java.util.List;

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

    // Path waypoints — list of [x, y] tile coords to walk through (C2)
    public List<int[]> currentPath = null;
    public int pathIndex = 0;

    public void setTask(TaskType type, int tx, int ty) {
        this.taskType = type;
        this.targetX = tx;
        this.targetY = ty;
        this.currentPath = null; // force path recompute on next move
        this.pathIndex = 0;
    }

    public void clearTask() {
        this.taskType = TaskType.IDLE;
        this.targetX = -1;
        this.targetY = -1;
        this.currentPath = null;
        this.pathIndex = 0;
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
        if (world != null) {
            // Lazily compute A* path if not set (C4)
            if (currentPath == null) {
                int sx = (int) Math.floor(pos.x);
                int sy = (int) Math.floor(pos.y);
                currentPath = Pathfinder.findPath(world, sx, sy, targetX, targetY);
                pathIndex = 0;
                if (currentPath.isEmpty() && (sx != targetX || sy != targetY)) {
                    return; // no path found — stuck timer will fire
                }
            }

            // Follow path waypoints (C3)
            if (pathIndex < currentPath.size()) {
                int[] waypoint = currentPath.get(pathIndex);
                float wpCx = waypoint[0] + 0.5f; // center of tile
                float wpCy = waypoint[1] + 0.5f;
                float dx = wpCx - pos.x;
                float dy = wpCy - pos.y;
                float dist = (float) Math.sqrt(dx * dx + dy * dy);
                if (dist < 0.1f) {
                    pathIndex++;
                    return;
                }
                float nx = pos.x + (dx / dist) * speed * delta;
                float ny = pos.y + (dy / dist) * speed * delta;
                if (world.canMove(nx, pos.y)) pos.x = nx;
                if (world.canMove(pos.x, ny)) pos.y = ny;
            } else {
                // Path exhausted or start==goal — final approach to target center
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
                float nx = pos.x + (dx / dist) * speed * delta;
                float ny = pos.y + (dy / dist) * speed * delta;
                if (world.canMove(nx, pos.y)) pos.x = nx;
                if (world.canMove(pos.x, ny)) pos.y = ny;
            }
        } else {
            // No world — direct movement (fallback, no collision)
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

