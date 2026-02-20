package com.haraldsson.syntropy.ecs.components;

import com.haraldsson.syntropy.ecs.Component;

public class IdentityComponent implements Component {
    public String name;
    public int age;

    public IdentityComponent(String name, int age) {
        this.name = name;
        this.age = age;
    }
}

