package com.haraldsson.syntropy;

import com.haraldsson.syntropy.ecs.ECSWorld;
import com.haraldsson.syntropy.ecs.Entity;
import com.haraldsson.syntropy.ecs.components.AIComponent;
import com.haraldsson.syntropy.ecs.components.PositionComponent;
import com.haraldsson.syntropy.ecs.systems.AITaskSystem;
import com.haraldsson.syntropy.world.Pathfinder;
import com.haraldsson.syntropy.world.TerrainType;
import com.haraldsson.syntropy.world.Tile;
import com.haraldsson.syntropy.world.World;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PathfinderTest {

    /** Build a fully passable 10x10 world. */
    private World buildWorld(int w, int h) {
        Tile[][] tiles = new Tile[w][h];
        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                tiles[x][y] = new Tile(x, y, TerrainType.GRASS);
            }
        }
        return new World(w, h, tiles);
    }

    /** Place a STONE (impassable) tile at (x, y). */
    private void wall(World world, int x, int y) {
        world.getTile(x, y).setTerrainType(TerrainType.STONE);
    }

    @Test
    void findsPathAroundWall() {
        // 10×10 world; vertical wall at x=5 from y=0 to y=7 (inclusive)
        World world = buildWorld(10, 10);
        for (int y = 0; y <= 7; y++) {
            wall(world, 5, y);
        }

        List<int[]> path = Pathfinder.findPath(world, 2, 5, 8, 5);

        assertFalse(path.isEmpty(), "Expected a path around the wall");
        // Last waypoint must be the goal tile
        int[] last = path.get(path.size() - 1);
        assertArrayEquals(new int[]{8, 5}, last, "Last waypoint must be goal tile (8,5)");
        // First waypoint must be adjacent to start (2,5)
        int[] first = path.get(0);
        int fdx = Math.abs(first[0] - 2);
        int fdy = Math.abs(first[1] - 5);
        assertTrue(fdx <= 1 && fdy <= 1, "First waypoint must be adjacent to start");
        // Path must not pass through any wall tile
        for (int[] step : path) {
            assertTrue(world.isPassable(step[0], step[1]),
                    "Path step (" + step[0] + "," + step[1] + ") must be passable");
        }
    }

    @Test
    void returnsEmptyPathIfGoalImpassable() {
        World world = buildWorld(10, 10);
        wall(world, 8, 5);

        List<int[]> path = Pathfinder.findPath(world, 2, 5, 8, 5);

        assertTrue(path.isEmpty(), "Expected empty path when goal tile is impassable");
    }

    @Test
    void returnsEmptyPathIfStartEqualsGoal() {
        World world = buildWorld(10, 10);

        List<int[]> path = Pathfinder.findPath(world, 3, 3, 3, 3);

        assertTrue(path.isEmpty(), "Expected empty path when start equals goal");
    }

    @Test
    void npcDoesNotCornerCutThroughDiagonalWall() {
        // Two wall tiles diagonally adjacent: (5,5) and (6,6)
        // Path from (4,5) to (7,6) must not cut through the diagonal corner
        World world = buildWorld(10, 10);
        wall(world, 5, 5);
        wall(world, 6, 6);

        List<int[]> path = Pathfinder.findPath(world, 4, 5, 7, 6);

        // Verify no step in the path is impassable
        for (int[] step : path) {
            assertTrue(world.isPassable(step[0], step[1]),
                    "Path step (" + step[0] + "," + step[1] + ") must be passable");
        }
        // Verify no diagonal move crosses through both wall tiles
        // A move from (5,6) to (6,5) would cut through corners of (5,5) and (6,6)
        // The pathfinder must not produce that sequence
        for (int i = 0; i < path.size() - 1; i++) {
            int ax = path.get(i)[0], ay = path.get(i)[1];
            int bx = path.get(i + 1)[0], by = path.get(i + 1)[1];
            boolean diagonal = Math.abs(bx - ax) == 1 && Math.abs(by - ay) == 1;
            if (diagonal) {
                // Both cardinal neighbors must be passable
                assertTrue(world.isPassable(bx, ay),
                        "Diagonal move corner (" + bx + "," + ay + ") must be passable");
                assertTrue(world.isPassable(ax, by),
                        "Diagonal move corner (" + ax + "," + by + ") must be passable");
            }
        }
    }

    @Test
    void recoveryTeleportsNpcToNearestPassableTile() {
        // 10×10 world; NPC placed on an impassable STONE tile
        World world = buildWorld(10, 10);
        wall(world, 4, 4);

        ECSWorld ecsWorld = new ECSWorld();
        Entity npc = ecsWorld.createEntity();
        PositionComponent pos = new PositionComponent(4.5f, 4.5f); // inside stone tile
        npc.add(pos);
        npc.add(new AIComponent());

        boolean recovered = AITaskSystem.tryRecoverFromImpassable(npc, world);

        assertTrue(recovered, "Recovery should succeed when a passable tile exists nearby");
        int newTileX = (int) Math.floor(pos.x);
        int newTileY = (int) Math.floor(pos.y);
        assertTrue(world.isPassable(newTileX, newTileY),
                "NPC position after recovery must be on a passable tile");
    }
}
