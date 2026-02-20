package com.haraldsson.syntropy.ecs.systems;

import com.haraldsson.syntropy.ecs.ECSWorld;
import com.haraldsson.syntropy.ecs.Entity;
import com.haraldsson.syntropy.ecs.GameSystem;
import com.haraldsson.syntropy.ecs.components.HealthComponent;
import com.haraldsson.syntropy.ecs.components.InventoryComponent;
import com.haraldsson.syntropy.ecs.components.NeedsComponent;
import com.haraldsson.syntropy.ecs.components.PositionComponent;
import com.haraldsson.syntropy.entities.EnergyCategory;
import com.haraldsson.syntropy.entities.HungerCategory;
import com.haraldsson.syntropy.world.Tile;
import com.haraldsson.syntropy.world.World;

import java.util.ArrayList;
import java.util.List;

/**
 * Ticks hunger/energy decay. Health damage from starvation. Health regen when well-fed.
 * Mood is handled by MoodSystem (Pattern 2) — NOT here.
 */
public class NeedsSystem extends GameSystem {
    private static final float DEATH_DESPAWN_SECONDS = 30f;

    @Override
    public void update(ECSWorld ecsWorld, World world, float delta) {
        List<Entity> toRemove = new ArrayList<>();
        for (Entity e : ecsWorld.getEntitiesWith(NeedsComponent.class, HealthComponent.class)) {
            HealthComponent health = e.get(HealthComponent.class);
            if (health.dead) {
                // FIX: Drop carried items to ground tile on colonist death — 2026-02-20
                if (!health.deathItemsDropped) {
                    health.deathItemsDropped = true;
                    InventoryComponent inv = e.get(InventoryComponent.class);
                    PositionComponent pos = e.get(PositionComponent.class);
                    if (inv != null && inv.carriedItem != null && pos != null) {
                        Tile tile = world.getTile((int) pos.x, (int) pos.y);
                        if (tile != null) {
                            tile.addItem(inv.carriedItem);
                        }
                        inv.carriedItem = null;
                    }
                }
                health.deathTimer += delta;
                if (health.deathTimer >= DEATH_DESPAWN_SECONDS) {
                    toRemove.add(e);
                }
                continue;
            }

            NeedsComponent needs = e.get(NeedsComponent.class);
            needs.hunger = Math.max(0f, needs.hunger - NeedsComponent.HUNGER_DECAY * delta);
            needs.energy = Math.max(0f, needs.energy - NeedsComponent.ENERGY_DECAY * delta);

            // Starvation damage — only when STARVING, at half the original rate
            if (needs.getHungerCategory() == HungerCategory.STARVING) {
                needs.damage(0.025f * delta);
            }

            // Health regen when well-fed and rested
            if (needs.getHungerCategory() == HungerCategory.FED && needs.getEnergyCategory() == EnergyCategory.RESTED) {
                needs.heal(NeedsComponent.HEALTH_REGEN * delta);
            }

            if (needs.health <= 0f) {
                health.dead = true;
            }
        }
        for (Entity e : toRemove) {
            ecsWorld.removeEntity(e);
        }
    }
}
