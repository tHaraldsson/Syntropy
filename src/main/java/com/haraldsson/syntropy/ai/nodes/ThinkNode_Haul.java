package com.haraldsson.syntropy.ai.nodes;

import com.haraldsson.syntropy.ai.ThinkNode;
import com.haraldsson.syntropy.ecs.ECSWorld;
import com.haraldsson.syntropy.ecs.Entity;
import com.haraldsson.syntropy.ecs.components.*;
import com.haraldsson.syntropy.entities.ColonistRole;
import com.haraldsson.syntropy.entities.Item;
import com.haraldsson.syntropy.entities.TaskType;
import com.haraldsson.syntropy.world.Tile;
import com.haraldsson.syntropy.world.World;

/**
 * Haul items from building output to stockpile. Priority 50 (assigned job level).
 */
public class ThinkNode_Haul extends ThinkNode {
    private static final float MOVE_SPEED = 2.2f;
    private static final float STUCK_TIMEOUT_SECONDS = 5f;

    @Override
    public float getPriority(Entity entity, ECSWorld ecsWorld, World world) {
        // FIX: ThinkNode_Haul only activates for HAULER-role colonists — 2026-02-20
        WorkSettingsComponent work = entity.get(WorkSettingsComponent.class);
        if (work == null || work.getPriority(ColonistRole.HAULER) == 0) return 0f;

        InventoryComponent inv = entity.get(InventoryComponent.class);
        if (inv != null && inv.carriedItem != null) return 50f; // must deliver

        // FIX BUG1: haul logic now handles all item types including WOOD (2026-02-20)
        for (Entity bldg : ecsWorld.getEntitiesWith(BuildingComponent.class)) {
            BuildingComponent bc = bldg.get(BuildingComponent.class);
            if (bc.hasOutput()) return 50f;
        }
        return 0f;
    }

    @Override
    public boolean execute(Entity entity, ECSWorld ecsWorld, World world, float delta) {
        AIComponent ai = entity.get(AIComponent.class);
        PositionComponent pos = entity.get(PositionComponent.class);
        InventoryComponent inv = entity.get(InventoryComponent.class);
        if (ai == null || pos == null || inv == null) return false;

        // If carrying something, deliver to stockpile
        if (inv.carriedItem != null) {
            Tile stockpile = world.getStockpileTile();
            if (stockpile == null) return false;
            ai.setTask(TaskType.MOVE_TO_STOCKPILE, stockpile.getX(), stockpile.getY());
            ai.stuckTimer += delta;
            if (ai.stuckTimer > STUCK_TIMEOUT_SECONDS) {
                ai.recoverFromStuck(pos, world);
                return false;
            }
            ai.moveTowardTarget(pos, delta, MOVE_SPEED, world);
            if (ai.isAtTarget(pos.x, pos.y)) {
                stockpile.addItem(inv.carriedItem);
                inv.carriedItem = null;
                ai.clearTask();
                ai.stuckTimer = 0f;
            }
            return true;
        }

        // FIX BUG1: haul logic now handles all item types including WOOD (2026-02-20)
        // Find the nearest building with output to pick up (any building type)
        Entity nearest = null;
        float nearestDist = Float.MAX_VALUE;
        for (Entity bldg : ecsWorld.getEntitiesWith(BuildingComponent.class, PositionComponent.class)) {
            BuildingComponent bc = bldg.get(BuildingComponent.class);
            if (!bc.hasOutput()) continue;
            PositionComponent bp = bldg.get(PositionComponent.class);
            float dx = pos.x - bp.x;
            float dy = pos.y - bp.y;
            float dist = dx * dx + dy * dy;
            if (dist < nearestDist) {
                nearestDist = dist;
                nearest = bldg;
            }
        }
        if (nearest == null) return false;
        BuildingComponent bc = nearest.get(BuildingComponent.class);
        PositionComponent bp = nearest.get(PositionComponent.class);
        ai.setTask(TaskType.HAULING, (int) Math.floor(bp.x), (int) Math.floor(bp.y));
        ai.stuckTimer += delta;
        if (ai.stuckTimer > STUCK_TIMEOUT_SECONDS) {
            ai.recoverFromStuck(pos, world);
            return false;
        }
        ai.moveTowardTarget(pos, delta, MOVE_SPEED, world);
        if (ai.isAtTarget(pos.x, pos.y)) {
            Item output = bc.takeOutput();
            if (output != null) inv.carriedItem = output;
            ai.clearTask();
            ai.stuckTimer = 0f;
        }
        return true;
    }
}
