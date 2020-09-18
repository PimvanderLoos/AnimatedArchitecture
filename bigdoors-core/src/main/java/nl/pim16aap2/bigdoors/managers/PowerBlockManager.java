package nl.pim16aap2.bigdoors.managers;

import nl.pim16aap2.bigdoors.api.IConfigLoader;
import nl.pim16aap2.bigdoors.api.IRestartable;
import nl.pim16aap2.bigdoors.api.IRestartableHolder;
import nl.pim16aap2.bigdoors.doors.AbstractDoorBase;
import nl.pim16aap2.bigdoors.util.PLogger;
import nl.pim16aap2.bigdoors.util.Restartable;
import nl.pim16aap2.bigdoors.util.TimedMapCache;
import nl.pim16aap2.bigdoors.util.Util;
import nl.pim16aap2.bigdoors.util.vector.Vector2DiConst;
import nl.pim16aap2.bigdoors.util.vector.Vector3Di;
import nl.pim16aap2.bigdoors.util.vector.Vector3DiConst;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Manages all power block interactions.
 *
 * @author Pim
 */
public final class PowerBlockManager extends Restartable
{
    private final @NotNull Map<String, PowerBlockWorld> powerBlockWorlds = new ConcurrentHashMap<>();
    private final @NotNull IConfigLoader config;
    private final @NotNull DatabaseManager databaseManager;
    private final @NotNull PLogger pLogger;

    private static @Nullable PowerBlockManager instance;
    private final @NotNull IRestartableHolder restartableHolder;

    private PowerBlockManager(final @NotNull IRestartableHolder restartableHolder, final @NotNull IConfigLoader config,
                              final @NotNull DatabaseManager databaseManager, final @NotNull PLogger pLogger)
    {
        super(restartableHolder);
        this.restartableHolder = restartableHolder;
        this.config = config;
        this.databaseManager = databaseManager;
        this.pLogger = pLogger;

    }

    /**
     * Initializes the {@link PowerBlockManager}. If it has already been initialized, it'll return that instance
     * instead.
     *
     * @param holder          The {@link IRestartableHolder} that manages this object.
     * @param config          The configuration of this plugin.
     * @param databaseManager The database manager to use for power block retrieval.
     * @param pLogger         The logger used for error logging.
     * @return The instance of this {@link PowerBlockManager}.
     */
    public static @NotNull PowerBlockManager init(final @NotNull IRestartableHolder holder,
                                                  final @NotNull IConfigLoader config,
                                                  final @NotNull DatabaseManager databaseManager,
                                                  final @NotNull PLogger pLogger)
    {
        return (instance == null) ? instance = new PowerBlockManager(holder, config, databaseManager, pLogger) :
               instance;
    }

    /**
     * Gets the instance of the {@link PowerBlockManager} if it exists.
     *
     * @return The instance of the {@link PowerBlockManager}.
     */
    public static @NotNull PowerBlockManager get()
    {
//        Preconditions.checkState(instance != null,
//                                 "Instance has not yet been initialized. Be sure #init() has been invoked");
        return instance;
    }

    /**
     * Unloads a world from the cache.
     *
     * @param worldName The name of the world the unload.
     */
    public void unloadWorld(final @NotNull String worldName)
    {
        powerBlockWorlds.remove(worldName);
    }

    /**
     * Loads a world.
     *
     * @param worldName The name of the world.
     */
    public void loadWorld(final @NotNull String worldName)
    {
        powerBlockWorlds.put(worldName, new PowerBlockWorld(worldName));
    }

    /**
     * Gets all {@link AbstractDoorBase}s that have a powerblock at a location in a world.
     *
     * @param loc       The location.
     * @param worldName The name of the world.
     * @return All {@link AbstractDoorBase}s that have a powerblock at a location in a world.
     */
    // TODO: Try to have about 50% less CompletableFuture here.
    public @NotNull CompletableFuture<List<CompletableFuture<Optional<AbstractDoorBase>>>> doorsFromPowerBlockLoc(
        final @NotNull Vector3DiConst loc, final @NotNull String worldName)
    {
        final @NotNull PowerBlockWorld powerBlockWorld = powerBlockWorlds.get(worldName);
        if (powerBlockWorld == null)
        {
            pLogger.logMessage(Level.WARNING, "Failed to load power blocks for world: \"" + worldName + "\".");
            return CompletableFuture.completedFuture(Collections.emptyList());
        }

        final @NotNull CompletableFuture<List<Long>> doorUIDs = powerBlockWorld.getPowerBlocks(loc);
        return doorUIDs.handle(
            (list, throwable) ->
            {
                final @NotNull List<CompletableFuture<Optional<AbstractDoorBase>>> doorBases = new ArrayList<>();
                list.forEach(doorUID -> doorBases.add(databaseManager.getDoor(doorUID)));
                return doorBases;
            });
    }

    /**
     * Checks if a world is a BigDoors world. In other words, it checks if a world contains more than 0 doors.
     *
     * @param worldName The name of the world.
     * @return True if the world contains at least 1 door.
     */
    public boolean isBigDoorsWorld(final @NotNull String worldName)
    {
        final @NotNull PowerBlockWorld powerBlockWorld = powerBlockWorlds.get(worldName);
        if (powerBlockWorld == null)
        {
            pLogger.logMessage(Level.WARNING, "Failed to load power blocks for world: \"" + worldName + "\".");
            return false;
        }
        return powerBlockWorld.isBigDoorsWorld();
    }

    /**
     * Updates the position of the power block of a {@link AbstractDoorBase} in the database.
     *
     * @param door   The {@link AbstractDoorBase}.
     * @param oldPos The old position.
     * @param newPos The new position.
     */
    public void updatePowerBlockLoc(final @NotNull AbstractDoorBase door, final @NotNull Vector3DiConst oldPos,
                                    final @NotNull Vector3DiConst newPos)
    {
        door.setPowerBlockPosition(newPos).syncBaseData();
        final @NotNull PowerBlockWorld powerBlockWorld = powerBlockWorlds.get(door.getWorld().getWorldName());
        if (powerBlockWorld == null)
        {
            pLogger.logMessage(Level.WARNING,
                               "Failed to load power blocks for world: \"" + door.getWorld().getWorldName() + "\".");
            return;
        }

        // Invalidate both the old and the new positions.
        powerBlockWorld.invalidatePosition(oldPos);
        powerBlockWorld.invalidatePosition(newPos);
    }

    /**
     * Invalidates the cache for when a door is either added to a world or removed from it.
     *
     * @param worldName The name of the world of the door.
     * @param pos       The position of the door's power block.
     */
    public void onDoorAddOrRemove(final @NotNull String worldName, final @NotNull Vector3DiConst pos)
    {
        final @NotNull PowerBlockWorld powerBlockWorld = powerBlockWorlds.get(worldName);
        if (powerBlockWorld == null)
        {
            pLogger.logMessage(Level.WARNING, "Failed to load power blocks for world: \"" + worldName + "\".");
            return;
        }
        powerBlockWorld.invalidatePosition(pos);
        powerBlockWorld.checkBigDoorsWorldStatus();
    }

    /**
     * Invalidates the cache of a chunk in a world.
     *
     * @param worldName The name of the world.
     * @param chunk     The location (x,z) of the chunk in chunk-space.
     */
    public void invalidateChunk(final @NotNull String worldName, final @NotNull Vector2DiConst chunk)
    {
        final @NotNull PowerBlockWorld powerBlockWorld = powerBlockWorlds.get(worldName);
        if (powerBlockWorld == null)
        {
            pLogger.logMessage(Level.WARNING, "Failed to load power blocks for world: \"" + worldName + "\".");
            return;
        }
        powerBlockWorld.invalidatePosition(new Vector3Di(chunk.getX(), 64, chunk.getY()));
        powerBlockWorld.checkBigDoorsWorldStatus();
    }

    @Override
    public void restart()
    {
        powerBlockWorlds.values().forEach(PowerBlockWorld::restart);
    }

    @Override
    public void shutdown()
    {
        powerBlockWorlds.values().forEach(PowerBlockWorld::shutdown);
    }

    /**
     * Represents a world that may or may not contain any power blocks.
     *
     * @author Pim
     */
    private final class PowerBlockWorld implements IRestartable
    {
        private final @NotNull String worldName;
        private volatile boolean isBigDoorsWorld = false;
        /**
         * TimedCache of all {@link PowerBlockChunk}s in this world.
         * <p>
         * Key: Simple chunk hash, {@link Util#simpleChunkHashFromLocation(int, int)}.
         * <p>
         * Value: The {@link PowerBlockChunk}s.
         */
        private final @NotNull TimedMapCache<Long, PowerBlockChunk> powerBlockChunks =
            new TimedMapCache<>(restartableHolder, ConcurrentHashMap::new, config.cacheTimeout());

        private PowerBlockWorld(final @NotNull String worldName)
        {
            this.worldName = worldName;
            checkBigDoorsWorldStatus();
        }

        /**
         * Checks if this world contains more than 0 doors (and power blocks by extension).
         *
         * @return True if this world contains more than 0 doors.
         */
        private boolean isBigDoorsWorld()
        {
            return isBigDoorsWorld;
        }

        /**
         * Gets all UIDs of doors whose power blocks are in the given location.
         *
         * @param loc The location to check.
         * @return All UIDs of doors whose power blocks are in the given location.
         */
        private @NotNull CompletableFuture<List<Long>> getPowerBlocks(final @NotNull Vector3DiConst loc)
        {
            if (!isBigDoorsWorld())
                return CompletableFuture.completedFuture(Collections.emptyList());

            final long chunkHash = Util.simpleChunkHashFromLocation(loc.getX(), loc.getZ());

            if (!powerBlockChunks.containsKey(chunkHash))
            {
                final @Nullable PowerBlockChunk powerBlockChunk =
                    powerBlockChunks.put(chunkHash, new PowerBlockChunk());

                final @NotNull CompletableFuture<ConcurrentHashMap<Integer, List<Long>>> powerBlocks
                    = DatabaseManager.get().getPowerBlockData(chunkHash);

                return powerBlocks.handle(
                    (map, exception) ->
                    {
                        if (powerBlockChunk != null)
                            powerBlockChunk.setPowerBlocks(map);

                        final @NotNull List<Long> doorUIDs = new ArrayList<>(map.size());
                        map.forEach((key, value) -> doorUIDs.addAll(value));
                        return doorUIDs;
                    });
            }
            final @Nullable PowerBlockChunk entry = powerBlockChunks.get(chunkHash);
            return CompletableFuture.completedFuture(entry == null ?
                                                     Collections.emptyList() : entry.getPowerBlocks(loc));
        }

        /**
         * Removes the chunk of a position from the cache.
         *
         * @param pos The position.
         */
        private void invalidatePosition(final @NotNull Vector3DiConst pos)
        {
            powerBlockChunks.remove(Util.simpleChunkHashFromLocation(pos.getX(), pos.getZ()));
        }

        /**
         * Updates if this world contains more than 0 doors in the database (and power blocks by extension).
         * <p>
         * This differs from {@link #isBigDoorsWorld()} in that this method queries the database.
         */
        private void checkBigDoorsWorldStatus()
        {
            databaseManager.isBigDoorsWorld(worldName).whenComplete((result, throwable) -> isBigDoorsWorld = result);
        }

        @Override
        public void restart()
        {
            powerBlockChunks.restart();
        }

        @Override
        public void shutdown()
        {
            powerBlockChunks.shutdown();
        }
    }

    /**
     * Represents a chunk that may or may not contain any power blocks.
     *
     * @author Pim
     */
    private static final class PowerBlockChunk
    {
        /**
         * Map that contains all power blocks in this chunk.
         * <p>
         * Key: Hashed locations (in chunk-space coordinates), {@link Util#simpleChunkSpaceLocationhash(int, int,
         * int)}.
         * <p>
         * Value: List of UIDs of all doors whose power block occupy this space.
         */
        private @NotNull Map<Integer, List<Long>> powerBlocks = Collections.emptyMap();

        private void setPowerBlocks(final @NotNull Map<Integer, List<Long>> powerBlocks)
        {
            this.powerBlocks = powerBlocks;
        }

        /**
         * Gets all UIDs of doors whose power blocks are in the given location.
         *
         * @param loc The location to check.
         * @return All UIDs of doors whose power blocks are in the given location.
         */
        private @NotNull List<Long> getPowerBlocks(final @NotNull Vector3DiConst loc)
        {
            if (!isPowerBlockChunk())
                return Collections.emptyList();

            return powerBlocks.getOrDefault(Util.simpleChunkSpaceLocationhash(loc.getX(), loc.getY(), loc.getZ()),
                                            Collections.emptyList());
        }

        /**
         * Updates if this chunk contains more than 0 power blocks.
         *
         * @return True if this chunk contains more than 0 doors.
         */
        private boolean isPowerBlockChunk()
        {
            return powerBlocks.size() > 0;
        }
    }
}
