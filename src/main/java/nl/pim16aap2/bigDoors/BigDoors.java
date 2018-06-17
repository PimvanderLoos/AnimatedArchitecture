package nl.pim16aap2.bigDoors;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Vector;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import nl.pim16aap2.bigDoors.handlers.CommandHandler;
import nl.pim16aap2.bigDoors.handlers.EventHandlers;
import nl.pim16aap2.bigDoors.handlers.GUIHandler;
import nl.pim16aap2.bigDoors.moveBlocks.DoorOpener;
import nl.pim16aap2.bigDoors.storage.sqlite.SQLiteJDBCDriverConnection;

// TODO: Create commandlistener that can be used to wait for command trees. Create interface and extend queued up commands from there.

public class BigDoors extends JavaPlugin implements Listener
{
	private SQLiteJDBCDriverConnection db;
	private Vector<DoorCreator>      dcal;
	private MyLogger               logger;
	private File                  logFile;
	private Commander           commander;
	private DoorOpener         doorOpener;
	private CommandHandler commandHandler;

	@Override
	public void onEnable()
	{
		dcal       = new Vector<DoorCreator>(2);
		logFile    = new File(getDataFolder(), "log.txt");
		logger     = new MyLogger(this, logFile);
		this.db    = new SQLiteJDBCDriverConnection(this, "doorDB");
		doorOpener = new DoorOpener(this);
		
		commandHandler = new CommandHandler(this);
		commander      = new Commander(this, db);
		Bukkit.getPluginManager().registerEvents(new EventHandlers(this), this);
		Bukkit.getPluginManager().registerEvents(new GUIHandler   (this), this);
		getCommand("unlockDoor").setExecutor(new CommandHandler(this));
		getCommand("pausedoors").setExecutor(new CommandHandler(this));
		getCommand("opendoors" ).setExecutor(new CommandHandler(this));
		getCommand("listdoors" ).setExecutor(new CommandHandler(this));
		getCommand("stopdoors" ).setExecutor(new CommandHandler(this));
		getCommand("doorinfo"  ).setExecutor(new CommandHandler(this));
		getCommand("opendoor"  ).setExecutor(new CommandHandler(this));
		getCommand("nameDoor"  ).setExecutor(new CommandHandler(this));
		getCommand("bigdoors"  ).setExecutor(new CommandHandler(this));
		getCommand("newdoor"   ).setExecutor(new CommandHandler(this));
		getCommand("deldoor"   ).setExecutor(new CommandHandler(this));
		getCommand("fixdoor"   ).setExecutor(new CommandHandler(this));
		getCommand("shutup"    ).setExecutor(new CommandHandler(this));
		getCommand("bdm"       ).setExecutor(new CommandHandler(this));
		
//		readDoors(); // Import doors from .txt file. Only needed for debugging! I'm the only one with the file!
	}
	
	// Read the saved list of doors, if it exists. ONLY for debugging purposes. Should be removed from the final export!
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
				
				Door door = new Door(world, xMin, yMin, zMin, xMax, yMax, zMax, engineX, engineY, engineZ, name, isOpen, -1, false, -1, "3ba21c6f-6ad9-4310-9f82-f85f5602deef");
				commander.addDoor(door);
				
				// Add the door that was just read to the list.
//				addDoor(new Door(world, xMin, yMin, zMin, xMax, yMax, zMax, engineX, engineY, engineZ, name, isOpen));
				sCurrentLine = br.readLine();
			}
			br.close();

		} 
		catch (FileNotFoundException e)
		{
			Bukkit.getLogger().log(Level.INFO, "No save file found. No doors loaded!");
		} 
		catch (IOException e)
		{
			Bukkit.getLogger().log(Level.WARNING, "Could not read file!!");
			e.printStackTrace();
		}
	}

	@Override
	public void onDisable()
	{} // Nothing to do here for now.
	
	// Get the Vector of doorcreators (= users creating a door right now).
	public Vector<DoorCreator> getDoorCreators()
	{
		return this.dcal;
	}
	
	public DoorOpener getDoorOpener()
	{
		return this.doorOpener;
	}
	
	// Get the command Handler.
	public CommandHandler getCommandHandler()
	{
		return this.commandHandler;
	}
	
	// Get the commander (class executing commands).
	public Commander getCommander()
	{
		return this.commander;
	}
	
	// Get the logger.
	public MyLogger getMyLogger()
	{
		return this.logger;
	}
}