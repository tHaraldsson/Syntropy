package com.haraldsson.syntropy.systems;

import com.haraldsson.syntropy.core.EventType;
import com.haraldsson.syntropy.core.GameEvents;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 5-era tech tree with prerequisite enforcement.
 * Research progresses passively over time once started.
 * Unlocked techs enable new buildings or mechanics.
 */
public class ResearchSystem {
    private final List<Technology> techTree = new ArrayList<>();
    private Technology currentResearch;
    private final GameEvents events;

    public ResearchSystem(GameEvents events) {
        this.events = events;
        buildTechTree();
    }

    /** Legacy no-arg constructor for save/load compatibility (no event firing). */
    public ResearchSystem() {
        this(null);
    }

    private static List<String> prereqs(String... ids) {
        return List.of(ids);
    }

    private void buildTechTree() {
        // Era 1 — Survival
        techTree.add(new Technology("basic_farming",   "Basic Farming",   "Unlocks food grower buildings",          20f, 1, prereqs()));
        techTree.add(new Technology("basic_mining",    "Basic Mining",    "Unlocks miner buildings",                20f, 1, prereqs()));
        techTree.add(new Technology("basic_storage",   "Basic Storage",   "Unlocks stockpile designation",          20f, 1, prereqs()));

        // Era 2 — Automation
        techTree.add(new Technology("fast_mining",         "Fast Mining",         "Miners produce stone 50% faster",             30f, 2, prereqs("basic_mining")));
        techTree.add(new Technology("advanced_agriculture","Advanced Agriculture","Unlocks a second food grower",                 45f, 2, prereqs("basic_farming")));
        techTree.add(new Technology("blueprints",          "Blueprints",          "Colonists auto-build from ghost placements",  40f, 2, prereqs("basic_storage")));
        techTree.add(new Technology("basic_bots",          "Basic Bots",          "Unlocks worker bots for hauling",             60f, 2, prereqs("blueprints")));
        techTree.add(new Technology("early_medicine",      "Early Medicine",      "Colonists heal faster; unlocks medic role",   35f, 2, prereqs("basic_farming")));

        // Era 3 — Industrial
        techTree.add(new Technology("smelter",              "Smelting",             "Unlocks the smelter building",                    60f, 3, prereqs("fast_mining")));
        techTree.add(new Technology("conveyor",             "Conveyor Belts",       "Unlocks conveyor belt automation",                90f, 3, prereqs("basic_bots")));
        techTree.add(new Technology("androids",             "Androids",             "Unlocks android colonists (high pollution cost)",120f, 3, prereqs("basic_bots", "smelter")));
        techTree.add(new Technology("eco_variants",         "Eco-Friendly Machines","Unlocks low-pollution building variants",          80f, 3, prereqs("smelter")));
        techTree.add(new Technology("pollution_management", "Pollution Management", "Unlocks scrubbers and pollution reduction tech",  90f, 3, prereqs("eco_variants")));
        techTree.add(new Technology("diplomacy",            "Diplomacy",            "Unlocks faction diplomacy actions",               70f, 3, prereqs("advanced_agriculture")));

        // Era 4 — Space
        techTree.add(new Technology("rocketry",        "Rocketry",               "Unlocks rocket construction",                    150f, 4, prereqs("smelter", "androids")));
        techTree.add(new Technology("off_world_comms", "Off-World Communications","Enables contact with remote colonies",            120f, 4, prereqs("rocketry")));
        techTree.add(new Technology("terraforming_probe","Terraforming Probe",   "Build a probe to prepare new worlds",            180f, 4, prereqs("off_world_comms")));
        techTree.add(new Technology("multi_colony",    "Multi-Colony Management","Manage multiple colonies simultaneously",         120f, 4, prereqs("off_world_comms")));

        // Era 5 — Legacy
        techTree.add(new Technology("advanced_terraforming",  "Advanced Terraforming",    "Full planetary ecosystem modification",      240f, 5, prereqs("terraforming_probe")));
        techTree.add(new Technology("colony_network",         "Inter-Colony Networks",    "Real-time resource sharing across colonies", 200f, 5, prereqs("multi_colony")));
        techTree.add(new Technology("planetary_stewardship",  "Planetary Stewardship",    "Reverse global pollution; restore biomes",   300f, 5, prereqs("advanced_terraforming", "colony_network")));
        techTree.add(new Technology("new_world_colonization", "New World Colonization",   "Establish a self-sustaining colony on a new planet", 360f, 5, prereqs("planetary_stewardship")));
    }

    public void update(float delta) {
        if (currentResearch != null && !currentResearch.isUnlocked()) {
            currentResearch.addProgress(delta);
            if (currentResearch.isUnlocked()) {
                String techName = currentResearch.getName();
                if (events != null) {
                    events.fire(EventType.RESEARCH_COMPLETED, techName);
                }
            }
        }
    }

    /**
     * Start researching a specific tech by ID.
     * Does nothing if already researching, already completed, or prerequisites unmet.
     */
    public void startResearch(String techId) {
        if (currentResearch != null && !currentResearch.isUnlocked()) {
            return; // already in progress
        }
        for (Technology tech : techTree) {
            if (tech.getId().equals(techId) && !tech.isUnlocked() && prerequisitesMet(tech)) {
                currentResearch = tech;
                return;
            }
        }
    }

    /** Start researching the next available tech in tree order (era-ascending). */
    public void startNextResearch() {
        if (currentResearch != null && !currentResearch.isUnlocked()) {
            return; // already researching
        }
        for (Technology tech : techTree) {
            if (!tech.isUnlocked() && prerequisitesMet(tech)) {
                currentResearch = tech;
                return;
            }
        }
    }

    /** Returns true if all prerequisites for the given tech are completed. */
    public boolean prerequisitesMet(Technology tech) {
        for (String prereqId : tech.getPrerequisites()) {
            if (!isCompleted(prereqId)) return false;
        }
        return true;
    }

    public boolean isCompleted(String techId) {
        for (Technology tech : techTree) {
            if (tech.getId().equals(techId) && tech.isUnlocked()) {
                return true;
            }
        }
        return false;
    }

    /** @deprecated Use {@link #isCompleted(String)} instead. */
    @Deprecated
    public boolean isTechUnlocked(String techId) {
        return isCompleted(techId);
    }

    public Technology getCurrentResearch() {
        return currentResearch;
    }

    public List<Technology> getTechTree() {
        return Collections.unmodifiableList(techTree);
    }
}

