package com.haraldsson.syntropy.input;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.haraldsson.syntropy.entities.Colonist;
import com.haraldsson.syntropy.world.World;

public class PlayerController {
    private static final float MOVE_SPEED = 3.2f;
    private static final float CAMERA_PAN_SPEED = 300f;
    private static final float ZOOM_STEP = 0.1f;
    private static final float ZOOM_MIN = 0.4f;
    private static final float ZOOM_MAX = 2.5f;

    private final World world;
    private final OrthographicCamera camera;
    private final Viewport viewport;
    private final int tileSize;

    private Colonist possessed;
    private boolean scrollListenerAdded;

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

        // Auto-unpossess if colonist dies
        if (possessed != null && possessed.isDead()) {
            possessed.setAiDisabled(false);
            possessed = null;
        }

        handlePossessionToggle();
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

    private void handlePossessionToggle() {
        if (!Gdx.input.isKeyJustPressed(Input.Keys.P)) {
            return;
        }
        if (possessed != null) {
            possessed.setAiDisabled(false);
            possessed.clearTask();
            possessed = null;
            return;
        }

        Vector2 cursorWorld = viewport.unproject(new Vector2(Gdx.input.getX(), Gdx.input.getY()));
        float bestDist = Float.MAX_VALUE;
        Colonist nearest = null;
        for (Colonist colonist : world.getColonists()) {
            if (colonist.isDead()) continue;
            float dx = colonist.getX() - (cursorWorld.x / tileSize);
            float dy = colonist.getY() - (cursorWorld.y / tileSize);
            float dist = dx * dx + dy * dy;
            if (dist < bestDist) {
                bestDist = dist;
                nearest = colonist;
            }
        }
        if (nearest != null) {
            possessed = nearest;
            possessed.setAiDisabled(true);
        }
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
        if (Gdx.input.isKeyPressed(Input.Keys.LEFT))  panX -= 1f;
        if (Gdx.input.isKeyPressed(Input.Keys.RIGHT)) panX += 1f;
        if (Gdx.input.isKeyPressed(Input.Keys.DOWN))   panY -= 1f;
        if (Gdx.input.isKeyPressed(Input.Keys.UP))     panY += 1f;

        if (panX != 0f || panY != 0f) {
            camera.position.x += panX * CAMERA_PAN_SPEED * camera.zoom * delta;
            camera.position.y += panY * CAMERA_PAN_SPEED * camera.zoom * delta;
            camera.update();
        }
    }
}
