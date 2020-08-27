package nl.pim16aap2.bigdoors.doors;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.Value;
import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.api.IChunkManager;
import nl.pim16aap2.bigdoors.api.IMessageable;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.api.IPWorld;
import nl.pim16aap2.bigdoors.doortypes.DoorType;
import nl.pim16aap2.bigdoors.events.dooraction.DoorActionCause;
import nl.pim16aap2.bigdoors.events.dooraction.DoorActionType;
import nl.pim16aap2.bigdoors.events.dooraction.IDoorEventTogglePrepare;
import nl.pim16aap2.bigdoors.managers.LimitsManager;
import nl.pim16aap2.bigdoors.moveblocks.BlockMover;
import nl.pim16aap2.bigdoors.util.DoorOwner;
import nl.pim16aap2.bigdoors.util.DoorToggleResult;
import nl.pim16aap2.bigdoors.util.Limit;
import nl.pim16aap2.bigdoors.util.PLogger;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import nl.pim16aap2.bigdoors.util.Util;
import nl.pim16aap2.bigdoors.util.vector.IVector2DiConst;
import nl.pim16aap2.bigdoors.util.vector.IVector3DiConst;
import nl.pim16aap2.bigdoors.util.vector.Vector2Di;
import nl.pim16aap2.bigdoors.util.vector.Vector3Di;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import java.util.logging.Level;

/**
 * Represents an unspecialized door.
 *
 * @author Pim
 */
public abstract class AbstractDoorBase implements IDoorBase
{
    @Getter(onMethod = @__({@Override}))
    private final long doorUID;
    @NotNull
    protected final DoorOpeningUtility doorOpeningUtility;

    @Getter(onMethod = @__({@Override}))
    @NotNull
    protected final IPWorld world;

    @Getter(onMethod = @__({@Override}))
    @NotNull
    protected IVector3DiConst minimum, maximum, engine, powerBlock, dimensions;

    @Getter(onMethod = @__({@Override}))
    @Setter(onMethod = @__({@Override}))
    private String name;

    @Getter(onMethod = @__({@Override}))
    @Setter(onMethod = @__({@Override}))
    private boolean open;
    @Getter(onMethod = @__({@Override}))
    @NotNull
    private RotateDirection openDir;

    @Getter(onMethod = @__({@Override}))
    @Setter(onMethod = @__({@Override}))
    private boolean isLocked;

    @Getter(onMethod = @__({@Override}))
    @Setter(onMethod = @__({@Override}))
    private DoorOwner doorOwner;

    // "cached" values that only get calculated when first retrieved.
    private Vector2Di engineChunk = null;
    private Integer blockCount = null;

    /**
     * Min and Max Vector2Di coordinates of the range of Vector2Dis that this {@link AbstractDoorBase} might interact
     * with.
     */
    private Vector2Di minChunkCoords = null, maxChunkCoords = null;

    /**
     * Constructs a new {@link AbstractDoorBase}.
     */
    protected AbstractDoorBase(final @NotNull DoorData doorData)
    {
        doorUID = doorData.getUid();

        name = doorData.getName();
        minimum = doorData.getMin();
        maximum = doorData.getMax();
        engine = doorData.getEngine();
        powerBlock = doorData.getPowerBlock();
        world = doorData.getWorld();
        open = doorData.isOpen();
        openDir = doorData.getOpenDirection();
        doorOwner = doorData.getDoorOwner();
        isLocked = doorData.isLocked();
        dimensions = calculateDimensions();

        doorOpeningUtility = DoorOpeningUtility.get();
        onCoordsUpdate();
    }

    /**
     * Gets the {@link DoorType} of this door.
     *
     * @return The {@link DoorType} of this door.
     */
    public abstract DoorType getDoorType();

    @Override
    public boolean isPowerBlockActive()
    {
        Vector3Di powerBlockChunkSpaceCoords = Util.getChunkSpacePosition(getPowerBlock());
        Vector2Di powerBlockChunk = Util.getChunkCoords(getPowerBlock());
        if (BigDoors.get().getPlatform().getChunkManager().load(getWorld(), powerBlockChunk) ==
            IChunkManager.ChunkLoadResult.FAIL)
        {
            PLogger.get()
                   .logException(new IllegalStateException("Failed to load chunk at: " + powerBlockChunk.toString()));
            return false;
        }

        // TODO: Make sure that all corners around the block are also loaded (to check redstone).
        //       Might have to load up to 3 chunks.
        return BigDoors.get().getPlatform().getPowerBlockRedstoneManager()
                       .isBlockPowered(getWorld(), getPowerBlock());
    }

    @Override
    public boolean isValidOpenDirection(final @NotNull RotateDirection openDir)
    {
        return false;
    }

    /**
     * Attempts to toggle a door. Think twice before using this method. Instead, please look at {@link
     * DoorOpener#animateDoorAsync(AbstractDoorBase, DoorActionCause, IMessageable, IPPlayer, double, boolean,
     * DoorActionType)}.
     *
     * @param cause           What caused this action.
     * @param messageReceiver Who will receive any messages that have to be sent.
     * @param responsible     Who is responsible for this door. Either the player who directly toggled it (via a command
     *                        or the GUI), or the original creator when this data is not available.
     * @param time            The amount of time this {@link AbstractDoorBase} will try to use to move. The maximum
     *                        speed is limited, so at a certain point lower values will not increase door speed.
     * @param skipAnimation   If the {@link AbstractDoorBase} should be opened instantly (i.e. skip animation) or not.
     * @param actionType      The type of action.
     * @return The result of the attempt.
     */
    final @NotNull DoorToggleResult toggle(final @NotNull DoorActionCause cause,
                                           final @NotNull IMessageable messageReceiver,
                                           final @NotNull IPPlayer responsible, final double time,
                                           boolean skipAnimation,
                                           final @NotNull DoorActionType actionType)
    {
        if (openDir == RotateDirection.NONE)
        {
            IllegalStateException e = new IllegalStateException("OpenDir cannot be NONE!");
            PLogger.get().logException(e);
            throw e;
        }

        if (skipAnimation && !canSkipAnimation())
            return doorOpeningUtility.abort(this, DoorToggleResult.ERROR, cause, responsible);

        DoorToggleResult isOpenable = doorOpeningUtility.canBeToggled(this, cause, actionType);
        if (isOpenable != DoorToggleResult.SUCCESS)
            return doorOpeningUtility.abort(this, isOpenable, cause, responsible);

        if (LimitsManager.exceedsLimit(responsible, Limit.DOOR_SIZE, getBlockCount()))
            return doorOpeningUtility.abort(this, DoorToggleResult.TOOBIG, cause, responsible);

        Vector3Di newMin = new Vector3Di(getMinimum());
        Vector3Di newMax = new Vector3Di(getMaximum());

        if (!getPotentialNewCoordinates(newMin, newMax))
            return doorOpeningUtility.abort(this, DoorToggleResult.ERROR, cause, responsible);

        IDoorEventTogglePrepare prepareEvent = BigDoors.get().getPlatform().getDoorActionEventFactory()
                                                       .createPrepareEvent(this, cause, actionType, responsible, time,
                                                                           skipAnimation, newMin, newMax);
        BigDoors.get().getPlatform().callDoorActionEvent(prepareEvent);
        if (prepareEvent.isCancelled())
            return doorOpeningUtility.abort(this, DoorToggleResult.CANCELLED, cause, responsible);

        if (!doorOpeningUtility.isLocationEmpty(newMin, newMax, minimum, maximum,
                                                cause.equals(DoorActionCause.PLAYER) ? responsible : null, getWorld()))
            return doorOpeningUtility.abort(this, DoorToggleResult.OBSTRUCTED, cause, responsible);

        if (!doorOpeningUtility.canBreakBlocksBetweenLocs(this, newMin, newMax, responsible))
            return doorOpeningUtility.abort(this, DoorToggleResult.NOPERMISSION, cause, responsible);

        registerBlockMover(cause, time, skipAnimation, newMin, newMax, responsible, actionType);

        BigDoors.get().getPlatform().callDoorActionEvent(BigDoors.get().getPlatform().getDoorActionEventFactory()
                                                                 .createStartEvent(this, cause, actionType, responsible,
                                                                                   time, skipAnimation, newMin,
                                                                                   newMax));

        return DoorToggleResult.SUCCESS;
    }

    @Override
    public final void onRedstoneChange(final int newCurrent)
    {
        IPPlayer player = BigDoors.get().getPlatform().getPPlayerFactory().create(getDoorOwner().getPlayer().getUUID(),
                                                                                  getDoorOwner().getPlayer().getName());
        if (newCurrent == 0 && isCloseable())
            DoorOpener.get().animateDoorAsync(this, DoorActionCause.REDSTONE,
                                              BigDoors.get().getPlatform().getMessageableServer(), player, 0.0D,
                                              false, DoorActionType.CLOSE);
        else if (newCurrent > 0 && isOpenable())
            DoorOpener.get().animateDoorAsync(this, DoorActionCause.REDSTONE,
                                              BigDoors.get().getPlatform().getMessageableServer(), player, 0.0D,
                                              false, DoorActionType.OPEN);
    }

    /**
     * Starts and registers a new {@link BlockMover}.
     *
     * @param cause         What caused this action.
     * @param time          The amount of time this {@link AbstractDoorBase} will try to use to move. The maximum speed
     *                      is limited, so at a certain point lower values will not increase door speed.
     * @param skipAnimation If the {@link AbstractDoorBase} should be opened instantly (i.e. skip animation) or not.
     * @param newMin        The new minimum position this door will have after the toggle.
     * @param newMax        The new maximmum position this door will have after the toggle.
     * @param responsible   The {@link IPPlayer} responsible for the door action.
     * @param actionType    The type of action that will be performed by the BlockMover.
     */
    protected abstract void registerBlockMover(final @NotNull DoorActionCause cause, final double time,
                                               final boolean skipAnimation, final @NotNull IVector3DiConst newMin,
                                               final @NotNull IVector3DiConst newMax,
                                               final @NotNull IPPlayer responsible,
                                               final @NotNull DoorActionType actionType);

    @Override
    public @NotNull Vector2Di[] calculateCurrentChunkRange()
    {
        Vector2Di minChunk = Util.getChunkCoords(minimum);
        Vector2Di maxChunk = Util.getChunkCoords(maximum);

        return new Vector2Di[]{new Vector2Di(minChunk.getX(), minChunk.getY()),
                               new Vector2Di(maxChunk.getX(), maxChunk.getY())};
    }

    @Override
    public final boolean chunkInRange(final @NotNull IPWorld otherWorld, final @NotNull IVector2DiConst chunk)
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
     */
    private void verifyChunkRange()
    {
        if (maxChunkCoords == null || minChunkCoords == null)
        {
            Vector2Di[] range = calculateChunkRange();
            minChunkCoords = range[0];
            maxChunkCoords = range[1];
        }
    }

    @Override
    public final @NotNull UUID getPlayerUUID()
    {
        if (doorOwner == null)
        {
            NullPointerException npe = new NullPointerException(
                "Door " + getDoorUID() + " did not have an owner! Please contact pim16aap2.");
            PLogger.get().logException(npe);
            throw npe;
        }
        return doorOwner.getPlayer().getUUID();
    }

    @Override
    public final int getPermission()
    {
        return doorOwner == null ? -1 : doorOwner.getPermission();
    }

    @Override
    public final void setOpenDir(final @NotNull RotateDirection newRotDir)
    {
        if (newRotDir.equals(RotateDirection.NONE))
        {
            PLogger.get()
                   .logMessage(Level.WARNING,
                               "\"NONE\" is not a valid rotate direction! Defaulting to: \"" + getOpenDir() + "\".");
            return;
        }
        openDir = newRotDir;
        invalidateChunkRange();
    }

    /**
     * {@inheritDoc}
     * <p>
     * Triggers {@link #onCoordsUpdate()}.
     */
    @Override
    public final void setMinimum(final @NotNull IVector3DiConst pos)
    {
        minimum = new Vector3Di(pos);
        onCoordsUpdate();
    }

    /**
     * {@inheritDoc}
     * <p>
     * Triggers {@link #onCoordsUpdate()}.
     */
    @Override
    public final void setMaximum(final @NotNull IVector3DiConst pos)
    {
        maximum = new Vector3Di(pos);
        onCoordsUpdate();
    }

    /**
     * Make sure variables that depend on the coordinates of the {@link AbstractDoorBase} are invalidated or
     * recalculated when applicable.
     */
    private void onCoordsUpdate()
    {
        invalidateCoordsDependents();
        dimensions = calculateDimensions();
    }

    /**
     * Invalidate Vector2DiRange for this door.
     * <p>
     * This function should be called whenever certain aspects change that affect the way/distance this door might move.
     * E.g. the open direction.
     */
    protected void invalidateChunkRange()
    {
        maxChunkCoords = null;
        minChunkCoords = null;
    }

    @Override
    public final @NotNull Vector2Di[] getChunkRange()
    {
        verifyChunkRange();
        return new Vector2Di[]{minChunkCoords, maxChunkCoords};
    }

    /**
     * Invalidate variables that depend on the coordinates of the {@link AbstractDoorBase} when those are modified.
     * <p>
     * Only applies to variables that are not guaranteed to always be available. They are calculated when needed.
     */
    private void invalidateCoordsDependents()
    {
        engineChunk = null;
        blockCount = null;
        invalidateChunkRange();
    }

    /**
     * Calculates the dimensions of the door based on its current min/max coordinates.
     *
     * @return The current dimensions.
     */
    protected Vector3Di calculateDimensions()
    {
        return new Vector3Di(maximum.getX() - minimum.getX(),
                             maximum.getY() - minimum.getY(),
                             maximum.getZ() - minimum.getZ());
    }

    /**
     * {@inheritDoc}
     * <p>
     * Triggers {@link #onCoordsUpdate()}.
     */
    @Override
    public final void setEnginePosition(final @NotNull IVector3DiConst pos)
    {
        engine = new Vector3Di(pos);
        onCoordsUpdate();
    }

    /**
     * {@inheritDoc}
     * <p>
     * Triggers {@link #onCoordsUpdate()}.
     */
    @Override
    public final void setPowerBlockPosition(final @NotNull IVector3DiConst pos)
    {
        powerBlock = new Vector3Di(pos);
        onCoordsUpdate();
    }

    private @NotNull Vector2Di calculateEngineChunk()
    {
        return Util.getChunkCoords(engine);
    }

    @Override
    public final @NotNull IVector2DiConst getChunk()
    {
        return engineChunk == null ? engineChunk = calculateEngineChunk() : engineChunk;
    }

    /**
     * Calculate the total number of blocks this {@link AbstractDoorBase} is made out of.
     *
     * @return Total number of blocks this {@link AbstractDoorBase} is made out of.
     */
    private int calculateBlockCount()
    {
        return (dimensions.getX() + 1) * (dimensions.getY() + 1) * (dimensions.getZ() + 1);
    }

    /**
     * {@inheritDoc}
     * <p>
     * It can be invalidated by {@link #invalidateCoordsDependents()}.
     */
    @Override
    public final int getBlockCount()
    {
        return blockCount == null ? blockCount = calculateBlockCount() : blockCount;
    }

    @Override
    public final long getSimplePowerBlockChunkHash()
    {
        return Util.simpleChunkHashFromLocation(powerBlock.getX(), powerBlock.getZ());
    }

    @Override
    public final @NotNull String getBasicInfo()
    {
        return doorUID + " (" + getPermission() + ")" + ": " + name;
    }

    /**
     * @return String with (almost) all data of this door.
     */
    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder();
        builder.append(doorUID).append(": ").append(name).append("\n");
        builder.append("Type: ").append(getDoorType().toString()).append(". Permission: ").append(getPermission())
               .append("\n");
        builder.append("Min: ").append(minimum.toString()).append(", Max: ").append(maximum.toString())
               .append(", Engine: ")
               .append(engine.toString()).append("\n");
        builder.append("PowerBlock position: ").append(powerBlock.toString()).append(". Hash: ")
               .append(getSimplePowerBlockChunkHash()).append("\n");
        builder.append("World: ").append(getWorld().getUID().toString()).append("\n");
        builder.append("This door is ").append((isLocked ? "" : "NOT ")).append("locked. ");
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
        return doorUID == other.doorUID && name.equals(other.name) && minimum.equals(other.minimum) &&
            maximum.equals(other.maximum) && engine.equals(other.engine) && getDoorType().equals(other.getDoorType()) &&
            open == other.open && doorOwner.equals(other.doorOwner) && isLocked == other.isLocked &&
            world.getUID().equals(other.world.getUID());
    }

    /**
     * POD class that stores all the data needed for basic door initialization.
     *
     * @author Pim
     */
    @Value
    @AllArgsConstructor
    public static class DoorData
    {
        /**
         * The UID of this door.
         */
        long uid;

        /**
         * The name of this door.
         */
        @NotNull
        String name;

        /**
         * The location with the coordinates closest to the origin.
         */
        @NotNull
        IVector3DiConst min;

        /**
         * The location with the coordinates furthest away from the origin.
         */
        @NotNull
        IVector3DiConst max;

        /**
         * The location of the engine.
         */
        @NotNull
        IVector3DiConst engine;

        /**
         * The location of the powerblock.
         */
        @NotNull
        IVector3DiConst powerBlock;

        /**
         * The {@link IPWorld} this door is in.
         */
        @NotNull
        IPWorld world;

        /**
         * Whether or not this door is currently open.
         */
        boolean isOpen; // TODO: Use the bitflag here instead.

        /**
         * The open direction of this door.
         */
        @NotNull
        RotateDirection openDirection;

        /**
         * The {@link DoorOwner} of this door.
         */
        @NotNull
        DoorOwner doorOwner;

        /**
         * Whether or not this door is currently locked.
         */
        boolean isLocked;
    }
}
