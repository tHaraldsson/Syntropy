package com.haraldsson.syntropy.entities;

/** Colonist job roles — determines what Think Tree nodes are available. */
public enum ColonistRole {
    IDLE,       // Unassigned
    FARMER,     // Operates food growers, plants crops
    MINER,      // Operates miners, quarries
    HAULER,     // Moves items between buildings/stockpile
    BUILDER,    // Constructs buildings from blueprints
    RESEARCHER, // Generates research points
    MEDIC,      // Heals injured colonists
    SOLDIER     // Guards, fights threats
}

