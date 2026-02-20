package com.haraldsson.syntropy.ecs.components;

import com.haraldsson.syntropy.ecs.Component;
import com.haraldsson.syntropy.entities.EnergyCategory;
import com.haraldsson.syntropy.entities.HungerCategory;

/**
 * Colonist needs — raw floats 0.0–1.0 internally, exposed as tiered categories.
 * Mood is NOT stored here — it's calculated by MoodSystem via ThoughtWorkers (Pattern 2).
 */
public class NeedsComponent implements Component {
    public static final float HUNGER_DECAY = 0.02f;   // per second (0–1 scale)
    public static final float ENERGY_DECAY = 0.012f;
    public static final float HEALTH_REGEN = 0.003f;
    public static final float EAT_AMOUNT = 0.4f;
    public static final float REST_AMOUNT = 0.3f;

    // Raw values 0.0–1.0
    public float hunger = 1f;
    public float energy = 1f;
    public float health = 1f;

    // ── Category accessors (Pattern 1) ──

    public HungerCategory getHungerCategory() { return HungerCategory.fromLevel(hunger); }
    public EnergyCategory getEnergyCategory() { return EnergyCategory.fromLevel(energy); }

    // ── Convenience checks ──

    public boolean isHungry() { return getHungerCategory() == HungerCategory.URGENTLY_HUNGRY || getHungerCategory() == HungerCategory.STARVING; }
    public boolean isTired() { return getEnergyCategory() == EnergyCategory.EXHAUSTED || getEnergyCategory() == EnergyCategory.COLLAPSED; }
    public boolean isStarving() { return getHungerCategory() == HungerCategory.STARVING; }
    public boolean isExhausted() { return getEnergyCategory() == EnergyCategory.COLLAPSED; }

    // ── Actions ──

    public void eat() { hunger = Math.min(1f, hunger + EAT_AMOUNT); }
    public void rest() { energy = Math.min(1f, energy + REST_AMOUNT); }
    public void restPartial(float amount) { energy = Math.min(1f, energy + amount); }
    public void heal(float amount) { health = Math.min(1f, health + amount); }
    public void damage(float amount) { health = Math.max(0f, health - amount); }
}
