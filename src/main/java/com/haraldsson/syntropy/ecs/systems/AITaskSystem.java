package com.haraldsson.syntropy.ecs.systems;

import com.haraldsson.syntropy.ecs.ECSWorld;
import com.haraldsson.syntropy.ecs.Entity;
import com.haraldsson.syntropy.ecs.GameSystem;
import com.haraldsson.syntropy.ecs.components.*;
import com.haraldsson.syntropy.entities.Item;
import com.haraldsson.syntropy.entities.ItemType;
import com.haraldsson.syntropy.entities.TaskType;
import com.haraldsson.syntropy.world.Tile;
import com.haraldsson.syntropy.world.World;

import java.util.List;

/**
 * AI task system — drives colonist behavior: eat, rest, haul, wander.
 */
public class AITaskSystem extends GameSystem {
    private static final float MOVE_SPEED = 2.2f;
    private static final float REST_DURATION = 3f;

    @Override
    public void update(ECSWorld ecsWorld, World world, float delta) {
        for (Entity e : ecsWorld.getEntitiesWith(
                AIComponent.class, NeedsComponent.class, PositionComponent.class,
                InventoryComponent.class, HealthComponent.class)) {

            AIComponent ai = e.get(AIComponent.class);
            HealthComponent health = e.get(HealthComponent.class);
            if (health.dead || ai.aiDisabled) continue;

            NeedsComponent needs = e.get(NeedsComponent.class);
            PositionComponent pos = e.get(PositionComponent.class);
            InventoryComponent inv = e.get(InventoryComponent.class);

            if (handleHunger(world, ai, needs, pos, delta)) continue;
            if (handleRest(ai, needs, pos, delta)) continue;
            if (handleHauling(ecsWorld, world, ai, pos, inv, delta)) continue;
            handleWander(world, ai, pos, delta);
        }
    }

    private boolean handleHunger(World world, AIComponent ai, NeedsComponent needs,
                                  PositionComponent pos, float delta) {
        if (!needs.isHungry()) return false;
        Tile foodTile = world.findNearestFoodTile(pos.x, pos.y);
        if (foodTile == null) { ai.clearTask(); return true; }

        ai.setTask(TaskType.MOVE_TO_FOOD, foodTile.getX(), foodTile.getY());
        ai.moveTowardTarget(pos, delta, MOVE_SPEED);
        if (ai.isAtTarget(pos.x, pos.y)) {
            Item food = foodTile.takeFirstItem(ItemType.FOOD);
            if (food != null) needs.eat();
            ai.clearTask();
        }
        return true;
    }

    private boolean handleRest(AIComponent ai, NeedsComponent needs,
                                PositionComponent pos, float delta) {
        if (!needs.isTired() && ai.taskType != TaskType.RESTING) return false;
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

    private boolean handleHauling(ECSWorld ecsWorld, World world, AIComponent ai,
                                   PositionComponent pos, InventoryComponent inv, float delta) {
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

        // Find building with output
        for (Entity bldg : ecsWorld.getEntitiesWith(BuildingComponent.class, PositionComponent.class)) {
            BuildingComponent bc = bldg.get(BuildingComponent.class);
            if (!bc.hasOutput()) continue;
            PositionComponent bp = bldg.get(PositionComponent.class);
            TaskType taskType = "MINER".equals(bc.buildingType)
                    ? TaskType.MOVE_TO_MINER : TaskType.MOVE_TO_FOOD_GROWER;
            ai.setTask(taskType, (int) bp.x, (int) bp.y);
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

    private void handleWander(World world, AIComponent ai, PositionComponent pos, float delta) {
        if (ai.shouldPickNewWanderTarget(delta) || ai.taskType == TaskType.IDLE) {
            int tx = (int) (Math.random() * world.getWidth());
            int ty = (int) (Math.random() * world.getHeight());
            ai.setTask(TaskType.WANDER, tx, ty);
            ai.resetWanderCooldown(2f + (float) Math.random() * 2f);
        }
        ai.moveTowardTarget(pos, delta, MOVE_SPEED * 0.6f);
        if (ai.isAtTarget(pos.x, pos.y)) {
            ai.clearTask();
        }
    }
}

