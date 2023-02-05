package nl.pim16aap2.bigdoors.core.managers;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.longs.LongImmutableList;
import it.unimi.dsi.fastutil.longs.LongList;
import it.unimi.dsi.fastutil.longs.LongLists;
import lombok.Getter;
import lombok.extern.flogger.Flogger;
import nl.pim16aap2.bigdoors.core.api.IConfigLoader;
import nl.pim16aap2.bigdoors.core.api.IPLocation;
import nl.pim16aap2.bigdoors.core.api.IPWorld;
import nl.pim16aap2.bigdoors.core.api.restartable.Restartable;
import nl.pim16aap2.bigdoors.core.api.restartable.RestartableHolder;
import nl.pim16aap2.bigdoors.core.data.cache.timed.TimedCache;
import nl.pim16aap2.bigdoors.core.structures.AbstractStructure;
import nl.pim16aap2.bigdoors.core.structures.IStructureConst;
import nl.pim16aap2.bigdoors.core.util.Util;
import nl.pim16aap2.bigdoors.core.util.vector.Vector2Di;
import nl.pim16aap2.bigdoors.core.util.vector.Vector3Di;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages all power block interactions.
 *
 * @author Pim
 */
@Singleton
@Flogger
public final class PowerBlockManager extends Restartable implements StructureDeletionManager.IDeletionListener
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
    @Inject PowerBlockManager(
        RestartableHolder restartableHolder, IConfigLoader config, DatabaseManager databaseManager,
        StructureDeletionManager structureDeletionManager)
    {
        super(restartableHolder);

        this.config = config;
        this.databaseManager = databaseManager;

        structureDeletionManager.registerDeletionListener(this);
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
     * Gets all {@link AbstractStructure}s that have a powerblock at a location in a world.
     *
     * @param location
     *     The location.
     * @return All {@link AbstractStructure}s that have a powerblock at a location in a world.
     */
    public CompletableFuture<List<AbstractStructure>> structuresFromPowerBlockLoc(IPLocation location)
    {
        return this.structuresFromPowerBlockLoc(location.getPosition(), location.getWorld().worldName());
    }

    /**
     * Gets all {@link AbstractStructure}s that have a powerblock at a location in a world.
     *
     * @param loc
     *     The location.
     * @param world
     *     The world.
     * @return All {@link AbstractStructure}s that have a powerblock at a location in a world.
     */
    public CompletableFuture<List<AbstractStructure>> structuresFromPowerBlockLoc(Vector3Di loc, IPWorld world)
    {
        return this.structuresFromPowerBlockLoc(loc, world.worldName());
    }

    /**
     * Gets all {@link AbstractStructure}s that have a powerblock at a location in a world.
     *
     * @param loc
     *     The location.
     * @param worldName
     *     The name of the world.
     * @return All {@link AbstractStructure}s that have a powerblock at a location in a world.
     */
    public CompletableFuture<List<AbstractStructure>> structuresFromPowerBlockLoc(Vector3Di loc, String worldName)
    {
        final PowerBlockWorld powerBlockWorld = powerBlockWorlds.get(worldName);
        if (powerBlockWorld == null)
        {
            log.atWarning().log("Failed to load power blocks for world: '%s'.", worldName);
            return CompletableFuture.completedFuture(Collections.emptyList());
        }
        return mapUidsToStructures(powerBlockWorld.getPowerBlocksAtLocation(loc));
    }

    /**
     * Retrieves all structures whose powerblocks exist in the same chunk as a given location.
     *
     * @param loc
     *     The location.
     * @param worldName
     *     The name of the world to search in.
     * @return The UIDs of the structures that whose powerblocks lie in the same chunk as the location.
     */
    public CompletableFuture<List<AbstractStructure>> structuresInChunk(Vector3Di loc, String worldName)
    {
        final PowerBlockWorld powerBlockWorld = powerBlockWorlds.get(worldName);
        if (powerBlockWorld == null)
        {
            log.atWarning().log("Failed to load power blocks for world: '%s'.", worldName);
            return CompletableFuture.completedFuture(Collections.emptyList());
        }
        return mapUidsToStructures(powerBlockWorld.getPowerBlocksInChunk(loc));
    }

    private CompletableFuture<List<AbstractStructure>> mapUidsToStructures(CompletableFuture<LongList> uids)
    {
        return uids
            .thenApplyAsync(lst -> lst.longStream().mapToObj(databaseManager::getStructure).toList())
            .thenCompose(Util::getAllCompletableFutureResults)
            .thenApply(lst -> lst.stream().filter(Optional::isPresent).map(Optional::get).toList())
            .exceptionally(ex -> Util.exceptionally(ex, Collections.emptyList()));
    }

    /**
     * Checks if a world is a BigDoors world. In other words, it checks if a world contains more than 0 structures.
     *
     * @param worldName
     *     The name of the world.
     * @return True if the world contains at least 1 structure.
     */
    public boolean isBigDoorsWorld(String worldName)
    {
        final PowerBlockWorld powerBlockWorld = powerBlockWorlds.get(worldName);
        if (powerBlockWorld == null)
        {
            log.atWarning().log("Failed to load power blocks for world: '%s'.", worldName);
            return false;
        }
        return powerBlockWorld.isBigDoorsWorld();
    }

    /**
     * Updates the position of the power block of a {@link AbstractStructure} in the database.
     *
     * @param structure
     *     The {@link AbstractStructure}.
     * @param oldPos
     *     The old position.
     * @param newPos
     *     The new position.
     */
    @SuppressWarnings("unused")
    public void updatePowerBlockLoc(AbstractStructure structure, Vector3Di oldPos, Vector3Di newPos)
    {
        structure.setPowerBlock(newPos);
        structure.syncData();
        final PowerBlockWorld powerBlockWorld = powerBlockWorlds.get(structure.getWorld().worldName());
        if (powerBlockWorld == null)
        {
            log.atWarning().log("Failed to load power blocks for world: '%s'.", structure.getWorld().worldName());
            return;
        }

        // Invalidate both the old and the new positions.
        powerBlockWorld.invalidatePosition(oldPos);
        powerBlockWorld.invalidatePosition(newPos);
    }

    /**
     * Invalidates the cache for when a structure is either added to a world or removed from it.
     *
     * @param worldName
     *     The name of the world of the structure.
     * @param pos
     *     The position of the structure's power block.
     */
    public void onStructureAddOrRemove(String worldName, Vector3Di pos)
    {
        final PowerBlockWorld powerBlockWorld = powerBlockWorlds.get(worldName);
        if (powerBlockWorld == null)
        {
            log.atWarning().log("Failed to load power blocks for world: '%s'.", worldName);
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
            log.atWarning().log("Failed to load power blocks for world: '%s'.", worldName);
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

    @Override
    public void onStructureDeletion(IStructureConst structure)
    {
        onStructureAddOrRemove(structure.getWorld().worldName(), structure.getPowerBlock());
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
        private final TimedCache<Long, CompletableFuture<PowerBlockChunk>> powerBlockChunks =
            TimedCache.<Long, CompletableFuture<PowerBlockChunk>>builder()
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
         * Checks if this world contains more than 0 structures (and power blocks by extension).
         *
         * @return True if this world contains more than 0 structures.
         */
        private boolean isBigDoorsWorld()
        {
            return isBigDoorsWorld;
        }

        private CompletableFuture<PowerBlockChunk> getPowerBlockChunk(Vector3Di loc)
        {
            if (!isBigDoorsWorld())
                return CompletableFuture.failedFuture(
                    new IllegalStateException("Cannot create PowerBlockChunks in non-BigDoors worlds!"));

            final long chunkId = Util.getChunkId(loc);

            return powerBlockChunks.computeIfAbsent(chunkId, chunkId0 ->
                databaseManager.getPowerBlockData(chunkId).thenApply(PowerBlockChunk::new));
        }

        /**
         * Gets all powerblocks in a chunk.
         *
         * @param loc
         *     The location in a chunk.
         * @return The UIDs of all structures whose powerblocks existing in the same chunk as the provided location.
         */
        private CompletableFuture<LongList> getPowerBlocksInChunk(Vector3Di loc)
        {
            if (!isBigDoorsWorld())
                return CompletableFuture.completedFuture(LongLists.emptyList());
            return getPowerBlockChunk(loc).thenApply(PowerBlockChunk::getPowerBlocks);
        }

        /**
         * Gets all UIDs of structures whose power blocks are in the given location.
         *
         * @param loc
         *     The location to check.
         * @return All UIDs of structures whose power blocks are in the given location.
         */
        private CompletableFuture<LongList> getPowerBlocksAtLocation(Vector3Di loc)
        {
            if (!isBigDoorsWorld())
                return CompletableFuture.completedFuture(LongLists.emptyList());

            return getPowerBlockChunk(loc).thenApply(chunk -> chunk.getPowerBlocks(loc));
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
         * Updates if this world contains more than 0 structures in the database (and power blocks by extension).
         * <p>
         * This differs from {@link #isBigDoorsWorld()} in that this method queries the database.
         */
        private void checkBigDoorsWorldStatus()
        {
            databaseManager.isBigDoorsWorld(worldName)
                           .thenAccept(result -> isBigDoorsWorld = result)
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
         * Map that contains all power blocks in this chunk mapped to the hash for their location in the chunk.
         * <p>
         * Key: Hashed locations (in chunk-space coordinates),
         * {@link Util#simpleChunkSpaceLocationHash(int, int, int)}.
         * <p>
         * Value: List of UIDs of all structures whose power block occupy this space.
         */
        private final Int2ObjectMap<LongList> powerBlocksMap;

        /**
         * List of all the UIDs that have their powerblock in this chunk.
         */
        @Getter
        private final LongList powerBlocks;

        public PowerBlockChunk(Int2ObjectMap<LongList> powerBlocksMap)
        {
            this.powerBlocksMap = powerBlocksMap;
            this.powerBlocks =
                LongImmutableList.toList(powerBlocksMap.values().stream().flatMapToLong(LongList::longStream));
        }

        /**
         * Gets all UIDs of structures whose power blocks are in the given location.
         *
         * @param loc
         *     The location to check.
         * @return All UIDs of structures whose power blocks are in the given location.
         */
        private LongList getPowerBlocks(Vector3Di loc)
        {
            if (!isPowerBlockChunk())
                return LongLists.emptyList();

            return powerBlocksMap.getOrDefault(Util.simpleChunkSpaceLocationHash(loc.x(), loc.y(), loc.z()),
                                               LongLists.emptyList());
        }

        /**
         * Updates if this chunk contains more than 0 power blocks.
         *
         * @return True if this chunk contains more than 0 structures.
         */
        private boolean isPowerBlockChunk()
        {
            return !powerBlocksMap.isEmpty();
        }
    }
}
