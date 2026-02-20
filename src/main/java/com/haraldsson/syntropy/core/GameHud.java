package com.haraldsson.syntropy.core;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.haraldsson.syntropy.ecs.ECSWorld;
import com.haraldsson.syntropy.ecs.Entity;
import com.haraldsson.syntropy.ecs.components.*;
import com.haraldsson.syntropy.ecs.systems.PollutionSystem;
import com.haraldsson.syntropy.entities.ItemType;
import com.haraldsson.syntropy.input.PlayerController;
import com.haraldsson.syntropy.systems.EventSystem;
import com.haraldsson.syntropy.systems.ResearchSystem;
import com.haraldsson.syntropy.systems.Technology;
import com.haraldsson.syntropy.world.Tile;
import com.haraldsson.syntropy.world.World;

import java.util.List;

/**
 * Scene2D-based HUD. Replaces manual BitmapFont drawing.
 */
public class GameHud {
    private final Stage stage;
    private final Skin skin;

    // Widgets
    private final Label resourceLabel;
    private final Label buildingLabel;
    private final Label colonistListLabel;
    private final Label controlsLabel;
    private final Label possessionLabel;
    private final Label researchLabel;
    private final Label eventLogLabel;
    private final Label pickupLabel;
    private final Label statusLabel;
    private final Label pollutionLabel;
    private final Label leaderInfoLabel;
    private final Label buildModeLabel;

    private Texture whitePixel;

    public GameHud() {
        stage = new Stage(new ScreenViewport());
        skin = buildSkin();

        // Root table fills the screen
        Table root = new Table();
        root.setFillParent(true);
        root.pad(10);
        stage.addActor(root);

        // Top row: resources (left) + colonists (right)
        Table topRow = new Table();

        // Left column
        Table leftCol = new Table();
        leftCol.top().left();
        resourceLabel = new Label("", skin, "hud");
        resourceLabel.setColor(Color.WHITE);
        leftCol.add(resourceLabel).left().row();

        buildingLabel = new Label("", skin, "hud");
        buildingLabel.setColor(Color.WHITE);
        leftCol.add(buildingLabel).left().padTop(4).row();

        possessionLabel = new Label("", skin, "hud-bold");
        possessionLabel.setColor(Color.RED);
        leftCol.add(possessionLabel).left().padTop(8).row();

        researchLabel = new Label("", skin, "hud");
        researchLabel.setColor(0.6f, 0.8f, 1f, 1f);
        leftCol.add(researchLabel).left().padTop(4).row();

        pickupLabel = new Label("", skin, "hud");
        pickupLabel.setColor(Color.CYAN);
        leftCol.add(pickupLabel).left().padTop(4).row();

        pollutionLabel = new Label("", skin, "hud");
        pollutionLabel.setColor(0.8f, 0.5f, 0.2f, 1f);
        leftCol.add(pollutionLabel).left().padTop(4).row();

        leaderInfoLabel = new Label("", skin, "hud");
        leaderInfoLabel.setColor(0.9f, 0.85f, 0.6f, 1f);
        leftCol.add(leaderInfoLabel).left().padTop(4).row();

        // Right column
        Table rightCol = new Table();
        rightCol.top().right();
        colonistListLabel = new Label("", skin, "hud");
        colonistListLabel.setColor(Color.WHITE);
        colonistListLabel.setAlignment(Align.topRight);
        rightCol.add(colonistListLabel).right();

        topRow.add(leftCol).expand().fill().left().top();
        topRow.add(rightCol).expand().fill().right().top();

        root.add(topRow).expand().fill().row();

        // Bottom section
        Table bottomRow = new Table();

        // Event log
        eventLogLabel = new Label("", skin, "small");
        eventLogLabel.setColor(0.9f, 0.7f, 0.3f, 1f);
        bottomRow.add(eventLogLabel).left().expandX().fillX().row();

        // Controls
        controlsLabel = new Label("[WASD] Move Leader  [E] Pickup/Drop  [R] Research  [B] Build Mode  [Scroll] Zoom  [F5] Save  [F9] Load", skin, "small");
        controlsLabel.setColor(0.6f, 0.6f, 0.6f, 1f);
        bottomRow.add(controlsLabel).left().expandX().fillX().row();

        root.add(bottomRow).bottom().left().fillX().row();

        // Status (centered, overlaid)
        statusLabel = new Label("", skin, "hud-bold");
        statusLabel.setColor(Color.YELLOW);
        statusLabel.setAlignment(Align.center);
        statusLabel.setPosition(0, 0);
        stage.addActor(statusLabel);

        // Build mode label (centered, below status)
        buildModeLabel = new Label("", skin, "hud-bold");
        buildModeLabel.setColor(Color.YELLOW);
        buildModeLabel.setAlignment(Align.center);
        stage.addActor(buildModeLabel);
    }

    private Skin buildSkin() {
        Skin s = new Skin();

        // White pixel texture for drawables
        Pixmap pm = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pm.setColor(Color.WHITE);
        pm.fill();
        whitePixel = new Texture(pm);
        pm.dispose();
        s.add("white", whitePixel);

        // Fonts
        BitmapFont defaultFont = new BitmapFont();
        BitmapFont smallFont = new BitmapFont();
        smallFont.getData().setScale(0.8f);
        BitmapFont boldFont = new BitmapFont();
        boldFont.getData().setScale(1.1f);

        s.add("default-font", defaultFont);
        s.add("small-font", smallFont);
        s.add("bold-font", boldFont);

        // Label styles
        Label.LabelStyle hudStyle = new Label.LabelStyle(defaultFont, Color.WHITE);
        s.add("hud", hudStyle);

        Label.LabelStyle smallStyle = new Label.LabelStyle(smallFont, Color.GRAY);
        s.add("small", smallStyle);

        Label.LabelStyle boldStyle = new Label.LabelStyle(boldFont, Color.WHITE);
        s.add("hud-bold", boldStyle);

        return s;
    }

    public void update(GameState gameState, PlayerController playerController, EventSystem eventSystem) {

        ECSWorld ecsWorld = gameState.ecsWorld;
        World world = gameState.world;
        PollutionSystem pollutionSystem = gameState.pollution;
        ResearchSystem researchSystem = gameState.research;
        GameEvents gameEvents = gameState.events;

        // Resources
        int totalStone = 0, totalFood = 0, totalWood = 0;
        Tile stockpile = world.getStockpileTile();
        if (stockpile != null) {
            totalStone = stockpile.countItems(ItemType.STONE);
            totalFood = stockpile.countItems(ItemType.FOOD);
            totalWood = stockpile.countItems(ItemType.WOOD);
        }
        resourceLabel.setText("Stockpile  Stone: " + totalStone + "  Food: " + totalFood + "  Wood: " + totalWood);

        // Buildings
        StringBuilder bldg = new StringBuilder();
        int minerIdx = 1, growerIdx = 1, woodcutterIdx = 1;
        for (Entity e : ecsWorld.getEntitiesWith(BuildingComponent.class)) {
            BuildingComponent bc = e.get(BuildingComponent.class);
            if ("MINER".equals(bc.buildingType)) {
                bldg.append("Miner ").append(minerIdx++).append(" output: ").append(bc.getOutputCount()).append("\n");
            } else if ("WOODCUTTER".equals(bc.buildingType)) {
                bldg.append("Woodcutter ").append(woodcutterIdx++).append(" output: ").append(bc.getOutputCount()).append("\n");
            } else {
                bldg.append("FoodGrower ").append(growerIdx++).append(" output: ").append(bc.getOutputCount()).append("\n");
            }
        }
        buildingLabel.setText(bldg.toString().trim());

        // Colonist list
        Entity leaderEntity = playerController.getLeader();
        StringBuilder col = new StringBuilder("-- Colony --\n");
        for (Entity e : ecsWorld.getEntitiesWith(IdentityComponent.class, NeedsComponent.class, HealthComponent.class)) {
            IdentityComponent id = e.get(IdentityComponent.class);
            HealthComponent health = e.get(HealthComponent.class);
            NeedsComponent needs = e.get(NeedsComponent.class);
            AIComponent ai = e.get(AIComponent.class);
            RoleComponent role = e.get(RoleComponent.class);
            AgingComponent aging = e.get(AgingComponent.class);
            boolean isLeader = e.has(LeaderComponent.class);
            if (health.dead) {
                col.append(id.name).append(" (DEAD)\n");
            } else {
                if (isLeader) col.append("[LEADER] ");
                col.append(id.name);
                if (aging != null) col.append(" age:").append((int) aging.ageYears);
                if (role != null) col.append(" [").append(role.role.name()).append("]");
                col.append("\n");
                col.append("  ").append(needs.getHungerCategory().name())
                   .append(" | ").append(needs.getEnergyCategory().name());
                MoodComponent moodComp = e.get(MoodComponent.class);
                if (moodComp != null) {
                    col.append(" | Mood:").append((int) moodComp.mood);
                }
                col.append("\n");
                if (ai != null && !isLeader) {
                    col.append("  Task: ").append(ai.taskType.name()).append("\n");
                }
            }
        }
        colonistListLabel.setText(col.toString().trim());

        // Leader info
        if (leaderEntity != null) {
            HealthComponent lh = leaderEntity.get(HealthComponent.class);
            if (lh != null && !lh.dead) {
                IdentityComponent pid = leaderEntity.get(IdentityComponent.class);
                LeaderComponent lc = leaderEntity.get(LeaderComponent.class);
                AgingComponent la = leaderEntity.get(AgingComponent.class);
                StringBuilder lb = new StringBuilder();
                lb.append("LEADER: ").append(pid != null ? pid.name : "???");
                if (la != null) lb.append("  Age: ").append((int) la.ageYears);
                if (lc != null) lb.append("  CHA:").append((int) lc.charisma)
                        .append(" ENG:").append((int) lc.engineering)
                        .append(" SCI:").append((int) lc.science)
                        .append(" CMB:").append((int) lc.combat);
                possessionLabel.setText(lb.toString());
            } else {
                possessionLabel.setText("LEADER IS DEAD — awaiting succession");
            }
        } else {
            possessionLabel.setText("NO LEADER");
        }

        // Leader detailed info
        if (leaderEntity != null && leaderEntity.has(NeedsComponent.class)) {
            NeedsComponent ln = leaderEntity.get(NeedsComponent.class);
            MoodComponent lm = leaderEntity.get(MoodComponent.class);
            String moodStr = lm != null ? String.valueOf((int) lm.mood) : "?";
            leaderInfoLabel.setText("Leader needs — Hunger:" + ln.getHungerCategory().name()
                    + "  Energy:" + ln.getEnergyCategory().name()
                    + "  Mood:" + moodStr
                    + "  HP:" + (int)(ln.health * 100) + "%");
        } else {
            leaderInfoLabel.setText("");
        }

        // Pollution
        if (pollutionSystem != null) {
            pollutionLabel.setColor(getPollutionColor(pollutionSystem.getGlobalPollution()));
            pollutionLabel.setText("Pollution: " + (int) pollutionSystem.getGlobalPollution()
                    + "% — " + pollutionSystem.getSeverityLabel()
                    + "  Planet HP: " + (int) pollutionSystem.getPlanetaryHealth() + "%");
        }

        // Research
        Technology currentTech = researchSystem.getCurrentResearch();
        if (currentTech != null) {
            if (currentTech.isUnlocked()) {
                researchLabel.setColor(Color.GREEN);
                researchLabel.setText("Researched: " + currentTech.getName() + " done");
            } else {
                researchLabel.setColor(0.6f, 0.8f, 1f, 1f);
                int pct = (int) (currentTech.getProgressRatio() * 100);
                researchLabel.setText("Researching: " + currentTech.getName() + " " + pct + "%");
            }
        } else {
            researchLabel.setText("[R] to start research");
        }

        // Event log — merge old random events + event bus log
        List<String> randomEvents = eventSystem.getEventLog();
        List<String> busEvents = gameEvents.getEventLog();
        StringBuilder evtSb = new StringBuilder();
        for (int i = Math.max(0, randomEvents.size() - 2); i < randomEvents.size(); i++) {
            evtSb.append(randomEvents.get(i)).append("\n");
        }
        for (int i = Math.max(0, busEvents.size() - 2); i < busEvents.size(); i++) {
            evtSb.append(busEvents.get(i)).append("\n");
        }
        eventLogLabel.setText(evtSb.toString().trim());

        // Pickup message
        String pickupMsg = playerController.getPickupMessage();
        pickupLabel.setText(pickupMsg);

        // Dynasty info in status
        statusLabel.setText("Dynasty: " + gameState.dynastyName + " (Gen " + gameState.leaderGeneration + ")");
        statusLabel.setPosition(
                stage.getWidth() / 2f - statusLabel.getPrefWidth() / 2f,
                stage.getHeight() - 20
        );

        // Build mode indicator
        if (playerController.getBuildModeActive()) {
            buildModeLabel.setText("[BUILD MODE — Click to place Bed (costs 3 Wood) — B to cancel]");
            buildModeLabel.pack();
            buildModeLabel.setPosition(
                    stage.getWidth() / 2f - buildModeLabel.getPrefWidth() / 2f,
                    stage.getHeight() / 2f
            );
        } else {
            buildModeLabel.setText("");
        }

        stage.act(Gdx.graphics.getDeltaTime());
    }

    public Stage getStage() {
        return stage;
    }

    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    public void dispose() {
        stage.dispose();
        skin.dispose();
        if (whitePixel != null) whitePixel.dispose();
    }

    private Color getPollutionColor(float pollution) {
        if (pollution < 20f) return new Color(0.3f, 0.8f, 0.3f, 1f);
        if (pollution < 50f) return new Color(0.8f, 0.7f, 0.2f, 1f);
        if (pollution < 75f) return new Color(0.9f, 0.4f, 0.1f, 1f);
        return new Color(1f, 0.2f, 0.1f, 1f);
    }
}

