package com.haraldsson.syntropy.systems;

import com.haraldsson.syntropy.entities.Colonist;
import com.haraldsson.syntropy.entities.FoodGrower;
import com.haraldsson.syntropy.entities.Item;
import com.haraldsson.syntropy.entities.ItemType;
import com.haraldsson.syntropy.entities.Miner;
import com.haraldsson.syntropy.entities.TaskType;
import com.haraldsson.syntropy.world.Tile;
import com.haraldsson.syntropy.world.World;

public class TaskSystem {
    private static final float MOVE_SPEED = 2.2f;
    private static final float REST_DURATION = 3f;

    public void update(World world, float delta) {
        for (Colonist colonist : world.getColonists()) {
            if (colonist.isDead() || colonist.isAiDisabled()) {
                continue;
            }

            // Priority 1: Eat if hungry
            if (handleHunger(world, colonist, delta)) {
                continue;
            }

            // Priority 2: Rest if tired
            if (handleRest(colonist, delta)) {
                continue;
            }

            // Priority 3: Haul resources
            if (handleHauling(world, colonist, delta)) {
                continue;
            }

            // Priority 4: Wander
            handleWander(world, colonist, delta);
        }
    }

    private boolean handleHunger(World world, Colonist colonist, float delta) {
        if (!colonist.isHungry()) {
            return false;
        }

        Tile foodTile = world.findNearestFoodTile(colonist);
        if (foodTile == null) {
            colonist.clearTask();
            return true;
        }

        colonist.setTask(TaskType.MOVE_TO_FOOD, foodTile.getX(), foodTile.getY());
        colonist.moveTowardTarget(delta, MOVE_SPEED);
        if (colonist.isAtTarget()) {
            Item food = foodTile.takeFirstItem(ItemType.FOOD);
            if (food != null) {
                colonist.eat();
            }
            colonist.clearTask();
        }
        return true;
    }

    private boolean handleRest(Colonist colonist, float delta) {
        if (!colonist.isTired() && colonist.getTaskType() != TaskType.RESTING) {
            return false;
        }

        if (colonist.getTaskType() != TaskType.RESTING) {
            colonist.setTask(TaskType.RESTING, (int) colonist.getX(), (int) colonist.getY());
            colonist.resetWanderCooldown(REST_DURATION);
        }

        if (colonist.shouldPickNewWanderTarget(delta)) {
            colonist.rest();
            colonist.clearTask();
            return false;
        }
        return true;
    }

    private boolean handleHauling(World world, Colonist colonist, float delta) {
        if (colonist.getCarriedItem() != null) {
            Tile stockpile = world.getStockpileTile();
            if (stockpile == null) {
                return false;
            }
            colonist.setTask(TaskType.MOVE_TO_STOCKPILE, stockpile.getX(), stockpile.getY());
            colonist.moveTowardTarget(delta, MOVE_SPEED);
            if (colonist.isAtTarget()) {
                stockpile.addItem(colonist.getCarriedItem());
                colonist.setCarriedItem(null);
                colonist.clearTask();
            }
            return true;
        }

        // Find any miner with output
        for (Miner miner : world.getMiners()) {
            if (!miner.hasOutput()) {
                continue;
            }
            colonist.setTask(TaskType.MOVE_TO_MINER, miner.getX(), miner.getY());
            colonist.moveTowardTarget(delta, MOVE_SPEED);
            if (colonist.isAtTarget()) {
                Item output = miner.takeOutput();
                if (output != null) {
                    colonist.setCarriedItem(output);
                }
                colonist.clearTask();
            }
            return true;
        }

        // Find any food grower with output
        for (FoodGrower grower : world.getFoodGrowers()) {
            if (!grower.hasOutput()) {
                continue;
            }
            colonist.setTask(TaskType.MOVE_TO_FOOD_GROWER, grower.getX(), grower.getY());
            colonist.moveTowardTarget(delta, MOVE_SPEED);
            if (colonist.isAtTarget()) {
                Item output = grower.takeOutput();
                if (output != null) {
                    colonist.setCarriedItem(output);
                }
                colonist.clearTask();
            }
            return true;
        }

        return false;
    }

    private void handleWander(World world, Colonist colonist, float delta) {
        if (colonist.shouldPickNewWanderTarget(delta) || colonist.getTaskType() == TaskType.IDLE) {
            int targetX = (int) (Math.random() * world.getWidth());
            int targetY = (int) (Math.random() * world.getHeight());
            colonist.setTask(TaskType.WANDER, targetX, targetY);
            colonist.resetWanderCooldown(2f + (float) Math.random() * 2f);
        }

        colonist.moveTowardTarget(delta, MOVE_SPEED * 0.6f);
        if (colonist.isAtTarget()) {
            colonist.clearTask();
        }
    }
}

