package com.haraldsson.syntropy.ecs.systems;

import com.haraldsson.syntropy.ecs.ECSWorld;
import com.haraldsson.syntropy.ecs.Entity;
import com.haraldsson.syntropy.ecs.GameSystem;
import com.haraldsson.syntropy.ecs.components.*;
import com.haraldsson.syntropy.world.Tile;
import com.haraldsson.syntropy.world.World;

import java.util.ArrayList;
import java.util.List;

/**
 * Aging system — ticks age for all entities with AgingComponent.
 * Handles natural death and leader succession.
 */
public class AgingSystem extends GameSystem {
    private boolean successionNeeded = false;
    private String deathMessage = "";

    @Override
    public void update(ECSWorld ecsWorld, World world, float delta) {
        successionNeeded = false;

        // Despawn dead entities after 30 seconds
        for (Entity e : ecsWorld.getEntitiesWith(HealthComponent.class)) {
            HealthComponent health = e.get(HealthComponent.class);
            if (!health.dead) continue;
            health.deathTimer += delta;
            if (health.deathTimer >= 30f) {
                // Drop any carried item onto the ground before removing
                InventoryComponent inv = e.get(InventoryComponent.class);
                PositionComponent pos = e.get(PositionComponent.class);
                if (inv != null && inv.carriedItem != null && pos != null) {
                    Tile tile = world.getTile((int) pos.x, (int) pos.y);
                    if (tile != null) tile.addItem(inv.carriedItem);
                    inv.carriedItem = null;
                }
                ecsWorld.removeEntity(e);
            }
        }

        for (Entity e : ecsWorld.getEntitiesWith(AgingComponent.class, HealthComponent.class)) {
            HealthComponent health = e.get(HealthComponent.class);
            if (health.dead) continue;

            AgingComponent aging = e.get(AgingComponent.class);
            boolean yearPassed = aging.tick(delta);

            if (aging.shouldDieOfOldAge()) {
                health.dead = true;
                IdentityComponent id = e.get(IdentityComponent.class);
                String name = id != null ? id.name : "Unknown";

                if (e.has(LeaderComponent.class)) {
                    successionNeeded = true;
                    deathMessage = "Leader " + name + " has died of old age at " + (int) aging.ageYears + "!";
                }
            }
        }
    }

    public boolean isSuccessionNeeded() { return successionNeeded; }
    public String getDeathMessage() { return deathMessage; }

    /** Called externally when the leader dies by any cause other than old age. */
    public void triggerSuccession(String message) {
        this.successionNeeded = true;
        this.deathMessage = message;
    }

    /**
     * Get list of living colonists who could be successors.
     */
    public List<Entity> getSuccessorCandidates(ECSWorld ecsWorld) {
        List<Entity> candidates = new ArrayList<>();
        for (Entity e : ecsWorld.getEntitiesWith(
                IdentityComponent.class, HealthComponent.class, AgingComponent.class)) {
            if (e.has(LeaderComponent.class)) continue;
            HealthComponent health = e.get(HealthComponent.class);
            if (health.dead) continue;
            AgingComponent aging = e.get(AgingComponent.class);
            if (aging.ageYears >= 16) { // must be adult
                candidates.add(e);
            }
        }
        return candidates;
    }

    /**
     * Promote a colonist to leader. Inherits partial stats from old leader.
     */
    public void promoteToLeader(Entity successor, Entity oldLeader) {
        // Transfer leader component
        LeaderComponent newLC = new LeaderComponent();
        if (oldLeader != null) {
            LeaderComponent oldLC = oldLeader.get(LeaderComponent.class);
            if (oldLC != null) {
                // Inherit ~60% of old leader stats
                newLC.charisma = oldLC.charisma * 0.6f + 2f;
                newLC.engineering = oldLC.engineering * 0.6f + 2f;
                newLC.science = oldLC.science * 0.6f + 1f;
                newLC.combat = oldLC.combat * 0.6f + 1f;
            }
            oldLeader.remove(LeaderComponent.class);
        }
        successor.add(newLC);

        // Re-enable AI disabled flag removal (leader is player-controlled)
        AIComponent ai = successor.get(AIComponent.class);
        if (ai != null) {
            ai.aiDisabled = true;
            ai.clearTask();
        }
    }
}

