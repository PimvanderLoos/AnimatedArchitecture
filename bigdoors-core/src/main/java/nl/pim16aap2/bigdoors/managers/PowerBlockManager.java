package nl.pim16aap2.bigdoors.managers;

import lombok.extern.flogger.Flogger;
import nl.pim16aap2.bigdoors.api.IConfigLoader;
import nl.pim16aap2.bigdoors.api.restartable.Restartable;
import nl.pim16aap2.bigdoors.api.restartable.RestartableHolder;
import nl.pim16aap2.bigdoors.data.cache.timed.TimedCache;
import nl.pim16aap2.bigdoors.doors.AbstractDoor;
import nl.pim16aap2.bigdoors.doors.DoorBase;
import nl.pim16aap2.bigdoors.util.Util;
import nl.pim16aap2.bigdoors.util.vector.Vector2Di;
import nl.pim16aap2.bigdoors.util.vector.Vector3Di;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.time.Duration;
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
@Singleton
@Flogger
public final class PowerBlockManager extends Restartable
{
    private final Map<String, PowerBlockWorld> powerBlockWorlds = new ConcurrentHashMap<>();
    private final IConfigLoader config;
    private final DatabaseManager databaseManager;

    /**
     * Initializes the {@link PowerBlockManager}. If it has already been initialized, it'll return that instance
     * instead.
     *
     * @param restartableHolder
     *     The {@link RestartableHolder} that manages this object.
     * @param config
     *     The configuration of this plugin.
     * @param databaseManager
     *     The database manager to use for power block retrieval.
     */
    @Inject
    public PowerBlockManager(RestartableHolder restartableHolder, IConfigLoader config, DatabaseManager databaseManager)
    {
        super(restartableHolder);
        this.config = config;
        this.databaseManager = databaseManager;
    }

    /**
     * Unloads a world from the cache.
     *
     * @param worldName
     *     The name of the world to unload.
     */
    public void unloadWorld(String worldName)
    {
        powerBlockWorlds.remove(worldName);
    }

    /**
     * Loads a world.
     *
     * @param worldName
     *     The name of the world.
     */
    public void loadWorld(String worldName)
    {
        powerBlockWorlds.put(worldName, new PowerBlockWorld(worldName));
    }

    /**
     * Gets all {@link DoorBase}s that have a powerblock at a location in a world.
     *
     * @param loc
     *     The location.
     * @param worldName
     *     The name of the world.
     * @return All {@link DoorBase}s that have a powerblock at a location in a world.
     */
    // TODO: Try to have about 50% less CompletableFuture here.
    public CompletableFuture<List<CompletableFuture<Optional<AbstractDoor>>>> doorsFromPowerBlockLoc(
        Vector3Di loc,
        String worldName)
    {
        final PowerBlockWorld powerBlockWorld = powerBlockWorlds.get(worldName);
        if (powerBlockWorld == null)
        {
            log.at(Level.WARNING).log("Failed to load power blocks for world: '%s'.", worldName);
            return CompletableFuture.completedFuture(Collections.emptyList());
        }

        return powerBlockWorld.getPowerBlocks(loc).handle(
            (list, throwable) ->
            {
                final List<CompletableFuture<Optional<AbstractDoor>>> doorBases = new ArrayList<>();
                list.forEach(doorUID -> doorBases.add(databaseManager.getDoor(doorUID)));
                return doorBases;
            }).exceptionally(ex -> Util.exceptionally(ex, Collections.emptyList()));
    }

    /**
     * Checks if a world is a BigDoors world. In other words, it checks if a world contains more than 0 doors.
     *
     * @param worldName
     *     The name of the world.
     * @return True if the world contains at least 1 door.
     */
    public boolean isBigDoorsWorld(String worldName)
    {
        final PowerBlockWorld powerBlockWorld = powerBlockWorlds.get(worldName);
        if (powerBlockWorld == null)
        {
            log.at(Level.WARNING).log("Failed to load power blocks for world: '%s'.", worldName);
            return false;
        }
        return powerBlockWorld.isBigDoorsWorld();
    }

    /**
     * Updates the position of the power block of a {@link DoorBase} in the database.
     *
     * @param door
     *     The {@link DoorBase}.
     * @param oldPos
     *     The old position.
     * @param newPos
     *     The new position.
     */
    @SuppressWarnings("unused")
    public void updatePowerBlockLoc(AbstractDoor door, Vector3Di oldPos, Vector3Di newPos)
    {
        door.setPowerBlock(newPos);
        door.syncData();
        final PowerBlockWorld powerBlockWorld = powerBlockWorlds.get(door.getWorld().worldName());
        if (powerBlockWorld == null)
        {
            log.at(Level.WARNING).log("Failed to load power blocks for world: '%s'.", door.getWorld().worldName());
            return;
        }

        // Invalidate both the old and the new positions.
        powerBlockWorld.invalidatePosition(oldPos);
        powerBlockWorld.invalidatePosition(newPos);
    }

    /**
     * Invalidates the cache for when a door is either added to a world or removed from it.
     *
     * @param worldName
     *     The name of the world of the door.
     * @param pos
     *     The position of the door's power block.
     */
    public void onDoorAddOrRemove(String worldName, Vector3Di pos)
    {
        final PowerBlockWorld powerBlockWorld = powerBlockWorlds.get(worldName);
        if (powerBlockWorld == null)
        {
            log.at(Level.WARNING).log("Failed to load power blocks for world: '%s'.", worldName);
            return;
        }
        powerBlockWorld.invalidatePosition(pos);
        powerBlockWorld.checkBigDoorsWorldStatus();
    }

    /**
     * Invalidates the cache of a chunk in a world.
     *
     * @param worldName
     *     The name of the world.
     * @param chunk
     *     The location (x,z) of the chunk in chunk-space.
     */
    public void invalidateChunk(String worldName, Vector2Di chunk)
    {
        final PowerBlockWorld powerBlockWorld = powerBlockWorlds.get(worldName);
        if (powerBlockWorld == null)
        {
            log.at(Level.WARNING).log("Failed to load power blocks for world: '%s'.", worldName);
            return;
        }
        powerBlockWorld.invalidatePosition(new Vector3Di(chunk.x(), 64, chunk.y()));
        powerBlockWorld.checkBigDoorsWorldStatus();
    }

    @Override
    public void shutDown()
    {
        powerBlockWorlds.values().forEach(PowerBlockWorld::clear);
    }

    /**
     * Represents a world that may or may not contain any power blocks.
     *
     * @author Pim
     */
    private final class PowerBlockWorld
    {
        private final String worldName;
        private volatile boolean isBigDoorsWorld = false;

        /**
         * TimedCache of all {@link PowerBlockChunk}s in this world.
         * <p>
         * Key: chunkId: {@link Util#getChunkId(Vector3Di)}.
         * <p>
         * Value: The {@link PowerBlockChunk}s.
         */
        private final TimedCache<Long, PowerBlockChunk> powerBlockChunks =
            TimedCache.<Long, PowerBlockChunk>builder()
                      .duration(Duration.ofMinutes(config.cacheTimeout()))
                      .cleanup(Duration.ofMinutes(Math.max(1, config.cacheTimeout())))
                      .softReference(true)
                      .refresh(true).build();

        private PowerBlockWorld(String worldName)
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
         * @param loc
         *     The location to check.
         * @return All UIDs of doors whose power blocks are in the given location.
         */
        private CompletableFuture<List<Long>> getPowerBlocks(Vector3Di loc)
        {
            if (!isBigDoorsWorld())
                return CompletableFuture.completedFuture(Collections.emptyList());

            final long chunkId = Util.getChunkId(loc);

            if (!powerBlockChunks.containsKey(chunkId))
            {
                final PowerBlockChunk powerBlockChunk =
                    powerBlockChunks.put(chunkId, new PowerBlockChunk());

                return databaseManager.getPowerBlockData(chunkId).handle(
                    (map, exception) ->
                    {
                        powerBlockChunk.setPowerBlocks(map);

                        final List<Long> doorUIDs = new ArrayList<>(map.size());
                        map.forEach((key, value) -> doorUIDs.addAll(value));
                        return doorUIDs;
                    }).exceptionally(ex -> Util.exceptionally(ex, Collections.emptyList()));
            }

            return CompletableFuture.completedFuture(powerBlockChunks.get(chunkId)
                                                                     .map((entry) -> entry.getPowerBlocks(loc))
                                                                     .orElseGet(Collections::emptyList));
        }

        /**
         * Removes the chunk of a position from the cache.
         *
         * @param pos
         *     The position.
         */
        private void invalidatePosition(Vector3Di pos)
        {
            powerBlockChunks.remove(Util.getChunkId(pos));
        }

        /**
         * Updates if this world contains more than 0 doors in the database (and power blocks by extension).
         * <p>
         * This differs from {@link #isBigDoorsWorld()} in that this method queries the database.
         */
        private void checkBigDoorsWorldStatus()
        {
            databaseManager.isBigDoorsWorld(worldName).whenComplete((result, throwable) -> isBigDoorsWorld = result)
                           .exceptionally(Util::exceptionally);
        }

        void clear()
        {
            powerBlockChunks.clear();
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
         * Key: Hashed locations (in chunk-space coordinates),
         * {@link Util#simpleChunkSpaceLocationhash(int, int, int)}.
         * <p>
         * Value: List of UIDs of all doors whose power block occupy this space.
         */
        private Map<Integer, List<Long>> powerBlocks = Collections.emptyMap();

        private void setPowerBlocks(Map<Integer, List<Long>> powerBlocks)
        {
            this.powerBlocks = powerBlocks;
        }

        /**
         * Gets all UIDs of doors whose power blocks are in the given location.
         *
         * @param loc
         *     The location to check.
         * @return All UIDs of doors whose power blocks are in the given location.
         */
        private List<Long> getPowerBlocks(Vector3Di loc)
        {
            if (!isPowerBlockChunk())
                return Collections.emptyList();

            return powerBlocks.getOrDefault(Util.simpleChunkSpaceLocationhash(loc.x(), loc.y(), loc.z()),
                                            Collections.emptyList());
        }

        /**
         * Updates if this chunk contains more than 0 power blocks.
         *
         * @return True if this chunk contains more than 0 doors.
         */
        private boolean isPowerBlockChunk()
        {
            return !powerBlocks.isEmpty();
        }
    }
}
