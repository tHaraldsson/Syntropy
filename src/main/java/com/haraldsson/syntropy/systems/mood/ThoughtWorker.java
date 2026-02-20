package com.haraldsson.syntropy.systems.mood;

import com.haraldsson.syntropy.ecs.Entity;

/**
 * Pattern 2 — ThoughtWorker interface.
 * Each worker observes entity state and returns a mood offset.
 * Needs NEVER touch mood directly.
 */
public interface ThoughtWorker {
    float getMoodOffset(Entity entity);
}

