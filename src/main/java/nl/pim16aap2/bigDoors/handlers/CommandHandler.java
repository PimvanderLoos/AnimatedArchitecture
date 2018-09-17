package nl.pim16aap2.bigDoors.handlers;

import java.util.ArrayList;
import java.util.UUID;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import com.mysql.jdbc.Messages;

import net.md_5.bungee.api.ChatColor;
import nl.pim16aap2.bigDoors.BigDoors;
import nl.pim16aap2.bigDoors.Door;
import nl.pim16aap2.bigDoors.DoorCreator;
import nl.pim16aap2.bigDoors.PortcullisCreator;
import nl.pim16aap2.bigDoors.PowerBlockRelocator;
import nl.pim16aap2.bigDoors.GUI.GUIPage;
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
		plugin.getCommander().setCanGo(false);
		plugin.getCommander().emptyBusyDoors();
		new BukkitRunnable()
		{
			@Override
			public void run()
			{
				plugin.getCommander().setCanGo(true);
			}
		}.runTaskLater(plugin, 5L);
	}
	
	// Open the door.
	public void openDoorCommand(CommandSender sender, Door door, double time)
	{
		// Get a new instance of the door to make sure the locked / unlocked status is recent.
		if (plugin.getCommander().getDoor(door.getDoorUID()).isLocked())
			plugin.getMyLogger().returnToSender(sender, Level.INFO, ChatColor.RED, "This door is locked!");
		// If the sender is a Player && the player's permission level is larger than 1 for this door, the player doesn't have permission (for now).
		else if (sender instanceof Player && plugin.getCommander().getPermission(((Player) (sender)).getUniqueId().toString(), door.getDoorUID()) > 1)
			plugin.getMyLogger().returnToSender(sender, Level.INFO, ChatColor.RED, "You do not have permission to open this door!");
		else if (!plugin.getDoorOpener(door.getType()).openDoor(door, time))
			plugin.getMyLogger().returnToSender(sender, Level.INFO, ChatColor.RED, "This door cannot be opened! Check if one side of the \"engine\" blocks is unobstructed!");
	}
	
	public void openDoorCommand(Player player, Door door, double time)
	{
		openDoorCommand((CommandSender) player, door, time);
	}
	
	public void openDoorCommand(Player player, Door door)
	{
		openDoorCommand((CommandSender) player, door, 0.0);
	}
	
	public void lockDoorCommand(Player player, Door door)
	{
		plugin.getCommander().setLock(door.getDoorUID(), !door.isLocked());
	}
	
	// Get the number of doors owned by player player with name doorName (or any name, if doorName == null)
	public long countDoors(Player player, String doorName)
	{
		return plugin.getCommander().countDoors(player.getUniqueId().toString(), doorName);
	}

	// Print a list of the doors currently in the db.
	public void listDoors(Player player, String doorName)
	{
		ArrayList<Door> doors = plugin.getCommander().getDoors(player.getUniqueId().toString(), doorName);
		for (Door door : doors)
			Util.messagePlayer(player, getBasicDoorInfo(door));
	}
	
	// Print a list of the doors currently in the db.
	public void listDoorInfo(Player player, String name)
	{
		long doorUID = Util.longFromString(name, -1L);
		if (doorUID != -1)
			Util.messagePlayer(player, getFullDoorInfo(plugin.getCommander().getDoor(doorUID)));
		else
		{
			ArrayList<Door> doors = plugin.getCommander().getDoors(player.getUniqueId().toString(), name);
			for (Door door : doors)
				Util.messagePlayer(player, getFullDoorInfo(door));
		}
	}
	
	public void listDoorInfo(Player player, Door door)
	{
		Util.messagePlayer(player, getFullDoorInfo(door));
	}
	
	public void listDoorInfoFromConsole(String str)
	{
		long doorUID = Util.longFromString(str, -1L);
		if (doorUID == -1)
		{
			plugin.getMyLogger().myLogger(Level.INFO, "Door not found! Please remember to use doorUIDs! "
					              + "Find them using /ListDoors <DoorName || PlayerName || PlayerUUID>");
			return;
		}
		Door door = plugin.getCommander().getDoor(doorUID);
		if (door == null)
			return;
		plugin.getMyLogger().myLogger(Level.INFO, nameFromUUID(door.getPlayerUUID()) + ": " + getFullDoorInfo(door));
	}
	
	private void listDoorsFromConsole(String playerUID, String name)
	{
		ArrayList<Door> doors = plugin.getCommander().getDoors(playerUID, name);
		for (Door door : doors)
		{
			String playerName = nameFromUUID(door.getPlayerUUID());
			plugin.getMyLogger().myLogger(Level.INFO, 
					(playerName == null ? "" : playerName + ": ") + getBasicDoorInfo(door));
		}
	}
	
	public void listDoorsFromConsole(String str)
	{
		String playerUUID = playerUUIDFromString(str);
		if (playerUUID != null)
			listDoorsFromConsole(playerUUID, null);
		else
			listDoorsFromConsole(null, str);
	}
	
	private String nameFromUUID(UUID playerUUID)
	{
		if (playerUUID == null)
			return null;
		String output = null;
		Player player = Bukkit.getPlayer(playerUUID);
		if (player != null)
			output = player.getName();
		else
			output = Bukkit.getOfflinePlayer(playerUUID).getName();
		return output;
	}
	
	private String playerUUIDFromString(String input)
	{
		Player player = null;
		player = Bukkit.getPlayer(input);
		if (player == null)
			try { player = Bukkit.getPlayer(UUID.fromString(input)); }
			// Not doing anything with catch because I really couldn't care less if it didn't work.
			catch (Exception e)	{}
		if (player != null)
			return player.getUniqueId().toString();

		OfflinePlayer offPlayer = null;
		try { offPlayer = Bukkit.getOfflinePlayer(UUID.fromString(input)); }
		// Not doing anything with catch because I really couldn't care less if it didn't work.
		catch (Exception e)	{}
		if (offPlayer != null)
			return offPlayer.getUniqueId().toString();
		return null;
	}
	
	private String getBasicDoorInfo(Door door)
	{
		return door.getDoorUID() + " (" + door.getPermission() + ")" + ": " + door.getName().toString();
	}
	
	private String getFullDoorInfo(Door door)
	{
		
		return 	door == null ? "Door not found!" :
				door.getDoorUID() + ": " + door.getName().toString() + 
				", Min("    + door.getMinimum().getBlockX() + ";"    + door.getMinimum().getBlockY() + ";" + door.getMinimum().getBlockZ() + ")" +
				", Max("    + door.getMaximum().getBlockX() + ";"    + door.getMaximum().getBlockY() + ";" + door.getMaximum().getBlockZ() + ")" +
				", Engine(" + door.getEngine().getBlockX()  + ";"    + door.getEngine().getBlockY()  + ";" + door.getEngine().getBlockZ()  + ")" +
				", " + (door.isLocked() ? "" : "NOT ") + "locked"    + "; Type=" + door.getType()    + 
				(door.getEngSide() == null ? "" : ("; EngineSide = " + door.getEngSide().toString()  + "; doorLen = " +
				 door.getLength())) + ", PowerBlockPos = (" + door.getPowerBlockLoc().getBlockX()    + ";" + 
				 door.getPowerBlockLoc().getBlockY() + ";"  + door.getPowerBlockLoc().getBlockZ()    + ")"; 
	}

	public boolean isValidName(String name)
	{
		try
		{
			Integer.parseInt(name);
			return false;
		}
		catch(NumberFormatException e)
		{
			return true;
		}
	}
	
	// Create a new door.
	public void makePortcullis(Player player, String name)
	{
		if (name != null && !isValidName(name))
		{
			Util.messagePlayer(player, ChatColor.RED, "Name \"" + name + "\" is not valid!");
			return;
		}
		
		PortcullisCreator pcc = new PortcullisCreator(plugin, player, name);
		plugin.getPCCreators().add(pcc);
		
		int tickrate     = 4;
		new BukkitRunnable()
		{
			int count    = 0;
			int seconds  = 60;
			int totTicks = 20 / tickrate * seconds;
			
			@Override
			public void run()
			{
				if (pcc != null && pcc.isDone())	// Cancel and cleanup when the door creation process is doen.
				{
					pcc.finishUp();
					plugin.getPCCreators().remove(pcc);
					this.cancel();
				}
				else if (count > totTicks)
				{
					pcc.takeToolFromPlayer();
					plugin.getPCCreators().remove(pcc);
					plugin.getMyLogger().returnToSender((CommandSender) player, Level.INFO, ChatColor.RED, Messages.getString("DC.TimeUp"));
					this.cancel();
				}
				++count;
			}
		}.runTaskTimer(plugin, 0, 20); // Once a second.
	}
	
	// Create a new door.
	public void makeDoor(Player player, String name)
	{
		if (name != null && !isValidName(name))
		{
			Util.messagePlayer(player, ChatColor.RED, "Name \"" + name + "\" is not valid!");
			return;
		}
		
		DoorCreator dc = new DoorCreator(plugin, player, name);
		plugin.getDoorCreators().add(dc);

		int tickrate     = 4;
		new BukkitRunnable()
		{
			int count    = 0;
			int seconds  = 60;
			int totTicks = 20 / tickrate * seconds;
			
			@Override
			public void run()
			{
				if (dc != null && dc.isDone())	// Cancel and cleanup when the door creation process is doen.
				{
					dc.finishUp();
					plugin.getDoorCreators().remove(dc);
					this.cancel();
				}
				else if (count > totTicks)
				{
					dc.takeToolFromPlayer();
					plugin.getDoorCreators().remove(dc);
					plugin.getMyLogger().returnToSender((CommandSender) player, Level.INFO, ChatColor.RED, Messages.getString("DC.TimeUp"));
					this.cancel();
				}
				++count;
			}
		}.runTaskTimer(plugin, 0, 20); // Once a second.
	}
	
	// Check if a provided player is a doorCreator.
	public DoorCreator isCreatingDoor(Player player)
	{
		for (DoorCreator dc : plugin.getDoorCreators())
			if (dc.getPlayer() == player)
				return dc;
		return null;
	}
	
	// Check if a provided player is a doorCreator.
	public PortcullisCreator isCreatingPortcullis(Player player)
	{
		for (PortcullisCreator pcc : plugin.getPCCreators())
			if (pcc.getPlayer() == player)
				return pcc;
		return null;
	}
	
	public void relocatePowerBlock(Player player, long doorUID)
	{
		PowerBlockRelocator pbr = new PowerBlockRelocator(plugin, player, doorUID);
		
		plugin.getRelocators().add(pbr);
		
		int tickrate     = 4;
		new BukkitRunnable()
		{
			int count    = 0;
			int seconds  = 20;
			int totTicks = 20 / tickrate * seconds;
			
			@Override
			public void run()
			{
				if (pbr != null && pbr.isDone())	// Cancel and cleanup when the door creation process is doen.
				{
					pbr.finishUp();
					plugin.getRelocators().remove(pbr);
					this.cancel();
				}
				else if (count > totTicks) // Cancel after 20 seconds.
				{
					pbr.takeToolFromPlayer();
					plugin.getRelocators().remove(pbr);
					plugin.getMyLogger().returnToSender((CommandSender) player, Level.INFO, ChatColor.RED, Messages.getString("DC.TimeUp"));
					this.cancel();
				}
				++count;
			}
		}.runTaskTimer(plugin, 0, tickrate); // Once a second.
	}

	// Check if a provided player is a PowerBlockRelocator.
	public PowerBlockRelocator isRelocatingPB(Player player)
	{
		for (PowerBlockRelocator pbr : plugin.getRelocators())
			if (pbr.getPlayer() == player)
				return pbr;
		return null;
	}
	
	// Handle commands.
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args)
	{
		Player player;
		
		// /stopdoors
		if (cmd.getName().equalsIgnoreCase("stopdoors"))
		{
			stopDoors();
			return true;
		}
		
		// /doordebug
		if (cmd.getName().equalsIgnoreCase("doordebug"))
		{
			doorDebug();
			return true;
		}
		
		// /pausedoors
		if (cmd.getName().equalsIgnoreCase("pausedoors"))
		{
			plugin.getCommander().togglePaused();
			return true;
		}
		
		// /bdversion
		if (cmd.getName().equalsIgnoreCase("bdversion"))
		{
			plugin.getMyLogger().returnToSender(sender, Level.INFO, ChatColor.GREEN, "This server uses version " + 
											plugin.getDescription().getVersion() + " of this plugin!");
			return true;
		}
		
		// /opendoors <doorName1> <doorName2> etc etc [time]
		if (cmd.getName().equalsIgnoreCase("opendoors"))
		{
			if (args.length >= 2)
			{
				if (sender instanceof Player)
					player = (Player) sender;
				else
					player = null;
				
				String lastStr = args[args.length - 1];
				// Last argument sets speed if it's a double.
				double time = Util.longFromString(lastStr, -1L) == -1L ? Util.doubleFromString(lastStr, 0.0D) : 0.0D;
				int endIDX = args.length;
				if (time != 0.0D)
					--endIDX;
				
				for (int index = 0; index < endIDX; ++index)
				{
					Door door = plugin.getCommander().getDoor(args[index], player);
					if (door == null && index != args.length - 1)
						plugin.getMyLogger().returnToSender(sender, Level.INFO, ChatColor.RED, "\"" + args[index] + "\" is not a valid door name!");
					else if (door != null)
						openDoorCommand(sender, door, time);
				}
				return true;
			}
		}

		// /opendoor <doorName>
		if (cmd.getName().equalsIgnoreCase("opendoor"))
		{
			if (args.length >= 1)
			{
				if (sender instanceof Player)
					player = (Player) sender;
				else
					player = null;
				
				Door door = plugin.getCommander().getDoor(args[0], player);
				// TODO: Add this shit to en_US.txt translation file.
				if (door == null)
				{
					if (player != null)
						plugin.getMyLogger().returnToSender(sender, Level.INFO, ChatColor.RED, "\"" + args[0] + "\" is not a valid door name!");
					else
						plugin.getMyLogger().returnToSender(sender, Level.INFO, ChatColor.RED, "\"" + args[0] + "\" is not a valid door ID!");
				}
				else
				{
					double time = args.length == 2 ? Util.doubleFromString(args[1], 0.0D) : 0.0;
					openDoorCommand(sender, door, time);
				}
				return true;
			}
		}

		// /listdoors [name]
		// /listdoors <doorName || playerName>
		if (cmd.getName().equalsIgnoreCase("listdoors"))
		{
			if (sender instanceof Player)
			{
				player = (Player) sender;
				if (args.length == 0)
					listDoors(player, null);
				else if (args.length == 1)
					listDoors(player, args[0]);
			}
			else
			{
				if (args.length == 0)
					return false;
				else if (args.length == 1)
					listDoorsFromConsole(args[0]);
			}

			return true;
		}
		
		// /doorinfo <doorName>
		// /doorinfo <doorUID>
		if (cmd.getName().equalsIgnoreCase("doorinfo"))
		{
			if (args.length == 1)
			{
				if (sender instanceof Player)
				{
					player = (Player) sender;
					listDoorInfo(player, args[0]);
				}
				else
				{
					listDoorInfoFromConsole(args[0]);
				}
				return true;
			}
		}
		
		if (sender instanceof Player)
		{
			player = (Player) sender;
			
			// /bigdoorsenableas
			if (cmd.getName().equalsIgnoreCase("bigdoorsenableas"))
			{
				plugin.bigDoorsEnableAS();
				return true;
			}
			
			// /bigdoors
			if (cmd.getName().equalsIgnoreCase("bigdoors") || cmd.getName().equalsIgnoreCase("bdm"))
			{
				new GUIPage(plugin, player);
				return true;
			}
			
			// /namedoor
			if (cmd.getName().equalsIgnoreCase("namedoor"))
			{
				DoorCreator dc = isCreatingDoor(player);
				if (dc != null)
					if (args.length == 1)
						if (isValidName(args[0]))
						{
							dc.setName(args[0]);
							return true;
						}
				PortcullisCreator pcc = isCreatingPortcullis(player);
				if (pcc != null)
					if (args.length == 1)
						if (isValidName(args[0]))
						{
							pcc.setName(args[0]);
							return true;
						}
			}
			
			// /changePowerBlockLoc
			if (cmd.getName().equalsIgnoreCase("changePowerBlockLoc"))
			{
				if (args.length < 1)
					return false;
				Door door = plugin.getCommander().getDoor(args[0], player);
				if (door == null)
					return true;
				long doorUID = door.getDoorUID();
				this.relocatePowerBlock(player, doorUID);
				return true;
			}
			
			// /bdcancel
			if (cmd.getName().equalsIgnoreCase("bdcancel"))
			{
				DoorCreator dc = isCreatingDoor(player);
				if (dc != null)
				{
					dc.setIsDone(true);
					plugin.getMyLogger().returnToSender((CommandSender) player, Level.INFO, ChatColor.RED, Messages.getString("DC.Cancelled"));
				}
				PortcullisCreator pc = isCreatingPortcullis(player);
				if (pc != null)
				{
					pc.setIsDone(true);
					plugin.getMyLogger().returnToSender((CommandSender) player, Level.INFO, ChatColor.RED, Messages.getString("DC.Cancelled"));
				}
				PowerBlockRelocator pbc = isRelocatingPB(player);
				if (pbc != null)
				{
					pbc.setIsDone(true);
					plugin.getMyLogger().returnToSender((CommandSender) player, Level.INFO, ChatColor.RED, Messages.getString("DC.Cancelled"));
				}
				return true;
			}
			
			// /unlockdoor <doorName>
			if (cmd.getName().equalsIgnoreCase("unlockdoor"))
				if (args.length == 1)
				{
					Door door = plugin.getCommander().getDoor(args[0], player);
					if (	door != null)
					{
						plugin.getCommander().setDoorAvailable(door.getDoorUID());
						plugin.getMyLogger().returnToSender(sender, Level.INFO, ChatColor.GREEN, "Door unlocked!");
						return true;
					}
				}
			
			// deldoor <doorName>
			if (cmd.getName().equalsIgnoreCase("deldoor"))
				if (args.length == 1)
				{
					delDoor(player, args[0]);
					return true;
				}
			
			// /newdoor <doorName>
			if (cmd.getName().equalsIgnoreCase("newdoor"))
				if (args.length >= 1)
				{
					if (args[0].equals("-p") && args.length == 2)
						makePortcullis(player, args[1]);
					else
						makeDoor(player, args[0]);
					return true;
				}
				
			// /newportcullis <doorName>
			if (cmd.getName().equalsIgnoreCase("newportcullis"))
				if (args.length == 1)
				{
					makePortcullis(player, args[0]);
					return true;
				}
				
			// /fixdoor
			if (cmd.getName().equalsIgnoreCase("fixdoor"))
				if (args.length >= 1)
				{
					Door door = plugin.getCommander().getDoor(args[0], player);
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
			Util.swap(door, 0);
		if (yMin > yMax)
			Util.swap(door, 1);
		if (zMin > zMax)
			Util.swap(door, 2);
	}
	
	public void delDoor(Player player, long doorUID)
	{
		plugin.getCommander().removeDoor(doorUID);
	}
	
	public void delDoor(Player player, String doorName)
	{
		CommandSender sender = (CommandSender) player;
		try
		{
			long doorUID     = Long.parseLong(doorName);
			plugin.getCommander().removeDoor(doorUID);
			plugin.getMyLogger().returnToSender(sender, Level.INFO, ChatColor.GREEN, "Door deleted!");
			return;
		}
		catch(NumberFormatException e)
		{
			// Logging this makes no sense.
//			plugin.getMyLogger().logMessage("Could not parse to long!", true, false);
		}
		
		long doorCount = countDoors(player, doorName);
		if (doorCount == 0)
			plugin.getMyLogger().returnToSender(sender, Level.INFO, ChatColor.RED, "No door found by that name!");
		else if (doorCount == 1)
		{
			plugin.getCommander().removeDoor(player.getUniqueId().toString(), doorName);
			plugin.getMyLogger().returnToSender(sender, Level.INFO, ChatColor.GREEN, "Door deleted!");
		}
		else
		{
			plugin.getMyLogger().returnToSender(sender, Level.INFO, ChatColor.RED, "More than one door found with that name! Please use their ID instead:");
			listDoors(player, doorName);
		}
	}
	
	// Used for various debugging purposes (you don't say!).
	public void doorDebug()
	{
		Bukkit.broadcastMessage("Doors can" + (plugin.getCommander().canGo() ? " " : "not ") + "go!");
	}
}
