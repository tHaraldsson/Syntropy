package com.haraldsson.syntropy.ecs;

import com.haraldsson.syntropy.world.World;

/**
 * Base class for all ECS systems. Systems contain logic, no data.
 */
public abstract class GameSystem {
    public abstract void update(ECSWorld ecsWorld, World world, float delta);
}

