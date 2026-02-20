package com.haraldsson.syntropy.core;

/**
 * Pattern 4 — Event types for the decoupled event bus.
 */
public enum EventType {
    RESOURCE_PRODUCED,
    RESOURCE_CONSUMED,
    COLONIST_DIED,
    COLONIST_BORN,
    POLLUTION_INCREASED,
    BUILDING_PLACED,
    BUILDING_COMPLETED,
    RESEARCH_COMPLETED,
    LEADER_DIED,
    LEADER_SUCCEEDED,
    FACTION_RELATION_CHANGED,
    COLONIST_RECRUITED,
    BLUEPRINT_PLACED,
    BLUEPRINT_COMPLETED
}

