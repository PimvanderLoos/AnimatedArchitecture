package nl.pim16aap2.bigdoors.moveblocks;

import net.md_5.bungee.api.ChatColor;
import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.doors.DoorBase;
import nl.pim16aap2.bigdoors.spigotutil.DoorOpenResult;

public abstract class Opener
{
    protected final BigDoors plugin;

    protected Opener(final BigDoors plugin)
    {
        this.plugin = plugin;
    }

    private void setBusy(DoorBase door)
    {
        // Change door availability so it cannot be opened again (just temporarily,
        // don't worry!).
        plugin.getDatabaseManager().setDoorBusy(door.getDoorUID());
    }

    protected final DoorOpenResult abort(DoorBase door, DoorOpenResult result)
    {
        if (!result.equals(DoorOpenResult.BUSY))
            plugin.getDatabaseManager().setDoorAvailable(door.getDoorUID());
        return result;
    }

    protected final boolean isTooBig(DoorBase door)
    {
        // Make sure the doorSize does not exceed the total doorSize.
        // If it does, open the door instantly.
        int maxDoorSize = plugin.getConfigLoader().maxDoorSize();
        if (maxDoorSize != -1)
            return door.getBlockCount() > maxDoorSize;
        return false;
    }

    /**
     * Checks if a door is busy and set it to busy if that is the case.
     *
     * @param doorUID The UID of the door to check.
     * @return True if already busy.
     */
    private boolean isBusySetIfNot(final long doorUID)
    {
        if (plugin.getDatabaseManager().isDoorBusy(doorUID))
            return true;
        plugin.getDatabaseManager().setDoorBusy(doorUID);
        return false;
    }

    protected final DoorOpenResult isOpenable(DoorBase door, boolean silent)
    {
        if (isBusySetIfNot(door.getDoorUID()))
        {
            if (!silent)
                plugin.getPLogger().warn("Door " + door.getName() + " is not available right now!");
            return DoorOpenResult.BUSY;
        }

        if (!chunksLoaded(door))
        {
            plugin.getPLogger().warn(ChatColor.RED + "Chunk for door " + door.getName() + " is not loaded!");
            return DoorOpenResult.ERROR;
        }
        return DoorOpenResult.SUCCESS;
    }

    // Check if the chunks at the minimum and maximum locations of the door are loaded.
    protected final boolean chunksLoaded(final DoorBase door)
    {
        // Return true if the chunk at the max and at the min of the chunks were loaded correctly.
        if (door.getWorld() == null)
        {
            plugin.getPLogger().warn("World is null for door \"" + door.getName() + "\"");
            return false;
        }

        // Try to load doors and return if successful.
        return door.getWorld().getChunkAt(door.getMaximum()).load() &&
                door.getWorld().getChunkAt(door.getMinimum()).isLoaded();
    }

    public DoorOpenResult openDoor(final DoorBase door, final double time)
    {
        return openDoor(door, time, false, false);
    }

    // TODO: Get rid of (boolean silent).
    public abstract DoorOpenResult openDoor(final DoorBase door, final double time, final boolean instantOpen,
                                            final boolean silent);
}
