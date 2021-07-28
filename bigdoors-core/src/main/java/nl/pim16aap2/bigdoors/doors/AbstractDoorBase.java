package nl.pim16aap2.bigdoors.doors;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.val;
import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.annotations.Initializer;
import nl.pim16aap2.bigdoors.api.IChunkManager;
import nl.pim16aap2.bigdoors.api.IMessageable;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.api.IPWorld;
import nl.pim16aap2.bigdoors.doortypes.DoorType;
import nl.pim16aap2.bigdoors.events.dooraction.DoorActionCause;
import nl.pim16aap2.bigdoors.events.dooraction.DoorActionType;
import nl.pim16aap2.bigdoors.events.dooraction.IDoorEventTogglePrepare;
import nl.pim16aap2.bigdoors.managers.DatabaseManager;
import nl.pim16aap2.bigdoors.managers.DoorActivityManager;
import nl.pim16aap2.bigdoors.moveblocks.BlockMover;
import nl.pim16aap2.bigdoors.util.Cuboid;
import nl.pim16aap2.bigdoors.util.DoorOwner;
import nl.pim16aap2.bigdoors.util.DoorToggleResult;
import nl.pim16aap2.bigdoors.util.Limit;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import nl.pim16aap2.bigdoors.util.Util;
import nl.pim16aap2.bigdoors.util.vector.Vector2Di;
import nl.pim16aap2.bigdoors.util.vector.Vector3Di;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.logging.Level;

/**
 * Represents an unspecialized door.
 *
 * @author Pim
 */
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = false)
public abstract class AbstractDoorBase extends DatabaseManager.FriendDoorAccessor implements IDoorBase
{
    @Getter
    private final long doorUID;

    @Getter
    private final @NotNull IPWorld world;

    @Getter
    private volatile @NotNull Vector3Di engine;

    @Getter
    private volatile @NotNull Vector2Di engineChunk;

    @Getter
    private volatile @NotNull Vector3Di powerBlock;

    @Getter
    @Setter
    private volatile @NotNull String name;

    private volatile Cuboid cuboid;

    @Getter
    @Setter
    private volatile boolean open;

    @Getter
    @Setter
    private volatile @NotNull RotateDirection openDir;

    /**
     * Represents the locked status of this door. True = locked, False = unlocked.
     */
    @Getter
    @Setter
    private volatile boolean locked;

    @EqualsAndHashCode.Exclude
    // This is a ConcurrentHashMap to ensure serialization uses the correct type.
    private final @NotNull ConcurrentHashMap<@NotNull UUID, @NotNull DoorOwner> doorOwners;

    @Getter
    private final @NotNull DoorOwner primeOwner;

    private final @Nullable DoorSerializer<?> serializer = getDoorType().getDoorSerializer().orElse(null);

    /**
     * Min and Max Vector2Di coordinates of the range of Vector2Dis that this {@link AbstractDoorBase} might interact
     * with.
     */
    @EqualsAndHashCode.Exclude
    private @Nullable Vector2Di minChunkCoords = null, maxChunkCoords = null;

    @Override
    protected final void addOwner(final @NotNull UUID uuid, final @NotNull DoorOwner doorOwner)
    {
        if (doorOwner.permission() == 0)
        {
            BigDoors.get().getPLogger().logThrowable(new IllegalArgumentException(
                "Failed to add owner: " + doorOwner.pPlayerData() + " as owner to door: " +
                    getDoorUID() +
                    " because a permission level of 0 is not allowed!"));
            return;
        }
        doorOwners.put(uuid, doorOwner);
    }

    @Override
    protected final boolean removeOwner(final @NotNull UUID uuid)
    {
        if (primeOwner.pPlayerData().getUUID().equals(uuid))
        {
            BigDoors.get().getPLogger().logThrowable(new IllegalArgumentException(
                "Failed to remove owner: " + primeOwner.pPlayerData() + " as owner from door: " +
                    getDoorUID() + " because removing an owner with a permission level of 0 is not allowed!"));
            return false;
        }
        return doorOwners.remove(uuid) != null;
    }

    /**
     * Constructs a new {@link AbstractDoorBase}.
     */
    protected AbstractDoorBase(final @NotNull DoorData doorData)
    {
        init(doorData);
        doorUID = doorData.getUid();
        world = doorData.getWorld();
        primeOwner = doorData.getPrimeOwner();
        doorOwners = doorData.getDoorOwners();
    }

    @Initializer
    private void init(final @NotNull DoorData doorData)
    {
        BigDoors.get().getPLogger().logMessage(Level.FINEST, "Instantiating door: " + doorUID);
        if (doorUID > 0 && !BigDoors.get().getDoorRegistry().registerDoor(new Registerable()))
        {
            final @NotNull IllegalStateException exception = new IllegalStateException(
                "Tried to create new door \"" + doorUID + "\" while it is already registered!");
            BigDoors.get().getPLogger().logThrowableSilently(exception);
            throw exception;
        }

        name = doorData.getName();
        cuboid = doorData.getCuboid();
        engine = doorData.getEngine();
        engineChunk = Util.getChunkCoords(engine);
        powerBlock = doorData.getPowerBlock();
        open = doorData.isOpen();
        openDir = doorData.getOpenDirection();
        locked = doorData.isLocked();
    }

    /**
     * Synchronizes all data of this door with the database.
     *
     * @return True if the synchronization was successful.
     */
    public final synchronized @NotNull CompletableFuture<Boolean> syncData()
    {
        if (serializer == null)
        {
            BigDoors.get().getPLogger()
                    .severe("Failed to sync data for door: " + getBasicInfo() + "! Reason: Serializer unavailable!");
            return CompletableFuture.completedFuture(false);
        }

        try
        {
            return BigDoors.get().getDatabaseManager()
                           .syncDoorData(getSimpleDoorDataCopy(), serializer.serialize(this));
        }
        catch (Throwable t)
        {
            BigDoors.get().getPLogger().logThrowable(t, "Failed to sync data for door: " + getBasicInfo());
        }
        return CompletableFuture.completedFuture(false);
    }

    @Override
    public @NotNull Cuboid getCuboid()
    {
        return cuboid;
    }

    /**
     * Obtains a copy of the {@link DoorData} the describes this door.
     * <p>
     * Note that this creates a deep copy of the DoorData <u><b>including</u></b><b> its {@link DoorOwner}s</b>, so use
     * it sparingly.
     *
     * @return A copy of the {@link DoorData} the describes this door.
     */
    public synchronized @NotNull DoorData getDoorDataCopy()
    {
        return new DoorData(doorUID, name, cuboid, engine, powerBlock,
                            world.clone(), open, locked, openDir, primeOwner, getDoorOwnersCopy());
    }

    /**
     * Obtains a simple copy of the {@link DoorData} the describes this door.
     * <p>
     * Note that this creates a deep copy of the DoorData <u><b>excluding</u></b><b> its {@link DoorOwner}s</b>, so use
     * it sparingly.
     *
     * @return A copy of the {@link DoorData} the describes this door.
     */
    public synchronized @NotNull SimpleDoorData getSimpleDoorDataCopy()
    {
        return new SimpleDoorData(doorUID, name, cuboid, engine, powerBlock,
                                  world.clone(), open, locked, openDir, primeOwner);
    }

    /**
     * Gets the {@link DoorType} of this door.
     *
     * @return The {@link DoorType} of this door.
     */
    public abstract @NotNull DoorType getDoorType();

    @Deprecated
    @Override
    public boolean isPowerBlockActive()
    {
        // FIXME: Cleanup
        Vector3Di powerBlockChunkSpaceCoords = Util.getChunkSpacePosition(getPowerBlock());
        Vector2Di powerBlockChunk = Util.getChunkCoords(getPowerBlock());
        if (BigDoors.get().getPlatform().getChunkManager().load(getWorld(), powerBlockChunk) ==
            IChunkManager.ChunkLoadResult.FAIL)
        {
            BigDoors.get().getPLogger()
                    .logThrowable(new IllegalStateException("Failed to load chunk at: " + powerBlockChunk));
            return false;
        }

        // TODO: Make sure that all corners around the block are also loaded (to check redstone).
        //       Might have to load up to 3 chunks.
        return BigDoors.get().getPlatform().getPowerBlockRedstoneManager()
                       .isBlockPowered(getWorld(), getPowerBlock());
    }

    /**
     * Attempts to toggle a door. Think twice before using this method. Instead, please look at {@link
     * DoorOpener#animateDoorAsync(AbstractDoorBase, DoorActionCause, IMessageable, IPPlayer, double, boolean,
     * DoorActionType)}.
     *
     * @param cause           What caused this action.
     * @param messageReceiver Who will receive any messages that have to be sent.
     * @param responsible     Who is responsible for this door. Either the player who directly toggled it (via a command
     *                        or the GUI), or the prime owner when this data is not available.
     * @param time            The amount of time this {@link AbstractDoorBase} will try to use to move. The maximum
     *                        speed is limited, so at a certain point lower values will not increase door speed.
     * @param skipAnimation   If the {@link AbstractDoorBase} should be opened instantly (i.e. skip animation) or not.
     * @param actionType      The type of action.
     * @return The result of the attempt.
     */
    // TODO: When aborting the toggle, send the messages to the messageReceiver, not to the responsible player.
    //       These aren't necessarily the same entity.
    final synchronized @NotNull DoorToggleResult toggle(final @NotNull DoorActionCause cause,
                                                        final @NotNull IMessageable messageReceiver,
                                                        final @NotNull IPPlayer responsible, final double time,
                                                        boolean skipAnimation, final @NotNull DoorActionType actionType)
    {
        if (!BigDoors.get().getPlatform().isMainThread(Thread.currentThread().getId()))
        {
            BigDoors.get().getPLogger().logThrowable(
                new IllegalStateException("Doors must be toggled on the main thread!"));
            return DoorToggleResult.ERROR;
        }

        if (openDir == RotateDirection.NONE)
        {
            BigDoors.get().getPLogger().logThrowable(new IllegalStateException("OpenDir cannot be NONE!"));
            return DoorToggleResult.ERROR;
        }

        if (!BigDoors.get().getDoorRegistry().isRegistered(this))
            return DoorOpeningUtility.abort(this, DoorToggleResult.INSTANCE_UNREGISTERED, cause, responsible);

        if (skipAnimation && !canSkipAnimation())
            return DoorOpeningUtility.abort(this, DoorToggleResult.ERROR, cause, responsible);

        final @NotNull DoorToggleResult isOpenable = DoorOpeningUtility.canBeToggled(this, cause, actionType);
        if (isOpenable != DoorToggleResult.SUCCESS)
            return DoorOpeningUtility.abort(this, isOpenable, cause, responsible);

        if (BigDoors.get().getLimitsManager().exceedsLimit(responsible, Limit.DOOR_SIZE, getBlockCount()))
            return DoorOpeningUtility.abort(this, DoorToggleResult.TOOBIG, cause, responsible);

        final @NotNull Optional<Cuboid> newCuboid = getPotentialNewCoordinates();

        if (newCuboid.isEmpty())
            return DoorOpeningUtility.abort(this, DoorToggleResult.ERROR, cause, responsible);

        final @NotNull IDoorEventTogglePrepare prepareEvent = BigDoors.get().getPlatform().getBigDoorsEventFactory()
                                                                      .createTogglePrepareEvent(this, cause, actionType,
                                                                                                responsible, time,
                                                                                                skipAnimation,
                                                                                                newCuboid.get());
        BigDoors.get().getPlatform().callDoorEvent(prepareEvent);
        if (prepareEvent.isCancelled())
            return DoorOpeningUtility.abort(this, DoorToggleResult.CANCELLED, cause, responsible);

        final @Nullable IPPlayer responsiblePlayer = cause.equals(DoorActionCause.PLAYER) ? responsible : null;
        if (!DoorOpeningUtility.isLocationEmpty(newCuboid.get(), cuboid, responsiblePlayer, getWorld()))
            return DoorOpeningUtility.abort(this, DoorToggleResult.OBSTRUCTED, cause, responsible);

        if (!DoorOpeningUtility.canBreakBlocksBetweenLocs(this, newCuboid.get(), responsible))
            return DoorOpeningUtility.abort(this, DoorToggleResult.NOPERMISSION, cause, responsible);

        CompletableFuture<Boolean> scheduled = BigDoors.get().getPlatform().getPExecutor().supplyOnMainThread(
            () -> registerBlockMover(cause, time, skipAnimation, newCuboid.get(), responsible, actionType));

        if (!scheduled.join())
            return DoorToggleResult.ERROR;

        BigDoors.get().getPlatform().callDoorEvent(BigDoors.get().getPlatform().getBigDoorsEventFactory()
                                                           .createToggleStartEvent(this, cause, actionType, responsible,
                                                                                   time, skipAnimation,
                                                                                   newCuboid.get()));
        return DoorToggleResult.SUCCESS;
    }

    @Override
    public final void onRedstoneChange(final int newCurrent)
    {
        final IPPlayer player = BigDoors.get().getPlatform().getPPlayerFactory()
                                        .create(getPrimeOwner().pPlayerData());

        final @Nullable DoorActionType doorActionType;
        if (newCurrent == 0 && isCloseable())
            doorActionType = DoorActionType.CLOSE;
        else if (newCurrent > 0 && isOpenable())
            doorActionType = DoorActionType.CLOSE;
        else
            doorActionType = null;
        if (doorActionType != null)
            BigDoors.get().getDoorOpener()
                    .animateDoorAsync(this, DoorActionCause.REDSTONE,
                                      BigDoors.get().getPlatform().getMessageableServer(),
                                      player, 0.0D, false, DoorActionType.CLOSE);
    }

    @Override
    public boolean isOpenable()
    {
        return !open;
    }

    @Override
    public boolean isCloseable()
    {
        return open;
    }

    /**
     * Gets the {@link Supplier} for the {@link BlockMover} for this type.
     * <p>
     * This method MUST BE CALLED FROM THE MAIN THREAD! (Because of MC, spawning entities needs to happen
     * synchronously)
     *
     * @param cause         What caused this action.
     * @param time          The amount of time this {@link AbstractDoorBase} will try to use to move. The maximum speed
     *                      is limited, so at a certain point lower values will not increase door speed.
     * @param skipAnimation If the {@link AbstractDoorBase} should be opened instantly (i.e. skip animation) or not.
     * @param newCuboid     The {@link Cuboid} representing the area the door will take up after the toggle.
     * @param responsible   The {@link IPPlayer} responsible for the door action.
     * @param actionType    The type of action that will be performed by the BlockMover.
     * @return The {@link BlockMover} for this class.
     */
    protected abstract @NotNull BlockMover constructBlockMover(final @NotNull DoorActionCause cause,
                                                               final double time, final boolean skipAnimation,
                                                               final @NotNull Cuboid newCuboid,
                                                               final @NotNull IPPlayer responsible,
                                                               final @NotNull DoorActionType actionType)
        throws Exception;

    /**
     * Registers a {@link BlockMover} with the {@link DoorActivityManager}.
     * <p>
     * This method MUST BE CALLED FROM THE MAIN THREAD! (Because of MC, spawning entities needs to happen
     * synchronously)
     *
     * @param cause         What caused this action.
     * @param time          The amount of time this {@link AbstractDoorBase} will try to use to move. The maximum speed
     *                      is limited, so at a certain point lower values will not increase door speed.
     * @param skipAnimation If the {@link AbstractDoorBase} should be opened instantly (i.e. skip animation) or not.
     * @param newCuboid     The {@link Cuboid} representing the area the door will take up after the toggle.
     * @param responsible   The {@link IPPlayer} responsible for the door action.
     * @param actionType    The type of action that will be performed by the BlockMover.
     * @return True when everything went all right, otherwise false.
     */
    private synchronized boolean registerBlockMover(final @NotNull DoorActionCause cause,
                                                    final double time, final boolean skipAnimation,
                                                    final @NotNull Cuboid newCuboid,
                                                    final @NotNull IPPlayer responsible,
                                                    final @NotNull DoorActionType actionType)
    {
        if (!BigDoors.get().getPlatform().isMainThread(Thread.currentThread().getId()))
        {
            BigDoors.get().getDoorActivityManager().setDoorAvailable(getDoorUID());
            BigDoors.get().getPLogger().logThrowable(
                new IllegalThreadStateException("BlockMovers must be instantiated on the main thread!"));
            return true;
        }

        try
        {
            DoorOpeningUtility.registerBlockMover(constructBlockMover(cause, time, skipAnimation, newCuboid,
                                                                      responsible, actionType));
        }
        catch (Exception e)
        {
            BigDoors.get().getPLogger().logThrowable(e);
            return false;
        }
        return true;
    }

    /**
     * @deprecated This method needs to be replaced with the 'new' chunk-system from v1.
     */
    @Deprecated
    @Override
    public @NotNull Vector2Di[] calculateCurrentChunkRange()
    {
        final @NotNull Vector2Di minChunk = Util.getChunkCoords(cuboid.getMin());
        final @NotNull Vector2Di maxChunk = Util.getChunkCoords(cuboid.getMax());

        return new Vector2Di[]{new Vector2Di(minChunk.x(), minChunk.y()),
                               new Vector2Di(maxChunk.x(), maxChunk.y())};
    }

    /**
     * @deprecated This method needs to be replaced with the 'new' chunk-system from v1.
     */
    @Deprecated
    @Override
    public final boolean chunkInRange(final @NotNull IPWorld otherWorld, final @NotNull Vector2Di chunk)
    {
        if (!otherWorld.equals(getWorld()))
            return false;

        verifyChunkRange();
        Util.requireNonNull(minChunkCoords, "minChunkCoords");
        Util.requireNonNull(maxChunkCoords, "maxChunkCoords");
        return Util.between(chunk.x(), minChunkCoords.x(), maxChunkCoords.x()) &&
            Util.between(chunk.y(), minChunkCoords.y(), maxChunkCoords.y());
        // It's a Vector2D, so there's no z. Instead of Z, use the second value (Y).
    }

    /**
     * Verify that the ranges of Vector2Di coordinates are available. if not, {@link #calculateChunkRange()} is called
     * to calculate and store them.
     *
     * @deprecated This method needs to be replaced with the 'new' chunk-system from v1.
     */
    @Deprecated
    private void verifyChunkRange()
    {
        if (maxChunkCoords == null || minChunkCoords == null)
        {
            final @NotNull Vector2Di[] range = calculateChunkRange();
            minChunkCoords = range[0];
            maxChunkCoords = range[1];
        }
    }

    @Override
    public @NotNull List<DoorOwner> getDoorOwners()
    {
        final List<DoorOwner> ret = new ArrayList<>(doorOwners.size());
        ret.addAll(doorOwners.values());
        return ret;
    }

    protected @NotNull Map<UUID, DoorOwner> getDoorOwnersCopy()
    {
        final @NotNull Map<UUID, DoorOwner> copy = new HashMap<>(doorOwners.size());
        doorOwners.forEach(copy::put);
        return copy;
    }

    @Override
    public @NotNull Optional<DoorOwner> getDoorOwner(final @NotNull IPPlayer player)
    {
        return getDoorOwner(player.getUUID());
    }

    @Override
    public @NotNull Optional<DoorOwner> getDoorOwner(final @NotNull UUID uuid)
    {
        return Optional.ofNullable(doorOwners.get(uuid));
    }

    @Override
    public @NotNull AbstractDoorBase setCoordinates(final @NotNull Cuboid newCuboid)
    {
        cuboid = newCuboid;
        return this;
    }

    @Override
    public final @NotNull AbstractDoorBase setCoordinates(final @NotNull Vector3Di posA,
                                                          final @NotNull Vector3Di posB)
    {
        cuboid = new Cuboid(posA, posB);
        return this;
    }

    @Override
    public @NotNull Vector3Di getMinimum()
    {
        return cuboid.getMin();
    }

    @Override
    public @NotNull Vector3Di getMaximum()
    {
        return cuboid.getMax();
    }

    /**
     * Invalidate Vector2DiRange for this door.
     * <p>
     * This function should be called whenever certain aspects change that affect the way/distance this door might move.
     * E.g. the open direction.
     *
     * @deprecated This method needs to be replaced with the 'new' chunk-system from v1.
     */
    @Deprecated
    protected void invalidateChunkRange()
    {
        maxChunkCoords = null;
        minChunkCoords = null;
    }

    /**
     * @deprecated This method needs to be replaced with the 'new' chunk-system from v1.
     */
    @Deprecated
    @Override
    public final @NotNull Vector2Di[] getChunkRange()
    {
        verifyChunkRange();
        return new Vector2Di[]{minChunkCoords, maxChunkCoords};
    }

    @Override
    public @NotNull Vector3Di getDimensions()
    {
        return cuboid.getDimensions();
    }

    @Override
    public final synchronized @NotNull AbstractDoorBase setEngine(final @NotNull Vector3Di pos)
    {
        engine = pos;
        engineChunk = Util.getChunkCoords(pos);
        return this;
    }

    @Override
    public final @NotNull AbstractDoorBase setPowerBlockPosition(final @NotNull Vector3Di pos)
    {
        powerBlock = pos;
        invalidateChunkRange();
        return this;
    }

    @Override
    public final int getBlockCount()
    {
        return cuboid.getVolume();
    }

    @Override
    public final synchronized long getSimplePowerBlockChunkHash()
    {
        return Util.simpleChunkHashFromLocation(powerBlock.x(), powerBlock.z());
    }

    @Override
    public synchronized @NotNull String getBasicInfo()
    {
        return doorUID + " (" + getPrimeOwner() + ") - " + getDoorType().getSimpleName() + ": " + name;
    }

    /**
     * @return String with (almost) all data of this door.
     */
    @Override
    public synchronized @NotNull String toString()
    {
        StringBuilder builder = new StringBuilder();
        builder.append(doorUID).append(": ").append(name).append("\n");
        builder.append("Type: ").append(getDoorType()).append("\n");
        builder.append("Cuboid: ").append(cuboid.toString()).append(", Engine: ").append(engine).append("\n");
        builder.append("PowerBlock position: ").append(powerBlock).append(". Hash: ")
               .append(getSimplePowerBlockChunkHash()).append("\n");
        builder.append("World: ").append(getWorld().getWorldName()).append("\n");
        builder.append("This door is ").append((locked ? "" : "NOT ")).append("locked. ").append("\n");
        builder.append("This door is ").append((open ? "open.\n" : "closed.\n"));
        builder.append("OpenDir: ").append(openDir.name()).append("\n");

        builder.append("\nType-specific data:\n");
        val serializerOpt = getDoorType().getDoorSerializer();
        if (serializerOpt.isEmpty())
            builder.append("Invalid serializer!\n");
        else
            builder.append(serializerOpt.get().toString(this));

        return builder.toString();
    }

    /**
     * POD class that stores all the data needed for basic door initialization.
     * <p>
     * This type is called 'simple', as it doesn't include the list of all {@link DoorOwner}s. If you need that, use an
     * {@link DoorData} instead.
     *
     * @author Pim
     */
    @AllArgsConstructor
    @Getter
    public static class SimpleDoorData
    {
        /**
         * The UID of this door.
         */
        final long uid;

        /**
         * The name of this door.
         */
        final @NotNull String name;

        /**
         * The cuboid that describes this door.
         */
        final @NotNull Cuboid cuboid;

        /**
         * The location of the engine.
         */
        final @NotNull Vector3Di engine;

        /**
         * The location of the powerblock.
         */

        final @NotNull Vector3Di powerBlock;

        /**
         * The {@link IPWorld} this door is in.
         */
        final @NotNull IPWorld world;

        /**
         * Whether or not this door is currently open.
         */
        final boolean isOpen; // TODO: Use the bitflag here instead.

        /**
         * Whether or not this door is currently locked.
         */
        final boolean isLocked;

        /**
         * The open direction of this door.
         */

        final @NotNull RotateDirection openDirection;

        /**
         * The {@link DoorOwner} that originally created this door.
         */
        final @NotNull DoorOwner primeOwner;
    }

    /**
     * Represents a more complete picture of {@link SimpleDoorData}, as it includes a list of <u>all</u> {@link
     * DoorOwner}s of a door.
     * <p>
     * When no list of {@link DoorOwner}s is provided, it is assumed that the {@link SimpleDoorData#primeOwner} is the
     * only {@link DoorOwner}.
     *
     * @author Pim
     */
    public static class DoorData extends SimpleDoorData
    {
        /**
         * The list of {@link DoorOwner}s of this door.
         */
        @Getter
        private final @NotNull ConcurrentHashMap<@NotNull UUID, @NotNull DoorOwner> doorOwners;

        public DoorData(final long uid, final @NotNull String name, final @NotNull Cuboid cuboid,
                        final @NotNull Vector3Di engine, final @NotNull Vector3Di powerBlock,
                        final @NotNull IPWorld world, final boolean isOpen, final boolean isLocked,
                        final @NotNull RotateDirection openDirection, final @NotNull DoorOwner primeOwner)
        {
            super(uid, name, cuboid, engine, powerBlock, world, isOpen, isLocked, openDirection, primeOwner);
            doorOwners = new ConcurrentHashMap<>();
            doorOwners.put(primeOwner.pPlayerData().getUUID(), primeOwner);
        }

        public DoorData(final long uid, final @NotNull String name, final @NotNull Vector3Di min,
                        final @NotNull Vector3Di max, final @NotNull Vector3Di engine,
                        final @NotNull Vector3Di powerBlock, final @NotNull IPWorld world, final boolean isOpen,
                        final boolean isLocked, final @NotNull RotateDirection openDirection,
                        final @NotNull DoorOwner primeOwner)
        {
            this(uid, name, new Cuboid(min, max), engine, powerBlock, world, isOpen, isLocked, openDirection,
                 primeOwner);
        }

        public DoorData(final long uid, final @NotNull String name, final @NotNull Cuboid cuboid,
                        final @NotNull Vector3Di engine, final @NotNull Vector3Di powerBlock,
                        final @NotNull IPWorld world, final boolean isOpen, final boolean isLocked,
                        final @NotNull RotateDirection openDirection, final @NotNull DoorOwner primeOwner,
                        final @NotNull Map<@NotNull UUID, @NotNull DoorOwner> doorOwners)
        {
            super(uid, name, cuboid, engine, powerBlock, world, isOpen, isLocked, openDirection, primeOwner);
            this.doorOwners = new ConcurrentHashMap<>(doorOwners);
        }

        public DoorData(final long uid, final @NotNull String name, final @NotNull Vector3Di min,
                        final @NotNull Vector3Di max, final @NotNull Vector3Di engine,
                        final @NotNull Vector3Di powerBlock, final @NotNull IPWorld world, final boolean isOpen,
                        final boolean isLocked, final @NotNull RotateDirection openDirection,
                        final @NotNull DoorOwner primeOwner,
                        final @NotNull Map<@NotNull UUID, @NotNull DoorOwner> doorOwners)
        {
            this(uid, name, new Cuboid(min, max), engine, powerBlock, world, isOpen, isLocked, openDirection,
                 primeOwner, doorOwners);
        }
    }

    // test
    public class Registerable
    {
        private Registerable()
        {
        }

        /**
         * Gets the {@link AbstractDoorBase} that is associated with this {@link Registerable}.
         *
         * @return The {@link AbstractDoorBase} that is associated with this {@link Registerable}.
         */
        public @NotNull AbstractDoorBase getAbstractDoorBase()
        {
            return AbstractDoorBase.this;
        }
    }
}
