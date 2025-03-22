package nl.pim16aap2.animatedarchitecture.core.managers;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.longs.LongImmutableList;
import it.unimi.dsi.fastutil.longs.LongList;
import it.unimi.dsi.fastutil.longs.LongLists;
import lombok.Getter;
import lombok.experimental.ExtensionMethod;
import lombok.extern.flogger.Flogger;
import nl.pim16aap2.animatedarchitecture.core.api.IConfig;
import nl.pim16aap2.animatedarchitecture.core.api.IExecutor;
import nl.pim16aap2.animatedarchitecture.core.api.ILocation;
import nl.pim16aap2.animatedarchitecture.core.api.IWorld;
import nl.pim16aap2.animatedarchitecture.core.api.restartable.Restartable;
import nl.pim16aap2.animatedarchitecture.core.api.restartable.RestartableHolder;
import nl.pim16aap2.animatedarchitecture.core.data.cache.timed.TimedCache;
import nl.pim16aap2.animatedarchitecture.core.structures.IStructureConst;
import nl.pim16aap2.animatedarchitecture.core.structures.Structure;
import nl.pim16aap2.animatedarchitecture.core.util.CompletableFutureExtensions;
import nl.pim16aap2.animatedarchitecture.core.util.FutureUtil;
import nl.pim16aap2.animatedarchitecture.core.util.LocationUtil;
import nl.pim16aap2.animatedarchitecture.core.util.vector.Vector2Di;
import nl.pim16aap2.animatedarchitecture.core.util.vector.Vector3Di;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages all power block interactions.
 */
@Singleton
@Flogger
@ExtensionMethod(CompletableFutureExtensions.class)
public final class PowerBlockManager extends Restartable implements StructureDeletionManager.IDeletionListener
{
    private final Map<String, PowerBlockWorld> powerBlockWorlds = new ConcurrentHashMap<>();
    private final IExecutor executor;
    private final IConfig config;
    private final DatabaseManager databaseManager;

    @Inject
    PowerBlockManager(
        RestartableHolder restartableHolder,
        IExecutor executor,
        IConfig config,
        DatabaseManager databaseManager,
        StructureDeletionManager structureDeletionManager)
    {
        super(restartableHolder);
        this.executor = executor;

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
     * Gets all {@link Structure}s that have a powerblock at a location in a world.
     *
     * @param location
     *     The location.
     * @return All {@link Structure}s that have a powerblock at a location in a world.
     */
    public CompletableFuture<List<Structure>> structuresFromPowerBlockLoc(ILocation location)
    {
        return this.structuresFromPowerBlockLoc(location.getPosition(), location.getWorld().worldName());
    }

    /**
     * Gets all {@link Structure}s that have a powerblock at a location in a world.
     *
     * @param loc
     *     The location.
     * @param world
     *     The world.
     * @return All {@link Structure}s that have a powerblock at a location in a world.
     */
    public CompletableFuture<List<Structure>> structuresFromPowerBlockLoc(Vector3Di loc, IWorld world)
    {
        return this.structuresFromPowerBlockLoc(loc, world.worldName());
    }

    /**
     * Gets all {@link Structure}s that have a powerblock at a location in a world.
     *
     * @param loc
     *     The location.
     * @param worldName
     *     The name of the world.
     * @return All {@link Structure}s that have a powerblock at a location in a world.
     */
    public CompletableFuture<List<Structure>> structuresFromPowerBlockLoc(Vector3Di loc, String worldName)
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
    public CompletableFuture<List<Structure>> structuresInChunk(Vector3Di loc, String worldName)
    {
        final PowerBlockWorld powerBlockWorld = powerBlockWorlds.get(worldName);
        if (powerBlockWorld == null)
        {
            log.atWarning().log("Failed to load power blocks for world: '%s'.", worldName);
            return CompletableFuture.completedFuture(Collections.emptyList());
        }
        return mapUidsToStructures(powerBlockWorld.getPowerBlocksInChunk(loc));
    }

    private CompletableFuture<List<Structure>> mapUidsToStructures(CompletableFuture<LongList> uids)
    {
        return uids
            .thenApplyAsync(
                lst -> lst.longStream().mapToObj(databaseManager::getStructure).toList(),
                executor.getVirtualExecutor())
            .thenCompose(FutureUtil::getAllCompletableFutureResults)
            .thenApply(lst -> lst.stream().filter(Optional::isPresent).map(Optional::get).toList())
            .withExceptionContext(() -> "Mapping UIDs to structures: " + uids);
    }

    /**
     * Checks if a world is a AnimatedArchitecture world. In other words, it checks if a world contains more than 0
     * structures.
     *
     * @param worldName
     *     The name of the world.
     * @return True if the world contains at least 1 structure.
     */
    public boolean isAnimatedArchitectureWorld(String worldName)
    {
        final PowerBlockWorld powerBlockWorld = powerBlockWorlds.get(worldName);
        if (powerBlockWorld == null)
        {
            log.atWarning().log("Failed to load power blocks for world: '%s'.", worldName);
            return false;
        }
        return powerBlockWorld.isAnimatedArchitectureWorld();
    }

    /**
     * Invalidates the cache for a chunk that contains a position.
     *
     * @param worldName
     *     The name of the world of the structure.
     * @param pos
     *     The position of the structure's power block.
     */
    public void invalidateChunkAt(String worldName, Vector3Di pos)
    {
        final PowerBlockWorld powerBlockWorld = powerBlockWorlds.get(worldName);
        if (powerBlockWorld == null)
        {
            log.atWarning().log("Failed to load power blocks for world: '%s'.", worldName);
            return;
        }
        powerBlockWorld.invalidatePosition(pos);
        powerBlockWorld.checkAnimatedArchitectureWorldStatus();
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
        powerBlockWorld.checkAnimatedArchitectureWorldStatus();
    }

    @Override
    public void shutDown()
    {
        powerBlockWorlds.values().forEach(PowerBlockWorld::clear);
    }

    @Override
    public void onStructureDeletion(IStructureConst structure)
    {
        invalidateChunkAt(structure.getWorld().worldName(), structure.getPowerBlock());
    }

    /**
     * Represents a world that may or may not contain any power blocks.
     */
    private final class PowerBlockWorld
    {
        private final String worldName;
        private volatile boolean isAnimatedArchitectureWorld = false;

        /**
         * TimedCache of all {@link PowerBlockChunk}s in this world.
         * <p>
         * Key: chunkId: {@link LocationUtil#getChunkId(Vector3Di)}.
         * <p>
         * Value: The {@link PowerBlockChunk}s.
         */
        private final TimedCache<Long, CompletableFuture<PowerBlockChunk>> powerBlockChunks =
            TimedCache.<Long, CompletableFuture<PowerBlockChunk>>builder()
                .timeOut(Duration.ofMinutes(config.cacheTimeout()))
                .cleanup(Duration.ofMinutes(Math.max(1, config.cacheTimeout())))
                .softReference(true)
                .refresh(true)
                .build();

        private PowerBlockWorld(String worldName)
        {
            this.worldName = worldName;
            checkAnimatedArchitectureWorldStatus();
        }

        /**
         * Checks if this world contains more than 0 structures (and power blocks by extension).
         *
         * @return True if this world contains more than 0 structures.
         */
        private boolean isAnimatedArchitectureWorld()
        {
            return isAnimatedArchitectureWorld;
        }

        private CompletableFuture<PowerBlockChunk> getPowerBlockChunk(Vector3Di loc)
        {
            if (!isAnimatedArchitectureWorld())
                return CompletableFuture.failedFuture(
                    new IllegalStateException("Cannot create PowerBlockChunks in non-AnimatedArchitecture worlds!"));

            final long chunkId = LocationUtil.getChunkId(loc);

            return Objects.requireNonNull(powerBlockChunks.computeIfAbsent(chunkId, this::findPowerBlockChunk));
        }

        private CompletableFuture<PowerBlockManager.PowerBlockChunk> findPowerBlockChunk(long chunkId)
        {
            return databaseManager
                .getPowerBlockData(chunkId)
                .thenApply(PowerBlockChunk::new)
                .exceptionally(ex ->
                {
                    log.atSevere().withCause(ex).log("Failed to find power block chunk with id: %d", chunkId);
                    return null;
                });
        }

        /**
         * Gets all power blocks in a chunk.
         *
         * @param loc
         *     The location in a chunk.
         * @return The UIDs of all structures whose powerblocks existing in the same chunk as the provided location.
         */
        private CompletableFuture<LongList> getPowerBlocksInChunk(Vector3Di loc)
        {
            if (!isAnimatedArchitectureWorld())
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
            if (!isAnimatedArchitectureWorld())
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
            powerBlockChunks.remove(LocationUtil.getChunkId(pos));
        }

        /**
         * Updates if this world contains more than 0 structures in the database (and power blocks by extension).
         * <p>
         * This differs from {@link #isAnimatedArchitectureWorld()} in that this method queries the database.
         */
        private void checkAnimatedArchitectureWorldStatus()
        {
            databaseManager
                .isAnimatedArchitectureWorld(worldName)
                .thenAccept(result -> isAnimatedArchitectureWorld = result)
                .handleExceptional(ex ->
                    log.atSevere().withCause(ex).log(
                        "Failed to check if world '%s' is an AnimatedArchitecture world.",
                        worldName)
                );
        }

        void clear()
        {
            powerBlockChunks.clear();
        }
    }

    /**
     * Represents a chunk that may or may not contain any power blocks.
     */
    private static final class PowerBlockChunk
    {
        /**
         * Map that contains all power blocks in this chunk mapped to the hash for their location in the chunk.
         * <p>
         * Key: Hashed locations (in chunk-space coordinates),
         * {@link LocationUtil#simpleChunkSpaceLocationHash(int, int, int)}.
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
            this.powerBlocks = LongImmutableList.toList(
                powerBlocksMap.values().stream().flatMapToLong(LongList::longStream));
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

            return powerBlocksMap.getOrDefault(
                LocationUtil.simpleChunkSpaceLocationHash(loc.x(), loc.y(), loc.z()),
                LongLists.emptyList()
            );
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
