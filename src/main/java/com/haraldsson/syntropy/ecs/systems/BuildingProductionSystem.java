package com.haraldsson.syntropy.ecs.systems;

import com.haraldsson.syntropy.core.EventType;
import com.haraldsson.syntropy.core.GameEvents;
import com.haraldsson.syntropy.ecs.ECSWorld;
import com.haraldsson.syntropy.ecs.Entity;
import com.haraldsson.syntropy.ecs.GameSystem;
import com.haraldsson.syntropy.ecs.components.BuildingComponent;
import com.haraldsson.syntropy.entities.Item;
import com.haraldsson.syntropy.entities.ItemType;
import com.haraldsson.syntropy.world.World;

public class BuildingProductionSystem extends GameSystem {
    private GameEvents events;

    public void setEvents(GameEvents events) {
        this.events = events;
    }

    @Override
    public void update(ECSWorld ecsWorld, World world, float delta) {
        for (Entity e : ecsWorld.getEntitiesWith(BuildingComponent.class)) {
            BuildingComponent bc = e.get(BuildingComponent.class);
            if (!bc.built) continue;
            bc.timer += delta;
            if (bc.timer >= bc.productionInterval && bc.outputBuffer.size() < bc.maxOutput) {
                bc.timer = 0f;
                ItemType itemType = bc.producedItemType;
                bc.outputBuffer.add(new Item(itemType));
                if (events != null && itemType != null) {
                    events.fireAndLog(EventType.RESOURCE_PRODUCED, itemType.name(),
                            "PRODUCED: " + itemType.name() + " from " + bc.buildingType);
                }
            }
        }
    }
}
