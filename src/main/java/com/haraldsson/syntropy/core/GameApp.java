package com.haraldsson.syntropy.core;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.haraldsson.syntropy.ecs.ECSWorld;
import com.haraldsson.syntropy.ecs.Entity;
import com.haraldsson.syntropy.ecs.components.*;
import com.haraldsson.syntropy.ecs.systems.AITaskSystem;
import com.haraldsson.syntropy.ecs.systems.BuildingProductionSystem;
import com.haraldsson.syntropy.ecs.systems.NeedsSystem;
import com.haraldsson.syntropy.entities.Item;
import com.haraldsson.syntropy.entities.ItemType;
import com.haraldsson.syntropy.input.PlayerController;
import com.haraldsson.syntropy.systems.EventSystem;
import com.haraldsson.syntropy.systems.ResearchSystem;
import com.haraldsson.syntropy.systems.Technology;
import com.haraldsson.syntropy.world.Tile;
import com.haraldsson.syntropy.world.World;
import com.haraldsson.syntropy.world.WorldGenerator;

import java.util.List;

public class GameApp extends ApplicationAdapter {
    public static final int TILE_SIZE = 48;
    private static final int WORLD_WIDTH = 30;
    private static final int WORLD_HEIGHT = 30;

    private OrthographicCamera camera;
    private OrthographicCamera hudCamera;
    private Viewport viewport;
    private ShapeRenderer shapeRenderer;
    private SpriteBatch spriteBatch;
    private BitmapFont font;
    private BitmapFont smallFont;

    private World world;
    private ECSWorld ecsWorld;
    private PlayerController playerController;
    private ResearchSystem researchSystem;
    private EventSystem eventSystem;

    // ECS systems
    private NeedsSystem needsSystem;
    private BuildingProductionSystem buildingProductionSystem;
    private AITaskSystem aiTaskSystem;

    private String statusMessage = "";
    private float statusTimer;

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

        WorldGenerator.GenerationResult result = WorldGenerator.generate(WORLD_WIDTH, WORLD_HEIGHT);
        world = result.world;
        ecsWorld = result.ecsWorld;

        playerController = new PlayerController(world, ecsWorld, camera, viewport, TILE_SIZE);
        researchSystem = new ResearchSystem();
        eventSystem = new EventSystem();
        needsSystem = new NeedsSystem();
        buildingProductionSystem = new BuildingProductionSystem();
        aiTaskSystem = new AITaskSystem();

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

        handleSaveLoad();
        handleResearchInput();

        playerController.update(delta);
        aiTaskSystem.update(ecsWorld, world, delta);
        needsSystem.update(ecsWorld, world, delta);
        buildingProductionSystem.update(ecsWorld, world, delta);
        researchSystem.update(delta);
        eventSystem.update(ecsWorld, world, delta);

        // Clamp colonist positions
        for (Entity e : ecsWorld.getEntitiesWith(PositionComponent.class, AIComponent.class)) {
            PositionComponent pos = e.get(PositionComponent.class);
            float[] xy = {pos.x, pos.y};
            world.clampPosition(xy);
            pos.x = xy[0];
            pos.y = xy[1];
        }

        if (statusTimer > 0) statusTimer -= delta;

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

    private void handleSaveLoad() {
        // Save/load temporarily disabled during ECS refactor
        // TODO: Rewrite SaveLoadSystem for ECS
    }

    private void handleResearchInput() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.R)) {
            researchSystem.startNextResearch();
            Technology current = researchSystem.getCurrentResearch();
            if (current != null && !current.isUnlocked()) {
                showStatus("Researching: " + current.getName());
            } else {
                showStatus("All research complete!");
            }
        }
    }

    private void showStatus(String msg) {
        statusMessage = msg;
        statusTimer = 3f;
    }

    private void centerCameraOnWorld() {
        float worldPixelWidth = world.getWidth() * TILE_SIZE;
        float worldPixelHeight = world.getHeight() * TILE_SIZE;
        camera.position.set(worldPixelWidth / 2f, worldPixelHeight / 2f, 0f);
        camera.update();
    }

    // ── World rendering ──────────────────────────────────────────────

    private void renderWorld() {
        Gdx.gl.glClearColor(0.06f, 0.06f, 0.08f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        camera.update();
        shapeRenderer.setProjectionMatrix(camera.combined);

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        renderTiles();
        renderBuildings();
        renderGroundItems();
        renderColonists();
        shapeRenderer.end();

        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(0.08f, 0.08f, 0.08f, 0.3f);
        for (int y = 0; y < world.getHeight(); y++) {
            for (int x = 0; x < world.getWidth(); x++) {
                shapeRenderer.rect(x * TILE_SIZE, y * TILE_SIZE, TILE_SIZE, TILE_SIZE);
            }
        }
        shapeRenderer.end();

        // World-space text
        spriteBatch.setProjectionMatrix(camera.combined);
        spriteBatch.begin();
        for (Entity e : ecsWorld.getEntitiesWith(IdentityComponent.class, PositionComponent.class, HealthComponent.class)) {
            IdentityComponent id = e.get(IdentityComponent.class);
            PositionComponent pos = e.get(PositionComponent.class);
            HealthComponent health = e.get(HealthComponent.class);
            float cx = pos.x * TILE_SIZE;
            float cy = pos.y * TILE_SIZE;
            if (health.dead) {
                smallFont.setColor(Color.GRAY);
                smallFont.draw(spriteBatch, id.name + " (dead)", cx - 4, cy + TILE_SIZE + 10);
            } else {
                smallFont.setColor(Color.WHITE);
                smallFont.draw(spriteBatch, id.name, cx + 4, cy + TILE_SIZE + 10);
                AIComponent ai = e.get(AIComponent.class);
                if (ai != null) {
                    smallFont.setColor(0.7f, 0.7f, 0.7f, 1f);
                    smallFont.draw(spriteBatch, ai.taskType.name(), cx + 4, cy + TILE_SIZE + 22);
                }
            }
        }
        Entity possessed = playerController.getPossessed();
        if (possessed != null) {
            PositionComponent pp = possessed.get(PositionComponent.class);
            font.setColor(Color.RED);
            font.draw(spriteBatch, "[P]", pp.x * TILE_SIZE + 10, pp.y * TILE_SIZE + TILE_SIZE + 34);
            font.setColor(Color.WHITE);
        }
        spriteBatch.end();
    }

    private void renderTiles() {
        for (int y = 0; y < world.getHeight(); y++) {
            for (int x = 0; x < world.getWidth(); x++) {
                float px = x * TILE_SIZE;
                float py = y * TILE_SIZE;
                Tile tile = world.getTile(x, y);
                shapeRenderer.setColor(tile.getTerrainType().r, tile.getTerrainType().g, tile.getTerrainType().b, 1f);
                shapeRenderer.rect(px, py, TILE_SIZE, TILE_SIZE);
                if (tile.isStockpile()) {
                    shapeRenderer.setColor(0.12f, 0.2f, 0.4f, 1f);
                    shapeRenderer.rect(px + 4, py + 4, TILE_SIZE - 8, TILE_SIZE - 8);
                }
            }
        }
    }

    private void renderBuildings() {
        for (Entity e : ecsWorld.getEntitiesWith(BuildingComponent.class, PositionComponent.class)) {
            BuildingComponent bc = e.get(BuildingComponent.class);
            PositionComponent pos = e.get(PositionComponent.class);
            float px = pos.x * TILE_SIZE;
            float py = pos.y * TILE_SIZE;

            if ("MINER".equals(bc.buildingType)) {
                shapeRenderer.setColor(0.35f, 0.35f, 0.35f, 1f);
            } else {
                shapeRenderer.setColor(0.25f, 0.45f, 0.2f, 1f);
            }
            shapeRenderer.rect(px + 6, py + 6, TILE_SIZE - 12, TILE_SIZE - 12);

            int count = Math.min(bc.getOutputCount(), 5);
            for (int i = 0; i < count; i++) {
                if ("MINER".equals(bc.buildingType)) {
                    shapeRenderer.setColor(0.55f, 0.55f, 0.55f, 1f);
                } else {
                    shapeRenderer.setColor(0.85f, 0.75f, 0.25f, 1f);
                }
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
        for (Entity e : ecsWorld.getEntitiesWith(PositionComponent.class, HealthComponent.class, NeedsComponent.class)) {
            PositionComponent pos = e.get(PositionComponent.class);
            HealthComponent health = e.get(HealthComponent.class);
            NeedsComponent needs = e.get(NeedsComponent.class);
            AIComponent ai = e.get(AIComponent.class);
            InventoryComponent inv = e.get(InventoryComponent.class);

            float cx = pos.x * TILE_SIZE;
            float cy = pos.y * TILE_SIZE;

            if (health.dead) {
                shapeRenderer.setColor(0.3f, 0.1f, 0.1f, 1f);
                shapeRenderer.rect(cx + 14, cy + 14, TILE_SIZE - 28, TILE_SIZE - 28);
                continue;
            }

            if (ai != null && ai.aiDisabled) {
                shapeRenderer.setColor(0.9f, 0.25f, 0.25f, 1f);
            } else {
                shapeRenderer.setColor(0.95f, 0.6f, 0.2f, 1f);
            }
            shapeRenderer.rect(cx + 12, cy + 12, TILE_SIZE - 24, TILE_SIZE - 24);

            if (inv != null && inv.carriedItem != null) {
                if (inv.carriedItem.getType() == ItemType.STONE) {
                    shapeRenderer.setColor(0.6f, 0.6f, 0.6f, 1f);
                } else {
                    shapeRenderer.setColor(0.85f, 0.75f, 0.25f, 1f);
                }
                shapeRenderer.rect(cx + 18, cy + 18, 6, 6);
            }

            float barY = cy + TILE_SIZE - 6;
            float barWidth = TILE_SIZE - 16;
            drawBar(cx + 8, barY, barWidth, needs.hunger / 100f, 0.2f, 0.8f, 0.3f);
            drawBar(cx + 8, barY - 5, barWidth, needs.energy / 100f, 0.3f, 0.5f, 0.9f);
            drawBar(cx + 8, barY - 10, barWidth, needs.mood / 100f, 0.9f, 0.8f, 0.2f);
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

        int totalStone = 0;
        int totalFood = 0;
        Tile stockpile = world.getStockpileTile();
        if (stockpile != null) {
            totalStone = stockpile.countItems(ItemType.STONE);
            totalFood = stockpile.countItems(ItemType.FOOD);
        }
        font.setColor(Color.WHITE);
        font.draw(spriteBatch, "Stockpile  Stone: " + totalStone + "  Food: " + totalFood, 10, screenH - 10);

        float yOffset = screenH - 30;
        int minerIdx = 1, growerIdx = 1;
        for (Entity e : ecsWorld.getEntitiesWith(BuildingComponent.class)) {
            BuildingComponent bc = e.get(BuildingComponent.class);
            if ("MINER".equals(bc.buildingType)) {
                font.draw(spriteBatch, "Miner " + minerIdx++ + " output: " + bc.getOutputCount(), 10, yOffset);
            } else {
                font.draw(spriteBatch, "FoodGrower " + growerIdx++ + " output: " + bc.getOutputCount(), 10, yOffset);
            }
            yOffset -= 18;
        }

        float rightX = screenW - 220;
        float listY = screenH - 10;
        font.setColor(Color.LIGHT_GRAY);
        font.draw(spriteBatch, "-- Colonists --", rightX, listY);
        listY -= 18;
        Entity possessedEntity = playerController.getPossessed();
        for (Entity e : ecsWorld.getEntitiesWith(IdentityComponent.class, NeedsComponent.class, HealthComponent.class)) {
            IdentityComponent id = e.get(IdentityComponent.class);
            HealthComponent health = e.get(HealthComponent.class);
            NeedsComponent needs = e.get(NeedsComponent.class);
            AIComponent ai = e.get(AIComponent.class);
            if (health.dead) {
                font.setColor(Color.GRAY);
                font.draw(spriteBatch, id.name + " (DEAD)", rightX, listY);
            } else {
                boolean isPossessed = e == possessedEntity;
                font.setColor(isPossessed ? Color.RED : Color.WHITE);
                String status = id.name + " H:" + (int) needs.hunger
                        + " E:" + (int) needs.energy
                        + " M:" + (int) needs.mood;
                font.draw(spriteBatch, status, rightX, listY);
                if (ai != null) {
                    listY -= 14;
                    smallFont.setColor(0.7f, 0.7f, 0.7f, 1f);
                    smallFont.draw(spriteBatch, "  Task: " + ai.taskType.name(), rightX, listY);
                }
            }
            listY -= 18;
        }

        smallFont.setColor(0.6f, 0.6f, 0.6f, 1f);
        smallFont.draw(spriteBatch, "[DblClick] Possess  [WASD] Move/Pan  [E] Pickup/Drop  [R] Research  [Scroll] Zoom", 10, 20);

        if (possessedEntity != null) {
            IdentityComponent pid = possessedEntity.get(IdentityComponent.class);
            font.setColor(Color.RED);
            font.draw(spriteBatch, "POSSESSING: " + (pid != null ? pid.name : "???"), 10, screenH - 80);
            font.setColor(Color.WHITE);
        }

        Technology currentTech = researchSystem.getCurrentResearch();
        if (currentTech != null) {
            float researchY = screenH - 100;
            if (currentTech.isUnlocked()) {
                font.setColor(Color.GREEN);
                font.draw(spriteBatch, "Researched: " + currentTech.getName() + " done", 10, researchY);
            } else {
                font.setColor(0.6f, 0.8f, 1f, 1f);
                int pct = (int) (currentTech.getProgressRatio() * 100);
                font.draw(spriteBatch, "Researching: " + currentTech.getName() + " " + pct + "%", 10, researchY);
            }
            font.setColor(Color.WHITE);
        }

        List<String> events = eventSystem.getEventLog();
        float eventY = 60;
        smallFont.setColor(0.9f, 0.7f, 0.3f, 1f);
        for (int i = events.size() - 1; i >= 0 && i >= events.size() - 3; i--) {
            smallFont.draw(spriteBatch, events.get(i), 10, eventY);
            eventY += 14;
        }

        String pickupMsg = playerController.getPickupMessage();
        if (!pickupMsg.isEmpty()) {
            font.setColor(Color.CYAN);
            font.draw(spriteBatch, pickupMsg, 10, screenH - 120);
            font.setColor(Color.WHITE);
        }

        if (statusTimer > 0) {
            font.setColor(Color.YELLOW);
            font.draw(spriteBatch, statusMessage, screenW / 2f - 60, screenH / 2f);
            font.setColor(Color.WHITE);
        }

        spriteBatch.end();
    }
}
