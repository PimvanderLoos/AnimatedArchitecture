package nl.pim16aap2.bigDoors.moveBlocks;

import nl.pim16aap2.bigDoors.Door;

public interface BlockMover
{
    // Put blocks in their final position.
    // Use onDisable = false to make it safe to use during onDisable().
    public void putBlocks(boolean onDisable);
    public long getDoorUID();
    public Door getDoor();
}