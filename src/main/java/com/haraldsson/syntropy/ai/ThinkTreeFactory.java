package com.haraldsson.syntropy.ai;

import com.haraldsson.syntropy.ai.nodes.*;

/**
 * Factory that builds Think Trees for different entity types.
 */
public final class ThinkTreeFactory {
    private ThinkTreeFactory() {}

    /**
     * Default colonist Think Tree (context priority order):
     * 1. EatFood (priority 100 when starving)
     * 2. Rest (priority 80 when collapsed)
     * 3. DoAssignedJob (priority 50 — uses WorkSettings)
     * 4. Haul (priority 50 — general hauling fallback)
     * 5. Socialize (priority 10 — when needs met)
     * 6. Wander (priority 1 — idle fallback)
     */
    public static ThinkTreeRoot createColonistTree() {
        return new ThinkTreeRoot()
                .addChild(new ThinkNode_EatFood())
                .addChild(new ThinkNode_Rest())
                .addChild(new ThinkNode_DoAssignedJob())
                .addChild(new ThinkNode_Haul())
                .addChild(new ThinkNode_Socialize())
                .addChild(new ThinkNode_Wander());
    }
}
