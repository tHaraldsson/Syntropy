package com.haraldsson.syntropy;

import com.haraldsson.syntropy.ecs.ECSWorld;
import com.haraldsson.syntropy.ecs.Entity;
import com.haraldsson.syntropy.ecs.components.HealthComponent;
import com.haraldsson.syntropy.ecs.components.InventoryComponent;
import com.haraldsson.syntropy.ecs.components.NeedsComponent;
import com.haraldsson.syntropy.ecs.components.PositionComponent;
import com.haraldsson.syntropy.ecs.systems.NeedsSystem;
import com.haraldsson.syntropy.entities.Item;
import com.haraldsson.syntropy.entities.ItemType;
import com.haraldsson.syntropy.world.TerrainType;
import com.haraldsson.syntropy.world.Tile;
import com.haraldsson.syntropy.world.World;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ColonistDeathTest {

    private static World buildWorldWithTile(int tx, int ty) {
        int size = Math.max(tx + 1, ty + 1);
        Tile[][] tiles = new Tile[size][size];
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                tiles[x][y] = new Tile(x, y, TerrainType.GRASS);
            }
        }
        return new World(size, size, tiles);
    }

    @Test
    void dropsCarriedItemToGroundTileOnDeath() {
        // arrange
        ECSWorld ecsWorld = new ECSWorld();
        World world = buildWorldWithTile(3, 3);

        Entity colonist = ecsWorld.createEntity();

        NeedsComponent needs = new NeedsComponent();
        colonist.add(needs);

        HealthComponent health = new HealthComponent();
        health.dead = true; // colonist is already dead
        colonist.add(health);

        PositionComponent pos = new PositionComponent(3f, 3f);
        colonist.add(pos);

        InventoryComponent inv = new InventoryComponent();
        inv.carriedItem = new Item(ItemType.STONE);
        colonist.add(inv);

        NeedsSystem needsSystem = new NeedsSystem();

        // act
        needsSystem.update(ecsWorld, world, 0.1f);

        // assert — item should be on the tile and inventory should be empty
        Tile tile = world.getTile(3, 3);
        assertNotNull(tile);
        assertFalse(tile.getGroundItems().isEmpty(), "Expected item to be dropped to the ground tile");
        assertEquals(ItemType.STONE, tile.getGroundItems().get(0).getType());
        assertNull(inv.carriedItem, "Expected colonist inventory to be cleared after death");
    }

    @Test
    void doesNotDropItemsIfInventoryEmpty() {
        // arrange — dead colonist with no carried item
        ECSWorld ecsWorld = new ECSWorld();
        World world = buildWorldWithTile(2, 2);

        Entity colonist = ecsWorld.createEntity();
        colonist.add(new NeedsComponent());

        HealthComponent health = new HealthComponent();
        health.dead = true;
        colonist.add(health);

        colonist.add(new PositionComponent(2f, 2f));
        colonist.add(new InventoryComponent()); // no carried item

        NeedsSystem needsSystem = new NeedsSystem();
        needsSystem.update(ecsWorld, world, 0.1f);

        Tile tile = world.getTile(2, 2);
        assertTrue(tile.getGroundItems().isEmpty(), "Expected no items dropped when inventory is empty");
    }

    @Test
    void dropsItemOnlyOnce() {
        // arrange — verify the item is dropped exactly once across multiple ticks
        ECSWorld ecsWorld = new ECSWorld();
        World world = buildWorldWithTile(1, 1);

        Entity colonist = ecsWorld.createEntity();
        colonist.add(new NeedsComponent());

        HealthComponent health = new HealthComponent();
        health.dead = true;
        colonist.add(health);

        colonist.add(new PositionComponent(1f, 1f));

        InventoryComponent inv = new InventoryComponent();
        inv.carriedItem = new Item(ItemType.FOOD);
        colonist.add(inv);

        NeedsSystem needsSystem = new NeedsSystem();
        needsSystem.update(ecsWorld, world, 0.1f);
        needsSystem.update(ecsWorld, world, 0.1f);
        needsSystem.update(ecsWorld, world, 0.1f);

        Tile tile = world.getTile(1, 1);
        assertEquals(1, tile.getGroundItems().size(), "Expected exactly one item on the tile");
    }
}
