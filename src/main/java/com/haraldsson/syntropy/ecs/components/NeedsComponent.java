package com.haraldsson.syntropy.ecs.components;

import com.haraldsson.syntropy.ecs.Component;

public class NeedsComponent implements Component {
    public static final float HUNGER_DECAY = 2f;
    public static final float ENERGY_DECAY = 1.2f;
    public static final float MOOD_DECAY = 0.5f;
    public static final float EAT_AMOUNT = 40f;
    public static final float REST_AMOUNT = 30f;
    public static final float MOOD_BOOST = 15f;

    public float hunger = 100f;
    public float energy = 100f;
    public float mood = 100f;

    public boolean isHungry() { return hunger <= 35f; }
    public boolean isTired() { return energy <= 25f; }

    public void eat() {
        hunger = Math.min(100f, hunger + EAT_AMOUNT);
        mood = Math.min(100f, mood + MOOD_BOOST);
    }

    public void rest() {
        energy = Math.min(100f, energy + REST_AMOUNT);
    }
}

