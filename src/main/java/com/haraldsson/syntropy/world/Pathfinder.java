package com.haraldsson.syntropy.world;

import java.util.*;

/**
 * Simple A* pathfinder over the World tile grid.
 * Returns a list of [x, y] waypoints from start to goal (excluding start, including goal).
 * Returns empty list if no path found or if start equals goal.
 * Inspired by RimWorld's PathFinder.FindPath() in Verse.AI.
 *
 * Stateless utility — all methods are static.
 */
public class Pathfinder {

    private static final int SEARCH_LIMIT = 2000; // prevent runaway search on large maps

    private Pathfinder() {}

    public static List<int[]> findPath(World world, int startX, int startY, int goalX, int goalY) {
        if (!world.isPassable(goalX, goalY)) return List.of();
        if (startX == goalX && startY == goalY) return List.of();

        record Node(int x, int y, int cost, int heuristic) implements Comparable<Node> {
            public int total() { return cost + heuristic; }
            @Override
            public int compareTo(Node other) { return Integer.compare(this.total(), other.total()); }
        }

        PriorityQueue<Node> open = new PriorityQueue<>();
        Map<Long, Integer> visited = new HashMap<>();
        Map<Long, int[]> cameFrom = new HashMap<>();

        long startKey = key(startX, startY);
        open.add(new Node(startX, startY, 0, heuristic(startX, startY, goalX, goalY)));
        visited.put(startKey, 0);

        int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}, {1, 1}, {-1, 1}, {1, -1}, {-1, -1}};
        int searched = 0;

        while (!open.isEmpty() && searched++ < SEARCH_LIMIT) {
            Node cur = open.poll();
            long curKey = key(cur.x(), cur.y());

            if (cur.x() == goalX && cur.y() == goalY) {
                return reconstructPath(cameFrom, goalX, goalY, startX, startY);
            }

            for (int[] d : dirs) {
                int nx = cur.x() + d[0];
                int ny = cur.y() + d[1];
                if (!world.isPassable(nx, ny)) continue;

                // Diagonal movement: check both cardinal neighbors to prevent corner-cutting
                if (d[0] != 0 && d[1] != 0) {
                    if (!world.isPassable(cur.x() + d[0], cur.y())) continue;
                    if (!world.isPassable(cur.x(), cur.y() + d[1])) continue;
                }

                int moveCost = (d[0] != 0 && d[1] != 0) ? 14 : 10; // diagonal=14, cardinal=10
                int newCost = cur.cost() + moveCost;
                long nKey = key(nx, ny);

                if (visited.getOrDefault(nKey, Integer.MAX_VALUE) <= newCost) continue;

                visited.put(nKey, newCost);
                cameFrom.put(nKey, new int[]{cur.x(), cur.y()});
                open.add(new Node(nx, ny, newCost, heuristic(nx, ny, goalX, goalY)));
            }
        }
        return List.of(); // no path found
    }

    private static int heuristic(int x, int y, int gx, int gy) {
        return 10 * Math.max(Math.abs(x - gx), Math.abs(y - gy)); // Chebyshev
    }

    private static long key(int x, int y) {
        return ((long) x << 32) | (y & 0xFFFFFFFFL);
    }

    private static List<int[]> reconstructPath(Map<Long, int[]> cameFrom, int gx, int gy, int sx, int sy) {
        List<int[]> path = new ArrayList<>();
        int[] cur = {gx, gy};
        while (cur[0] != sx || cur[1] != sy) {
            path.add(0, cur);
            int[] prev = cameFrom.get(key(cur[0], cur[1]));
            if (prev == null) return List.of(); // broken path
            cur = prev;
        }
        return path;
    }
}
