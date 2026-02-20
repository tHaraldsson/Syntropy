package com.haraldsson.syntropy.ecs.systems;

import com.haraldsson.syntropy.ai.ThinkTreeFactory;
import com.haraldsson.syntropy.ai.ThinkTreeRoot;
import com.haraldsson.syntropy.ecs.ECSWorld;
import com.haraldsson.syntropy.ecs.Entity;
import com.haraldsson.syntropy.ecs.GameSystem;
import com.haraldsson.syntropy.ecs.components.*;
import com.haraldsson.syntropy.world.World;

/**
 * AI system — runs Think Tree for each NPC colonist.
 * The leader entity is skipped (has LeaderComponent, controlled by player).
 */
public class AITaskSystem extends GameSystem {
    private final ThinkTreeRoot colonistTree;
    private static final int MAX_RECOVERY_RADIUS = 5;

    public AITaskSystem() {
        this.colonistTree = ThinkTreeFactory.createColonistTree();
    }

    @Override
    public void update(ECSWorld ecsWorld, World world, float delta) {
        for (Entity e : ecsWorld.getEntitiesWith(
                AIComponent.class, NeedsComponent.class, PositionComponent.class,
                InventoryComponent.class, HealthComponent.class)) {

            // Skip leader — player-controlled
            if (e.has(LeaderComponent.class)) continue;

            AIComponent ai = e.get(AIComponent.class);
            HealthComponent health = e.get(HealthComponent.class);
            if (health.dead || ai.aiDisabled) continue;

            PositionComponent pos = e.get(PositionComponent.class);
            int tileX = (int) Math.floor(pos.x);
            int tileY = (int) Math.floor(pos.y);

            // FIX BUG-STUCK: recover from impassable position before thinking (2026-02-20)
            if (!world.isPassable(tileX, tileY)) {
                tryRecoverFromImpassable(e, world);
                continue; // skip this frame's think cycle
            }

            colonistTree.execute(e, ecsWorld, world, delta);
        }
    }

    /**
     * If this entity is inside impassable terrain, teleport it to the nearest passable tile
     * within radius 5. Returns true if recovery succeeded.
     * Inspired by RimWorld's Pawn_PathFollower.TryRecoverFromUnwalkablePosition()
     * FIX BUG-STUCK: RimWorld-style TryRecoverFromUnwalkablePosition (2026-02-20)
     */
    public static boolean tryRecoverFromImpassable(Entity entity, World world) {
        PositionComponent pos = entity.get(PositionComponent.class);
        AIComponent ai = entity.get(AIComponent.class);
        if (pos == null || ai == null) return false;

        int cx = (int) Math.floor(pos.x);
        int cy = (int) Math.floor(pos.y);

        // Already on passable tile — no recovery needed
        if (world.isPassable(cx, cy)) return true;

        // Spiral outward to find nearest passable tile (up to MAX_RECOVERY_RADIUS)
        for (int r = 1; r <= MAX_RECOVERY_RADIUS; r++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dy = -r; dy <= r; dy++) {
                    if (Math.abs(dx) != r && Math.abs(dy) != r) continue; // only outer ring
                    int nx = cx + dx;
                    int ny = cy + dy;
                    if (world.isPassable(nx, ny)) {
                        // Teleport to center of nearest passable tile
                        pos.x = nx + 0.5f;
                        pos.y = ny + 0.5f;
                        ai.clearTask();
                        ai.stuckTimer = 0f;
                        return true;
                    }
                }
            }
        }
        return false; // no passable tile found within radius
    }
}
