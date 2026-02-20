package com.haraldsson.syntropy.input;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.haraldsson.syntropy.entities.Building;
import com.haraldsson.syntropy.entities.Colonist;
import com.haraldsson.syntropy.entities.FoodGrower;
import com.haraldsson.syntropy.entities.Item;
import com.haraldsson.syntropy.entities.Miner;
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
    private final OrthographicCamera camera;
    private final Viewport viewport;
    private final int tileSize;

    private Colonist possessed;
    private boolean scrollListenerAdded;

    private float lastClickTime;
    private float lastClickX;
    private float lastClickY;

    private String pickupMessage = "";
    private float pickupMessageTimer;

    public PlayerController(World world, OrthographicCamera camera, Viewport viewport, int tileSize) {
        this.world = world;
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
        if (possessed != null && possessed.isDead()) {
            possessed.setAiDisabled(false);
            possessed = null;
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

    public Colonist getPossessed() {
        return possessed;
    }

    public String getPickupMessage() {
        return pickupMessageTimer > 0 ? pickupMessage : "";
    }

    private void handleDoubleClickPossession() {
        if (!Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
            return;
        }

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

        if (!isDoubleClick) {
            return;
        }

        // Double-click detected — toggle possession
        if (possessed != null) {
            possessed.setAiDisabled(false);
            possessed.clearTask();
            possessed = null;
            return;
        }

        Vector2 cursorWorld = viewport.unproject(new Vector2(mx, my));
        float bestDist = Float.MAX_VALUE;
        Colonist nearest = null;
        for (Colonist colonist : world.getColonists()) {
            if (colonist.isDead()) continue;
            float cdx = colonist.getX() - (cursorWorld.x / tileSize);
            float cdy = colonist.getY() - (cursorWorld.y / tileSize);
            float dist = cdx * cdx + cdy * cdy;
            if (dist < bestDist) {
                bestDist = dist;
                nearest = colonist;
            }
        }
        if (nearest != null && bestDist < 4f) {
            possessed = nearest;
            possessed.setAiDisabled(true);
        }
    }

    /** Press E to pick up / drop items at the possessed colonist's feet. */
    private void handlePickup() {
        if (possessed == null || possessed.isDead()) return;
        if (!Gdx.input.isKeyJustPressed(Input.Keys.E)) return;

        int tileX = (int) possessed.getX();
        int tileY = (int) possessed.getY();
        Tile tile = world.getTile(tileX, tileY);
        if (tile == null) {
            showPickupMessage("No tile here!");
            return;
        }

        if (possessed.getCarriedItem() != null) {
            tile.addItem(possessed.getCarriedItem());
            showPickupMessage("Dropped " + possessed.getCarriedItem().getType().name());
            possessed.setCarriedItem(null);
            return;
        }

        // Try picking up from a building's output buffer first
        Building building = tile.getBuilding();
        if (building instanceof Miner) {
            Miner miner = (Miner) building;
            if (miner.hasOutput()) {
                Item item = miner.takeOutput();
                possessed.setCarriedItem(item);
                showPickupMessage("Picked up " + item.getType().name() + " from Miner");
                return;
            }
        }
        if (building instanceof FoodGrower) {
            FoodGrower grower = (FoodGrower) building;
            if (grower.hasOutput()) {
                Item item = grower.takeOutput();
                possessed.setCarriedItem(item);
                showPickupMessage("Picked up " + item.getType().name() + " from FoodGrower");
                return;
            }
        }

        // Otherwise pick up ground item
        if (!tile.getGroundItems().isEmpty()) {
            Item picked = tile.getGroundItems().remove(0);
            possessed.setCarriedItem(picked);
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
        float moveX = 0f;
        float moveY = 0f;
        if (Gdx.input.isKeyPressed(Input.Keys.W)) moveY += 1f;
        if (Gdx.input.isKeyPressed(Input.Keys.S)) moveY -= 1f;
        if (Gdx.input.isKeyPressed(Input.Keys.A)) moveX -= 1f;
        if (Gdx.input.isKeyPressed(Input.Keys.D)) moveX += 1f;

        if (moveX == 0f && moveY == 0f) return;
        float length = (float) Math.sqrt(moveX * moveX + moveY * moveY);
        moveX /= length;
        moveY /= length;

        possessed.setPosition(
                possessed.getX() + moveX * MOVE_SPEED * delta,
                possessed.getY() + moveY * MOVE_SPEED * delta
        );
    }

    private void updateCameraFollow() {
        camera.position.set(possessed.getX() * tileSize, possessed.getY() * tileSize, 0f);
        camera.update();
    }

    private void handleCameraPan(float delta) {
        float panX = 0f;
        float panY = 0f;
        if (Gdx.input.isKeyPressed(Input.Keys.A))  panX -= 1f;
        if (Gdx.input.isKeyPressed(Input.Keys.D))  panX += 1f;
        if (Gdx.input.isKeyPressed(Input.Keys.S))  panY -= 1f;
        if (Gdx.input.isKeyPressed(Input.Keys.W))  panY += 1f;

        if (panX != 0f || panY != 0f) {
            camera.position.x += panX * CAMERA_PAN_SPEED * camera.zoom * delta;
            camera.position.y += panY * CAMERA_PAN_SPEED * camera.zoom * delta;
            camera.update();
        }
    }
}
