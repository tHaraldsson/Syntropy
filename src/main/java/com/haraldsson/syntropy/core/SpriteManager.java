package com.haraldsson.syntropy.core;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.haraldsson.syntropy.entities.ItemType;
import com.haraldsson.syntropy.world.TerrainType;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Generates detailed procedural sprites with a Factorio-inspired industrial aesthetic.
 * Replace with real sprite sheets later.
 */
public class SpriteManager {
    private final Map<String, Texture> textures = new HashMap<>();
    private final int T; // tile size
    private final Random rng = new Random(42); // deterministic seed for consistency

    public SpriteManager(int tileSize) {
        this.T = tileSize;
        generateTerrainTextures();
        generateBuildingTextures();
        generateColonistTextures();
        generateItemTextures();
        generateStockpileTexture();
    }

    // ── Helpers ──

    private void setPixel(Pixmap pm, int x, int y, float r, float g, float b) {
        if (x >= 0 && y >= 0 && x < pm.getWidth() && y < pm.getHeight()) {
            pm.setColor(r, g, b, 1f);
            pm.drawPixel(x, y);
        }
    }

    /** Add per-pixel noise variation to a base color */
    private void fillWithNoise(Pixmap pm, float r, float g, float b, float noiseAmount) {
        for (int y = 0; y < pm.getHeight(); y++) {
            for (int x = 0; x < pm.getWidth(); x++) {
                float n = (rng.nextFloat() - 0.5f) * 2f * noiseAmount;
                float rr = clamp(r + n);
                float gg = clamp(g + n * 0.8f);
                float bb = clamp(b + n * 0.6f);
                setPixel(pm, x, y, rr, gg, bb);
            }
        }
    }

    private float clamp(float v) { return Math.max(0f, Math.min(1f, v)); }

    private void drawBorder(Pixmap pm, float r, float g, float b, int thickness) {
        pm.setColor(r, g, b, 1f);
        for (int i = 0; i < thickness; i++) {
            pm.drawRectangle(i, i, pm.getWidth() - i * 2, pm.getHeight() - i * 2);
        }
    }

    private void drawShadow(Pixmap pm, int x, int y, int w, int h) {
        pm.setColor(0f, 0f, 0f, 0.3f);
        pm.fillRectangle(x + 2, y + 2, w, h);
    }

    // ── Terrain ──

    private void generateTerrainTextures() {
        // WATER — animated-looking ripple pattern
        generateWater();
        // SAND — grainy desert look
        generateSand();
        // GRASS — lush with blade details
        generateGrass();
        // DIRT — earthy with pebble details
        generateDirt();
        // STONE — rocky with cracks
        generateStone();
    }

    private void generateWater() {
        Pixmap pm = new Pixmap(T, T, Pixmap.Format.RGBA8888);
        for (int y = 0; y < T; y++) {
            for (int x = 0; x < T; x++) {
                float wave = (float) Math.sin((x + y * 0.7f) * 0.3f) * 0.04f;
                float depth = (float) Math.sin(x * 0.15f) * 0.02f;
                float r = clamp(0.06f + wave);
                float g = clamp(0.12f + wave + depth);
                float b = clamp(0.28f + wave * 2f + (rng.nextFloat() - 0.5f) * 0.03f);
                setPixel(pm, x, y, r, g, b);
            }
        }
        // Highlight ripples
        for (int i = 0; i < 4; i++) {
            int rx = rng.nextInt(T - 8) + 4;
            int ry = rng.nextInt(T - 4) + 2;
            pm.setColor(0.15f, 0.22f, 0.4f, 1f);
            pm.drawLine(rx, ry, rx + 4 + rng.nextInt(6), ry);
            pm.setColor(0.1f, 0.18f, 0.35f, 1f);
            pm.drawLine(rx + 1, ry + 1, rx + 3 + rng.nextInt(4), ry + 1);
        }
        textures.put("terrain_WATER", new Texture(pm));
        pm.dispose();
    }

    private void generateSand() {
        Pixmap pm = new Pixmap(T, T, Pixmap.Format.RGBA8888);
        fillWithNoise(pm, 0.55f, 0.48f, 0.3f, 0.06f);
        // Sandy speckles
        for (int i = 0; i < 20; i++) {
            int x = rng.nextInt(T);
            int y = rng.nextInt(T);
            setPixel(pm, x, y, 0.62f, 0.56f, 0.38f);
        }
        // Dark grains
        for (int i = 0; i < 10; i++) {
            int x = rng.nextInt(T);
            int y = rng.nextInt(T);
            setPixel(pm, x, y, 0.42f, 0.36f, 0.22f);
        }
        // Subtle edge
        pm.setColor(0.48f, 0.42f, 0.26f, 1f);
        pm.drawRectangle(0, 0, T, T);
        textures.put("terrain_SAND", new Texture(pm));
        pm.dispose();
    }

    private void generateGrass() {
        Pixmap pm = new Pixmap(T, T, Pixmap.Format.RGBA8888);
        // Base with variation
        for (int y = 0; y < T; y++) {
            for (int x = 0; x < T; x++) {
                float n = (rng.nextFloat() - 0.5f) * 0.08f;
                float patch = (float) Math.sin(x * 0.4f + y * 0.3f) * 0.03f;
                setPixel(pm, x, y,
                        clamp(0.12f + n * 0.5f + patch),
                        clamp(0.22f + n + patch),
                        clamp(0.08f + n * 0.3f));
            }
        }
        // Grass blade highlights
        for (int i = 0; i < 15; i++) {
            int x = rng.nextInt(T);
            int y = rng.nextInt(T - 3);
            pm.setColor(0.18f, 0.32f, 0.12f, 1f);
            pm.drawLine(x, y, x + (rng.nextBoolean() ? 1 : -1), y + 2);
        }
        // Dark patches
        for (int i = 0; i < 5; i++) {
            int x = rng.nextInt(T - 4);
            int y = rng.nextInt(T - 4);
            pm.setColor(0.08f, 0.14f, 0.06f, 1f);
            pm.fillRectangle(x, y, 2 + rng.nextInt(3), 2 + rng.nextInt(3));
        }
        pm.setColor(0.1f, 0.16f, 0.07f, 1f);
        pm.drawRectangle(0, 0, T, T);
        textures.put("terrain_GRASS", new Texture(pm));
        pm.dispose();
    }

    private void generateDirt() {
        Pixmap pm = new Pixmap(T, T, Pixmap.Format.RGBA8888);
        fillWithNoise(pm, 0.28f, 0.2f, 0.12f, 0.05f);
        // Pebbles
        for (int i = 0; i < 8; i++) {
            int x = rng.nextInt(T - 3);
            int y = rng.nextInt(T - 3);
            float shade = 0.22f + rng.nextFloat() * 0.1f;
            pm.setColor(shade, shade * 0.85f, shade * 0.6f, 1f);
            pm.fillRectangle(x, y, 2 + rng.nextInt(2), 2);
        }
        // Cracks
        for (int i = 0; i < 3; i++) {
            int x = rng.nextInt(T);
            int y = rng.nextInt(T);
            pm.setColor(0.18f, 0.12f, 0.07f, 1f);
            pm.drawLine(x, y, x + rng.nextInt(8) - 4, y + rng.nextInt(6) - 3);
        }
        pm.setColor(0.2f, 0.14f, 0.08f, 1f);
        pm.drawRectangle(0, 0, T, T);
        textures.put("terrain_DIRT", new Texture(pm));
        pm.dispose();
    }

    private void generateStone() {
        Pixmap pm = new Pixmap(T, T, Pixmap.Format.RGBA8888);
        // Multi-tone rocky base
        for (int y = 0; y < T; y++) {
            for (int x = 0; x < T; x++) {
                float n = (rng.nextFloat() - 0.5f) * 0.1f;
                float blocky = ((x / 6 + y / 6) % 2 == 0) ? 0.02f : -0.02f;
                float base = 0.3f + n + blocky;
                setPixel(pm, x, y, clamp(base), clamp(base * 0.95f), clamp(base * 0.88f));
            }
        }
        // Crack lines
        for (int i = 0; i < 4; i++) {
            int x = rng.nextInt(T);
            int y = rng.nextInt(T);
            pm.setColor(0.18f, 0.17f, 0.15f, 1f);
            int dx = rng.nextInt(12) - 6;
            int dy = rng.nextInt(12) - 6;
            pm.drawLine(x, y, x + dx, y + dy);
        }
        // Mineral specks
        for (int i = 0; i < 6; i++) {
            int x = rng.nextInt(T);
            int y = rng.nextInt(T);
            setPixel(pm, x, y, 0.4f, 0.38f, 0.35f);
        }
        pm.setColor(0.22f, 0.21f, 0.19f, 1f);
        pm.drawRectangle(0, 0, T, T);
        textures.put("terrain_STONE", new Texture(pm));
        pm.dispose();
    }

    public Texture getTerrainTexture(TerrainType type) {
        return textures.get("terrain_" + type.name());
    }

    // ── Stockpile ──

    private void generateStockpileTexture() {
        int s = T - 8;
        Pixmap pm = new Pixmap(s, s, Pixmap.Format.RGBA8888);
        // Dark steel floor
        for (int y = 0; y < s; y++) {
            for (int x = 0; x < s; x++) {
                float n = (rng.nextFloat() - 0.5f) * 0.03f;
                float grid = ((x / 4 + y / 4) % 2 == 0) ? 0.02f : -0.01f;
                float base = 0.14f + n + grid;
                setPixel(pm, x, y, clamp(base * 0.8f), clamp(base * 0.85f), clamp(base + 0.05f));
            }
        }
        // Corner bolts
        int boltSize = 3;
        float br = 0.25f, bg = 0.28f, bb = 0.35f;
        pm.setColor(br, bg, bb, 1f);
        pm.fillRectangle(2, 2, boltSize, boltSize);
        pm.fillRectangle(s - 2 - boltSize, 2, boltSize, boltSize);
        pm.fillRectangle(2, s - 2 - boltSize, boltSize, boltSize);
        pm.fillRectangle(s - 2 - boltSize, s - 2 - boltSize, boltSize, boltSize);
        // Border — double line industrial
        pm.setColor(0.2f, 0.25f, 0.35f, 1f);
        pm.drawRectangle(0, 0, s, s);
        pm.setColor(0.15f, 0.18f, 0.28f, 1f);
        pm.drawRectangle(1, 1, s - 2, s - 2);
        // Center marker (chest icon)
        int cx = s / 2, cy = s / 2;
        pm.setColor(0.22f, 0.26f, 0.38f, 1f);
        pm.fillRectangle(cx - 6, cy - 4, 12, 8);
        pm.setColor(0.28f, 0.32f, 0.45f, 1f);
        pm.drawRectangle(cx - 6, cy - 4, 12, 8);
        pm.fillRectangle(cx - 1, cy - 2, 2, 4);
        textures.put("stockpile", new Texture(pm));
        pm.dispose();
    }

    public Texture getStockpileTexture() {
        return textures.get("stockpile");
    }

    // ── Buildings ──

    private void generateBuildingTextures() {
        generateMiner();
        generateFoodGrower();
    }

    private void generateMiner() {
        int s = T - 8;
        Pixmap pm = new Pixmap(s, s, Pixmap.Format.RGBA8888);
        // Machine body — dark steel with noise
        for (int y = 0; y < s; y++) {
            for (int x = 0; x < s; x++) {
                float n = (rng.nextFloat() - 0.5f) * 0.04f;
                setPixel(pm, x, y, clamp(0.3f + n), clamp(0.28f + n), clamp(0.26f + n));
            }
        }
        // Shadow under
        pm.setColor(0.12f, 0.11f, 0.1f, 1f);
        pm.fillRectangle(2, s - 4, s - 2, 4);

        // Body panel
        pm.setColor(0.38f, 0.36f, 0.33f, 1f);
        pm.fillRectangle(4, 4, s - 8, s - 12);
        pm.setColor(0.45f, 0.42f, 0.38f, 1f);
        pm.drawRectangle(4, 4, s - 8, s - 12);

        // Drill head (triangle-ish)
        pm.setColor(0.5f, 0.48f, 0.4f, 1f);
        int midX = s / 2;
        for (int i = 0; i < 8; i++) {
            pm.drawLine(midX - i / 2, 6 + i, midX + i / 2, 6 + i);
        }
        // Drill tip
        pm.setColor(0.6f, 0.55f, 0.3f, 1f);
        pm.fillRectangle(midX - 1, 4, 3, 4);

        // Gear detail
        pm.setColor(0.42f, 0.4f, 0.36f, 1f);
        pm.fillCircle(s - 10, s / 2, 4);
        pm.setColor(0.32f, 0.3f, 0.27f, 1f);
        pm.fillCircle(s - 10, s / 2, 2);

        // Conveyor belt stripes at bottom
        pm.setColor(0.25f, 0.24f, 0.22f, 1f);
        for (int x = 2; x < s - 2; x += 4) {
            pm.fillRectangle(x, s - 6, 2, 3);
        }

        // Bolts
        pm.setColor(0.5f, 0.48f, 0.42f, 1f);
        pm.fillRectangle(5, 5, 2, 2);
        pm.fillRectangle(s - 7, 5, 2, 2);
        pm.fillRectangle(5, s - 13, 2, 2);
        pm.fillRectangle(s - 7, s - 13, 2, 2);

        // Border
        pm.setColor(0.22f, 0.2f, 0.18f, 1f);
        pm.drawRectangle(0, 0, s, s);

        textures.put("building_MINER", new Texture(pm));
        pm.dispose();
    }

    private void generateFoodGrower() {
        int s = T - 8;
        Pixmap pm = new Pixmap(s, s, Pixmap.Format.RGBA8888);

        // Greenhouse base — warm green-brown
        for (int y = 0; y < s; y++) {
            for (int x = 0; x < s; x++) {
                float n = (rng.nextFloat() - 0.5f) * 0.04f;
                float yGrad = (float) y / s * 0.06f;
                setPixel(pm, x, y,
                        clamp(0.18f + n + yGrad),
                        clamp(0.3f + n - yGrad),
                        clamp(0.1f + n * 0.5f));
            }
        }

        // Shadow
        pm.setColor(0.08f, 0.12f, 0.05f, 1f);
        pm.fillRectangle(2, s - 4, s - 2, 4);

        // Glass panel (greenhouse roof)
        pm.setColor(0.25f, 0.4f, 0.3f, 0.7f);
        pm.fillRectangle(4, 3, s - 8, s / 2 - 2);
        pm.setColor(0.35f, 0.5f, 0.38f, 1f);
        pm.drawRectangle(4, 3, s - 8, s / 2 - 2);
        // Glass pane lines
        pm.setColor(0.3f, 0.45f, 0.33f, 1f);
        pm.drawLine(s / 2, 3, s / 2, s / 2);
        pm.drawLine(4, s / 4, s - 4, s / 4);

        // Crop rows at bottom
        for (int row = 0; row < 3; row++) {
            int ry = s / 2 + 2 + row * 5;
            // Soil
            pm.setColor(0.2f, 0.14f, 0.08f, 1f);
            pm.fillRectangle(5, ry, s - 10, 3);
            // Plants
            for (int px = 6; px < s - 6; px += 4) {
                pm.setColor(0.2f + rng.nextFloat() * 0.1f, 0.4f + rng.nextFloat() * 0.15f, 0.12f, 1f);
                pm.fillRectangle(px, ry - 2, 2, 3);
                pm.setColor(0.3f, 0.55f, 0.2f, 1f);
                pm.drawPixel(px, ry - 3);
                pm.drawPixel(px + 1, ry - 3);
            }
        }

        // Frame border
        pm.setColor(0.12f, 0.18f, 0.08f, 1f);
        pm.drawRectangle(0, 0, s, s);
        pm.setColor(0.15f, 0.22f, 0.1f, 1f);
        pm.drawRectangle(1, 1, s - 2, s - 2);

        textures.put("building_FOOD_GROWER", new Texture(pm));
        pm.dispose();
    }

    public Texture getBuildingTexture(String buildingType) {
        return textures.get("building_" + buildingType);
    }

    // ── Colonists ──

    private void generateColonistTextures() {
        generateColonist("colonist_alive", 0.85f, 0.55f, 0.2f, false, false);
        generateColonist("colonist_possessed", 0.9f, 0.2f, 0.15f, false, true);
        generateColonist("colonist_dead", 0.25f, 0.15f, 0.12f, true, false);
    }

    private void generateColonist(String key, float bodyR, float bodyG, float bodyB, boolean dead, boolean possessed) {
        int s = T - 16;
        Pixmap pm = new Pixmap(s, s, Pixmap.Format.RGBA8888);

        if (dead) {
            // Fallen body — horizontal oval
            pm.setColor(bodyR, bodyG, bodyB, 1f);
            pm.fillRectangle(2, s / 2, s - 4, s / 3);
            pm.setColor(bodyR * 0.7f, bodyG * 0.7f, bodyB * 0.7f, 1f);
            pm.drawRectangle(2, s / 2, s - 4, s / 3);
            // X eyes
            pm.setColor(0.5f, 0.15f, 0.1f, 1f);
            int ey = s / 2 + 3;
            pm.drawLine(4, ey, 7, ey + 3);
            pm.drawLine(7, ey, 4, ey + 3);
            pm.drawLine(s - 8, ey, s - 5, ey + 3);
            pm.drawLine(s - 5, ey, s - 8, ey + 3);
        } else {
            // Shadow
            pm.setColor(0f, 0f, 0f, 0.2f);
            pm.fillCircle(s / 2 + 1, s - 3, s / 3);

            // Body (torso)
            float bDark = 0.75f;
            pm.setColor(bodyR * bDark, bodyG * bDark, bodyB * bDark, 1f);
            pm.fillRectangle(s / 4, s / 3, s / 2, s / 2);
            pm.setColor(bodyR, bodyG, bodyB, 1f);
            pm.fillRectangle(s / 4 + 1, s / 3 + 1, s / 2 - 2, s / 2 - 2);

            // Belt detail
            pm.setColor(bodyR * 0.5f, bodyG * 0.5f, bodyB * 0.5f, 1f);
            pm.fillRectangle(s / 4, s / 2 + 2, s / 2, 2);

            // Head (circle)
            float headR = 0.75f, headG = 0.6f, headB = 0.45f;
            int headY = s / 4;
            pm.setColor(headR, headG, headB, 1f);
            pm.fillCircle(s / 2, headY, s / 5);
            pm.setColor(headR * 0.85f, headG * 0.85f, headB * 0.85f, 1f);
            pm.drawCircle(s / 2, headY, s / 5);

            // Eyes
            pm.setColor(0.15f, 0.12f, 0.1f, 1f);
            pm.fillRectangle(s / 2 - 3, headY - 1, 2, 2);
            pm.fillRectangle(s / 2 + 2, headY - 1, 2, 2);

            // Legs
            pm.setColor(bodyR * 0.6f, bodyG * 0.6f, bodyB * 0.6f, 1f);
            pm.fillRectangle(s / 4 + 2, s * 3 / 4, 3, s / 4 - 2);
            pm.fillRectangle(s * 3 / 4 - 5, s * 3 / 4, 3, s / 4 - 2);

            // Boots
            pm.setColor(0.2f, 0.18f, 0.15f, 1f);
            pm.fillRectangle(s / 4 + 1, s - 4, 5, 3);
            pm.fillRectangle(s * 3 / 4 - 6, s - 4, 5, 3);

            // Possessed glow border
            if (possessed) {
                pm.setColor(1f, 0.3f, 0.2f, 0.8f);
                pm.drawRectangle(0, 0, s, s);
                pm.drawRectangle(1, 1, s - 2, s - 2);
                // Pulsing indicator on head
                pm.setColor(1f, 0.4f, 0.3f, 1f);
                pm.fillCircle(s / 2, headY - s / 5 - 2, 2);
            }
        }

        textures.put(key, new Texture(pm));
        pm.dispose();
    }

    public Texture getColonistTexture(boolean dead, boolean possessed) {
        if (dead) return textures.get("colonist_dead");
        if (possessed) return textures.get("colonist_possessed");
        return textures.get("colonist_alive");
    }

    // ── Items ──

    private void generateItemTextures() {
        generateStoneItem();
        generateFoodItem();
    }

    private void generateStoneItem() {
        int s = 12;
        Pixmap pm = new Pixmap(s, s, Pixmap.Format.RGBA8888);
        // Rock shape — irregular polygon approximation
        for (int y = 0; y < s; y++) {
            for (int x = 0; x < s; x++) {
                float dx = x - s / 2f;
                float dy = y - s / 2f;
                float dist = (float) Math.sqrt(dx * dx + dy * dy);
                if (dist < s / 2.2f) {
                    float n = (rng.nextFloat() - 0.5f) * 0.08f;
                    float shade = 0.45f + n - dist * 0.02f;
                    setPixel(pm, x, y, shade, shade * 0.95f, shade * 0.88f);
                }
            }
        }
        // Highlight
        setPixel(pm, 3, 3, 0.55f, 0.53f, 0.48f);
        setPixel(pm, 4, 3, 0.55f, 0.53f, 0.48f);
        // Dark crack
        pm.setColor(0.3f, 0.28f, 0.25f, 1f);
        pm.drawLine(3, 5, 7, 8);
        textures.put("item_STONE", new Texture(pm));
        pm.dispose();
    }

    private void generateFoodItem() {
        int s = 12;
        Pixmap pm = new Pixmap(s, s, Pixmap.Format.RGBA8888);
        // Wheat/grain bundle
        // Stalks
        pm.setColor(0.65f, 0.5f, 0.15f, 1f);
        pm.drawLine(3, 2, 3, 10);
        pm.drawLine(6, 1, 6, 10);
        pm.drawLine(9, 2, 9, 10);
        // Grain heads
        pm.setColor(0.8f, 0.65f, 0.2f, 1f);
        pm.fillRectangle(2, 1, 3, 3);
        pm.fillRectangle(5, 0, 3, 3);
        pm.fillRectangle(8, 1, 3, 3);
        // Tie
        pm.setColor(0.5f, 0.35f, 0.1f, 1f);
        pm.drawLine(2, 7, 10, 7);
        textures.put("item_FOOD", new Texture(pm));
        pm.dispose();
    }

    public Texture getItemTexture(ItemType type) {
        return textures.get("item_" + type.name());
    }

    public void dispose() {
        for (Texture tex : textures.values()) {
            tex.dispose();
        }
        textures.clear();
    }
}
