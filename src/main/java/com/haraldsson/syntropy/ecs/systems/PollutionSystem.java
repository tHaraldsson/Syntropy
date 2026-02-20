package com.haraldsson.syntropy.ecs.systems;

import com.haraldsson.syntropy.ecs.ECSWorld;
import com.haraldsson.syntropy.ecs.Entity;
import com.haraldsson.syntropy.ecs.GameSystem;
import com.haraldsson.syntropy.ecs.components.*;
import com.haraldsson.syntropy.world.World;

/**
 * Global pollution system.
 * Buildings with pollution output contribute to a global planetary health meter.
 * As pollution rises, colonists get debuffs and disasters become more frequent.
 */
public class PollutionSystem extends GameSystem {
    private float globalPollution = 0f;       // 0 = pristine, 100 = uninhabitable
    private float planetaryHealth = 100f;     // inverse of pollution, for display
    private static final float MAX_POLLUTION = 100f;
    private static final float NATURAL_DECAY = 0.05f; // slow natural cleanup per second

    @Override
    public void update(ECSWorld ecsWorld, World world, float delta) {
        // Accumulate pollution from industrial buildings
        float pollutionPerSecond = 0f;
        for (Entity e : ecsWorld.getEntitiesWith(BuildingComponent.class)) {
            BuildingComponent bc = e.get(BuildingComponent.class);
            if (!bc.built) continue;
            pollutionPerSecond += bc.pollutionRate;
        }

        globalPollution += pollutionPerSecond * delta;
        globalPollution -= NATURAL_DECAY * delta; // nature tries to recover
        globalPollution = Math.max(0f, Math.min(MAX_POLLUTION, globalPollution));
        planetaryHealth = MAX_POLLUTION - globalPollution;

        // Apply debuffs to colonists based on pollution severity
        if (globalPollution > 20f) {
            float severity = (globalPollution - 20f) / 80f; // 0-1 scale above threshold
            for (Entity e : ecsWorld.getEntitiesWith(NeedsComponent.class, HealthComponent.class)) {
                HealthComponent health = e.get(HealthComponent.class);
                if (health.dead) continue;
                NeedsComponent needs = e.get(NeedsComponent.class);
                needs.damage(severity * 0.002f * delta); // slow health drain from pollution
            }
        }
    }

    public float getGlobalPollution() { return globalPollution; }
    public float getPlanetaryHealth() { return planetaryHealth; }

    /** Severity tier for HUD display */
    public String getSeverityLabel() {
        if (globalPollution < 10f) return "Pristine";
        if (globalPollution < 30f) return "Clean";
        if (globalPollution < 50f) return "Polluted";
        if (globalPollution < 75f) return "Degraded";
        if (globalPollution < 90f) return "Critical";
        return "Uninhabitable";
    }
}

