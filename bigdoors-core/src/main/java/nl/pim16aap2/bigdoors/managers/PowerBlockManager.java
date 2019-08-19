package nl.pim16aap2.bigdoors.managers;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.config.ConfigLoader;
import nl.pim16aap2.bigdoors.doors.DoorBase;
import nl.pim16aap2.bigdoors.util.IRestartable;
import nl.pim16aap2.bigdoors.util.IRestartableHolder;
import nl.pim16aap2.bigdoors.util.Restartable;
import nl.pim16aap2.bigdoors.util.TimedMapCache;
import nl.pim16aap2.bigdoors.util.Util;
import nl.pim16aap2.bigdoors.util.Vector3D;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Manages all power block interactions.
 *
 * @author Pim
 */
public final class PowerBlockManager extends Restartable
{
    @NotNull
    private final HashMap<UUID, PowerBlockWorld> powerBlockWorlds = new HashMap<>();
    @NotNull
    private final ConfigLoader config;
    @NotNull
    private final DatabaseManager databaseManager;

    /**
     * Empty, unmodifiable list that is returned when no power blocks are found in a given location.
     */
    @NotNull
    private static final List<Long> EMPTYLIST = Collections.unmodifiableList(new ArrayList<>());
    @Nullable
    private static PowerBlockManager instance;

    private PowerBlockManager(final @NotNull IRestartableHolder holder, final @NotNull ConfigLoader config,
                              final @NotNull DatabaseManager databaseManager)
    {
        super(holder);
        this.config = config;
        this.databaseManager = databaseManager;
    }

    /**
     * Initializes the {@link PowerBlockManager}. If it has already been initialized, it'll return that instance
     * instead.
     *
     * @param holder          The {@link IRestartableHolder} that manages this object.
     * @param config          The configuration of this plugin.
     * @param databaseManager The database manager to use for power block retrieval.
     * @return The instance of this {@link PowerBlockManager}.
     */
    @NotNull
    public static PowerBlockManager init(final @NotNull IRestartableHolder holder, final @NotNull ConfigLoader config,
                                         final @NotNull DatabaseManager databaseManager)
    {
        return (instance == null) ? instance = new PowerBlockManager(holder, config, databaseManager) :
               instance;
    }

    /**
     * Gets the instance of the {@link PowerBlockManager} if it exists.
     *
     * @return The instance of the {@link PowerBlockManager}.
     */
    @Nullable
    public static PowerBlockManager get()
    {
        return instance;
    }

    /**
     * Gets the {@link PowerBlockWorld} from its {@link UUID}. If it isn't in the cache yet, a new instance is created.
     *
     * @param worldUUID The UUID of the world.
     * @return The {@link PowerBlockWorld} with this {@link UUID}.
     */
    @NotNull
    private PowerBlockWorld getPowerBlockWorld(final @NotNull UUID worldUUID)
    {
        if (!powerBlockWorlds.containsKey(worldUUID))
            powerBlockWorlds.put(worldUUID, new PowerBlockWorld(worldUUID));
        return powerBlockWorlds.get(worldUUID);
    }

    /**
     * Gets all {@link DoorBase}s that have a powerblock at a location in a world.
     *
     * @param loc       The location.
     * @param worldUUID The {@link UUID} of the world.
     * @return All {@link DoorBase}s that have a powerblock at a location in a world.
     */
    @NotNull
    public List<DoorBase> doorsFromPowerBlockLoc(final @NotNull Vector3D loc, final @NotNull UUID worldUUID)
    {
        List<Long> doorUIDs = getPowerBlockWorld(worldUUID).getPowerBlocks(loc);
        List<DoorBase> doorBases = new ArrayList<>();
        doorUIDs.forEach(U -> databaseManager.getDoor(U).ifPresent(doorBases::add));
        return doorBases;
    }

    /**
     * Checks if a world is a BigDoors world. In other words, it checks if a world contains more than 0 doors.
     *
     * @param worldUUID The world.
     * @return True if the world contains at least 1 door.
     */
    public boolean isBigDoorsWorld(final @NotNull UUID worldUUID)
    {
        return getPowerBlockWorld(worldUUID).isBigDoorsWorld();
    }

    /**
     * Updates the position of the power block of a {@link DoorBase} in the database.
     *
     * @param doorUID   The UID of the {@link DoorBase}.
     * @param worldUUID The UUID of the world.
     * @param oldPos    The old position.
     * @param newPos    The new position.
     */
    public void updatePowerBlockLoc(final long doorUID, final @NotNull UUID worldUUID,
                                    final @NotNull Vector3D oldPos, final @NotNull Vector3D newPos)
    {
        databaseManager.updatePowerBlockLoc(doorUID, newPos, worldUUID);
        PowerBlockWorld powerBlockWorld = getPowerBlockWorld(worldUUID);

        // Invalidate both the old and the new positions.
        powerBlockWorld.invalidatePosition(oldPos);
        powerBlockWorld.invalidatePosition(newPos);
    }

    /**
     * Invalidates the cache for when a door is either added to a world or removed from it.
     *
     * @param worldUUID The world of the door.
     * @param pos       The position of the door's power block.
     */
    public void onDoorAddOrRemove(final @NotNull UUID worldUUID, final @NotNull Vector3D pos)
    {
        PowerBlockWorld powerBlockWorld = getPowerBlockWorld(worldUUID);
        powerBlockWorld.invalidatePosition(pos);
        powerBlockWorld.checkBigDoorsWorldStatus();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void restart()
    {
        powerBlockWorlds.values().forEach(PowerBlockWorld::restart);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void shutdown()
    {
        powerBlockWorlds.values().forEach(PowerBlockWorld::shutdown);
    }

    @NotNull
    private ConfigLoader getConfig()
    {
        return config;
    }

    /**
     * Represents a world that may or may not contain any power blocks.
     *
     * @author Pim
     */
    private final class PowerBlockWorld implements IRestartable
    {
        @NotNull
        private final UUID world;
        private boolean isBigDoorsWorld;
        /**
         * TimedCache of all {@link PowerBlockChunk}s in this world.
         * <p>
         * Key: Simple chunk hash, {@link Util#simpleChunkHashFromLocation(int, int)}.
         * <p>
         * Value: The {@link PowerBlockChunk}s.
         */
        @NotNull
        private final TimedMapCache<Long, PowerBlockChunk> powerBlockChunks =
            new TimedMapCache<>(BigDoors.get(), HashMap::new, getConfig().cacheTimeout());

        private PowerBlockWorld(final @NotNull UUID world)
        {
            this.world = world;
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
        @NotNull
        private List<Long> getPowerBlocks(final @NotNull Vector3D loc)
        {
            if (!isBigDoorsWorld())
                return EMPTYLIST;

            final long chunkHash = Util.simpleChunkHashFromLocation(loc.getX(), loc.getZ());
            if (!powerBlockChunks.containsKey(chunkHash))
                powerBlockChunks.put(chunkHash, new PowerBlockChunk(chunkHash));

            return powerBlockChunks.get(chunkHash).getPowerBlocks(loc);
        }

        /**
         * Removes the chunk of a position from the cache.
         *
         * @param pos The position.
         */
        private void invalidatePosition(final @NotNull Vector3D pos)
        {
            powerBlockChunks.remove(Util.simpleChunkHashFromLocation(pos.getX(), pos.getZ()));
        }

        /**
         * Updates if this world contains more than 0 doors in the database (and power blocks by extension).
         * <p>
         * This differs from {@link #isBigDoorsWorld()} in that this method queries the database.
         *
         * @return True if this world contains more than 0 doors.
         */
        private boolean checkBigDoorsWorldStatus()
        {
            return isBigDoorsWorld = databaseManager.isBigDoorsWorld(world);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void restart()
        {
            powerBlockChunks.restart();
        }

        /**
         * {@inheritDoc}
         */
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
    private final class PowerBlockChunk
    {
        private final long chunkHash;
        /**
         * Map that contains all power blocks in this chunk.
         * <p>
         * Key: Hashed locations (in chunk-space coordinates), {@link Util#simpleChunkSpaceLocationhash(int, int,
         * int)}.
         * <p>
         * Value: List of UIDs of all doors whose power block occupy this space.
         */
        private Map<Integer, List<Long>> powerBlocks;

        private PowerBlockChunk(final long chunkHash)
        {
            this.chunkHash = chunkHash;
            checkPowerBlockChunkStatus();
        }

        /**
         * Gets all UIDs of doors whose power blocks are in the given location.
         *
         * @param loc The location to check.
         * @return All UIDs of doors whose power blocks are in the given location.
         */
        @NotNull
        private List<Long> getPowerBlocks(final @NotNull Vector3D loc)
        {
            if (!isPowerBlockChunk())
                return EMPTYLIST;

            return powerBlocks.getOrDefault(Util.simpleChunkSpaceLocationhash(loc.getX(), loc.getY(), loc.getZ()),
                                            EMPTYLIST);
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

        /**
         * Updates the power blocks mapped in this
         * <p>
         * This differs from {@link #isPowerBlockChunk()} in that this method queries the database.
         *
         * @return True if this chunk contains more than 0 doors.
         */
        private boolean checkPowerBlockChunkStatus()
        {
            powerBlocks = databaseManager.getPowerBlockData(chunkHash);
            return powerBlocks.size() > 0;
        }
    }
}
