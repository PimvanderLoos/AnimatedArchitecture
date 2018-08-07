package nl.pim16aap2.bigDoors.handlers;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import nl.pim16aap2.bigDoors.BigDoors;
import nl.pim16aap2.bigDoors.Door;
import nl.pim16aap2.bigDoors.moveBlocks.Opener;

public class RedstoneHandler implements Listener
{
	private final BigDoors plugin;
	private final Material powerBlock;
	
	public RedstoneHandler(BigDoors plugin)
	{
		this.plugin = plugin;
		this.powerBlock = Material.getMaterial(plugin.getConfigLoader().getString("powerBlockType"));
	}

	// Log players interacting with redstone source and log the time and location at which it happened.
	// Then, if a door is found, check for nearby players that triggered a redstone signal recently.
	// Use this information to check for permissions to open doors. Store it in an arraylist or something.
//	@EventHandler
	public void onRedStoneToggled(PlayerInteractEvent event)
	{
		// TODO: Check interactions with switches, placing redstone torches, hitting buttons (hand / arrow), pressure plates
	}
	
	// When redstone changes, check if there's a power block on any side of it (just not below it).
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

            Door door = null;
            int x = location.getBlockX(), y = location.getBlockY(), z = location.getBlockZ();
            if (     location.getWorld().getBlockAt(x, y, z - 1).getType() == powerBlock) // North
            		door = plugin.getCommander().doorFromEngineLoc(x, y + 1, z - 1);
            else if (location.getWorld().getBlockAt(x + 1, y, z).getType() == powerBlock) // East
            		door = plugin.getCommander().doorFromEngineLoc(x + 1, y + 1, z);
            else if (location.getWorld().getBlockAt(x, y, z + 1).getType() == powerBlock) // South
            		door = plugin.getCommander().doorFromEngineLoc(x, y + 1, z + 1);
            else if (location.getWorld().getBlockAt(x - 1, y, z).getType() == powerBlock) // West
        			door = plugin.getCommander().doorFromEngineLoc(x - 1, y + 1, z);
            else if (location.getWorld().getBlockAt(x, y + 1, z).getType() == powerBlock) // Above
            		door = plugin.getCommander().doorFromEngineLoc(x, y + 2, z);
            else 
            		return;
          
            if (door != null && !door.isLocked())
            		plugin.getDoorOpener(door.getType()).openDoor(door, 0.2, true);
        }
        catch (Exception e)
        {
			plugin.getMyLogger().logMessage("Exception thrown while handling redstone event!", true, false);
			plugin.getMyLogger().logMessage("79 " + e.getMessage());
        }
    }
}