package nl.pim16aap2.bigdoors.moveblocks;

import java.util.logging.Level;

import net.md_5.bungee.api.ChatColor;
import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.Door;
import nl.pim16aap2.bigdoors.util.DoorOpenResult;

public class FlagOpener extends Opener
{
    public FlagOpener(BigDoors plugin)
    {
        super(plugin);
    }

    // Open a door.
    @Override
    public DoorOpenResult openDoor(Door door, double time, boolean instantOpen, boolean silent)
    {
        if (plugin.getDatabaseManager().isDoorBusy(door.getDoorUID()))
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
        plugin.getDatabaseManager().setDoorBusy(door.getDoorUID());

        plugin.addBlockMover(new FlagMover(plugin, door.getWorld(), 60, door, plugin.getConfigLoader().flMultiplier()));

        return DoorOpenResult.SUCCESS;
    }
}
