package com.haraldsson.syntropy.ecs.components;

import com.haraldsson.syntropy.ecs.Component;

/**
 * Marks an entity as the player-controlled leader.
 * Only ONE entity should have this at a time.
 * Stats: charisma, engineering, science, combat — each affects colony efficiency.
 */
public class LeaderComponent implements Component {
    public float charisma = 5f;
    public float engineering = 5f;
    public float science = 4f;
    public float combat = 3f;

    public LeaderComponent() {}

    /** Colony efficiency bonus from leader stats */
    public float getEfficiencyBonus() {
        return (charisma + engineering) / 40f;
    }

    /** Research speed bonus from science stat */
    public float getResearchBonus() {
        return science / 10f;
    }

    /** Diplomacy modifier from charisma */
    public float getDiplomacyModifier() {
        return charisma / 10f;
    }

    /** Combat effectiveness modifier */
    public float getCombatModifier() {
        return combat / 10f;
    }
}
