package com.haraldsson.syntropy.ecs.components;

import com.haraldsson.syntropy.ecs.Component;

/**
 * Mood is calculated each tick by MoodSystem via ThoughtWorkers (Pattern 2).
 * Never written directly by needs — only by MoodSystem.
 */
public class MoodComponent implements Component {
    public float mood = 50f;  // base neutral mood (0–100 scale)

    public MoodComponent() {}

    public boolean isHappy() { return mood >= 60f; }
    public boolean isNeutral() { return mood >= 30f && mood < 60f; }
    public boolean isUnhappy() { return mood < 30f; }
    public boolean isBroken() { return mood < 10f; }
}

