package nl.pim16aap2.bigdoors.storage;

import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.doors.AbstractDoorBase;
import nl.pim16aap2.bigdoors.util.BitFlag;
import nl.pim16aap2.bigdoors.util.DoorOwner;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

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
    int getPermission(final @NotNull String playerUUID, final long doorUID);

    /**
     * Checks if this storage type only allows single threaded access or not.
     *
     * @return True if only single threaded access is allowed.
     */
    boolean isSingleThreaded();

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
    boolean removeDoors(final @NotNull String playerUUID, final @NotNull String doorName);

    /**
     * Checks whether or not there are any doors in a given world.
     *
     * @param worldUUID The world.
     * @return True if there are more than 0 doors in a given world.
     */
    boolean isBigDoorsWorld(final @NotNull UUID worldUUID);

    /**
     * Gets the total number of doors own by the given player.
     *
     * @param playerUUID The uuid of the player whose doors to count.
     * @return The total number of doors own by the given player.
     */
    int getDoorCountForPlayer(final @NotNull UUID playerUUID);

    /**
     * Gets the number of doors own by the given player with the given name.
     *
     * @param playerUUID The uuid of the player whose doors to count.
     * @param doorName   The name of the door to search for.
     * @return The number of doors own by the given player with the given name.
     */
    int getDoorCountForPlayer(final @NotNull UUID playerUUID, final @NotNull String doorName);

    /**
     * Gets the total number of doors with the given name regardless of who owns them.
     *
     * @param doorName The name of the doors to search for.
     * @return The total number of doors with the given name.
     */
    int getDoorCountByName(final @NotNull String doorName);

    /**
     * Gets the total number of owners of a door.
     *
     * @param doorUID The {@link AbstractDoorBase}.
     * @return The total number of owners of this door.
     */
    int getOwnerCountOfDoor(final long doorUID);

    /**
     * Gets the door with the given UID for the given player at the level of ownership this player has over this door,
     * if any.
     *
     * @param playerUUID The UUID of the player.
     * @param doorUID    The UID of the door to retrieve.
     * @return The door if it exists and if the player is an owner of it.
     */
    @NotNull
    Optional<AbstractDoorBase> getDoor(final @NotNull UUID playerUUID, final long doorUID);

    /**
     * Gets the door with the given doorUID and the original creator as {@link DoorOwner};
     *
     * @param doorUID The UID of the door to retrieve.
     * @return The door with the given doorUID and the original creator.
     */
    @NotNull
    Optional<AbstractDoorBase> getDoor(final long doorUID);

    /**
     * Gets all the doors owned by the the given player with the given name.
     *
     * @param playerUUID The UUID of the player to search for.
     * @param name       The name of the doors to search for.
     * @return All doors owned by the given player with the given name.
     */
    @NotNull
    Optional<List<AbstractDoorBase>> getDoors(final @NotNull UUID playerUUID, final @NotNull String name);

    /**
     * Gets all the doors owned by the the given player.
     *
     * @param playerUUID The UUID of the player to search for.
     * @return All doors owned by the given player.
     */
    @NotNull
    Optional<List<AbstractDoorBase>> getDoors(final @NotNull UUID playerUUID);

    /**
     * Gets all the doors with the given name, regardless of who owns them.
     *
     * @param name The name of the doors to search for.
     * @return All doors with the given name or an empty Optional if none exist.
     */
    @NotNull
    Optional<List<AbstractDoorBase>> getDoors(final @NotNull String name);

    /**
     * Gets all the doors with the given name, owned by the player with at least a certain permission level.
     *
     * @param playerUUID    The name of the player who owns the doors.
     * @param doorName      The name of the doors to search for.
     * @param maxPermission The maximum level of ownership (inclusive) this player has over the doors.
     * @return All the doors with the given name, owned the player with at least a certain permission level.
     */
    @NotNull
    Optional<List<AbstractDoorBase>> getDoors(final @NotNull String playerUUID, final @NotNull String doorName,
                                              final int maxPermission);

    /**
     * Gets all the doors owned by a given player with at least a certain permission level.
     *
     * @param playerUUID    The name of the player who owns the doors.
     * @param maxPermission The maximum level of ownership (inclusive) this player has over the doors.
     * @return All the doors owned by the player with at least a certain permission level.
     */
    @NotNull
    Optional<List<AbstractDoorBase>> getDoors(final @NotNull String playerUUID, final int maxPermission);

    /**
     * Updates the name of the player.
     * <p>
     * PlayerUUIDs cannot change, but player names can, so this is needed to keep the names in sync with the UUIDs.
     *
     * @param playerUUID The UUID of the player.
     * @param playerName The current name of the player.
     * @return True if at least 1 record was modified.
     */
    boolean updatePlayerName(final @NotNull String playerUUID, final @NotNull String playerName);

    /**
     * Gets the UUID of a player with a given name.
     *
     * @param playerName The name of the player to search for.
     * @return The UUID of the player if there is exactly one player with this name.
     */
    @NotNull
    Optional<UUID> getPlayerUUID(final @NotNull String playerName);

    /**
     * Gets the name this player had when they last connected to the server from their UUID.
     *
     * @param playerUUID The UUID of the player to search for.
     * @return The name this player had when they last connected to the server.
     */
    @NotNull
    Optional<String> getPlayerName(final @NotNull String playerUUID);

    /**
     * Gets the original creator of a door.
     *
     * @param doorUID The door whose owner to get.
     * @return The original creator of a door.
     */
    @NotNull
    Optional<DoorOwner> getOwnerOfDoor(final long doorUID);

    /**
     * Gets a map of location hashes and their connected powerblocks for all doors in a chunk.
     * <p>
     * The key is the hashed location in chunk space, the value is the list of UIDs of the doors whose powerblocks
     * occupies that location.
     *
     * @param chunkHash The hash of the chunk the doors are in.
     * @return A map of location hashes and their connected powerblocks for all doors in a chunk.
     */
    @NotNull
    ConcurrentHashMap<Integer, List<Long>> getPowerBlockData(final long chunkHash);

    /**
     * Gets a list of door UIDs that have their engine in a given chunk.
     *
     * @param chunkHash The hash of the chunk the doors are in.
     * @return A list of door UIDs that have their engine in a given chunk.
     */
    @NotNull
    List<Long> getDoorsInChunk(final long chunkHash);

    /**
     * Changes the blocksToMove value of a door.
     *
     * @param doorUID      The door to modify.
     * @param blocksToMove The new number of blocks this door will try to move.
     * @return True if the update was successful.
     */
    boolean updateDoorBlocksToMove(final long doorUID, final int blocksToMove);

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
     * @return True if the update was successful.
     */
    boolean updateDoorCoords(final long doorUID, final boolean isOpen, final int xMin, final int yMin, final int zMin,
                             final int xMax, final int yMax, final int zMax);

    /**
     * Changes the blocksToMove value of a door.
     *
     * @param doorUID   The door to modify.
     * @param autoClose The new auto close timer of this door.
     * @return True if the update was successful.
     */
    boolean updateDoorAutoClose(final long doorUID, final int autoClose);

    /**
     * Changes the blocksToMove value of a door.
     *
     * @param doorUID The door to modify.
     * @param openDir The new rotation direction of this door.
     * @return True if the update was successful.
     */
    boolean updateDoorOpenDirection(final long doorUID, final @NotNull RotateDirection openDir);

    /**
     * Changes the location of the powerblock of a door.
     *
     * @param doorUID The door to modify.
     * @param xPos    The new x coordinate for the powerblock.
     * @param yPos    The new y coordinate for the powerblock.
     * @param zPos    The new z coordinate for the powerblock.
     * @return True if the update was successful.
     */
    boolean updateDoorPowerBlockLoc(final long doorUID, final int xPos, final int yPos, final int zPos);

    /**
     * Changes the lock status of a door.
     *
     * @param doorUID       The UID of the door to modify.
     * @param newLockStatus The new lock status of this door.
     * @return True if the update was successful.
     */
    boolean setLock(final long doorUID, final boolean newLockStatus);

    /**
     * Inserts a new door in the database.
     *
     * @param door The door to insert.
     * @return True if the update was successful.
     */
    boolean insert(final @NotNull AbstractDoorBase door);

    /**
     * Removes an owner of a door. Note that the original creator (= permission level 0) can never be removed.
     *
     * @param doorUID    The UID of the door to modify.
     * @param playerUUID The UUID of the player to remove as owner of the door.
     * @return True if an owner was removed.
     */
    boolean removeOwner(final long doorUID, final @NotNull String playerUUID);

    /**
     * Gets a list of all owners of a door. Contains at least 1 entry (creator).
     *
     * @param doorUID The door to get the owners of.
     * @return A list of all owners of the door.
     */
    @NotNull
    List<DoorOwner> getOwnersOfDoor(final long doorUID);

    /**
     * Adds a player as owner of a door with at a certain permission level to a door.
     * <p>
     * Note that permission level 0 is reserved for the creator, and negative values are not allowed.
     *
     * @param doorUID    The UID of the door to modify.
     * @param player     The player to add as owner.
     * @param permission The level of ownership the player will have over the door.
     * @return True if the update was successful.
     */
    boolean addOwner(final long doorUID, final @NotNull IPPlayer player, final int permission);

    /**
     * Gets the flag value of various boolean properties of a {@link AbstractDoorBase}.
     *
     * @param door The {@link AbstractDoorBase}.
     * @return The flag value of a {@link AbstractDoorBase}.
     */
    default int getFlag(final @NotNull AbstractDoorBase door)
    {
        int flag = 0;
        flag = BitFlag.changeFlag(DoorFlag.getFlagValue(DoorFlag.ISOPEN), door.isOpen(), flag);
        flag = BitFlag.changeFlag(DoorFlag.getFlagValue(DoorFlag.ISLOCKED), door.isLocked(), flag);
        return flag;
    }

    enum DatabaseStatus
    {
        ERROR,
        OUT_OF_DATE,
        TOO_NEW,

        ;
    }

    /**
     * Set of bit flags to represent various properties of doors.
     *
     * @author Pim
     */
    enum DoorFlag implements BitFlag
    {
        /**
         * Consider a door to be opened if this flag is enabled.
         */
        ISOPEN(1),

        /**
         * Consider a door to be locked if this flag is enabled.
         */
        ISLOCKED(2),

        /**
         * Consider a door switched on if this flag is enabled. Used in cases of perpetual movement.
         */
        ISSWITCHEDON(4),

        /**
         * Not used for anything at the moment.
         */
        UNUSED(8),
        ;

        /**
         * The bit value of the flag.
         */
        private final int flagValue;

        DoorFlag(final int flagValue)
        {
            this.flagValue = flagValue;
        }

        /**
         * Gets the flag value of a {@link DoorFlag}.
         *
         * @param flag The {@link DoorFlag}.
         * @return The flag value of a {@link DoorFlag}.
         */
        public static int getFlagValue(final @NotNull DoorFlag flag)
        {
            return flag.flagValue;
        }
    }
}
