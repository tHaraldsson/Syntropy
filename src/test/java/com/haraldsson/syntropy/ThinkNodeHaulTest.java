package com.haraldsson.syntropy;

import com.haraldsson.syntropy.ai.nodes.ThinkNode_Haul;
import com.haraldsson.syntropy.ecs.ECSWorld;
import com.haraldsson.syntropy.ecs.Entity;
import com.haraldsson.syntropy.ecs.components.AIComponent;
import com.haraldsson.syntropy.ecs.components.BuildingComponent;
import com.haraldsson.syntropy.ecs.components.InventoryComponent;
import com.haraldsson.syntropy.ecs.components.PositionComponent;
import com.haraldsson.syntropy.ecs.components.WorkSettingsComponent;
import com.haraldsson.syntropy.entities.ColonistRole;
import com.haraldsson.syntropy.entities.ItemType;
import com.haraldsson.syntropy.world.TerrainType;
import com.haraldsson.syntropy.world.Tile;
import com.haraldsson.syntropy.world.World;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ThinkNodeHaulTest {

    private ECSWorld ecsWorld;
    private World world;
    private ThinkNode_Haul haulNode;

    @BeforeEach
    void setUp() {
        ecsWorld = new ECSWorld();
        Tile[][] tiles = new Tile[5][5];
        for (int x = 0; x < 5; x++) {
            for (int y = 0; y < 5; y++) {
                tiles[x][y] = new Tile(x, y, TerrainType.GRASS);
            }
        }
        world = new World(5, 5, tiles);
        haulNode = new ThinkNode_Haul();

        // Place a MINER building with output
        Entity miner = ecsWorld.createEntity();
        BuildingComponent bc = new BuildingComponent("MINER", 1f, 5, ItemType.STONE);
        bc.built = true;
        bc.outputBuffer.add(new com.haraldsson.syntropy.entities.Item(ItemType.STONE));
        miner.add(bc);
        miner.add(new PositionComponent(2f, 2f));
    }

    private Entity createColonist(ColonistRole role) {
        Entity colonist = ecsWorld.createEntity();
        colonist.add(new PositionComponent(0f, 0f));
        colonist.add(new InventoryComponent());
        colonist.add(new AIComponent());

        WorkSettingsComponent work = new WorkSettingsComponent();
        if (role != null) {
            work.setPriority(role, 2);
        }
        colonist.add(work);
        return colonist;
    }

    @Test
    void haulerColonistGetsPriorityGreaterThanZero() {
        Entity hauler = createColonist(ColonistRole.HAULER);

        float priority = haulNode.getPriority(hauler, ecsWorld, world);

        assertTrue(priority > 0f,
                "Expected HAULER colonist to have priority > 0 when building has output, got " + priority);
    }

    @Test
    void minerColonistGetsZeroPriorityFromHaulNode() {
        Entity miner = createColonist(ColonistRole.MINER);

        float priority = haulNode.getPriority(miner, ecsWorld, world);

        assertEquals(0f, priority,
                "Expected MINER colonist to have priority 0 from ThinkNode_Haul");
    }

    @Test
    void colonistWithNoRoleGetsZeroPriorityFromHaulNode() {
        // Create colonist with no active roles (all at priority 0)
        Entity colonist = createColonist(null);

        float priority = haulNode.getPriority(colonist, ecsWorld, world);

        assertEquals(0f, priority,
                "Expected colonist with no role to have priority 0 from ThinkNode_Haul");
    }

    @Test
    void colonistWithNoWorkSettingsComponentGetsZeroPriority() {
        // Entity without WorkSettingsComponent at all
        Entity colonist = ecsWorld.createEntity();
        colonist.add(new PositionComponent(0f, 0f));
        colonist.add(new InventoryComponent());
        colonist.add(new AIComponent());

        float priority = haulNode.getPriority(colonist, ecsWorld, world);

        assertEquals(0f, priority,
                "Expected colonist without WorkSettingsComponent to have priority 0");
    }
}
