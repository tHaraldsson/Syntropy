package com.haraldsson.syntropy.entities;

/**
 * Pattern 1 — Tiered energy categories.
 * Drives AI priority for rest, speed penalty, mood offsets.
 */
public enum EnergyCategory {
    RESTED, TIRED, EXHAUSTED, COLLAPSED;

    public static EnergyCategory fromLevel(float level) {
        if (level > 0.6f) return RESTED;
        if (level > 0.3f) return TIRED;
        if (level > 0.1f) return EXHAUSTED;
        return COLLAPSED;
    }
}

