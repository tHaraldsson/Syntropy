package com.haraldsson.syntropy.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Pattern 4 — Event Bus.
 * Systems never call each other directly. All communication goes through events.
 * Instance-based (not static) — lives inside GameState for serialization safety.
 */
public class GameEvents {
    private final Map<EventType, List<Consumer<Object>>> listeners = new HashMap<>();
    private final List<String> eventLog = new ArrayList<>();
    private static final int MAX_LOG = 20;

    public void on(EventType type, Consumer<Object> listener) {
        listeners.computeIfAbsent(type, k -> new ArrayList<>()).add(listener);
    }

    public void fire(EventType type, Object payload) {
        List<Consumer<Object>> handlers = listeners.get(type);
        if (handlers != null) {
            for (Consumer<Object> handler : handlers) {
                handler.accept(payload);
            }
        }
    }

    /** Fire an event and also log a human-readable message. */
    public void fireAndLog(EventType type, Object payload, String message) {
        fire(type, payload);
        log(message);
    }

    public void log(String message) {
        eventLog.add(message);
        if (eventLog.size() > MAX_LOG) eventLog.remove(0);
    }

    public List<String> getEventLog() {
        return eventLog;
    }

    /** Clear all listeners (call on load). */
    public void clearListeners() {
        listeners.clear();
    }
}

