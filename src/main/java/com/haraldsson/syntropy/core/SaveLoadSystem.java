package com.haraldsson.syntropy.core;

/**
 * Save/Load system — temporarily stubbed during ECS refactor.
 * TODO: Rewrite to serialize ECSWorld entities and components.
 */
public final class SaveLoadSystem {
    private SaveLoadSystem() {
    }

    public static void save(Object world, String fileName) {
        // TODO: ECS save
        throw new UnsupportedOperationException("Save not yet implemented for ECS");
    }

    public static Object load(String fileName) {
        // TODO: ECS load
        throw new UnsupportedOperationException("Load not yet implemented for ECS");
    }
}
