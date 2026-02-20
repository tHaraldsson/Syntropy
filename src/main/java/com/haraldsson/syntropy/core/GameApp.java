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

import java.util.List;

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

    private boolean successionPending = false;
    private float successionAutoTimer = 0f;
    private List<Entity> successionCandidates = null;

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

        handleGlobalInput(delta);

        if (!successionPending) {
            playerController.update(delta);
            aiTaskSystem.update(gameState.ecsWorld, gameState.world, delta);
            needsSystem.update(gameState.ecsWorld, gameState.world, delta);
            moodSystem.update(gameState.ecsWorld, gameState.world, delta);
            buildingProductionSystem.update(gameState.ecsWorld, gameState.world, delta);
            gameState.research.update(delta);
            eventSystem.update(gameState.ecsWorld, gameState.world, delta);
            gameState.pollution.update(gameState.ecsWorld, gameState.world, delta);
            agingSystem.update(gameState.ecsWorld, gameState.world, delta);
        }

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

        // Remove dead entities after 30 seconds
        java.util.List<Entity> toRemove = new java.util.ArrayList<>();
        for (Entity e : gameState.ecsWorld.getEntitiesWith(HealthComponent.class)) {
            HealthComponent health = e.get(HealthComponent.class);
            if (health.dead) {
                health.deadTimer += delta;
                if (health.deadTimer >= 30f) {
                    toRemove.add(e);
                }
            }
        }
        for (Entity e : toRemove) {
            gameState.ecsWorld.removeEntity(e);
        }

        // Handle leader succession
        if (!successionPending && agingSystem.isSuccessionNeeded()) {
            showStatus(agingSystem.getDeathMessage());
            successionCandidates = agingSystem.getSuccessorCandidates(gameState.ecsWorld);
            if (successionCandidates.isEmpty()) {
                showStatus("No survivor available! Press Ctrl+R to restart.");
            } else if (successionCandidates.size() == 1) {
                successionPending = true;
                successionAutoTimer = 2f;
            } else {
                successionPending = true;
                successionAutoTimer = 0f;
            }
        }

        if (successionPending) {
            successionAutoTimer -= delta;
            if (successionCandidates != null && successionCandidates.size() == 1 && successionAutoTimer <= 0f) {
                pickSuccessor(0);
            }
            // Check numeric key presses for multi-candidate choice
            if (successionCandidates != null && successionCandidates.size() > 1) {
                for (int i = 0; i < Math.min(successionCandidates.size(), 9); i++) {
                    if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_1 + i)) {
                        pickSuccessor(i);
                        break;
                    }
                }
            }
        }

        if (!successionPending) {
            handleSaveLoad();
            handleResearchInput();
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
        if (successionPending) renderSuccessionOverlay();

        gameHud.update(gameState, playerController, eventSystem);
        gameHud.getStage().draw();
    }

    private void pickSuccessor(int index) {
        Entity oldLeader = playerController.getLeader();
        Entity successor = successionCandidates.get(index);
        agingSystem.promoteToLeader(successor, oldLeader);
        playerController.findLeader();
        gameState.leaderGeneration++;
        IdentityComponent sid = successor.get(IdentityComponent.class);
        String name = sid != null ? sid.name : "Unknown";
        showStatus("New leader: " + name + " (Gen " + gameState.leaderGeneration + ")");
        gameState.events.fireAndLog(EventType.LEADER_SUCCEEDED, name,
                "SUCCESSION: " + name + " is the new leader (Gen " + gameState.leaderGeneration + ")");
        successionPending = false;
        successionCandidates = null;
    }

    private void renderSuccessionOverlay() {
        spriteBatch.setProjectionMatrix(gameHud.getStage().getCamera().combined);
        spriteBatch.begin();
        float cx = Gdx.graphics.getWidth() / 2f;
        float ty = Gdx.graphics.getHeight() - 40f;
        smallFont.setColor(Color.YELLOW);
        smallFont.draw(spriteBatch, "LEADER HAS DIED — Choose a successor:", cx - 160f, ty);
        if (successionCandidates != null) {
            for (int i = 0; i < Math.min(successionCandidates.size(), 9); i++) {
                Entity c = successionCandidates.get(i);
                IdentityComponent id = c.get(IdentityComponent.class);
                AgingComponent aging = c.get(AgingComponent.class);
                LeaderComponent lc = c.get(LeaderComponent.class);
                String name = id != null ? id.name : "?";
                int age = aging != null ? (int) aging.ageYears : 0;
                String stats = lc != null
                        ? String.format("Chr%.0f Eng%.0f Sci%.0f Cmb%.0f", lc.charisma, lc.engineering, lc.science, lc.combat)
                        : "";
                smallFont.draw(spriteBatch,
                        (i + 1) + ". " + name + " age " + age + "  " + stats,
                        cx - 160f, ty - 20f - i * 18f);
            }
            if (successionCandidates.size() > 1) {
                smallFont.draw(spriteBatch, "Press 1–" + Math.min(successionCandidates.size(), 9) + " to choose",
                        cx - 160f, ty - 20f - successionCandidates.size() * 18f - 10f);
            } else if (successionCandidates.size() == 1) {
                smallFont.draw(spriteBatch, "Auto-selecting in " + (int) Math.ceil(successionAutoTimer) + "s...",
                        cx - 160f, ty - 40f);
            }
        }
        spriteBatch.end();
        spriteBatch.setProjectionMatrix(camera.combined);
    }

    @Override
    public void dispose() {
        spriteBatch.dispose();
        smallFont.dispose();
        shapeRenderer.dispose();
        spriteManager.dispose();
        gameHud.dispose();
    }

    private void handleGlobalInput(float delta) {
        // Ctrl+R: full game reset at any time
        if (Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT) && Gdx.input.isKeyJustPressed(Input.Keys.R)) {
            resetGame();
        }
    }

    private void resetGame() {
        WorldGenerator.GenerationResult result = WorldGenerator.generate(WORLD_WIDTH, WORLD_HEIGHT);
        gameState = new GameState(result.world, result.ecsWorld);
        playerController = new PlayerController(gameState.world, gameState.ecsWorld, camera, viewport, TILE_SIZE);
        eventSystem = new EventSystem();
        needsSystem = new NeedsSystem();
        buildingProductionSystem = new BuildingProductionSystem();
        aiTaskSystem = new AITaskSystem();
        agingSystem = new AgingSystem();
        moodSystem = new MoodSystem();
        wireEventBus();
        successionPending = false;
        successionCandidates = null;
        centerCameraOnWorld();
        showStatus("New game started");
    }

    private void handleResearchInput() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.R) && !Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT)) {
            gameState.research.startNextResearch();
            Technology current = gameState.research.getCurrentResearch();
            if (current != null && !current.isUnlocked()) {
                showStatus("Researching: " + current.getName());
            } else {
                showStatus("All research complete!");
            }
        }
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
            MoodComponent moodComp = e.get(MoodComponent.class);
            float moodRatio = moodComp != null ? moodComp.mood / 100f : 0.5f;
            drawBar(cx + 8, barY - 10, barWidth, moodRatio, 0.9f, 0.8f, 0.2f);
            drawBar(cx + 8, barY - 15, barWidth, needs.energy, 0.1f, 0.8f, 0.9f);
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

    private void renderWorldText() {
        // Draw status message centered at top of screen
        if (statusTimer > 0 && !statusMessage.isEmpty()) {
            smallFont.setColor(Color.YELLOW);
            smallFont.draw(spriteBatch, statusMessage,
                    camera.position.x - camera.viewportWidth * camera.zoom / 2f + 10f,
                    camera.position.y + camera.viewportHeight * camera.zoom / 2f - 5f);
        }

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
    }
}
