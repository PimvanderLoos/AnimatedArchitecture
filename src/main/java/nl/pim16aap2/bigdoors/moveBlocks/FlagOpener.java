package nl.pim16aap2.bigdoors.moveBlocks;

import java.util.logging.Level;

import net.md_5.bungee.api.ChatColor;
import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.Door;
import nl.pim16aap2.bigdoors.util.DoorOpenResult;

public class FlagOpener implements Opener
{
    private final BigDoors plugin;

    public FlagOpener(BigDoors plugin)
    {
        this.plugin = plugin;
    }

    // Check if the chunks at the minimum and maximum locations of the door are loaded.
    private boolean chunksLoaded(Door door)
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

    @Override
    public DoorOpenResult openDoor(Door door, double time)
    {
        return openDoor(door, time, false, false);
    }

    // Open a door.
    @Override
    public DoorOpenResult openDoor(Door door, double time, boolean instantOpen, boolean silent)
    {
        if (plugin.getCommander().isDoorBusy(door.getDoorUID()))
        {
            if (!silent)
                plugin.getMyLogger().myLogger(Level.INFO, "Door " + door.getName() + " is not available right now!");
            return DoorOpenResult.BUSY;
        }

        if (!chunksLoaded(door))
        {
            plugin.getMyLogger().logMessage(ChatColor.RED + "Chunk for door " + door.getName() + " is not loaded!", true, false);
            return DoorOpenResult.ERROR;
        }

        // Make sure the doorSize does not exceed the total doorSize.
        // If it does, open the door instantly.
        int maxDoorSize = plugin.getConfigLoader().maxDoorSize();
        if (maxDoorSize != -1)
            if(door.getBlockCount() > maxDoorSize)
            {
                plugin.getMyLogger().myLogger(Level.INFO, "Flag " + door.getName() + " is too big!");
                return DoorOpenResult.ERROR;
            }

        // Change door availability so it cannot be opened again (just temporarily, don't worry!).
        plugin.getCommander().setDoorBusy(door.getDoorUID());

        plugin.addBlockMover(new FlagMover(plugin, door.getWorld(), 60, door));

        return DoorOpenResult.SUCCESS;
    }
}
