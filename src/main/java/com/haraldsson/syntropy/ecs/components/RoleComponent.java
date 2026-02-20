package com.haraldsson.syntropy.ecs.components;

import com.haraldsson.syntropy.ecs.Component;
import com.haraldsson.syntropy.entities.ColonistRole;

/**
 * Colonist role assignment — determines what jobs the Think Tree AI will execute.
 */
public class RoleComponent implements Component {
    public ColonistRole role = ColonistRole.IDLE;

    public RoleComponent() {}

    public RoleComponent(ColonistRole role) {
        this.role = role;
    }
}

