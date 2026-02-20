package com.haraldsson.syntropy.world;

public enum TerrainType {
    WATER(0.1f, 0.2f, 0.45f),
    SAND(0.6f, 0.55f, 0.35f),
    GRASS(0.15f, 0.2f, 0.15f),
    DIRT(0.3f, 0.22f, 0.12f),
    STONE(0.35f, 0.33f, 0.3f);

    public final float r, g, b;

    TerrainType(float r, float g, float b) {
        this.r = r;
        this.g = g;
        this.b = b;
    }
}

