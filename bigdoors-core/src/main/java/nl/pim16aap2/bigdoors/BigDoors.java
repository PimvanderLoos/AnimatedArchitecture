package nl.pim16aap2.bigdoors;

import nl.pim16aap2.bigdoors.api.IBigDoorsPlatform;
import nl.pim16aap2.bigdoors.api.IMessagingInterface;
import nl.pim16aap2.bigdoors.api.IRestartable;
import nl.pim16aap2.bigdoors.api.IRestartableHolder;
import nl.pim16aap2.bigdoors.managers.AutoCloseScheduler;
import nl.pim16aap2.bigdoors.managers.DatabaseManager;
import nl.pim16aap2.bigdoors.managers.DoorActivityManager;
import nl.pim16aap2.bigdoors.managers.PowerBlockManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;

/*
 * Experimental
 */
// TODO: Do not define the version of a DoorType in its class. Instead, use the version found in the manifest.
// TODO: Consider storing boolean type-specific entries in single integers as much as possible. Let the db or some
//       service handle the automatic packing/unpacking, so the door classes won't even notice it.
// TODO: Store a general lastOp in the database to keep track of the last action of a door (e.g. Toggle North), useful
//       To help figure out what to do when its data was changed.
// TODO: Consider storing a serialized version of a door before removing its blocks and removing it before placing the blocks.
//       This would make sure that blocks aren't lost in case of a server crash, which will be more important to avoid
//       with the new perpetually moving objects. Just make sure to do it async and it should be fine. Also, it should
//       be a configurable setting (e.g. to minimize disk writes). Maybe even have different levels, so you could use this
//       system only for perpetually moving objects.
// TODO: Consider being more strict in the data types used for the door types system. If the type is actually correctly
//       defined, it's possible to cast it, right?
// TODO: Look into allowing people to set a (estimated) max size in RAM for certain caches.
//       Example: https://www.javaworld.com/article/2074458/estimating-java-object-sizes-with-instrumentation.html
//       Simpler example: https://stackoverflow.com/questions/52353/in-java-what-is-the-best-way-to-determine-the-size-of-an-object#
//       Another: https://javamagic.blog/2018/07/11/how-to-find-size-of-java-object-in-memory-using-jol/
//       Also: https://www.baeldung.com/java-size-of-object
// TODO: Fix violation of LSP for doorAttributes. Instead of having a switch per type in the GUI, override a return in the DoorAttribute enum.
// TODO: Look into Aikar's command system to replace my command system: https://www.spigotmc.org/threads/acf-beta-annotation-command-framework.234266/
// TODO: Instead of placing all blocks one by one and sending packets to the players about it, use this method instead:
//       https://www.spigotmc.org/threads/efficiently-change-large-area-of-blocks.262341/#post-2585360
// TODO: Write script (python? might be fun to switch it up) to build plugin.yml on compilation.
// TODO: For storing player permissions, consider storing them in the database when a player leaves.
//       Then ONLY use those whenever that player is offline. Just use the online permissions otherwise.
//       Also include if they're an OP or not. All this can just be handled via a bitflag.
// TODO: When initializing the plugin, initialize vital functions first (database, etc). Because some
//       things are intialized async (e.g. database upgrades), make sure to wait for everything on a
//       separate thread. If anything fails, make sure to try to unload everything properly.
//       Only keep something loaded to inform users about the issue and to handle command attempts
//       gracefully.
// TODO: Consider breaking utilities such as the GlowingBlockSpawner and the Falling Blocks up into
//       separate repositories. Perhaps other people would like to use them as well? Or at least the GlowingBlockSpawner.
//       The falling blocks might be a little too specialized.
// TODO: Consider adding linked doors that will toggle upon activation of any of the existing doors.
//       Need to figure out how to deal with the new powerblock system, though. Perhaps let the doors
//       share a powerblock? Might be tricky to do in an efficient manner.
// TODO: Consider switching to tight instance-control for doors. This would mean that each door can have at most
//       1 instance. All setters would have to be synchronized or disabled. Perhaps use a manager with weak references.
//       This would have the advantage that you cannot have out-of-date versions of a door, (for example in a GUI).
//       There are some issues regarding what should and should not be loaded, though. For example, should ALL owners
//       be loaded into a door at all times? Or should there be a separate system to retrieve that data?
//       Also, when mass-selecting doors (e.g. all doors part of a certain group), how would that work with the cache?
//       Ideally, it wouldn't have to construct all those doors when retrieving it, but the database has no reason to
//       know about the door manager's existence. Perhaps _all_ door creation should then be routed via a factory of
//       some kind? Only this factory should be allowed to create new doors, so everything would have to be routed
//       through it regardless. The factory can then check if the door already has an instance. What to do if the instance
//       is different from the data in the db, though? Should that (out-of-sync instances) even be possible? If not,
//       when should they be updated? And how?
//       Design Pattern: https://en.wikipedia.org/wiki/Multiton_pattern
// TODO: Consider using some kind of component system in the doors. You'd have to add something like
//       ComponentBoolean("NS", Clock::getNS); A separate ComponentManager (initialized statically per-type)
//       should then handle the creation of the objects array as well as the parsing of it. The parsing should happens
//       just via the constructor (use reflection or something to track down the right constructor).
//       This would make door definition much more friendly.
//       The database might be able to use the components as well. Perhaps each component should have an ID?
//       Then it could just store "PreparedStatement::setString" (for example). Or they all just use setObject, also fine.

/*
 * Doors
 */
// TODO: Flags: Add a secondary mode where it doesn't use a hardcoded formula, but a user-defined one instead.
//       Use a JSON file and allow the user to define a set of formulas and store the results in variables.
//       There should be 3 types of formulas:
//       1) Per-Door:  Calculated once, then reused for every run .
//       2) Per-Run:   Calculated once per run (tick), then reused for every block.
//       3) Per-Block: Calculated for every block.
// TODO: Make a separate config file (file.zip?, so people who don't understand it won't touch it) for advanced options
//       including, but not limited to, setting cache sizes.
// TODO: Make doors thread-safe: https://stackoverflow.com/questions/53769141/making-a-pojo-thread-safe
//       https://stackoverflow.com/questions/14648627/java-synchronization-lock-without-blocking
//       https://stackoverflow.com/questions/10548066/multiple-object-locks-in-java
//       https://howtodoinjava.com/java/multi-threading/object-vs-class-level-locking/
//       https://stackoverflow.com/questions/34611106/synchronized-vs-readwritelock-performance
//       https://winterbe.com/posts/2015/04/30/java8-concurrency-tutorial-synchronized-locks-examples/
// TODO: Consider creating optional per-type configs.
// TODO: When changing the open direction of a GarageDoor, the engine location needs to be updated as well, otherwise
//       it'll just break. Alternatively, consider ignoring the engine location altogether and just figuring it out
//       when it needs to be toggled? When toggling it, the engine location can just be derived from the open direction
//       and the current position without any guess work being involved.
// TODO: Think about how lenient the Drawbridge should be. Should it allow setting any of the 4 possible rotations,
//       or should it restrict itself to only the 2 that are possible.
// TODO: getBlocksToMove() should return the number of blocks it'll move, regardless of if this value was set.
//       Internally, keep track of the specified and the default value, then return the specified value if possible,
//       otherwise the default value. Also distinguish between goal and actual.
// TODO: Create method in DoorBase for checking distance. Take Vector3D for distance and direction.
// TODO: Having both openDirection and rotateDirection is stupid. Check DoorBase#getNewLocations for example.
// TODO: Cache value of DoorBase#getSimplePowerBlockChunkHash().
// TODO: Use the IGetNewLocation code to check new pos etc.
// TODO: Store calculated stuff such as blocksInDirection in object-scope variables, so they don't have to be
//       calculated more than once.
// TODO: Implement this type: https://www.filt3rs.net/sites/default/files/study/_3VIS%20-%20318%20Fer-211%20visera%20proyectable%20visor%20fachada%20basculante.jpg
//       Perhaps simply allow the drawbridge to go "North" even when flat in northern direction, so it goes down.
// TODO: Allow players to change the autoCloseTimer to an autoOpenTimer or something like that.
// TODO: Allow redefining doors.
// TODO: Consider letting IPerpetualMoverArchetype implement IStationaryDoorArchetype. There shouldn't be any situation
//       where a perpetualMovement isn't also stationary.
// TODO: Allow 3D doors and drawbridges.
// TODO: Allow excluding specific block locations for toggling doors. These locations should not be
//       included in any animation, even though they are part of a door area. This also requires the
//       feature where doors check for gaps in their old position to match any existing blocks in the
//       new position. Just use a creator or something to allow excluding/including blocks using
//       left/right click (include only works for previously excluded blocks). Then use either
//       guardian beams or glowing blocks for the edges of the door, and red glowing blocks to show
//       which blocks have been excluded so far.
//       Store them as a list or something in the database (in the top level table).

/*
 * General
 */
// TODO: See if Guava's LoadingCache might be useful: https://guava.dev/releases/18.0/api/docs/com/google/common/cache/LoadingCache.html
//       "Values are automatically loaded by the cache, and are stored in the cache until either evicted or manually invalidated."
// TODO: There are some instances where it is just assumed something will never be null, even though it's clearly
//       @Nullable. E.g. when retrieving the SpigotPlatform. This should be handled better. Either use Optionals,
//       or add 'throws NotInstantiatedException' or something to those methods. Then just propagate the exceptions.
//       The same goes for getting the BigDoorsPlatform, which is actually not entirely unlikely to happen, as some
//       plugins could try to get the BigDoorsSpigot instance before it has been loaded.
//       For example, any plugin that requires the platform or whatever might just not call getPlatform whenever it
//       needs, but only on init, after it has been guaranteed to be available. Then just re-call it on restart?
//       It can already occur that the plugin will fail to start because of this:
//       org.bukkit.plugin.InvalidPluginException: java.lang.NullPointerException: Cannot invoke "nl.pim16aap2.bigdoors.api.IBigDoorsPlatform.getMessagingInterface()" because the return value of "nl.pim16aap2.bigdoors.BigDoors.getPlatform()" is null
//       For stuff like the messaging interface, it might be nice to provide a default implementation in case no
//       patform exists. For the messaging interface, perhaps use stdout? Or /dev/null.
//       Either way, this needs to be resolved!!!!
// TODO: Consistency in word usage: Unregistered = not currently registered, Deregister = go to the state of being
//       unregistered.
// TODO: Use variables for the names of doors in the creator messages. This would also make it possible to make various
//       messages much more generic. Many of them only differ in the hardcoded name of the type.
// TODO: Expand the script that generates the testing translation file a bit.
//       It should copy the latest real file to its own directory if the testing file doesn't exist (it shouldn't be
//       in the repo). If the file does exist, quickly check the hashes of both to see if the copied file is still
//       up-to-date. If it isn't up-to-date, copy over the latest file.
//       If the file had to be copied over for whatever reason, regenerate the test translations file using the existing
//       script. Also, make this all part of the test step in Maven, so the plugin can be compiled on Windows by skipping
//       the test step.
// TODO: Per-door speed multiplier.
// TODO: Replace the chunk loading system with v1's new system.
// TODO: Move implementations for tests and stuff to its own module.
// TODO: Place the creator stick in the player's hotbar. If possible, in the currently selected place.
//       If there is no space in the hotbar (or maybe always do this), move the currently selected item into the
//       player's inventory. If there is no space, abort the process and inform the player that they need to sort
//       their shit out and do some inventory management.
// TODO: Implement a way to retrieve the original locations in the database. Then use the OpenDirection as the
//       direction to go when in the original position. Then you cannot make regular doors go in a full circle anymore.
//       Perhaps this could just be a single value in the database that describes the original min value (they can't
//       change shape, after all). How this hash is implemented can be determined on a per-type basis. A portcullis
//       can just use its lowest y-value as the x/z values do not matter at all.
//       However, it might also be nice to have a reversible way to retrieve the original location, so it's easier to
//       determine how to toggle a door back to its original position. Be careful to not make a very complicated
//       system to avoid storing a few more ints in the database; it's not worth it.
// TODO: Make a decision about using null in parameters. You could make a point that instead of value vs null, you can
//       value vs empty Optional vs null. However, Optionals make it more clear when an object might not be present
//       and they shouldn't be null anyway. On the other hand, the biggest reason I want it right now is because
//       the initiator type is incomplete. It is Optional because it's either a player, or not a player.
//       However, an Initiator class will be constructed in the future, using abstraction for stuff like permission
//       checking and message parsing, so that won't ever be null or Optional anyway. Maybe it's best to conform
//       to the standards for my CV?
// TODO: Allow directly opening the GUI for a door.
// TODO: Make sure that portals don't break when set. Also, they should rotate.
// TODO: Try to make sure that items like stairs maintain their connection state (so corner stairs move like corner stairs).
// TODO: Use "synchronized" to avoid timing/scheduling issues.
// TODO: InterruptedException should not be caught, it should be rethrown. Either avoid it altogether, or handle it
//       properly.
// TODO: DoorTypes currently need to be registered before BigDoors is initialized, so that they are put in the config.
//       However, registering DoorTypes requires the DatabaseManager to exist, but it doesn't until halfway through
//       BigDoor's initialization.
//       Do not remove any invalid names from the list in the config, but store them in a map instead. Then match from
//       that map when a new type is registered. If that value didn't exist yet, rewrite the config and add the value.
//       When a type is unregistered, either remove them from the list and use the old value for the new one, or leave
//       them there to avoid destroying user data.
// TODO: Look into overriding Equals() properly for all the subtypes of the door.
// TODO: Make sure there aren't any errors on startup if the plugin folder doesn't exist.
// TODO: Create a system that allows external plugins to register custom tick stuff for the falling blocks. For every tick,
//       all the registered custom tick methods will be executed. Either give the custom ticker the falling block itself,
//       or just position (old and new)/velocity/whatever else is needed. Additionally, maybe add a kinda of async ticker
//       as well, which will be executed by the mover after processing each animated block. Might need a better name than
//       "async", though, to indicate it's actually executed by the mover. Perhaps they should register a builder for the
//       ticker so that they can store custom per-entity data.
// TODO: Instead of bitflags, consider using EnumSets. Much cleaner.
//       https://docs.oracle.com/javase/7/docs/api/java/util/EnumSet.html
// TODO: Restrict setters where appropriate.
// TODO: Don't use 'final' in function parameters. It doesn't do anything (fuck you, Java).
// TODO: Everything that should be accessible by other plugins should be initialized in the constructor, not in onEnable.
//       This will ensure it's initialized in time, as Spigot doesn't start enabling plugins until they're all constructed.
// TODO: Look into Java's ResourceBundle and MessageFormat classes for localisation handling.
//       More info: https://docs.oracle.com/javase/7/docs/api/java/util/ResourceBundle.html
//                  https://docs.oracle.com/javase/7/docs/api/java/text/MessageFormat.html
// TODO: Add more basic options to the messages system. For example:
//       - Add different strings for singular and plural objects.
//       - Add different versions of the same word for different circumstances ("the door" vs "door").
//       Especially useful in languages that use genders (f.i. German).
// TODO: Use static unmodifiable lists as "null object". Return these from functions that can return empty lists.
//       This should also replace Optionals. Also, look into Collections#emptyList() (it doesn't create new objects).
//       Note that this list will be unmodifiable. If the list might be modified later on, use "new Arraylist(0)". This
//       won't initialize anything either (minimizing overhead), but it will be modifiable. This might actually be
//       preferable, since it won't mix modifiable (when not empty) and unmodifiable (when empty), which is confusing.
// TODO: Look into this: https://www.jetbrains.com/help/idea/parametersarenonnullbydefault-annotation.html
// TODO: Look into overriding Equals() properly for all the subtypes of the door.
// TODO: Make sure there aren't any errors on startup if the plugin folder doesn't exist.
// TODO: Use reflection or something to hack Spigot's API-version to always use the currently-used API-version.
// TODO: Add a new type of powerblock that locks/unlocks doors instead of toggling them.
// TODO: Handle restartable interface options in DatabaseManager class.
// TODO: Don't use the local maven files. Use this method instead: https://stackoverflow.com/a/4955695
// TODO: Don't just play sound at the door's engine. Instead, play it more realistically (so not from a single point).
//       If you look at CustomEntityFallingBlock_V1_15_R1, you can see how to get a list of all players that can 'see'
//       a given entity. Consider using one or more entities (center, 8 corners?) to determine which players to
//       actually send the audio to.
// TODO: Move AbortableTaskManager and PowerBlockRedstoneManagerSpigot to bigdoors-core.
// TODO: Implement TPS limit. Below a certain TPS, doors cannot be opened.
//       double tps = ((CraftServer) Bukkit.getServer()).getServer().recentTps[0]; // 3 values: last 1, 5, 15 mins.
// TODO: Move all non-database related stuff out of DatabaseManager.
// TODO: Rename region bypass permission to bigdoors.admin.bypass.region.
// TODO: ICustomEntityFallingBlock: Clean this damn class up!
// TODO: Look into restartables interface. Perhaps it's a good idea to split restart() into stop() and init().
//       This way, it can call all init()s in BigDoors::onEnable and all stop()s in BigDoors::onDisable.
//       Then just load everything on initial construction (including RedstoneListener (should be singleton)).
// TODO: Perhaps let IGlowingBlock implement IAnimatedBlock (or whatever it'll be called, but I mean the same one that's
//       used for the current animated blocks). Then it can toggle doors as preview.
// TODO: Store PBlockFace in rotateDirection so I don't have to cast it via strings. ewww.
//       Alternatively, merge PBlockFace and RotateDirection into Direction.
// TODO: Get rid of all occurrences of "boolean onDisable". Just do it via the main class.
// TODO: Get rid of ugly 1.14 hack for checking for forceloaded chunks.
// TODO: Allow wand material selection in config.
// TODO: Get rid of code duplication in ProtectionCompatManager.
// TODO: Force users to register permissions for stuff like the number / max size of doors in the config or something.
//       This would make it much more efficient to check the max number/size of doors for a user in most cases.
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
// TODO: Make sure to keep the config file's maxDoorCount in mind. Or just remove it.
// TODO: Fix (big) mushroom blocks changing color.
// TODO: Documentation: Instead of "Get the result", use "Gets the result" and similar.
// TODO: Create abstraction layer for config stuff. Just wrap Bukkit's config stuff for the Spigot implementation (for now).
// TODO: Get rid of all calls to SpigotUtil for messaging players. They should all go via the proper interface for that.
// TODO: Every Manager must be a singleton.
// TODO: Add door creation event (or perhaps door modification event?).
// TODO: Use the following snippet for all singletons, not just the ones in bigdoors-core. This will require the use of
//       "com.google.common.base.Preconditions" (so import that via Maven).
/*
Preconditions.checkState(instance != null, "Instance has not yet been initialized. Be sure #init() has been invoked");
 */
// TODO: Do permissions checking for bypasses etc (compats) fully async (so not with an ugly .get()).
// TODO: Create proper (abstract) factory for the event system.
// TODO: Use the messaging interface to send messages to players.
// TODO: Make sure all entities are named "BigDoorsEntity".
// TODO: Use YAML for messages system.
// TODO: Use generic translation messages for door creators etc and allow overriding these messages for specific types.
//       Figure this stuff out while reading the messages file, so there's 0 impact while the plugin is running.
// TODO: Make some kind of interface for the vectors, to avoid code duplication.
// TODO: Add default pitch and volume to PSound enum. Allow overriding them, though. Perhaps also store tick length?
//       Perhaps just don't use an enum at all, but store the sound in the DoorType definition.
// TODO: Add some kind of method to reset the timer on falling blocks, so they don't despawn (for perpetual movers).
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
// TODO: Make GUI options always use the correct subCommand.
// TODO: Update items in inventory instead of opening a completely new inventory.
//       No longer requires dirty code to check is it's refreshing etc. Bweugh.
// TODO: Use some NMS stuff to change the name of a GUI without reopening it:
//       https://www.spigotmc.org/threads/how-to-set-the-title-of-an-open-inventory-itemgui.95572/#post-1049250
// TODO: Use a less stupid way to check for interaction:
//       https://www.spigotmc.org/threads/quick-tip-how-to-check-if-a-player-is-interacting-with-your-custom-gui.225871/
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
// TODO: Allow renaming doors.
// TODO: Allow transfer of door ownership.
// TODO: Make sure you can remove yourself as (co)owner.
// TODO: (not SQL-related), make isLocked part of DoorData.
// TODO: Make sure that trying to use unregistered doortypes is handled gracefully.
//       This includes: Toggling, Creating, Commands, and GUI.
// TODO: Use batch statements to reduce the number of transactions for door inserts. More info:
//       https://stackoverflow.com/questions/9601030/transaction-in-java-sqlite3
//       Some more info about general optimizations:
//       https://www.whoishostingthis.com/compare/sqlite/optimize/
//       Or maybe insert into views using triggers?
// TODO: Look into triggers to potentially improve stuff.
// TODO: Allow storing lists/arrays. Perhaps do this dynamically via creating yet more dynamic tables? Sorry, future me!
// TODO: Store the locked variable as a flag. Also (not SQL-related), make it part of DoorData.
// TODO: Implement a doorUpdate method that updates both the doorBase and the door-specific data.
// TODO: Implement multiple powerblocks per door.
// TODO: Not using any additional data is completely fine! Right now the DoorType system assumes a strict minimum of 1
//       additional data value.
// TODO: Look into this stuff:
//       https://www.sqlite.org/lang_analyze.html
//       https://docs.oracle.com/javase/7/docs/api/java/sql/Connection.html#setReadOnly%28boolean%29
// TODO: Allow deleting door types.
// TODO: Allow retrieving all doors from a certain type.
// TODO: Allow updating a DoorType. This should not change the UID of the door. In the DoorBase table, only the column
//       "doorType" should be modified. All entries in the typeSpecific table should be passed to some kind of update
//       function that takes the array of Objects and returns a new array of Objects that should be put in the new
//       type-specific table. All this should be done off the main thread and the database should be locked until it's done.
// TODO: Be consistent in UUID usage. Either use Strings everywhere or UUIDs everywhere, not the current mix.
// TODO: Add more overloaded methods for different types to make stuff easier to use. For example, if a method expexts
//       a Player's UUID, don't just have a method for the IPPlayer, but also for the UUID. Same for worlds, etc.
// TODO: When registering DoorTypes, make sure to take into account that the database might be upgrading itself on
//       another thread. If this is the case, wait until the upgrades have finished before registering them.
//       Just return a CompletableFuture<Boolean> instead of a boolean from the register method.
// TODO: Cache functions in PPreparedStatement.
// TODO: Do not allow modifying properties of a door while it is busy.
// TODO: Consider creating groups of users, which would make it easy to add an entire group of users as co-owner of a door.
//       Remember that when removing a player from a group, the player should also be removed as co-owner from all doors
//       as well, as long as they are not in another group that is also a co-owner of this door.
//       Alternatively, consider splitting the DoorOwner table into DoorOwnerPlayer and DoorOwnerGroup, so that when you
//       add a player to a group that is an owner of a door, you don't have to modify everything. Would make queries
//       more difficult, though.
// TODO: Consider adding folders. This would require a "Folders" table with an owner, ID, and a folder name as well as
//       a table that links up doors with folders.
// TODO: Store engineChunkHash in the database, so stuff can be done when the chunk of a door is loaded. Also make sure
//       to move the engine location for movable doors (sliding, etc).
// TODO: Maybe store UUIDs as 16 byte binary blobs to save space: https://stackoverflow.com/a/17278095
// TODO: Move database upgrades out of the main SQL class. Perhaps create some kind of upgrade routine interface.

/*
 * Commands
 */
// TODO: Make sure onTabComplete works. Perhaps switch to using a Suffix Tree instead of a map?
//       Though bukkit just uses brute force to iterate over the entryset, so that might be overkill?
//       https://en.wikipedia.org/wiki/Suffix_tree
// TODO: Respect CompletableFutures better.
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
 * Creators / ToolUsers
 */
// TODO: In the creation process, ask the user if the door is currently opened or closed (when appropriate).
//       Then do not ask the user to set the openDirection, but ask them which direction they want the move.
//       From this, we can infer what the openDir is supposed to be. Perhaps ask the user to hit the stick in the
//       desired direction. Or use the glowing blocks to show a preview?
// TODO: When registering a new ToolUser, also make sure to immediately start the timer thingy, otherwise it just
//       gets too messy. Also handle the removal in the shutdown method.
// TODO: Consider introducing different (optional) modes for the creators such as Creator.Mode#NEW_DOOR
//       and Creator.Mode#UPDATE_COORDS, Creator.Mode#UPDATE_POWERBLOCK, etc. These modes can be used to
//       update a specific aspect of a door. Only including the desired steps should be enough to
//       actually use it. Any mode other than NEW_DOOR should update a door instead of inserting a new one.
//       However, this needs to use the door attribute system and as such this issue is blocked until that is in place.
//       Because an Enum won't cut it for custom attributes.
// TODO: Before starting the creation process, make sure that there is space in the player's inventory for the tool
//       So when the player's inventory is full:
//       "/bigdoors newdoor -type BigDoor" -> "Your inventory appears to be full! Please make some space and try again!"
//       If their inventory fills up during the process, just abort when they get to the point where they should have
//       received the tool.
// TODO: Implement a system to request text input from the user. Either by registering a command waiter to request command
//       input, or by making text in a message clickable, or opening a GUI or whatever. Perhaps even all of them and
//       let the user decide which option they prefer.

/*
 * Openers / Movers
 */
// TODO: Consider making it possible to still use the velocity method. But to do so, it'll have to be a separate mode.
//       This is needed to select the correct strategy for stuff like returning the correct current location.
// TODO: Make sure the relmove isn't too far. If so, limit it to the highest/lowest possible value.
// TODO: Instead of a relmove, use a full teleport every now and then to avoid client-server desync from dropped packets.
// TODO: Port the chunk range shit from v1 to v2.
// TODO: There is still a bunch of duplicated code (e.g. BigDoor#getPotentialNewCoordinates and
//       RevolvingDoor#getPotentialNewCoordinates). This is horrible and should be fixed. Perhaps create a separate
//       object that can do it? Or a static method? Ugh.
// TODO: The ClockMover should keep in mind that clocks can have a depth greater than 2 blocks. Currently, it only
//       thinks there are 2 arms, each 1 block deep, which would cause issues with any other size.
// TODO: The perpetual movers (revolving door, windmill) should also have a mode where they can be opened and closed like regular doors.
// TODO: When a door is modified in a way that leaves it in an 'impossible' state, make sure to first return to the proper state.
//       So, if a door is currently open to the west and the opendir is changed to east and it is toggled again,
//       toggle it to the east again first, even though the closedir would normally be the opposite of the opendir
//       (therefore close to the west).
// TODO: RevolvingDoor: The final location of the blocks is not the original location. You can see this issue when
//       using a revolving door with an off-center rotation point. The same applies to
// TODO: Figure out what to do with the player sometimes being nullable and notnull at other times. Make a clear decision.
// TODO: Get rid of the weird speed multipliers in the CustomEntityFallingBlock_VX_XX_RX classes.
// TODO: Clamp angles to [-2PI ; 2PI].
// TODO: Make sure the new types don't just open instantly without a provided time parameter.
// TODO: Rename variables in updateCoords to avoid confusion. Perhaps a complete removal altogether would be nice as well.
// TODO: Get rid of the GNL interface etc. The movers class can handle it on its own using Function interface.
// TODO: Move getBlocksMoved() to Mover.
// TODO: Make setDefaultOpenDirection() smarter by checking which side is actually available.
// TODO: Movers: updateCoords should be final and use the DoorBase::getNewLocations method to get the new min/max.
// TODO: GarageDoor: The pivot point offset (where it starts pivoting), should depend on the radius. The higher the radius of the block compared
//       to the total radius, the smaller the offset should be. This way, the final blocks will line up better with the final position.
//       radius = maxRadius -> offset = 0. Should probably only look at the last 3 blocks. e.g.: offset = Min((offset / 4) * (totalRadius - radius)).
// TODO: When checking if a door's chunks are loaded, use the door's chunkRange variables.
// TODO: Instead of having methods to open/close/toggle animated objects, have a single method that receives
//       a redstone value or something. Then each animated object can determine how to handle it on its own.
//       Open/close/toggle for doors, activate/deactivate for persistent movement (flags, clocks, etc).
// TODO: SlidingDoor, Portcullis: Cache BlocksToMove in a Vec2D. Invalidate when coors and stuff change.
// TODO: Update NS variables in the movers so they don't mean "active along north/south axis" but rather
//       "engine aligned with north/south axis", which effectively means the opposite. Also, obtain the variable from
//       the door.
// TODO: Highlight all blocking blocks if BlocksToMove is set.
// TODO: When trying to activate a door in an unloaded chunk, load the chunk and instantly toggle the door (skip the animation).
//       Extension: Add config option to send an error message to the player instead (so abort activation altogether).

/*



 */

/*
 * Manual Testing
 */
// TODO: Make sure no unnecessary database calls are made. Log them in the construct method of the PPreparedStatement class.
// TODO: Make sure that new lines in the messages work (check Util::stringFromArray).
// TODO: Check if TimedCache#containsValue() works properly.
// TODO: Test all methods in the database manager stuff.
// TODO: Fix not being able to use doorUID in setBlocksToMove (direct).
// TODO: Fix start message of creator appearing twice as well as receiving 2 creator sticks (at least for flag and sliding door).
// TODO: Verify the following types work: (Wall)Signs, (Wall)Banners, plants (potatoes, carrots, etc), redstone stuff (wire, conduit, repeater, etc),
//       Rails, coral, beds, carpet, dragon egg, concrete powders, pressure plates, buttons, levers, saplings,
//       Structure blocks, bubble column, (wall)torches, enderchest, (shulkerbox?).

/*
 * Unit tests
 */
// TODO: To speed up testing, use an in-memory database: https://www.sqlite.org/inmemorydb.html
//       Then write it to disk once it's done (for potential manual inspection): https://www.sqlite.org/backup.html
// TODO: Test the DoorRegistry system somehow.
// TODO: Don't use the beforeLastMessage method. Instead, use something like getLastWarning. Also means that levels will
//       have to be used when messaging players.
// TODO: The Unit Tests have access to protected members. Use this to test specific methods (in addition to running
//       the whole procedure, of course).
// TODO: https://bukkit.org/threads/how-to-unit-test-your-plugin-with-example-project.23569/
// TODO: https://www.spigotmc.org/threads/using-junit-to-test-plugins.71420/#post-789671
// TODO: https://github.com/seeseemelk/MockBukkit
// TODO: Make sure to test database upgrade to v11. Make this future-proof somehow. Perhaps store the old v10 creation
//       stuff somewhere.
// TODO: Write a completely standalone implementation of the API, which can be used not only to test all core stuff,
//       but also for development. If the jar is launched on its own, it should load up this implementation.
//       This would allow developing without using the Spigot server, which takes a bit of time to launch for every
//       change made to the core, slowing down development.


/**
 * Represents the core class of BigDoors.
 *
 * @author Pim
 */
public final class BigDoors implements IRestartableHolder, IRestartable
{
    @NotNull
    private static final BigDoors instance = new BigDoors();

    @Nullable
    private IMessagingInterface messagingInterface = null;

    @NotNull
    private final Set<IRestartable> restartables = new HashSet<>();

    /**
     * The platform to use. e.g. "Spigot".
     */
    @NotNull
    private IBigDoorsPlatform platform;

    private BigDoors()
    {
    }

    /**
     * Gets the instance of this class.
     *
     * @return The instance of this class.
     */
    public static @NotNull BigDoors get()
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
        if (this.platform != null)
            this.platform.deregisterRestartable(this);
        this.platform = platform;
        this.platform.registerRestartable(this);
    }

    /**
     * gets the platform implementing BigDoor's internal API.
     *
     * @return The platform implementing BigDoor's internal API.
     */
    public @NotNull IBigDoorsPlatform getPlatform()
    {
        return platform;
    }

    /**
     * Gets the {@link DoorActivityManager} instance.
     *
     * @return The {@link DoorActivityManager} instance.
     */
    public @NotNull DoorActivityManager getDoorManager()
    {
        return DoorActivityManager.get();
    }

    /**
     * Gets the {@link AutoCloseScheduler} instance.
     *
     * @return The {@link AutoCloseScheduler} instance.
     */
    public @NotNull AutoCloseScheduler getAutoCloseScheduler()
    {
        return AutoCloseScheduler.get();
    }

    /**
     * Gets the {@link PowerBlockManager} instance.
     *
     * @return The {@link PowerBlockManager} instance.
     */
    public @NotNull PowerBlockManager getPowerBlockManager()
    {
        return PowerBlockManager.get();
    }

    /**
     * Gets the currently used {@link IMessagingInterface}. If the current one isn't set, {@link
     * IBigDoorsPlatform#getMessagingInterface} is used instead.
     *
     * @return The currently used {@link IMessagingInterface}.
     */
    public @NotNull IMessagingInterface getMessagingInterface()
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
    public @NotNull DatabaseManager getDatabaseManager()
    {
        return DatabaseManager.get();
    }

    @Override
    public void restart()
    {
        restartables.forEach(IRestartable::restart);
    }

    @Override
    public void shutdown()
    {
        restartables.forEach(IRestartable::shutdown);
    }

    @Override
    public void registerRestartable(final @NotNull IRestartable restartable)
    {
        restartables.add(restartable);
    }

    @Override
    public boolean isRestartableRegistered(final @NotNull IRestartable restartable)
    {
        return restartables.contains(restartable);
    }

    @Override
    public void deregisterRestartable(final @NotNull IRestartable restartable)
    {
        restartables.remove(restartable);
    }
}
