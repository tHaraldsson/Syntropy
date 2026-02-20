package com.haraldsson.syntropy.input;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.haraldsson.syntropy.ecs.ECSWorld;
import com.haraldsson.syntropy.ecs.Entity;
import com.haraldsson.syntropy.ecs.components.*;
import com.haraldsson.syntropy.entities.Item;
import com.haraldsson.syntropy.world.Tile;
import com.haraldsson.syntropy.world.World;

/**
 * Player controller for the permanent leader character.
 * No possession swapping — WASD always moves the leader.
 * Camera follows leader. When leader is dead, switches to free camera.
 */
public class PlayerController {
    private static final float MOVE_SPEED = 3.2f;
    private static final float CAMERA_PAN_SPEED = 300f;

    private final World world;
    private final ECSWorld ecsWorld;
    private final OrthographicCamera camera;
    private final Viewport viewport;
    private final int tileSize;

    private Entity leader; // cached reference
    private String pickupMessage = "";
    private float pickupMessageTimer;

    public PlayerController(World world, ECSWorld ecsWorld, OrthographicCamera camera, Viewport viewport, int tileSize) {
        this.world = world;
        this.ecsWorld = ecsWorld;
        this.camera = camera;
        this.viewport = viewport;
        this.tileSize = tileSize;
        findLeader();
    }

    /** Find the leader entity in the ECS world */
    public void findLeader() {
        leader = null;
        for (Entity e : ecsWorld.getEntitiesWith(LeaderComponent.class)) {
            leader = e;
            break;
        }
    }

    public void update(float delta) {
        if (pickupMessageTimer > 0) pickupMessageTimer -= delta;

        // If leader is dead, only allow camera pan
        if (leader != null) {
            HealthComponent health = leader.get(HealthComponent.class);
            if (health != null && health.dead) {
                handleCameraPan(delta);
                return;
            }
            handleLeaderMovement(delta);
            handlePickup();
            updateCameraFollow();
        } else {
            handleCameraPan(delta);
        }
    }

    public Entity getLeader() { return leader; }

    /** Used by GameApp to get the leader for HUD display, renamed from getPossessed */
    public Entity getPossessed() { return leader; }

    public String getPickupMessage() {
        return pickupMessageTimer > 0 ? pickupMessage : "";
    }

    private void handleLeaderMovement(float delta) {
        PositionComponent pos = leader.get(PositionComponent.class);
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

    private void handlePickup() {
        if (!Gdx.input.isKeyJustPressed(Input.Keys.E)) return;

        PositionComponent pos = leader.get(PositionComponent.class);
        InventoryComponent inv = leader.get(InventoryComponent.class);
        if (pos == null || inv == null) return;

        int tileX = Math.round(pos.x - 0.5f);
        int tileY = Math.round(pos.y - 0.5f);

        if (inv.carriedItem != null) {
            Tile tile = world.getTile(tileX, tileY);
            if (tile != null) {
                tile.addItem(inv.carriedItem);
                showPickupMessage("Dropped " + inv.carriedItem.getType().name());
                inv.carriedItem = null;
            }
            return;
        }

        int[][] offsets = {{0,0}, {-1,0}, {1,0}, {0,-1}, {0,1}};
        for (int[] off : offsets) {
            int cx = tileX + off[0];
            int cy = tileY + off[1];
            Tile tile = world.getTile(cx, cy);
            if (tile == null) continue;

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

            if (!tile.getGroundItems().isEmpty()) {
                Item picked = tile.getGroundItems().remove(0);
                inv.carriedItem = picked;
                showPickupMessage("Picked up " + picked.getType().name());
                return;
            }
        }

        showPickupMessage("Nothing nearby");
    }

    private void showPickupMessage(String msg) {
        pickupMessage = msg;
        pickupMessageTimer = 2f;
    }

    private void updateCameraFollow() {
        PositionComponent pos = leader.get(PositionComponent.class);
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
