package com.haraldsson.syntropy.ecs.components;

import com.haraldsson.syntropy.ecs.Component;

/**
 * Aging component — entities with this age over game time.
 * Used for leader dynasty system and colonist lifecycle.
 */
public class AgingComponent implements Component {
    public float ageYears;           // current age in game-years
    public float maxAge;             // natural death age (randomized per entity)
    public float yearAccumulator;    // accumulates delta to tick years

    /** Game seconds per in-game year. Adjust for pacing. */
    public static final float SECONDS_PER_YEAR = 60f; // 1 minute real = 1 year

    public AgingComponent() {}

    public AgingComponent(float startAge, float maxAge) {
        this.ageYears = startAge;
        this.maxAge = maxAge;
    }

    /** Returns true if a new year just passed */
    public boolean tick(float delta) {
        yearAccumulator += delta;
        if (yearAccumulator >= SECONDS_PER_YEAR) {
            yearAccumulator -= SECONDS_PER_YEAR;
            ageYears += 1f;
            return true;
        }
        return false;
    }

    public boolean isElderly() {
        return ageYears >= maxAge * 0.8f;
    }

    public boolean shouldDieOfOldAge() {
        return ageYears >= maxAge;
    }
}

