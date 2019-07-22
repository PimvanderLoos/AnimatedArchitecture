package nl.pim16aap2.bigdoors.storage;

import nl.pim16aap2.bigdoors.doors.DoorBase;
import nl.pim16aap2.bigdoors.exceptions.TooManyDoorsException;
import nl.pim16aap2.bigdoors.util.DoorOwner;
import nl.pim16aap2.bigdoors.util.PBlockFace;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Represents storage of all door related stuff.
 *
 * @author Pim
 */
public interface IStorage
{
    /**
     * Gets the level of ownership a given player has over a given door.
     *
     * @param playerUUID The player to check.
     * @param doorUID    The door to retrieve this player's level of ownership of.
     * @return The level of ownership this player has over this door or -1 for no ownership at all.
     */
    int getPermission(@NotNull final String playerUUID, final long doorUID);

    /**
     * Delete the door with the given doorUID from the database.
     *
     * @param doorUID The UID of the door to delete.
     * @return True if at least 1 door was successfully removed.
     */
    boolean removeDoor(final long doorUID);

    /**
     * Delete all doors owned by the given player with the given name.
     *
     * @param playerUUID The player whose doors to delete.
     * @param doorName   The name of the doors to delete.
     * @return True if at least 1 door was successfully removed.
     */
    boolean removeDoors(@NotNull final String playerUUID, @NotNull final String doorName);

    /**
     * Gets the total number of doors own by the given player.
     *
     * @param playerUUID The uuid of the player whose doors to count.
     * @return The total number of doors own by the given player.
     */
    int getDoorCountForPlayer(@NotNull UUID playerUUID);

    /**
     * Gets the number of doors own by the given player with the given name.
     *
     * @param playerUUID The uuid of the player whose doors to count.
     * @param doorName   The name of the door to search for.
     * @return The number of doors own by the given player with the given name.
     */
    int getDoorCountForPlayer(@NotNull UUID playerUUID, @NotNull String doorName);

    /**
     * Gets the total number of doors with the given name regardless of who owns them.
     *
     * @param doorName The name of the doors to search for.
     * @return The total number of doors with the given name.
     */
    int getDoorCountByName(@NotNull String doorName);

    /**
     * Gets the door with the given UID for the given player at the level of ownership this player has over this door,
     * if any.
     *
     * @param playerUUID The UUID of the player.
     * @param doorUID    The UID of the door to retrieve.
     * @return The door if it exists and if the player is an owner of it.
     */
    @NotNull Optional<DoorBase> getDoor(@NotNull UUID playerUUID, final long doorUID);


    /**
     * Gets the door with the given doorUID and the original creator as {@link DoorOwner};
     *
     * @param doorUID The UID of the door to retrieve.
     * @return The door with the given doorUID and the original creator.
     */
    @NotNull Optional<DoorBase> getDoor(final long doorUID);

    /**
     * Gets all the doors owned by the the given player with the given name.
     *
     * @param playerUUID The UUID of the player to search for.
     * @param name       The name of the doors to search for.
     * @return All doors owned by the given player with the given name.
     */
    @NotNull Optional<List<DoorBase>> getDoors(@NotNull final UUID playerUUID, @NotNull final String name);

    /**
     * Gets all the doors owned by the the given player.
     *
     * @param playerUUID The UUID of the player to search for.
     * @return All doors owned by the given player.
     */
    @NotNull Optional<List<DoorBase>> getDoors(@NotNull final UUID playerUUID);

    /**
     * Gets all the doors with the given name, regardless of who owns them.
     *
     * @param name The name of the doors to search for.
     * @return All doors with the given name.
     */
    @NotNull Optional<List<DoorBase>> getDoors(@NotNull final String name);

    /**
     * Gets all the doors with the given name, owned by the player with at least a certain permission level.
     *
     * @param playerUUID    The name of the player who owns the doors.
     * @param doorName      The name of the doors to search for.
     * @param maxPermission The maximum level of ownership (inclusive) this player has over the doors.
     * @return All the doors with the given name, owned the player with at least a certain permission level.
     */
    @NotNull Optional<List<DoorBase>> getDoors(@NotNull final String playerUUID, @NotNull final String doorName,
                                               int maxPermission);

    /**
     * Gets all the doors owned by a given player with at least a certain permission level.
     *
     * @param playerUUID    The name of the player who owns the doors.
     * @param maxPermission The maximum level of ownership (inclusive) this player has over the doors.
     * @return All the doors owned by the player with at least a certain permission level.
     */
    @NotNull Optional<List<DoorBase>> getDoors(@NotNull final String playerUUID, int maxPermission);

    /**
     * Gets the {@link DoorBase} with the given name owned by the given player. If the player owns 0 or more than 1
     * doors with the given name, no doors are returned at all!
     *
     * @param playerUUID The UUID of the player whose {@link DoorBase} will be obtained.
     * @param doorName   The name of the {@link DoorBase} to look for.
     * @return The {@link DoorBase} with the given name owned by this player if exactly 1 such DoorBase exists.
     */
    @NotNull Optional<DoorBase> getDoor(@NotNull final String playerUUID, @NotNull final String doorName)
            throws TooManyDoorsException;

    /**
     * Gets the {@link DoorBase} with the given name owned by the given player. If the player owns 0 or more than 1
     * doors with the given name, no doors are returned at all!
     *
     * @param playerUUID    The UUID of the player whose {@link DoorBase} will be obtained.
     * @param doorName      The name of the {@link DoorBase} to look for.
     * @param maxPermission The highest level of ownership this player can have over the door being looked for. If set
     *                      to 0, for example, only door doors where the given player has ownership level 0 (= creator)
     *                      are included in the search.
     * @return The {@link DoorBase} with the given name owned by this player if exactly 1 such DoorBase exists.
     */
    @NotNull Optional<DoorBase> getDoor(@NotNull final String playerUUID, @NotNull final String doorName,
                                        int maxPermission)
            throws TooManyDoorsException;

    /**
     * Updates the name of the player.
     * <p>
     * PlayerUUIDs cannot change, but player names can, so this is needed to keep the names in sync with the UUIDs.
     *
     * @param playerUUID The UUID of the player.
     * @param playerName The current name of the player.
     * @return True if at least 1 record was modified.
     */
    boolean updatePlayerName(@NotNull final String playerUUID, @NotNull final String playerName);

    /**
     * Gets the UUID of a player with a given name.
     *
     * @param playerName The name of the player to search for.
     * @return The UUID of the player if there is exactly one player with this name.
     */
    @NotNull Optional<UUID> getPlayerUUID(@NotNull final String playerName);

    /**
     * Gets the name this player had when they last connected to the server from their UUID.
     *
     * @param playerUUID The UUID of the player to search for.
     * @return The name this player had when they last connected to the server.
     */
    @NotNull Optional<String> getPlayerName(@NotNull final String playerUUID);

    /**
     * Gets the original creator of a door.
     *
     * @param doorUID The door whose owner to get.
     * @return The original creator of a door.
     */
    @NotNull Optional<DoorOwner> getOwnerOfDoor(final long doorUID);

    /**
     * Gets a map of location hashes and their connected powerblocks for all doors in a chunk.
     * <p>
     * The key is the hashed location in world space, the value is the UID of the door whose powerblock occupies that
     * location.
     *
     * @param chunkHash The hash of the chunk the doors are in.
     * @return A map of location hashes and their connected powerblocks for all doors in a chunk.
     */
    @NotNull Map<Long, List<Long>> getPowerBlockData(final long chunkHash);

    /**
     * Changes the blocksToMove value of a door.
     *
     * @param doorUID      The door to modify.
     * @param blocksToMove The new number of blocks this door will try to move.
     */
    void updateDoorBlocksToMove(final long doorUID, final int blocksToMove);

    /**
     * Updates the coordinates of a door.
     *
     * @param doorUID The UID of the door to update.
     * @param isOpen  The new open-status of the door.
     * @param xMin    The new minimum x coordinate.
     * @param yMin    The new minimum y coordinate.
     * @param zMin    The new minimum z coordinate.
     * @param xMax    The new maximum x coordinate.
     * @param yMax    The new maximum y coordinate.
     * @param zMax    The new maximum z coordinate.
     * @param engSide The new engine side of the door, if applicable.
     */
    void updateDoorCoords(final long doorUID, final boolean isOpen, final int xMin, final int yMin,
                          final int zMin, final int xMax, final int yMax, final int zMax,
                          @NotNull final PBlockFace engSide);

    /**
     * Updates the coordinates of a door.
     *
     * @param doorUID The UID of the door to update.
     * @param isOpen  The new open-status of the door.
     * @param xMin    The new minimum x coordinate.
     * @param yMin    The new minimum y coordinate.
     * @param zMin    The new minimum z coordinate.
     * @param xMax    The new maximum x coordinate.
     * @param yMax    The new maximum y coordinate.
     * @param zMax    The new maximum z coordinate.
     */
    void updateDoorCoords(final long doorUID, final boolean isOpen, final int xMin, final int yMin,
                          final int zMin, final int xMax, final int yMax, final int zMax);

    /**
     * Changes the blocksToMove value of a door.
     *
     * @param doorUID   The door to modify.
     * @param autoClose The new auto close timer of this door.
     */
    void updateDoorAutoClose(final long doorUID, final int autoClose);

    /**
     * Changes the blocksToMove value of a door.
     *
     * @param doorUID The door to modify.
     * @param openDir The new rotation direction of this door.
     */
    void updateDoorOpenDirection(final long doorUID, @NotNull final RotateDirection openDir);

    /**
     * Changes the location of the powerblock of a door.
     *
     * @param doorUID   The door to modify.
     * @param xPos      The new x coordinate for the powerblock.
     * @param yPos      The new y coordinate for the powerblock.
     * @param zPos      The new z coordinate for the powerblock.
     * @param worldUUID The UUID of the world the power block is now in.
     */
    void updateDoorPowerBlockLoc(final long doorUID, final int xPos, final int yPos, final int zPos,
                                 @NotNull final UUID worldUUID);

    /**
     * Changes the lock status of a door.
     *
     * @param doorUID       The UID of the door to modify.
     * @param newLockStatus The new lock status of this door.
     */
    void setLock(final long doorUID, final boolean newLockStatus);

    /**
     * Inserts a new door in the database.
     *
     * @param door The door to insert.
     */
    void insert(@NotNull final DoorBase door);

    /**
     * Removes an owner of a door. Note that the original creator (= permission level 0) can never be removed.
     *
     * @param doorUID    The UID of the door to modify.
     * @param playerUUID The UUID of the player to remove as owner of the door.
     * @return True if an owner was removed.
     */
    boolean removeOwner(final long doorUID, @NotNull final String playerUUID);

    /**
     * Gets a list of all owners of a door. Contains at least 1 entry (creator).
     *
     * @param doorUID The door to get the owners of.
     * @return A list of all owners of the door.
     */
    @NotNull List<DoorOwner> getOwnersOfDoor(final long doorUID);

    /**
     * Adds a player as owner of a door with at a certain permission level to a door.
     * <p>
     * Note that permission level 0 is reserved for the creator, and negative values are not allowed.
     *
     * @param doorUID    The UID of the door to modify.
     * @param playerUUID The UUID of the player to add as owner.
     * @param permission The level of ownership the player will have over the door.
     */
    void addOwner(final long doorUID, @NotNull final UUID playerUUID, final int permission);
}
