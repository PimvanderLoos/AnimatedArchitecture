package nl.pim16aap2.bigDoors.moveBlocks;

import nl.pim16aap2.bigDoors.Door;

public interface BlockMover
{
    // Put blocks in their final position.
    // Use onDisable = false to make it safe to use during onDisable().
    public void putBlocks(boolean onDisable);

    public long getDoorUID();

    public Door getDoor();

    /**
     * Gets the number of ticks the door should to the delay to make sure the second
     * toggle of a button doesn't toggle the door again.
     *
     * @param endCount The number of ticks the animation took.
     * @return The number of ticks to wait before a button cannot toggle the door
     *         again.
     */
    default int buttonDelay(final int endCount)
    {
        return Math.max(0, 17 - endCount);
    }
}