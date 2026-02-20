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

            colonistTree.execute(e, ecsWorld, world, delta);
        }
    }
}
