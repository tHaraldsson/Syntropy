package com.haraldsson.syntropy.ecs;

import java.util.HashMap;
import java.util.Map;

/**
 * An entity is just an ID with a bag of components.
 */
public class Entity {
    private static int nextId = 0;

    private final int id;
    private final Map<Class<? extends Component>, Component> components = new HashMap<>();

    public Entity() {
        this.id = nextId++;
    }

    public int getId() {
        return id;
    }

    public <T extends Component> Entity add(T component) {
        components.put(component.getClass(), component);
        return this;
    }

    @SuppressWarnings("unchecked")
    public <T extends Component> T get(Class<T> type) {
        return (T) components.get(type);
    }

    public boolean has(Class<? extends Component> type) {
        return components.containsKey(type);
    }

    public void remove(Class<? extends Component> type) {
        components.remove(type);
    }

    public static void resetIdCounter() {
        nextId = 0;
    }
}

