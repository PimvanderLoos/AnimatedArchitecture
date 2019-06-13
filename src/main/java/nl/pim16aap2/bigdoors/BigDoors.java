package nl.pim16aap2.bigdoors;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.Vector;

import javax.annotation.Nullable;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import nl.pim16aap2.bigdoors.commands.CommandBigDoors;
import nl.pim16aap2.bigdoors.commands.CommandData;
import nl.pim16aap2.bigdoors.commands.CommandMenu;
import nl.pim16aap2.bigdoors.commands.ICommand;
import nl.pim16aap2.bigdoors.commands.SuperCommand;
import nl.pim16aap2.bigdoors.commands.subcommands.SubCommandAddOwner;
import nl.pim16aap2.bigdoors.commands.subcommands.SubCommandCancel;
import nl.pim16aap2.bigdoors.commands.subcommands.SubCommandChangePowerBlock;
import nl.pim16aap2.bigdoors.commands.subcommands.SubCommandClose;
import nl.pim16aap2.bigdoors.commands.subcommands.SubCommandDebug;
import nl.pim16aap2.bigdoors.commands.subcommands.SubCommandDelete;
import nl.pim16aap2.bigdoors.commands.subcommands.SubCommandFill;
import nl.pim16aap2.bigdoors.commands.subcommands.SubCommandInfo;
import nl.pim16aap2.bigdoors.commands.subcommands.SubCommandInspectPowerBlock;
import nl.pim16aap2.bigdoors.commands.subcommands.SubCommandListDoors;
import nl.pim16aap2.bigdoors.commands.subcommands.SubCommandListPlayerDoors;
import nl.pim16aap2.bigdoors.commands.subcommands.SubCommandMenu;
import nl.pim16aap2.bigdoors.commands.subcommands.SubCommandNew;
import nl.pim16aap2.bigdoors.commands.subcommands.SubCommandOpen;
import nl.pim16aap2.bigdoors.commands.subcommands.SubCommandPause;
import nl.pim16aap2.bigdoors.commands.subcommands.SubCommandRemoveOwner;
import nl.pim16aap2.bigdoors.commands.subcommands.SubCommandRestart;
import nl.pim16aap2.bigdoors.commands.subcommands.SubCommandSetAutoCloseTime;
import nl.pim16aap2.bigdoors.commands.subcommands.SubCommandSetBlocksToMove;
import nl.pim16aap2.bigdoors.commands.subcommands.SubCommandSetName;
import nl.pim16aap2.bigdoors.commands.subcommands.SubCommandSetRotation;
import nl.pim16aap2.bigdoors.commands.subcommands.SubCommandStopDoors;
import nl.pim16aap2.bigdoors.commands.subcommands.SubCommandToggle;
import nl.pim16aap2.bigdoors.commands.subcommands.SubCommandVersion;
import nl.pim16aap2.bigdoors.compatiblity.ProtectionCompatManager;
import nl.pim16aap2.bigdoors.doors.DoorBase;
import nl.pim16aap2.bigdoors.doors.DoorType;
import nl.pim16aap2.bigdoors.gui.GUI;
import nl.pim16aap2.bigdoors.handlers.ChunkUnloadHandler;
import nl.pim16aap2.bigdoors.handlers.EventHandlers;
import nl.pim16aap2.bigdoors.handlers.GUIHandler;
import nl.pim16aap2.bigdoors.handlers.LoginMessageHandler;
import nl.pim16aap2.bigdoors.handlers.LoginResourcePackHandler;
import nl.pim16aap2.bigdoors.handlers.RedstoneHandler;
import nl.pim16aap2.bigdoors.managers.AutoCloseScheduler;
import nl.pim16aap2.bigdoors.managers.CommandManager;
import nl.pim16aap2.bigdoors.managers.DatabaseManager;
import nl.pim16aap2.bigdoors.managers.HeadManagerv2;
import nl.pim16aap2.bigdoors.managers.VaultManager;
import nl.pim16aap2.bigdoors.moveblocks.BlockMover;
import nl.pim16aap2.bigdoors.moveblocks.BridgeOpener;
import nl.pim16aap2.bigdoors.moveblocks.DoorOpener;
import nl.pim16aap2.bigdoors.moveblocks.ElevatorOpener;
import nl.pim16aap2.bigdoors.moveblocks.FlagOpener;
import nl.pim16aap2.bigdoors.moveblocks.GarageDoorOpener;
import nl.pim16aap2.bigdoors.moveblocks.Opener;
import nl.pim16aap2.bigdoors.moveblocks.PortcullisOpener;
import nl.pim16aap2.bigdoors.moveblocks.RevolvingDoorOpener;
import nl.pim16aap2.bigdoors.moveblocks.SlidingDoorOpener;
import nl.pim16aap2.bigdoors.moveblocks.WindmillOpener;
import nl.pim16aap2.bigdoors.nms.FallingBlockFactory_Vall;
import nl.pim16aap2.bigdoors.nms.v1_14_R1.FallingBlockFactory_V1_14_R1;
import nl.pim16aap2.bigdoors.nms.v1_14_R1.SkullCreator_V1_14_R1;
import nl.pim16aap2.bigdoors.storage.sqlite.SQLiteJDBCDriverConnection;
import nl.pim16aap2.bigdoors.toolusers.ToolUser;
import nl.pim16aap2.bigdoors.toolusers.ToolVerifier;
import nl.pim16aap2.bigdoors.util.ConfigLoader;
import nl.pim16aap2.bigdoors.util.DoorOpenResult;
import nl.pim16aap2.bigdoors.util.Messages;
import nl.pim16aap2.bigdoors.util.Metrics;
import nl.pim16aap2.bigdoors.util.Restartable;
import nl.pim16aap2.bigdoors.util.TimedCache;
import nl.pim16aap2.bigdoors.util.Util;
import nl.pim16aap2.bigdoors.waitforcommand.WaitForCommand;

/*
 * Modules
 */
// TODO: Split up project POM into modules. Use API to get all implementation (Bukkit, Forge, different versions) specific stuff.
//       https://bukkit.org/threads/support-multiple-minecraft-versions-with-abstraction-maven.115810/

/*
 * Experimental
 */
// TODO: Use custom events for door opening. Perhaps allow other plugins (keys?) to hook into those plugins.
// TODO: Look into allowing people to set a max size in RAM for certain caches.
//       Example: https://www.javaworld.com/article/2074458/estimating-java-object-sizes-with-instrumentation.html
//       Simpler example: https://stackoverflow.com/questions/52353/in-java-what-is-the-best-way-to-determine-the-size-of-an-object#
//       Another: https://javamagic.blog/2018/07/11/how-to-find-size-of-java-object-in-memory-using-jol/
//       Also: https://www.baeldung.com/java-size-of-object
// TODO: Fix violation of Liskov Substituion Problem in certain subclasses of Door (i.e. Revovlving Door, as it has no lookingDir).
//       Though, thinking about it, DoorBase doesn't really need this function anyway, if all opener code is put in the doorClass anyway.
// TODO: Fix violation of LSP for doorAttributes. Instead of having a switch per type in the GUI, override a return in the DoorAttribute enum.
// TODO: NEVER use something like this: private final HashMap<Object>; Instead, use this: private final Map<Object>; So it doesn't
//       violate LSP. Same goes for lists and what-not.
// TODO: Data is duplicated a lot! Currently, the falling block has the IBlockData, and so does MyBlockData. If the block can rotate, it has it twice, even!
//       This is a huge waste of Ram. The falling block should be the only place to store the block data.
// TODO: Instead of killing the FBlock, rotating it and then respawning it, rotate the FBlockData and then send the appropriate packets to the appropriate players.
// TODO: Async logging: https://www.javaworld.com/article/2077526/java-tip-29--how-to-decouple-the-observer-observable-object-model.html
//       Use this: LinkedBlockingQueue
// TODO: Async database operations, perhaps in combination with an event queue or something. So queue a door open operation and execute it once the door has been retrieved.
// TODO: Add command to upload error log to pastebin or something similar.
// TODO: Look into FutureTask.
// TODO: Add config option to limit logging file size.
// TODO: Properly use PlotSquared's API.
// TODO: Look into previously-blacklisted material. Torches, for example, work just fine in 1.13. They just need to be removed first and placed last.
// TODO: Replace the enum of DoorTypes by a static list. Types should then statically register themselves.
// TODO: Figure out a way to use Interfaces or something to generate 2 separate builds: Premium and non-premium.
// TODO: Redo all messages. Create enum to store them, to prevent typos and to use for string replacing.
//       Then get a replace function to insert variables. Example: getMessage(Message.PAGENUM, new String(currentPage), new String(nextPage)).
//       Store the variables that will be replaced in the enum or something. Also, get some software or unit test to make sure the number of arguments matches.
//       More info about asserts: https://stackoverflow.com/questions/998553/how-to-assert-something-at-compile-time-in-java
//       Also look at this: https://stackoverflow.com/questions/36538133/can-i-use-java-annotations-to-define-compile-time-checks
//       And this: https://stackoverflow.com/questions/42644170/compile-time-validation-of-method-arguments
//       And this: https://www.logicbig.com/tutorials/core-java-tutorial/java-se-annotation-processing-api/annotation-processor-validation.html
//       Also, remove nameKeys from DoorTypes enum.
//       In the new ENUM, perhaps have a method getMessage(MessageEnum msg, Object ...). Every message should be something like
//       NextPage("GUI.nextPage", {Integer.class, Integer.class}); Then, verify using an interface that the passed objects
//       are of the correct type and count. (in this case, the result should be messages.getMessage(Message.NextPage, currentPage, nextPage);
// TODO: Look into Aikar's command system to replace everything I just made myself: https://www.spigotmc.org/threads/acf-beta-annotation-command-framework.234266/
// TODO: Add 1 block depth requirement to assertValidCoords() in HorizontalAxisAlignedBase.
// TODO: Check if Util::needsRefresh(Material mat) is still needed.
// TODO: Door pooling. When a door is requested from the database, store it in a timedCache. Only get the creator by default, but store
//       the other owners if needed. Add a Door::Sync function to sync (new) data with the database.
// TODO: Make sure that all writing to the log file is done from the correct thread!
// TODO: Get rid of the DoorType enum. Instead, allow dynamic registration of door types.
// TODO: Stop naming all animatable objects "doors". An elevator is hardly a door.

/*
 * General
 */
// TODO: Use Optional where applicable: https://docs.oracle.com/javase/8/docs/api/java/util/Optional.html
// TODO: Put @Nullable everywhere where applicable.
//       More info about which to use: https://stackoverflow.com/questions/4963300/which-notnull-java-annotation-should-i-use
// TODO: Put all listeners and command stuff (basically everything users can interact with) in try-catch blocks, so I can
//       Dump more errors into the log file.
// TODO: Add command to upload log file to PasteBin or something.
// TODO: Catch specific exceptions in update checker. Or at least ssl exception, it's very spammy.
// TODO: Implement TPS limit. Below a certain TPS, doors cannot be opened.
//       double tps = ((CraftServer) Bukkit.getServer()).getServer().recentTps[0]; // 3 values: last 1, 5, 15 mins.
// TODO: Add javadoc (@ param) stuff etc to "api" and replace any method comment by jdoc stuff.
// TODO: Update version checker. Start using the new format. Also, decide what the new format will be. R200? 200R? %100 = Stable?
// TODO: Rename RotateDirection to moveDirection. Lifts don't rotate. They lift.
// TODO: Update rotatable blocks after finishing rotation etc.
// TODO: When a block is "blocking" a door from being opened, check if there isn't a corresponding gap in the door to be opened.
// TODO: ConfigLoader should figure out which resource pack version to use on its own.
// TODO: Move all non-database related stuff out of DatabaseManager.
// TODO: OPs and people with bigdoors.admin.bypass.attribute should bypass permissions check for allowing options.
// TODO: Rename region bypass permission to bigdoors.admin.bypass.region.
// TODO: CustomEntityFallingBlock: Clean this damn class up!
// TODO: Portcullis info prints it'll open going North when looking east. That's not right.
//       Same issue for regular doors.
// TODO: blocksToMove isn't printed by door full info.
// TODO: Look into Unit testing: https://bukkit.org/threads/how-to-unit-test-your-plugin-with-example-project.23569/
// TODO: Don't use TypeString for DoorCreator, but use DoorType codeName instead. Also, the entire format is pretty stupid. Lots of repetition in the language file for every type.
// TODO: Look into restartables interface. Perhaps it's a good idea to split restart() into stop() and init().
//       This way, it can call all init()s in BigDoors::onEnable and all stop()s in BigDoors::onDisable.
// TODO: Every door type needs its own class. This is getting too messy. A lot of the Opener stuff can be moved to those specialized classes. Might not even need openers anymore.
//       Wouldn't that be nice? This also means that doors cannot be created in the Creator superClass. Instead, it'll need an abstract finishUp() method.
// TODO: Allow position etc validation code to be used on existing doors one way or another.
// TODO: Creators: updateEngineLoc from DoorCreator() and setEngine() should be the same and declared as abstract method in Creator.
// TODO: GarageDoorCreator: Should extend DrawBridgeCreator.
// TODO: Store MyBlockFace in rotateDirection so I don't have to cast it via strings. ewww.
// TODO: Make sure ALL players stored in hashMaps are cleaned up when they leave to avoid a memory leak!!
// TODO: Make sure you cannot use direct commands (i.e. /setPowerBlockLoc 12) of doors not owned by the one using the command.
// TODO: When returning null after unexpected input, just fucking thrown an IllegalArgumentException. Will make debugging a lot easier.
// TODO: Use Functional interface for the ProtectionCompatManager. Too much code duplication.
// TODO: Look into exceptions a bit more. "MyStackTrace" seems rather dumb and superfluous.
// TODO: Improve calculateChunkRange() methods. Drawbridge, for example, could be much smaller.
// TODO: Get rid of all occurrences of "boolean onDisable". Just do it via the main class.
// TODO: When truncating exceptions etc, make sure to write it down in the log.
// TODO: Make sure adding a new door properly invalidates the chunk cache. Same for moving a power block.

/*
 * GUI
 */
// TODO: Look into playerheads for GUI buttons. Example: https://minecraft-heads.com/player-heads/alphabet/2762-arrow-left
// TODO: Make GUI options always use the correct subCommand.
// TODO: Create ItemManager that stores repeatedly used items (such as door creation books and doorInfo stuff).
// TODO: Store 2 player objects: 1) Subject (the owner of all the doors), and 2) InventoryHolder (who is looking at the inventory).
// TODO: Cannot toggle openDirection for portcullis type. Might be related to the fact that it says that the portcullis openDirection is North instead of Up/Down.
// TODO: Use ButtonAction GUI and GUIItem::specialValue to phase out raw interactionIDX stuff for getting actions.
// TODO: Update items in inventory instead of opening a completely new inventory. No longer requires dirty code to check is it's refreshing etc. Bweugh.
// TODO: Make sure all player head construction is handled on a secondary thread. Only getting from hashMap should be done on the main thread.
// TODO: Once door pooling is implemented, use Observers to update doors in the GUI when needed.
// TODO: Move rotation cycling away from GUI and into the Door class.
// TODO: Put all GUI buttons and whatnot in try/catch blocks.

/*
 * SQL
 */
// TODO: Split up SQL functions into 2: One taking a connection and one without the connection, to get rid of code duplication.
// TODO: Use nested statements.
// TODO: Use preparedStatements for everything (with values(?,?,?) etc).
// TODO: See if inserting into doors works when adding another question mark for the UUID (but leaving it empty).
//       Then +1 won't have to be appended to everything.
// TODO: Use proper COUNT operation for getting the number of doors.
// TODO: Merge isOpen and isLocked into single FLAG value.
// TODO: Switch RotateDirection values to line up with MyBlockFace.
// TODO: Remove all NONE RotateDirection values from the database.

/*
 * Commands
 */
// TODO: Add /BDM [PlayerName (when online) || PlayerUUID || Server] to open a doorMenu for a specific player
// TODO: Make invalid input stuff more informative (e.g. int, float etc).
// TODO: Improve recovering from invalid input. When people use a float instead of an int, cast to int.
// TODO: Allow adding owners to doors from console.
// TODO: When the plugin fails to initialize properly, register alternative command handler to display this info.
// TODO: Move stuff such as StartTimerForAbortable into appropriate command classes.
// TODO: When retrieving player argument (SubCommandAddOwner, SubCommandListPlayerDoors) don't just try to convert
//       the provided playerArg to a UUID. That's not very user friendly at all.
// TODO: Check if force unlock door as admin still exists.
// TODO: Do not use the commander for anything command-related that isn't strictly database abstraction.
// TODO: Store actual minArgCount in subCommands so it doesn't have to be calculated constantly.
// TODO: Make sure there are no commands that use hard coded argument positions.
// TODO: SetblocksToMove commandWaiter doesn't work. Possibly issue with minArgCount.
// TODO: NPE thrown when trying to use direct command after initiating a commandWaiter a while ago (should've been cancelled already!).
//       Might be related to the issue listed above (regarding setBlocksToMove commandWaiter).
// TODO: Explain why there are 2 hashMaps storing seemingly the same data in the CommandManager.
// TODO: Make sure super command can be chained.
// TODO: Fix bigdoors doorinfo in console.
// TODO: Cancelling command can result in NPE. possible after timeout.
// TODO: SetBlocksToMove: Waiter is cancelled both by the subCommand and the waiter. Make sure all commandWaiters are disabled in the waiter.
// TODO: CommandWaiters should register themselves in the CommandManager class. No outside class should even know about this stuff. Especially not
//       the fucking DatabaseManager.
// TODO: Make more liberal use of IllegalArgumentException.
// TODO: Put all command related stuff in try/catch blocks.
// TODO: SubCommandSetRotation should be updated/removed, as it doesn't work properly with types that do not go (counter)clockwise.

/*
 * Openers / Movers
 */
// TODO: When a door isn't set to open in a specific direction and therefore naively tries to find the first possible
//       free location, automatically set the openDirection for this door. HOWEVER, this value must be reset when it is
//       closed again, but ONLY if it used to be unset. So instead of storing value, add flag for un/intentionally set.
// TODO: Make the getOpenDirection function of the openers static, so the Creators can check which direction to pick. Then set the direction
//       in the creator.
// TODO: Remove NONE openDirection. Replace all of those in the database by the correct one.
// TODO: Rotate Sea Pickle and turtle egg.
// TODO: Replace current time/speed/tickRate system. It's a mess.
// TODO: Get rid of all material related stuff in these classes. isAllowedBlock should be abstracted away. Should be a method of fabf.
// TODO: Consider using HashSet for blocks. It's faster: https://stackoverflow.com/questions/10196343/hash-set-and-array-list-performances
// TODO: Do second pass (possibly remove first pass) after placing all blocks to make sure that all connected blocks are actually connected.
//       Currently, connected blocks will only be connected to blocks that have already been processed.
// TODO: Test and finish flag type.
// TODO: Rewrite parts of the drawBridge opener and mover. The upDown etc stuff should not be used.
// TODO: ElevatorOpener should extend PortcullisOpener.
// TODO: ElevatorOpener and PortcullisOpener should respect setOpenDirection and min/max world height (0, 256).
// TODO: Remove getNewLocation() method from Movers. Instead, they should ALL use a GNL. GNLs should not just get the x,y,z values, but the entire block and blocksMoved. Then
//       they can figure it out for themselves.
// TODO: Make some kind of interface TravelingDoor, that includes the updateCoords and getNewLocation methods. Then movers that don't actually move the object (flag, windmill)
//       Don't need to include those methods.
// TODO: Move rotation/respawning block code out of runnables. Perhaps even into BLockMover. Same goes for termination conditions.
// TODO: Windmill: Remove magic values in endCount and Stap variables in WindmillMover::animateEntities();
// TODO: Windmill: Allow perpetual movement (or at least while players are nearby AND chunks are loaded).
// TODO: Windmill: Allow setting rotational speed (seconds per rotation).
// TODO: Drawbridge: Learn from WindmillMover and simplify the class. Also applies to CylindricalMover.
// TODO: Clamp angles to [-2PI ; 2PI].
// TODO: Either use time or ticks. Not both.
// TODO: Use the GetNewLocation code to check new pos etc.
// TODO: Make sure the new types don't just open instantly without a provided time parameter    !!!!!!!!
// TODO: Rename variables in updateCoords to avoid confusion. Perhaps a complete removal altogether would be nice as well.
// TODO: Get rid of the GNL interface etc. The movers class can handle it on its own using Function interface.
// TODO: Move getBlocksMoved() to Mover.
// TODO: GarageDoorCreator: Fix having to double click last block.
// TODO: GarageDoorCreator: Before defaulting to North/East, check if those directions are actually available.
// TODO: Allow blocks with inventories to be moved.
// TODO: GarageDoor: Use EngineSide to figure out what the current direction is.
// TODO: Do not update engine position when opening doors that actually change position: Portcullis, Elevator, Sliding door, etc.
//       This way, the original position is still stored after opening it.
// TODO: Do not allow setting of invalid rotation directions. If a garage door is positioned along the z axis, only North and South are valid options.
// TODO: Instead of creating and running the runnables in the animateEntities method, create the runnable earlier and store it. Then call animateEntities()
//       from BlockMover. Then let BlockMover extend Restartable and/or abortable, so that the it can cancel all movers etc on restart, so this code doesn't have to be part
//       of the runnable anymore. Much cleaner.
// TODO: Drawbridge: Do not use Clockwise / CounterClockwise. Use cardinal directions instead (as seen from up-position).
// TODO: BigDoor: Do not use Clockwise / CounterClockwise here either. This one should also use cardinal directions.
// TODO: Make setDefaultOpenDirection() smarter by checking which side is actually available.
// TODO: Movers: updateCoords should be final and use the DoorBase::getNewLocations method to get the new min/max.
// TODO: GarageDoor: The pivot point offset (where it starts pivoting), should depend on the radius. The higher the radius of the block compared
//       to the total radius, the smaller the offset should be. This way, the final blocks will line up better with the final position.
//       radius = maxRadius -> offset = 0. Should probably only look at the last 3 blocks. e.g.: offset = Min((offset / 4) * (totalRadius - radius)).
// TODO: Drawbridge: Cleanup #getNewLocation().
// TODO: When checking if a door's chunks are loaded, use the door's chunkRange variables.




/*
 * Manual Testing
 */
// TODO: Test new creators: Windmill, RevolvingDoor, GarageDoor. Make sure it cannot be fucked up.
// TODO: Test new chunkInRange methods. Especially sliding door.
// TODO: Make sure that new lines in the messages work (check Util::stringFromArray).
// TODO: Fix no permission to set AutoCloseTime from GUI.

/*
 * Unit tests
 */
// TODO: Test that Creators and Openers of all enabled types can be properly retrieved (e.g. in BigDoors::getDoorOpener(DoorType type);
//       And that they are properly initialized.


public class BigDoors extends JavaPlugin implements Listener
{
    public static final boolean DEVBUILD = true;
    // Minimum number of ticks a door needs to cool down before it can be
    // toggled again. A slight delay prevents issues with doors being toggled
    // before they're fully ready (data race?).
    private static final int MINIMUMDOORDELAY = 10;

    private ToolVerifier tf;
    private SQLiteJDBCDriverConnection db;
    private FallingBlockFactory_Vall fabf;
    private ConfigLoader config;
    private MyLogger logger;
    private SpigotUpdater updater;
    private Metrics metrics;
    private File logFile;
    private Messages messages;
    private DatabaseManager databaseManager = null;
    private Vector<WaitForCommand> cmdWaiters;
    private Vector<BlockMover> blockMovers;

    private DoorOpener doorOpener;
    private BridgeOpener bridgeOpener;
    private SlidingDoorOpener slidingDoorOpener;
    private PortcullisOpener portcullisOpener;
    private RedstoneHandler redstoneHandler;
    private ElevatorOpener elevatorOpener;
    private WindmillOpener windmillOpener;
    private FlagOpener flagOpener;
    private RevolvingDoorOpener revolvingDoorOpener;
    private GarageDoorOpener garageDoorOpener;

    private boolean validVersion;
    private String loginString;
    private CommandManager commandManager;
    private HashMap<UUID, ToolUser> toolUsers;
    private HashMap<UUID, GUI> playerGUIs;
    private List<Restartable> restartables = new ArrayList<>();
    private boolean is1_13 = false;
    private ProtectionCompatManager protCompatMan;
    private LoginResourcePackHandler rPackHandler;
    private TimedCache<Long /*Chunk*/, HashMap<Long /*Loc*/, Long /*doorUID*/>> pbCache = null;
    private HeadManager headManager;
    private VaultManager vaultManager;
    private AutoCloseScheduler autoCloseScheduler;

    private HeadManagerv2 headManagerv2;

    @Override
    public void onEnable()
    {
        logFile = new File(getDataFolder(), "log.txt");
        logger = new MyLogger(this, logFile);

        try
        {
            Bukkit.getPluginManager().registerEvents(new LoginMessageHandler(this), this);
            if (DEVBUILD)
                setLoginString("[BigDoors] Warning: You are running a devbuild! Auto-Updater has been disabled!");

            validVersion = compatibleMCVer();
            // Load the files for the correct version of Minecraft.
            if (!validVersion)
            {
                logger.severe("Trying to load the plugin on an incompatible version of Minecraft! (\""  +
                    (Bukkit.getServer().getClass().getPackage().getName().replace(".",  ",").split(",")[3]) +
                                  "\"). This plugin will NOT be enabled!");
                return;
            }


            init();
            headManager.init();
            vaultManager = new VaultManager(this);
            autoCloseScheduler = new AutoCloseScheduler(this);


            headManagerv2 = new HeadManagerv2(this);


            Bukkit.getPluginManager().registerEvents(new EventHandlers(this), this);
            Bukkit.getPluginManager().registerEvents(new GUIHandler(this), this);
            Bukkit.getPluginManager().registerEvents(new ChunkUnloadHandler(this), this);
            // No need to put these in init, as they should not be reloaded.
            pbCache = new TimedCache<>(this, config.cacheTimeout());
            protCompatMan = new ProtectionCompatManager(this);
            Bukkit.getPluginManager().registerEvents(protCompatMan, this);
            db = new SQLiteJDBCDriverConnection(this, config.dbFile());
            databaseManager = new DatabaseManager(this, db);

            doorOpener = new DoorOpener(this);
            flagOpener = new FlagOpener(this);
            bridgeOpener = new BridgeOpener(this);
            elevatorOpener = new ElevatorOpener(this);
            portcullisOpener = new PortcullisOpener(this);
            slidingDoorOpener = new SlidingDoorOpener(this);
            windmillOpener = new WindmillOpener(this);
            revolvingDoorOpener = new RevolvingDoorOpener(this);
            garageDoorOpener = new GarageDoorOpener(this);


            commandManager = new CommandManager(this);
            SuperCommand commandBigDoors = new CommandBigDoors(this, commandManager);
            {
                commandBigDoors.registerSubCommand(new SubCommandAddOwner(this, commandManager));
                commandBigDoors.registerSubCommand(new SubCommandCancel(this, commandManager));
                commandBigDoors.registerSubCommand(new SubCommandChangePowerBlock(this, commandManager));
                commandBigDoors.registerSubCommand(new SubCommandClose(this, commandManager));
                commandBigDoors.registerSubCommand(new SubCommandDebug(this, commandManager));
                commandBigDoors.registerSubCommand(new SubCommandDelete(this, commandManager));
                commandBigDoors.registerSubCommand(new SubCommandFill(this, commandManager));
                commandBigDoors.registerSubCommand(new SubCommandInfo(this, commandManager));
                commandBigDoors.registerSubCommand(new SubCommandInspectPowerBlock(this, commandManager));
                commandBigDoors.registerSubCommand(new SubCommandListDoors(this, commandManager));
                commandBigDoors.registerSubCommand(new SubCommandListPlayerDoors(this, commandManager));
                commandBigDoors.registerSubCommand(new SubCommandMenu(this, commandManager));
                commandBigDoors.registerSubCommand(new SubCommandSetName(this, commandManager));
                commandBigDoors.registerSubCommand(new SubCommandNew(this, commandManager));
                commandBigDoors.registerSubCommand(new SubCommandOpen(this, commandManager));
                commandBigDoors.registerSubCommand(new SubCommandPause(this, commandManager));
                commandBigDoors.registerSubCommand(new SubCommandRemoveOwner(this, commandManager));
                commandBigDoors.registerSubCommand(new SubCommandRestart(this, commandManager));
                commandBigDoors.registerSubCommand(new SubCommandSetAutoCloseTime(this, commandManager));
                commandBigDoors.registerSubCommand(new SubCommandSetBlocksToMove(this, commandManager));
                commandBigDoors.registerSubCommand(new SubCommandSetRotation(this, commandManager));
                commandBigDoors.registerSubCommand(new SubCommandStopDoors(this, commandManager));
                commandBigDoors.registerSubCommand(new SubCommandToggle(this, commandManager));
                commandBigDoors.registerSubCommand(new SubCommandVersion(this, commandManager));
            }
            commandManager.registerCommand(commandBigDoors);
            commandManager.registerCommand(new CommandMenu(this, commandManager));




            logger.info("Successfully enabled BigDoors " + getDescription().getVersion());
        }
        catch(Exception exception)
        {
            logger.logException(exception);
        }
    }

    @SuppressWarnings("unused")
    private void init()
    {
        if (!validVersion)
            return;

        readConfigValues();
        messages    = new Messages(this);
        toolUsers   = new HashMap<>();
        playerGUIs  = new HashMap<>();
        blockMovers = new Vector<>(2);
        cmdWaiters  = new Vector<>(2);
        tf          = new ToolVerifier(messages.getString("CREATOR.GENERAL.StickName"));
        loginString = "";

        if (config.enableRedstone())
        {
            redstoneHandler = new RedstoneHandler(this);
            Bukkit.getPluginManager().registerEvents(redstoneHandler, this);
        }
        // If the resourcepack is set to "NONE", don't load it.
        if (!config.resourcePack().equals("NONE"))
        {
            // If a resource pack was set for the current version of Minecraft, send that pack to the client on login.
            rPackHandler = new LoginResourcePackHandler(this, config.resourcePack());
            Bukkit.getPluginManager().registerEvents(rPackHandler,  this);
        }

        // Load stats collector if allowed, otherwise unload it if needed or simply don't load it in the first place.
        if (config.allowStats())
        {
            logger.info("Enabling stats! Thanks, it really helps!");
            if (metrics == null)
                metrics = new Metrics(this);
        }
        else
        {
            // Y u do dis? :(
            metrics = null;
            logger.info("Stats disabled, not laoding stats :(... Please consider enabling it! I am a simple man, seeing higher user numbers helps me stay motivated!");
        }

        // Load update checker if allowed, otherwise unload it if needed or simply don't load it in the first place.
        if (config.checkForUpdates() && !DEVBUILD)
        {
            if (updater == null)
                updater = new SpigotUpdater(this, 58669);
        }
        else
            updater = null;

        if (databaseManager != null)
            databaseManager.setCanGo(true);
    }

    public ICommand getCommand(CommandData command)
    {
        return commandManager.getCommand(command);
    }

    public int getMinimumDoorDelay()
    {
        return MINIMUMDOORDELAY;
    }

    public String canBreakBlock(UUID playerUUID, Location loc)
    {
        return protCompatMan.canBreakBlock(playerUUID, loc);
    }

    public String canBreakBlocksBetweenLocs(UUID playerUUID, Location loc1, Location loc2)
    {
        return protCompatMan.canBreakBlocksBetweenLocs(playerUUID, loc1, loc2);
    }

    public void registerRestartable(Restartable restartable)
    {
        restartables.add(restartable);
    }

    public void restart()
    {
        if (!validVersion)
            return;
        reloadConfig();

        onDisable();
        playerGUIs.forEach((key,value) -> value.close());
        playerGUIs.clear();

        HandlerList.unregisterAll(redstoneHandler);
        redstoneHandler = null;
        HandlerList.unregisterAll(rPackHandler);
        rPackHandler = null;

        init();

        restartables.forEach((K) -> K.restart());
    }

    @Override
    public void onDisable()
    {
        if (!validVersion)
            return;

        // Stop all toolUsers and take all BigDoor tools from players.
        databaseManager.setCanGo(false);

        Iterator<Entry<UUID, ToolUser>> it = toolUsers.entrySet().iterator();
        while (it.hasNext())
        {
            Entry<UUID, ToolUser> entry = it.next();
            entry.getValue().abort();
        }

        for (final BlockMover bm : blockMovers)
            bm.putBlocks(true);

        toolUsers.clear();
        cmdWaiters.clear();
        blockMovers.clear();
    }

    public TimedCache<Long, HashMap<Long, Long>> getPBCache()
    {
        return pbCache;
    }

    public FallingBlockFactory_Vall getFABF()
    {
        return fabf;
    }

    public BigDoors getPlugin()
    {
        return this;
    }

    public AutoCloseScheduler getAutoCloseScheduler()
    {
        return autoCloseScheduler;
    }

    public @Nullable Opener getDoorOpener(DoorType type)
    {
        if (!DoorType.isEnabled(type))
        {
            getMyLogger().severe("Trying to open door of type: \"" + type.toString() + "\", but this type is not enabled!");
            return null;
        }

        switch (type)
        {
        case BIGDOOR:
            return doorOpener;
        case DRAWBRIDGE:
            return bridgeOpener;
        case PORTCULLIS:
            return portcullisOpener;
        case SLIDINGDOOR:
            return slidingDoorOpener;
        case ELEVATOR:
            return elevatorOpener;
        case FLAG:
            return flagOpener;
        case WINDMILL:
            return windmillOpener;
        case REVOLVINGDOOR:
            return revolvingDoorOpener;
        case GARAGEDOOR:
            return garageDoorOpener;
        default:
            return null;
        }
    }

    public void addBlockMover(BlockMover blockMover)
    {
        blockMovers.add(blockMover);
    }

    public void removeBlockMover(BlockMover blockMover)
    {
        blockMovers.remove(blockMover);
    }

    public Vector<BlockMover> getBlockMovers()
    {
        return blockMovers;
    }

    public ToolUser getToolUser(Player player)
    {
        ToolUser tu = null;
        if (toolUsers.containsKey(player.getUniqueId()))
            tu = toolUsers.get(player.getUniqueId());
        return tu;
    }

    public void addToolUser(ToolUser toolUser)
    {
        toolUsers.put(toolUser.getPlayer().getUniqueId(), toolUser);
    }

    public void removeToolUser(ToolUser toolUser)
    {
        toolUsers.remove(toolUser.getPlayer().getUniqueId());
    }

    public boolean isPlayerBusy(Player player)
    {
        boolean isBusy = (getToolUser(player) != null || isCommandWaiter(player) != null);
        if (isBusy)
            Util.messagePlayer(player, getMessages().getString("GENERAL.IsBusy"));
        return isBusy;
    }

    public GUI getGUIUser(Player player)
    {
        GUI gui = null;
        if (playerGUIs.containsKey(player.getUniqueId()))
            gui = playerGUIs.get(player.getUniqueId());
        return gui;
    }

    public void addGUIUser(GUI gui)
    {
        playerGUIs.put(gui.getPlayer().getUniqueId(), gui);
    }

    public void removeGUIUser(GUI gui)
    {
        playerGUIs.remove(gui.getPlayer().getUniqueId());
    }

    public WaitForCommand getCommandWaiter(Player player)
    {
        for (final WaitForCommand wfc : cmdWaiters)
            if (wfc.getPlayer().equals(player))
                return wfc;
        return null;
    }

    // Get the Vector of WaitForCommand.
    public Vector<WaitForCommand> getCommandWaiters()
    {
        return cmdWaiters;
    }

    public WaitForCommand isCommandWaiter(Player player)
    {
        for (WaitForCommand cw : cmdWaiters)
            if (cw.getPlayer() == player)
                return cw;
        return null;
    }

    public void addCommandWaiter(WaitForCommand cmdWaiter)
    {
        cmdWaiters.add(cmdWaiter);
    }

    public void removeCommandWaiter(WaitForCommand cmdWaiter)
    {
        cmdWaiters.remove(cmdWaiter);
    }

    // Get the commander (class executing commands).
    public DatabaseManager getDatabaseManager()
    {
        return databaseManager;
    }

    // Get the logger.
    public MyLogger getMyLogger()
    {
        return logger;
    }

    // Get the messages.
    public Messages getMessages()
    {
        return messages;
    }

    // Returns the config handler.
    public ConfigLoader getConfigLoader()
    {
        return config;
    }

    public VaultManager getVaultManager()
    {
        return vaultManager;
    }

    // Get the ToolVerifier.
    public ToolVerifier getTF()
    {
        return tf;
    }

    private void readConfigValues()
    {
        // Load the settings from the config file.
        config = new ConfigLoader(this);
    }

    public boolean is1_13()
    {
        return is1_13;
    }

    // Check + initialize for the correct version of Minecraft.
    private boolean compatibleMCVer()
    {
        String version;

        try
        {
            version = Bukkit.getServer().getClass().getPackage().getName().replace(".",  ",").split(",")[3];
        }
        catch (final ArrayIndexOutOfBoundsException useAVersionMentionedInTheDescriptionPleaseException)
        {
            getMyLogger().logException(useAVersionMentionedInTheDescriptionPleaseException);
            return false;
        }

        fabf  = null;
        if (version.equals("v1_14_R1"))
        {
            is1_13 = true; // Yeah, it's actually 1.14, but it still needs to use new stuff.
            fabf = new FallingBlockFactory_V1_14_R1();
            headManager = new SkullCreator_V1_14_R1(this);
        }
        // Return true if compatible.
        return fabf != null;
    }

    public String getLoginString()
    {
        return loginString;
    }

    public void setLoginString(String str)
    {
        loginString = str;
    }

    public ItemStack getPlayerHead(UUID playerUUID, String displayName, OfflinePlayer oPlayer)
    {
        return headManagerv2.getPlayerHead(playerUUID, displayName, oPlayer);
    }



    /*
     * API Starts here.
     */

    // (Instantly?) Toggle a door with a given time.
    private DoorOpenResult toggleDoor(DoorBase door, double time, boolean instantOpen)
    {
        return getDoorOpener(door.getType()).openDoor(door, time, instantOpen, false);
    }

    // Toggle a door from a doorUID and instantly or not.
    public boolean toggleDoor(long doorUID, boolean instantOpen)
    {
        final DoorBase door = getDatabaseManager().getDoor(null, doorUID);
        return toggleDoor(door, 0.0, instantOpen) == DoorOpenResult.SUCCESS;
    }

    // Toggle a door from a doorUID and a given time.
    public boolean toggleDoor(long doorUID, double time)
    {
        final DoorBase door = getDatabaseManager().getDoor(null, doorUID);
        return toggleDoor(door, time, false) == DoorOpenResult.SUCCESS;
    }

    // Toggle a door from a doorUID using default values.
    public boolean toggleDoor(long doorUID)
    {
        final DoorBase door = getDatabaseManager().getDoor(null, doorUID);
        return toggleDoor(door, 0.0, false) == DoorOpenResult.SUCCESS;
    }

    // Check the open-status of a door.
    private boolean isOpen(DoorBase door)
    {
        return door.isOpen();
    }

    // Check the open-status of a door from a doorUID.
    public boolean isOpen(long doorUID)
    {
        final DoorBase door = getDatabaseManager().getDoor(null, doorUID);
        return this.isOpen(door);
    }

//    public long createNewDoor(Location min, Location max, Location engine,
//                              Location powerBlock, DoorType type, )
}
