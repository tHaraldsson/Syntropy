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

import java.util.List;

/**
 * Pattern 6 — Perform assigned job based on WorkSettingsComponent priorities.
 * Priority 50 (job-level). Checks the colonist's highest-priority enabled job
 * and tries to execute it.
 */
public class ThinkNode_DoAssignedJob extends ThinkNode {
    private static final float MOVE_SPEED = 2.0f;

    @Override
    public float getPriority(Entity entity, ECSWorld ecsWorld, World world) {
        WorkSettingsComponent ws = entity.get(WorkSettingsComponent.class);
        if (ws == null) return 0f;
        List<ColonistRole> jobs = ws.getActiveJobsSorted();
        if (jobs.isEmpty()) return 0f;

        // Check if there's actually work to do for any active job
        for (ColonistRole role : jobs) {
            if (hasWorkAvailable(role, entity, ecsWorld, world)) {
                return 50f;
            }
        }
        return 0f;
    }

    @Override
    public boolean execute(Entity entity, ECSWorld ecsWorld, World world, float delta) {
        WorkSettingsComponent ws = entity.get(WorkSettingsComponent.class);
        if (ws == null) return false;

        List<ColonistRole> jobs = ws.getActiveJobsSorted();
        for (ColonistRole role : jobs) {
            if (executeJob(role, entity, ecsWorld, world, delta)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasWorkAvailable(ColonistRole role, Entity entity, ECSWorld ecsWorld, World world) {
        return switch (role) {
            case HAULER -> {
                InventoryComponent inv = entity.get(InventoryComponent.class);
                if (inv != null && inv.carriedItem != null) yield true;
                for (Entity bldg : ecsWorld.getEntitiesWith(BuildingComponent.class)) {
                    if (bldg.get(BuildingComponent.class).hasOutput()) yield true;
                }
                yield false;
            }
            case MINER -> {
                for (Entity bldg : ecsWorld.getEntitiesWith(BuildingComponent.class)) {
                    BuildingComponent bc = bldg.get(BuildingComponent.class);
                    if ("MINER".equals(bc.buildingType) && bc.hasOutput()) yield true;
                }
                yield false;
            }
            case FARMER -> {
                for (Entity bldg : ecsWorld.getEntitiesWith(BuildingComponent.class)) {
                    BuildingComponent bc = bldg.get(BuildingComponent.class);
                    if ("FOOD_GROWER".equals(bc.buildingType) && bc.hasOutput()) yield true;
                }
                yield false;
            }
            default -> false;
        };
    }

    private boolean executeJob(ColonistRole role, Entity entity, ECSWorld ecsWorld, World world, float delta) {
        return switch (role) {
            case HAULER -> executeHaul(entity, ecsWorld, world, delta);
            case MINER -> executeCollectFrom(entity, ecsWorld, world, delta, "MINER");
            case FARMER -> executeCollectFrom(entity, ecsWorld, world, delta, "FOOD_GROWER");
            default -> false;
        };
    }

    private boolean executeHaul(Entity entity, ECSWorld ecsWorld, World world, float delta) {
        AIComponent ai = entity.get(AIComponent.class);
        PositionComponent pos = entity.get(PositionComponent.class);
        InventoryComponent inv = entity.get(InventoryComponent.class);
        if (ai == null || pos == null || inv == null) return false;

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

        for (Entity bldg : ecsWorld.getEntitiesWith(BuildingComponent.class, PositionComponent.class)) {
            BuildingComponent bc = bldg.get(BuildingComponent.class);
            if (!bc.hasOutput()) continue;
            PositionComponent bp = bldg.get(PositionComponent.class);
            ai.setTask(TaskType.MOVE_TO_MINER, (int) bp.x, (int) bp.y);
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

    private boolean executeCollectFrom(Entity entity, ECSWorld ecsWorld, World world, float delta, String buildingType) {
        AIComponent ai = entity.get(AIComponent.class);
        PositionComponent pos = entity.get(PositionComponent.class);
        InventoryComponent inv = entity.get(InventoryComponent.class);
        if (ai == null || pos == null || inv == null) return false;

        // If carrying, deliver to stockpile
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

        for (Entity bldg : ecsWorld.getEntitiesWith(BuildingComponent.class, PositionComponent.class)) {
            BuildingComponent bc = bldg.get(BuildingComponent.class);
            if (!buildingType.equals(bc.buildingType) || !bc.hasOutput()) continue;
            PositionComponent bp = bldg.get(PositionComponent.class);
            TaskType task = "MINER".equals(buildingType) ? TaskType.MOVE_TO_MINER : TaskType.MOVE_TO_FOOD_GROWER;
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


