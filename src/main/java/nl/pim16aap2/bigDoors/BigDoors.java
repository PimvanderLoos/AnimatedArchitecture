package nl.pim16aap2.bigDoors;

import java.io.File;
import java.util.ArrayList;

import org.bukkit.Bukkit;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import nl.pim16aap2.bigDoors.handlers.CommandHandler;
import nl.pim16aap2.bigDoors.handlers.EventHandlers;
import nl.pim16aap2.bigDoors.moveBlocks.DoorOpener;
import nl.pim16aap2.bigDoors.storage.sqlite.SQLiteJDBCDriverConnection;

// TODO: Create commandlistener that cna be used to wait for command trees. Create interface and extend queued up commands from there.

public class BigDoors extends JavaPlugin implements Listener
{
	private DoorOpener doorOpener;
	private ArrayList<DoorCreator> dcal;
	private MyLogger logger;
	private SQLiteJDBCDriverConnection db;
	private File logFile;
	private CommandHandler commandHandler;
	private Commander      commander;

	@Override
	public void onEnable()
	{
		dcal       = new ArrayList<DoorCreator>();
		logFile    = new File(getDataFolder(), "log.txt");
		logger     = new MyLogger(this, logFile);
		logger.logMessage("Startup...", false, true);
		this.db    = new SQLiteJDBCDriverConnection(this, "doorDB");
		doorOpener = new DoorOpener(this);
		
		commandHandler = new CommandHandler(this);
		commander      = new Commander(this, db);
		Bukkit.getPluginManager().registerEvents(new EventHandlers(this), this);
		getCommand("shutup").setExecutor(new CommandHandler(this));
		getCommand("pausedoors").setExecutor(new CommandHandler(this));
		getCommand("stopdoors").setExecutor(new CommandHandler(this));
		getCommand("newdoor").setExecutor(new CommandHandler(this));
		getCommand("deldoor").setExecutor(new CommandHandler(this));
		getCommand("opendoor").setExecutor(new CommandHandler(this));
		getCommand("opendoors").setExecutor(new CommandHandler(this));
		getCommand("listdoors").setExecutor(new CommandHandler(this));
		getCommand("fixdoor").setExecutor(new CommandHandler(this));
		getCommand("bigdoors").setExecutor(new CommandHandler(this));
		getCommand("bdm").setExecutor(new CommandHandler(this));
		getCommand("nameDoor").setExecutor(new CommandHandler(this));
		getCommand("unlockDoor").setExecutor(new CommandHandler(this));
		getCommand("doorinfo").setExecutor(new CommandHandler(this));
	}

	@Override
	public void onDisable()
	{} // Nothing to do here for now.
	
	// Get the ArrayList of doorcreators (= users creating a door right now).
	public ArrayList<DoorCreator> getDoorCreators()
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