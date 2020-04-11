package nl.pim16aap2.bigdoors;

import nl.pim16aap2.bigdoors.api.IBigDoorsPlatform;
import nl.pim16aap2.bigdoors.api.IMessagingInterface;
import nl.pim16aap2.bigdoors.managers.AutoCloseScheduler;
import nl.pim16aap2.bigdoors.managers.DatabaseManager;
import nl.pim16aap2.bigdoors.managers.DoorManager;
import nl.pim16aap2.bigdoors.managers.PowerBlockManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/*
 * Moonshoots
 */
// TODO: Make blocks massive
// TODO: Implement perpetual motion (for flags, clocks, ??).
// TODO: Allow custom sounds. Every type should have its own sound file. This file should describe the name of the
//       sounds in the resource pack and the length of the sound. It should contain both movement and finish sounds.
// TODO: Look into https://hub.spigotmc.org/javadocs/spigot/org/bukkit/Chunk.html#getChunkSnapshot to verify if a door
//       can be opened and while I'm at it, also to construct the FBlocks. This way, that stuff can all be done
//       async.
// TODO: Perhaps use a separate table for every type of door. That would allow more specialzed storage. More research
//       is required, though. https://stackoverflow.com/a/3579462


/*
 * Modules
 */
// TODO: Put Config-related stuff in BigDoorsUtil and don't use Bukkit stuff for reading it. Just give it a regular file.
//       Make the ConfigLoader abstract and extend a Spigot-specific config in SpigotUtil with Spigot-Specific options
//       such as resource packs.
// TODO: Every version submodule (e.g. spigot-v1_14_R1) should depend on spigot-core and be compiled as a separate jar.
//       Load these version-specific submodules from a folder on startup.

/*
 * Experimental
 */
// TODO: Look into allowing people to set a (estimated) max size in RAM for certain caches.
//       Example: https://www.javaworld.com/article/2074458/estimating-java-object-sizes-with-instrumentation.html
//       Simpler example: https://stackoverflow.com/questions/52353/in-java-what-is-the-best-way-to-determine-the-size-of-an-object#
//       Another: https://javamagic.blog/2018/07/11/how-to-find-size-of-java-object-in-memory-using-jol/
//       Also: https://www.baeldung.com/java-size-of-object
// TODO: Fix violation of LSP for doorAttributes. Instead of having a switch per type in the GUI, override a return in the DoorAttribute enum.
// TODO: Data is duplicated a lot! Currently, the falling block has the IBlockData, and so does PBlockData. If the block can rotate, it has it twice, even!
//       This is a huge waste of Ram. The falling block should be the only place to store the block data.
// TODO: Instead of killing the FBlock, rotating it and then respawning it, rotate the FBlockData and then send the
//       appropriate packets to the appropriate players. This can also be done async, so no more sync scheduling needed.
// TODO: Add command to upload error log to pastebin or something similar.
// TODO: Add config option to limit logging file size: https://kodejava.org/how-do-i-limit-the-size-of-log-file/
// TODO: Look into previously-blacklisted material. Torches, for example, work just fine in 1.13.
//       They just need to be removed first and placed last. So-called "greylisting"
// TODO: Figure out a way to use Interfaces or something to generate 2 separate builds: Premium and non-premium.
// TODO: Look into Aikar's command system to replace my command system: https://www.spigotmc.org/threads/acf-beta-annotation-command-framework.234266/
// TODO: Get rid of the DoorType enum. Instead, allow dynamic registration of door types.
// TODO: Stop naming all animatable objects "doors". An elevator is hardly a door.
// TODO: Special PBlockData subclass for every type of opener. This is a messy system.
// TODO: Use Spigot Premium's placeholders: https://www.spigotmc.org/wiki/premium-resource-placeholders-identifiers/
//       Then send the placeholder to my site on startup. Why? As evidence the buyer has, in fact, downloaded the plugin.
//       This could be useful in case of a PayPal chargeback.
// TODO: Implement admin command to show database statistics (player count, door count, owner count, etc).
// TODO: Consider storing original locations in the database. Then use the OpenDirection as the direction to go when in
//       the original position. Then you cannot make regular doors go in a full circle anymore.
// TODO: Instead of placing all blocks one by one and sending packets to the players about it, use this method instead:
//       https://www.spigotmc.org/threads/efficiently-change-large-area-of-blocks.262341/#post-2585360
// TODO: Write script (python? might be fun to switch it up) to build plugin.yml on compilation.
// TODO: Create a system that allows a set of default messages for creators (with placeholders for the types), but also
//       allows overriding for custom types. Then people who really want to, can write fully custom messages for every
//       type, but the messages system will be much cleaner by default.
// TODO: For storing player permissions, consider storing them in the database when a player leaves.
//       Then ONLY use those whenever that player is offline. Just use the online permissions otherwise.
// TODO: When initializing the plugin, initialize vital functions first (database, etc). Because some
//       things are intialized async (e.g. database upgrades), make sure to wait for everything on a
//       separate thread. If anything fails, make sure to try to unload everything properly.
//       Only keep something loaded to inform users about the issue and to handle command attempts
//       gracefully.

/*
 * Doors
 */
// TODO: getBlocksToMove() should return the number of blocks it'll move, regardless of if this value was set.
//       Internally, keep track of the specified and the default value, then return the specified value if possible,
//       otherwise the default value. Also distinguish between goal and actual.
// TODO: Create method in DoorBase for checking distance. Take Vector3D for distance and direction.
// TODO: Having both openDirection and rotateDirection is stupid. Check DoorBase#getNewLocations for example.
// TODO: Don't use Location for the locations. Use vectors instead.
// TODO: Cache value of DoorBase#getSimplePowerBlockChunkHash().
// TODO: Use the IGetNewLocation code to check new pos etc.
// TODO: Statically store GNL's in each door type.
// TODO: Add getCloseDirection() method. This is NOT!!! the opposite of the openDirection once the original coordinates
//       are stored in the database. It should be the direction back to the original position.
// TODO: Add DoorBase#isValidOpenDirection. Can be useful for creation and validation.
// TODO: Store calculated stuff such as blocksInDirection in object-scope variables, so they don't have to be
//       calculated more than once.
// TODO: Make interface of of DoorBase, so interfaces can provide default implementations (e.g. StationaryDoor,
//       AxisAligned?, PerpetualMovement?).
// TODO: Look into nested interfaces for AbstractDoorBase. It might be possible to make them protected that way.
// TODO: Implement this type: https://www.filt3rs.net/sites/default/files/study/_3VIS%20-%20318%20Fer-211%20visera%20proyectable%20visor%20fachada%20basculante.jpg
//       Perhaps simply allow the drawbridge to go "North" even when flat in northern direction, so it goes down.
// TODO: Allow players to change the autoCloseTimer to an autoOpenTimer or something like that.
// TODO: Allow redefining doors.
// TODO: Consider letting IPerpetualMoverArchetype implement IStationaryDoorArchetype. There shouldn't be any situation
//       where a perpetualMovement isn't also stationary.
// TODO: Allow 3D doors and drawbridges.

/*
 * General
 */
// TODO: Use reflection or something to hack Spigot's API-version to always use the currently-used API-version.
// TODO: Add a new type of powerblock that locks/unlocks doors instead of toggling them.
// TODO: Don't use the local maven files. Use this method instead: https://stackoverflow.com/a/4955695
// TODO: Load everything on startup (including RedstoneListener (should be singleton)). Use the IRestartable interface to handle restarts instead.
// TODO: Don't just play sound at the door's engine. Instead, play it more realistically (so not from a single point). Perhaps packets might help with this.
// TODO: Move AbortableTaskManager and PowerBlockRedstoneManagerSpigot to bigdoors-core.
// TODO: Implement TPS limit. Below a certain TPS, doors cannot be opened.
//       double tps = ((CraftServer) Bukkit.getServer()).getServer().recentTps[0]; // 3 values: last 1, 5, 15 mins.
// TODO: Don't overwrite the default language file (and don't make it read-only). Instead, load as many translations as possible and revert to the internal default for any missing ones. Dump some messages in the log.
// TODO: Move all non-database related stuff out of DatabaseManager.
// TODO: Rename region bypass permission to bigdoors.admin.bypass.region.
// TODO: ICustomEntityFallingBlock: Clean this damn class up!
// TODO: Look into restartables interface. Perhaps it's a good idea to split restart() into stop() and init().
//       This way, it can call all init()s in BigDoors::onEnable and all stop()s in BigDoors::onDisable.
// TODO: Make GlowingBlockSpawner restartable. Upon restarting the plugin, all glowing blocks have to be removed.
// TODO: Store PBlockFace in rotateDirection so I don't have to cast it via strings. ewww.
//       Alternatively, merge PBlockFace and RotateDirection into Direction.
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
// TODO: Remove blockMovers from BigDoors-core.
// TODO: Make sure to keep the config file's maxDoorCount in mind. Or just remove it.
// TODO: Fix (big) mushroom blocks changing color.
// TODO: Documentation: Instead of "Get the result", use "Gets the result" and similar.
// TODO: Create abstraction layer for config stuff. Just wrap Bukkit's config stuff for the Spigot implementation (for now).
// TODO: Get rid of all calls to SpigotUtil for messaging players. They should all go via the proper interface for that.
// TODO: Logging, instead of "onlyLogExceptions", properly use logging levels. Also implement a
//       MINIMALISTIC logging level. On this level, only the names + messages of exceptions are written
//       to the console. Make this the default setting.
// TODO: Every Manager must be a singleton.
// TODO: Add door creation event (or perhaps door modification event?).
// TODO: Use the following snippet for all singletons, not just the ones in bigdoors-core. This will require the use of
//       "com.google.common.base.Preconditions" (so import that via Maven).
/*
Preconditions.checkState(instance != null, "Instance has not yet been initialized. Be sure #init() has been invoked");
 */
// TODO: Get rid of all occurrences of ".orElse(new ArrayList<>())". Use a default, unmodifiable list instead. See
//       PowerBlockManager#EMPTYDOORSLIST for example.
// TODO: Do permissions checking for bypasses etc (compats) fully async (so not with an ugly .get()).
// TODO: Create proper (abstract) factory for the event system.
// TODO: Use the messaging interface to send messages to players.
// TODO: Make sure all entities are named "BigDoorsEntity".
// TODO: Write a method that can loop over 3 3d vectors (min, max x+y+z) and takes a supplier/consumer to execute for
//       every entry.
// TODO: Cache IPWorldFactory#getCurrentToggleDir(). It'd be easiest if all doors used the same formula.
// TODO: Use YAML for messages system.
// TODO: Use generic translation messages for door creators etc and allow overriding these messages for specific types.
//       Figure this stuff out while reading the messages file, so there's 0 impact while the plugin is running.
// TODO: Make some kind of interface for the vectors, to avoid code duplication.
// TODO: Add default pitch and volume to PSound enum. Allow overriding them, though. Perhaps also store tick length?
// TODO: Add some kind of method to reset the timer on falling blocks, so they don't despawn (for perpetual movers).
// TODO: Split DoorActionEvent into 2: one for future doors, the other for existing doors.
// TODO: Merge spigot-core and spigot-util. It's just annoying and messy.
// TODO: Allow the "server" to own doors.
// TODO: Add material blacklist to the config.
// TODO: Add option to config to set the max number of doors per power block.
// TODO: Consider only using a ConcurrentHashMap for the TimedCache, so it can loop over it async.
// TODO: Make some kind of MessageRecipient interface. Much cleaner than sending an "Object" to sendMessageToTarget.
//       Just let IPPlayer extend it for players.
// TODO: Send out event after toggling a door.

/*
 * GUI
 */
// TODO: Look into using player heads for GUI buttons. Example: https://minecraft-heads.com/player-heads/alphabet/2762-arrow-left
// TODO: Make GUI options always use the correct subCommand.
// TODO: Create ItemManager that stores repeatedly used items (such as door creation books and doorInfo stuff).
// TODO: Store 2 player objects: 1) Subject (the owner of all the doors), and 2) InventoryHolder (who is looking at the inventory).
// TODO: Update items in inventory instead of opening a completely new inventory. No longer requires dirty code to check is it's refreshing etc. Bweugh.
// TODO: Use some NMS stuff to change the name of a GUI without reopening it:
//       https://www.spigotmc.org/threads/how-to-set-the-title-of-an-open-inventory-itemgui.95572/#post-1049250
// TODO: Use a less stupid way to check for interaction: https://www.spigotmc.org/threads/quick-tip-how-to-check-if-a-player-is-interacting-with-your-custom-gui.225871/
// TODO: Once (if?) door pooling is implemented, use Observers to update doors in the GUI when needed.
// TODO: Move rotation cycling away from GUI and into the Door class.
// TODO: Put all GUI buttons and whatnot in try/catch blocks.
// TODO: Documentation.
// TODO: Look into refresh being called too often. Noticed this in GUIPageRemoveOwner (it tries to get a head twice).
// TODO: Store Message in GUI pages. Then use that to check if the player is in a custom GUI page.
// TODO: Make sure some data is always available, like a list of door owners of a door. The individual page shouldn't
//       contain logic like obtaining a list of owners.
// TODO: Create dedicated GUI button classes. This is too messy.

/*
 * SQL
 */
// TODO: Store original coordinates in the database. These can be used to find the actual close direction.
// TODO: Create new table for DoorTypes: {ID (AI) | PLUGIN | TYPENAME}, with UNIQUE(PLUGIN, TYPENAME).
//       Then use FK from doors to doortypes. Useful for allowing custom door types.
// TODO: Store engineChunkHash in the database, so stuff can be done when the chunk of a door is loaded. Also make sure
//       to move the engine location for movable doors (sliding, etc).
// TODO: Look into creating a separate table for worlds and put an FK to them in the doors table. This should save a bit
//       of space, but more importantly, it makes it easier to implement worlds that do not have UUIDs (e.g. Forge).
// TODO: Add a "folder" column to the sqlUnion table. This would allow users to organize doors into different folders.
// TODO: Store UUIDs as 16 byte binary blobs to save space: https://stackoverflow.com/a/17278095
// TODO: Move database upgrades out of the main SQL class. Perhaps create some kind of upgrade routine interface. 

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
// TODO: Store actual minArgCount in subCommands so it doesn't have to be calculated constantly.
// TODO: Make sure there are no commands that use hard coded argument positions.
// TODO: NPE thrown when trying to use direct command after initiating a commandWaiter a while ago (should've been cancelled already!).
//       Might be related to the issue listed above (regarding setBlocksToMove commandWaiter).
// TODO: Make sure super command can be chained.
// TODO: Fix bigdoors doorinfo in console.
// TODO: SetBlocksToMove: Waiter is cancelled both by the subCommand and the waiter. Make sure all commandWaiters are disabled in the waiter.
// TODO: CommandWaiters should register themselves in the CommandManager class. No outside class should even know about
//       this stuff. Especially not the fucking DatabaseManager.
// TODO: SubCommandSetRotation should be updated/removed, as it doesn't work properly with types that do not go (counter)clockwise.
// TODO: Let users verify door price (if more than 0) if they're buying a door.
// TODO: Add an isWaiter() method to the commands. If true, it should catch those calls before calling the command implementations.
//       Gets rid of some code duplication.
// TODO: Check if minArgCount is used properly.
// TODO: Make "/BigDoors new" require the type as flag input. No more defaulting to regular doors.
// TODO: Fix "/BigDoors filldoor db4" not working.
// TODO: Make sure you cannot use direct commands (i.e. /setPowerBlockLoc 12) of doors not owned by the one using the command.
// TODO: For all commands that support either players or door names etc, just use flags instead of the current mess.
// TODO: Door deletion confirmation message.
// TODO: Allow "/BigDoors new -PC -p pim16aap2 testDoor", "/BigDoors menu -p pim16aap2", etc. So basically allow doing
//       stuff in someone else's name.
// TODO: Instead of using an enum, consider using annotations instead. It can also include stuff like PlayerOnly etc.

/*
 * Creators
 */
// TODO: Make users explicitly specify the openDirection on door creation.
// TODO: GarageDoorCreator: Fix having to double click last block.
// TODO: GarageDoorCreator: Before defaulting to North/East, check if those directions are actually available.

/*
 * Openers / Movers
 */
// TODO: Get rid of the weird speed multipliers in the CustomEntityFallingBlock_VX_XX_RX classes.
// TODO: Make the getOpenDirection function of the openers static, so the Creators can check which direction to pick.
//       Then set the direction in the creator.
// TODO: Rotate Sea Pickle and turtle egg.
// TODO: Replace current time/speed/tickRate system. It's a mess.
// TODO: Get rid of all material related stuff in these classes. isAllowedBlock should be abstracted away.
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
// TODO: Make sure the new types don't just open instantly without a provided time parameter.
// TODO: Rename variables in updateCoords to avoid confusion. Perhaps a complete removal altogether would be nice as well.
// TODO: Get rid of the GNL interface etc. The movers class can handle it on its own using Function interface.
// TODO: Move getBlocksMoved() to Mover.
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
// TODO: Instead of having methods to open/close/toggle animated objects, have a single method that receives
//       a redstone value or something. Then each animated object can determine how to handle it on its own.
//       Open/close/toggle for doors, activate/deactivate for persistent movement (flags, clocks, etc).
// TODO: SlidingDoor, Portcullis: Cache BlocksToMove in a Vec2D. Invalidate when coors and stuff change.
// TODO: Update NS variables in the movers so they don't mean "active along north/south axis" but rather
//       "engine aligned with north/south axis", which effectively means the opposite. Also, obtain the variable from
//       the door.
// TODO: Potentially implement BlockMover#animateEntities in BlockMover. Then use function pointers to calculate everything.
// TODO: Highlight all blocking blocks if BlocksToMove is set.
// TODO: When a block is "blocking" a door from being opened, check if there isn't a corresponding gap in the door to be opened.
// TODO: Reduce code duplication in the blockmovers (specifically animateEntities).
// TODO: Make RevolvingDoorMover and CylindricalMover more closely related.
// TODO: Make sure all types respect their multiplier.
// TODO: Get rid of the stupid .101 multiplier for vectors. Use proper normalization and shit instead.
//       Look at this: https://github.com/InventivetalentDev/AdvancedSlabs/blob/ad2932d5293fa913b9a0670a0bc8ea52f1e27e0d/Plugin/src/main/java/org.inventivetalent.advancedslabs/movement/path/types/CircularSwitchController.java#L85
// TODO: Properly keep track of who opened a door, instead of just passing along the owner's UUID.
// TODO: Add PerpetualMover BlockMover. Then use an interface for the functions, which the individual movers can set.
//       Also have a rotation method.
// TODO: Don't use locations for GNL's, just use positions instead (cross-world stuff isn't possible anyway).
// TODO: Limit the number of doors that can be active in a world at any given time. Maybe also limit the number per chunk.
//       Figure out a way to deal with redstone activation. Perhaps try again after a certain amount of time?
// TODO: Rotate blocks for garage doors, wind mills, clocks, and revolving doors.
// TODO: When trying to activate a door in an unloaded chunk, load the chunk and instantly toggle the door (skip the animation).
//       Extension: Add config option to send an error message to the player instead (so abort activation altogether).
// TODO: Variable door depth.
// TODO: Add a doortype that allows sliding doors to go diagonally. Might be possible to modify sliding doors as well.
// TODO: New door type: Diagonal doors.
// TODO: New door type: Folding doors.
// TODO: Magic carpets? Flat flags.

/*



 */

/*
 * Manual Testing
 */
// TODO: Make sure no unnecessary database calls are made. Log them in the construct method of the PPreparedStatement class.
// TODO: Test new creators: Windmill, RevolvingDoor, GarageDoor. Make sure it cannot be fucked up.
// TODO: Test new chunkInRange methods. Especially sliding door.
// TODO: Make sure that new lines in the messages work (check Util::stringFromArray).
// TODO: Fix no permission to set AutoCloseTime from GUI.
// TODO: Check if TimedCache#containsValue() works properly.
// TODO: What happens when a player is given a creator stick while their inventory is full?
// TODO: Test all methods in the database manager stuff.
// TODO: Fix command waiter system.
// TODO: Fix not being able to use doorUID in setBlocksToMove (direct).
// TODO: Fix start message of creator appearing twice as well as receiving 2 creator sticks (at least for flag and sliding door).
// TODO: Verify the following types work: (Wall)Signs, (Wall)Banners, plants (potatoes, carrots, etc), redstone stuff (wire, conduit, repeater, etc),
//       Rails, coral, beds, carpet, dragon egg, concrete powders, pressure plates, buttons, levers, saplings,
//       Structure blocks, bubble column, (wall)torches, enderchest, (shulkerbox?).

/*
 * Unit tests
 */
// TODO: https://bukkit.org/threads/how-to-unit-test-your-plugin-with-example-project.23569/
// TODO: https://www.spigotmc.org/threads/using-junit-to-test-plugins.71420/#post-789671
// TODO: https://github.com/seeseemelk/MockBukkit
// TODO: Make sure to test database upgrade to v11. Make this future-proof somehow. Perhaps store the old v10 creation
//       stuff somewhere.


/**
 * Represents the core class of BigDoors.
 *
 * @author Pim
 */
public final class BigDoors
{
    @NotNull
    private static final BigDoors instance = new BigDoors();

    @Nullable
    private IMessagingInterface messagingInterface = null;

    /**
     * The platform to use. e.g. "Spigot".
     */
    private IBigDoorsPlatform platform;

    private BigDoors()
    {
    }

    /**
     * Gets the instance of this class.
     *
     * @return The instance of this class.
     */
    @NotNull
    public static BigDoors get()
    {
        return instance;
    }

    /**
     * Sets the platform implementing BigDoor's internal API.
     *
     * @param platform The platform implementing BigDoor's internal API.
     */
    public void setBigDoorsPlatform(final @NotNull IBigDoorsPlatform platform)
    {
        this.platform = platform;
    }

    /**
     * gets the platform implementing BigDoor's internal API.
     *
     * @return The platform implementing BigDoor's internal API.
     */
    public IBigDoorsPlatform getPlatform()
    {
        return platform;
    }

    /**
     * Gets the {@link DoorManager} instance.
     *
     * @return The {@link DoorManager} instance.
     */
    @NotNull
    public DoorManager getDoorManager()
    {
        return DoorManager.get();
    }

    /**
     * Gets the {@link AutoCloseScheduler} instance.
     *
     * @return The {@link AutoCloseScheduler} instance.
     */
    @NotNull
    public AutoCloseScheduler getAutoCloseScheduler()
    {
        return AutoCloseScheduler.get();
    }

    /**
     * Gets the {@link PowerBlockManager} instance.
     *
     * @return The {@link PowerBlockManager} instance.
     */
    @NotNull
    public PowerBlockManager getPowerBlockManager()
    {
        return PowerBlockManager.get();
    }

    /**
     * Gets the currently used {@link IMessagingInterface}. If the current one isn't set, {@link
     * IBigDoorsPlatform#getMessagingInterface} is used instead.
     *
     * @return The currently used {@link IMessagingInterface}.
     */
    @NotNull
    public IMessagingInterface getMessagingInterface()
    {
        if (messagingInterface == null)
            return getPlatform().getMessagingInterface();
        return messagingInterface;
    }

    public void setMessagingInterface(final @Nullable IMessagingInterface messagingInterface)
    {
        this.messagingInterface = messagingInterface;
    }


    /**
     * Gets the {@link DatabaseManager} instance.
     *
     * @return The {@link DatabaseManager} instance.
     */
    @NotNull
    public DatabaseManager getDatabaseManager()
    {
        return DatabaseManager.get();
    }
}
