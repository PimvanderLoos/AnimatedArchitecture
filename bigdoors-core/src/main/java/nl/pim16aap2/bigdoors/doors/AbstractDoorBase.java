package nl.pim16aap2.bigdoors.doors;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.api.IChunkManager;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.api.IPWorld;
import nl.pim16aap2.bigdoors.doortypes.DoorType;
import nl.pim16aap2.bigdoors.events.dooraction.DoorActionCause;
import nl.pim16aap2.bigdoors.events.dooraction.DoorActionType;
import nl.pim16aap2.bigdoors.moveblocks.BlockMover;
import nl.pim16aap2.bigdoors.util.DoorOwner;
import nl.pim16aap2.bigdoors.util.DoorToggleResult;
import nl.pim16aap2.bigdoors.util.PLogger;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import nl.pim16aap2.bigdoors.util.Util;
import nl.pim16aap2.bigdoors.util.vector.Vector2Di;
import nl.pim16aap2.bigdoors.util.vector.Vector3Di;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Represents an unspecialized door.
 *
 * @author Pim
 */
public abstract class AbstractDoorBase implements IDoorBase
{
    protected final long doorUID;
    @NotNull
    protected final DoorOpeningUtility doorOpeningUtility;
    @NotNull
    protected final nl.pim16aap2.bigdoors.api.IPWorld IPWorld;

    protected Vector3Di min, max, engine, powerBlock, dimensions;

    private String name;
    private boolean isOpen;
    private RotateDirection openDir;

    private boolean isLocked;
    private DoorOwner doorOwner;

    // "cached" values that only get calculated when first retrieved.
    private Vector2Di engineChunk = null;
    private Integer blockCount = null;

    /**
     * Min and Max Vector2Di coordinates of the range of Vector2Dis that this {@link AbstractDoorBase} might interact
     * with.
     */
    private Vector2Di minChunkCoords = null, maxChunkCoords = null;

    @Deprecated
    protected AbstractDoorBase(final @NotNull PLogger pLogger, final long uid, final @NotNull DoorData doorData,
                               final @NotNull EDoorType eDoorType)
    {
        System.out.println("USING DEPRECATED DOORBASE CONSTRUCTOR!");
        doorOpeningUtility = null;
        IPWorld = null;
        doorUID = 0;
    }

    /**
     * Constructs a new {@link AbstractDoorBase}.
     */
    protected AbstractDoorBase(final @NotNull DoorData doorData)
    {
        doorUID = doorData.getUID();

        name = doorData.getName();
        min = doorData.getMin();
        max = doorData.getMax();
        engine = doorData.getEngine();
        powerBlock = doorData.getPowerBlock();
        IPWorld = doorData.getWorld();
        isOpen = doorData.getIsOpen();
        openDir = doorData.getOpenDirection();
        doorOwner = doorData.getDoorOwner();
        isLocked = doorData.getIsLocked();

        doorOpeningUtility = DoorOpeningUtility.get();
        onCoordsUpdate();
    }

    /**
     * Gets the {@link DoorType} of this door.
     *
     * @return The {@link DoorType} of this door.
     */
    public abstract DoorType getDoorType();

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isPowerBlockActive()
    {
        Vector3Di powerBlockChunkSpaceCoords = Util.getChunkSpacePosition(getPowerBlockLoc());
        Vector2Di powerBlockChunk = Util.getChunkCoords(getPowerBlockLoc());
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
                       .isBlockPowered(getWorld(), getPowerBlockLoc());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isValidOpenDirection(final @NotNull RotateDirection openDir)
    {
        return false;
    }

    /**
     * Attempts to toggle a door.
     *
     * @param cause         What caused this action.
     * @param initiator     The player that initiated the DoorAction.
     * @param time          The amount of time this {@link AbstractDoorBase} will try to use to move. The maximum speed
     *                      is limited, so at a certain point lower values will not increase door speed.
     * @param skipAnimation If the {@link AbstractDoorBase} should be opened instantly (i.e. skip animation) or not.
     * @param actionType    The type of action.
     * @return The result of the attempt.
     */
    @NotNull
    final DoorToggleResult toggle(final @NotNull DoorActionCause cause, final @NotNull IPPlayer initiator,
                                  final double time, boolean skipAnimation, final @NotNull DoorActionType actionType)
    {
        if (openDir.equals(RotateDirection.NONE))
        {
            IllegalStateException e = new IllegalStateException("OpenDir cannot be NONE!");
            PLogger.get().logException(e);
            throw e;
        }

        if (skipAnimation && !canSkipAnimation())
            doorOpeningUtility.abort(this, DoorToggleResult.ERROR, cause, initiator);

        DoorToggleResult isOpenable = doorOpeningUtility.canBeToggled(this, cause, actionType);
        if (isOpenable != DoorToggleResult.SUCCESS)
            return doorOpeningUtility.abort(this, isOpenable, cause, initiator);

        if (doorOpeningUtility.isTooBig(this))
            return doorOpeningUtility.abort(this, DoorToggleResult.TOOBIG, cause, initiator);

        Vector3Di newMin = getMinimum();
        Vector3Di newMax = getMaximum();

        if (!getPotentialNewCoordinates(newMin, newMax))
            return doorOpeningUtility.abort(this, DoorToggleResult.ERROR, cause, initiator);

        if (!doorOpeningUtility.isLocationEmpty(newMin, newMax, min, max,
                                                cause.equals(DoorActionCause.PLAYER) ? initiator : null, getWorld()))
            return doorOpeningUtility.abort(this, DoorToggleResult.OBSTRUCTED, cause, initiator);

        if (!doorOpeningUtility.canBreakBlocksBetweenLocs(this, newMin, newMax, initiator))
            return doorOpeningUtility.abort(this, DoorToggleResult.NOPERMISSION, cause, initiator);

        registerBlockMover(cause, time, skipAnimation, newMin, newMax, initiator);
        return DoorToggleResult.SUCCESS;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final void onRedstoneChange(final int newCurrent)
    {
        IPPlayer player = BigDoors.get().getPlatform().getPPlayerFactory().create(getDoorOwner().getPlayerUUID(),
                                                                                  getDoorOwner().getPlayerName());
        if (newCurrent == 0 && isCloseable())
            toggle(DoorActionCause.REDSTONE, player, 0.0D, false, DoorActionType.CLOSE);
        else if (newCurrent > 0 && isOpenable())
            toggle(DoorActionCause.REDSTONE, player, 0.0D, false, DoorActionType.OPEN);
    }

    /**
     * Starts and registers a new {@link BlockMover}.
     *
     * @param cause         What caused this action.
     * @param time          The amount of time this {@link AbstractDoorBase} will try to use to move. The maximum speed
     *                      is limited, so at a certain point lower values will not increase door speed.
     * @param skipAnimation If the {@link AbstractDoorBase} should be opened instantly (i.e. skip animation) or not.
     * @param newMin        The new minimum IPLocation this door will have after the toggle.
     * @param newMax        The new maximmum IPLocation this door will have after the toggle.
     * @param initiator     The {@link IPPlayer} responsilbe for the door action.
     */
    protected abstract void registerBlockMover(final @NotNull DoorActionCause cause,
                                               final double time, final boolean skipAnimation,
                                               final @NotNull Vector3Di newMin, final @NotNull Vector3Di newMax,
                                               final @Nullable IPPlayer initiator);

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public Vector2Di[] calculateCurrentChunkRange()
    {
        Vector2Di minChunk = Util.getChunkCoords(min);
        Vector2Di maxChunk = Util.getChunkCoords(max);

        return new Vector2Di[]{new Vector2Di(minChunk.getX(), minChunk.getY()),
                               new Vector2Di(maxChunk.getX(), maxChunk.getY())};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final boolean chunkInRange(final @NotNull IPWorld otherWorld, final @NotNull Vector2Di chunk)
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

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public final String getName()
    {
        return name;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final void setName(final @NotNull String name)
    {
        this.name = name;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public final IPWorld getWorld()
    {
        return IPWorld;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final long getDoorUID()
    {
        return doorUID;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final boolean isLocked()
    {
        return isLocked;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final boolean isOpen()
    {
        return isOpen;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public final UUID getPlayerUUID()
    {
        if (doorOwner == null)
        {
            NullPointerException npe = new NullPointerException(
                "Door " + getDoorUID() + " did not have an owner! Please contact pim16aap2.");
            PLogger.get().logException(npe);
            throw npe;
        }
        return doorOwner.getPlayerUUID();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final int getPermission()
    {
        return doorOwner == null ? -1 : doorOwner.getPermission();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public final RotateDirection getOpenDir()
    {
        return openDir;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final void setOpenDir(final @NotNull RotateDirection newRotDir)
    {
        if (newRotDir.equals(RotateDirection.NONE))
        {
            setDefaultOpenDirection();
            PLogger.get()
                   .logMessage("\"NONE\" is not a valid rotate direction! Defaulting to: \"" + getOpenDir() + "\".");
            return;
        }
        openDir = newRotDir;
        invalidateChunkRange();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final void setOpenStatus(final boolean bool)
    {
        isOpen = bool;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public final Vector3Di getPowerBlockLoc()
    {
        return powerBlock.clone();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public final Vector3Di getEngine()
    {
        return engine.clone();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public final Vector3Di getMinimum()
    {
        return min.clone();
    }

    /**
     * {@inheritDoc}
     * <p>
     * Triggers {@link #onCoordsUpdate()}.
     */
    @Override
    public final void setMinimum(final @NotNull Vector3Di loc)
    {
        min = loc.clone();
        onCoordsUpdate();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public final Vector3Di getMaximum()
    {
        return max.clone();
    }

    /**
     * {@inheritDoc}
     * <p>
     * Triggers {@link #onCoordsUpdate()}.
     */
    @Override
    public final void setMaximum(final @NotNull Vector3Di loc)
    {
        max = loc.clone();
        onCoordsUpdate();
    }

    /**
     * Make sure variables that depend on the coordinates of the {@link AbstractDoorBase} are invalidated or
     * recalculated when applicable.
     */
    private void onCoordsUpdate()
    {
        invalidateCoordsDependents();
        updateCoordsDependents();
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

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public final Vector2Di[] getChunkRange()
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
     * Update variables that depend on the coordinates of the {@link AbstractDoorBase} when those are modified.
     * <p>
     * Only applies to variables that are guaranteed to always be available if the {@link AbstractDoorBase} is in a
     * valid state.
     */
    protected void updateCoordsDependents()
    {
        dimensions = new Vector3Di(max.getX() - min.getX(),
                                   max.getY() - min.getY(),
                                   max.getZ() - min.getZ());
    }

    /**
     * {@inheritDoc}
     * <p>
     * Triggers {@link #onCoordsUpdate()}.
     */
    @Override
    public final void setEngineLocation(final @NotNull Vector3Di loc)
    {
        engine = loc.clone();
        onCoordsUpdate();
    }

    /**
     * {@inheritDoc}
     * <p>
     * Triggers {@link #onCoordsUpdate()}.
     */
    @Override
    public final void setPowerBlockLocation(final @NotNull Vector3Di loc)
    {
        powerBlock = loc.clone();
        onCoordsUpdate();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final void setLock(final boolean lock)
    {
        isLocked = lock;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final void setDoorOwner(final @NotNull DoorOwner doorOwner)
    {
        this.doorOwner = doorOwner;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public final DoorOwner getDoorOwner()
    {
        return doorOwner;
    }

    /**
     * {@inheritDoc}
     */
    @NotNull
    private Vector2Di calculateEngineChunk()
    {
        return Util.getChunkCoords(engine);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public final Vector2Di getChunk()
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

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public final Vector3Di getDimensions()
    {
        return dimensions.clone();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final long getSimplePowerBlockChunkHash()
    {
        if (powerBlock == null)
        {
            NullPointerException e = new NullPointerException("Powerblock unexpectedly null!");
            PLogger.get().logException(e);
            throw e;
        }
        return Util.simpleChunkHashFromLocation(powerBlock.getX(), powerBlock.getZ());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public final String getBasicInfo()
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
        builder.append("Min: ").append(min.toString()).append(", Max: ").append(max.toString()).append(", Engine: ")
               .append(engine.toString()).append("\n");
        builder.append("PowerBlock IPLocation: ").append(powerBlock.toString()).append(". Hash: ")
               .append(getSimplePowerBlockChunkHash()).append("\n");
        builder.append("World: ").append(getWorld().getUID().toString()).append("\n");
        builder.append("This door is ").append((isLocked ? "" : "NOT ")).append("locked. ");
        builder.append("This door is ").append((isOpen ? "Open.\n" : "Closed.\n"));
        builder.append("OpenDir: ").append(openDir.toString()).append("\n");

        return builder.toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object o)
    {
        if (this == o)
            return true;

        if (o == null || getClass() != o.getClass())
            return false;

        AbstractDoorBase other = (AbstractDoorBase) o;
        return doorUID == other.doorUID && name.equals(other.name) && min.equals(other.min) && max.equals(other.max) &&
            getDoorType().equals(other.getDoorType()) && isOpen == other.isOpen && doorOwner.equals(other.doorOwner) &&
            isLocked == other.isLocked && IPWorld.getUID().equals(other.IPWorld.getUID());
    }

    /**
     * POD class that stores all the data needed for basic door initialization.
     *
     * @author Pim
     */
    public static final class DoorData
    {
        private final long uid;
        @NotNull
        private final String name;
        @NotNull
        private final Vector3Di min;
        @NotNull
        private final Vector3Di max;
        @NotNull
        private final Vector3Di engine;
        @NotNull
        private final Vector3Di powerBlock; // TODO: Use a list of powerblocks.
        @NotNull
        private final IPWorld world;
        private final boolean isOpen; // TODO: Use the bitflag here instead.
        @NotNull
        private final RotateDirection openDirection;
        @NotNull
        private final DoorOwner doorOwner;
        private final boolean isLocked;

        /**
         * Initializes all basic data of this door. This refers to all data required to put this door in a basic valid
         * state. It can then infer further details from this data (such as NorthSouthAxis and dimensions).
         *
         * @param uid           The UID of this door.
         * @param name          The name of this door.
         * @param min           The IPLocation with the coordinates closest to the origin.
         * @param max           The IPLocation with the coordinates furthest away from the origin.
         * @param engine        The IPLocation of the engine.
         * @param powerBlock    The IPLocation of the powerblock.
         * @param world         The IPWorld this door is in.
         * @param isOpen        Whether or not this door is currently open.
         * @param openDirection The open direction of this door.
         * @param doorOwner     The {@link DoorOwner} of this door.
         */
        public DoorData(final long uid, final @NotNull String name, final @NotNull Vector3Di min,
                        final @NotNull Vector3Di max, final @NotNull Vector3Di engine,
                        final @NotNull Vector3Di powerBlock, final @NotNull IPWorld world, final boolean isOpen,
                        final @NotNull RotateDirection openDirection, final @NotNull DoorOwner doorOwner,
                        final boolean isLocked)
        {
            this.uid = uid;
            this.name = name;
            this.min = min;
            this.max = max;
            this.engine = engine;
            this.powerBlock = powerBlock;
            this.world = world;
            this.isOpen = isOpen;
            this.openDirection = openDirection;
            this.doorOwner = doorOwner;
            this.isLocked = isLocked;
        }

        private long getUID()
        {
            return uid;
        }

        @NotNull
        private Vector3Di getMin()
        {
            return min;
        }

        @NotNull
        private Vector3Di getMax()
        {
            return max;
        }

        @NotNull
        private Vector3Di getEngine()
        {
            return engine;
        }

        @NotNull
        private Vector3Di getPowerBlock()
        {
            return powerBlock;
        }

        @NotNull
        private IPWorld getWorld()
        {
            return world;
        }

        private boolean getIsOpen()
        {
            return isOpen;
        }

        private boolean getIsLocked()
        {
            return isLocked;
        }

        @NotNull
        private RotateDirection getOpenDirection()
        {
            return openDirection;
        }

        @NotNull
        private String getName()
        {
            return name;
        }

        @NotNull
        private DoorOwner getDoorOwner()
        {
            return doorOwner;
        }
    }
}
