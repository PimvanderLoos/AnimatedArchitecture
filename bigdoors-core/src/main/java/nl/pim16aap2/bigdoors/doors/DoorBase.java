package nl.pim16aap2.bigdoors.doors;

import nl.pim16aap2.bigdoors.spigotutil.SpigotUtil;
import nl.pim16aap2.bigdoors.util.DoorOwner;
import nl.pim16aap2.bigdoors.util.Mutable;
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
import org.jetbrains.annotations.Nullable;

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

    protected Location min;
    protected Location max;
    protected String name;
    protected DoorType doorType;
    protected World world = null;
    protected boolean isOpen;
    protected Location engine;
    protected RotateDirection openDir;
    protected boolean isLocked;
    protected int autoClose = -1;
    protected PBlockFace engineSide;
    protected Location powerBlock;
    protected int blocksToMove;
    protected DoorOwner doorOwner;
    protected Vector3D dimensions;

    // "cached" values.
    private Chunk engineChunk = null;
    private Integer blockCount = null;
    private PBlockFace currentDirection = null;
    // Min and Max chunk coordinates of the range of chunks that
    // this door might interact with.
    private Vector2D minChunkCoords = null;
    private Vector2D maxChunkCoords = null;

    DoorBase(final PLogger pLogger, final long doorUID, final DoorType doorType)
    {
        this.pLogger = pLogger;
        this.doorUID = doorUID;
        this.doorType = doorType;
    }

    /**
     * Cycle the {@link nl.pim16aap2.bigdoors.util.RotateDirection} direction this door will open in. By default it'll
     * set and return the opposite direction of the current direction.
     *
     * @return The new {@link nl.pim16aap2.bigdoors.util.RotateDirection} direction this door will open in.
     */
    public RotateDirection cycleOpenDirection()
    {
        openDir = RotateDirection.getOpposite(openDir);
        return openDir;
    }

    /**
     * Calculate the {@link PBlockFace} of the side the door is on relative to its engine.
     * <p>
     * When taking a {@link nl.pim16aap2.bigdoors.doors.BigDoor} as an example, it would return NORTH if the door was
     * positioned along the x-axis (North / South) and the engine was on the south-most point of the door.
     *
     * @return The {@link PBlockFace} of the side the door is on relative to its engine.
     */
    public abstract PBlockFace calculateCurrentDirection();

    /**
     * Calculate the Min and Max coordinates of the range of chunks that this door might interact with.
     *
     * @return 2 {@link nl.pim16aap2.bigdoors.util.Vector2D}. Min and Max coordinates of chunks in range.
     */
    public abstract Vector2D[] calculateChunkRange();

    /**
     * Find out the default open direction for the current setup and use {@link #setOpenDir(RotateDirection)} to set the
     * open direction to the default one. Useful when trying to open a door before an open direction was set.
     * <p>
     * There is no universal default because the default depends on the type and current orientation of the door.
     * <p>
     * The openDirection is guaranteed to not be null or NONE after running this method. A type may look for a direction
     * that isn't blocked, but that is not guaranteed.
     */
    public abstract void setDefaultOpenDirection();

    /**
     * Get the new min and max values of the door and the new engine side when opened according to the specified
     * variables.
     *
     * @param openDirection   The {@link PBlockFace} the door will open in.
     * @param rotateDirection The {@link nl.pim16aap2.bigdoors.util.RotateDirection} the door will rotate in.
     * @param newMin          The new minimum location (mutable) of the door. x,y,z values are set in the method.
     * @param newMax          The new maximum location (mutable) of the door. x,y,z values are set in the method.
     * @param blocksMoved     The number of blocks the door actually moved. Note that this differs from the suggested
     *                        number of blocks to move!
     * @param newEngineSide   The new {@link PBlockFace} describing the side the engine would be on if opened according
     *                        to the provided variables. Using {@link nl.pim16aap2.bigdoors.util.Mutable} to make it
     *                        mutable.
     */
    public abstract void getNewLocations(PBlockFace openDirection, RotateDirection rotateDirection,
                                         @NotNull Location newMin, @NotNull Location newMax, int blocksMoved,
                                         @Nullable Mutable<PBlockFace> newEngineSide);

    /**
     * Check if a provided chunk is in range of the door. Range in this case refers to all chunks this door could
     * potentially occupy using animated blocks.
     *
     * @param chunk The chunk to check
     * @return True if the chunk is in range of the door.
     */
    public final boolean chunkInRange(Chunk chunk)
    {
        verifyChunkRange();
        return Util.between(chunk.getX(), minChunkCoords.getX(), maxChunkCoords.getX()) &&
                Util.between(chunk.getZ(), minChunkCoords.getY(), maxChunkCoords.getY());
        // It's a Vector2D, so there's no z. Instead of Z, use the second value (Y).
    }

    /**
     * Verify that the ranges of chunk coordinates are available. if not, {@link #calculateChunkRange()} is called to
     * calculate and store them.
     */
    public void verifyChunkRange()
    {
        if (maxChunkCoords == null || minChunkCoords == null)
        {
            Vector2D[] range = calculateChunkRange();
            minChunkCoords = range[0];
            maxChunkCoords = range[1];
        }
    }

    /**
     * Get the number of blocks this door will try to move. As explained at {@link #setBlocksToMove(int)}, the door is
     * not guaranteed to move as far as specified.
     *
     * @return The number of blocks the door will try to move.
     */
    public int getBlocksToMove()
    {
        return blocksToMove;
    }

    /**
     * Change the number of blocks this door will try to move when opened. Note that this is only a suggestion. It will
     * never move more blocks than possible. Values less than 1 will let the door move as many blocks as possible.
     * <p>
     * Also calls {@link #invalidateChunkRange()}
     *
     * @param newBTM The number of blocks the door will try to move.
     */
    public void setBlocksToMove(int newBTM)
    {
        blocksToMove = newBTM;
        invalidateChunkRange();
    }

    /**
     * The the {@link nl.pim16aap2.bigdoors.doors.DoorType} of this door.
     *
     * @return The {@link nl.pim16aap2.bigdoors.doors.DoorType} of this door.
     */
    public DoorType getType()
    {
        return doorType;
    }

    /**
     * Get the name of this door.
     *
     * @return The name of this door.
     */
    public @NotNull String getName()
    {
        return name;
    }

    /**
     * Change the name of the door.
     *
     * @param name The new name of this door.
     */
    public void setName(String name)
    {
        this.name = name;
    }

    /**
     * Get the world this door exists in.
     *
     * @return The world this door exists in
     */
    public World getWorld()
    {
        return world;
    }

    /**
     * Set the world this door exists in.
     *
     * @param world The world the door exists in.
     * @throws IllegalStateException when trying to change the world when it's already been set.
     */
    public void setWorld(World world)
    {
        if (this.world != null)
            throw new IllegalStateException();
        this.world = world;
    }

    /**
     * Get the UID of the door as used in the database. Guaranteed to be unique and available.
     *
     * @return The UID of the door as used in the database.
     */
    public final long getDoorUID()
    {
        return doorUID;
    }

    /**
     * Check if the door is currently locked. When locked, doors cannot be opened.
     *
     * @return True if the door is locked
     */
    public boolean isLocked()
    {
        return isLocked;
    }

    /**
     * Check if the door is currently open.
     *
     * @return True if the door is open
     */
    public boolean isOpen()
    {
        return isOpen;
    }

    /**
     * Get the UUID of the current owner of the door if possible. Not that there can be multiple owners with varying
     * permissions, so this method isn't guaranteed to return the owner.
     * <p>
     * Returns null if the owner of the door wasn't set.
     *
     * @return The UUID of the current owner if available
     */
    public UUID getPlayerUUID()
    {
        if (doorOwner == null)
            return null;
        return doorOwner.getPlayerUUID();
    }

    /**
     * Get the permission level of the current owner of the door. If the owner wasn't set (as explained at {@link
     * #getPlayerUUID()}, -1 is returned.
     *
     * @return The permission of the current owner if available, -1 otherwise
     */
    public int getPermission()
    {
        if (doorOwner == null)
            return -1;
        return doorOwner.getPermission();
    }

    /**
     * Get the side the engine is on relative to the rest of the door. When taking a {@link
     * nl.pim16aap2.bigdoors.doors.BigDoor} as example, this would return North if the door was place along the z axis
     * (North/South) and the engine was at the north-most point.
     *
     * @return The side of the door the engine is on relative to the rest of the door.
     */
    public PBlockFace getEngineSide()
    {
        return engineSide;
    }

    /**
     * Change the engineSide of this door.
     *
     * @param newEngineSide The new {@link PBlockFace} engine side of this door
     */
    public void setEngineSide(PBlockFace newEngineSide)
    {
        engineSide = newEngineSide;
    }

    /**
     * Get the {@link nl.pim16aap2.bigdoors.util.RotateDirection} this door will open if currently closed. Note that if
     * it's currently in the open status, it'll go in the opposite direction, as the closing direction is the opposite
     * of the opening direction.
     *
     * @return The {@link nl.pim16aap2.bigdoors.util.RotateDirection} this door will open in.
     */
    public RotateDirection getOpenDir()
    {
        return openDir;
    }

    /**
     * Change the direction the door will open in.
     * <p>
     * Also calls {@link #invalidateChunkRange()}.
     *
     * @param newRotDir New {@link nl.pim16aap2.bigdoors.util.RotateDirection} direction the door will open in.
     */
    public void setOpenDir(RotateDirection newRotDir)
    {
        openDir = newRotDir;
        invalidateChunkRange();
    }

    /**
     * Get amount of time (in seconds) this door will wait before automatically trying to close after having been
     * opened.
     *
     * @return The amount of time (in seconds) after which the door will close automatically.
     */
    public int getAutoClose()
    {
        return autoClose;
    }

    /**
     * Change the amount of time (in seconds) this door will wait before automatically trying to close after having been
     * opened. Negative values disable auto-closing altogether.
     *
     * @param newVal Time (in seconds) after which the door will close after opening.
     */
    public void setAutoClose(int newVal)
    {
        autoClose = newVal;
    }

    /**
     * Change the open-status of this door. True if open, False if closed.
     *
     * @param bool The new open-status of the door.
     */
    public void setOpenStatus(boolean bool)
    {
        isOpen = bool;
    }

    /**
     * Get a copy of the power block location of this door.
     *
     * @return A copy of the power block location of this door.
     */
    public Location getPowerBlockLoc()
    {
        return powerBlock.clone();
    }

    /**
     * Get a copy of the engine location of this door.
     *
     * @return A copy of the engine location of this door.
     */
    public Location getEngine()
    {
        return engine.clone();
    }

    /**
     * Get a copy of the minimum location of this door.
     *
     * @return A copy of the minimum location of this door.
     */
    public Location getMinimum()
    {
        return min.clone();
    }

    /**
     * Change the 'minimum' location of this door to a copy of the provided location.
     * <p>
     * Triggers {@link #onCoordsUpdate()}.
     *
     * @param loc The new minimum location to copy
     */
    public void setMinimum(final Location loc)
    {
        min = loc.clone();
        onCoordsUpdate();
    }

    /**
     * Get a copy of the maximum location of this door.
     *
     * @return A copy of the maximum location of this door.
     */
    public Location getMaximum()
    {
        return max.clone();
    }

    /**
     * Change the 'maximum' location of this door to a copy of the provided location.
     * <p>
     * Triggers {@link #onCoordsUpdate()}.
     *
     * @param loc The new maximum location to copy.
     */
    public void setMaximum(final Location loc)
    {
        max = loc.clone();
        onCoordsUpdate();
    }

    /**
     * Check if the coordinates of the door are in a valid state.
     *
     * @return True if all coordinates are in a valid state.
     */
    public final boolean validCoords()
    {
        return engine != null && max != null && min != null;
    }

    /**
     * Make sure all coordinates are in a valid state as described by {@link #validCoords()}.
     *
     * @throws IllegalStateException if any of the coordinates are not in a valid state.
     */
    public final void assertValidCoords()
    {
        if (!validCoords())
            throw new IllegalStateException();
    }

    /**
     * Make sure variables that depend on the coordinates of the door are invalidated or recalculated when applicable.
     */
    private void onCoordsUpdate()
    {
        invalidateCoordsDependents();
        if (validCoords())
            updateCoordsDependents();
    }

    /**
     * Invalidate chunkRange for this door.
     */
    public void invalidateChunkRange()
    {
        maxChunkCoords = null;
        minChunkCoords = null;
    }

    /**
     * Get the the chunk coordinates of the min and max chunks that are in range of this door.
     *
     * @return The chunk coordinates of the min and max chunks in range of this door.
     */
    public final Vector2D[] getChunkRange()
    {
        return new Vector2D[]{minChunkCoords, maxChunkCoords};
    }

    /**
     * Invalidate variables that depend on the coordinates of the door when those are modified.
     * <p>
     * Only applies to variables that are not guaranteed to always be available. They are calculated when needed.
     */
    protected void invalidateCoordsDependents()
    {
        engineChunk = null;
        blockCount = null;
        currentDirection = null;
        invalidateChunkRange();
    }

    /**
     * Update variables that depend on the coordinates of the door when those are modified.
     * <p>
     * Only applies to variables that are guaranteed to always be available if the door is in a valid state.
     */
    protected void updateCoordsDependents()
    {
        assertValidCoords();
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
    public void setEngineLocation(Location loc)
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
    public void setPowerBlockLocation(final Location loc)
    {
        powerBlock = loc.clone();
        onCoordsUpdate();
    }

    /**
     * Changes the lock status of this door. Locked doors cannot be opened.
     *
     * @param lock New lock status.
     */
    public void setLock(boolean lock)
    {
        isLocked = lock;
    }

    /**
     * Change the doorOwner of this door.
     *
     * @param doorOwner The new {@link DoorOwner} doorOwner of this door
     */
    public void setDoorOwner(DoorOwner doorOwner)
    {
        this.doorOwner = doorOwner;
    }

    /**
     * Gets the owner of this door.
     *
     * @return The owner of this door.
     */
    public @NotNull DoorOwner getDoorOwner()
    {
        return doorOwner;
    }

    /**
     * Calculate in which chunk the power block of this door resides in.
     *
     * @return The chunk the power block of this door resides in.
     */
    protected Chunk calculateEngineChunk()
    {
        assertValidCoords();
        return world.getBlockAt((int) engine.getX(), (int) engine.getY(), (int) engine.getZ()).getChunk();
    }

    /**
     * Retrieve the chunk the power block of this door resides in. If not calculated/invalidated, {@link
     * #calculateEngineChunk()} is called to (re)calculate it.
     * <p>
     * It's calculated once and then stored until invalidated by {@link #invalidateCoordsDependents()}.
     *
     * @return The chunk the power block of this door resides in.
     */
    public final Chunk getChunk()
    {
        if (engineChunk == null)
            engineChunk = calculateEngineChunk();
        return engineChunk;
    }

    /**
     * Calculate the total number of blocks this door is made out of.
     *
     * @return Total number of blocks this door is made out of.
     */
    protected int calculateBlockCount()
    {
        assertValidCoords();
        int xLen = Math.abs(max.getBlockX() - min.getBlockX()) + 1;
        int yLen = Math.abs(max.getBlockY() - min.getBlockY()) + 1;
        int zLen = Math.abs(max.getBlockZ() - min.getBlockZ()) + 1;
        xLen = xLen == 0 ? 1 : xLen;
        yLen = yLen == 0 ? 1 : yLen;
        zLen = zLen == 0 ? 1 : zLen;

        return xLen * yLen * zLen;
    }

    /**
     * Retrieve the total number of blocks this door is made out of. If not calculated/invalidated, {@link
     * #calculateBlockCount()} is called to (re)calculate it.
     * <p>
     * It's calculated once and then stored until invalidated by {@link #invalidateCoordsDependents()}.
     *
     * @return Total number of blocks this door is made out of.
     */
    public final int getBlockCount()
    {
        if (blockCount == null)
            blockCount = calculateBlockCount();
        return blockCount;
    }

    /**
     * Get the side the door is on relative to the engine. If invalidated or not calculated yet, {@link
     * #calculateCurrentDirection()} is called to (re)calculate it.
     *
     * @return The side the door is on relative to the engine
     */
    public final PBlockFace getCurrentDirection()
    {
        if (currentDirection == null)
            currentDirection = calculateCurrentDirection();
        return currentDirection;
    }

    /**
     * @return The hash of the chunk in which the power block resides.
     */
    public long getPowerBlockChunkHash()
    {
        if (powerBlock == null)
            throw new NullPointerException();
        if (world == null)
            throw new NullPointerException();
        return SpigotUtil.chunkHashFromLocation(powerBlock.getBlockX(), powerBlock.getBlockZ(), world.getUID());
    }

    /**
     * Get basic information of this door: uid, permission, and name.
     *
     * @return String with basic door info
     */
    public String getBasicInfo()
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
        ).append(getPowerBlockChunkHash()).append("\n");
        builder.append("This door is ").append((isLocked ? "" : "NOT ")).append("locked. ");
        builder.append("This door is ").append((isOpen ? "Open.\n" : "Closed.\n"));
        builder.append("OpenDir: ").append(openDir.toString()).append("; Current Dir: ").append(getCurrentDirection())
               .append("\n");

        builder.append("AutoClose: ").append(autoClose);
        builder.append("; BlocksToMove: ").append(blocksToMove).append("\n");

        return builder.toString();
    }


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
