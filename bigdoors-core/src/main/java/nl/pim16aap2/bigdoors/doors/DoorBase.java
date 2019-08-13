package nl.pim16aap2.bigdoors.doors;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.events.dooraction.DoorActionCause;
import nl.pim16aap2.bigdoors.moveblocks.BlockMover;
import nl.pim16aap2.bigdoors.spigotutil.SpigotUtil;
import nl.pim16aap2.bigdoors.util.DoorOwner;
import nl.pim16aap2.bigdoors.util.DoorToggleResult;
import nl.pim16aap2.bigdoors.util.PBlockFace;
import nl.pim16aap2.bigdoors.util.PLogger;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import nl.pim16aap2.bigdoors.util.Util;
import nl.pim16aap2.bigdoors.util.Vector2D;
import nl.pim16aap2.bigdoors.util.Vector3D;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Represents an unspecialized door.
 *
 * @author Pim
 */
public abstract class DoorBase
{
    protected final PLogger pLogger;
    protected final long doorUID;
    protected final DoorType doorType;

    protected Location min;
    protected Location max;
    protected String name;
    protected World world;
    protected boolean isOpen;
    protected Location engine;
    protected RotateDirection openDir;
    protected int blocksToMove;
    protected Vector3D dimensions;

    private boolean isLocked;
    private int autoClose = -1;
    private Location powerBlock;
    private DoorOwner doorOwner;

    // "cached" values.
    private Chunk engineChunk = null;
    private Integer blockCount = null;
    private PBlockFace currentDirection = null;

    @NotNull
    protected final DoorOpener doorOpener;

    /**
     * Min and Max chunk coordinates of the range of chunks that this {@link DoorBase} might interact with.
     */
    private Vector2D minChunkCoords = null, maxChunkCoords = null;

    /**
     * Constructs a new DoorBase.
     *
     * @param pLogger  The {@link PLogger} used for logging.
     * @param doorUID  The UID of this door.
     * @param doorType The type of this door.
     */
    protected DoorBase(final @NotNull PLogger pLogger, final long doorUID, final @NotNull DoorType doorType)
    {
        this.pLogger = pLogger;
        this.doorUID = doorUID;
        this.doorType = doorType;
        if (DoorOpener.get() == null)
        {
            IllegalStateException e = new IllegalStateException("Could not obtain DoorOpener!");
            pLogger.logException(e);
            throw e;
        }
        doorOpener = DoorOpener.get();
    }

    /**
     * Initializes all basic data of this door. This refers to all data required to put this door in a basic valid
     * state. It can then infer further details from this data (such as NorthSouthAxis and dimensions).
     *
     * @param min        The location with the coordinates closest to the origin.
     * @param max        The location with the coordinates furthest away from the origin.
     * @param engine     The location of the engine.
     * @param powerBlock The location of the powerblock.
     * @param world      The world this door is in.
     * @param openDir    The open direction of this door.
     * @param isOpen     Whether or not this door is currently open.
     */
    public final void initBasicData(final @NotNull Location min, final @NotNull Location max,
                                    final @NotNull Location engine, final @NotNull Location powerBlock,
                                    final @NotNull World world, final @NotNull RotateDirection openDir,
                                    final boolean isOpen)
    {
        this.min = min;
        this.max = max;
        this.engine = engine;
        this.powerBlock = powerBlock;
        this.world = world;
        this.isOpen = isOpen;
        this.openDir = openDir;
        onCoordsUpdate();
    }

    /**
     * Checks if this door can be opened right now.
     *
     * @return True if this door can be opened right now.
     */
    public abstract boolean isOpenable();

    /**
     * Checks if this door can be closed right now.
     *
     * @return True if this door can be closed right now.
     */
    public abstract boolean isCloseable();

    // TODO: ABSTRACT! (also, implement)
    public boolean isValidOpenDirection(final @NotNull RotateDirection openDir)
    {
        return false;
    }

    /**
     * Attempts to open a door.
     *
     * @param cause       What caused this action.
     * @param time        The amount of time this {@link DoorBase} will try to use to move. The maximum speed is
     *                    limited, so at a certain point lower values will not increase door speed.
     * @param instantOpen If the {@link DoorBase} should be opened instantly (i.e. skip animation) or not.
     * @return The result of the attempt.
     */
    @NotNull
    public final DoorToggleResult open(final @NotNull DoorActionCause cause,
                                       final double time, final boolean instantOpen)
    {
        if (!isOpenable())
            return DoorToggleResult.ALREADYCLOSED;
        return toggle(cause, time, instantOpen);
    }

    /**
     * Attempts to close a door.
     *
     * @param cause       What caused this action.
     * @param time        The amount of time this {@link DoorBase} will try to use to move. The maximum speed is
     *                    limited, so at a certain point lower values will not increase door speed.
     * @param instantOpen If the {@link DoorBase} should be opened instantly (i.e. skip animation) or not.
     * @return The result of the attempt.
     */
    @NotNull
    public final DoorToggleResult close(final @NotNull DoorActionCause cause,
                                        final double time, final boolean instantOpen)
    {
        if (!isCloseable())
            return DoorToggleResult.ALREADYOPEN;
        return toggle(cause, time, instantOpen);
    }

    /**
     * Attempts to toggle a door.
     *
     * @param cause       What caused this action.
     * @param time        The amount of time this {@link DoorBase} will try to use to move. The maximum speed is
     *                    limited, so at a certain point lower values will not increase door speed.
     * @param instantOpen If the {@link DoorBase} should be opened instantly (i.e. skip animation) or not.
     * @return The result of the attempt.
     */
    @NotNull
    public final DoorToggleResult toggle(final @NotNull DoorActionCause cause,
                                         final double time, boolean instantOpen)
    {
        DoorToggleResult isOpenable = doorOpener.canBeToggled(this, cause);
        if (isOpenable != DoorToggleResult.SUCCESS)
            return doorOpener.abort(this, isOpenable, cause);

        if (doorOpener.isTooBig(this))
            instantOpen = true;

        Location newMin = getMinimum();
        Location newMax = getMaximum();

        if (!getPotentialNewCoordinates(newMin, newMax))
            return doorOpener.abort(this, DoorToggleResult.ERROR, cause);

        if (!doorOpener.isLocationEmpty(newMin, newMax, getMinimum(), getMaximum(),
                                        cause.equals(DoorActionCause.PLAYER) ? getPlayerUUID() : null, getWorld()))
            return doorOpener.abort(this, DoorToggleResult.OBSTRUCTED, cause);

        if (!doorOpener.canBreakBlocksBetweenLocs(this, newMin, newMax))
            return doorOpener.abort(this, DoorToggleResult.NOPERMISSION, cause);

        registerBlockMover(cause, time, instantOpen, newMin, newMax, BigDoors.INSTANCE);
        return DoorToggleResult.SUCCESS;
    }

    /**
     * Handles a change in redstone current for this door's powerblock.
     *
     * @param newCurrent The new current of the powerblock.
     */
    public final void onRedstoneChange(final int newCurrent)
    {

    }

    /**
     * Starts and registers a new {@link BlockMover}.
     *
     * @param cause       What caused this action.
     * @param time        The amount of time this {@link DoorBase} will try to use to move. The maximum speed is
     *                    limited, so at a certain point lower values will not increase door speed.
     * @param instantOpen If the {@link DoorBase} should be opened instantly (i.e. skip animation) or not.
     * @param newMin      The new minimum location this door will have after the toggle.
     * @param newMax      The new maximmum location this door will have after the toggle.
     */
    protected abstract void registerBlockMover(final @NotNull DoorActionCause cause,
                                               final double time, final boolean instantOpen,
                                               final @NotNull Location newMin, final @NotNull Location newMax,
                                               final @NotNull BigDoors plugin);

    /**
     * Gets the direction the door would go given its {@link #getCurrentDirection()} and its {@link #isOpen()} status.
     *
     * @return The direction the door would go if it were to be toggled.
     */
    @NotNull
    public abstract RotateDirection getCurrentToggleDir();

    /**
     * Finds the new minimum and maximum coordinates of this door that would be the result of toggling it.
     *
     * @param min Used to store the new minimum coordinates.
     * @param max Used to store the new maximum coordinates.
     * @return True if the new locations are available.
     */
    protected abstract boolean getPotentialNewCoordinates(final @NotNull Location min, final @NotNull Location max);

    /**
     * Cycle the {@link nl.pim16aap2.bigdoors.util.RotateDirection} direction this {@link DoorBase} will open in. By
     * default it'll set and return the opposite direction of the current direction.
     *
     * @return The new {@link nl.pim16aap2.bigdoors.util.RotateDirection} direction this {@link DoorBase} will open in.
     */
    @NotNull
    public abstract RotateDirection cycleOpenDirection();

    /**
     * Calculate the {@link PBlockFace} of the side the {@link DoorBase} is on relative to its engine.
     * <p>
     * When taking a {@link nl.pim16aap2.bigdoors.doors.BigDoor} as an example, it would return NORTH if the {@link
     * DoorBase} was positioned along the x-axis (North / South) and the engine was on the south-most point of the
     * door.
     *
     * @return The {@link PBlockFace} of the side the {@link DoorBase} is on relative to its engine.
     */
    @NotNull
    public abstract PBlockFace calculateCurrentDirection();

    /**
     * Calculate the Min and Max coordinates of the range of chunks that this {@link DoorBase} might interact with.
     *
     * @return 2 {@link nl.pim16aap2.bigdoors.util.Vector2D}. Min and Max coordinates of chunks in range.
     */
    @NotNull
    public abstract Vector2D[] calculateChunkRange();

    /**
     * Find out the default open direction for the current setup and use {@link #setOpenDir(RotateDirection)} to set the
     * open direction to the default one. Useful when trying to open a {@link DoorBase} before an open direction was
     * set.
     * <p>
     * There is no universal default because the default depends on the type and current orientation of the door.
     * <p>
     * The openDirection is guaranteed to not be null or NONE after running this method. A type may look for a direction
     * that isn't blocked, but that is not guaranteed.
     */
    public abstract void setDefaultOpenDirection();

    /**
     * Check if a provided chunk is in range of the door. Range in this case refers to all chunks this {@link DoorBase}
     * could potentially occupy using animated blocks.
     *
     * @param chunk The chunk to check
     * @return True if the chunk is in range of the door.
     */
    public final boolean chunkInRange(final @NotNull Chunk chunk)
    {
        if (!chunk.getWorld().equals(getWorld()))
            return false;
        verifyChunkRange();
        return Util.between(chunk.getX(), minChunkCoords.getX(), maxChunkCoords.getX()) &&
            Util.between(chunk.getZ(), minChunkCoords.getY(), maxChunkCoords.getY());
        // It's a Vector2D, so there's no z. Instead of Z, use the second value (Y).
    }

    /**
     * Verify that the ranges of chunk coordinates are available. if not, {@link #calculateChunkRange()} is called to
     * calculate and store them.
     */
    private void verifyChunkRange()
    {
        if (maxChunkCoords == null || minChunkCoords == null)
        {
            Vector2D[] range = calculateChunkRange();
            minChunkCoords = range[0];
            maxChunkCoords = range[1];
        }
    }

    /**
     * Get the number of blocks this {@link DoorBase} will try to move. As explained at {@link #setBlocksToMove(int)},
     * the {@link DoorBase} is not guaranteed to move as far as specified.
     *
     * @return The number of blocks the {@link DoorBase} will try to move.
     */
    public final int getBlocksToMove()
    {
        return blocksToMove;
    }

    /**
     * Change the number of blocks this {@link DoorBase} will try to move when opened. Note that this is only a
     * suggestion. It will never move more blocks than possible. Values less than 1 will let the {@link DoorBase} move
     * as many blocks as possible.
     * <p>
     * Also calls {@link #invalidateChunkRange()}
     *
     * @param newBTM The number of blocks the {@link DoorBase} will try to move.
     */
    public final void setBlocksToMove(final int newBTM)
    {
        blocksToMove = newBTM;
        invalidateChunkRange();
    }

    /**
     * The the {@link nl.pim16aap2.bigdoors.doors.DoorType} of this door.
     *
     * @return The {@link nl.pim16aap2.bigdoors.doors.DoorType} of this door.
     */
    @NotNull
    public final DoorType getType()
    {
        return doorType;
    }

    /**
     * Get the name of this door.
     *
     * @return The name of this door.
     */
    @NotNull
    public final String getName()
    {
        return name;
    }

    /**
     * Change the name of the door.
     *
     * @param name The new name of this door.
     */
    public final void setName(final @NotNull String name)
    {
        this.name = name;
    }

    /**
     * Get the world this {@link DoorBase} exists in.
     *
     * @return The world this {@link DoorBase} exists in
     */
    @NotNull
    public final World getWorld()
    {
        return world;
    }

    /**
     * Set the world this {@link DoorBase} exists in.
     *
     * @param world The world the {@link DoorBase} exists in.
     * @throws IllegalStateException when trying to change the world when it's already been set.
     */
    public final void setWorld(final @NotNull World world)
    {
        if (this.world != null && !this.world.equals(world))
        {
            IllegalStateException e = new IllegalStateException("World may not be changed!");
            pLogger.logException(e);
            throw e;
        }
        this.world = world;
    }

    /**
     * Get the UID of the {@link DoorBase} as used in the database. Guaranteed to be unique and available.
     *
     * @return The UID of the {@link DoorBase} as used in the database.
     */
    public final long getDoorUID()
    {
        return doorUID;
    }

    /**
     * Check if the {@link DoorBase} is currently locked. When locked, doors cannot be opened.
     *
     * @return True if the {@link DoorBase} is locked
     */
    public final boolean isLocked()
    {
        return isLocked;
    }

    /**
     * Check if the {@link DoorBase} is currently open.
     *
     * @return True if the {@link DoorBase} is open
     */
    public final boolean isOpen()
    {
        return isOpen;
    }

    /**
     * Get the UUID of the current owner of the {@link DoorBase} if possible. Not that there can be multiple owners with
     * varying permissions, so this method isn't guaranteed to return the owner.
     * <p>
     * Returns null if the owner of the {@link DoorBase} wasn't set.
     *
     * @return The UUID of the current owner if available
     */
    @NotNull
    public final UUID getPlayerUUID()
    {
        if (doorOwner == null)
        {
            NullPointerException npe = new NullPointerException("The door did not have an owner!");
            pLogger.logException(npe);
            throw npe;
        }
        return doorOwner.getPlayerUUID();
    }

    /**
     * Get the permission level of the current owner of the door. If the owner wasn't set (as explained at {@link
     * #getPlayerUUID()}, -1 is returned.
     *
     * @return The permission of the current owner if available, -1 otherwise
     */
    public final int getPermission()
    {
        if (doorOwner == null)
            return -1;
        return doorOwner.getPermission();
    }

    /**
     * Get the {@link nl.pim16aap2.bigdoors.util.RotateDirection} this {@link DoorBase} will open if currently closed.
     * Note that if it's currently in the open status, it'll go in the opposite direction, as the closing direction is
     * the opposite of the opening direction.
     *
     * @return The {@link nl.pim16aap2.bigdoors.util.RotateDirection} this {@link DoorBase} will open in.
     */
    @NotNull
    public final RotateDirection getOpenDir()
    {
        return openDir;
    }

    /**
     * Change the direction the {@link DoorBase} will open in.
     * <p>
     * Also calls {@link #invalidateChunkRange()}.
     *
     * @param newRotDir New {@link nl.pim16aap2.bigdoors.util.RotateDirection} direction the {@link DoorBase} will open
     *                  in.
     */
    public final void setOpenDir(final @NotNull RotateDirection newRotDir)
    {
        if (newRotDir.equals(RotateDirection.NONE))
        {
            setDefaultOpenDirection();
            pLogger.logMessage("\"NONE\" is not a valid rotate direction! Defaulting to: \"" + getOpenDir() + "\".");
            return;
        }
        openDir = newRotDir;
        invalidateChunkRange();
    }

    /**
     * Get amount of time (in seconds) this {@link DoorBase} will wait before automatically trying to close after having
     * been opened.
     *
     * @return The amount of time (in seconds) after which the {@link DoorBase} will close automatically.
     */
    public final int getAutoClose()
    {
        return autoClose;
    }

    /**
     * Change the amount of time (in seconds) this {@link DoorBase} will wait before automatically trying to close after
     * having been opened. Negative values disable auto-closing altogether.
     *
     * @param newVal Time (in seconds) after which the {@link DoorBase} will close after opening.
     */
    public final void setAutoClose(final int newVal)
    {
        autoClose = newVal;
    }

    /**
     * Change the open-status of this door. True if open, False if closed.
     *
     * @param bool The new open-status of the door.
     */
    public final void setOpenStatus(final boolean bool)
    {
        isOpen = bool;
    }

    /**
     * Get a copy of the power block location of this door.
     *
     * @return A copy of the power block location of this door.
     */
    @NotNull
    public final Location getPowerBlockLoc()
    {
        return powerBlock.clone();
    }

    /**
     * Get a copy of the engine location of this door.
     *
     * @return A copy of the engine location of this door.
     */
    @NotNull
    public final Location getEngine()
    {
        return engine.clone();
    }

    /**
     * Get a copy of the minimum location of this door.
     *
     * @return A copy of the minimum location of this door.
     */
    @NotNull
    public final Location getMinimum()
    {
        return min.clone();
    }

    /**
     * Change the 'minimum' location of this {@link DoorBase} to a copy of the provided location.
     * <p>
     * Triggers {@link #onCoordsUpdate()}.
     *
     * @param loc The new minimum location to copy
     */
    public final void setMinimum(final @NotNull Location loc)
    {
        min = loc.clone();
        onCoordsUpdate();
    }

    /**
     * Get a copy of the maximum location of this door.
     *
     * @return A copy of the maximum location of this door.
     */
    @NotNull
    public final Location getMaximum()
    {
        return max.clone();
    }

    /**
     * Change the 'maximum' location of this {@link DoorBase} to a copy of the provided location.
     * <p>
     * Triggers {@link #onCoordsUpdate()}.
     *
     * @param loc The new maximum location to copy.
     */
    public final void setMaximum(final @NotNull Location loc)
    {
        max = loc.clone();
        onCoordsUpdate();
    }

    /**
     * Make sure variables that depend on the coordinates of the {@link DoorBase} are invalidated or recalculated when
     * applicable.
     */
    private void onCoordsUpdate()
    {
        invalidateCoordsDependents();
        updateCoordsDependents();
    }

    /**
     * Invalidate chunkRange for this door.
     */
    private void invalidateChunkRange()
    {
        maxChunkCoords = null;
        minChunkCoords = null;
    }

    /**
     * Get the the chunk coordinates of the min and max chunks that are in range of this door.
     *
     * @return The chunk coordinates of the min and max chunks in range of this door.
     */
    public final @NotNull Vector2D[] getChunkRange()
    {
        return new Vector2D[]{minChunkCoords, maxChunkCoords};
    }

    /**
     * Invalidate variables that depend on the coordinates of the {@link DoorBase} when those are modified.
     * <p>
     * Only applies to variables that are not guaranteed to always be available. They are calculated when needed.
     */
    private void invalidateCoordsDependents()
    {
        engineChunk = null;
        blockCount = null;
        currentDirection = null;
        invalidateChunkRange();
    }

    /**
     * Update variables that depend on the coordinates of the {@link DoorBase} when those are modified.
     * <p>
     * Only applies to variables that are guaranteed to always be available if the {@link DoorBase} is in a valid
     * state.
     */
    protected void updateCoordsDependents()
    {
        dimensions = new Vector3D(max.getBlockX() - min.getBlockX(), max.getBlockY() - min.getBlockY(),
                                  max.getBlockZ() - min.getBlockZ());
    }

    /**
     * Change the location of the engine to a copy of the provided location.
     * <p>
     * Triggers {@link #onCoordsUpdate()}.
     *
     * @param loc The new location of the engine location to copy.
     */
    public final void setEngineLocation(final @NotNull Location loc)
    {
        engine = loc.clone();
        onCoordsUpdate();
    }

    /**
     * Change the location of the power block to a copy of the provided location.
     * <p>
     * Triggers {@link #onCoordsUpdate()}.
     *
     * @param loc The new location of the power block location to copy.
     */
    public final void setPowerBlockLocation(final @NotNull Location loc)
    {
        powerBlock = loc.clone();
        onCoordsUpdate();
    }

    /**
     * Changes the lock status of this door. Locked doors cannot be opened.
     *
     * @param lock New lock status.
     */
    public final void setLock(final boolean lock)
    {
        isLocked = lock;
    }

    /**
     * Change the doorOwner of this door.
     *
     * @param doorOwner The new {@link DoorOwner} doorOwner of this door
     */
    public final void setDoorOwner(final @NotNull DoorOwner doorOwner)
    {
        this.doorOwner = doorOwner;
    }

    /**
     * Gets the owner of this door.
     *
     * @return The owner of this door.
     */
    @NotNull
    public final DoorOwner getDoorOwner()
    {
        return doorOwner;
    }

    /**
     * Calculate in which chunk the power block of this {@link DoorBase} resides in.
     *
     * @return The chunk the power block of this {@link DoorBase} resides in.
     */
    @NotNull
    private Chunk calculateEngineChunk()
    {
        return world.getBlockAt((int) engine.getX(), (int) engine.getY(), (int) engine.getZ()).getChunk();
    }

    /**
     * Retrieve the chunk the power block of this {@link DoorBase} resides in. If not calculated/invalidated, {@link
     * #calculateEngineChunk()} is called to (re)calculate it.
     * <p>
     * It's calculated once and then stored until invalidated by {@link #invalidateCoordsDependents()}.
     *
     * @return The chunk the power block of this {@link DoorBase} resides in.
     */
    public final @NotNull Chunk getChunk()
    {
        if (engineChunk == null)
            engineChunk = calculateEngineChunk();
        return engineChunk;
    }

    /**
     * Calculate the total number of blocks this {@link DoorBase} is made out of.
     *
     * @return Total number of blocks this {@link DoorBase} is made out of.
     */
    private int calculateBlockCount()
    {
        return (dimensions.getX() + 1) * (dimensions.getY() + 1) * (dimensions.getZ() + 1);
    }

    /**
     * Retrieve the total number of blocks this {@link DoorBase} is made out of. If not calculated/invalidated, {@link
     * #calculateBlockCount()} is called to (re)calculate it.
     * <p>
     * It's calculated once and then stored until invalidated by {@link #invalidateCoordsDependents()}.
     *
     * @return Total number of blocks this {@link DoorBase} is made out of.
     */
    public final int getBlockCount()
    {
        if (blockCount == null)
            blockCount = calculateBlockCount();
        return blockCount;
    }

    /**
     * Get the side the {@link DoorBase} is on relative to the engine. If invalidated or not calculated yet, {@link
     * #calculateCurrentDirection()} is called to (re)calculate it.
     *
     * @return The side the {@link DoorBase} is on relative to the engine
     */
    @NotNull
    public final PBlockFace getCurrentDirection()
    {
        if (currentDirection == null)
            currentDirection = calculateCurrentDirection();
        return currentDirection;
    }

    /**
     * @return The simple hash of the chunk in which the power block resides.
     */
    public final long getSimplePowerBlockChunkHash()
    {
        if (powerBlock == null)
        {
            NullPointerException e = new NullPointerException("Powerblock unexpectedly null!");
            pLogger.logException(e);
            throw e;
        }
        if (world == null)
        {
            NullPointerException e = new NullPointerException("world unexpectedly null!");
            pLogger.logException(e);
            throw e;
        }
        return Util.simpleChunkHashFromLocation(powerBlock.getBlockX(), powerBlock.getBlockZ());
    }

    /**
     * Get basic information of this door: uid, permission, and name.
     *
     * @return Basic {@link DoorBase} info
     */
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
        builder.append("Type: ").append(doorType.toString()).append(". Permission: ").append(getPermission())
               .append("\n");
        builder.append("Min: ").append(SpigotUtil.locIntToString(min)).append(", Max: ")
               .append(SpigotUtil.locIntToString(max)
               ).append(", Engine: ").append(SpigotUtil.locIntToString(engine)).append("\n");
        builder.append("PowerBlock location: ").append(SpigotUtil.locIntToString(powerBlock)).append(". Hash: "
        ).append(getSimplePowerBlockChunkHash()).append("\n");
        builder.append("This door is ").append((isLocked ? "" : "NOT ")).append("locked. ");
        builder.append("This door is ").append((isOpen ? "Open.\n" : "Closed.\n"));
        builder.append("OpenDir: ").append(openDir.toString()).append("; Current Dir: ").append(getCurrentDirection())
               .append("\n");

        builder.append("AutoClose: ").append(autoClose);
        builder.append("; BlocksToMove: ").append(blocksToMove).append("\n");

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

        DoorBase other = (DoorBase) o;
        return doorUID == other.doorUID && name.equals(other.name) && min.equals(other.min) &&
            max.equals(other.max) && powerBlock.equals(other.powerBlock) &&
            doorType.equals(other.doorType) && isOpen == other.isOpen && doorOwner.equals(other.doorOwner) &&
            blocksToMove == other.blocksToMove && isLocked == other.isLocked && autoClose == other.autoClose &&
            world.getUID().equals(other.world.getUID());
    }
}
