package nl.pim16aap2.bigDoors;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import com.sk89q.worldedit.bukkit.*;
import com.sk89q.worldedit.bukkit.selections.*;

import net.md_5.bungee.api.ChatColor;
import nl.pim16aap2.bigDoors.moveBlocks.DoorOpener;

/* TODO: Merge NoClipArmorStand and falling sand, cutting entities being used in half.
 * TODO: Clean up this class.
 * 
 */

public class BigDoors extends JavaPlugin implements Listener
{
	private WorldEditPlugin worldEdit;
	private String[] allowedEngineMats = { "IRON_FENCE" };
	private String[] allowedDoorMats   = { "GOLD_BLOCK" };
	private List<Door> doors;
	private DoorOpener doorOpener;
	private int debugLevel = 100;

	@Override
	public void onEnable()
	{
		doors = new ArrayList<Door>();
		doorOpener = new DoorOpener(this);
		Bukkit.getPluginManager().registerEvents(new EventHandlers(this), this);
		worldEdit = (WorldEditPlugin) Bukkit.getServer().getPluginManager().getPlugin("WorldEdit");
		readDoors();
	}

	@Override
	public void onDisable()
	{
		saveDoors();
	}

	// Read the saved list of doors, if it exists.
	public void readDoors()
	{
		File dataFolder = getDataFolder();
		if (!dataFolder.exists())
		{
			Bukkit.getLogger().log(Level.INFO, "No save file found. No doors loaded!");
			return;
		}
		File readFrom = new File(getDataFolder(), "doors.txt");
		try (BufferedReader br = new BufferedReader(new FileReader(readFrom)))
		{
			String sCurrentLine;
			sCurrentLine = br.readLine();

			while (sCurrentLine != null)
			{
				int xMin, yMin, zMin, xMax, yMax, zMax;
				int engineX, engineY, engineZ;
				String name;
				boolean isOpen;
				World world;

				String[] strs = sCurrentLine.trim().split("\\s+");

				name    = strs[0];
				isOpen  = Boolean.getBoolean(strs[1]);
				world   = Bukkit.getServer().getWorld(strs[2]);
				xMin    = Integer.parseInt(strs[3]);
				yMin    = Integer.parseInt(strs[4]);
				zMin    = Integer.parseInt(strs[5]);
				xMax    = Integer.parseInt(strs[6]);
				yMax    = Integer.parseInt(strs[7]);
				zMax    = Integer.parseInt(strs[8]);
				engineX = Integer.parseInt(strs[9]);
				engineY = Integer.parseInt(strs[10]);
				engineZ = Integer.parseInt(strs[11]);

				// Add the door that was just read to the list.
				addDoor(new Door(world, xMin, yMin, zMin, xMax, yMax, zMax, engineX, engineY, engineZ, name, isOpen));
				sCurrentLine = br.readLine();
			}
			br.close();

		} catch (FileNotFoundException e)
		{
			Bukkit.getLogger().log(Level.INFO, "No save file found. No doors loaded!");
		} catch (IOException e)
		{
			Bukkit.getLogger().log(Level.WARNING, "Could not read file!!");
			e.printStackTrace();
		}
	}

	// Save the list of doors.
	public void saveDoors()
	{
		try
		{
			File dataFolder = getDataFolder();
			if (!dataFolder.exists())
				dataFolder.mkdir();
			File saveTo = new File(getDataFolder(), "doors.txt");
			if (!saveTo.exists())
				saveTo.createNewFile();
			else
			{
				saveTo.delete();
				saveTo.createNewFile();
			}
			FileWriter fw = new FileWriter(saveTo, true);
			PrintWriter pw = new PrintWriter(fw);
			for (Door door : doors)
				pw.println(door.toString());
			pw.flush();
			pw.close();
		} catch (IOException e)
		{
			Bukkit.getLogger().log(Level.SEVERE, "Could not save file!");
			e.printStackTrace();
		}
	}
	
	// Send a message to a player in a specific color.
	public void messagePlayer(Player player, ChatColor color, String s)
	{
		player.sendMessage(color + s);
	}
	
	// Send a message to a player.
	public void messagePlayer(Player player, String s)
	{
		messagePlayer(player, ChatColor.WHITE, s);
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
			messagePlayer((Player) sender, color + str);
		else
			myLogger(level, str);
	}
	
	public void openDoorCommand(CommandSender sender, Door door, double speed)
	{
		Bukkit.broadcastMessage("speed = " + speed);
		if (!doorOpener.openDoor(door, speed))
			returnToSender(sender, Level.INFO, ChatColor.RED, "This door cannot be opened! Check if one side of the \"engine\" blocks is unobstructed!");
	}

	// Handle commands.
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args)
	{
		Player player;
		
		// /shutup
		if (cmd.getName().equalsIgnoreCase("shutup"))
		{
			if (args.length == 1)
			{
				try
				{
					this.debugLevel = Integer.parseInt(args[0]);
				}
				catch (NumberFormatException e)
				{
					returnToSender(sender, Level.INFO, ChatColor.RED, "expected numerical input!");
				}
				return true;
			}
		}
			
		// /opendoors <doorName1> <doorName2>
		if (cmd.getName().equalsIgnoreCase("opendoors"))
		{
			if (args.length >= 2)
			{
				// If the last argument is not a door (so getDoor returns null), it should be the speed. If it it null, use default speed.
				double speed = getDoor(args[args.length - 1]) == null ? Double.parseDouble(args[args.length - 1]) : 0.2;
				for (int index = 0; index < args.length; ++index)
				{
					Door door = getDoor(args[index]);
					if (door == null && index != args.length - 1)
						returnToSender(sender, Level.INFO, ChatColor.RED, "\"" + args[index] + "\" is not a valid door name!");
					else if (door != null)
						openDoorCommand(sender, door, speed);
				}
				return true;
			}
		}

		if (sender instanceof Player)
		{
			player = (Player) sender;
			// /newdoor <doorName>
			if (cmd.getName().equalsIgnoreCase("newdoor"))
			{
				if (args.length == 1)
				{
					makeDoor(player, args[0]);
					return true;
				}
			}

			// /updatedoor <doorName>
			if (cmd.getName().equalsIgnoreCase("updatedoor"))
			{
				if (args.length == 1)
				{
					Door door = getDoor(args[0]);
					if (door != null)
						updateDoor(player, door);
					else
						messagePlayer(player, ChatColor.RED, "Not a valid door name!");
					return true;
				}
			}

			// /opendoor <doorName>
			if (cmd.getName().equalsIgnoreCase("opendoor"))
			{
				if (args.length >= 1)
				{
					Door door = getDoor(args[0]);
					if (door != null)
					{
						double speed = args.length == 2 ? (args[1] == null ? 0.2 : Double.parseDouble(args[1])) : 0.2;
						openDoorCommand(sender, door, speed);
					} 
					else
						messagePlayer(player, ChatColor.RED, "Not a valid door name!");
					return true;
				}
			}

			// /listdoors
			if (cmd.getName().equalsIgnoreCase("listdoors"))
			{
				listDoors(player);
				return true;
			}
			
			// /fixdoor
			if (cmd.getName().equalsIgnoreCase("fixdoor"))
			{
				if (args.length >= 1)
				{
					Door door = getDoor(args[0]);
					if (door != null)
						verifyDoorCoords(door);
					else
						messagePlayer(player, ChatColor.RED, "Not a valid door name!");
					return true;
				}
			}
		}
		return false;
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
			swap(door, 0);
		if (yMin > yMax)
			swap(door, 1);
		if (zMin > zMax)
			swap(door, 2);
	}

	// Check if a block is a valid engine block.
	public boolean isValidEngineBlock(Block block)
	{
		for (String s : allowedEngineMats)
			if (block.getType().toString() == s)
				return true;
		return false;
	}

	// Check if the selection contains a valid engine.
	public boolean hasValidEngine(World w, int xPos, int zPos, int yMin, int yMax)
	{
		for (int index = yMin; index <= yMax; index++)
		{
			if (!isValidEngineBlock(w.getBlockAt(xPos, index, zPos)))
			{
				debugMsg(Level.WARNING, "Invalid Engine: Block at " + xPos + ", " + index + ", " + zPos + " is: " + w.getBlockAt(xPos, index, zPos).getType().toString());
				return false;
			}
		}
		debugMsg(Level.WARNING, "The door has a valid engine!");
		return true;
	}

	// Check if a block is a valid door block.
	public boolean isValidDoorBlock(Block block)
	{
		for (String s : allowedDoorMats)
		{
			if (block.getType().toString() == s)
				return true;
		}
		return false;
	}

	// Check if the selection contains valid door blocks.
	public boolean hasValidDoorBlocks(World w, int xMin, int xMax, int zMin, int zMax, int yMin, int yMax)
	{
		for (int xAxis = xMin; xAxis <= xMax; xAxis++)
		{
			for (int yAxis = yMin; yAxis <= yMax; yAxis++)
			{
				for (int zAxis = zMin; zAxis <= zMax; zAxis++)
				{
					if (!isValidDoorBlock(w.getBlockAt(xAxis, yAxis, zAxis)))
					{
						debugMsg(Level.WARNING, "Invalid Door: Block at " + xAxis + ", " + yAxis + ", " + zAxis + " is: " + w.getBlockAt(xAxis, yAxis, zAxis).getType().toString());
						return false;
					}
				}
			}
		}
		return true;
	}

	// Check if the selection contains a valid door and return the location of the
	// engine.
	public Location verifySelection(Player player, Selection selection)
	{
		Location loc = null;
		if (selection != null)
		{
			int xMin = selection.getMinimumPoint().getBlockX();
			int xMax = selection.getMaximumPoint().getBlockX();
			int yMin = selection.getMinimumPoint().getBlockY();
			int yMax = selection.getMaximumPoint().getBlockY();
			int zMin = selection.getMinimumPoint().getBlockZ();
			int zMax = selection.getMaximumPoint().getBlockZ();
			World world = selection.getWorld();

			// If the selection is only 1 deep in the z-direction...
			if (selection.getLength() == 1)
			{
				// First check the side with the lowest X value.
				if (hasValidEngine(world, xMin, zMax, yMin, yMax))
				{
					loc = new Location(world, xMin, yMin, zMin);
					// Check if the blocks (excluding the engine) are valid door blocks.
					if (hasValidDoorBlocks(world, xMin, xMax, zMin + 1, zMax, yMin, yMax))
						return loc;
					// Then check the side with the highest X value.
				} 
				else if (hasValidEngine(world, xMax, zMax, yMin, yMax))
				{
					loc = new Location(world, xMax, yMin, zMin);
					// Check if the blocks (excluding the engine) are valid door blocks.
					if (hasValidDoorBlocks(world, xMin, xMax, zMin, zMax - 1, yMin, yMax))
						return loc;
				}
				// If the selection is only 1 deep in the x-direction...
			} 
			else if (selection.getWidth() == 1)
			{
				// First check the side with the lowest Z value.
				if (hasValidEngine(world, xMax, zMin, yMin, yMax))
				{
					loc = new Location(world, xMax, yMin, zMin);
					// Check if the blocks (excluding the engine) are valid door blocks.
					if (hasValidDoorBlocks(world, xMin, xMax, zMin + 1, zMax, yMin, yMax))
						return loc;
					// Then check the side with the highest Z value.
				} 
				else if (hasValidEngine(world, xMax, zMax, yMin, yMax))
				{
					loc = new Location(world, xMax, yMin, zMax);
					// Check if the blocks (excluding the engine) are valid door blocks.
					if (hasValidDoorBlocks(world, xMin, xMax, zMin, zMax - 1, yMin, yMax))
						return loc;
				}
			}
		}
		return loc;
	}

	// Print a list of the doors currently in the db.
	public void listDoors(Player player)
	{
		int count = 0;
		for (Door door : doors)
		{
			messagePlayer(player, count + ": " + door.getName() + ":\nMinimumCoords:" + door.getMinimum().getBlockX() + ","
					+ door.getMinimum().getBlockY() + "," + door.getMinimum().getBlockZ() + ", MaximumCoords:"
					+ door.getMaximum().getBlockX() + "," + door.getMaximum().getBlockY() + ","
					+ door.getMaximum().getBlockZ());
			count++;
		}
	}

	// Delete a door from the list of doors.
	public void deleteDoor(Door oldDoor)
	{
		doors.remove(oldDoor);
	}

	// Get the door named "name".
	public Door getDoor(String name)
	{
		for (Door door : doors)
			if (door.getName().equals(name))
				return door;
		return null;
	}

	// Add a door to the list of doors.
	public void addDoor(Door newDoor)
	{
		doors.add(newDoor);
	}

	// Check if a given name is already in use or not.
	public boolean isNameAvailable(String name)
	{
		for (Door door : doors)
			if (name.equals(door.getName()))
				return false;
		return true;
	}
	
	// Update the coords of the specified door.
	public void updateDoor(Player player, Door door)
	{
		Selection selection = worldEdit.getSelection(player);
		if (selection != null)
		{
			int xMin = selection.getMinimumPoint().getBlockX();
			int xMax = selection.getMaximumPoint().getBlockX();
			int yMin = selection.getMinimumPoint().getBlockY();
			int yMax = selection.getMaximumPoint().getBlockY();
			int zMin = selection.getMinimumPoint().getBlockZ();
			int zMax = selection.getMaximumPoint().getBlockZ();
			World world = selection.getWorld();

			Location newMax = new Location (world, xMax, yMax, zMax);
			Location newMin = new Location (world, xMin, yMin, zMin);
			
			door.setMaximum(newMax);
			door.setMinimum(newMin);
			
			messagePlayer(player, "Door coordinates updated successfully!");

		} 
		else
			debugMsg(Level.WARNING, "This is not a valid selection!");
	}

	// Create a new door.
	public void makeDoor(Player player, String name)
	{
		Selection selection = worldEdit.getSelection(player);
		Location engineLoc = verifySelection(player, selection);
		if (engineLoc != null)
		{
			int xMin = selection.getMinimumPoint().getBlockX();
			int xMax = selection.getMaximumPoint().getBlockX();
			int yMin = selection.getMinimumPoint().getBlockY();
			int yMax = selection.getMaximumPoint().getBlockY();
			int zMin = selection.getMinimumPoint().getBlockZ();
			int zMax = selection.getMaximumPoint().getBlockZ();
			World world = selection.getWorld();
			if (isNameAvailable(name))
			{
				Door newDoor = new Door(world, xMin, yMin, zMin, xMax, yMax, zMax, engineLoc.getBlockX(),
						engineLoc.getBlockY(), engineLoc.getBlockZ(), name, false);
				addDoor(newDoor);
			} 
			else
				player.sendMessage(ChatColor.RED + "Name \"" + name + "\" already in use!");
			debugMsg(Level.WARNING, "This is a valid selection!");

		} 
		else
			debugMsg(Level.WARNING, "This is not a valid selection!");
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