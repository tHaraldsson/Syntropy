package com.haraldsson.syntropy.core;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.haraldsson.syntropy.entities.Colonist;
import com.haraldsson.syntropy.entities.Item;
import com.haraldsson.syntropy.entities.ItemType;
import com.haraldsson.syntropy.entities.Miner;
import com.haraldsson.syntropy.input.PlayerController;
import com.haraldsson.syntropy.systems.TaskSystem;
import com.haraldsson.syntropy.world.Tile;
import com.haraldsson.syntropy.world.World;
import com.haraldsson.syntropy.world.WorldGenerator;

public class GameApp extends ApplicationAdapter {
    public static final int TILE_SIZE = 48;
    private static final int WORLD_WIDTH = 10;
    private static final int WORLD_HEIGHT = 10;

    private OrthographicCamera camera;
    private OrthographicCamera hudCamera;
    private Viewport viewport;
    private ShapeRenderer shapeRenderer;
    private SpriteBatch spriteBatch;
    private BitmapFont font;
    private BitmapFont smallFont;

    private World world;
    private PlayerController playerController;
    private TaskSystem taskSystem;

    @Override
    public void create() {
        camera = new OrthographicCamera();
        hudCamera = new OrthographicCamera();
        viewport = new FitViewport(800, 600, camera);
        viewport.apply();

        shapeRenderer = new ShapeRenderer();
        spriteBatch = new SpriteBatch();
        font = new BitmapFont();
        smallFont = new BitmapFont();
        smallFont.getData().setScale(0.7f);

        world = WorldGenerator.generate(WORLD_WIDTH, WORLD_HEIGHT);
        playerController = new PlayerController(world, camera, viewport, TILE_SIZE);
        taskSystem = new TaskSystem();

        centerCameraOnWorld();
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height);
        hudCamera.setToOrtho(false, width, height);
        centerCameraOnWorld();
    }

    @Override
    public void render() {
        float delta = Gdx.graphics.getDeltaTime();

        playerController.update(delta);
        taskSystem.update(world, delta);
        world.update(delta);

        renderWorld();
        renderHUD();
    }

    @Override
    public void dispose() {
        shapeRenderer.dispose();
        spriteBatch.dispose();
        font.dispose();
        smallFont.dispose();
    }

    private void centerCameraOnWorld() {
        float worldPixelWidth = WORLD_WIDTH * TILE_SIZE;
        float worldPixelHeight = WORLD_HEIGHT * TILE_SIZE;
        camera.position.set(worldPixelWidth / 2f, worldPixelHeight / 2f, 0f);
        camera.update();
    }

    // ── World rendering ──────────────────────────────────────────────

    private void renderWorld() {
        Gdx.gl.glClearColor(0.06f, 0.06f, 0.08f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        camera.update();
        shapeRenderer.setProjectionMatrix(camera.combined);

        // Filled pass
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        renderTiles();
        renderBuildings();
        renderGroundItems();
        renderColonists();
        shapeRenderer.end();

        // Grid lines
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(0.08f, 0.08f, 0.08f, 1f);
        for (int y = 0; y < world.getHeight(); y++) {
            for (int x = 0; x < world.getWidth(); x++) {
                shapeRenderer.rect(x * TILE_SIZE, y * TILE_SIZE, TILE_SIZE, TILE_SIZE);
            }
        }
        shapeRenderer.end();

        // World-space text (names, tasks, possession marker)
        spriteBatch.setProjectionMatrix(camera.combined);
        spriteBatch.begin();
        for (Colonist colonist : world.getColonists()) {
            float cx = colonist.getX() * TILE_SIZE;
            float cy = colonist.getY() * TILE_SIZE;
            if (colonist.isDead()) {
                smallFont.setColor(Color.GRAY);
                smallFont.draw(spriteBatch, colonist.getName() + " (dead)", cx - 4, cy + TILE_SIZE + 10);
            } else {
                smallFont.setColor(Color.WHITE);
                smallFont.draw(spriteBatch, colonist.getName(), cx + 4, cy + TILE_SIZE + 10);
                smallFont.setColor(0.7f, 0.7f, 0.7f, 1f);
                smallFont.draw(spriteBatch, colonist.getTaskType().name(), cx + 4, cy + TILE_SIZE + 22);
            }
        }
        Colonist possessed = playerController.getPossessed();
        if (possessed != null) {
            font.setColor(Color.RED);
            float px = possessed.getX() * TILE_SIZE;
            float py = possessed.getY() * TILE_SIZE;
            font.draw(spriteBatch, "[P]", px + 10, py + TILE_SIZE + 34);
            font.setColor(Color.WHITE);
        }
        spriteBatch.end();
    }

    private void renderTiles() {
        for (int y = 0; y < world.getHeight(); y++) {
            for (int x = 0; x < world.getWidth(); x++) {
                float px = x * TILE_SIZE;
                float py = y * TILE_SIZE;
                shapeRenderer.setColor(0.15f, 0.2f, 0.15f, 1f);
                shapeRenderer.rect(px, py, TILE_SIZE, TILE_SIZE);

                Tile tile = world.getTile(x, y);
                if (tile.isStockpile()) {
                    shapeRenderer.setColor(0.12f, 0.2f, 0.4f, 1f);
                    shapeRenderer.rect(px + 4, py + 4, TILE_SIZE - 8, TILE_SIZE - 8);
                }
            }
        }
    }

    private void renderBuildings() {
        for (Miner miner : world.getMiners()) {
            float px = miner.getX() * TILE_SIZE;
            float py = miner.getY() * TILE_SIZE;
            shapeRenderer.setColor(0.35f, 0.35f, 0.35f, 1f);
            shapeRenderer.rect(px + 6, py + 6, TILE_SIZE - 12, TILE_SIZE - 12);
            // Small indicator for output count
            int count = Math.min(miner.getOutputCount(), 5);
            for (int i = 0; i < count; i++) {
                shapeRenderer.setColor(0.55f, 0.55f, 0.55f, 1f);
                shapeRenderer.rect(px + 10 + i * 6, py + 10, 4, 4);
            }
        }
    }

    private void renderGroundItems() {
        for (int y = 0; y < world.getHeight(); y++) {
            for (int x = 0; x < world.getWidth(); x++) {
                Tile tile = world.getTile(x, y);
                if (tile.getGroundItems().isEmpty()) continue;
                float baseX = x * TILE_SIZE + 8;
                float baseY = y * TILE_SIZE + 8;
                for (int i = 0; i < tile.getGroundItems().size(); i++) {
                    Item item = tile.getGroundItems().get(i);
                    if (item.getType() == ItemType.STONE) {
                        shapeRenderer.setColor(0.6f, 0.6f, 0.6f, 1f);
                    } else {
                        shapeRenderer.setColor(0.85f, 0.75f, 0.25f, 1f);
                    }
                    shapeRenderer.rect(baseX + i * 8f, baseY, 6, 6);
                }
            }
        }
    }

    private void renderColonists() {
        for (Colonist colonist : world.getColonists()) {
            float cx = colonist.getX() * TILE_SIZE;
            float cy = colonist.getY() * TILE_SIZE;

            if (colonist.isDead()) {
                shapeRenderer.setColor(0.3f, 0.1f, 0.1f, 1f);
                shapeRenderer.rect(cx + 14, cy + 14, TILE_SIZE - 28, TILE_SIZE - 28);
                continue;
            }

            if (colonist.isAiDisabled()) {
                shapeRenderer.setColor(0.9f, 0.25f, 0.25f, 1f);
            } else {
                shapeRenderer.setColor(0.95f, 0.6f, 0.2f, 1f);
            }
            shapeRenderer.rect(cx + 12, cy + 12, TILE_SIZE - 24, TILE_SIZE - 24);

            // Carried item
            if (colonist.getCarriedItem() != null) {
                if (colonist.getCarriedItem().getType() == ItemType.STONE) {
                    shapeRenderer.setColor(0.6f, 0.6f, 0.6f, 1f);
                } else {
                    shapeRenderer.setColor(0.85f, 0.75f, 0.25f, 1f);
                }
                shapeRenderer.rect(cx + 18, cy + 18, 6, 6);
            }

            // Need bars: hunger, energy, mood
            float barY = cy + TILE_SIZE - 6;
            float barWidth = TILE_SIZE - 16;
            drawBar(cx + 8, barY, barWidth, colonist.getHunger() / 100f, 0.2f, 0.8f, 0.3f);
            drawBar(cx + 8, barY - 5, barWidth, colonist.getEnergy() / 100f, 0.3f, 0.5f, 0.9f);
            drawBar(cx + 8, barY - 10, barWidth, colonist.getMood() / 100f, 0.9f, 0.8f, 0.2f);
        }
    }

    private void drawBar(float x, float y, float maxWidth, float ratio, float r, float g, float b) {
        shapeRenderer.setColor(Color.DARK_GRAY);
        shapeRenderer.rect(x, y, maxWidth, 3);
        shapeRenderer.setColor(r, g, b, 1f);
        shapeRenderer.rect(x, y, maxWidth * ratio, 3);
    }

    // ── HUD rendering (screen-space) ─────────────────────────────────

    private void renderHUD() {
        hudCamera.update();
        spriteBatch.setProjectionMatrix(hudCamera.combined);
        spriteBatch.begin();

        float screenW = Gdx.graphics.getWidth();
        float screenH = Gdx.graphics.getHeight();

        // Top-left: resource info
        int totalStone = 0;
        int totalFood = 0;
        Tile stockpile = world.getStockpileTile();
        if (stockpile != null) {
            totalStone = stockpile.countItems(ItemType.STONE);
            totalFood = stockpile.countItems(ItemType.FOOD);
        }
        font.setColor(Color.WHITE);
        font.draw(spriteBatch, "Stockpile  Stone: " + totalStone + "  Food: " + totalFood, 10, screenH - 10);

        // Top-left: miner info
        float yOffset = screenH - 30;
        for (int i = 0; i < world.getMiners().size(); i++) {
            Miner m = world.getMiners().get(i);
            font.draw(spriteBatch, "Miner " + (i + 1) + " output: " + m.getOutputCount(), 10, yOffset);
            yOffset -= 18;
        }

        // Top-right: colonist list
        float rightX = screenW - 200;
        float listY = screenH - 10;
        font.setColor(Color.LIGHT_GRAY);
        font.draw(spriteBatch, "-- Colonists --", rightX, listY);
        listY -= 18;
        for (Colonist c : world.getColonists()) {
            if (c.isDead()) {
                font.setColor(Color.GRAY);
                font.draw(spriteBatch, c.getName() + " (DEAD)", rightX, listY);
            } else {
                boolean isPossessed = c == playerController.getPossessed();
                font.setColor(isPossessed ? Color.RED : Color.WHITE);
                String status = c.getName() + " H:" + (int) c.getHunger()
                        + " E:" + (int) c.getEnergy()
                        + " M:" + (int) c.getMood();
                font.draw(spriteBatch, status, rightX, listY);
                listY -= 14;
                smallFont.setColor(0.7f, 0.7f, 0.7f, 1f);
                smallFont.draw(spriteBatch, "  Task: " + c.getTaskType().name(), rightX, listY);
            }
            listY -= 18;
        }

        // Bottom-left: controls help
        smallFont.setColor(0.6f, 0.6f, 0.6f, 1f);
        smallFont.draw(spriteBatch, "[P] Possess/Unpossess   [WASD] Move (possessed)   [Arrows] Pan camera   [Scroll] Zoom", 10, 20);

        // Possession banner
        Colonist possessed = playerController.getPossessed();
        if (possessed != null) {
            font.setColor(Color.RED);
            font.draw(spriteBatch, "POSSESSING: " + possessed.getName(), 10, screenH - 80);
            font.setColor(Color.WHITE);
        }

        spriteBatch.end();
    }
}
