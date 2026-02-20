package com.haraldsson.syntropy.ecs.components;

import com.haraldsson.syntropy.ecs.Component;

public class PositionComponent implements Component {
    public float x;
    public float y;

    public PositionComponent() {}

    public PositionComponent(float x, float y) {
        this.x = x;
        this.y = y;
    }
}

