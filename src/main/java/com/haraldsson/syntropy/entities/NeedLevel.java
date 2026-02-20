package com.haraldsson.syntropy.entities;

/** RimWorld-inspired tiered need levels. */
public enum NeedLevel {
    FULL(1.0f, 0.75f),
    SATISFIED(0.75f, 0.5f),
    LOW(0.5f, 0.25f),
    URGENT(0.25f, 0.05f),
    CRITICAL(0.05f, 0.0f);

    public final float upperBound;
    public final float lowerBound;

    NeedLevel(float upper, float lower) {
        this.upperBound = upper;
        this.lowerBound = lower;
    }

    public static NeedLevel fromRatio(float ratio) {
        if (ratio >= 0.75f) return FULL;
        if (ratio >= 0.5f) return SATISFIED;
        if (ratio >= 0.25f) return LOW;
        if (ratio >= 0.05f) return URGENT;
        return CRITICAL;
    }

    public boolean isUrgent() { return this == URGENT || this == CRITICAL; }
    public boolean isCritical() { return this == CRITICAL; }
}

