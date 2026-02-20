package com.haraldsson.syntropy.ai.nodes;

import com.haraldsson.syntropy.ai.ThinkNode;
import com.haraldsson.syntropy.ecs.ECSWorld;
import com.haraldsson.syntropy.ecs.Entity;
import com.haraldsson.syntropy.ecs.components.*;
import com.haraldsson.syntropy.entities.HungerCategory;
import com.haraldsson.syntropy.entities.Item;
import com.haraldsson.syntropy.entities.ItemType;
import com.haraldsson.syntropy.entities.TaskType;
import com.haraldsson.syntropy.world.Tile;
import com.haraldsson.syntropy.world.World;

/**
 * Seek food when hungry. Priority scales with hunger severity.
 * If no food tile exists, haul food from a FOOD_GROWER to the stockpile then eat.
 */
public class ThinkNode_EatFood extends ThinkNode {
    private static final float MOVE_SPEED = 2.2f;
    private static final float STUCK_TIMEOUT_SECONDS = 6f;

    @Override
    public float getPriority(Entity entity, ECSWorld ecsWorld, World world) {
        NeedsComponent needs = entity.get(NeedsComponent.class);
        if (needs == null) return 0f;
        AIComponent ai = entity.get(AIComponent.class);
        InventoryComponent inv = entity.get(InventoryComponent.class);
        // If we are mid-delivery of food we picked up, keep this node running
        // at priority 90 — above URGENTLY_HUNGRY (80) but below STARVING (100).
        if (ai != null && ai.taskType == TaskType.HAULING
                && inv != null && inv.carriedItem != null
                && inv.carriedItem.getType() == ItemType.FOOD) {
            return 90f;
        }
        return switch (needs.getHungerCategory()) {
            case STARVING        -> 100f;
            case URGENTLY_HUNGRY ->  80f;
            case HUNGRY          ->  50f;
            case FED             ->   0f;
        };
    }

    @Override
    public boolean execute(Entity entity, ECSWorld ecsWorld, World world, float delta) {
        NeedsComponent needs = entity.get(NeedsComponent.class);
        AIComponent ai = entity.get(AIComponent.class);
        PositionComponent pos = entity.get(PositionComponent.class);
        InventoryComponent inv = entity.get(InventoryComponent.class);
        if (needs == null || ai == null || pos == null) return false;

        // Step 1: Timeout check for stuck navigation states
        if (ai.taskType == TaskType.MOVE_TO_FOOD || ai.taskType == TaskType.MOVE_TO_FOOD_GROWER) {
            ai.stuckTimer += delta;
            if (ai.stuckTimer > STUCK_TIMEOUT_SECONDS) {
                ai.clearTask();
                ai.stuckTimer = 0f;
                return false;
            }
        }

        Tile foodTile = world.findNearestFoodTile(pos.x, pos.y);
        if (foodTile != null) {
            // Step 2: Walk to food tile and eat
            // Reset timer if target tile has changed
            if (ai.taskType != TaskType.MOVE_TO_FOOD
                    || ai.targetX != foodTile.getX()
                    || ai.targetY != foodTile.getY()) {
                ai.setTask(TaskType.MOVE_TO_FOOD, foodTile.getX(), foodTile.getY());
                ai.stuckTimer = 0f;
            }
            ai.moveTowardTarget(pos, delta, MOVE_SPEED, world);
            if (ai.isAtTarget(pos.x, pos.y)) {
                Item food = foodTile.takeFirstItem(ItemType.FOOD);
                if (food != null) needs.eat();
                ai.clearTask();
                ai.stuckTimer = 0f;
            }
            return true;
        }

        // No food on ground — try to haul from a FOOD_GROWER
        if (inv != null && inv.carriedItem != null && inv.carriedItem.getType() == ItemType.FOOD) {
            // Already carrying food: deliver to stockpile then eat
            Tile stockpile = world.getStockpileTile();
            if (stockpile == null) {
                inv.carriedItem = null;
                ai.clearTask();
                ai.stuckTimer = 0f;
                return false;
            }
            ai.setTask(TaskType.HAULING, stockpile.getX(), stockpile.getY());
            ai.moveTowardTarget(pos, delta, MOVE_SPEED, world);
            if (ai.isAtTarget(pos.x, pos.y)) {
                stockpile.addItem(inv.carriedItem);
                inv.carriedItem = null;
                Item food = stockpile.takeFirstItem(ItemType.FOOD);
                if (food != null) needs.eat();
                ai.clearTask();
                ai.stuckTimer = 0f;
            }
            return true;
        }

        // Step 3: Find the nearest FOOD_GROWER building with output to pick up
        Entity nearest = null;
        float nearestDist = Float.MAX_VALUE;
        for (Entity bldg : ecsWorld.getEntitiesWith(BuildingComponent.class, PositionComponent.class)) {
            BuildingComponent bc = bldg.get(BuildingComponent.class);
            if (!"FOOD_GROWER".equalsIgnoreCase(bc.buildingType) && !"FOODGROWER".equalsIgnoreCase(bc.buildingType)) continue;
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
        if (nearest != null) {
            BuildingComponent bc = nearest.get(BuildingComponent.class);
            PositionComponent bp = nearest.get(PositionComponent.class);
            if (ai.taskType != TaskType.MOVE_TO_FOOD_GROWER) {
                ai.setTask(TaskType.MOVE_TO_FOOD_GROWER, (int) bp.x, (int) bp.y);
                ai.stuckTimer = 0f;
            }
            ai.moveTowardTarget(pos, delta, MOVE_SPEED, world);
            if (ai.isAtTarget(pos.x, pos.y)) {
                Item output = bc.takeOutput();
                if (output != null && inv != null) {
                    inv.carriedItem = output;
                    Tile stockpile = world.getStockpileTile();
                    if (stockpile != null) {
                        ai.setTask(TaskType.HAULING, stockpile.getX(), stockpile.getY());
                    } else {
                        ai.clearTask();
                    }
                } else {
                    ai.clearTask();
                }
                ai.stuckTimer = 0f;
            }
            return true;
        }

        // Step 4: Nothing found — fall through to wander/idle
        return false;
    }
}

