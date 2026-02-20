package com.haraldsson.syntropy.core;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;

public class GameMain {

    public static void main(String[] args) {
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("Syntropy MVP");
        config.setWindowedMode(800, 600);
        config.useVsync(true);
        new Lwjgl3Application(new GameApp(), config);
    }
}

