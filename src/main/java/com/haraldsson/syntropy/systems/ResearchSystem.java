package com.haraldsson.syntropy.systems;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Simple tech tree. Press R to start researching the next available tech.
 * Research progresses passively over time once started.
 * Unlocked techs enable new buildings or mechanics.
 */
public class ResearchSystem {
    private final List<Technology> techTree = new ArrayList<>();
    private Technology currentResearch;

    public ResearchSystem() {
        // Define the tech tree in order
        techTree.add(new Technology("fast_mining", "Fast Mining",
                "Miners produce stone 50% faster", 30f));
        techTree.add(new Technology("food_grower_2", "Advanced Agriculture",
                "Unlocks a second food grower", 45f));
        techTree.add(new Technology("smelter", "Smelting",
                "Unlocks the Smelter building (future)", 60f));
        techTree.add(new Technology("conveyor", "Conveyor Belts",
                "Unlocks conveyor belt automation (future)", 90f));
    }

    public void update(float delta) {
        if (currentResearch != null && !currentResearch.isUnlocked()) {
            currentResearch.addProgress(delta);
            if (currentResearch.isUnlocked()) {
                // Research completed — currentResearch stays set so UI can show it
            }
        }
    }

    /** Start researching the next un-unlocked tech, or do nothing if all done. */
    public void startNextResearch() {
        if (currentResearch != null && !currentResearch.isUnlocked()) {
            return; // already researching
        }
        for (Technology tech : techTree) {
            if (!tech.isUnlocked()) {
                currentResearch = tech;
                return;
            }
        }
    }

    public boolean isTechUnlocked(String techId) {
        for (Technology tech : techTree) {
            if (tech.getId().equals(techId) && tech.isUnlocked()) {
                return true;
            }
        }
        return false;
    }

    public Technology getCurrentResearch() {
        return currentResearch;
    }

    public List<Technology> getTechTree() {
        return Collections.unmodifiableList(techTree);
    }
}

