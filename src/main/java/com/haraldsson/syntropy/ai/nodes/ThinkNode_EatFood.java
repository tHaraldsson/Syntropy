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
 */
public class ThinkNode_EatFood extends ThinkNode {
    private static final float MOVE_SPEED = 2.2f;

    @Override
    public float getPriority(Entity entity, ECSWorld ecsWorld, World world) {
        NeedsComponent needs = entity.get(NeedsComponent.class);
        if (needs == null) return 0f;
        return switch (needs.getHungerCategory()) {
            case STARVING -> 100f;
            case URGENTLY_HUNGRY -> 70f;
            case HUNGRY -> 20f;
            case FED -> 0f;
        };
    }

    @Override
    public boolean execute(Entity entity, ECSWorld ecsWorld, World world, float delta) {
        NeedsComponent needs = entity.get(NeedsComponent.class);
        AIComponent ai = entity.get(AIComponent.class);
        PositionComponent pos = entity.get(PositionComponent.class);
        if (needs == null || ai == null || pos == null) return false;

        Tile foodTile = world.findNearestFoodTile(pos.x, pos.y);
        if (foodTile == null) return false;

        ai.setTask(TaskType.MOVE_TO_FOOD, foodTile.getX(), foodTile.getY());
        ai.moveTowardTarget(pos, delta, MOVE_SPEED);
        if (ai.isAtTarget(pos.x, pos.y)) {
            Item food = foodTile.takeFirstItem(ItemType.FOOD);
            if (food != null) needs.eat();
            ai.clearTask();
        }
        return true;
    }
}
