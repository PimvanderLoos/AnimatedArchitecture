package nl.pim16aap2.bigdoors;

import nl.pim16aap2.bigdoors.api.IFallingBlockFactory;
import nl.pim16aap2.bigdoors.api.IGlowingBlockSpawner;
import nl.pim16aap2.bigdoors.commands.CommandBigDoors;
import nl.pim16aap2.bigdoors.commands.CommandData;
import nl.pim16aap2.bigdoors.commands.CommandMenu;
import nl.pim16aap2.bigdoors.commands.ICommand;
import nl.pim16aap2.bigdoors.commands.SuperCommand;
import nl.pim16aap2.bigdoors.commands.subcommands.SubCommandAddOwner;
import nl.pim16aap2.bigdoors.commands.subcommands.SubCommandCancel;
import nl.pim16aap2.bigdoors.commands.subcommands.SubCommandClose;
import nl.pim16aap2.bigdoors.commands.subcommands.SubCommandDebug;
import nl.pim16aap2.bigdoors.commands.subcommands.SubCommandDelete;
import nl.pim16aap2.bigdoors.commands.subcommands.SubCommandFill;
import nl.pim16aap2.bigdoors.commands.subcommands.SubCommandInfo;
import nl.pim16aap2.bigdoors.commands.subcommands.SubCommandInspectPowerBlock;
import nl.pim16aap2.bigdoors.commands.subcommands.SubCommandListDoors;
import nl.pim16aap2.bigdoors.commands.subcommands.SubCommandListPlayerDoors;
import nl.pim16aap2.bigdoors.commands.subcommands.SubCommandMenu;
import nl.pim16aap2.bigdoors.commands.subcommands.SubCommandMovePowerBlock;
import nl.pim16aap2.bigdoors.commands.subcommands.SubCommandNew;
import nl.pim16aap2.bigdoors.commands.subcommands.SubCommandOpen;
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
import nl.pim16aap2.bigdoors.config.ConfigLoader;
import nl.pim16aap2.bigdoors.doors.DoorBase;
import nl.pim16aap2.bigdoors.doors.DoorOpener;
import nl.pim16aap2.bigdoors.events.dooraction.DoorActionCause;
import nl.pim16aap2.bigdoors.gui.GUI;
import nl.pim16aap2.bigdoors.listeners.ChunkUnloadListener;
import nl.pim16aap2.bigdoors.listeners.EventListeners;
import nl.pim16aap2.bigdoors.listeners.GUIListener;
import nl.pim16aap2.bigdoors.listeners.LoginMessageListener;
import nl.pim16aap2.bigdoors.listeners.LoginResourcePackListener;
import nl.pim16aap2.bigdoors.listeners.RedstoneListener;
import nl.pim16aap2.bigdoors.listeners.WorldListener;
import nl.pim16aap2.bigdoors.managers.AutoCloseScheduler;
import nl.pim16aap2.bigdoors.managers.CommandManager;
import nl.pim16aap2.bigdoors.managers.DatabaseManager;
import nl.pim16aap2.bigdoors.managers.DoorManager;
import nl.pim16aap2.bigdoors.managers.HeadManager;
import nl.pim16aap2.bigdoors.managers.PowerBlockManager;
import nl.pim16aap2.bigdoors.managers.UpdateManager;
import nl.pim16aap2.bigdoors.managers.VaultManager;
import nl.pim16aap2.bigdoors.spigot.spigot_v1_14_R1.FallingBlockFactory_V1_14_R1;
import nl.pim16aap2.bigdoors.spigot.spigot_v1_14_R1.GlowingBlockSpawner_V1_14_R1;
import nl.pim16aap2.bigdoors.spigotutil.MessagingInterfaceSpigot;
import nl.pim16aap2.bigdoors.toolusers.ToolUser;
import nl.pim16aap2.bigdoors.toolusers.ToolVerifier;
import nl.pim16aap2.bigdoors.util.DoorToggleResult;
import nl.pim16aap2.bigdoors.util.IRestartable;
import nl.pim16aap2.bigdoors.util.IRestartableHolder;
import nl.pim16aap2.bigdoors.util.PLogger;
import nl.pim16aap2.bigdoors.util.messages.Messages;
import nl.pim16aap2.bigdoors.waitforcommand.WaitForCommand;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.UUID;

/*
 * Moonshoots
 */
// TODO: Make blocks massive
// TODO: Implement perpetual motion (for flags, clocks, ??).
// TODO: Allow custom sounds. Every type should have its own sound file. This file should describe the name of the
//       sounds in the resource pack and the length of the sound. It should contain both movement and finish sounds.
// TODO: Implement my own Optional that allows ifPresent(Consumer<? super T> consumer).orElse(Consumer<? super T> consumer).
//       Or implement's Java9's ifPresentOrElse:
//       https://docs.oracle.com/javase/9/docs/api/java/util/Optional.html#ifPresentOrElse-java.util.function.Consumer-java.lang.Runnable-
// TODO: Use custom events for door opening. Perhaps allow other plugins (keys-plugin?) to hook into those plugins.
//       The implementation for Spigot can use Spigot's event handling system, but don't forget to keep other
//       implementations in mind!
// TODO: Write an event to open doors. It should be able to retrieve door from the database on a secondary thread and also check offline permission on the second thread.
// TODO: Door pooling. When a door is requested from the database, store it in a timedCache. Only get the creator by default, but store
//       the other owners if needed. Add a Door::Sync function to sync (new) data with the database.
//       Also part of this should be a DoorManager. NO setters should be available to any class other than the doorManager.
//       Creators/Database might have to go back to the awful insanely long constructor.
//       When a door is modified, post a doorModificationEvent. Instead of firing events, perhaps look into observers?
// TODO: Write a wrapper or something for Tasks etc to allow for something like .executeAsync(0, 20000L).whenComplete(doSomething).
//       This would be useful for the BlockMovers.
// TODO: Look into https://hub.spigotmc.org/javadocs/spigot/org/bukkit/Chunk.html#getChunkSnapshot to verify if a door
//       can be opened and while I'm at it, also to construct the FBlocks. This way, that stuff can all be done
//       async.


/*
 * Modules
 */
// TODO: Use AbstractWorld and AbstractLocation etc for API related stuff. Also use an abstracted player. This can also
//       be used to store additional data, such as sorting preferences and whatnot.
// TODO: Put Config-related stuff in BigDoorsUtil and don't use Bukkit stuff for reading it. Just give it a regular file.
//       Make the ConfigLoader abstract and extend a Spigot-specific config in SpigotUtil with Spigot-Specific options
//       such as resource packs.
// TODO: Put isAllowed block and such in appropriate modules.

/*
 * Experimental
 */
// TODO: Look into allowing people to set a (estimated) max size in RAM for certain caches.
//       Example: https://www.javaworld.com/article/2074458/estimating-java-object-sizes-with-instrumentation.html
//       Simpler example: https://stackoverflow.com/questions/52353/in-java-what-is-the-best-way-to-determine-the-size-of-an-object#
//       Another: https://javamagic.blog/2018/07/11/how-to-find-size-of-java-object-in-memory-using-jol/
//       Also: https://www.baeldung.com/java-size-of-object
// TODO: Fix violation of Liskov Substitution Problem in certain subclasses of Door (i.e. Revovlving Door, as it has no lookingDir).
//       Though, thinking about it, DoorBase doesn't really need this function anyway, if all opener code is put in the doorClass anyway.
// TODO: Fix violation of LSP for doorAttributes. Instead of having a switch per type in the GUI, override a return in the DoorAttribute enum.
// TODO: Data is duplicated a lot! Currently, the falling block has the IBlockData, and so does PBlockData. If the block can rotate, it has it twice, even!
//       This is a huge waste of Ram. The falling block should be the only place to store the block data.
// TODO: Instead of killing the FBlock, rotating it and then respawning it, rotate the FBlockData and then send the
//       appropriate packets to the appropriate players. This can also be done async, so no more sync scheduling needed.
// TODO: Add command to upload error log to pastebin or something similar.
// TODO: Add config option to limit logging file size: https://kodejava.org/how-do-i-limit-the-size-of-log-file/
// TODO: Look into previously-blacklisted material. Torches, for example, work just fine in 1.13. They just need to be removed first and placed last.
// TODO: Replace the enum of DoorTypes by a static list. Types should then statically register themselves.
// TODO: Figure out a way to use Interfaces or something to generate 2 separate builds: Premium and non-premium.
// TODO: Look into Aikar's command system to replace my command system: https://www.spigotmc.org/threads/acf-beta-annotation-command-framework.234266/
//       Alternatively, fix up the current system and use abstraction to make sure it does NOT rely on bukkit anymore.
// TODO: Get rid of the DoorType enum. Instead, allow dynamic registration of door types.
// TODO: Stop naming all animatable objects "doors". An elevator is hardly a door.
// TODO: Add a system that can check the integrity of the plugin. If something goes wrong during startup, make sure OPs get a message on login.
//       Tell them to use the command to run the self-check. Then it can say "not the latest version!", "Money formulas set, but Vault not enabled!",
//       "Enabled PlotSquared support, but failed to initialize it!", "Trying to load on version X, but this version isn't supported!", etc.
// TODO: Special PBlockData subclass for every type of opener. This is a messy system.
// TODO: Use Spigot Premium's placeholders: https://www.spigotmc.org/wiki/premium-resource-placeholders-identifiers/
//       Then send the placeholder to my site on startup. Why? As evidence the buyer has, in fact, downloaded the plugin.
//       This could be useful in case of a PayPal chargeback.
// TODO: Implement admin command to show database statistics (player count, door count, owner count, etc).
// TODO: Process ALL redstone actions on a separate thread. Then allow async powerblock cache checking so the queue can
//       query the cache and if needed schedule a sync event to either update the cache or toggle a door.
// TODO: Make more use of CompletableFuture. It's useful as fuck.
// TODO: Move all database interaction off of the main thread.
// TODO: Consider storing original locations in the database. Then use the OpenDirection as the direction to go when in
//       the original position. Then you cannot make regular doors go in a full circle anymore.
// TODO: Instead of placing all blocks one by one and sending packets to the players about it, use this method instead:
//       https://www.spigotmc.org/threads/efficiently-change-large-area-of-blocks.262341/#post-2585360

/*
 * Doors
 */
// TODO: getBlocksToMove() should return the number of blocks it'll move, regardless of if this value was set.
//       Internally, keep track if the specified and the default value, then return the specified value if possible,
//       otherwise the default value. Also distinguish between goal and actual.
// TODO: Create method in DoorBase for checking distance. Take Vector3D for distance and direction.
// TODO: Get rid of the engineSide and currentDirection. It's basically the same thing. I don't think the database
//       should be troubled with storing this data either, so remove it there.
// TODO: Having both openDirection and rotateDirection is stupid. Check DoorBase#getNewLocations for example.
// TODO: Don't use Locations for the locations. Use vectors instead.
// TODO: Cache value of DoorBase#getSimplePowerBlockChunkHash().
// TODO: Use the IGetNewLocation code to check new pos etc.
// TODO: Statically store GNL's in each door type.
// TODO: Add getCloseDirection() method. This is NOT!!! the opposite of the openDirection once the original coordinates
//       are stored in the database. It should be the direction back to the original position.
// TODO: Add DoorBase#isValidOpenDirection. Can be useful for creation and validation.
// TODO: Store calculated stuff such as blocksInDirection in object-scope variables, so they don't have to be
//       calculated more than once.

/*
 * General
 */
// TODO: Implement TPS limit. Below a certain TPS, doors cannot be opened.
//       double tps = ((CraftServer) Bukkit.getServer()).getServer().recentTps[0]; // 3 values: last 1, 5, 15 mins.
// TODO: Move all non-database related stuff out of DatabaseManager.
// TODO: Rename region bypass permission to bigdoors.admin.bypass.region.
// TODO: ICustomEntityFallingBlock: Clean this damn class up!
// TODO: Look into restartables interface. Perhaps it's a good idea to split restart() into stop() and init().
//       This way, it can call all init()s in BigDoors::onEnable and all stop()s in BigDoors::onDisable.
// TODO: Store PBlockFace in rotateDirection so I don't have to cast it via strings. ewww.
//       Alternatively, merge PBlockFace and RotateDirection into Direction.
// TODO: When returning null after unexpected input, just fucking thrown an IllegalArgumentException. Will make debugging a lot easier.
// TODO: Get rid of all occurrences of "boolean onDisable". Just do it via the main class.
// TODO: ConfigLoader: Use dynamic protection compat listing. Just like how door prices etc are handled.
// TODO: Get rid of ugly 1.14 hack for checking for forceloaded chunks.
// TODO: Allow wand material selection in config.
// TODO: Get rid of code duplication in ProtectionCompatManager.
// TODO: Make sure permission checking for offline users isn't done on the main thread.
// TODO: Make timeout for CommandWaiters and Creators configurable and put variable in messages.
// TODO: Somehow replace the %HOOK% variable in the message of DoorOpenResult.NOPERMISSION.
// TODO: Instead of routing everything through this class (e.g. getPLogger(), getConfigLoader()), make sure that these
//       Objects do NOT get reinitialized on restart and then pass references to class that need them. Should reduce the
//       clutter in this class a bit and reduce dependency on this class.
// TODO: Rename bigdoors-api. Maybe bigdoors-abstraction? Just to make it clear that it's not the actual API.
// TODO: Give TimedMapCache some TLC. Make sure all methods are implemented properly and find a solution for timely removal of entries.
//       Also: Use lastAccessTime instead of addTime for timeout values.
//       Alternatively, consider deleting it and using this instead: https://github.com/jhalterman/expiringmap
// TODO: Keep VaultManager#setupPermissions result. Perhaps this class should be split up.
// TODO: Keep track of the DoorActionCause when opening doors. Don't use a dumb boolean anymore for isSilent.
// TODO: Remove blockMovers from BigDoors-core.
// TODO: Make sure to keep the config file's maxDoorCount in mind. Or just remove it.
// TODO: Fix (big) mushroom blocks changing color.
// TODO: Configurable timeouts for commands + creators. Also, put variable in messages.
// TODO: Documentation: Instead of "Get the result", use "Gets the result" and similar.
// TODO: Create abstraction layer for config stuff. Just wrap Bukkit's config stuff for the Spigot implementation (for now).
// TODO: Get rid of all calls to SpigotUtil for messaging players. They should all go via the proper interface for that.
// TODO: Logging, instead of "onlyLogExceptions", properly use logging levels.

/*
 * GUI
 */
// TODO: Look into using player heads for GUI buttons. Example: https://minecraft-heads.com/player-heads/alphabet/2762-arrow-left
// TODO: Make GUI options always use the correct subCommand.
// TODO: Create ItemManager that stores repeatedly used items (such as door creation books and doorInfo stuff).
// TODO: Store 2 player objects: 1) Subject (the owner of all the doors), and 2) InventoryHolder (who is looking at the inventory).
// TODO: Cannot toggle openDirection for portcullis type. Might be related to the fact that it says that the portcullis openDirection is North instead of Up/Down.
// TODO: Use ButtonAction GUI and GUIItem::specialValue to phase out raw interactionIDX stuff for getting actions.
// TODO: Update items in inventory instead of opening a completely new inventory. No longer requires dirty code to check is it's refreshing etc. Bweugh.
// TODO: Once door pooling is implemented, use Observers to update doors in the GUI when needed.
// TODO: Move rotation cycling away from GUI and into the Door class.
// TODO: Put all GUI buttons and whatnot in try/catch blocks.
// TODO: Get rid of the retarded isRefreshing bullshit. Instead of reopening an inventory, just replace the items. Simpler, faster, less demented.
// TODO: Use a less stupid way to check for interaction: https://www.spigotmc.org/threads/quick-tip-how-to-check-if-a-player-is-interacting-with-your-custom-gui.225871/
// TODO: Documentation.
// TODO: Look into refresh being called too often. Noticed this in GUIPageRemoveOwner (it tries to get a head twice).
// TODO: Store Message in GUI pages. Then use that to check if the player is in a custom GUI page.

/*
 * SQL
 */
// TODO: Store original coordinates in the database. These can be used to find the actual close direction.
// TODO: Consider doing all upgrades on a separate thread. Then the server won't have to wait for the upgrade to finish.
//       Added bonus: startReplaceTempPlayerNames() can be simplified.
// TODO: Create new table for DoorTypes: {ID (AI) | PLUGIN | TYPENAME}, with UNIQUE(PLUGIN, TYPENAME).
//       Then use FK from doors to doortypes. Useful for allowing custom door types.

/*
 * Commands
 */
// TODO: Add /BDM [PlayerName (when online) || PlayerUUID || Server] to open a doorMenu for a specific player
// TODO: Make invalid input stuff more informative (e.g. int, float etc).
// TODO: Properly use flags for stuff. For example: "/bigdoors open testDoor -time 10" to open door "testDoor" in 10 seconds.
// TODO: Allow adding owners to doors from console.
// TODO: When the plugin fails to initialize properly, register alternative command handler to display this info.
// TODO: Move stuff such as StartTimerForAbortable into appropriate command classes.
// TODO: When retrieving player argument (SubCommandAddOwner, SubCommandListPlayerDoors) don't just try to convert
//       the provided playerArg to a UUID. That's not very user friendly at all.
// TODO: Check if force unlock door as admin still exists.
// TODO: Do not use the commander for anything command-related that isn't strictly database abstraction.
// TODO: Store actual minArgCount in subCommands so it doesn't have to be calculated constantly.
// TODO: Make sure there are no commands that use hard coded argument positions.
// TODO: NPE thrown when trying to use direct command after initiating a commandWaiter a while ago (should've been cancelled already!).
//       Might be related to the issue listed above (regarding setBlocksToMove commandWaiter).
// TODO: Document why there are 2 hashMaps storing seemingly the same data in the CommandManager.
// TODO: Make sure super command can be chained.
// TODO: Fix bigdoors doorinfo in console.
// TODO: Cancelling command can result in NPE. Possibly after timeout.
// TODO: SetBlocksToMove: Waiter is cancelled both by the subCommand and the waiter. Make sure all commandWaiters are disabled in the waiter.
// TODO: CommandWaiters should register themselves in the CommandManager class. No outside class should even know about
//       this stuff. Especially not the fucking DatabaseManager.
// TODO: Make more liberal use of IllegalArgumentException.
// TODO: SubCommandSetRotation should be updated/removed, as it doesn't work properly with types that do not go (counter)clockwise.
// TODO: Make system similar to door initialization for DoorCreators. Look at SubCommandNew.
// TODO: Also look into SubCommandRemoveOwner.
// TODO: Let users verify door price (if more than 0) if they're buying a door.
// TODO: What's going on with the newDoor variable in SubCommandToggle::execute(CommandSender, DoorBase, double)?
// TODO: Add an isWaiter() method to the commands. If true, it should catch those calls before calling the command implementations.
//       Gets rid of some code duplication.
// TODO: CHeck if minArgCount is used properly.
// TODO: Make "/BigDoors new" require the type as flag input. No more defaulting to regular doors.
// TODO: Fix "/BigDoors filldoor db4" not working.
// TODO: Make sure you cannot use direct commands (i.e. /setPowerBlockLoc 12) of doors not owned by the one using the command.
// TODO: For all commands that support either players or door names etc, just use flags instead of the current mess.

/*
 * Creators
 */
// TODO: Make users explicitly specify the openDirection on door creation.

/*
 * Openers / Movers
 */
// TODO: Figure out why the animated blocks despawn after a certain amount of time.
// TODO: Make the getOpenDirection function of the openers static, so the Creators can check which direction to pick.
//       Then set the direction in the creator.
// TODO: Rotate Sea Pickle and turtle egg.
// TODO: Replace current time/speed/tickRate system. It's a mess.
// TODO: Get rid of all material related stuff in these classes. isAllowedBlock should be abstracted away.
// TODO: Consider using HashSet for blocks. It's faster: https://stackoverflow.com/questions/10196343/hash-set-and-array-list-performances
// TODO: Do block deleting + placing in two passes: For removal: First remove all "attached" blocks such as torches.
//       Then do the rest on the second pass. For placing: Place all non-"attached" blocks on the first pass. Then
//       place all "attached" blocks on the second pass and at the same time verify all connected blocks (fences, etc)
//       are properly connected to each other.
// TODO: Test and finish flag type.
// TODO: Rewrite parts of the drawBridge opener and mover. The upDown etc stuff should not be used.
// TODO: ElevatorOpener and PortcullisOpener should respect setOpenDirection and min/max world height (0, 256).
// TODO: Remove getNewLocation() method from Movers. Instead, they should ALL use a GNL. GNLs should not just get the
//       x,y,z values, but the entire block and blocksMoved. Then they can figure it out for themselves.
// TODO: Make some kind of interface TravelingDoor, that includes the updateCoords and getNewLocation methods.
//       Then movers that don't actually move the object (flag, windmill) don't need to include those methods.
// TODO: Move rotation/respawning block code out of runnables. Perhaps even into BLockMover. Same goes for termination conditions.
// TODO: Windmill: Remove magic values in endCount and Step variables in WindmillMover::animateEntities();
// TODO: Windmill: Allow setting rotational speed (seconds per rotation).
// TODO: Clamp angles to [-2PI ; 2PI].
// TODO: Either use time or ticks. Not both.
// TODO: Make sure the new types don't just open instantly without a provided time parameter    !!!!!!!!
// TODO: Rename variables in updateCoords to avoid confusion. Perhaps a complete removal altogether would be nice as well.
// TODO: Get rid of the GNL interface etc. The movers class can handle it on its own using Function interface.
// TODO: Move getBlocksMoved() to Mover.
// TODO: GarageDoorCreator: Fix having to double click last block.
// TODO: GarageDoorCreator: Before defaulting to North/East, check if those directions are actually available.
// TODO: Allow blocks with inventories to be moved.
// TODO: Do not allow setting of invalid rotation directions. If a garage door is positioned along the z axis, only North and South are valid options.
// TODO: Instead of creating and running the runnables in the animateEntities method, create the runnable earlier and store it. Then call animateEntities()
//       from BlockMover. Then let BlockMover extend Restartable and/or abortable, so that the it can cancel all movers etc on restart, so this code doesn't have to be part
//       of the runnable anymore. Much cleaner.
// TODO: Make setDefaultOpenDirection() smarter by checking which side is actually available.
// TODO: Movers: updateCoords should be final and use the DoorBase::getNewLocations method to get the new min/max.
// TODO: GarageDoor: The pivot point offset (where it starts pivoting), should depend on the radius. The higher the radius of the block compared
//       to the total radius, the smaller the offset should be. This way, the final blocks will line up better with the final position.
//       radius = maxRadius -> offset = 0. Should probably only look at the last 3 blocks. e.g.: offset = Min((offset / 4) * (totalRadius - radius)).
// TODO: Drawbridge: Cleanup #getNewLocation().
// TODO: When checking if a door's chunks are loaded, use the door's chunkRange variables.
// TODO: Instead of having methods to open/close/toggle animated objects, instead have a single method that receives
//       a redstone value or something. Then each animated object can determine how to handle it on its own.
//       Open/close/toggle for doors, activate/deactivate for persistent movement (flags, clocks, etc).
// TODO: Implement clock.
// TODO: SlidingDoor, Portcullis: Cache BlocksToMove in a Vec2D. Invalidate when coors and stuff change.
// TODO: Update NS variables in the movers so they don't mean "active along north/south axis" but rather
//       "engine aligned with north/south axis", which effectively means the opposite. Also, obtain the variable from
//       the door.
// TODO: Potentially implement BlockMover#animateEntities in BlockMover. Then use function pointers to calculate everything.
// TODO: Highlight all blocking blocks if BlocksToMove is set.
// TODO: Pass new min and new max to the movers. Then they can just update it.
// TODO: When a block is "blocking" a door from being opened, check if there isn't a corresponding gap in the door to be opened.
// TODO: Reduce code duplication in the blockmovers (specifically animateEntities).
// TODO: Make RevolvingDoorMover and CylindricalMover more closely related.
// TODO: Make sure all types respect their multiplier.
// TODO: Get rid of the stupid .101 multiplier for vectors. Use proper normalization and shit instead.
//       Look at this: https://github.com/InventivetalentDev/AdvancedSlabs/blob/ad2932d5293fa913b9a0670a0bc8ea52f1e27e0d/Plugin/src/main/java/org.inventivetalent.advancedslabs/movement/path/types/CircularSwitchController.java#L85
// TODO: Properly keep track of who opened a door, instead of just passing along the owner's UUID.
// TODO: Add PerpetualMover BlockMover. Then use an interface for the functions, which the individual movers can set.
//       Also have a rotation method.

/*



 */

/*
 * Manual Testing
 */
// TODO: ALL the new Openers (which are now part of the doors themselves).
// TODO: Test new creators: Windmill, RevolvingDoor, GarageDoor. Make sure it cannot be fucked up.
// TODO: Test new chunkInRange methods. Especially sliding door.
// TODO: Make sure that new lines in the messages work (check Util::stringFromArray).
// TODO: Fix no permission to set AutoCloseTime from GUI.
// TODO: Check if TimedCache#containsValue() works properly.
// TODO: What happens when a player is given a creator stick while their inventory is full?

/*
 * Unit tests
 */
// TODO: https://bukkit.org/threads/how-to-unit-test-your-plugin-with-example-project.23569/
// TODO: https://www.spigotmc.org/threads/using-junit-to-test-plugins.71420/#post-789671
// TODO: https://github.com/seeseemelk/MockBukkit

public final class BigDoors extends JavaPlugin implements Listener, IRestartableHolder
{
    public static final boolean DEVBUILD = true;
    private static BigDoors INSTANCE;

    /**
     * Minimum number of ticks a door needs to cool down before it can be toggled again. This should help with some rare
     * cases of overlapping processes and whatnot.
     */
    private static final int MINIMUMDOORDELAY = 10;

    private ToolVerifier tf;
    private IFallingBlockFactory fabf;
    private ConfigLoader config;
    private PLogger logger;
    private Metrics metrics;
    private Messages messages;
    private DatabaseManager databaseManager = null;

    private RedstoneListener redstoneListener;
    private boolean validVersion;
    private CommandManager commandManager;
    private Map<UUID, WaitForCommand> cmdWaiters;
    private Map<UUID, ToolUser> toolUsers;
    private Map<UUID, GUI> playerGUIs;
    private List<IRestartable> restartables = new ArrayList<>();
    private ProtectionCompatManager protCompatMan;
    private LoginResourcePackListener rPackHandler;
    private VaultManager vaultManager;
    private AutoCloseScheduler autoCloseScheduler;
    private HeadManager headManager;
    private IGlowingBlockSpawner glowingBlockSpawner;
    private UpdateManager updateManager;
    private DoorManager doorManager;
    private PowerBlockManager powerBlockManager;

    @Override
    public void onEnable()
    {
        INSTANCE = this;
        logger = PLogger.init(new File(getDataFolder(), "log.txt"), new MessagingInterfaceSpigot(this));

        try
        {
            updateManager = new UpdateManager(this, 58669);

            Bukkit.getPluginManager().registerEvents(new LoginMessageListener(this), this);

            validVersion = compatibleMCVer();

            // Load the files for the correct version of Minecraft.
            if (!validVersion)
            {
                logger.severe("Trying to load the plugin on an incompatible version of Minecraft! (\""
                                  + (Bukkit.getServer().getClass().getPackage().getName().replace(".", ",")
                                           .split(",")[3])
                                  + "\"). This plugin will NOT be enabled!");
                return;
            }

            config = ConfigLoader.init(this, getPLogger());
            doorManager = DoorManager.init(this);

            init();
            tf = new ToolVerifier(messages, this);
            vaultManager = new VaultManager(this);
            autoCloseScheduler = new AutoCloseScheduler(this);

            headManager = HeadManager.init(this, getConfigLoader());

            Bukkit.getPluginManager().registerEvents(new EventListeners(this), this);
            Bukkit.getPluginManager().registerEvents(new GUIListener(this), this);
            Bukkit.getPluginManager().registerEvents(new ChunkUnloadListener(this), this);
            protCompatMan = new ProtectionCompatManager(this);
            Bukkit.getPluginManager().registerEvents(protCompatMan, this);
            databaseManager = DatabaseManager.init(this, config.dbFile());
            powerBlockManager = PowerBlockManager.init(this, config, databaseManager, getPLogger());
            Bukkit.getPluginManager().registerEvents(WorldListener.init(powerBlockManager), this);
            DoorOpener.init(getPLogger(), getDoorManager(), getGlowingBlockSpawner(), getConfigLoader(), protCompatMan);
            commandManager = new CommandManager(this);
            SuperCommand commandBigDoors = new CommandBigDoors(this, commandManager);
            {
                commandBigDoors.registerSubCommand(new SubCommandAddOwner(this, commandManager));
                commandBigDoors.registerSubCommand(new SubCommandCancel(this, commandManager));
                commandBigDoors.registerSubCommand(new SubCommandMovePowerBlock(this, commandManager));
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
        catch (Exception exception)
        {
            logger.logException(exception);
        }
    }

    public static BigDoors get()
    {
        return INSTANCE;
    }

    private void init()
    {
        if (!validVersion)
            return;

        config.reloadConfig();
        getPLogger().setConsoleLogging(getConfigLoader().consoleLogging());
        messages = new Messages(this, getDataFolder(), getConfigLoader().languageFile(), getPLogger());
        toolUsers = new HashMap<>();
        playerGUIs = new HashMap<>();
        cmdWaiters = new HashMap<>();

        if (config.enableRedstone())
        {
            redstoneListener = new RedstoneListener(this);
            Bukkit.getPluginManager().registerEvents(redstoneListener, this);
        }
        // If the resourcepack is set to "NONE", don't load it.
        if (!config.resourcePack().equals("NONE"))
        {
            // If a resource pack was set for the current version of Minecraft, send that
            // pack to the client on login.
            rPackHandler = new LoginResourcePackListener(this, config.resourcePack());
            Bukkit.getPluginManager().registerEvents(rPackHandler, this);
        }

        // Load stats collector if allowed, otherwise unload it if needed or simply
        // don't load it in the first place.
        if (config.allowStats())
        {
            logger.info("Enabling stats! Thanks, it really helps!");
            if (metrics == null)
                try
                {
                    metrics = new Metrics(this);
                }
                catch (Exception e)
                {
                    logger.logException(e, "Failed to intialize stats! Please contact pim16aap2!");
                }
        }
        else
        {
            // Y u do dis? :(
            metrics = null;
            logger.info("Stats disabled; not loading stats :(... Please consider enabling it! "
                            + "It helps me stay motivated to keep working on this plugin!");
        }

        updateManager.setEnabled(getConfigLoader().checkForUpdates(), getConfigLoader().autoDLUpdate());
    }

    @NotNull
    public ICommand getCommand(final @NotNull CommandData command)
    {
        return commandManager.getCommand(command);
    }

    public int getMinimumDoorDelay()
    {
        return MINIMUMDOORDELAY;
    }

    @NotNull
    public Optional<String> canBreakBlock(final @NotNull UUID playerUUID, final @NotNull Location loc)
    {
        return protCompatMan.canBreakBlock(playerUUID, loc);
    }

    @NotNull
    public Optional<String> canBreakBlocksBetweenLocs(final @NotNull UUID playerUUID,
                                                      final @NotNull Location loc1,
                                                      final @NotNull Location loc2)
    {
        return protCompatMan.canBreakBlocksBetweenLocs(playerUUID, loc1, loc2);
    }

    @Override
    public void registerRestartable(final @NotNull IRestartable restartable)
    {
        restartables.add(restartable);
    }

    public void restart()
    {
        if (!validVersion)
            return;

        reloadConfig();

        shutdown();
        playerGUIs.forEach((key, value) -> value.close());
        playerGUIs.clear();

        HandlerList.unregisterAll(redstoneListener);
        redstoneListener = null;
        HandlerList.unregisterAll(rPackHandler);
        rPackHandler = null;

        init();

        restartables.forEach(IRestartable::restart);
    }

    @Override
    public void onDisable()
    {
        shutdown();
        restartables.forEach(IRestartable::shutdown);
    }

    private void shutdown()
    {
        if (!validVersion)
            return;

        Iterator<Entry<UUID, ToolUser>> it = toolUsers.entrySet().iterator();
        while (it.hasNext())
        {
            Entry<UUID, ToolUser> entry = it.next();
            entry.getValue().abort();
        }

        toolUsers.clear();
        cmdWaiters.clear();
    }

    @NotNull
    public IFallingBlockFactory getFABF()
    {
        return fabf;
    }

    @NotNull
    public IGlowingBlockSpawner getGlowingBlockSpawner()
    {
        return glowingBlockSpawner;
    }

    @NotNull
    public BigDoors getPlugin()
    {
        return this;
    }

    @NotNull
    public AutoCloseScheduler getAutoCloseScheduler()
    {
        return autoCloseScheduler;
    }

    @NotNull
    public Optional<ToolUser> getToolUser(final @NotNull Player player)
    {
        return Optional.ofNullable(toolUsers.get(player.getUniqueId()));
    }

    public void addToolUser(final @NotNull ToolUser toolUser)
    {
        toolUsers.put(toolUser.getPlayer().getUniqueId(), toolUser);
    }

    public void removeToolUser(final @NotNull ToolUser toolUser)
    {
        toolUsers.remove(toolUser.getPlayer().getUniqueId());
    }

    @NotNull
    public Optional<GUI> getGUIUser(final @NotNull Player player)
    {
        GUI gui = null;
        if (playerGUIs.containsKey(player.getUniqueId()))
            gui = playerGUIs.get(player.getUniqueId());
        return Optional.ofNullable(gui);
    }

    public void addGUIUser(final @NotNull GUI gui)
    {
        playerGUIs.put(gui.getPlayer().getUniqueId(), gui);
    }

    public void removeGUIUser(final @NotNull GUI gui)
    {
        playerGUIs.remove(gui.getPlayer().getUniqueId());
    }

    @NotNull
    public Optional<WaitForCommand> getCommandWaiter(final @NotNull Player player)
    {
        if (cmdWaiters.containsKey(player.getUniqueId()))
            return Optional.of(cmdWaiters.get(player.getUniqueId()));
        return Optional.empty();
    }

    public void addCommandWaiter(final @NotNull WaitForCommand cmdWaiter)
    {
        cmdWaiters.put(cmdWaiter.getPlayer().getUniqueId(), cmdWaiter);
    }

    public void removeCommandWaiter(final @NotNull WaitForCommand cmdWaiter)
    {
        cmdWaiters.remove(cmdWaiter.getPlayer().getUniqueId());
    }

    public void onPlayerLogout(final @NotNull Player player)
    {
        getCommandWaiter(player).ifPresent(WaitForCommand::abortSilently);
        cmdWaiters.remove(player.getUniqueId());
        playerGUIs.remove(player.getUniqueId());
        getToolUser(player).ifPresent(ToolUser::abortSilently);
        toolUsers.remove(player.getUniqueId());
    }

    // Get the DatabaseManager
    @NotNull
    public DatabaseManager getDatabaseManager()
    {
        return databaseManager;
    }

    @NotNull
    public PowerBlockManager getPowerBlockManager()
    {
        return powerBlockManager;
    }

    // Get the DoorManager.
    @NotNull
    public DoorManager getDoorManager()
    {
        return doorManager;
    }

    // Get the logger.
    @NotNull
    public PLogger getPLogger()
    {
        return logger;
    }

    // Get the messages.
    @NotNull
    public Messages getMessages()
    {
        return messages;
    }

    // Returns the config handler.
    @NotNull
    public ConfigLoader getConfigLoader()
    {
        return config;
    }

    @NotNull
    public VaultManager getVaultManager()
    {
        return vaultManager;
    }

    // Get the ToolVerifier.
    @NotNull
    public ToolVerifier getTF()
    {
        return tf;
    }

    // Check + initialize for the correct version of Minecraft.
    private boolean compatibleMCVer()
    {
        String version;

        try
        {
            version = Bukkit.getServer().getClass().getPackage().getName().replace(".", ",").split(",")[3];
        }
        catch (final ArrayIndexOutOfBoundsException useAVersionMentionedInTheDescriptionPleaseException)
        {
            getPLogger().logException(useAVersionMentionedInTheDescriptionPleaseException);
            return false;
        }

        fabf = null;
        if (version.equals("v1_14_R1"))
        {
            glowingBlockSpawner = new GlowingBlockSpawner_V1_14_R1(this, getPLogger());
            fabf = new FallingBlockFactory_V1_14_R1();
        }

        // Return true if compatible.
        return fabf != null;
    }

    @NotNull
    public String getLoginMessage()
    {
        String ret = "";
        if (DEVBUILD)
            ret += "[BigDoors] Warning: You are running a devbuild!\n";
        if (updateManager.updateAvailable())
        {
            if (getConfigLoader().autoDLUpdate() && updateManager.hasUpdateBeenDownloaded())
                ret += "[BigDoors] A new update (" + updateManager.getNewestVersion() + ") has been downloaded! "
                    + "Restart your server to apply the update!\n";
            else if (updateManager.updateAvailable())
                ret += "[BigDoors] A new update is available: " + updateManager.getNewestVersion() + "\n";
        }
        return ret;
    }

    @NotNull
    public HeadManager getHeadManager()
    {
        return headManager;
    }

    /*
     * API Starts here.
     */

    // (Instantly?) Toggle a door with a given time.
    private DoorToggleResult toggleDoor(final @Nullable UUID playerUUID, final @NotNull DoorBase door,
                                        final double time, final boolean instantOpen)
    {
        if (playerUUID == null)
            return door.toggle(DoorActionCause.REDSTONE, door.getPlayerUUID(), time, instantOpen);
        else
            return door.toggle(DoorActionCause.PLAYER, playerUUID, time, instantOpen);
    }

    // Toggle a door from a doorUID and instantly or not.
    public boolean toggleDoor(final long doorUID, final boolean instantOpen)
    {
        return getDatabaseManager().getDoor(doorUID)
                                   .filter(door -> toggleDoor(null, door, 0.0, instantOpen) ==
                                       DoorToggleResult.SUCCESS)
                                   .isPresent();
    }

    // Toggle a door from a doorUID and a given time.
    public boolean toggleDoor(final long doorUID, final double time)
    {
        return getDatabaseManager().getDoor(doorUID)
                                   .filter(door -> toggleDoor(null, door, time, false) == DoorToggleResult.SUCCESS)
                                   .isPresent();
    }

    // Toggle a door from a doorUID using default values.
    public boolean toggleDoor(final long doorUID)
    {
        return getDatabaseManager().getDoor(doorUID)
                                   .filter(door -> toggleDoor(null, door, 0.0, false) == DoorToggleResult.SUCCESS)
                                   .isPresent();
    }

    // Check the open-status of a door.
    private boolean isOpen(final @NotNull DoorBase door)
    {
        return door.isOpen();
    }

    // Check the open-status of a door from a doorUID.
    public boolean isOpen(final long doorUID)
    {
        return getDatabaseManager().getDoor(doorUID)
                                   .filter(door -> isOpen(door.getDoorUID()))
                                   .isPresent();
    }
}
