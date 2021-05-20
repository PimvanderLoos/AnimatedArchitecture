package nl.pim16aap2.bigdoors.doors;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.Synchronized;
import lombok.experimental.Accessors;
import lombok.val;
import nl.pim16aap2.bigdoors.BigDoors;
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
import nl.pim16aap2.bigdoors.util.CuboidConst;
import nl.pim16aap2.bigdoors.util.DoorOwner;
import nl.pim16aap2.bigdoors.util.DoorToggleResult;
import nl.pim16aap2.bigdoors.util.Limit;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import nl.pim16aap2.bigdoors.util.Util;
import nl.pim16aap2.bigdoors.util.vector.Vector2Di;
import nl.pim16aap2.bigdoors.util.vector.Vector2DiConst;
import nl.pim16aap2.bigdoors.util.vector.Vector3Di;
import nl.pim16aap2.bigdoors.util.vector.Vector3DiConst;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
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
    protected final @NonNull IPWorld world;

    @Getter
    protected volatile @NonNull Vector3DiConst engine;

    @Getter
    private volatile @NonNull Vector2DiConst engineChunk;

    @Getter
    protected volatile @NonNull Vector3DiConst powerBlock;

    @Getter
    @Setter(onMethod = @__({@Override}))
    private volatile @NonNull String name;

    private volatile CuboidConst cuboid;

    @Getter
    @Setter(onMethod = @__({@Override}))
    private volatile boolean open;

    @Getter
    @Setter(onMethod = @__({@Override}))
    private volatile @NonNull RotateDirection openDir;

    /**
     * Represents the locked status of this door. True = locked, False = unlocked.
     */
    @Getter
    @Setter(onMethod = @__({@Override}))
    private volatile boolean locked;

    @EqualsAndHashCode.Exclude
    private final @NonNull Map<@NonNull UUID, @NonNull DoorOwner> doorOwners;

    @EqualsAndHashCode.Exclude
    private final @NonNull Object doorOwnersLock = new Object();

    @Getter(onMethod = @__({@Override, @Synchronized("doorOwnersLock")}))
    private final @NonNull DoorOwner primeOwner;

    private final @Nullable DoorSerializer<?> serializer = getDoorType().getDoorSerializer().orElse(null);

    /**
     * Min and Max Vector2Di coordinates of the range of Vector2Dis that this {@link AbstractDoorBase} might interact
     * with.
     */
    @EqualsAndHashCode.Exclude
    private Vector2Di minChunkCoords = null, maxChunkCoords = null;

    @Override
    protected final void addOwner(final @NonNull UUID uuid, final @NonNull DoorOwner doorOwner)
    {
        synchronized (doorOwnersLock)
        {
            if (doorOwner.getPermission() == 0)
            {
                BigDoors.get().getPLogger().logThrowable(new IllegalArgumentException(
                    "Failed to add owner: " + doorOwner.getPPlayerData().toString() + " as owner to door: " +
                        getDoorUID() +
                        " because a permission level of 0 is not allowed!"));
                return;
            }
            doorOwners.put(uuid, doorOwner);
        }
    }

    @Override
    protected final boolean removeOwner(final @NonNull UUID uuid)
    {
        synchronized (doorOwnersLock)
        {
            if (primeOwner.getPPlayerData().getUUID().equals(uuid))
            {
                BigDoors.get().getPLogger().logThrowable(new IllegalArgumentException(
                    "Failed to remove owner: " + primeOwner.getPPlayerData().toString() + " as owner from door: " +
                        getDoorUID() + " because removing an owner with a permission level of 0 is not allowed!"));
                return false;
            }
            return doorOwners.remove(uuid) != null;
        }
    }

    /**
     * Constructs a new {@link AbstractDoorBase}.
     */
    protected AbstractDoorBase(final @NonNull DoorData doorData)
    {
        init(doorData);
        doorUID = doorData.getUid();
        world = doorData.getWorld();
        primeOwner = doorData.getPrimeOwner();
        doorOwners = doorData.getDoorOwners();
    }

    private void init(final @NonNull DoorData doorData)
    {
        BigDoors.get().getPLogger().logMessage(Level.FINEST, "Instantiating door: " + doorUID);
        if (doorUID > 0 && !BigDoors.get().getDoorRegistry().registerDoor(new Registerable()))
        {
            final @NonNull IllegalStateException exception = new IllegalStateException(
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
    public final synchronized @NonNull CompletableFuture<Boolean> syncData()
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
    public @NonNull CuboidConst getCuboid()
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
    public synchronized @NonNull DoorData getDoorDataCopy()
    {
        return new DoorData(doorUID, name, cuboid.clone(), engine.clone(), powerBlock.clone(),
                            world.clone(), open, locked, openDir, getPrimeOwner().clone(), getDoorOwnersCopy());
    }

    /**
     * Obtains a simple copy of the {@link DoorData} the describes this door.
     * <p>
     * Note that this creates a deep copy of the DoorData <u><b>excluding</u></b><b> its {@link DoorOwner}s</b>, so use
     * it sparingly.
     *
     * @return A copy of the {@link DoorData} the describes this door.
     */
    public synchronized @NonNull SimpleDoorData getSimpleDoorDataCopy()
    {
        return new SimpleDoorData(doorUID, name, cuboid.clone(), engine.clone(), powerBlock.clone(),
                                  world.clone(), open, locked, openDir, getPrimeOwner().clone());
    }

    /**
     * Gets the {@link DoorType} of this door.
     *
     * @return The {@link DoorType} of this door.
     */
    public abstract @NonNull DoorType getDoorType();

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
                    .logThrowable(new IllegalStateException("Failed to load chunk at: " + powerBlockChunk.toString()));
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
    final synchronized @NonNull DoorToggleResult toggle(final @NonNull DoorActionCause cause,
                                                        final @NonNull IMessageable messageReceiver,
                                                        final @NonNull IPPlayer responsible, final double time,
                                                        boolean skipAnimation, final @NonNull DoorActionType actionType)
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

        final @NonNull DoorToggleResult isOpenable = DoorOpeningUtility.canBeToggled(this, cause, actionType);
        if (isOpenable != DoorToggleResult.SUCCESS)
            return DoorOpeningUtility.abort(this, isOpenable, cause, responsible);

        if (BigDoors.get().getLimitsManager().exceedsLimit(responsible, Limit.DOOR_SIZE, getBlockCount()))
            return DoorOpeningUtility.abort(this, DoorToggleResult.TOOBIG, cause, responsible);

        final @NonNull Optional<Cuboid> newCuboid = getPotentialNewCoordinates();

        if (newCuboid.isEmpty())
            return DoorOpeningUtility.abort(this, DoorToggleResult.ERROR, cause, responsible);

        final @NonNull IDoorEventTogglePrepare prepareEvent = BigDoors.get().getPlatform().getBigDoorsEventFactory()
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
                                        .create(getPrimeOwner().getPPlayerData());

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
     * @param newCuboid     The {@link CuboidConst} representing the area the door will take up after the toggle.
     * @param responsible   The {@link IPPlayer} responsible for the door action.
     * @param actionType    The type of action that will be performed by the BlockMover.
     * @return The {@link BlockMover} for this class.
     */
    protected abstract @NonNull BlockMover constructBlockMover(final @NonNull DoorActionCause cause,
                                                               final double time, final boolean skipAnimation,
                                                               final @NonNull CuboidConst newCuboid,
                                                               final @NonNull IPPlayer responsible,
                                                               final @NonNull DoorActionType actionType)
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
     * @param newCuboid     The {@link CuboidConst} representing the area the door will take up after the toggle.
     * @param responsible   The {@link IPPlayer} responsible for the door action.
     * @param actionType    The type of action that will be performed by the BlockMover.
     * @return True when everything went all right, otherwise false.
     */
    private synchronized boolean registerBlockMover(final @NonNull DoorActionCause cause,
                                                    final double time, final boolean skipAnimation,
                                                    final @NonNull CuboidConst newCuboid,
                                                    final @NonNull IPPlayer responsible,
                                                    final @NonNull DoorActionType actionType)
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
    public @NonNull Vector2Di[] calculateCurrentChunkRange()
    {
        final @NonNull Vector2Di minChunk = Util.getChunkCoords(cuboid.getMin());
        final @NonNull Vector2Di maxChunk = Util.getChunkCoords(cuboid.getMax());

        return new Vector2Di[]{new Vector2Di(minChunk.getX(), minChunk.getY()),
                               new Vector2Di(maxChunk.getX(), maxChunk.getY())};
    }

    /**
     * @deprecated This method needs to be replaced with the 'new' chunk-system from v1.
     */
    @Deprecated
    @Override
    public final boolean chunkInRange(final @NonNull IPWorld otherWorld, final @NonNull Vector2DiConst chunk)
    {
        if (!otherWorld.equals(getWorld()))
            return false;

        verifyChunkRange();
        return Util.between(chunk.getX(), minChunkCoords.getX(), maxChunkCoords.getX()) &&
            Util.between(chunk.getY(), minChunkCoords.getY(), maxChunkCoords.getY());
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
            final @NonNull Vector2Di[] range = calculateChunkRange();
            minChunkCoords = range[0];
            maxChunkCoords = range[1];
        }
    }

    @Override
    public @NonNull Collection<@NonNull DoorOwner> getDoorOwners()
    {
        synchronized (doorOwnersLock)
        {
            return Collections.unmodifiableCollection(doorOwners.values());
        }
    }

    protected @NonNull Map<@NonNull UUID, @NonNull DoorOwner> getDoorOwnersCopy()
    {
        synchronized (doorOwnersLock)
        {
            final @NonNull Map<@NonNull UUID, @NonNull DoorOwner> copy = new HashMap<>(doorOwners.size());
            doorOwners.forEach(
                (key, value) -> copy.put(new UUID(key.getMostSignificantBits(), key.getLeastSignificantBits()),
                                         value.clone()));
            return copy;
        }
    }

    @Override
    public @NonNull Optional<DoorOwner> getDoorOwner(final @NonNull IPPlayer player)
    {
        return getDoorOwner(player.getUUID());
    }

    @Override
    public @NonNull Optional<DoorOwner> getDoorOwner(final @NonNull UUID uuid)
    {
        synchronized (doorOwnersLock)
        {
            return Optional.ofNullable(doorOwners.get(uuid));
        }
    }

    @Override
    public @NonNull AbstractDoorBase setCoordinates(final @NonNull CuboidConst newCuboid)
    {
        cuboid = new CuboidConst(newCuboid);
        return this;
    }

    @Override
    public final @NonNull AbstractDoorBase setCoordinates(final @NonNull Vector3DiConst posA,
                                                          final @NonNull Vector3DiConst posB)
    {
        cuboid = new Cuboid(posA, posB);
        return this;
    }

    @Override
    public @NonNull Vector3DiConst getMinimum()
    {
        return cuboid.getMin();
    }

    @Override
    public @NonNull Vector3DiConst getMaximum()
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
    public final @NonNull Vector2Di[] getChunkRange()
    {
        verifyChunkRange();
        return new Vector2Di[]{minChunkCoords, maxChunkCoords};
    }

    @Override
    public @NonNull Vector3DiConst getDimensions()
    {
        return cuboid.getDimensions();
    }

    @Override
    public synchronized final @NonNull AbstractDoorBase setEngine(final @NonNull Vector3DiConst pos)
    {
        engine = new Vector3Di(pos);
        engineChunk = Util.getChunkCoords(pos);
        return this;
    }

    @Override
    public final @NonNull AbstractDoorBase setPowerBlockPosition(final @NonNull Vector3DiConst pos)
    {
        powerBlock = new Vector3Di(pos);
        invalidateChunkRange();
        return this;
    }

    @Override
    public final int getBlockCount()
    {
        return cuboid.getVolume();
    }

    @Override
    public synchronized final long getSimplePowerBlockChunkHash()
    {
        return Util.simpleChunkHashFromLocation(powerBlock.getX(), powerBlock.getZ());
    }

    @Override
    public synchronized @NonNull String getBasicInfo()
    {
        return doorUID + " (" + getPrimeOwner() + ") - " + getDoorType().getSimpleName() + ": " + name;
    }

    /**
     * @return String with (almost) all data of this door.
     */
    @Override
    public synchronized @NonNull String toString()
    {
        StringBuilder builder = new StringBuilder();
        builder.append(doorUID).append(": ").append(name).append("\n");
        builder.append("Type: ").append(getDoorType().toString()).append("\n");
        builder.append("Cuboid: ").append(cuboid.toString()).append(", Engine: ").append(engine).append("\n");
        builder.append("PowerBlock position: ").append(powerBlock).append(". Hash: ")
               .append(getSimplePowerBlockChunkHash()).append("\n");
        builder.append("World: ").append(getWorld().getWorldName()).append("\n");
        builder.append("This door is ").append((locked ? "" : "NOT ")).append("locked. ").append("\n");
        builder.append("This door is ").append((open ? "open.\n" : "closed.\n"));
        builder.append("OpenDir: ").append(openDir.name()).append("\n");

        builder.append("\nType-specific data:\n");
        val serializer = getDoorType().getDoorSerializer();
        if (serializer.isEmpty())
            builder.append("Invalid serializer!\n");
        else
            builder.append(serializer.get().toString(this));

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
        long uid;

        /**
         * The name of this door.
         */
        @NonNull String name;

        /**
         * The cuboid that describes this door.
         */
        @NonNull Cuboid cuboid;

        /**
         * The location of the engine.
         */
        @NonNull Vector3DiConst engine;

        /**
         * The location of the powerblock.
         */

        @NonNull Vector3DiConst powerBlock;

        /**
         * The {@link IPWorld} this door is in.
         */
        @NonNull IPWorld world;

        /**
         * Whether or not this door is currently open.
         */
        boolean isOpen; // TODO: Use the bitflag here instead.

        /**
         * Whether or not this door is currently locked.
         */
        boolean isLocked;

        /**
         * The open direction of this door.
         */

        @NonNull RotateDirection openDirection;

        /**
         * The {@link DoorOwner} that originally created this door.
         */
        @NonNull DoorOwner primeOwner;
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
        @NonNull Map<@NonNull UUID, @NonNull DoorOwner> doorOwners;

        public DoorData(final long uid, final @NonNull String name, final @NonNull Cuboid cuboid,
                        final @NonNull Vector3DiConst engine, final @NonNull Vector3DiConst powerBlock,
                        final @NonNull IPWorld world, final boolean isOpen, final boolean isLocked,
                        final @NonNull RotateDirection openDirection, final @NonNull DoorOwner primeOwner)
        {
            super(uid, name, cuboid, engine, powerBlock, world, isOpen, isLocked, openDirection, primeOwner);
            doorOwners = Collections.singletonMap(primeOwner.getPPlayerData().getUUID(), primeOwner);
        }

        public DoorData(final long uid, final @NonNull String name, final @NonNull Vector3DiConst min,
                        final @NonNull Vector3DiConst max, final @NonNull Vector3DiConst engine,
                        final @NonNull Vector3DiConst powerBlock, final @NonNull IPWorld world, final boolean isOpen,
                        final boolean isLocked, final @NonNull RotateDirection openDirection,
                        final @NonNull DoorOwner primeOwner)
        {
            this(uid, name, new Cuboid(min, max), engine, powerBlock, world, isOpen, isLocked, openDirection,
                 primeOwner);
        }

        public DoorData(final long uid, final @NonNull String name, final @NonNull Cuboid cuboid,
                        final @NonNull Vector3DiConst engine, final @NonNull Vector3DiConst powerBlock,
                        final @NonNull IPWorld world, final boolean isOpen, final boolean isLocked,
                        final @NonNull RotateDirection openDirection, final @NonNull DoorOwner primeOwner,
                        final @NonNull Map<@NonNull UUID, @NonNull DoorOwner> doorOwners)
        {
            this(uid, name, cuboid, engine, powerBlock, world, isOpen, isLocked, openDirection, primeOwner);
            this.doorOwners = doorOwners;
        }

        public DoorData(final long uid, final @NonNull String name, final @NonNull Vector3DiConst min,
                        final @NonNull Vector3DiConst max, final @NonNull Vector3DiConst engine,
                        final @NonNull Vector3DiConst powerBlock, final @NonNull IPWorld world, final boolean isOpen,
                        final boolean isLocked, final @NonNull RotateDirection openDirection,
                        final @NonNull DoorOwner primeOwner,
                        final @NonNull Map<@NonNull UUID, @NonNull DoorOwner> doorOwners)
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
        public @NonNull AbstractDoorBase getAbstractDoorBase()
        {
            return AbstractDoorBase.this;
        }
    }
}
