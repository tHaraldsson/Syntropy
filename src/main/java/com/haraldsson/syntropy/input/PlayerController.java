package com.haraldsson.syntropy.input;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.haraldsson.syntropy.ecs.ECSWorld;
import com.haraldsson.syntropy.ecs.Entity;
import com.haraldsson.syntropy.ecs.components.*;
import com.haraldsson.syntropy.entities.EnergyCategory;
import com.haraldsson.syntropy.entities.HungerCategory;
import com.haraldsson.syntropy.entities.Item;
import com.haraldsson.syntropy.entities.ItemType;
import com.haraldsson.syntropy.world.Tile;
import com.haraldsson.syntropy.world.TerrainType;
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

    private Entity leader;
    private String pickupMessage = "";
    private float pickupMessageTimer;
    private boolean buildModeActive = false;
    private float sleepTimer = 0f;

    public PlayerController(World world, ECSWorld ecsWorld, OrthographicCamera camera, Viewport viewport, int tileSize) {
        this.world = world;
        this.ecsWorld = ecsWorld;
        this.camera = camera;
        this.viewport = viewport;
        this.tileSize = tileSize;
        findLeader();
    }

    public void findLeader() {
        leader = null;
        for (Entity e : ecsWorld.getEntitiesWith(LeaderComponent.class)) {
            leader = e;
            break;
        }
    }

    public void update(float delta) {
        if (pickupMessageTimer > 0) pickupMessageTimer -= delta;
        if (sleepTimer > 0) sleepTimer -= delta;

        if (leader != null) {
            HealthComponent health = leader.get(HealthComponent.class);
            if (health != null && health.dead) {
                handleCameraPan(delta);
                return;
            }
            if (sleepTimer <= 0f) {
                handleLeaderMovement(delta);
            }
            handlePickup();
            handleEatAndSleep();
            handleBuildMode();
            updateCameraFollow();
        } else {
            handleCameraPan(delta);
        }
    }

    public Entity getLeader() { return leader; }

    public Entity getPossessed() { return leader; }

    public boolean getBuildModeActive() { return buildModeActive; }

    public String getPickupMessage() {
        return pickupMessageTimer > 0 ? pickupMessage : "";
    }

    private void handleLeaderMovement(float delta) {
        HealthComponent health = leader.get(HealthComponent.class);
        if (health != null && health.dead) return;

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

        float newX = pos.x + moveX * MOVE_SPEED * delta;
        float newY = pos.y + moveY * MOVE_SPEED * delta;

        if (world.isPassable((int) newX, (int) pos.y)) {
            pos.x = newX;
        }
        if (world.isPassable((int) pos.x, (int) newY)) {
            pos.y = newY;
        }
    }

    private void handleEatAndSleep() {
        NeedsComponent needs = leader.get(NeedsComponent.class);
        if (needs == null) return;

        // F key: eat food from stockpile when hungry
        if (Gdx.input.isKeyJustPressed(Input.Keys.F)) {
            if (needs.getHungerCategory() != HungerCategory.FED) {
                Tile stockpile = world.getStockpileTile();
                if (stockpile != null && stockpile.hasItem(ItemType.FOOD)) {
                    stockpile.takeFirstItem(ItemType.FOOD);
                    needs.eat();
                    showPickupMessage("Ate food");
                } else {
                    showPickupMessage("No food available");
                }
            } else {
                showPickupMessage("Not hungry");
            }
        }

        // Z key: rest in place for 3 seconds when tired
        if (Gdx.input.isKeyJustPressed(Input.Keys.Z)) {
            if (needs.getEnergyCategory() != EnergyCategory.RESTED) {
                needs.rest();
                sleepTimer = 3f;
                showPickupMessage("Rested");
            } else {
                showPickupMessage("Not tired");
            }
        }
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

    private void handleBuildMode() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.B)) {
            buildModeActive = !buildModeActive;
        }

        if (buildModeActive && Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
            Vector3 worldCoords = camera.unproject(new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0));
            int tileX = (int)(worldCoords.x / tileSize);
            int tileY = (int)(worldCoords.y / tileSize);
            tryPlaceBed(tileX, tileY);
        }
    }

    private void tryPlaceBed(int tileX, int tileY) {
        Tile tile = world.getTile(tileX, tileY);
        if (tile == null) return;

        TerrainType t = tile.getTerrainType();
        if (t != TerrainType.GRASS && t != TerrainType.DIRT) return;

        if (tile.getBuildingEntity() != null) return;

        Tile stockpile = world.getStockpileTile();
        if (stockpile == null) return;
        int woodCount = stockpile.countItems(ItemType.WOOD);
        if (woodCount < 3) {
            showPickupMessage("Need 3 Wood to place bed");
            return;
        }

        for (int i = 0; i < 3; i++) {
            stockpile.takeFirstItem(ItemType.WOOD);
        }

        Entity bedEntity = ecsWorld.createEntity();
        bedEntity.add(new PositionComponent(tileX, tileY));
        bedEntity.add(new BedComponent());
        tile.setBuildingEntity(bedEntity);

        showPickupMessage("Bed placed!");
        buildModeActive = false;
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