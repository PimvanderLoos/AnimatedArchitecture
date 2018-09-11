package nl.pim16aap2.bigDoors.util;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import net.md_5.bungee.api.ChatColor;
import nl.pim16aap2.bigDoors.Door;

public final class Util
{
	// Send a message to a player in a specific color.
	public static void messagePlayer(Player player, ChatColor color, String s)
	{
		player.sendMessage(color + s);
	}
	
	// Play sound at a location.
	public static void playSound(Location loc, String sound, float volume, float pitch)
	{
		for (Entity ent : loc.getWorld().getNearbyEntities(loc, 15, 15, 15))
			if (ent instanceof Player)
				((Player) ent).playSound(loc, sound, volume, pitch);
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

	public static boolean isAir(Material mat)
	{
		XMaterial xmat = XMaterial.fromString(mat.toString());
		if (xmat == null)
			return false;
		return xmat.equals(XMaterial.AIR);
	}
	
	// Logs, stairs and glass panes can rotate, but they don't rotate in exactly the same way.
	public static int canRotate(Material mat)
	{
		XMaterial xmat = XMaterial.fromString(mat.toString());
		if (xmat.equals(XMaterial.ACACIA_LOG)             || xmat.equals(XMaterial.BIRCH_LOG)           || xmat.equals(XMaterial.DARK_OAK_LOG)       || 
			xmat.equals(XMaterial.JUNGLE_LOG)             || xmat.equals(XMaterial.OAK_LOG)             || xmat.equals(XMaterial.SPRUCE_LOG)         || 
			xmat.equals(XMaterial.STRIPPED_ACACIA_LOG)    || xmat.equals(XMaterial.STRIPPED_BIRCH_LOG)  || xmat.equals(XMaterial.STRIPPED_SPRUCE_LOG)|| 
			xmat.equals(XMaterial.STRIPPED_DARK_OAK_LOG)  || xmat.equals(XMaterial.STRIPPED_JUNGLE_LOG) || xmat.equals(XMaterial.STRIPPED_OAK_LOG))
			return 1;
		if (xmat.equals(XMaterial.ACACIA_STAIRS)          || xmat.equals(XMaterial.BIRCH_STAIRS)        || xmat.equals(XMaterial.BRICK_STAIRS)       || 
			xmat.equals(XMaterial.COBBLESTONE_STAIRS)     || xmat.equals(XMaterial.DARK_OAK_STAIRS)     || xmat.equals(XMaterial.JUNGLE_STAIRS)      || 
			xmat.equals(XMaterial.NETHER_BRICK_STAIRS)    || xmat.equals(XMaterial.PURPUR_STAIRS)       || xmat.equals(XMaterial.QUARTZ_STAIRS)      || 
			xmat.equals(XMaterial.RED_SANDSTONE_STAIRS)   || xmat.equals(XMaterial.SANDSTONE_STAIRS)    || xmat.equals(XMaterial.PRISMARINE_STAIRS)  || 
			xmat.equals(XMaterial.DARK_PRISMARINE_STAIRS) || xmat.equals(XMaterial.SPRUCE_STAIRS)       || xmat.equals(XMaterial.OAK_STAIRS)	      ||
			xmat.equals(XMaterial.PRISMARINE_BRICK_STAIRS)|| xmat.equals(XMaterial.RED_SANDSTONE_STAIRS)|| xmat.equals(XMaterial.STONE_BRICK_STAIRS))
			return 2;
		if (xmat.equals(XMaterial.WHITE_STAINED_GLASS)       || xmat.equals(XMaterial.YELLOW_STAINED_GLASS)          || 
			xmat.equals(XMaterial.PURPLE_STAINED_GLASS)      || xmat.equals(XMaterial.LIGHT_BLUE_STAINED_GLASS)      || 
			xmat.equals(XMaterial.GRAY_STAINED_GLASS)        || xmat.equals(XMaterial.GREEN_STAINED_GLASS)           || 
			xmat.equals(XMaterial.BLACK_STAINED_GLASS)       || xmat.equals(XMaterial.LIME_STAINED_GLASS)            || 
			xmat.equals(XMaterial.BLUE_STAINED_GLASS)        || xmat.equals(XMaterial.BROWN_STAINED_GLASS)           || 
			xmat.equals(XMaterial.CYAN_STAINED_GLASS)        || xmat.equals(XMaterial.RED_STAINED_GLASS)             ||
			xmat.equals(XMaterial.WHITE_STAINED_GLASS_PANE)  || xmat.equals(XMaterial.YELLOW_STAINED_GLASS_PANE)     || 
			xmat.equals(XMaterial.PURPLE_STAINED_GLASS_PANE) || xmat.equals(XMaterial.LIGHT_BLUE_STAINED_GLASS_PANE) || 
			xmat.equals(XMaterial.GRAY_STAINED_GLASS_PANE)   || xmat.equals(XMaterial.GREEN_STAINED_GLASS_PANE)      || 
			xmat.equals(XMaterial.BLACK_STAINED_GLASS_PANE)  || xmat.equals(XMaterial.LIME_STAINED_GLASS_PANE)       || 
			xmat.equals(XMaterial.BLUE_STAINED_GLASS_PANE)   || xmat.equals(XMaterial.BROWN_STAINED_GLASS_PANE)      || 
			xmat.equals(XMaterial.CYAN_STAINED_GLASS_PANE)   || xmat.equals(XMaterial.RED_STAINED_GLASS_PANE))
			return 3;
		return 0;
	}
	
	// Certain blocks don't work in doors, so don't allow their usage.
	public static boolean isAllowedBlock(Material mat)
	{
		XMaterial xmat = XMaterial.fromString(mat.toString());
//		if (xmat == null || mat.toString().equals("DOUBLE_STEP"))
		if (xmat == null)
			return false;
		switch(xmat)
		{
		case AIR:
		case WATER:
		case LAVA:

		case ARMOR_STAND:
		case BREWING_STAND:
		case CAULDRON:
		case CHEST:
		case DROPPER:
		case DRAGON_EGG:
		case ENDER_CHEST:
		case HOPPER:
		case JUKEBOX:
		case PAINTING:
		case SIGN:
		case WALL_SIGN:		
		case SPAWNER:
		case FURNACE:
		case	 FURNACE_MINECART:
		case CAKE:
		
		case WHITE_SHULKER_BOX:
		case YELLOW_SHULKER_BOX:
		case PURPLE_SHULKER_BOX:
		case LIGHT_BLUE_SHULKER_BOX:
		case GRAY_SHULKER_BOX:
		case GREEN_SHULKER_BOX:
		case BLACK_SHULKER_BOX:
		case LIME_SHULKER_BOX:
		case BLUE_SHULKER_BOX:
		case BROWN_SHULKER_BOX:
		case CYAN_SHULKER_BOX:
		case RED_SHULKER_BOX:

		case ACACIA_TRAPDOOR:
		case BIRCH_TRAPDOOR:
		case DARK_OAK_TRAPDOOR:
		case IRON_TRAPDOOR:
		case JUNGLE_TRAPDOOR:
		case OAK_TRAPDOOR:
		case SPRUCE_TRAPDOOR:
		case ACACIA_DOOR:
		case BIRCH_DOOR:
		case IRON_DOOR:
		case JUNGLE_DOOR:
		case OAK_DOOR:
		case SPRUCE_DOOR:
		case DARK_OAK_DOOR:
		
		case CREEPER_HEAD:
		case CREEPER_WALL_HEAD:
		case DRAGON_HEAD:
		case PISTON_HEAD:
		case PLAYER_HEAD:
		case PLAYER_WALL_HEAD:
		case ZOMBIE_HEAD:
		case ZOMBIE_WALL_HEAD:

		case RAIL:
		case DETECTOR_RAIL:
		case ACTIVATOR_RAIL:
		case POWERED_RAIL:
			
		case REDSTONE:
		case REDSTONE_WIRE:
		case TRAPPED_CHEST:
		case TRIPWIRE:
		case TRIPWIRE_HOOK:
		case REDSTONE_TORCH:
		case REDSTONE_WALL_TORCH:
		case TORCH:
			
		case BLACK_CARPET:
		case BLUE_CARPET:
		case BROWN_CARPET:
		case CYAN_CARPET:
		case GRAY_CARPET:
		case GREEN_CARPET:
		case LIGHT_BLUE_CARPET:
		case LIGHT_GRAY_CARPET:
		case LIME_CARPET:
		case MAGENTA_CARPET:
		case ORANGE_CARPET:
		case PINK_CARPET:
		case PURPLE_CARPET:
		case RED_CARPET:
		case WHITE_CARPET:
		case YELLOW_CARPET:
			
		case ACACIA_BUTTON:
		case BIRCH_BUTTON:
		case DARK_OAK_BUTTON:
		case JUNGLE_BUTTON:
		case OAK_BUTTON:
		case SPRUCE_BUTTON:
		case STONE_BUTTON:
			
		case ROSE_BUSH:
		case ATTACHED_MELON_STEM:
		case ATTACHED_PUMPKIN_STEM:
		case WHITE_TULIP:
		case DANDELION_YELLOW:
		case LILY_PAD:		
		case SUGAR_CANE:
		case PUMPKIN_STEM:
		case NETHER_WART:
		case NETHER_WART_BLOCK:
		case VINE:		
		case CHORUS_FLOWER:
		case CHORUS_FRUIT:
		case CHORUS_PLANT:
		case SUNFLOWER:
			
		case ACACIA_SAPLING:
		case BIRCH_SAPLING:
		case DARK_OAK_SAPLING:
		case JUNGLE_SAPLING:
		case OAK_SAPLING:
		case SPRUCE_SAPLING:
		case SHULKER_BOX:
		case LIGHT_GRAY_SHULKER_BOX:
		case MAGENTA_SHULKER_BOX:
		case ORANGE_SHULKER_BOX:
		case PINK_SHULKER_BOX:
		
		case BLACK_BED:
		case BLUE_BED:
		case BROWN_BED:
		case CYAN_BED:
		case GRAY_BED:
		case GREEN_BED:
		case LIME_BED:
		case MAGENTA_BED:
		case ORANGE_BED:
		case PINK_BED:
		case RED_BED:
		case WHITE_BED:
		case YELLOW_BED:
		case LIGHT_BLUE_BED:
		case LIGHT_GRAY_BED:

		case BLACK_BANNER:
		case BLACK_WALL_BANNER:
		case BLUE_BANNER:
		case BLUE_WALL_BANNER:
		case BROWN_BANNER:
		case BROWN_WALL_BANNER:
		case CYAN_BANNER:
		case CYAN_WALL_BANNER:
		case GRAY_BANNER:
		case GRAY_WALL_BANNER:
		case GREEN_BANNER:
		case GREEN_WALL_BANNER:
		case LIME_BANNER:
		case LIME_WALL_BANNER:
		case MAGENTA_BANNER:
		case MAGENTA_WALL_BANNER:
		case ORANGE_BANNER:
		case ORANGE_WALL_BANNER:
		case PINK_BANNER:
		case PINK_WALL_BANNER:
		case RED_BANNER:
		case RED_WALL_BANNER:
		case WHITE_BANNER:
		case WHITE_WALL_BANNER:
		case YELLOW_BANNER:
		case YELLOW_WALL_BANNER:
		case LIGHT_BLUE_BANNER:
		case LIGHT_BLUE_WALL_BANNER:
		case LIGHT_GRAY_BANNER:
		case LIGHT_GRAY_WALL_BANNER:
			return false;
		default:
			return true;
		}
	}
	
	public static boolean between(int value, int start, int end)
	{
		return value <= end && value >= start;
	}
}
