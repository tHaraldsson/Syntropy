package com.haraldsson.syntropy.ecs.components;

import com.haraldsson.syntropy.ecs.Component;

public class SleepQualityComponent implements Component {
    public enum Quality { NONE, ON_GROUND, IN_BED }
    public Quality lastSleepQuality = Quality.NONE;
}
