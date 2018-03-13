package nl.pim16aap2.bigDoors.util;

import org.bukkit.entity.Player;

import net.md_5.bungee.api.ChatColor;

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
}
