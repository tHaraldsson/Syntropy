package com.haraldsson.syntropy.entities;

import com.haraldsson.syntropy.world.World;

public abstract class Building {
    private final int x;
    private final int y;
    private boolean built = true;

    protected Building(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public abstract void update(float delta, World world);

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public boolean isBuilt() {
        return built;
    }

    public void setBuilt(boolean built) {
        this.built = built;
    }
}

