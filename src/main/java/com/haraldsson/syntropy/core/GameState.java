package com.haraldsson.syntropy.core;

import com.haraldsson.syntropy.ecs.ECSWorld;
import com.haraldsson.syntropy.ecs.systems.*;
import com.haraldsson.syntropy.systems.ResearchSystem;
import com.haraldsson.syntropy.world.World;

/**
 * Pattern 5 — All game state in one root object.
 * No statics, no singletons. Passed explicitly to all systems.
 * Foundation for save/load and future multiplayer.
 */
public class GameState {
    public World world;
    public ECSWorld ecsWorld;
    public GameEvents events;
    public ResearchSystem research;
    public PollutionSystem pollution;

    // Dynasty tracking
    public int leaderGeneration = 1;
    public String dynastyName = "Kael Dynasty";

    public GameState() {
        this.events = new GameEvents();
    }

    public GameState(World world, ECSWorld ecsWorld) {
        this.world = world;
        this.ecsWorld = ecsWorld;
        this.events = new GameEvents();
        this.research = new ResearchSystem(this.events);
        this.pollution = new PollutionSystem();
    }
}

