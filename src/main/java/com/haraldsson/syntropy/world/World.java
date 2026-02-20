package com.haraldsson.syntropy.world;

import com.haraldsson.syntropy.entities.ItemType;

import java.util.ArrayList;
import java.util.List;

/**
 * Spatial grid — stores tiles and stockpile reference.
 * Entity management is now handled by ECSWorld.
 */
public class World {
    private final int width;
    private final int height;
    private final Tile[][] tiles;
    private Tile stockpileTile;

    public World(int width, int height, Tile[][] tiles) {
        this.width = width;
        this.height = height;
        this.tiles = tiles;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public Tile getTile(int x, int y) {
        if (x < 0 || y < 0 || x >= width || y >= height) {
            return null;
        }
        return tiles[x][y];
    }

    public Tile[][] getTiles() {
        return tiles;
    }

    public Tile getStockpileTile() {
        return stockpileTile;
    }

    public void setStockpileTile(Tile stockpileTile) {
        this.stockpileTile = stockpileTile;
    }

    public Tile findNearestFoodTile(float fromX, float fromY) {
        Tile best = null;
        float bestDist = Float.MAX_VALUE;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Tile tile = tiles[x][y];
                if (tile.hasItem(ItemType.FOOD)) {
                    float dx = fromX - (x + 0.5f);
                    float dy = fromY - (y + 0.5f);
                    float dist = dx * dx + dy * dy;
                    if (dist < bestDist) {
                        bestDist = dist;
                        best = tile;
                    }
                }
            }
        }
        return best;
    }

    public boolean isPassable(int tileX, int tileY) {
        // FIX BUG2c: verified/corrected passable terrain types (2026-02-20)
        // FIX BUG4c: Tile (x, y) occupies world-space from (x, y) to (x+1, y+1).
        // A position (px, py) is in tile (floor(px), floor(py)).
        // Use (int)Math.floor(px) — NOT (int)px — to avoid asymmetric collision at negative coords.
        Tile t = getTile(tileX, tileY);
        if (t == null) return false;
        TerrainType type = t.getTerrainType();
        return type != TerrainType.WATER && type != TerrainType.STONE;
    }

    public int[] findNearestPassableTile(float fromX, float fromY) {
        // FIX BUG4d: stuck NPC teleports to nearest passable tile before clearing task (2026-02-20)
        // Spiral outward from current position for efficient early termination
        int cx = (int) Math.floor(fromX);
        int cy = (int) Math.floor(fromY);
        for (int r = 0; r < Math.max(width, height); r++) {
            for (int dy = -r; dy <= r; dy++) {
                for (int dx = -r; dx <= r; dx++) {
                    if (Math.abs(dx) != r && Math.abs(dy) != r) continue; // only check ring edges
                    int tx = cx + dx;
                    int ty = cy + dy;
                    if (tx >= 0 && ty >= 0 && tx < width && ty < height && isPassable(tx, ty)) {
                        return new int[]{tx, ty};
                    }
                }
            }
        }
        return null;
    }

    public void clampPosition(float[] xy) {
        xy[0] = Math.max(0.1f, Math.min(width - 0.1f, xy[0]));
        xy[1] = Math.max(0.1f, Math.min(height - 0.1f, xy[1]));
    }
}
