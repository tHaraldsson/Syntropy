package com.haraldsson.syntropy.systems.mood;

import com.haraldsson.syntropy.ecs.Entity;
import com.haraldsson.syntropy.ecs.components.NeedsComponent;

public class HungerThoughtWorker implements ThoughtWorker {
    @Override
    public float getMoodOffset(Entity entity) {
        NeedsComponent needs = entity.get(NeedsComponent.class);
        if (needs == null) return 0f;
        return switch (needs.getHungerCategory()) {
            case FED -> 0f;
            case HUNGRY -> -5f;
            case URGENTLY_HUNGRY -> -15f;
            case STARVING -> -40f;
        };
    }
}

