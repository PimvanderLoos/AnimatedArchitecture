package nl.pim16aap2.bigdoors.doors;

import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.api.IPWorld;
import nl.pim16aap2.bigdoors.util.Cuboid;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import nl.pim16aap2.bigdoors.util.Util;
import nl.pim16aap2.bigdoors.util.vector.Vector3Di;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

/**
 * Represents a read-only door.
 *
 * @author Pim
 */
@SuppressWarnings("unused")
public interface IDoorConst
{
    /**
     * Checks if this door can be opened right now.
     *
     * @return True if this door can be opened right now.
     */
    default boolean isOpenable()
    {
        return !isOpen();
    }

    /**
     * Checks if this door can be closed right now.
     *
     * @return True if this door can be closed right now.
     */
    default boolean isCloseable()
    {
        return isOpen();
    }

    /**
     * Gets the {@link Cuboid} representing the area taken up by this door.
     *
     * @return The {@link Cuboid} representing the area taken up by this door.
     */
    Cuboid getCuboid();

    /**
     * Gets the name of this door.
     *
     * @return The name of this door.
     */
    String getName();

    /**
     * Gets the IPWorld this {@link IDoor} exists in.
     *
     * @return The IPWorld this {@link IDoor} exists in
     */
    IPWorld getWorld();

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
    DoorOwner getPrimeOwner();

    /**
     * Gets all {@link DoorOwner}s of this door, including the original creator.
     *
     * @return All {@link DoorOwner}s of this door, including the original creator.
     */
    Collection<DoorOwner> getDoorOwners();

    /**
     * Attempts to get the {@link DoorOwner} of this door represented by the UUID of a player.
     *
     * @param player
     *     The UUID of the player that may or may not be an owner of this door.
     * @return The {@link DoorOwner} of this door for the given player, if this player is a {@link DoorOwner} of this
     * door.
     */
    Optional<DoorOwner> getDoorOwner(UUID player);

    /**
     * Attempts to get the {@link DoorOwner} of this door represented by an {@link IPPlayer}.
     *
     * @param player
     *     The player that may or may not be an owner of this door.
     * @return The {@link DoorOwner} of this door for the given player, if this player is a {@link DoorOwner} of this
     * door.
     */
    default Optional<DoorOwner> getDoorOwner(IPPlayer player)
    {
        return getDoorOwner(player.getUUID());
    }

    /**
     * Checks if a player with a given UUID is a (co-)owner of this door with any level of ownership.
     * <p>
     * If the level of ownership matters, use {@link #getDoorOwner(UUID)} instead.
     *
     * @param player
     *     The UUID of a player.
     * @return True if the player with the given UUID is an owner of this door with any level of ownership.
     */
    boolean isDoorOwner(UUID player);

    /**
     * See {@link #isDoorOwner(UUID)}.
     */
    default boolean isDoorOwner(IPPlayer player)
    {
        return isDoorOwner(player.getUUID());
    }

    /**
     * Gets the {@link RotateDirection} this {@link IDoor} will open if currently closed.
     * <p>
     * Note that if it's currently in the open status, it is supposed go in the opposite direction, as the closing
     * direction is the opposite of the opening direction. This isn't taken into account by this method.
     *
     * @return The {@link RotateDirection} this {@link IDoor} will open in.
     */
    RotateDirection getOpenDir();

    /**
     * Gets the position of power block of this door.
     *
     * @return The position of the power block of this door.
     */
    Vector3Di getPowerBlock();

    /**
     * Gets the position of the rotation point of this door.
     *
     * @return The position of the rotation point block of this door.
     */
    Vector3Di getRotationPoint();

    /**
     * Gets the minimum position of this door.
     *
     * @return The minimum coordinates of this door.
     */
    default Vector3Di getMinimum()
    {
        return getCuboid().getMin();
    }

    /**
     * Gets a copy of the maximum position of this door.
     *
     * @return A copy of the maximum position of this door.
     */
    default Vector3Di getMaximum()
    {
        return getCuboid().getMax();
    }

    /**
     * Retrieve the total number of blocks this {@link IDoor} is made out of. If invalidated or not calculated * yet, it
     * is (re)calculated first.
     * <p>
     * It's calculated once and then stored until invalidated.
     *
     * @return Total number of blocks this {@link IDoor} is made out of.
     */
    default int getBlockCount()
    {
        return getCuboid().getVolume();
    }

    /**
     * Gets the dimensions of this door.
     * <p>
     * If a door has a min and max X value of 120, for example, it would have an X-dimension of 0. If the min X value is
     * 119 instead, it would have an X-dimension of 1.
     *
     * @return The dimensions of this door.
     */
    default Vector3Di getDimensions()
    {
        return getCuboid().getDimensions();
    }

    /**
     * @return The simple hash of the chunk in which the power block resides.
     */
    default long getChunkId()
    {
        return Util.getChunkId(getPowerBlock());
    }

    @Override
    boolean equals(Object o);
}
