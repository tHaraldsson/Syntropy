package com.haraldsson.syntropy.ecs.components;

import com.haraldsson.syntropy.ecs.Component;

/**
 * Marks a tile entity as a bed.
 * Each colonist owns one bed (ownerEntityId). -1 = unowned.
 */
public class BedComponent implements Component {
    public int ownerEntityId = -1;
    public boolean occupied = false;
}
