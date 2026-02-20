package com.haraldsson.syntropy.core;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.haraldsson.syntropy.ecs.Entity;
import com.haraldsson.syntropy.ecs.components.*;
import com.haraldsson.syntropy.ecs.systems.*;
import com.haraldsson.syntropy.entities.Item;
import com.haraldsson.syntropy.input.PlayerController;
import com.haraldsson.syntropy.systems.EventSystem;
import com.haraldsson.syntropy.systems.Technology;
import com.haraldsson.syntropy.world.Tile;
import com.haraldsson.syntropy.world.WorldGenerator;

public class GameApp extends ApplicationAdapter {
    public static final int TILE_SIZE = 64;
    private static final int WORLD_WIDTH = 50;
    private static final int WORLD_HEIGHT = 50;

    private OrthographicCamera camera;
    private Viewport viewport;
    private SpriteBatch spriteBatch;
    private BitmapFont smallFont;
    private ShapeRenderer shapeRenderer;

    private SpriteManager spriteManager;
    private GameHud gameHud;

    // Pattern 5 — all game state in one root object
    private GameState gameState;

    private PlayerController playerController;
    private EventSystem eventSystem;

    private NeedsSystem needsSystem;
    private BuildingProductionSystem buildingProductionSystem;
    private AITaskSystem aiTaskSystem;
    private AgingSystem agingSystem;
    private MoodSystem moodSystem;

    private String statusMessage = "";
    private float statusTimer;

    // Succession UI state (FIX 13)
    private boolean pendingSuccession = false;
    private java.util.List<Entity> successionCandidates = new java.util.ArrayList<>();

    @Override
    public void create() {
        camera = new OrthographicCamera();
        viewport = new FitViewport(800, 600, camera);
        viewport.apply();

        spriteBatch = new SpriteBatch();
        smallFont = new BitmapFont();
        smallFont.getData().setScale(0.7f);
        shapeRenderer = new ShapeRenderer();

        spriteManager = new SpriteManager(TILE_SIZE);
        gameHud = new GameHud();

        WorldGenerator.GenerationResult result = WorldGenerator.generate(WORLD_WIDTH, WORLD_HEIGHT);
        gameState = new GameState(result.world, result.ecsWorld);

        playerController = new PlayerController(gameState.world, gameState.ecsWorld, camera, viewport, TILE_SIZE);
        eventSystem = new EventSystem();
        needsSystem = new NeedsSystem();
        buildingProductionSystem = new BuildingProductionSystem();
        aiTaskSystem = new AITaskSystem();
        agingSystem = new AgingSystem();
        moodSystem = new MoodSystem();

        // Wire event bus listeners
        wireEventBus();

        InputMultiplexer multiplexer = new InputMultiplexer();
        multiplexer.addProcessor(gameHud.getStage());
        multiplexer.addProcessor(new InputAdapter() {
            @Override
            public boolean scrolled(float amountX, float amountY) {
                camera.zoom = MathUtils.clamp(camera.zoom + amountY * 0.1f, 0.4f, 2.5f);
                return true;
            }
        });
        Gdx.input.setInputProcessor(multiplexer);

        centerCameraOnWorld();
    }

    private void wireEventBus() {
        GameEvents events = gameState.events;

        events.on(EventType.COLONIST_DIED, payload -> {
            String name = payload instanceof String ? (String) payload : "A colonist";
            events.log("DEATH: " + name + " has died.");
        });

        events.on(EventType.LEADER_DIED, payload -> {
            String name = payload instanceof String ? (String) payload : "The leader";
            events.log("LEADER DIED: " + name + "! Succession needed.");
        });

        events.on(EventType.RESEARCH_COMPLETED, payload -> {
            String techName = payload instanceof String ? (String) payload : "Unknown tech";
            events.log("RESEARCH: " + techName + " completed!");
        });

        events.on(EventType.BUILDING_COMPLETED, payload -> {
            String type = payload instanceof String ? (String) payload : "Building";
            events.log("BUILT: " + type + " construction complete.");
        });
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height);
        gameHud.resize(width, height);
        centerCameraOnWorld();
    }

    @Override
    public void render() {
        float delta = Gdx.graphics.getDeltaTime();

        handleSaveLoad();
        handleResearchInput();
        handleResetInput();

        // While succession UI is showing, pause all simulation (FIX 13)
        if (pendingSuccession) {
            renderWorld();
            renderSuccessionOverlay();
            gameHud.update(gameState, playerController, eventSystem);
            gameHud.getStage().draw();
            handleSuccessionInput();
            return;
        }

        playerController.update(delta);
        aiTaskSystem.update(gameState.ecsWorld, gameState.world, delta);
        needsSystem.update(gameState.ecsWorld, gameState.world, delta);
        moodSystem.update(gameState.ecsWorld, gameState.world, delta);
        buildingProductionSystem.update(gameState.ecsWorld, gameState.world, delta);
        gameState.research.update(delta);
        eventSystem.update(gameState.ecsWorld, gameState.world, delta);
        gameState.pollution.update(gameState.ecsWorld, gameState.world, delta);
        agingSystem.update(gameState.ecsWorld, gameState.world, delta);

        // Check for colonist deaths and fire events
        for (Entity e : gameState.ecsWorld.getEntitiesWith(HealthComponent.class, IdentityComponent.class)) {
            HealthComponent health = e.get(HealthComponent.class);
            if (health.dead && !health.deathEventFired) {
                health.deathEventFired = true;
                IdentityComponent id = e.get(IdentityComponent.class);
                if (e.has(LeaderComponent.class)) {
                    gameState.events.fire(EventType.LEADER_DIED, id.name);
                } else {
                    gameState.events.fire(EventType.COLONIST_DIED, id.name);
                }
            }
        }

        // Handle leader succession (FIX 13: show UI instead of auto-picking)
        if (agingSystem.isSuccessionNeeded()) {
            showStatus(agingSystem.getDeathMessage());
            java.util.List<Entity> candidates = agingSystem.getSuccessorCandidates(gameState.ecsWorld);
            if (!candidates.isEmpty()) {
                if (candidates.size() == 1) {
                    // Auto-pick the only candidate
                    Entity oldLeader = playerController.getLeader();
                    Entity successor = candidates.get(0);
                    agingSystem.promoteToLeader(successor, oldLeader);
                    playerController.findLeader();
                    gameState.leaderGeneration++;
                    IdentityComponent sid = successor.get(IdentityComponent.class);
                    String name = sid != null ? sid.name : "Unknown";
                    showStatus("New leader: " + name + " (Gen " + gameState.leaderGeneration + ")");
                    gameState.events.fireAndLog(EventType.LEADER_SUCCEEDED, name,
                            "SUCCESSION: " + name + " is the new leader (Gen " + gameState.leaderGeneration + ")");
                } else {
                    // Show player the succession UI
                    pendingSuccession = true;
                    successionCandidates = candidates;
                }
            } else {
                showStatus("No successor available! Colony is leaderless.");
            }
        }

        for (Entity e : gameState.ecsWorld.getEntitiesWith(PositionComponent.class, AIComponent.class)) {
            PositionComponent pos = e.get(PositionComponent.class);
            float[] xy = {pos.x, pos.y};
            gameState.world.clampPosition(xy);
            pos.x = xy[0];
            pos.y = xy[1];
        }

        if (statusTimer > 0) statusTimer -= delta;

        renderWorld();

        gameHud.update(gameState, playerController, eventSystem);
        gameHud.getStage().draw();
    }

    @Override
    public void dispose() {
        spriteBatch.dispose();
        smallFont.dispose();
        shapeRenderer.dispose();
        spriteManager.dispose();
        gameHud.dispose();
    }

    private void handleResearchInput() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.R) && !Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT)
                && !Gdx.input.isKeyPressed(Input.Keys.CONTROL_RIGHT)) {
            gameState.research.startNextResearch();
            Technology current = gameState.research.getCurrentResearch();
            if (current != null && !current.isUnlocked()) {
                showStatus("Researching: " + current.getName());
            } else {
                showStatus("All research complete!");
            }
        }
    }

    private void handleResetInput() {
        if ((Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT) || Gdx.input.isKeyPressed(Input.Keys.CONTROL_RIGHT))
                && Gdx.input.isKeyJustPressed(Input.Keys.R)) {
            resetGame();
        }
    }

    private void resetGame() {
        WorldGenerator.GenerationResult result = WorldGenerator.generate(WORLD_WIDTH, WORLD_HEIGHT);
        gameState = new GameState(result.world, result.ecsWorld);
        playerController = new PlayerController(gameState.world, gameState.ecsWorld, camera, viewport, TILE_SIZE);
        eventSystem = new EventSystem();
        pendingSuccession = false;
        successionCandidates = new java.util.ArrayList<>();
        wireEventBus();
        centerCameraOnWorld();
        showStatus("World reset!");
    }

    private void handleSaveLoad() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.F5)) {
            try {
                SaveLoadSystem.save(gameState.world, gameState.ecsWorld, "syntropy_save.json");
                showStatus("Game saved!");
            } catch (Exception e) {
                showStatus("Save failed: " + e.getMessage());
            }
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.F9)) {
            try {
                SaveLoadSystem.LoadResult loadResult = SaveLoadSystem.load("syntropy_save.json");
                gameState = new GameState(loadResult.world, loadResult.ecsWorld);
                playerController = new PlayerController(gameState.world, gameState.ecsWorld, camera, viewport, TILE_SIZE);
                eventSystem = new EventSystem();
                wireEventBus();
                centerCameraOnWorld();
                showStatus("Game loaded!");
            } catch (Exception e) {
                showStatus("Load failed: " + e.getMessage());
            }
        }
    }

    private void showStatus(String msg) {
        statusMessage = msg;
        statusTimer = 3f;
    }

    private void centerCameraOnWorld() {
        camera.position.set(gameState.world.getWidth() * TILE_SIZE / 2f,
                gameState.world.getHeight() * TILE_SIZE / 2f, 0f);
        camera.update();
    }

    // ── World rendering ──

    private void renderWorld() {
        Gdx.gl.glClearColor(0.02f, 0.02f, 0.03f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        camera.update();

        spriteBatch.setProjectionMatrix(camera.combined);
        spriteBatch.begin();

        renderTiles();
        renderBuildings();
        renderGroundItems();
        renderColonists();
        renderWorldText();

        spriteBatch.end();
    }

    private void renderTiles() {
        for (int y = 0; y < gameState.world.getHeight(); y++) {
            for (int x = 0; x < gameState.world.getWidth(); x++) {
                Tile tile = gameState.world.getTile(x, y);
                Texture tex = spriteManager.getTerrainTexture(tile.getTerrainType());
                spriteBatch.draw(tex, x * TILE_SIZE, y * TILE_SIZE, TILE_SIZE, TILE_SIZE);

                if (tile.isStockpile()) {
                    Texture stockTex = spriteManager.getStockpileTexture();
                    spriteBatch.draw(stockTex, x * TILE_SIZE + 4, y * TILE_SIZE + 4,
                            TILE_SIZE - 8, TILE_SIZE - 8);
                }
            }
        }
    }

    private void renderBuildings() {
        for (Entity e : gameState.ecsWorld.getEntitiesWith(BuildingComponent.class, PositionComponent.class)) {
            BuildingComponent bc = e.get(BuildingComponent.class);
            PositionComponent pos = e.get(PositionComponent.class);
            Texture tex = spriteManager.getBuildingTexture(bc.buildingType);
            if (tex != null) {
                spriteBatch.draw(tex, pos.x * TILE_SIZE + 6, pos.y * TILE_SIZE + 6,
                        TILE_SIZE - 12, TILE_SIZE - 12);
            }

            int count = Math.min(bc.getOutputCount(), 5);
            for (int i = 0; i < count; i++) {
                Texture itemTex = spriteManager.getItemTexture(bc.producedItemType);
                if (itemTex != null) {
                    spriteBatch.draw(itemTex, pos.x * TILE_SIZE + 10 + i * 7, pos.y * TILE_SIZE + 10, 5, 5);
                }
            }
        }

        // Render beds (separate component from BuildingComponent)
        for (Entity e : gameState.ecsWorld.getEntitiesWith(BedComponent.class, PositionComponent.class)) {
            PositionComponent pos = e.get(PositionComponent.class);
            Texture tex = spriteManager.getBuildingTexture("BED");
            if (tex != null) {
                spriteBatch.draw(tex, pos.x * TILE_SIZE + 6, pos.y * TILE_SIZE + 6,
                        TILE_SIZE - 12, TILE_SIZE - 12);
            }
        }
    }

    private void renderGroundItems() {
        for (int y = 0; y < gameState.world.getHeight(); y++) {
            for (int x = 0; x < gameState.world.getWidth(); x++) {
                Tile tile = gameState.world.getTile(x, y);
                if (tile.getGroundItems().isEmpty()) continue;
                for (int i = 0; i < tile.getGroundItems().size(); i++) {
                    Item item = tile.getGroundItems().get(i);
                    Texture tex = spriteManager.getItemTexture(item.getType());
                    if (tex != null) {
                        spriteBatch.draw(tex, x * TILE_SIZE + 8 + i * 9, y * TILE_SIZE + 8, 7, 7);
                    }
                }
            }
        }
    }

    private void renderColonists() {
        for (Entity e : gameState.ecsWorld.getEntitiesWith(PositionComponent.class, HealthComponent.class, NeedsComponent.class)) {
            PositionComponent pos = e.get(PositionComponent.class);
            HealthComponent health = e.get(HealthComponent.class);
            InventoryComponent inv = e.get(InventoryComponent.class);
            boolean isLeader = e.has(LeaderComponent.class);

            float cx = pos.x * TILE_SIZE;
            float cy = pos.y * TILE_SIZE;

            Texture tex = spriteManager.getColonistTexture(health.dead, isLeader);
            spriteBatch.draw(tex, cx + 10, cy + 10, TILE_SIZE - 20, TILE_SIZE - 20);

            if (health.dead) continue;

            if (inv != null && inv.carriedItem != null) {
                Texture itemTex = spriteManager.getItemTexture(inv.carriedItem.getType());
                if (itemTex != null) {
                    spriteBatch.draw(itemTex, cx + 18, cy + 18, 7, 7);
                }
            }
        }

        // Need bars via ShapeRenderer
        spriteBatch.end();
        shapeRenderer.setProjectionMatrix(camera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        for (Entity e : gameState.ecsWorld.getEntitiesWith(PositionComponent.class, HealthComponent.class, NeedsComponent.class)) {
            HealthComponent health = e.get(HealthComponent.class);
            if (health.dead) continue;
            PositionComponent pos = e.get(PositionComponent.class);
            NeedsComponent needs = e.get(NeedsComponent.class);
            float cx = pos.x * TILE_SIZE;
            float cy = pos.y * TILE_SIZE;
            float barY = cy + TILE_SIZE - 6;
            float barWidth = TILE_SIZE - 16;
            drawBar(cx + 8, barY, barWidth, needs.hunger, 0.2f, 0.8f, 0.3f);
            drawBar(cx + 8, barY - 5, barWidth, needs.health, 0.9f, 0.2f, 0.2f);
            drawBar(cx + 8, barY - 10, barWidth, needs.energy, 0.2f, 0.6f, 0.9f);
            MoodComponent moodComp = e.get(MoodComponent.class);
            float moodRatio = moodComp != null ? moodComp.mood / 100f : 0.5f;
            drawBar(cx + 8, barY - 15, barWidth, moodRatio, 0.9f, 0.8f, 0.2f);
        }
        shapeRenderer.end();
        spriteBatch.begin();
    }

    private void drawBar(float x, float y, float maxWidth, float ratio, float r, float g, float b) {
        shapeRenderer.setColor(Color.DARK_GRAY);
        shapeRenderer.rect(x, y, maxWidth, 3);
        shapeRenderer.setColor(r, g, b, 1f);
        shapeRenderer.rect(x, y, maxWidth * ratio, 3);
    }

    // ── Succession UI (FIX 13) ──

    private void renderSuccessionOverlay() {
        int sw = viewport.getScreenWidth();
        int sh = viewport.getScreenHeight();
        float panelW = 360f;
        float panelH = 40f + Math.min(successionCandidates.size(), 5) * 28f + 30f;
        float px = (sw - panelW) / 2f;
        float py = (sh - panelH) / 2f;

        // Semi-transparent background
        shapeRenderer.setProjectionMatrix(new com.badlogic.gdx.math.Matrix4().setToOrtho2D(0, 0, sw, sh));
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0f, 0f, 0f, 0.7f);
        shapeRenderer.rect(0, 0, sw, sh);
        shapeRenderer.setColor(0.15f, 0.15f, 0.2f, 1f);
        shapeRenderer.rect(px, py, panelW, panelH);
        shapeRenderer.end();

        // Text
        spriteBatch.setProjectionMatrix(new com.badlogic.gdx.math.Matrix4().setToOrtho2D(0, 0, sw, sh));
        spriteBatch.begin();
        smallFont.setColor(Color.YELLOW);
        smallFont.draw(spriteBatch, "Choose your successor:", px + 10, py + panelH - 10);
        int shown = Math.min(successionCandidates.size(), 5);
        for (int i = 0; i < shown; i++) {
            Entity c = successionCandidates.get(i);
            IdentityComponent cid = c.get(IdentityComponent.class);
            AgingComponent cage = c.get(AgingComponent.class);
            LeaderComponent lc = c.get(LeaderComponent.class);
            String name = cid != null ? cid.name : "Unknown";
            String age = cage != null ? "Age " + (int) cage.ageYears : "";
            String stats = lc != null
                    ? String.format("CHA:%.0f ENG:%.0f SCI:%.0f COM:%.0f", lc.charisma, lc.engineering, lc.science, lc.combat)
                    : "";
            smallFont.setColor(Color.WHITE);
            smallFont.draw(spriteBatch, (i + 1) + ". " + name + "  " + age + "  " + stats,
                    px + 10, py + panelH - 34 - i * 28f);
        }
        smallFont.setColor(0.6f, 0.6f, 0.6f, 1f);
        smallFont.draw(spriteBatch, "Press 1-" + shown + " to select", px + 10, py + 14);
        spriteBatch.end();
        spriteBatch.setProjectionMatrix(camera.combined);
    }

    private void handleSuccessionInput() {
        int shown = Math.min(successionCandidates.size(), 5);
        int[] keys = {Input.Keys.NUM_1, Input.Keys.NUM_2, Input.Keys.NUM_3, Input.Keys.NUM_4, Input.Keys.NUM_5};
        for (int i = 0; i < shown; i++) {
            if (Gdx.input.isKeyJustPressed(keys[i])) {
                Entity oldLeader = playerController.getLeader();
                Entity successor = successionCandidates.get(i);
                agingSystem.promoteToLeader(successor, oldLeader);
                playerController.findLeader();
                gameState.leaderGeneration++;
                IdentityComponent sid = successor.get(IdentityComponent.class);
                String name = sid != null ? sid.name : "Unknown";
                showStatus("New leader: " + name + " (Gen " + gameState.leaderGeneration + ")");
                gameState.events.fireAndLog(EventType.LEADER_SUCCEEDED, name,
                        "SUCCESSION: " + name + " is the new leader (Gen " + gameState.leaderGeneration + ")");
                pendingSuccession = false;
                successionCandidates = new java.util.ArrayList<>();
                return;
            }
        }
    }

    private void renderWorldText() {
        for (Entity e : gameState.ecsWorld.getEntitiesWith(IdentityComponent.class, PositionComponent.class, HealthComponent.class)) {
            IdentityComponent id = e.get(IdentityComponent.class);
            PositionComponent pos = e.get(PositionComponent.class);
            HealthComponent health = e.get(HealthComponent.class);
            boolean isLeader = e.has(LeaderComponent.class);
            float cx = pos.x * TILE_SIZE;
            float cy = pos.y * TILE_SIZE;
            if (health.dead) {
                smallFont.setColor(Color.GRAY);
                smallFont.draw(spriteBatch, id.name + " (dead)", cx - 4, cy + TILE_SIZE + 10);
            } else {
                smallFont.setColor(isLeader ? Color.RED : Color.WHITE);
                String prefix = isLeader ? "[L] " : "";
                smallFont.draw(spriteBatch, prefix + id.name, cx + 4, cy + TILE_SIZE + 10);
                AIComponent ai = e.get(AIComponent.class);
                if (ai != null && !isLeader) {
                    smallFont.setColor(0.7f, 0.7f, 0.7f, 1f);
                    smallFont.draw(spriteBatch, ai.taskType.name(), cx + 4, cy + TILE_SIZE + 22);
                }
            }
        }

        // Draw status message centered at top of viewport (FIX 9)
        if (statusTimer > 0 && !statusMessage.isEmpty()) {
            spriteBatch.end();
            spriteBatch.setProjectionMatrix(new com.badlogic.gdx.math.Matrix4().setToOrtho2D(0, 0,
                    viewport.getScreenWidth(), viewport.getScreenHeight()));
            spriteBatch.begin();
            smallFont.setColor(Color.YELLOW);
            GlyphLayout layout = new GlyphLayout(smallFont, statusMessage);
            smallFont.draw(spriteBatch, statusMessage,
                    (viewport.getScreenWidth() - layout.width) / 2f,
                    viewport.getScreenHeight() - 10);
            spriteBatch.end();
            spriteBatch.setProjectionMatrix(camera.combined);
            spriteBatch.begin();
        }
    }
}
