package com.haraldsson.syntropy.ai;

import com.haraldsson.syntropy.ecs.ECSWorld;
import com.haraldsson.syntropy.ecs.Entity;
import com.haraldsson.syntropy.world.World;

/**
 * Pattern 3 — Think Tree node.
 * Each node returns a priority float (0 = cannot run).
 * Highest valid priority wins each tick.
 */
public abstract class ThinkNode {

    /**
     * Return the priority of this node for the given entity.
     * Return 0 or negative if this node should not run.
     */
    public abstract float getPriority(Entity entity, ECSWorld ecsWorld, World world);

    /**
     * Execute this node's behavior. Returns true if the node handled the entity.
     */
    public abstract boolean execute(Entity entity, ECSWorld ecsWorld, World world, float delta);
}
