package nl.pim16aap2.bigDoors.util;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import net.md_5.bungee.api.ChatColor;
import nl.pim16aap2.bigDoors.Door;

public class Util
{
	// Send a message to a player in a specific color.
	public static void messagePlayer(Player player, ChatColor color, String s)
	{
		player.sendMessage(color + s);
	}
	
	// Send a message to a player.
	public static void messagePlayer(Player player, String s)
	{
		messagePlayer(player, ChatColor.WHITE, s);
	}

	// Print info for a door.
	public static void listDoorInfo(Player player, Door door)
	{
		Util.messagePlayer(player, "ID = " + door.getDoorUID()   + ", Name = " + door.getName().toString() + ", It is " + (door.isLocked() ? "" : "NOT ") + "locked" +
			"\nMinCoords ("    + door.getMinimum().getBlockX()   + ";"  + door.getMinimum().getBlockY()    + ";"+ door.getMinimum().getBlockZ() + ")"     +
			"\nMaxCoords ("    + door.getMaximum().getBlockX()   + ";"  + door.getMaximum().getBlockY()    + ";"+ door.getMaximum().getBlockZ() + ")"     +
			"\nEngineCoords (" + door.getEngine().getBlockX()    + ";"  + door.getEngine().getBlockY()     + ";"+ door.getEngine().getBlockZ()  + ")"   );
	}

	// Swap min and max values for type mode (0/1/2 -> X/Y/Z) for a specified door.
	public static void swap(Door door, int mode)
	{
		Location newMin = door.getMinimum();
		Location newMax = door.getMaximum();
		double temp;
		switch(mode)
		{
		case 0:
			temp = door.getMaximum().getX();
			newMax.setX(newMin.getX());
			newMin.setX(temp);
			break;
		case 1:
			temp = door.getMaximum().getY();
			newMax.setY(newMin.getY());
			newMin.setY(temp);
			break;
		case 2:
			temp = door.getMaximum().getZ();
			newMax.setZ(newMin.getZ());
			newMin.setZ(temp);
			break;
		}
	}
}
