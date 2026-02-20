package com.haraldsson.syntropy.systems.mood;

import com.haraldsson.syntropy.ecs.Entity;
import com.haraldsson.syntropy.ecs.components.NeedsComponent;

public class HealthThoughtWorker implements ThoughtWorker {
    @Override
    public float getMoodOffset(Entity entity) {
        NeedsComponent needs = entity.get(NeedsComponent.class);
        if (needs == null) return 0f;
        if (needs.health > 0.8f) return 0f;
        if (needs.health > 0.5f) return -5f;
        if (needs.health > 0.2f) return -15f;
        return -30f;
    }
}

