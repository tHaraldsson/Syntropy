package com.haraldsson.syntropy.ecs.components;

import com.haraldsson.syntropy.ecs.Component;

public class HealthComponent implements Component {
    public boolean dead;
    public boolean deathEventFired; // prevent firing death event more than once
}

