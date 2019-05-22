package nl.pim16aap2.bigdoors.handlers;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockRedstoneEvent;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.Door;
import nl.pim16aap2.bigdoors.util.DoorOpenResult;

public class RedstoneHandler implements Listener
{
    private final BigDoors plugin;

    public RedstoneHandler(BigDoors plugin)
    {
        this.plugin = plugin;
    }

    public boolean checkDoor(Location loc)
    {
        Door door = plugin.getDatabaseManager().doorFromPowerBlockLoc(loc);

        if (door != null && !door.isLocked())
            return plugin.getDoorOpener(door.getType()).openDoor(door, 0.0, false, true) == DoorOpenResult.SUCCESS;
        return false;
    }

    // When redstone changes, check if there's a power block on any side of it (just
    // not below it).
    // If so, a door has (probably) been found, so try to open it.
    @EventHandler
    public void onBlockRedstoneChange(BlockRedstoneEvent event)
    {
        try
        {
            Block block = event.getBlock();
            Location location = block.getLocation();
            if (event.getOldCurrent() != 0 && event.getNewCurrent() != 0)
                return;

            int x = location.getBlockX(), y = location.getBlockY(), z = location.getBlockZ();

            if (plugin.getConfigLoader().powerBlockTypes().contains(location.getWorld().getBlockAt(x, y, z - 1).getType())) // North
                checkDoor(new Location(location.getWorld(), x, y, z - 1));

            if (plugin.getConfigLoader().powerBlockTypes().contains(location.getWorld().getBlockAt(x + 1, y, z).getType())) // East
                checkDoor(new Location(location.getWorld(), x + 1, y, z));

            if (plugin.getConfigLoader().powerBlockTypes().contains(location.getWorld().getBlockAt(x, y, z + 1).getType())) // South
                checkDoor(new Location(location.getWorld(), x, y, z + 1));

            if (plugin.getConfigLoader().powerBlockTypes().contains(location.getWorld().getBlockAt(x - 1, y, z).getType())) // West
                checkDoor(new Location(location.getWorld(), x - 1, y, z));

            if (plugin.getConfigLoader().powerBlockTypes().contains(location.getWorld().getBlockAt(x, y + 1, z).getType())) // Above
                checkDoor(new Location(location.getWorld(), x, y + 1, z));

            if (plugin.getConfigLoader().powerBlockTypes().contains(location.getWorld().getBlockAt(x, y - 1, z).getType())) // Under
                checkDoor(new Location(location.getWorld(), x, y - 1, z));
        }
        catch (Exception e)
        {
            plugin.getMyLogger().logMessage("Exception thrown while handling redstone event!", true, false);
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            plugin.getMyLogger().logMessageToLogFile("79 " + sw.toString());
        }
    }
}