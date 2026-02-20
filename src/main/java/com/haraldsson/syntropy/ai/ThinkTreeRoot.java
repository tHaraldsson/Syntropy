package com.haraldsson.syntropy.ai;

import com.haraldsson.syntropy.ecs.ECSWorld;
import com.haraldsson.syntropy.ecs.Entity;
import com.haraldsson.syntropy.world.World;

import java.util.ArrayList;
import java.util.List;

/**
 * Pattern 3 — Root of a colonist's AI brain.
 * Evaluates all children, picks the one with the highest priority, and executes it.
 */
public class ThinkTreeRoot extends ThinkNode {
    private final List<ThinkNode> children = new ArrayList<>();

    public ThinkTreeRoot addChild(ThinkNode node) {
        children.add(node);
        return this;
    }

    @Override
    public float getPriority(Entity entity, ECSWorld ecsWorld, World world) {
        return 1f; // root always valid
    }

    @Override
    public boolean execute(Entity entity, ECSWorld ecsWorld, World world, float delta) {
        ThinkNode best = null;
        float bestPriority = 0f;

        for (ThinkNode child : children) {
            float p = child.getPriority(entity, ecsWorld, world);
            if (p > bestPriority) {
                bestPriority = p;
                best = child;
            }
        }

        if (best != null) {
            return best.execute(entity, ecsWorld, world, delta);
        }
        return false;
    }
}
