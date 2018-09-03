package nl.pim16aap2.bigDoors.handlers;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import nl.pim16aap2.bigDoors.BigDoors;
import nl.pim16aap2.bigDoors.Door;

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
	
	public boolean checkDoor(Location loc)
	{
		Door door = plugin.getCommander().doorFromPowerBlockLoc(loc);
		
        if (door != null && !door.isLocked())
        		return plugin.getDoorOpener(door.getType()).openDoor(door, 0.2, false, true);
		return false;
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
            
			int x = location.getBlockX(), y = location.getBlockY(), z = location.getBlockZ();
			
			if (location.getWorld().getBlockAt(x, y, z - 1).getType() == powerBlock) // North
				checkDoor(new Location(location.getWorld(), x,     y,     z - 1));
			
			if (location.getWorld().getBlockAt(x + 1, y, z).getType() == powerBlock) // East
				checkDoor(new Location(location.getWorld(), x + 1, y,     z    ));
			
			if (location.getWorld().getBlockAt(x, y, z + 1).getType() == powerBlock) // South
				checkDoor(new Location(location.getWorld(), x,     y,     z + 1));
			
			if (location.getWorld().getBlockAt(x - 1, y, z).getType() == powerBlock) // West
				checkDoor(new Location(location.getWorld(), x - 1, y,     z    ));
			
			if (location.getWorld().getBlockAt(x, y + 1, z).getType() == powerBlock) // Above
				checkDoor(new Location(location.getWorld(), x,     y + 1, z    ));
			
			if (location.getWorld().getBlockAt(x, y + 1, z).getType() == powerBlock) // Under
				checkDoor(new Location(location.getWorld(), x,     y - 1, z    ));
        }
        catch (Exception e)
        {
			plugin.getMyLogger().logMessage("Exception thrown while handling redstone event!", true, false);
			plugin.getMyLogger().logMessage("79 " + e.getMessage());
        }
    }
}