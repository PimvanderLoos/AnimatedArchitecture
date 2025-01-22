package nl.pim16aap2.animatedarchitecture.core.structures;

import nl.pim16aap2.animatedarchitecture.core.api.IConfig;
import nl.pim16aap2.animatedarchitecture.core.api.IPlayer;
import nl.pim16aap2.animatedarchitecture.core.api.IWorld;
import nl.pim16aap2.animatedarchitecture.core.structures.properties.IPropertyContainerConst;
import nl.pim16aap2.animatedarchitecture.core.structures.properties.IPropertyHolderConst;
import nl.pim16aap2.animatedarchitecture.core.structures.properties.PropertyContainerSnapshot;
import nl.pim16aap2.animatedarchitecture.core.util.Cuboid;
import nl.pim16aap2.animatedarchitecture.core.util.LocationUtil;
import nl.pim16aap2.animatedarchitecture.core.util.MovementDirection;
import nl.pim16aap2.animatedarchitecture.core.util.Rectangle;
import nl.pim16aap2.animatedarchitecture.core.util.vector.Vector3Di;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

/**
 * Represents a read-only structure.
 * <p>
 * Please read the documentation of {@link nl.pim16aap2.animatedarchitecture.core.structures} for more information about
 * the structure system.
 */
@SuppressWarnings("unused")
public interface IStructureConst extends IPropertyHolderConst
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
     * Formats a name and UID into a string.
     *
     * @param name
     *     The name of the structure.
     * @param uid
     *     The UID of the structure.
     * @return The name and UID formatted as "name (uid)".
     */
    static String formatNameAndUid(String name, long uid)
    {
        return String.format("%s (%d)", name, uid);
    }

    /**
     * @return The name and UID of this structure formatted as "name (uid)".
     */
    default String getNameAndUid()
    {
        return formatNameAndUid(getName(), getUid());
    }

    /**
     * Gets the IWorld this {@link Structure} exists in.
     *
     * @return The IWorld this {@link Structure} exists in
     */
    IWorld getWorld();

    /**
     * Gets the UID of the {@link Structure} as used in the database. Guaranteed to be unique and available.
     *
     * @return The UID of the {@link Structure} as used in the database.
     */
    long getUid();

    /**
     * Gets the basic information of this structure.
     * <p>
     * This includes the name, UID, type, and prime owner of this structure.
     *
     * @return The basic information of this structure.
     */
    default String getBasicInfo()
    {
        return String.format(
            "%d (%s) - %s: %s",
            getUid(),
            getPrimeOwner(),
            getType().getFullKey(),
            getName()
        );
    }

    /**
     * Gets the lower time limit for an animation.
     * <p>
     * Because animated blocks have a speed limit, as determined by {@link IConfig#maxBlockSpeed()}, there is also a
     * minimum amount of time for its animation.
     * <p>
     * The exact time limit depends on the shape, size, and type of structure.
     *
     * @return The lower animation time limit for this structure in seconds.
     */
    double getMinimumAnimationTime();

    /**
     * Check if the {@link Structure} is currently locked. When locked, structures cannot be opened.
     *
     * @return True if the {@link Structure} is locked
     */
    boolean isLocked();

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
     * Attempts to get the {@link StructureOwner} of this structure represented by an {@link IPlayer}.
     *
     * @param player
     *     The player that may or may not be an owner of this structure.
     * @return The {@link StructureOwner} of this structure for the given player, if this player is a
     * {@link StructureOwner} of this structure.
     */
    default Optional<StructureOwner> getOwner(IPlayer player)
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
     * Checks if a player with a given UUID is a (co-)owner of this structure with any level of ownership.
     * <p>
     * If the level of ownership matters, use {@link #getOwner(UUID)} instead.
     *
     * @param player
     *     The player.
     * @return True if the player with the given UUID is an owner of this structure with any level of ownership.
     */
    default boolean isOwner(IPlayer player)
    {
        return isOwner(player.getUUID());
    }

    /**
     * Checks if a player with a given UUID is a (co-)owner of this structure with a given level of ownership.
     *
     * @param player
     *     The UUID of a player.
     * @param permissionLevel
     *     The maximum level of ownership the player may have.
     *     <p>
     *     E.g. when permissionLevel is {@link PermissionLevel#ADMIN}, this method will return true if the player has
     *     admin access or lower ({@link PermissionLevel#ADMIN} or {@link PermissionLevel#CREATOR}).
     * @return True if the player with the given UUID is an owner of this structure with the given level of ownership
     * (or lower).
     */
    boolean isOwner(UUID player, PermissionLevel permissionLevel);

    /**
     * Checks if a player with a given UUID is a (co-)owner of this structure with a given level of ownership.
     *
     * @param player
     *     The player.
     * @param permissionLevel
     *     The maximum level of ownership the player may have.
     *     <p>
     *     E.g. when permissionLevel is {@link PermissionLevel#ADMIN}, this method will return true if the player has
     *     admin access or lower ({@link PermissionLevel#ADMIN} or {@link PermissionLevel#CREATOR}).
     * @return True if the player with the given UUID is an owner of this structure with the given level of ownership
     * (or lower).
     */
    default boolean isOwner(IPlayer player, PermissionLevel permissionLevel)
    {
        return isOwner(player.getUUID(), permissionLevel);
    }

    /**
     * Gets the {@link MovementDirection} this {@link Structure} will open if currently closed.
     * <p>
     * Note that if it's currently in the open status, it is supposed go in the opposite direction, as the closing
     * direction is the opposite of the opening direction. This isn't taken into account by this method.
     *
     * @return The {@link MovementDirection} this {@link Structure} will open in.
     */
    MovementDirection getOpenDirection();

    /**
     * Gets the position of power block of this structure.
     *
     * @return The position of the power block of this structure.
     */
    Vector3Di getPowerBlock();

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
     * Retrieve the total number of blocks this {@link Structure} is made out of. If invalidated or not calculated *
     * yet, it is (re)calculated first.
     * <p>
     * It's calculated once and then stored until invalidated.
     *
     * @return Total number of blocks this {@link Structure} is made out of.
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
        return LocationUtil.getChunkId(getPowerBlock());
    }

    /**
     * Cycle the {@link MovementDirection} direction this {@link Structure} will open in. By default, it will loop over
     * all valid directions. See {@link StructureType#getValidOpenDirectionsList()}. However, subclasses may override
     * this behavior.
     * <p>
     * Note that this does not actually change the open direction; it merely tells you which direction comes next!
     *
     * @return The new {@link MovementDirection} direction this {@link Structure} will open in.
     */
    MovementDirection getCycledOpenDirection();

    @Override
    boolean equals(Object o);

    /**
     * Gets the {@link PropertyContainerSnapshot} of this structure.
     *
     * @return The {@link PropertyContainerSnapshot} of this structure.
     */
    IPropertyContainerConst getPropertyContainerSnapshot();
}
