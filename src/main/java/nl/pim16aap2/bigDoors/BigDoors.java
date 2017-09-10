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
import nl.pim16aap2.bigDoors.moveBlocks.BlockMover;
 
public class BigDoors extends JavaPlugin implements Listener 
{
	private WorldEditPlugin worldEdit;
	private String[] allowedEngineMats = {"IRON_FENCE"};
	private String[] allowedDoorMats   = {"GOLD_BLOCK"};
	private List<Door> doors;
	private BlockMover blockMover;
	
	@Override
    public void onEnable() 
	{
		doors = new ArrayList<Door>();
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
        if(!dataFolder.exists())
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
            if(!dataFolder.exists())
            {
                dataFolder.mkdir();
            }
            File saveTo = new File(getDataFolder(), "doors.txt");
            if (!saveTo.exists())
            {
                saveTo.createNewFile();
            } else {
            	saveTo.delete();
                saveTo.createNewFile();
            }
            FileWriter fw = new FileWriter(saveTo, true);
            PrintWriter pw = new PrintWriter(fw);
            for (Door door : doors) 
            {
            	pw.println(door.toString());
            }
            pw.flush();
            pw.close();
        } catch (IOException e)
        {
        	Bukkit.getLogger().log(Level.SEVERE, "Could not save file!");
            e.printStackTrace();
 
        }
	}
	
	// Handle commands.
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) 
    {
    	Player player;
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

	    	// /deldoor <doorName>
	    	if (cmd.getName().equalsIgnoreCase("deldoor")) 
	    	{
	    		if (args.length == 1) 
	    		{
	    			deleteDoor(getDoor(args[0]));
	    			return true;
	    		}
	    	}
	    	
	    	// /opendoor <doorName>
	    	if (cmd.getName().equalsIgnoreCase("opendoor")) 
	    	{
	    		if (args.length == 1) 
	    		{	
	    			
	    			return true;
	    		}
	    	}
	    	
	    	// /listdoors
	    	if (cmd.getName().equalsIgnoreCase("listdoors")) 
	    	{
	    		listDoors(player);
	    		return true;
	    	}
	    	return false;
    	} else 
    	{
    		Bukkit.getLogger().log(Level.INFO, "This command can not be used from the console.");
    	}
    	return false;
    }
    
    // Check if a block is a valid engine block.
    public boolean isValidEngineBlock(Block block) 
    {
		for (String s : allowedEngineMats) 
		{
			if (block.getType().toString() == s) 
			{
				return true;
			}
    	}
    	return false;
    }
    
    // Check if the selection contains a valid engine.
    public boolean hasValidEngine(World w, int xPos, int zPos, int yMin, int yMax) 
    {	
    	for (int index = yMin; index <= yMax; index++)
    	{
//    		Bukkit.broadcastMessage("Engine: Checking block at:"+xPos+", "+index+", "+zPos+", which is: "+w.getBlockAt(xPos, index, zPos).getType().toString());
    		if (!isValidEngineBlock(w.getBlockAt(xPos, index, zPos)))
    		{
    			Bukkit.broadcastMessage("Invalid Engine: Block at "+xPos+", "+index+", "+zPos+" is: "+w.getBlockAt(xPos, index, zPos).getType().toString());
    			return false;
    		}
    	}
    	Bukkit.broadcastMessage("The door has a valid engine!");
    	return true;
    }
    
    // Check if a block is a valid door block.
    public boolean isValidDoorBlock(Block block) 
    {
		for (String s : allowedDoorMats) 
		{
			if (block.getType().toString() == s) 
			{
				return true;
			}
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
//    	    		Bukkit.broadcastMessage("Checking block at:"+xAxis+", "+yAxis+", "+zAxis+", which is: "+w.getBlockAt(xAxis, yAxis, zAxis).getType().toString());
    	    		if (!isValidDoorBlock(w.getBlockAt(xAxis, yAxis, zAxis)))
    	    		{
    	    			Bukkit.broadcastMessage("Invalid Door: Block at "+xAxis+", "+yAxis+", "+zAxis+" is: "+w.getBlockAt(xAxis, yAxis, zAxis).getType().toString());
    	    			return false;
    	    		}
    	    	}
        	}
    	}
    	return true;
    }
    
    
    // Check if the selection contains a valid door and return the location of the engine.
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
    		if (selection.getLength()==1) 
    		{	
    			// First check the side with the lowest X value.
    			if (hasValidEngine(world, xMin, zMax, yMin, yMax))
    			{   
    				loc = new Location(world, xMin, yMin, zMin);
    				// Check if the blocks (excluding the engine) are valid door blocks.
    				if (hasValidDoorBlocks(world, xMin, xMax, zMin+1, zMax, yMin, yMax))
    				{
        				return loc;
    				}
    			// Then check the side with the highest X value.
    			} else if (hasValidEngine(world, xMax, zMax, yMin, yMax))
    			{
    				loc = new Location(world, xMax, yMin, zMin);
    				// Check if the blocks (excluding the engine) are valid door blocks.
    				if (hasValidDoorBlocks(world, xMin, xMax, zMin, zMax-1, yMin, yMax))
    				{
        				return loc;
    				}
    			}
    		// If the selection is only 1 deep in the x-direction...
    		} else if (selection.getWidth()==1) 
    		{
    			// First check the side with the lowest Z value.
    			if (hasValidEngine(world, xMax, zMin, yMin, yMax))
    			{   
    				loc = new Location(world, xMax, yMin, zMin);
    				// Check if the blocks (excluding the engine) are valid door blocks.
    				if (hasValidDoorBlocks(world, xMin, xMax, zMin+1, zMax, yMin, yMax))
    				{
        				return loc;
    				}
    			// Then check the side with the highest Z value.
    			} else if (hasValidEngine(world, xMax, zMax, yMin, yMax))
    			{
    				loc = new Location(world, xMax, yMin, zMax);
    				// Check if the blocks (excluding the engine) are valid door blocks.
    				if (hasValidDoorBlocks(world, xMin, xMax, zMin, zMax-1, yMin, yMax))
    				{
        				return loc;
    				}
    			}
    		}
    	}
    	return loc;
    }
    
    // Print a list of the doors currently in the db.
    public void listDoors(Player player) 
    {
    	int count=0;
    	for (Door door : doors) 
    	{
    		player.sendMessage(count+": "+door.getName()+":\nMinimumCoords:"+door.getMinimum().getBlockX()+","+door.getMinimum().getBlockY()+","+door.getMinimum().getBlockZ()+", MaximumCoords:"+door.getMaximum().getBlockX()+","+door.getMaximum().getBlockY()+","+door.getMaximum().getBlockZ());
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
    	{
    		if (door.getName().equals(name))
    		{
    			return door;
    		}
    	}
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
    	{
    		if (name.equals(door.getName()))
    		{
    			return false;
    		}
    	}
    	return true;
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
				Door newDoor = new Door(world, xMin, yMin, zMin, xMax, yMax, zMax, engineLoc.getBlockX(), engineLoc.getBlockY(), engineLoc.getBlockZ(), name, false);
	    		addDoor(newDoor);
    		} else 
    		{
    			player.sendMessage(ChatColor.RED+"Name \""+name+"\" already in use!");
    		}
    	    Bukkit.broadcastMessage("This is a valid selection!");
    	    
    	} else 
    	{
    		Bukkit.broadcastMessage("This is not a valid selection!");
    	}
    }
}