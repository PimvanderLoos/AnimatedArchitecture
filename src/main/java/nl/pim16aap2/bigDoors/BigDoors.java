package nl.pim16aap2.bigDoors;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import net.md_5.bungee.api.ChatColor;
import nl.pim16aap2.bigDoors.handlers.CommandHandler;
import nl.pim16aap2.bigDoors.handlers.EventHandlers;
import nl.pim16aap2.bigDoors.moveBlocks.DoorOpener;
import nl.pim16aap2.bigDoors.storage.sqlite.SQLiteJDBCDriverConnection;
import nl.pim16aap2.bigDoors.util.Util;

public class BigDoors extends JavaPlugin implements Listener
{
	private DoorOpener doorOpener;
	private List<DoorCreator> dcal; 
	private int debugLevel = 100;
	private boolean goOn   = true;
	private boolean paused = false;
	private SQLiteJDBCDriverConnection db;
	private File logFile;

	@Override
	public void onEnable()
	{
		logFile    = new File(getDataFolder(), "log.txt");
		logMessage("Startup...", false, true);
		this.db    = new SQLiteJDBCDriverConnection(this, "doorDB");
		doorOpener = new DoorOpener(this);
		Bukkit.getPluginManager().registerEvents(new EventHandlers(this), this);
		loadLog();
		getCommand("shutup").setExecutor(new CommandHandler(this));
		getCommand("pausedoors").setExecutor(new CommandHandler(this));
		getCommand("stopdoors").setExecutor(new CommandHandler(this));
		getCommand("newdoor").setExecutor(new CommandHandler(this));
		getCommand("deldoor").setExecutor(new CommandHandler(this));
		getCommand("opendoor").setExecutor(new CommandHandler(this));
		getCommand("opendoors").setExecutor(new CommandHandler(this));
		getCommand("listdoors").setExecutor(new CommandHandler(this));
		getCommand("fixdoor").setExecutor(new CommandHandler(this));
//		getCommand("updatedoor").setExecutor(new CommandHandler(this));
		getCommand("unlockDoor").setExecutor(new CommandHandler(this));
		
		dcal = new ArrayList<DoorCreator>();
	}

	@Override
	public void onDisable()
	{} // Nothing to do here for now.
	
	public List<DoorCreator> getDoorCreators()
	{
		return this.dcal;
	}
	
	public DoorOpener getDoorOpener()
	{
		return this.doorOpener;
	}
	
	public void loadLog()
	{
		if (!logFile.exists())
		{
			try
			{
				logFile.createNewFile();
				getLogger().log(Level.INFO, "New file created at " + logFile);
			}
			catch (IOException e)
			{
				getLogger().log(Level.SEVERE, "File write error: " + logFile);
			}
		}
	}
	
	public boolean isPaused()
	{
		return this.paused;
	}
	
	public void togglePaused()
	{
		this.paused = !this.paused;
	}
	
	public void setDebugLevel(int level)
	{
		this.debugLevel = level;
	}
	
	public boolean canGo()
	{
		return this.goOn;
	}
	
	public void setCanGo(boolean bool)
	{
		this.goOn = bool;
	}
	
	public SQLiteJDBCDriverConnection getRDatabase() 
	{
        return this.db;
    }
	
	// Print a string to the log.
	public void myLogger(Level level, String str)
	{
		Bukkit.getLogger().log(level, "[" + this.getName() + "] " + str);
	}
	
	// Send a message to whomever issued a command.
	public void returnToSender(CommandSender sender, Level level, ChatColor color, String str)
	{
		if (sender instanceof Player)
			Util.messagePlayer((Player) sender, color + str);
		else
			myLogger(level, str);
	}
	
	// Swap min and max values for type mode (0/1/2 -> X/Y/Z) for a specified door.
	public void swap(Door door, int mode)
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
	
	// Print an ArrayList of doors to a player.
	public void printDoors(Player player, List<Door> doors)
	{
		for (Door door : doors)
			Util.messagePlayer(player, door.getDoorUID() + ": " + door.getName().toString());
	}

	// Get the door from the string. Can be use with a doorUID or a doorName.
	public Door getDoor(String doorStr, Player player)
	{
		// First try converting the doorStr to a doorUID.
		try
		{
			int doorUID = Integer.parseInt(doorStr);
			
			Door door = getRDatabase().getDoor(doorUID);
			
			Bukkit.broadcastMessage("" + door.toString());
			
			return getRDatabase().getDoor(doorUID);
		}
		// If it can't convert to an int, get all doors from the player with the provided name. 
		// If there is more than one, tell the player that they are going to have to make a choice.
		catch (NumberFormatException e)
		{
			List<Door> doors = new ArrayList<Door>();
			doors = getRDatabase().getDoors(player.getUniqueId().toString(), doorStr);
			if (doors.size() == 1)
				return doors.get(0);
			else 
			{
				Util.messagePlayer(player, "More than 1 door with that name found! Please use its ID instead!");
				printDoors(player, doors);
				return null;
			}
		}
	}

	// Add a door to the list of doors.
	public void addDoor(Door newDoor)
	{
		System.out.println("Adding door " + newDoor.getName().toString());
		getRDatabase().insert(newDoor);
	}

	// Check if a given name is already in use or not.
	public boolean isNameAvailable(String name, Player player)
	{
		return getRDatabase().isNameAvailable(name, player.getUniqueId().toString());
	}
	
//	// Update the coords of the specified door.
//	public void updateDoor(Player player, Door door)
//	{
//		Selection selection = worldEdit.getSelection(player);
//		if (selection != null)
//		{
//			int xMin = selection.getMinimumPoint().getBlockX();
//			int xMax = selection.getMaximumPoint().getBlockX();
//			int yMin = selection.getMinimumPoint().getBlockY();
//			int yMax = selection.getMaximumPoint().getBlockY();
//			int zMin = selection.getMinimumPoint().getBlockZ();
//			int zMax = selection.getMaximumPoint().getBlockZ();
//			World world = selection.getWorld();
//
//			Location newMax = new Location (world, xMax, yMax, zMax);
//			Location newMin = new Location (world, xMin, yMin, zMin);
//			
//			door.setMaximum(newMax);
//			door.setMinimum(newMin);
//			
//			Util.messagePlayer(player, "Door coordinates updated successfully!");
//
//		} 
//		else
//			debugMsg(Level.WARNING, "This is not a valid selection!");
//	}
	
	public void logMessage(String msg, boolean printToConsole, boolean startSkip)
	{
		if (printToConsole)
			System.out.println(msg);
		BufferedWriter bw = null;
		try
		{
			bw = new BufferedWriter(new FileWriter(logFile, true));
			Date now = new Date();
			SimpleDateFormat format = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
			if (startSkip)
				bw.write("\n\n[" + format.format(now) + "] " + msg);
			else
				bw.write("[" + format.format(now) + "] " + msg);
			bw.newLine();
			bw.flush();
		}
		catch (IOException e)
		{
			getLogger().log(Level.SEVERE, "Logging error! Could not log to logFile!");	
		}
	}
	
	public void logMessage(String msg)
	{
		logMessage(msg, false, false);
	}
	
	public void debugMsg(int level, Level lvl, String msg)
	{
		if (level <= debugLevel)
			Bukkit.broadcastMessage(""+msg);
	}
	
	public void debugMsg(Level lvl, String msg)
	{
		debugMsg(0, lvl, msg);
	}
}