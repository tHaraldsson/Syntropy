package com.haraldsson.syntropy.entities;

/**
 * Pattern 1 — Tiered hunger categories.
 * Drives AI priority, speed, efficiency, and mood offsets.
 */
public enum HungerCategory {
    FED, HUNGRY, URGENTLY_HUNGRY, STARVING;

    public static HungerCategory fromLevel(float level) {
        if (level > 0.6f) return FED;
        if (level > 0.3f) return HUNGRY;
        if (level > 0.1f) return URGENTLY_HUNGRY;
        return STARVING;
    }
}

