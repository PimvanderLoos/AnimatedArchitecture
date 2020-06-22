package nl.pim16aap2.bigdoors.doors;

import nl.pim16aap2.bigdoors.api.IPWorld;
import nl.pim16aap2.bigdoors.util.DoorOwner;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import nl.pim16aap2.bigdoors.util.vector.IVector2DiConst;
import nl.pim16aap2.bigdoors.util.vector.IVector3DiConst;
import nl.pim16aap2.bigdoors.util.vector.Vector2Di;
import nl.pim16aap2.bigdoors.util.vector.Vector3Di;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Represents a door.
 *
 * @author Pim
 */
public interface IDoorBase
{
    /**
     * Checks if this door can be opened instantly (i.e. skip the animation).
     *
     * @return True, if the door can skip its animation.
     */
    boolean canSkipAnimation();

//    /**
//     * Checks if this door should always be moving (clocks, windmills, etc).
//     *
//     * @return True if this door should always be active.
//     */
//    default boolean perpetualMovement()
//    {
//        return false;
//    }

    /**
     * Checks if the power block of a door is powered.
     *
     * @return True if the power block is receiving a redstone signal.
     */
    boolean isPowerBlockActive();

    /**
     * Checks if this door can be opened right now.
     *
     * @return True if this door can be opened right now.
     */
    boolean isOpenable();

    /**
     * Checks if this door can be closed right now.
     *
     * @return True if this door can be closed right now.
     */
    boolean isCloseable();

    // TODO: implement this.
    boolean isValidOpenDirection(final @NotNull RotateDirection openDir);

    /**
     * Handles a change in redstone current for this door's powerblock.
     *
     * @param newCurrent The new current of the powerblock.
     */
    void onRedstoneChange(final int newCurrent);

    /**
     * Gets the direction the door would go given its current state..
     *
     * @return The direction the door would go if it were to be toggled.
     */
    @NotNull
    RotateDirection getCurrentToggleDir();

    /**
     * Finds the new minimum and maximum coordinates of this door that would be the result of toggling it.
     * <p>
     * Note that the variables themselves are changed.
     *
     * @param newMin Used to store the new minimum coordinates.
     * @param newMax Used to store the new maximum coordinates.
     * @return True if the new locations are available.
     */
    boolean getPotentialNewCoordinates(final @NotNull Vector3Di newMin, final @NotNull Vector3Di newMax);

    /**
     * Cycle the {@link RotateDirection} direction this {@link IDoorBase} will open in. By default it'll set and return
     * the opposite direction of the current direction.
     *
     * @return The new {@link RotateDirection} direction this {@link IDoorBase} will open in.
     */
    @NotNull
    RotateDirection cycleOpenDirection();

    /**
     * Calculates the Min and Max coordinates of the range of Vector2Dis that this {@link IDoorBase} might interact
     * with.
     *
     * @return 2 {@link Vector2Di}. Min and Max coordinates of Vector2Dis in animation range.
     */
    @NotNull
    Vector2Di[] calculateChunkRange();

    /**
     * Calculates the Min and Max coordinates of the range of Vector2Dis that this {@link IDoorBase} might currently
     * exists in.
     *
     * @return 2 {@link Vector2Di}. Min and Max coordinates of Vector2Dis in current range.
     */
    @NotNull
    Vector2Di[] calculateCurrentChunkRange();

    /**
     * Find out the default open direction for the current setup and use.
     * <p>
     * There is no universal default because the default depends on the type and current orientation of the door.
     *
     * @return The default {@link RotateDirection} for this {@link IDoorBase}.
     */
    @NotNull
    RotateDirection getDefaultOpenDirection();

    /**
     * Check if a provided {@link IVector2DiConst} is in range of the door. Range in this case refers to all Vector2Dis
     * this {@link IDoorBase} could potentially occupy using animated blocks.
     *
     * @param chunk The chunk to check
     * @return True if the {@link IVector2DiConst} is in range of the door.
     */
    boolean chunkInRange(final @NotNull IPWorld otherWorld, final @NotNull IVector2DiConst chunk);

    /**
     * Gets the name of this door.
     *
     * @return The name of this door.
     */
    @NotNull
    String getName();

    /**
     * Changes the name of the door.
     *
     * @param name The new name of this door.
     */
    void setName(final @NotNull String name);

    /**
     * Gets the IPWorld this {@link IDoorBase} exists in.
     *
     * @return The IPWorld this {@link IDoorBase} exists in
     */
    @NotNull
    IPWorld getWorld();

    /**
     * Gets the UID of the {@link IDoorBase} as used in the database. Guaranteed to be unique and available.
     *
     * @return The UID of the {@link IDoorBase} as used in the database.
     */
    long getDoorUID();

    /**
     * Check if the {@link IDoorBase} is currently locked. When locked, doors cannot be opened.
     *
     * @return True if the {@link IDoorBase} is locked
     */
    boolean isLocked();

    /**
     * Check if the {@link IDoorBase} is currently open.
     *
     * @return True if the {@link IDoorBase} is open
     */
    boolean isOpen();

    /**
     * Gets the UUID of the current owner of the {@link IDoorBase} if possible. Not that there can be multiple owners
     * with varying permissions, so this method isn't guaranteed to return the owner.
     * <p>
     * Returns null if the owner of the {@link IDoorBase} wasn't set.
     *
     * @return The UUID of the current owner if available
     */
    @NotNull
    UUID getPlayerUUID();

    /**
     * Gets the permission level of the current owner of the door. If the owner wasn't set (as explained at {@link
     * #getPlayerUUID()}, -1 is returned.
     *
     * @return The permission of the current owner if available, -1 otherwise
     */
    int getPermission();

    /**
     * Gets the {@link RotateDirection} this {@link IDoorBase} will open if currently closed. Note that if it's
     * currently in the open status, it'll go in the opposite direction, as the closing direction is the opposite of the
     * opening direction.
     *
     * @return The {@link RotateDirection} this {@link IDoorBase} will open in.
     */
    @NotNull
    RotateDirection getOpenDir();

    /**
     * Changes the direction the {@link IDoorBase} will open in.
     *
     * @param newRotDir New {@link RotateDirection} direction the {@link IDoorBase} will open in.
     */
    void setOpenDir(final @NotNull RotateDirection newRotDir);

    /**
     * Changes the open-status of this door. True if open, False if closed.
     *
     * @param bool The new open-status of the door.
     */
    void setOpen(final boolean bool);

    /**
     * Gets the position of power block of this door.
     *
     * @return The position of the power block of this door.
     */
    @NotNull
    IVector3DiConst getPowerBlockLoc();

    /**
     * Gets the position of the engine of this door.
     *
     * @return The position of the engine block of this door.
     */
    @NotNull
    IVector3DiConst getEngine();

    /**
     * Gets the minimum position of this door.
     *
     * @return The minimum coordinates of this door.
     */
    @NotNull
    IVector3DiConst getMinimum();

    /**
     * Changes the 'minimum' position of this {@link IDoorBase}.
     *
     * @param pos The new minimum position.
     */
    void setMinimum(final @NotNull IVector3DiConst pos);

    /**
     * Gets a copy of the maximum position of this door.
     *
     * @return A copy of the maximum position of this door.
     */
    @NotNull
    IVector3DiConst getMaximum();

    /**
     * Changes the 'maximum' position of this {@link IDoorBase}.
     *
     * @param loc The new maximum position of this door.
     */
    void setMaximum(final @NotNull IVector3DiConst loc);

    /**
     * Gets the the Vector2Di coordinates of the min and max Vector2Dis that are in range of this door.
     * <p>
     * [0] contains the lower bound chunk coordinates, [1] contains the upper bound chunk coordinates.
     *
     * @return The Vector2Di coordinates of the min and max Vector2Dis in range of this door.
     */
    @NotNull
    Vector2Di[] getChunkRange();

    /**
     * Changes the location of the engine.
     *
     * @param loc The new location of the engine.
     */
    void setEngineLocation(final @NotNull IVector3DiConst loc);

    /**
     * Changes the location of the power block.
     *
     * @param loc The new location of the power block.
     */
    void setPowerBlockLocation(final @NotNull IVector3DiConst loc);

    /**
     * Changes the lock status of this door. Locked doors cannot be opened.
     *
     * @param locked New lock status.
     */
    void setLocked(final boolean locked);

    /**
     * Changes the doorOwner of this door.
     *
     * @param doorOwner The new {@link DoorOwner} doorOwner of this door
     */
    void setDoorOwner(final @NotNull DoorOwner doorOwner);

    /**
     * Gets the owner of this door.
     *
     * @return The owner of this door.
     */
    @NotNull
    DoorOwner getDoorOwner();

    /**
     * Retrieve the Vector2Di the power block of this {@link IDoorBase} resides in. If invalidated or not calculated
     * yet, it is (re)calculated first.
     * <p>
     * It's calculated once and then stored until invalidated.
     *
     * @return The Vector2Di the power block of this {@link IDoorBase} resides in.
     */
    @NotNull
    IVector2DiConst getChunk();

    /**
     * Retrieve the total number of blocks this {@link IDoorBase} is made out of. If invalidated or not calculated *
     * yet, it is (re)calculated first.
     * <p>
     * It's calculated once and then stored until invalidated.
     *
     * @return Total number of blocks this {@link IDoorBase} is made out of.
     */
    int getBlockCount();

    /**
     * Gets the dimensions of this door.
     * <p>
     * If a door has a min and max X value of 120, for example, it would have a X-dimension of 0. If the min X value is
     * 119 instead, it would have an X-dimension of 1.
     *
     * @return The dimensions of this door.
     */
    @NotNull
    Vector3Di getDimensions();

    /**
     * @return The simple hash of the chunk in which the power block resides.
     */
    long getSimplePowerBlockChunkHash();

    /**
     * Gets basic information of this door: uid, permission, and name.
     *
     * @return Basic {@link IDoorBase} info
     */
    @NotNull
    String getBasicInfo();

    /**
     * @return String with (almost) all data of this door.
     */
    @Override
    String toString();

    /** {@inheritDoc} */
    @Override
    boolean equals(Object o);
}
