package com.haraldsson.syntropy.ecs.components;

import com.haraldsson.syntropy.ecs.Component;

import java.util.HashMap;
import java.util.Map;

public class SkillsComponent implements Component {
    public final Map<String, Integer> skills = new HashMap<>();

    public SkillsComponent() {
        skills.put("mining", 1);
        skills.put("hauling", 1);
    }

    public int getSkill(String skill) {
        return skills.getOrDefault(skill, 0);
    }
}

