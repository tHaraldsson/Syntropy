package com.haraldsson.syntropy.systems.mood;

import com.haraldsson.syntropy.ecs.Entity;
import com.haraldsson.syntropy.ecs.components.NeedsComponent;

public class SleepThoughtWorker implements ThoughtWorker {
    @Override
    public float getMoodOffset(Entity entity) {
        NeedsComponent needs = entity.get(NeedsComponent.class);
        if (needs == null) return 0f;
        return switch (needs.getEnergyCategory()) {
            case RESTED -> 0f;
            case TIRED -> -3f;
            case EXHAUSTED -> -12f;
            case COLLAPSED -> -30f;
        };
    }
}

