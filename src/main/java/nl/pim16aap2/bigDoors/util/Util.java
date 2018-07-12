package nl.pim16aap2.bigDoors.util;

import org.bukkit.Location;
import org.bukkit.Material;
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
	
	// Send an array of messages to a player.
	public static void messagePlayer(Player player, String[] s)
	{
		String message = "";
		for (String str : s)
			message += str + "\n";
		messagePlayer(player, message);
	}
	
	// Send an array of messages to a player.
	public static void messagePlayer(Player player, ChatColor color, String[] s)
	{
		String message = "";
		for (String str : s)
			message += str + "\n";
		messagePlayer(player, color, message);
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
	
	
	public static boolean isAllowedBlock(Material mat)
	{
		switch(mat)
		{
		case ACACIA_DOOR:
		case ACTIVATOR_RAIL:
		case ARMOR_STAND:
		case BED_BLOCK:
		case BANNER:
		case BIRCH_DOOR:
		case BLACK_SHULKER_BOX:
		case BLUE_SHULKER_BOX:
		case BREWING_STAND:
		case BROWN_SHULKER_BOX:
		case BURNING_FURNACE:
		case CAKE_BLOCK:
		case CAULDRON:
		case CHEST:
		case CHORUS_FLOWER:
		case CHORUS_FRUIT:
		case CHORUS_PLANT:
		case CYAN_SHULKER_BOX:
		case DARK_OAK_DOOR:
		case DETECTOR_RAIL:
		case DROPPER:
		case DRAGON_EGG:
		case ENDER_CHEST:
		case FURNACE:
		case GRAY_SHULKER_BOX:
		case GREEN_SHULKER_BOX:
		case HOPPER:
		case JUKEBOX:
		case LIGHT_BLUE_SHULKER_BOX:
		case LIME_SHULKER_BOX:
		case MOB_SPAWNER:
		case NETHER_STALK:
		case NETHER_WART_BLOCK:
		case PAINTING:
		case POWERED_RAIL:
		case PUMPKIN_STEM:
		case PURPLE_SHULKER_BOX:
		case RAILS:
		case RED_ROSE:
		case RED_SHULKER_BOX:
		case REDSTONE_WIRE:
		case SAPLING:
		case SIGN:
		case SIGN_POST:
		case SILVER_SHULKER_BOX:
		case SKULL:
		case STANDING_BANNER:
		case SUGAR_CANE:
		case SUGAR_CANE_BLOCK:
		case TORCH:
		case TRAPPED_CHEST:
		case TRAP_DOOR:
		case TRIPWIRE:
		case TRIPWIRE_HOOK:
		case VINE:
		case WALL_BANNER:
		case WALL_SIGN:
		case WHITE_SHULKER_BOX:
		case WOOD_BUTTON:
		case WOOD_DOOR:
		case WOODEN_DOOR:
		case YELLOW_FLOWER:
		case YELLOW_SHULKER_BOX:
		return false;
		default:
			return true;
		}
	}
}
