package nl.pim16aap2.bigdoors.doors;

import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.api.IPWorld;
import nl.pim16aap2.bigdoors.util.Cuboid;
import nl.pim16aap2.bigdoors.util.DoorOwner;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import nl.pim16aap2.bigdoors.util.vector.Vector3Di;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Represents a door. Every implementation of this interface should be an {@link DoorBase}.
 *
 * @author Pim
 */
public interface IDoor
{
    /**
     * Checks if this door can be opened right now.
     *
     * @return True if this door can be opened right now.
     */
    boolean isOpenable();

    /**
     * Changes the open-status of this door. True if open, False if closed.
     *
     * @param bool The new open-status of the door.
     */
    void setOpen(boolean bool);

    /**
     * Changes the lock status of this door. Locked doors cannot be opened.
     *
     * @param locked New lock status.
     */
    void setLocked(boolean locked);

    /**
     * Checks if this door can be closed right now.
     *
     * @return True if this door can be closed right now.
     */
    boolean isCloseable();

    /**
     * Gets the {@link Cuboid} representing the area taken up by this door.
     *
     * @return The {@link Cuboid} representing the area taken up by this door.
     */
    @NotNull Cuboid getCuboid();

    /**
     * Gets the name of this door.
     *
     * @return The name of this door.
     */
    @NotNull String getName();

    /**
     * Changes the name of the door.
     *
     * @param name The new name of this door.
     */
    void setName(@NotNull String name);

    /**
     * Gets the IPWorld this {@link IDoor} exists in.
     *
     * @return The IPWorld this {@link IDoor} exists in
     */
    @NotNull IPWorld getWorld();

    /**
     * Gets the UID of the {@link IDoor} as used in the database. Guaranteed to be unique and available.
     *
     * @return The UID of the {@link IDoor} as used in the database.
     */
    long getDoorUID();

    /**
     * Check if the {@link IDoor} is currently locked. When locked, doors cannot be opened.
     *
     * @return True if the {@link IDoor} is locked
     */
    boolean isLocked();

    /**
     * Check if the {@link IDoor} is currently open.
     *
     * @return True if the {@link IDoor} is open
     */
    boolean isOpen();

    /**
     * Gets the prime owner (permission = 0) of this door. In most cases, this will be the original creator of the
     * door.
     *
     * @return The prime owner of this door.
     */
    @NotNull DoorOwner getPrimeOwner();

    /**
     * Gets all {@link DoorOwner}s of this door, including the original creator.
     *
     * @return All {@link DoorOwner}s of this door, including the original creator.
     */
    @NotNull List<@NotNull DoorOwner> getDoorOwners();

    /**
     * Attempts to get the {@link DoorOwner} of this door represented by an {@link IPPlayer}.
     *
     * @param player The player that may or may not be an owner of this door.
     * @return The {@link DoorOwner} of this door for the given player, if this player is a {@link DoorOwner} of this
     * door.
     */
    @NotNull Optional<DoorOwner> getDoorOwner(@NotNull IPPlayer player);

    /**
     * Attempts to get the {@link DoorOwner} of this door represented by the UUID of a player.
     *
     * @param player The UUID of the player that may or may not be an owner of this door.
     * @return The {@link DoorOwner} of this door for the given player, if this player is a {@link DoorOwner} of this
     * door.
     */
    @NotNull Optional<DoorOwner> getDoorOwner(@NotNull UUID player);

    /**
     * Gets the {@link RotateDirection} this {@link IDoor} will open if currently closed.
     * <p>
     * Note that if it's currently in the open status, it is supposed go in the opposite direction, as the closing
     * direction is the opposite of the opening direction. This isn't taken into account by this method.
     *
     * @return The {@link RotateDirection} this {@link IDoor} will open in.
     */
    @NotNull RotateDirection getOpenDir();

    /**
     * Sets the {@link RotateDirection} this {@link IDoor} will open if currently closed.
     * <p>
     * Note that if it's currently in the open status, it is supposed go in the opposite direction, as the closing
     * direction is the opposite of the opening direction.
     *
     * @param rotateDirection The {@link RotateDirection} this {@link IDoor} will open in.
     */
    void setOpenDir(@NotNull RotateDirection rotateDirection);

    /**
     * Gets the position of power block of this door.
     *
     * @return The position of the power block of this door.
     */
    @NotNull Vector3Di getPowerBlock();

    /**
     * Updates the position of the powerblock.
     *
     * @param pos The new position.
     */
    void setPowerBlockPosition(@NotNull Vector3Di pos);

    /**
     * Gets the position of the engine of this door.
     *
     * @return The position of the engine block of this door.
     */
    @NotNull Vector3Di getEngine();

    /**
     * Updates the position of the engine.
     *
     * @param pos The new position.
     */
    void setEngine(@NotNull Vector3Di pos);

    /**
     * Gets the minimum position of this door.
     *
     * @return The minimum coordinates of this door.
     */
    @NotNull Vector3Di getMinimum();

    /**
     * Changes the position of this {@link IDoor}. The min/max order of the positions doesn't matter.
     *
     * @param posA The first new position.
     */
    void setCoordinates(@NotNull Vector3Di posA, @NotNull Vector3Di posB);

    /**
     * Changes the position of this {@link IDoor}. The min/max order of the positions doesn't matter.
     *
     * @param newCuboid The {@link Cuboid} representing the area the door will take up from now on.
     */
    void setCoordinates(@NotNull Cuboid newCuboid);

    /**
     * Gets a copy of the maximum position of this door.
     *
     * @return A copy of the maximum position of this door.
     */
    @NotNull Vector3Di getMaximum();

    /**
     * Retrieve the total number of blocks this {@link IDoor} is made out of. If invalidated or not calculated * yet, it
     * is (re)calculated first.
     * <p>
     * It's calculated once and then stored until invalidated.
     *
     * @return Total number of blocks this {@link IDoor} is made out of.
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
    @NotNull Vector3Di getDimensions();

    /**
     * @return The simple hash of the chunk in which the power block resides.
     */
    long getSimplePowerBlockChunkHash();

    @Override
    boolean equals(Object o);
}
