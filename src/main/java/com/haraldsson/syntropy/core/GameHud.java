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
import com.haraldsson.syntropy.entities.ItemType;
import com.haraldsson.syntropy.input.PlayerController;
import com.haraldsson.syntropy.systems.EventSystem;
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
    private final Label colonistListLabel;
    private final Label possessionLabel;
    private final Label eventLogLabel;
    private final Label pickupLabel;
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

        possessionLabel = new Label("", skin, "hud-bold");
        possessionLabel.setColor(Color.RED);
        leftCol.add(possessionLabel).left().padTop(8).row();

        pickupLabel = new Label("", skin, "hud");
        pickupLabel.setColor(Color.CYAN);
        leftCol.add(pickupLabel).left().padTop(4).row();

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

        root.add(bottomRow).bottom().left().fillX().row();

        // Build mode label (centered, overlaid)
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

        // Colonist list — compact single-line format
        Entity leaderEntity = playerController.getLeader();
        StringBuilder col = new StringBuilder("-- Colony --\n");
        for (Entity e : ecsWorld.getEntitiesWith(IdentityComponent.class, NeedsComponent.class, HealthComponent.class)) {
            IdentityComponent id = e.get(IdentityComponent.class);
            HealthComponent health = e.get(HealthComponent.class);
            NeedsComponent needs = e.get(NeedsComponent.class);
            AIComponent ai = e.get(AIComponent.class);
            boolean isLeader = e.has(LeaderComponent.class);
            if (health.dead) {
                col.append(id.name).append("  DEAD\n");
            } else {
                col.append(isLeader ? "[L] " : "    ");
                col.append(id.name);
                col.append("  H:").append(needs.getHungerCategory().name());
                col.append("  E:").append(needs.getEnergyCategory().name());
                if (ai != null && !isLeader) {
                    col.append("  ").append(ai.taskType.name());
                }
                col.append("\n");
            }
        }
        colonistListLabel.setText(col.toString().trim());

        // Leader info
        if (leaderEntity != null) {
            HealthComponent lh = leaderEntity.get(HealthComponent.class);
            if (lh != null && !lh.dead) {
                IdentityComponent pid = leaderEntity.get(IdentityComponent.class);
                AgingComponent la = leaderEntity.get(AgingComponent.class);
                StringBuilder lb = new StringBuilder();
                lb.append("LEADER: ").append(pid != null ? pid.name : "???");
                if (la != null) lb.append("  Age: ").append((int) la.ageYears);
                possessionLabel.setText(lb.toString());
            } else {
                possessionLabel.setText("LEADER IS DEAD — awaiting succession");
            }
        } else {
            possessionLabel.setText("NO LEADER");
        }

        // Leader needs info
        if (leaderEntity != null && leaderEntity.has(NeedsComponent.class)) {
            NeedsComponent ln = leaderEntity.get(NeedsComponent.class);
            leaderInfoLabel.setText("Hunger: " + ln.getHungerCategory().name()
                    + "  Energy: " + ln.getEnergyCategory().name()
                    + "  HP: " + (int)(ln.health * 100) + "%");
        } else {
            leaderInfoLabel.setText("");
        }

        // Event log — last 3 events only
        List<String> randomEvents = eventSystem.getEventLog();
        List<String> busEvents = gameEvents.getEventLog();
        StringBuilder evtSb = new StringBuilder();
        int totalShown = 0;
        for (int i = busEvents.size() - 1; i >= 0 && totalShown < 3; i--, totalShown++) {
            evtSb.insert(0, busEvents.get(i) + "\n");
        }
        for (int i = randomEvents.size() - 1; i >= 0 && totalShown < 3; i--, totalShown++) {
            evtSb.insert(0, randomEvents.get(i) + "\n");
        }
        eventLogLabel.setText(evtSb.toString().trim());

        // Pickup message
        String pickupMsg = playerController.getPickupMessage();
        pickupLabel.setText(pickupMsg);

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
}

