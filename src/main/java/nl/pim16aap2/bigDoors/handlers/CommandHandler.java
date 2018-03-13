package nl.pim16aap2.bigDoors.handlers;

import java.util.List;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import net.md_5.bungee.api.ChatColor;

import nl.pim16aap2.bigDoors.BigDoors;
import nl.pim16aap2.bigDoors.Door;
import nl.pim16aap2.bigDoors.DoorCreator;
import nl.pim16aap2.bigDoors.util.Util;

public class CommandHandler implements CommandExecutor
{
	BigDoors plugin;
	
	public CommandHandler(BigDoors plugin)
	{
		this.plugin = plugin;
	}
	
	public void stopDoors()
	{
		plugin.setCanGo(false);
		new BukkitRunnable()
		{
			@Override
			public void run()
			{
				plugin.setCanGo(true);
			}
		}.runTaskLater(plugin, 5L);
	}
	
	// Open the door.
	public void openDoorCommand(CommandSender sender, Door door, double speed)
	{
		Bukkit.broadcastMessage("speed = " + speed);
		if (!plugin.getDoorOpener().openDoor(door, speed))
			plugin.returnToSender(sender, Level.INFO, ChatColor.RED, "This door cannot be opened! Check if one side of the \"engine\" blocks is unobstructed!");
	}
	
	// Get the number of doors owned by player player with name doorName (or any name, if doorName == null)
	public long countDoors(Player player, String doorName)
	{
		return plugin.getRDatabase().countDoors(player.getUniqueId().toString(), doorName);
	}

	// Print a list of the doors currently in the db.
	public void listDoors(Player player, String name)
	{
		List<Door> doors = plugin.getRDatabase().getDoors(player.getUniqueId().toString(), name);
		for (Door door : doors)
			Util.messagePlayer(player, door.getDoorUID() + ": " + door.getName().toString());
	}

	// Create a new door.
	public void makeDoor(Player player, String name)
	{
		try
		{
			Integer.parseInt(name);
			return;
		}
		catch(NumberFormatException e)
		{
			plugin.logMessage("Could not parse to int!", true, false);
		}
		
		DoorCreator dc = new DoorCreator(plugin, player, name);
		plugin.getDoorCreators().add(dc);
		
		new BukkitRunnable()
		{
			int count = 0;
			
			@Override
			public void run()
			{
				if (dc != null && dc.isDone())	// Cancel and cleanup when the door creation process is doen.
				{
					dc.finishUp();
					plugin.getDoorCreators().remove(dc);
					this.cancel();
				}
				else if (count > 120) // Cancel after 1 minute.
				{
					dc.takeToolFromPlayer();
					plugin.getDoorCreators().remove(dc);
					plugin.returnToSender((CommandSender) player, Level.INFO, ChatColor.RED, "Time's up! Door creation failed, please try again.");
					this.cancel();
				}
				++count;
			}
		}.runTaskTimer(plugin, 0, 10);
	}
	
	public double speedFromString(String input)
	{
		try
		{
			return Double.parseDouble(input);
		}
		catch (NumberFormatException e)
		{
			return 0.2;
		}
	}
	
	// Handle commands.
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args)
	{
		Player player;
		
		// stopdoors
		if (cmd.getName().equalsIgnoreCase("stopdoors"))
		{
			stopDoors();
			return true;
		}
		
		// pausedoors
		if (cmd.getName().equalsIgnoreCase("pausedoors"))
		{
			plugin.togglePaused();
			return true;
		}
			
		// /shutup <debugLevel>
		if (cmd.getName().equalsIgnoreCase("shutup"))
			if (args.length == 1)
			{
				try
				{
					plugin.setDebugLevel(Integer.parseInt(args[0]));
				}
				catch (NumberFormatException e)
				{
					plugin.returnToSender(sender, Level.INFO, ChatColor.RED, "Expected numerical input!");
				}
				return true;
			}

		if (sender instanceof Player)
		{
			player = (Player) sender;
			
			// /listdoors [name]
			if (cmd.getName().equalsIgnoreCase("listdoors"))
			{
				if (args.length == 0)
					listDoors(player, null);
				else if (args.length == 1)
					listDoors(player, args[0]);
				return true;
			}
			
			// /unlockdoor <doorName>
			if (cmd.getName().equalsIgnoreCase("unlockdoor"))
				if (args.length == 1)
				{
					Door door = plugin.getDoor(args[0], player);
					if (	door != null)
					{
						door.changeAvailability(true);
						plugin.returnToSender(sender, Level.INFO, ChatColor.GREEN, "Door unlocked!");
						return true;
					}
				}
			
			// deldoor <doorName>
			if (cmd.getName().equalsIgnoreCase("deldoor"))
				if (args.length == 1)
				{
					delDoor(sender, player, args[0]);
					return true;
				}
				
			// /opendoors <doorName1> <doorName2> etc etc [speed]
			if (cmd.getName().equalsIgnoreCase("opendoors"))
				if (args.length >= 2)
				{
					// If the last argument is not a door (so getDoor returns null), it should be the speed. If it it null, use default speed.
					double speed = plugin.getDoor(args[args.length - 1], player) == null ? speedFromString(args[args.length - 1]) : 0.2;
					for (int index = 0; index < args.length; ++index)
					{
						Door door = plugin.getDoor(args[index], player);
						if (door == null && index != args.length - 1)
							plugin.returnToSender(sender, Level.INFO, ChatColor.RED, "\"" + args[index] + "\" is not a valid door name!");
						else if (door != null)
							openDoorCommand(sender, door, speed);
					}
					return true;
				}
	
			// /opendoor <doorName>
			if (cmd.getName().equalsIgnoreCase("opendoor"))
				if (args.length >= 1)
				{
					Door door = plugin.getDoor(args[0], player);
					if (door == null)
						plugin.returnToSender(sender, Level.INFO, ChatColor.RED, "\"" + args[0] + "\" is not a valid door name!");
					else
					{
						double speed = args.length == 2 ? (args[1] == null ? 0.2 : speedFromString(args[1])) : 0.2;
						openDoorCommand(sender, door, speed);
					}
					return true;
				}
				
			// /newdoor <doorName>
			if (cmd.getName().equalsIgnoreCase("newdoor"))
				if (args.length == 1)
				{
					makeDoor(player, args[0]);
					return true;
				}
				
			// /fixdoor
			if (cmd.getName().equalsIgnoreCase("fixdoor"))
				if (args.length >= 1)
				{
					Door door = plugin.getDoor(args[0], player);
					if (door != null)
						verifyDoorCoords(door);
					else
						Util.messagePlayer(player, ChatColor.RED, "Not a valid door name!");
					return true;
				}
		}
		return false;
	}
	
	// Check if min really is min and max really max.
	public void verifyDoorCoords(Door door)
	{
		int xMin, yMin, zMin;
		int xMax, yMax, zMax;
		xMin = door.getMinimum().getBlockX();
		yMin = door.getMinimum().getBlockY();
		zMin = door.getMinimum().getBlockZ();
		xMax = door.getMaximum().getBlockX();
		yMax = door.getMaximum().getBlockY();
		zMax = door.getMaximum().getBlockZ();
		if (xMin > xMax)
			plugin.swap(door, 0);
		if (yMin > yMax)
			plugin.swap(door, 1);
		if (zMin > zMax)
			plugin.swap(door, 2);
	}
	
	public void delDoor(CommandSender sender, Player player, String doorName)
	{
		try
		{
			int doorUID  = Integer.parseInt(doorName);
			plugin.getRDatabase().removeDoor(doorUID);
			plugin.returnToSender(sender, Level.INFO, ChatColor.GREEN, "Door deleted!");
			return;
		}
		catch(NumberFormatException e)
		{
			plugin.logMessage("Could not parse to int!", true, false);
		}
		
		long doorCount = countDoors(player, null);
		if (doorCount == 0)
			plugin.returnToSender(sender, Level.INFO, ChatColor.RED, "No door found by that name!");
		else if (doorCount == 1)
		{
			plugin.getRDatabase().removeDoors(player.getUniqueId().toString(), doorName);
			plugin.returnToSender(sender, Level.INFO, ChatColor.GREEN, "Door deleted!");
		}
		else
		{
			plugin.returnToSender(sender, Level.INFO, ChatColor.RED, "More than one door found with that name, did you mean to use /deldoors <doorNames> to delete them all? Otherwise use their ID instead:");
			listDoors(player, doorName);
		}
	}	
}
