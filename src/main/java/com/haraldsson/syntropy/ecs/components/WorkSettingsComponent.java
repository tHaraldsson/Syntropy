package com.haraldsson.syntropy.ecs.components;

import com.haraldsson.syntropy.ecs.Component;
import com.haraldsson.syntropy.entities.ColonistRole;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Pattern 6 — Per-colonist work priority settings.
 * Each job has a priority 0–4 (0 = disabled, 4 = highest).
 * Used by ThinkNode_DoAssignedJob to pick the right task.
 */
public class WorkSettingsComponent implements Component {
    private final Map<ColonistRole, Integer> priorities = new HashMap<>();

    public WorkSettingsComponent() {
        // Default: all jobs at priority 0 (disabled)
        for (ColonistRole role : ColonistRole.values()) {
            priorities.put(role, 0);
        }
    }

    public void setPriority(ColonistRole job, int priority) {
        if (priority < 0 || priority > 4)
            throw new IllegalArgumentException("Priority must be 0–4, got " + priority);
        priorities.put(job, priority);
    }

    public int getPriority(ColonistRole job) {
        return priorities.getOrDefault(job, 0);
    }

    public List<ColonistRole> getActiveJobsSorted() {
        return priorities.entrySet().stream()
                .filter(e -> e.getValue() > 0)
                .sorted(Map.Entry.<ColonistRole, Integer>comparingByValue().reversed())
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    public Map<ColonistRole, Integer> getAllPriorities() {
        return Collections.unmodifiableMap(priorities);
    }
}

