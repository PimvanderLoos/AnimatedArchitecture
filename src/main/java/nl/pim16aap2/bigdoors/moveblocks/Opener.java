package nl.pim16aap2.bigdoors.moveblocks;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.Door;
import nl.pim16aap2.bigdoors.util.DoorOpenResult;

public abstract class Opener
{
    protected final BigDoors plugin;

    protected Opener(final BigDoors plugin)
    {
        this.plugin = plugin;
    }

    // Check if the chunks at the minimum and maximum locations of the door are loaded.
    protected final boolean chunksLoaded(final Door door)
    {
        // Return true if the chunk at the max and at the min of the chunks were loaded correctly.
        if (door.getWorld() == null)
            plugin.getMyLogger().logMessage("World is null for door \""    + door.getName().toString() + "\"",          true, false);
        if (door.getWorld().getChunkAt(door.getMaximum()) == null)
            plugin.getMyLogger().logMessage("Chunk at maximum for door \"" + door.getName().toString() + "\" is null!", true, false);
        if (door.getWorld().getChunkAt(door.getMinimum()) == null)
            plugin.getMyLogger().logMessage("Chunk at minimum for door \"" + door.getName().toString() + "\" is null!", true, false);

        return door.getWorld().getChunkAt(door.getMaximum()).load() && door.getWorld().getChunkAt(door.getMinimum()).isLoaded();
    }

    public DoorOpenResult openDoor(final Door door, final double time)
    {
        return openDoor(door, time, false, false);
    }

    public abstract DoorOpenResult openDoor(final Door door, final double time, final boolean instantOpen, final boolean silent);
}
