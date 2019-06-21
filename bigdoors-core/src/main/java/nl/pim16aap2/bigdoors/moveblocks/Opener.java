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

    protected final void setBusy(DoorBase door)
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
            if (door.getBlockCount() > maxDoorSize)
                return true;
        return false;
    }

    protected final DoorOpenResult isOpenable(DoorBase door, boolean silent)
    {
        if (plugin.getDatabaseManager().isDoorBusy(door.getDoorUID()))
        {
            if (!silent)
                plugin.getMyLogger().warn("Door " + door.getName() + " is not available right now!");
            return DoorOpenResult.BUSY;
        }

        if (!chunksLoaded(door))
        {
            plugin.getMyLogger().warn(ChatColor.RED + "Chunk for door " + door.getName() + " is not loaded!");
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
            plugin.getMyLogger().warn("World is null for door \"" + door.getName().toString() + "\"");
            return false;
        }
        if (door.getWorld().getChunkAt(door.getMaximum()) == null)
        {
            plugin.getMyLogger().warn("Chunk at maximum for door \"" + door.getName().toString() + "\" is null!");
            return false;
        }
        if (door.getWorld().getChunkAt(door.getMinimum()) == null)
        {
            plugin.getMyLogger().warn("Chunk at minimum for door \"" + door.getName().toString() + "\" is null!");
            return false;
        }

        return door.getWorld().getChunkAt(door.getMaximum()).load() && door.getWorld().getChunkAt(door.getMinimum()).isLoaded();
    }

    public DoorOpenResult openDoor(final DoorBase door, final double time)
    {
        return openDoor(door, time, false, false);
    }

    public abstract DoorOpenResult openDoor(final DoorBase door, final double time, final boolean instantOpen, final boolean silent);
}
