package com.haraldsson.syntropy.world;

public enum TerrainType {
    WATER(0.06f, 0.12f, 0.28f),
    SAND(0.52f, 0.45f, 0.28f),
    GRASS(0.12f, 0.2f, 0.08f),
    DIRT(0.25f, 0.18f, 0.1f),
    STONE(0.3f, 0.28f, 0.25f);

    public final float r, g, b;

    TerrainType(float r, float g, float b) {
        this.r = r;
        this.g = g;
        this.b = b;
    }
}

