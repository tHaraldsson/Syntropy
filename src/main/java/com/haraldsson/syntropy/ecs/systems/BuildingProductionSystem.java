package com.haraldsson.syntropy.ecs.systems;

import com.haraldsson.syntropy.ecs.ECSWorld;
import com.haraldsson.syntropy.ecs.Entity;
import com.haraldsson.syntropy.ecs.GameSystem;
import com.haraldsson.syntropy.ecs.components.BuildingComponent;
import com.haraldsson.syntropy.entities.Item;
import com.haraldsson.syntropy.world.World;

public class BuildingProductionSystem extends GameSystem {
    @Override
    public void update(ECSWorld ecsWorld, World world, float delta) {
        for (Entity e : ecsWorld.getEntitiesWith(BuildingComponent.class)) {
            BuildingComponent b = e.get(BuildingComponent.class);
            if (!b.built) continue;
            b.timer += delta;
            if (b.timer >= b.productionInterval && b.outputBuffer.size() < b.maxOutput) {
                b.timer = 0f;
                b.outputBuffer.add(new Item(b.producedItemType));
            }
        }
    }
}

