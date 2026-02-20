package com.haraldsson.syntropy.ai.nodes;

import com.haraldsson.syntropy.ai.ThinkNode;
import com.haraldsson.syntropy.ecs.ECSWorld;
import com.haraldsson.syntropy.ecs.Entity;
import com.haraldsson.syntropy.ecs.components.*;
import com.haraldsson.syntropy.entities.Item;
import com.haraldsson.syntropy.entities.TaskType;
import com.haraldsson.syntropy.world.Tile;
import com.haraldsson.syntropy.world.World;

/**
 * Haul items from building output to stockpile. Priority 50 (assigned job level).
 */
public class ThinkNode_Haul extends ThinkNode {
    private static final float MOVE_SPEED = 2.2f;

    @Override
    public float getPriority(Entity entity, ECSWorld ecsWorld, World world) {
        InventoryComponent inv = entity.get(InventoryComponent.class);
        if (inv != null && inv.carriedItem != null) return 50f; // must deliver

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
            ai.moveTowardTarget(pos, delta, MOVE_SPEED);
            if (ai.isAtTarget(pos.x, pos.y)) {
                stockpile.addItem(inv.carriedItem);
                inv.carriedItem = null;
                ai.clearTask();
            }
            return true;
        }

        // Find a building with output to pick up
        for (Entity bldg : ecsWorld.getEntitiesWith(BuildingComponent.class, PositionComponent.class)) {
            BuildingComponent bc = bldg.get(BuildingComponent.class);
            if (!bc.hasOutput()) continue;
            PositionComponent bp = bldg.get(PositionComponent.class);
            TaskType task = "MINER".equals(bc.buildingType)
                    ? TaskType.MOVE_TO_MINER : TaskType.MOVE_TO_FOOD_GROWER;
            ai.setTask(task, (int) bp.x, (int) bp.y);
            ai.moveTowardTarget(pos, delta, MOVE_SPEED);
            if (ai.isAtTarget(pos.x, pos.y)) {
                Item output = bc.takeOutput();
                if (output != null) inv.carriedItem = output;
                ai.clearTask();
            }
            return true;
        }
        return false;
    }
}

