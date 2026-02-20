package com.haraldsson.syntropy;

import com.haraldsson.syntropy.core.EventType;
import com.haraldsson.syntropy.core.GameEvents;
import com.haraldsson.syntropy.ecs.ECSWorld;
import com.haraldsson.syntropy.ecs.Entity;
import com.haraldsson.syntropy.ecs.components.BuildingComponent;
import com.haraldsson.syntropy.ecs.components.PositionComponent;
import com.haraldsson.syntropy.ecs.systems.BuildingProductionSystem;
import com.haraldsson.syntropy.entities.ItemType;
import com.haraldsson.syntropy.world.TerrainType;
import com.haraldsson.syntropy.world.Tile;
import com.haraldsson.syntropy.world.World;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BuildingProductionSystemTest {

    private static World buildMinimalWorld() {
        Tile[][] tiles = new Tile[1][1];
        tiles[0][0] = new Tile(0, 0, TerrainType.GRASS);
        return new World(1, 1, tiles);
    }

    @Test
    void firesResourceProducedEvent() {
        // arrange
        ECSWorld ecsWorld = new ECSWorld();
        World world = buildMinimalWorld();
        GameEvents events = new GameEvents();

        Entity miner = ecsWorld.createEntity();
        BuildingComponent bc = new BuildingComponent("MINER", 1f, 5, ItemType.STONE);
        bc.built = true;
        miner.add(bc);
        miner.add(new PositionComponent(0, 0));

        BuildingProductionSystem system = new BuildingProductionSystem();
        system.setEvents(events);

        List<Object> fired = new ArrayList<>();
        events.on(EventType.RESOURCE_PRODUCED, fired::add);

        // act — advance exactly past the production interval
        system.update(ecsWorld, world, 1.1f);

        // assert
        assertFalse(fired.isEmpty(), "Expected RESOURCE_PRODUCED event to be fired");
        assertEquals(ItemType.STONE.name(), fired.get(0));
    }

    @Test
    void doesNotFireBeforeProductionInterval() {
        ECSWorld ecsWorld = new ECSWorld();
        World world = buildMinimalWorld();
        GameEvents events = new GameEvents();

        Entity miner = ecsWorld.createEntity();
        BuildingComponent bc = new BuildingComponent("MINER", 5f, 5, ItemType.STONE);
        bc.built = true;
        miner.add(bc);
        miner.add(new PositionComponent(0, 0));

        BuildingProductionSystem system = new BuildingProductionSystem();
        system.setEvents(events);

        List<Object> fired = new ArrayList<>();
        events.on(EventType.RESOURCE_PRODUCED, fired::add);

        system.update(ecsWorld, world, 0.5f);

        assertTrue(fired.isEmpty(), "Expected no RESOURCE_PRODUCED event before interval");
    }
}
