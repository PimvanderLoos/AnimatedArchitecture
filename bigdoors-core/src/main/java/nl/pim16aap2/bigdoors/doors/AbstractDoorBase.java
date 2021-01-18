package nl.pim16aap2.bigdoors.doors;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.Synchronized;
import lombok.experimental.Accessors;
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
import nl.pim16aap2.bigdoors.managers.DoorRegistry;
import nl.pim16aap2.bigdoors.managers.LimitsManager;
import nl.pim16aap2.bigdoors.moveblocks.BlockMover;
import nl.pim16aap2.bigdoors.util.Cuboid;
import nl.pim16aap2.bigdoors.util.CuboidConst;
import nl.pim16aap2.bigdoors.util.DoorOwner;
import nl.pim16aap2.bigdoors.util.DoorToggleResult;
import nl.pim16aap2.bigdoors.util.Limit;
import nl.pim16aap2.bigdoors.util.PLogger;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import nl.pim16aap2.bigdoors.util.Util;
import nl.pim16aap2.bigdoors.util.vector.Vector2Di;
import nl.pim16aap2.bigdoors.util.vector.Vector2DiConst;
import nl.pim16aap2.bigdoors.util.vector.Vector3Di;
import nl.pim16aap2.bigdoors.util.vector.Vector3DiConst;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.logging.Level;

/**
 * Represents an unspecialized door.
 *
 * @author Pim
 */
@Accessors(chain = true)
public abstract class AbstractDoorBase extends DatabaseManager.FriendDoorAccessor implements IDoorBase
{
    @Getter(onMethod = @__({@Override}))
    private final long doorUID;
    protected final @NotNull DoorOpeningUtility doorOpeningUtility;

    @Getter(onMethod = @__({@Override}))
    protected final @NotNull IPWorld world;

    @Getter(onMethod = @__({@Override}))
    protected volatile @NotNull Vector3DiConst engine;

    @Getter(onMethod = @__({@Override}))
    private volatile @NotNull Vector2DiConst engineChunk;

    @Getter(onMethod = @__({@Override}))
    protected volatile @NotNull Vector3DiConst powerBlock;

    @Getter(onMethod = @__({@Override}))
    @Setter(onMethod = @__({@Override}))
    private volatile @NotNull String name;

    private volatile CuboidConst cuboid;

    @Getter(onMethod = @__({@Override}))
    @Setter(onMethod = @__({@Override}))
    private volatile boolean open;

    @Getter(onMethod = @__({@Override}))
    @Setter(onMethod = @__({@Override}))
    private volatile @NotNull RotateDirection openDir;

    @Getter(onMethod = @__({@Override}))
    @Setter(onMethod = @__({@Override}))
    private volatile boolean locked;

    private final @NotNull Map<@NotNull UUID, @NotNull DoorOwner> doorOwners;
    private final @NotNull Object doorOwnersLock = new Object();

    @Getter(onMethod = @__({@Override, @Synchronized("doorOwnersLock")}))
    private final @NotNull DoorOwner primeOwner;

    /**
     * Min and Max Vector2Di coordinates of the range of Vector2Dis that this {@link AbstractDoorBase} might interact
     * with.
     */
    private Vector2Di minChunkCoords = null, maxChunkCoords = null;

    @Override
    protected final void addOwner(final @NotNull UUID uuid, final @NotNull DoorOwner doorOwner)
    {
        synchronized (doorOwnersLock)
        {
            if (doorOwner.getPermission() == 0)
            {
                PLogger.get().logThrowable(new IllegalArgumentException(
                    "Failed to add owner: " + doorOwner.getPlayer().toString() + " as owner to door: " + getDoorUID() +
                        " because a permission level of 0 is not allowed!"));
                return;
            }
            doorOwners.put(uuid, doorOwner);
        }
    }

    @Override
    protected final boolean removeOwner(final @NotNull UUID uuid)
    {
        synchronized (doorOwnersLock)
        {
            if (primeOwner.getPlayer().getUUID().equals(uuid))
            {
                PLogger.get().logThrowable(new IllegalArgumentException(
                    "Failed to remove owner: " + primeOwner.getPlayer().toString() + " as owner from door: " +
                        getDoorUID() + " because removing an owner with a permission level of 0 is not allowed!"));
                return false;
            }
            return doorOwners.remove(uuid) != null;
        }
    }

    /**
     * Constructs a new {@link AbstractDoorBase}.
     */
    protected AbstractDoorBase(final @NotNull DoorData doorData)
    {
        doorUID = doorData.getUid();

        PLogger.get().logMessage(Level.FINEST, "Instantiating door: " + doorUID);
        if (doorUID > 0 && !DoorRegistry.get().registerDoor(new Registerable()))
        {
            final @NotNull IllegalStateException exception = new IllegalStateException(
                "Tried to create new door \"" + doorUID + "\" while it is already registered!");
            PLogger.get().logThrowableSilently(exception);
            throw exception;
        }

        name = doorData.getName();
        cuboid = doorData.getCuboid();
        engine = doorData.getEngine();
        engineChunk = Util.getChunkCoords(engine);
        powerBlock = doorData.getPowerBlock();
        world = doorData.getWorld();
        open = doorData.isOpen();
        openDir = doorData.getOpenDirection();
        primeOwner = doorData.getPrimeOwner();
        doorOwners = doorData.getDoorOwners();
        locked = doorData.isLocked();

        doorOpeningUtility = DoorOpeningUtility.get();
    }

    /**
     * Tries to get {@link DoorType#getTypeData(AbstractDoorBase)} for this door. If no type-specific data is found, an
     * exception will be logged.
     *
     * @return The result of {@link DoorType#getTypeData(AbstractDoorBase)}, no matter the outcome.
     */
    private @NotNull Optional<Object[]> verifyTypeData()
    {
        final @NotNull Optional<Object[]> typeDataOpt = getDoorType().getTypeData(this);
        if (!typeDataOpt.isPresent())
            PLogger.get().logThrowable(new IllegalArgumentException(
                "Failed to update door " + getDoorUID() + ": Could not get type-specific data!"));
        return typeDataOpt;
    }

    /**
     * Synchronizes all data of this door with the database.
     * <p>
     * Calling this method is faster than calling both {@link #syncBaseData()} and {@link #syncTypeData()}. However,
     * because of the added overhead of type-specific data, you should try to use {@link #syncBaseData()} whenever
     * possible (i.e. when no type-specific data was changed).
     */
    public synchronized final void syncAllData()
    {
        verifyTypeData().ifPresent(
            typeData -> DatabaseManager.get().syncAllDataOfDoor(getSimpleDoorDataCopy(), getDoorType(), typeData));
    }

    /**
     * Synchronizes the base data of this door with the database.
     */
    public synchronized final void syncBaseData()
    {
        DatabaseManager.get().syncDoorBaseData(getSimpleDoorDataCopy());
    }

    /**
     * Synchronizes the type-specific data in this door with the database.
     */
    public synchronized final void syncTypeData()
    {
        verifyTypeData().ifPresent(
            typeData -> DatabaseManager.get().syncDoorTypeData(getDoorUID(), getDoorType(), typeData));
    }

    @Override
    public @NotNull CuboidConst getCuboid()
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
    public synchronized @NotNull SimpleDoorData getSimpleDoorDataCopy()
    {
        return new SimpleDoorData(doorUID, name, cuboid.clone(), engine.clone(), powerBlock.clone(),
                                  world.clone(), open, locked, openDir, getPrimeOwner().clone());
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
            PLogger.get()
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
    final synchronized @NotNull DoorToggleResult toggle(final @NotNull DoorActionCause cause,
                                                        final @NotNull IMessageable messageReceiver,
                                                        final @NotNull IPPlayer responsible, final double time,
                                                        boolean skipAnimation, final @NotNull DoorActionType actionType)
    {
        if (!BigDoors.get().getPlatform().isMainThread(Thread.currentThread().getId()))
            throw new IllegalStateException("Doors must be toggled on the main thread!");

        if (openDir == RotateDirection.NONE)
        {
            IllegalStateException e = new IllegalStateException("OpenDir cannot be NONE!");
            PLogger.get().logThrowable(e);
            throw e;
        }

        if (!DoorRegistry.get().isRegistered(this))
            return doorOpeningUtility.abort(this, DoorToggleResult.INSTANCE_UNREGISTERED, cause, responsible);

        if (skipAnimation && !canSkipAnimation())
            return doorOpeningUtility.abort(this, DoorToggleResult.ERROR, cause, responsible);

        final @NotNull DoorToggleResult isOpenable = doorOpeningUtility.canBeToggled(this, cause, actionType);
        if (isOpenable != DoorToggleResult.SUCCESS)
            return doorOpeningUtility.abort(this, isOpenable, cause, responsible);

        if (LimitsManager.exceedsLimit(responsible, Limit.DOOR_SIZE, getBlockCount()))
            return doorOpeningUtility.abort(this, DoorToggleResult.TOOBIG, cause, responsible);

        final @NotNull Optional<Cuboid> newCuboid = getPotentialNewCoordinates();

        if (!newCuboid.isPresent())
            return doorOpeningUtility.abort(this, DoorToggleResult.ERROR, cause, responsible);

        final @NotNull IDoorEventTogglePrepare prepareEvent = BigDoors.get().getPlatform().getDoorActionEventFactory()
                                                                      .createPrepareEvent(this, cause, actionType,
                                                                                          responsible, time,
                                                                                          skipAnimation,
                                                                                          newCuboid.get());
        BigDoors.get().getPlatform().callDoorActionEvent(prepareEvent);
        if (prepareEvent.isCancelled())
            return doorOpeningUtility.abort(this, DoorToggleResult.CANCELLED, cause, responsible);

        final @Nullable IPPlayer responsiblePlayer = cause.equals(DoorActionCause.PLAYER) ? responsible : null;
        if (!doorOpeningUtility.isLocationEmpty(newCuboid.get(), cuboid, responsiblePlayer, getWorld()))
            return doorOpeningUtility.abort(this, DoorToggleResult.OBSTRUCTED, cause, responsible);

        if (!doorOpeningUtility.canBreakBlocksBetweenLocs(this, newCuboid.get(), responsible))
            return doorOpeningUtility.abort(this, DoorToggleResult.NOPERMISSION, cause, responsible);

        BigDoors.get().getPlatform().newPExecutor().runOnMainThread(
            () -> registerBlockMover(cause, time, skipAnimation, newCuboid.get(), responsible, actionType));

        BigDoors.get().getPlatform().callDoorActionEvent(BigDoors.get().getPlatform().getDoorActionEventFactory()
                                                                 .createStartEvent(this, cause, actionType, responsible,
                                                                                   time, skipAnimation,
                                                                                   newCuboid.get()));
        return DoorToggleResult.SUCCESS;
    }

    @Override
    public final void onRedstoneChange(final int newCurrent)
    {
        final @NotNull IPPlayer player = BigDoors.get().getPlatform().getPPlayerFactory()
                                                 .create(getPrimeOwner().getPlayer().getUUID(),
                                                         getPrimeOwner().getPlayer().getName());

        if (newCurrent == 0 && isCloseable())
            DoorOpener.get().animateDoorAsync(this, DoorActionCause.REDSTONE,
                                              BigDoors.get().getPlatform().getMessageableServer(), player, 0.0D,
                                              false, DoorActionType.CLOSE);
        else if (newCurrent > 0 && isOpenable())
            DoorOpener.get().animateDoorAsync(this, DoorActionCause.REDSTONE,
                                              BigDoors.get().getPlatform().getMessageableServer(), player, 0.0D,
                                              false, DoorActionType.OPEN);
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
    protected abstract @NotNull BlockMover constructBlockMover(final @NotNull DoorActionCause cause,
                                                               final double time, final boolean skipAnimation,
                                                               final @NotNull CuboidConst newCuboid,
                                                               final @NotNull IPPlayer responsible,
                                                               final @NotNull DoorActionType actionType);

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
     */
    private synchronized void registerBlockMover(final @NotNull DoorActionCause cause,
                                                 final double time, final boolean skipAnimation,
                                                 final @NotNull CuboidConst newCuboid,
                                                 final @NotNull IPPlayer responsible,
                                                 final @NotNull DoorActionType actionType)
    {
        if (!BigDoors.get().getPlatform().isMainThread(Thread.currentThread().getId()))
        {
            final @NotNull IllegalThreadStateException e = new IllegalThreadStateException(
                "BlockMovers must be instantiated on the main thread!");
            PLogger.get().logThrowableSilently(e);
            BigDoors.get().getDoorManager().setDoorAvailable(getDoorUID());
            return;
        }

        doorOpeningUtility.registerBlockMover(constructBlockMover(cause, time, skipAnimation, newCuboid,
                                                                  responsible, actionType));
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

        return new Vector2Di[]{new Vector2Di(minChunk.getX(), minChunk.getY()),
                               new Vector2Di(maxChunk.getX(), maxChunk.getY())};
    }

    /**
     * @deprecated This method needs to be replaced with the 'new' chunk-system from v1.
     */
    @Deprecated
    @Override
    public final boolean chunkInRange(final @NotNull IPWorld otherWorld, final @NotNull Vector2DiConst chunk)
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
            final @NotNull Vector2Di[] range = calculateChunkRange();
            minChunkCoords = range[0];
            maxChunkCoords = range[1];
        }
    }

    @Override
    public @NotNull Collection<@NotNull DoorOwner> getDoorOwners()
    {
        synchronized (doorOwnersLock)
        {
            return Collections.unmodifiableCollection(doorOwners.values());
        }
    }

    protected @NotNull Map<@NotNull UUID, @NotNull DoorOwner> getDoorOwnersCopy()
    {
        synchronized (doorOwnersLock)
        {
            final @NotNull Map<@NotNull UUID, @NotNull DoorOwner> copy = new HashMap<>(doorOwners.size());
            doorOwners.forEach(
                (key, value) -> copy.put(new UUID(key.getMostSignificantBits(), key.getLeastSignificantBits()),
                                         value.clone()));
            return copy;
        }
    }

    @Override
    public @NotNull Optional<DoorOwner> getDoorOwner(final @NotNull IPPlayer player)
    {
        return getDoorOwner(player.getUUID());
    }

    @Override
    public @NotNull Optional<DoorOwner> getDoorOwner(final @NotNull UUID uuid)
    {
        synchronized (doorOwnersLock)
        {
            return Optional.ofNullable(doorOwners.get(uuid));
        }
    }

    @Override
    public @NotNull AbstractDoorBase setCoordinates(final @NotNull CuboidConst newCuboid)
    {
        cuboid = new CuboidConst(newCuboid);
        return this;
    }

    @Override
    public final @NotNull AbstractDoorBase setCoordinates(final @NotNull Vector3DiConst posA,
                                                          final @NotNull Vector3DiConst posB)
    {
        cuboid = new Cuboid(posA, posB);
        return this;
    }

    @Override
    public @NotNull Vector3DiConst getMinimum()
    {
        return cuboid.getMin();
    }

    @Override
    public @NotNull Vector3DiConst getMaximum()
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
    public @NotNull Vector3DiConst getDimensions()
    {
        return cuboid.getDimensions();
    }

    @Override
    public synchronized final @NotNull AbstractDoorBase setEngine(final @NotNull Vector3DiConst pos)
    {
        engine = new Vector3Di(pos);
        engineChunk = Util.getChunkCoords(pos);
        return this;
    }

    @Override
    public final @NotNull AbstractDoorBase setPowerBlockPosition(final @NotNull Vector3DiConst pos)
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
    public synchronized @NotNull String getBasicInfo()
    {
        return doorUID + " (" + getPrimeOwner().toString() + ") - " + getDoorType().getSimpleName() + ": " + name;
    }

    /**
     * @return String with (almost) all data of this door.
     */
    @Override
    public synchronized @NotNull String toString()
    {
        StringBuilder builder = new StringBuilder();
        builder.append(doorUID).append(": ").append(name).append("\n");
        builder.append("Type: ").append(getDoorType().toString()).append("\n");
        builder.append("Cuboid: ").append(cuboid.toString()).append(", Engine: ").append(engine.toString())
               .append("\n");
        builder.append("PowerBlock position: ").append(powerBlock.toString()).append(". Hash: ")
               .append(getSimplePowerBlockChunkHash()).append("\n");
        builder.append("World: ").append(getWorld().getWorldName()).append("\n");
        builder.append("This door is ").append((locked ? "" : "NOT ")).append("locked. ");
        builder.append("This door is ").append((open ? "Open.\n" : "Closed.\n"));
        builder.append("OpenDir: ").append(openDir.toString()).append("\n");
        getDoorType().getTypeData(this).ifPresent(
            data ->
            {
                int idx = 0;
                for (DoorType.Parameter parameter : getDoorType().getParameters())
                    builder.append(parameter.getParameterName()).append(": ").append(data[idx++].toString())
                           .append("\n");
            });

        return builder.toString();
    }

    // TODO: Hashcode. Just the UID? Or actually calculate it?
    @Override
    public boolean equals(Object o)
    {
        if (this == o)
            return true;

        if (o == null || getClass() != o.getClass())
            return false;

        AbstractDoorBase other = (AbstractDoorBase) o;
        return doorUID == other.doorUID && name.equals(other.name) && cuboid.equals(other.cuboid) &&
            engine.equals(other.engine) && getDoorType().equals(other.getDoorType()) && open == other.open &&
            primeOwner.equals(other.primeOwner) && locked == other.locked &&
            world.getWorldName().equals(other.world.getWorldName());
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
        @NotNull String name;

        /**
         * The cuboid that describes this door.
         */
        @NotNull Cuboid cuboid;

        /**
         * The location of the engine.
         */
        @NotNull Vector3DiConst engine;

        /**
         * The location of the powerblock.
         */

        @NotNull Vector3DiConst powerBlock;

        /**
         * The {@link IPWorld} this door is in.
         */
        @NotNull IPWorld world;

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

        @NotNull RotateDirection openDirection;

        /**
         * The {@link DoorOwner} that originally created this door.
         */
        @NotNull DoorOwner primeOwner;
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
        @NotNull Map<@NotNull UUID, @NotNull DoorOwner> doorOwners;

        public DoorData(final long uid, final @NotNull String name, final @NotNull Cuboid cuboid,
                        final @NotNull Vector3DiConst engine, final @NotNull Vector3DiConst powerBlock,
                        final @NotNull IPWorld world, final boolean isOpen, final boolean isLocked,
                        final @NotNull RotateDirection openDirection, final @NotNull DoorOwner primeOwner)
        {
            super(uid, name, cuboid, engine, powerBlock, world, isOpen, isLocked, openDirection, primeOwner);
            doorOwners = Collections.singletonMap(primeOwner.getPlayer().getUUID(), primeOwner);
        }

        public DoorData(final long uid, final @NotNull String name, final @NotNull Vector3DiConst min,
                        final @NotNull Vector3DiConst max, final @NotNull Vector3DiConst engine,
                        final @NotNull Vector3DiConst powerBlock, final @NotNull IPWorld world, final boolean isOpen,
                        final boolean isLocked, final @NotNull RotateDirection openDirection,
                        final @NotNull DoorOwner primeOwner)
        {
            this(uid, name, new Cuboid(min, max), engine, powerBlock, world, isOpen, isLocked, openDirection,
                 primeOwner);
        }

        public DoorData(final long uid, final @NotNull String name, final @NotNull Cuboid cuboid,
                        final @NotNull Vector3DiConst engine, final @NotNull Vector3DiConst powerBlock,
                        final @NotNull IPWorld world, final boolean isOpen, final boolean isLocked,
                        final @NotNull RotateDirection openDirection, final @NotNull DoorOwner primeOwner,
                        final @NotNull Map<@NotNull UUID, @NotNull DoorOwner> doorOwners)
        {
            this(uid, name, cuboid, engine, powerBlock, world, isOpen, isLocked, openDirection, primeOwner);
            this.doorOwners = doorOwners;
        }

        public DoorData(final long uid, final @NotNull String name, final @NotNull Vector3DiConst min,
                        final @NotNull Vector3DiConst max, final @NotNull Vector3DiConst engine,
                        final @NotNull Vector3DiConst powerBlock, final @NotNull IPWorld world, final boolean isOpen,
                        final boolean isLocked, final @NotNull RotateDirection openDirection,
                        final @NotNull DoorOwner primeOwner,
                        final @NotNull Map<@NotNull UUID, @NotNull DoorOwner> doorOwners)
        {
            this(uid, name, new Cuboid(min, max), engine, powerBlock, world, isOpen, isLocked, openDirection,
                 primeOwner, doorOwners);
        }
    }

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
