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
                float wave1 = (float) Math.sin((x * 0.25f + y * 0.15f)) * 0.035f;
                float wave2 = (float) Math.sin((x * 0.12f - y * 0.22f + 3.5f)) * 0.025f;
                float deep = (float) Math.sin(y * 0.08f) * 0.02f;
                float n = rng.nextFloat() * 0.015f;
                setPixel(pm, x, y,
                        clamp(0.08f + wave1 + n),
                        clamp(0.18f + wave1 + wave2 + deep + n),
                        clamp(0.38f + wave2 * 2f + deep + n));
            }
        }
        // Specular highlight streaks
        for (int i = 0; i < 6; i++) {
            int ry = rng.nextInt(T - 2) + 1;
            int rx = rng.nextInt(T / 2);
            int len = 3 + rng.nextInt(8);
            for (int dx = 0; dx < len && rx + dx < T; dx++) {
                setPixel(pm, rx + dx, ry, 0.2f, 0.32f, 0.55f);
            }
        }
        // Foam dots
        for (int i = 0; i < 8; i++) {
            setPixel(pm, rng.nextInt(T), rng.nextInt(T), 0.25f, 0.35f, 0.5f);
        }
        textures.put("terrain_WATER", new Texture(pm));
        pm.dispose();
    }

    private void generateSand() {
        Pixmap pm = new Pixmap(T, T, Pixmap.Format.RGBA8888);
        for (int y = 0; y < T; y++) {
            for (int x = 0; x < T; x++) {
                float n = rng.nextFloat() * 0.06f - 0.03f;
                float dune = (float) Math.sin(x * 0.15f + y * 0.08f) * 0.03f;
                setPixel(pm, x, y,
                        clamp(0.76f + n + dune),
                        clamp(0.68f + n * 0.9f + dune),
                        clamp(0.45f + n * 0.5f + dune * 0.5f));
            }
        }
        // Pebbles and shells
        for (int i = 0; i < 12; i++) {
            int x = rng.nextInt(T);
            int y = rng.nextInt(T);
            float shade = 0.6f + rng.nextFloat() * 0.15f;
            setPixel(pm, x, y, shade, shade * 0.9f, shade * 0.65f);
            if (rng.nextBoolean()) setPixel(pm, x + 1, y, shade, shade * 0.9f, shade * 0.65f);
        }
        // Wind ripple lines
        for (int i = 0; i < 3; i++) {
            int ry = rng.nextInt(T);
            for (int x = 0; x < T; x += 2) {
                setPixel(pm, x, ry, 0.72f, 0.64f, 0.42f);
            }
        }
        // Subtle border
        pm.setColor(0.68f, 0.6f, 0.4f, 1f);
        pm.drawRectangle(0, 0, T, T);
        textures.put("terrain_SAND", new Texture(pm));
        pm.dispose();
    }

    private void generateGrass() {
        Pixmap pm = new Pixmap(T, T, Pixmap.Format.RGBA8888);
        // Rich base with overlapping color patches
        for (int y = 0; y < T; y++) {
            for (int x = 0; x < T; x++) {
                float n = (rng.nextFloat() - 0.5f) * 0.06f;
                float patch = (float) Math.sin(x * 0.3f + y * 0.2f) * 0.04f;
                float patch2 = (float) Math.cos(x * 0.15f - y * 0.25f) * 0.03f;
                setPixel(pm, x, y,
                        clamp(0.15f + n * 0.4f + patch),
                        clamp(0.35f + n + patch + patch2),
                        clamp(0.1f + n * 0.3f));
            }
        }
        // Grass blade highlights (varied)
        for (int i = 0; i < 30; i++) {
            int x = rng.nextInt(T);
            int y = rng.nextInt(T - 4) + 1;
            float g = 0.42f + rng.nextFloat() * 0.2f;
            setPixel(pm, x, y, 0.15f, g, 0.1f);
            setPixel(pm, x, y - 1, 0.18f, g + 0.05f, 0.12f);
            if (rng.nextBoolean()) {
                int ox = rng.nextBoolean() ? 1 : -1;
                setPixel(pm, x + ox, y - 1, 0.16f, g + 0.02f, 0.11f);
            }
        }
        // Small flowers (3-4 random colored dots)
        for (int i = 0; i < 4; i++) {
            int fx = rng.nextInt(T - 2) + 1;
            int fy = rng.nextInt(T - 3) + 1;
            float fr = 0.7f + rng.nextFloat() * 0.3f;
            float fg = 0.3f + rng.nextFloat() * 0.4f;
            float fb = 0.2f + rng.nextFloat() * 0.3f;
            setPixel(pm, fx, fy, fr, fg, fb);
            setPixel(pm, fx, fy + 1, 0.15f, 0.35f, 0.1f);
        }
        // Dark soil patches
        for (int i = 0; i < 4; i++) {
            int x = rng.nextInt(T - 4);
            int y = rng.nextInt(T - 4);
            pm.setColor(0.1f, 0.18f, 0.07f, 1f);
            pm.fillRectangle(x, y, 2 + rng.nextInt(3), 2 + rng.nextInt(2));
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
        for (int y = 0; y < T; y++) {
            for (int x = 0; x < T; x++) {
                float n = (rng.nextFloat() - 0.5f) * 0.08f;
                // Masonry block pattern
                int bx = (x / 8) % 2;
                int by = (y / 8) % 2;
                float block = (bx ^ by) == 0 ? 0.02f : -0.02f;
                float base = 0.4f + n + block;
                setPixel(pm, x, y, clamp(base), clamp(base * 0.96f), clamp(base * 0.9f));
            }
        }
        // Grid lines (mortar between blocks)
        pm.setColor(0.3f, 0.29f, 0.27f, 1f);
        for (int i = 0; i < T; i += 8) {
            pm.drawLine(i, 0, i, T);
            pm.drawLine(0, i, T, i);
        }
        // Crack details
        for (int i = 0; i < 4; i++) {
            int x = rng.nextInt(T);
            int y = rng.nextInt(T);
            pm.setColor(0.24f, 0.23f, 0.2f, 1f);
            pm.drawLine(x, y, x + rng.nextInt(14) - 7, y + rng.nextInt(14) - 7);
        }
        // Mineral specks
        for (int i = 0; i < 10; i++) {
            setPixel(pm, rng.nextInt(T), rng.nextInt(T), 0.52f, 0.5f, 0.44f);
        }
        pm.setColor(0.28f, 0.27f, 0.24f, 1f);
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
        generateWoodcutter();
        generateBed();
    }

    private void generateMiner() {
        int s = T - 8;
        Pixmap pm = new Pixmap(s, s, Pixmap.Format.RGBA8888);

        // Drop shadow
        pm.setColor(0.06f, 0.05f, 0.04f, 1f);
        pm.fillRectangle(3, 3, s - 3, s - 3);

        // Machine body — heavy industrial steel
        for (int y = 1; y < s - 3; y++) {
            for (int x = 1; x < s - 1; x++) {
                float n = (rng.nextFloat() - 0.5f) * 0.03f;
                float yShade = (float) y / s * 0.04f;
                setPixel(pm, x, y, clamp(0.35f + n - yShade), clamp(0.33f + n - yShade), clamp(0.3f + n - yShade));
            }
        }

        // Top housing plate
        pm.setColor(0.28f, 0.27f, 0.25f, 1f);
        pm.fillRectangle(3, 2, s - 6, 10);
        pm.setColor(0.42f, 0.4f, 0.36f, 1f);
        pm.drawRectangle(3, 2, s - 6, 10);

        // Drill arm and bit
        int mx = s / 2;
        pm.setColor(0.5f, 0.46f, 0.38f, 1f);
        pm.fillRectangle(mx - 2, 5, 4, 16);
        // Drill bit chevron
        pm.setColor(0.62f, 0.56f, 0.35f, 1f);
        for (int i = 0; i < 5; i++) {
            setPixel(pm, mx - i, 5 + i, 0.62f, 0.56f, 0.35f);
            setPixel(pm, mx + i, 5 + i, 0.62f, 0.56f, 0.35f);
        }
        // Drill tip glow
        setPixel(pm, mx, 3, 0.85f, 0.75f, 0.4f);
        setPixel(pm, mx, 4, 0.75f, 0.65f, 0.3f);

        // Gear wheel (right side)
        int gx = s - 10, gy = s / 2 + 2;
        pm.setColor(0.42f, 0.4f, 0.36f, 1f);
        pm.fillCircle(gx, gy, 5);
        pm.setColor(0.32f, 0.3f, 0.27f, 1f);
        pm.fillCircle(gx, gy, 3);
        pm.setColor(0.48f, 0.45f, 0.4f, 1f);
        pm.fillCircle(gx, gy, 1);
        // Gear teeth
        for (int a = 0; a < 8; a++) {
            double ang = a * Math.PI / 4.0;
            int tx = gx + (int) (Math.cos(ang) * 5);
            int ty = gy + (int) (Math.sin(ang) * 5);
            setPixel(pm, tx, ty, 0.5f, 0.48f, 0.42f);
        }

        // Conveyor belt at bottom
        pm.setColor(0.2f, 0.19f, 0.17f, 1f);
        pm.fillRectangle(2, s - 6, s - 4, 4);
        pm.setColor(0.28f, 0.27f, 0.24f, 1f);
        for (int x = 3; x < s - 3; x += 3) {
            pm.fillRectangle(x, s - 5, 1, 2);
        }

        // Corner bolts
        pm.setColor(0.52f, 0.5f, 0.45f, 1f);
        pm.fillRectangle(3, 3, 2, 2);
        pm.fillRectangle(s - 5, 3, 2, 2);
        pm.fillRectangle(3, s - 7, 2, 2);
        pm.fillRectangle(s - 5, s - 7, 2, 2);

        // Status light (green)
        pm.setColor(0.2f, 0.7f, 0.25f, 1f);
        pm.fillCircle(s - 7, 5, 2);
        setPixel(pm, s - 7, 4, 0.3f, 0.85f, 0.35f);

        // Border
        pm.setColor(0.22f, 0.21f, 0.19f, 1f);
        pm.drawRectangle(0, 0, s, s - 2);

        textures.put("building_MINER", new Texture(pm));
        pm.dispose();
    }

    private void generateFoodGrower() {
        int s = T - 8;
        Pixmap pm = new Pixmap(s, s, Pixmap.Format.RGBA8888);

        // Drop shadow
        pm.setColor(0.05f, 0.08f, 0.04f, 1f);
        pm.fillRectangle(3, 3, s - 3, s - 3);

        // Greenhouse body
        for (int y = 1; y < s - 1; y++) {
            for (int x = 1; x < s - 1; x++) {
                float n = (rng.nextFloat() - 0.5f) * 0.03f;
                float yGrad = (float) y / s * 0.05f;
                setPixel(pm, x, y, clamp(0.2f + n + yGrad), clamp(0.32f + n - yGrad), clamp(0.15f + n));
            }
        }

        // Glass dome roof (top third)
        int roofH = s / 3;
        pm.setColor(0.28f, 0.48f, 0.38f, 1f);
        pm.fillRectangle(3, 2, s - 6, roofH);
        pm.setColor(0.38f, 0.55f, 0.42f, 1f);
        pm.drawRectangle(3, 2, s - 6, roofH);
        // Glass pane dividers
        pm.setColor(0.35f, 0.52f, 0.4f, 1f);
        pm.drawLine(s / 2, 2, s / 2, 2 + roofH);
        pm.drawLine(3, 2 + roofH / 2, s - 3, 2 + roofH / 2);
        // Light reflection
        pm.setColor(0.42f, 0.58f, 0.48f, 1f);
        for (int x = 5; x < s / 2 - 2; x++) {
            setPixel(pm, x, 4, 0.45f, 0.6f, 0.5f);
        }

        // Crop rows (bottom 2/3)
        int cropStart = 2 + roofH + 3;
        for (int row = 0; row < 3; row++) {
            int ry = cropStart + row * 6;
            if (ry + 3 >= s) break;
            // Soil furrow
            pm.setColor(0.25f, 0.18f, 0.1f, 1f);
            pm.fillRectangle(4, ry, s - 8, 3);
            pm.setColor(0.2f, 0.14f, 0.08f, 1f);
            pm.fillRectangle(4, ry + 3, s - 8, 1);
            // Crop plants
            for (int px = 5; px < s - 5; px += 3) {
                int plantH = 3 + rng.nextInt(3);
                float gr = 0.2f + rng.nextFloat() * 0.15f;
                float gg = 0.45f + rng.nextFloat() * 0.2f;
                for (int ph = 0; ph < plantH; ph++) {
                    setPixel(pm, px, ry - ph, gr, gg, 0.15f);
                }
                // Leaf tips
                if (rng.nextFloat() > 0.4f) {
                    setPixel(pm, px - 1, ry - plantH, gr + 0.05f, gg + 0.1f, 0.18f);
                    setPixel(pm, px + 1, ry - plantH, gr + 0.05f, gg + 0.1f, 0.18f);
                }
                // Berry/fruit
                if (rng.nextFloat() > 0.65f) {
                    setPixel(pm, px, ry - plantH - 1, 0.8f, 0.3f, 0.15f);
                }
            }
        }

        // Water pipe along left side
        pm.setColor(0.3f, 0.35f, 0.45f, 1f);
        pm.fillRectangle(1, roofH + 3, 2, s - roofH - 6);
        setPixel(pm, 1, roofH + 3, 0.4f, 0.45f, 0.55f);

        // Frame border
        pm.setColor(0.15f, 0.22f, 0.12f, 1f);
        pm.drawRectangle(0, 0, s, s);

        textures.put("building_FOOD_GROWER", new Texture(pm));
        pm.dispose();
    }

    private void generateWoodcutter() {
        int s = T - 8;
        Pixmap pm = new Pixmap(s, s, Pixmap.Format.RGBA8888);

        // Greenish-brown body
        for (int y = 0; y < s; y++) {
            for (int x = 0; x < s; x++) {
                float n = (rng.nextFloat() - 0.5f) * 0.04f;
                setPixel(pm, x, y, clamp(0.28f + n), clamp(0.22f + n), clamp(0.12f + n));
            }
        }

        // Shadow
        pm.setColor(0.1f, 0.08f, 0.04f, 1f);
        pm.fillRectangle(2, s - 4, s - 2, 4);

        // Building panel
        pm.setColor(0.32f, 0.26f, 0.14f, 1f);
        pm.fillRectangle(4, 4, s - 8, s - 12);
        pm.setColor(0.38f, 0.3f, 0.18f, 1f);
        pm.drawRectangle(4, 4, s - 8, s - 12);

        // Axe head detail
        int midX = s / 2;
        pm.setColor(0.55f, 0.5f, 0.4f, 1f);
        pm.fillRectangle(midX - 4, 6, 8, 6);
        pm.setColor(0.45f, 0.35f, 0.2f, 1f);
        pm.fillRectangle(midX - 1, 10, 2, 8);

        // Log stack at bottom
        pm.setColor(0.4f, 0.28f, 0.14f, 1f);
        pm.fillRectangle(4, s - 10, s - 8, 4);
        pm.setColor(0.35f, 0.22f, 0.1f, 1f);
        pm.drawLine(4, s - 8, s - 4, s - 8);

        // Border
        pm.setColor(0.18f, 0.14f, 0.07f, 1f);
        pm.drawRectangle(0, 0, s, s);

        textures.put("building_WOODCUTTER", new Texture(pm));
        pm.dispose();
    }

    private void generateBed() {
        int s = T - 8;
        Pixmap pm = new Pixmap(s, s, Pixmap.Format.RGBA8888);

        // Drop shadow
        pm.setColor(0.06f, 0.04f, 0.03f, 1f);
        pm.fillRectangle(3, 3, s - 3, s - 3);

        // Bed frame — warm wood
        pm.setColor(0.5f, 0.35f, 0.2f, 1f);
        pm.fillRectangle(2, 2, s - 4, s - 4);
        pm.setColor(0.38f, 0.25f, 0.12f, 1f);
        pm.drawRectangle(2, 2, s - 4, s - 4);

        // Headboard (top)
        pm.setColor(0.45f, 0.3f, 0.16f, 1f);
        pm.fillRectangle(2, 2, s - 4, 4);
        pm.setColor(0.55f, 0.38f, 0.2f, 1f);
        pm.drawRectangle(2, 2, s - 4, 4);
        // Headboard posts
        pm.setColor(0.48f, 0.33f, 0.18f, 1f);
        pm.fillRectangle(2, 1, 4, 3);
        pm.fillRectangle(s - 6, 1, 4, 3);

        // Mattress
        pm.setColor(0.88f, 0.85f, 0.78f, 1f);
        pm.fillRectangle(5, 7, s - 10, s - 12);
        // Mattress stitch lines
        pm.setColor(0.8f, 0.77f, 0.7f, 1f);
        pm.drawLine(s / 2, 7, s / 2, s - 6);

        // Pillow
        pm.setColor(0.95f, 0.93f, 0.88f, 1f);
        pm.fillRectangle(7, 8, s - 14, 8);
        pm.setColor(0.82f, 0.8f, 0.75f, 1f);
        pm.drawRectangle(7, 8, s - 14, 8);
        // Pillow puff highlights
        setPixel(pm, 9, 10, 1f, 0.98f, 0.95f);
        setPixel(pm, 10, 10, 1f, 0.98f, 0.95f);
        setPixel(pm, s - 12, 10, 1f, 0.98f, 0.95f);

        // Blanket (lower 2/3)
        pm.setColor(0.3f, 0.4f, 0.6f, 1f);
        pm.fillRectangle(5, 18, s - 10, s - 24);
        pm.setColor(0.25f, 0.35f, 0.52f, 1f);
        pm.drawRectangle(5, 18, s - 10, s - 24);
        // Blanket fold line
        pm.setColor(0.28f, 0.37f, 0.55f, 1f);
        pm.drawLine(5, 22, s - 5, 22);
        // Blanket wrinkle details
        pm.setColor(0.33f, 0.43f, 0.62f, 1f);
        pm.drawLine(8, 20, 12, 19);
        pm.drawLine(s - 12, 20, s - 8, 19);

        // Foot board (bottom)
        pm.setColor(0.42f, 0.28f, 0.15f, 1f);
        pm.fillRectangle(2, s - 6, s - 4, 3);
        pm.setColor(0.52f, 0.36f, 0.2f, 1f);
        pm.drawRectangle(2, s - 6, s - 4, 3);

        textures.put("building_BED", new Texture(pm));
        pm.dispose();
    }

    public Texture getBuildingTexture(String buildingType) {
        return textures.get("building_" + buildingType);
    }

    // ── Colonists ──

    private void generateColonistTextures() {
        generateHumanoid("colonist_alive",
                0.3f, 0.42f, 0.72f, 0.22f, 0.32f, 0.55f,
                0.72f, 0.56f, 0.43f, 0.25f, 0.2f, 0.15f, false);
        generateHumanoid("colonist_possessed",
                0.85f, 0.7f, 0.15f, 0.7f, 0.55f, 0.1f,
                0.72f, 0.56f, 0.43f, 0.3f, 0.22f, 0.12f, true);
        generateDeadColonist();
    }

    private void generateHumanoid(String key,
                                   float sr, float sg, float sb,
                                   float sdr, float sdg, float sdb,
                                   float skinR, float skinG, float skinB,
                                   float pantsR, float pantsG, float pantsB,
                                   boolean isLeader) {
        int s = T - 16;
        Pixmap pm = new Pixmap(s, s, Pixmap.Format.RGBA8888);
        int cx = s / 2;

        // Drop shadow ellipse
        pm.setColor(0f, 0f, 0f, 0.2f);
        pm.fillCircle(cx, s - 4, 8);

        // Legs
        int legTop = s - 15;
        pm.setColor(pantsR, pantsG, pantsB, 1f);
        pm.fillRectangle(cx - 5, legTop, 3, 10);
        pm.fillRectangle(cx + 2, legTop, 3, 10);
        // Shoes
        pm.setColor(pantsR * 0.6f, pantsG * 0.6f, pantsB * 0.6f, 1f);
        pm.fillRectangle(cx - 5, legTop + 8, 4, 3);
        pm.fillRectangle(cx + 2, legTop + 8, 4, 3);

        // Torso
        int torsoTop = legTop - 12;
        pm.setColor(sr, sg, sb, 1f);
        pm.fillRectangle(cx - 6, torsoTop, 12, 13);
        // Torso shadow (right half)
        pm.setColor(sdr, sdg, sdb, 1f);
        pm.fillRectangle(cx + 1, torsoTop + 1, 5, 11);
        // Belt
        pm.setColor(pantsR * 0.5f, pantsG * 0.5f, pantsB * 0.5f, 1f);
        pm.fillRectangle(cx - 6, torsoTop + 11, 12, 2);
        pm.setColor(0.6f, 0.55f, 0.3f, 1f);
        pm.drawPixel(cx, torsoTop + 11);
        pm.drawPixel(cx, torsoTop + 12);

        // Arms
        pm.setColor(sr, sg, sb, 1f);
        pm.fillRectangle(cx - 9, torsoTop + 2, 3, 9);
        pm.setColor(sdr, sdg, sdb, 1f);
        pm.fillRectangle(cx + 6, torsoTop + 2, 3, 9);
        // Hands
        pm.setColor(skinR, skinG, skinB, 1f);
        pm.fillRectangle(cx - 9, torsoTop + 9, 3, 3);
        pm.fillRectangle(cx + 6, torsoTop + 9, 3, 3);

        // Neck
        pm.setColor(skinR, skinG, skinB, 1f);
        pm.fillRectangle(cx - 2, torsoTop - 2, 4, 3);

        // Head
        int headY = torsoTop - 8;
        int headR = 6;
        for (int y = headY - headR; y <= headY + headR; y++) {
            for (int x = cx - headR; x <= cx + headR; x++) {
                float dx = x - cx;
                float dy = y - headY;
                if (dx * dx + dy * dy <= headR * headR) {
                    float shade = (dx > 1) ? -0.04f : 0.02f;
                    pm.setColor(clamp(skinR + shade), clamp(skinG + shade), clamp(skinB + shade), 1f);
                    pm.drawPixel(x, y);
                }
            }
        }
        // Hair
        pm.setColor(0.15f, 0.1f, 0.07f, 1f);
        for (int x = cx - 5; x <= cx + 5; x++) {
            for (int y = headY - headR; y < headY - headR + 4; y++) {
                float dx = x - cx;
                float dy = y - headY;
                if (dx * dx + dy * dy <= headR * headR) {
                    pm.drawPixel(x, y);
                }
            }
        }
        // Eyes
        pm.setColor(0.1f, 0.1f, 0.1f, 1f);
        pm.drawPixel(cx - 2, headY);
        pm.drawPixel(cx + 2, headY);

        // Leader crown/marker
        if (isLeader) {
            pm.setColor(1f, 0.9f, 0.2f, 1f);
            pm.fillRectangle(cx - 4, headY - headR - 3, 8, 2);
            pm.drawPixel(cx - 3, headY - headR - 4);
            pm.drawPixel(cx, headY - headR - 5);
            pm.drawPixel(cx + 3, headY - headR - 4);
        }

        textures.put(key, new Texture(pm));
        pm.dispose();
    }

    private void generateDeadColonist() {
        int s = T - 16;
        Pixmap pm = new Pixmap(s, s, Pixmap.Format.RGBA8888);
        int cx = s / 2, cy = s / 2;

        // Blood/shadow pool
        pm.setColor(0.15f, 0.08f, 0.06f, 0.4f);
        pm.fillCircle(cx, cy + 2, 10);

        // Horizontal body (collapsed figure)
        pm.setColor(0.35f, 0.3f, 0.28f, 1f);
        pm.fillRectangle(cx - 9, cy - 3, 18, 7);
        // Legs (angled out)
        pm.setColor(0.25f, 0.2f, 0.16f, 1f);
        pm.fillRectangle(cx + 8, cy - 1, 7, 3);
        pm.fillRectangle(cx + 7, cy + 2, 6, 3);
        // Head
        pm.setColor(0.5f, 0.4f, 0.32f, 1f);
        pm.fillCircle(cx - 10, cy, 4);
        // Arm
        pm.setColor(0.35f, 0.3f, 0.28f, 1f);
        pm.fillRectangle(cx - 4, cy - 6, 3, 4);
        // X mark
        pm.setColor(0.6f, 0.15f, 0.1f, 1f);
        pm.drawLine(cx - 13, cy - 3, cx - 7, cy + 3);
        pm.drawLine(cx - 7, cy - 3, cx - 13, cy + 3);

        textures.put("colonist_dead", new Texture(pm));
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
        generateWoodItem();
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

    private void generateWoodItem() {
        int s = 12;
        Pixmap pm = new Pixmap(s, s, Pixmap.Format.RGBA8888);
        // Horizontal brown log
        int logH = 4;
        int logY = (s - logH) / 2;
        // Log body
        pm.setColor(0.45f, 0.28f, 0.1f, 1f);
        pm.fillRectangle(1, logY, s - 2, logH);
        // Log end caps
        pm.setColor(0.38f, 0.22f, 0.08f, 1f);
        pm.fillRectangle(1, logY, 2, logH);
        pm.fillRectangle(s - 3, logY, 2, logH);
        // Grain lines
        pm.setColor(0.38f, 0.22f, 0.08f, 1f);
        pm.drawLine(3, logY + 1, s - 4, logY + 1);
        pm.drawLine(3, logY + 3, s - 4, logY + 3);
        // Highlight
        pm.setColor(0.55f, 0.35f, 0.14f, 1f);
        pm.drawLine(2, logY, s - 3, logY);
        textures.put("item_WOOD", new Texture(pm));
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
