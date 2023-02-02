package nl.pim16aap2.bigdoors.structures;

import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.api.IPWorld;
import nl.pim16aap2.bigdoors.structuretypes.StructureType;
import nl.pim16aap2.bigdoors.util.Cuboid;
import nl.pim16aap2.bigdoors.util.MovementDirection;
import nl.pim16aap2.bigdoors.util.Rectangle;
import nl.pim16aap2.bigdoors.util.Util;
import nl.pim16aap2.bigdoors.util.vector.Vector3Di;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

/**
 * Represents a read-only structure.
 *
 * @author Pim
 */
@SuppressWarnings("unused")
public interface IStructureConst
{
    /**
     * @return A {@link StructureSnapshot} of this {@link StructureBase}.
     */
    StructureSnapshot getSnapshot();

    /**
     * @return The {@link StructureType} of this structure.
     */
    StructureType getType();

    /**
     * Checks if this structure can be opened right now.
     *
     * @return True if this structure can be opened right now.
     */
    default boolean isOpenable()
    {
        return !isOpen();
    }

    /**
     * Checks if this structure can be closed right now.
     *
     * @return True if this structure can be closed right now.
     */
    default boolean isCloseable()
    {
        return isOpen();
    }

    /**
     * Gets the {@link Cuboid} representing the area taken up by this structure.
     *
     * @return The {@link Cuboid} representing the area taken up by this structure.
     */
    Cuboid getCuboid();

    /**
     * Gets the rectangle describing the limits within an animation of this door takes place.
     * <p>
     * At no point during an animation will any animated block leave this cuboid, though not guarantees are given
     * regarding how tight the cuboid fits around the animated blocks.
     *
     * @return The animation range.
     */
    Rectangle getAnimationRange();

    /**
     * Gets the name of this structure.
     *
     * @return The name of this structure.
     */
    String getName();

    /**
     * @return The name and UID of this structure formatted as "name (uid)".
     */
    default String getNameAndUid()
    {
        return String.format("%s (%d)", getName(), getUid());
    }

    /**
     * Gets the IPWorld this {@link IStructure} exists in.
     *
     * @return The IPWorld this {@link IStructure} exists in
     */
    IPWorld getWorld();

    /**
     * Gets the UID of the {@link IStructure} as used in the database. Guaranteed to be unique and available.
     *
     * @return The UID of the {@link IStructure} as used in the database.
     */
    long getUid();

    /**
     * Check if the {@link IStructure} is currently locked. When locked, structures cannot be opened.
     *
     * @return True if the {@link IStructure} is locked
     */
    boolean isLocked();

    /**
     * Check if the {@link IStructure} is currently open.
     *
     * @return True if the {@link IStructure} is open
     */
    boolean isOpen();

    /**
     * Gets the prime owner (permission = 0) of this structure. In most cases, this will be the original creator of the
     * structure.
     *
     * @return The prime owner of this structure.
     */
    StructureOwner getPrimeOwner();

    /**
     * Gets all {@link StructureOwner}s of this structure, including the original creator.
     *
     * @return All {@link StructureOwner}s of this structure, including the original creator.
     */
    Collection<StructureOwner> getOwners();

    /**
     * Attempts to get the {@link StructureOwner} of this structure represented by the UUID of a player.
     *
     * @param player
     *     The UUID of the player that may or may not be an owner of this structure.
     * @return The {@link StructureOwner} of this structure for the given player, if this player is a
     * {@link StructureOwner} of this structure.
     */
    Optional<StructureOwner> getOwner(UUID player);

    /**
     * Attempts to get the {@link StructureOwner} of this structure represented by an {@link IPPlayer}.
     *
     * @param player
     *     The player that may or may not be an owner of this structure.
     * @return The {@link StructureOwner} of this structure for the given player, if this player is a
     * {@link StructureOwner} of this structure.
     */
    default Optional<StructureOwner> getOwner(IPPlayer player)
    {
        return getOwner(player.getUUID());
    }

    /**
     * Checks if a player with a given UUID is a (co-)owner of this structure with any level of ownership.
     * <p>
     * If the level of ownership matters, use {@link #getOwner(UUID)} instead.
     *
     * @param player
     *     The UUID of a player.
     * @return True if the player with the given UUID is an owner of this structure with any level of ownership.
     */
    boolean isOwner(UUID player);

    /**
     * See {@link #isOwner(UUID)}.
     */
    default boolean isOwner(IPPlayer player)
    {
        return isOwner(player.getUUID());
    }

    /**
     * Gets the {@link MovementDirection} this {@link IStructure} will open if currently closed.
     * <p>
     * Note that if it's currently in the open status, it is supposed go in the opposite direction, as the closing
     * direction is the opposite of the opening direction. This isn't taken into account by this method.
     *
     * @return The {@link MovementDirection} this {@link IStructure} will open in.
     */
    MovementDirection getOpenDir();

    /**
     * Gets the position of power block of this structure.
     *
     * @return The position of the power block of this structure.
     */
    Vector3Di getPowerBlock();

    /**
     * Gets the position of the rotation point of this structure.
     *
     * @return The position of the rotation point block of this structure.
     */
    Vector3Di getRotationPoint();

    /**
     * Gets the minimum position of this structure.
     *
     * @return The minimum coordinates of this structure.
     */
    default Vector3Di getMinimum()
    {
        return getCuboid().getMin();
    }

    /**
     * Gets a copy of the maximum position of this structure.
     *
     * @return A copy of the maximum position of this structure.
     */
    default Vector3Di getMaximum()
    {
        return getCuboid().getMax();
    }

    /**
     * Retrieve the total number of blocks this {@link IStructure} is made out of. If invalidated or not calculated *
     * yet, it is (re)calculated first.
     * <p>
     * It's calculated once and then stored until invalidated.
     *
     * @return Total number of blocks this {@link IStructure} is made out of.
     */
    default int getBlockCount()
    {
        return getCuboid().getVolume();
    }

    /**
     * Gets the dimensions of this structure.
     * <p>
     * If a structure has a min and max X value of 120, for example, it would have an X-dimension of 0. If the min X
     * value is 119 instead, it would have an X-dimension of 1.
     *
     * @return The dimensions of this structure.
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
