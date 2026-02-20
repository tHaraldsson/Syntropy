package com.haraldsson.syntropy.systems.mood;

import com.haraldsson.syntropy.ecs.Entity;
import com.haraldsson.syntropy.ecs.components.SleepQualityComponent;

public class SleepThoughtWorker implements ThoughtWorker {
    @Override
    public float getMoodOffset(Entity entity) {
        SleepQualityComponent sq = entity.get(SleepQualityComponent.class);
        if (sq == null) return 0f;
        return switch (sq.lastSleepQuality) {
            case IN_BED    ->   0f;
            case ON_GROUND -> -10f;
            case NONE      -> -25f;
        };
    }
}

