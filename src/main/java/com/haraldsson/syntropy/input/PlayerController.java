package com.haraldsson.syntropy.input;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.haraldsson.syntropy.ecs.ECSWorld;
import com.haraldsson.syntropy.ecs.Entity;
import com.haraldsson.syntropy.ecs.components.*;
import com.haraldsson.syntropy.entities.Item;
import com.haraldsson.syntropy.world.Tile;
import com.haraldsson.syntropy.world.World;

public class PlayerController {
    private static final float MOVE_SPEED = 3.2f;
    private static final float CAMERA_PAN_SPEED = 300f;
    private static final float ZOOM_STEP = 0.1f;
    private static final float ZOOM_MIN = 0.4f;
    private static final float ZOOM_MAX = 2.5f;
    private static final float DOUBLE_CLICK_TIME = 0.35f;

    private final World world;
    private final ECSWorld ecsWorld;
    private final OrthographicCamera camera;
    private final Viewport viewport;
    private final int tileSize;

    private Entity possessed;
    private boolean scrollListenerAdded;

    private float lastClickTime;
    private float lastClickX;
    private float lastClickY;

    private String pickupMessage = "";
    private float pickupMessageTimer;

    public PlayerController(World world, ECSWorld ecsWorld, OrthographicCamera camera, Viewport viewport, int tileSize) {
        this.world = world;
        this.ecsWorld = ecsWorld;
        this.camera = camera;
        this.viewport = viewport;
        this.tileSize = tileSize;
    }

    public void update(float delta) {
        if (!scrollListenerAdded) {
            Gdx.input.setInputProcessor(new InputAdapter() {
                @Override
                public boolean scrolled(float amountX, float amountY) {
                    camera.zoom = MathUtils.clamp(camera.zoom + amountY * ZOOM_STEP, ZOOM_MIN, ZOOM_MAX);
                    return true;
                }
            });
            scrollListenerAdded = true;
        }

        if (pickupMessageTimer > 0) pickupMessageTimer -= delta;

        // Auto-unpossess if colonist dies
        if (possessed != null) {
            HealthComponent health = possessed.get(HealthComponent.class);
            if (health != null && health.dead) {
                AIComponent ai = possessed.get(AIComponent.class);
                if (ai != null) ai.aiDisabled = false;
                possessed = null;
            }
        }

        handleDoubleClickPossession();
        handlePickup();

        if (possessed != null) {
            handlePossessedMovement(delta);
            updateCameraFollow();
        } else {
            handleCameraPan(delta);
        }
    }

    public Entity getPossessed() {
        return possessed;
    }

    public String getPickupMessage() {
        return pickupMessageTimer > 0 ? pickupMessage : "";
    }

    private void handleDoubleClickPossession() {
        if (!Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) return;

        float now = System.nanoTime() / 1_000_000_000f;
        float mx = Gdx.input.getX();
        float my = Gdx.input.getY();

        float timeSinceLast = now - lastClickTime;
        float dx = mx - lastClickX;
        float dy = my - lastClickY;
        boolean isDoubleClick = timeSinceLast < DOUBLE_CLICK_TIME && (dx * dx + dy * dy) < 400;

        lastClickTime = now;
        lastClickX = mx;
        lastClickY = my;

        if (!isDoubleClick) return;

        // Double-click: toggle possession
        if (possessed != null) {
            AIComponent ai = possessed.get(AIComponent.class);
            if (ai != null) { ai.aiDisabled = false; ai.clearTask(); }
            possessed = null;
            return;
        }

        Vector2 cursorWorld = viewport.unproject(new Vector2(mx, my));
        float bestDist = Float.MAX_VALUE;
        Entity nearest = null;
        for (Entity e : ecsWorld.getEntitiesWith(PositionComponent.class, HealthComponent.class, AIComponent.class)) {
            HealthComponent health = e.get(HealthComponent.class);
            if (health.dead) continue;
            PositionComponent pos = e.get(PositionComponent.class);
            float cdx = pos.x - (cursorWorld.x / tileSize);
            float cdy = pos.y - (cursorWorld.y / tileSize);
            float dist = cdx * cdx + cdy * cdy;
            if (dist < bestDist) {
                bestDist = dist;
                nearest = e;
            }
        }
        if (nearest != null && bestDist < 4f) {
            possessed = nearest;
            AIComponent ai = possessed.get(AIComponent.class);
            if (ai != null) ai.aiDisabled = true;
        }
    }

    private void handlePickup() {
        if (possessed == null) return;
        HealthComponent health = possessed.get(HealthComponent.class);
        if (health != null && health.dead) return;
        if (!Gdx.input.isKeyJustPressed(Input.Keys.E)) return;

        PositionComponent pos = possessed.get(PositionComponent.class);
        InventoryComponent inv = possessed.get(InventoryComponent.class);
        if (pos == null || inv == null) return;

        int tileX = (int) pos.x;
        int tileY = (int) pos.y;
        Tile tile = world.getTile(tileX, tileY);
        if (tile == null) { showPickupMessage("No tile here!"); return; }

        if (inv.carriedItem != null) {
            tile.addItem(inv.carriedItem);
            showPickupMessage("Dropped " + inv.carriedItem.getType().name());
            inv.carriedItem = null;
            return;
        }

        // Try building output
        Entity buildingEntity = tile.getBuildingEntity();
        if (buildingEntity != null) {
            BuildingComponent bc = buildingEntity.get(BuildingComponent.class);
            if (bc != null && bc.hasOutput()) {
                Item item = bc.takeOutput();
                inv.carriedItem = item;
                showPickupMessage("Picked up " + item.getType().name() + " from " + bc.buildingType);
                return;
            }
        }

        // Ground item
        if (!tile.getGroundItems().isEmpty()) {
            Item picked = tile.getGroundItems().remove(0);
            inv.carriedItem = picked;
            showPickupMessage("Picked up " + picked.getType().name());
            return;
        }

        showPickupMessage("Nothing to pick up at (" + tileX + "," + tileY + ")");
    }

    private void showPickupMessage(String msg) {
        pickupMessage = msg;
        pickupMessageTimer = 2f;
    }

    private void handlePossessedMovement(float delta) {
        PositionComponent pos = possessed.get(PositionComponent.class);
        if (pos == null) return;

        float moveX = 0f, moveY = 0f;
        if (Gdx.input.isKeyPressed(Input.Keys.W)) moveY += 1f;
        if (Gdx.input.isKeyPressed(Input.Keys.S)) moveY -= 1f;
        if (Gdx.input.isKeyPressed(Input.Keys.A)) moveX -= 1f;
        if (Gdx.input.isKeyPressed(Input.Keys.D)) moveX += 1f;

        if (moveX == 0f && moveY == 0f) return;
        float length = (float) Math.sqrt(moveX * moveX + moveY * moveY);
        moveX /= length;
        moveY /= length;

        pos.x += moveX * MOVE_SPEED * delta;
        pos.y += moveY * MOVE_SPEED * delta;
    }

    private void updateCameraFollow() {
        PositionComponent pos = possessed.get(PositionComponent.class);
        if (pos == null) return;
        camera.position.set(pos.x * tileSize, pos.y * tileSize, 0f);
        camera.update();
    }

    private void handleCameraPan(float delta) {
        float panX = 0f, panY = 0f;
        if (Gdx.input.isKeyPressed(Input.Keys.A)) panX -= 1f;
        if (Gdx.input.isKeyPressed(Input.Keys.D)) panX += 1f;
        if (Gdx.input.isKeyPressed(Input.Keys.S)) panY -= 1f;
        if (Gdx.input.isKeyPressed(Input.Keys.W)) panY += 1f;

        if (panX != 0f || panY != 0f) {
            camera.position.x += panX * CAMERA_PAN_SPEED * camera.zoom * delta;
            camera.position.y += panY * CAMERA_PAN_SPEED * camera.zoom * delta;
            camera.update();
        }
    }
}
