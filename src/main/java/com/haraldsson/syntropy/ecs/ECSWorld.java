package com.haraldsson.syntropy.ecs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Entity registry. Stores all entities and provides query helpers.
 */
public class ECSWorld {
    private final List<Entity> entities = new ArrayList<>();

    public Entity createEntity() {
        Entity entity = new Entity();
        entities.add(entity);
        return entity;
    }

    public void addEntity(Entity entity) {
        entities.add(entity);
    }

    public void removeEntity(Entity entity) {
        entities.remove(entity);
    }

    public List<Entity> getAll() {
        return Collections.unmodifiableList(entities);
    }

    /**
     * Returns all entities that have ALL of the given component types.
     */
    @SafeVarargs
    public final List<Entity> getEntitiesWith(Class<? extends Component>... types) {
        List<Entity> result = new ArrayList<>();
        for (Entity entity : entities) {
            boolean hasAll = true;
            for (Class<? extends Component> type : types) {
                if (!entity.has(type)) {
                    hasAll = false;
                    break;
                }
            }
            if (hasAll) {
                result.add(entity);
            }
        }
        return result;
    }
}

