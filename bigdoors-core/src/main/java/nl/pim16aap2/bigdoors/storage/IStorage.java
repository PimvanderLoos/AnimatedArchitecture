package nl.pim16aap2.bigdoors.storage;

import lombok.NonNull;
import nl.pim16aap2.bigdoors.api.PPlayerData;
import nl.pim16aap2.bigdoors.doors.AbstractDoorBase;
import nl.pim16aap2.bigdoors.doortypes.DoorType;
import nl.pim16aap2.bigdoors.util.DoorOwner;
import nl.pim16aap2.bigdoors.util.IBitFlag;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * Represents storage of all door related stuff.
 *
 * @author Pim
 */
public interface IStorage
{
    @NotNull
    Pattern VALID_TABLE_NAME = Pattern.compile("^[a-zA-Z0-9_]*$");

    /**
     * Checks if a specific String would make for a valid name for a table.
     *
     * @param str The String to check.
     * @return True if this String is valid as a table name.
     */
    static boolean isValidTableName(final @NotNull String str)
    {
        return VALID_TABLE_NAME.matcher(str).find();
    }

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
     * @param worldName The name of the world.
     * @return True if there are more than 0 doors in the given world.
     */
    boolean isBigDoorsWorld(final @NotNull String worldName);

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
     * Updates the {@link PPlayerData} for a given player.
     *
     * @param playerData The {@link PPlayerData} the represents a player.
     * @return True if at least 1 record was modified.
     */
    boolean updatePlayerData(final @NonNull PPlayerData playerData);

    /**
     * Tries to find the {@link PPlayerData} for a player with the given {@link UUID}.
     *
     * @param uuid The {@link UUID} of a player.
     * @return The {@link PPlayerData} that represents the player.
     */
    @NonNull Optional<PPlayerData> getPlayerData(final @NonNull UUID uuid);

    /**
     * Tries to get all the players with a given name. Because names are not unique, this may result in any number of
     * matches.
     * <p>
     * If you know the player's UUID, it is recommended to use {@link #getPlayerData(UUID)} instead.
     *
     * @param playerName The name of the player(s).
     * @return All the players with the given name.
     */
    @NonNull List<PPlayerData> getPlayerData(final @NonNull String playerName);

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
    @NotNull Optional<AbstractDoorBase> getDoor(final @NotNull UUID playerUUID, final long doorUID);

    /**
     * Gets the door with the given doorUID and the original creator as {@link DoorOwner};
     *
     * @param doorUID The UID of the door to retrieve.
     * @return The door with the given doorUID and the original creator.
     */
    @NotNull Optional<AbstractDoorBase> getDoor(final long doorUID);

    /**
     * Gets all the doors owned by the the given player with the given name.
     *
     * @param playerUUID The UUID of the player to search for.
     * @param name       The name of the doors to search for.
     * @return All doors owned by the given player with the given name.
     */
    @NotNull List<AbstractDoorBase> getDoors(final @NotNull UUID playerUUID, final @NotNull String name);

    /**
     * Gets all the doors owned by the the given player.
     *
     * @param playerUUID The UUID of the player to search for.
     * @return All doors owned by the given player.
     */
    @NotNull List<AbstractDoorBase> getDoors(final @NotNull UUID playerUUID);

    /**
     * Gets all the doors with the given name, regardless of who owns them.
     *
     * @param name The name of the doors to search for.
     * @return All doors with the given name or an empty Optional if none exist.
     */
    @NotNull List<AbstractDoorBase> getDoors(final @NotNull String name);

    /**
     * Gets all the doors with the given name, owned by the player with at least a certain permission level.
     *
     * @param playerUUID    The name of the player who owns the doors.
     * @param doorName      The name of the doors to search for.
     * @param maxPermission The maximum level of ownership (inclusive) this player has over the doors.
     * @return All the doors with the given name, owned the player with at least a certain permission level.
     */
    @NotNull List<AbstractDoorBase> getDoors(final @NotNull String playerUUID, final @NotNull String doorName,
                                             final int maxPermission);

    /**
     * Gets all the doors owned by a given player with at least a certain permission level.
     *
     * @param playerUUID    The name of the player who owns the doors.
     * @param maxPermission The maximum level of ownership (inclusive) this player has over the doors.
     * @return All the doors owned by the player with at least a certain permission level.
     */
    @NotNull List<AbstractDoorBase> getDoors(final @NotNull String playerUUID, final int maxPermission);

    /**
     * Gets a map of location hashes and their connected powerblocks for all doors in a chunk.
     * <p>
     * The key is the hashed location in chunk space, the value is the list of UIDs of the doors whose powerblocks
     * occupies that location.
     *
     * @param chunkHash The hash of the chunk the doors are in.
     * @return A map of location hashes and their connected powerblocks for all doors in a chunk.
     */
    @NotNull ConcurrentHashMap<Integer, List<Long>> getPowerBlockData(final long chunkHash);

    /**
     * Gets a list of door UIDs that have their engine in a given chunk.
     *
     * @param chunkHash The hash of the chunk the doors are in.
     * @return A list of door UIDs that have their engine in a given chunk.
     */
    @NotNull List<Long> getDoorsInChunk(final long chunkHash);

    /**
     * Inserts a new door in the database. If the insertion was successful, a new {@link AbstractDoorBase} will be
     * created with the correct doorUID.
     *
     * @param door The door to insert.
     * @return The {@link AbstractDoorBase} that was just inserted if insertion was successful. This is
     * <u><b>NOT!!</b></u> the same object as the one passed to this method.
     */
    @NotNull Optional<AbstractDoorBase> insert(final @NotNull AbstractDoorBase door);

    /**
     * Synchronizes an {@link AbstractDoorBase} door with the database. This will synchronize both the base and the
     * type-specific data of the {@link AbstractDoorBase}.
     *
     * @param simpleDoorData The {@link AbstractDoorBase.SimpleDoorData} that describes the base data of door.
     * @param typeData       The type-specific data of this door.
     * @return True if the update was successful.
     */
    boolean syncDoorData(final @NotNull AbstractDoorBase.SimpleDoorData simpleDoorData, final byte[] typeData);

    /**
     * Deletes a {@link DoorType} and all {@link AbstractDoorBase}s of this type from the database.
     * <p>
     * Note that the {@link DoorType} has to be registered before it can be deleted! It doesn't need to be enabled,
     * though.
     *
     * @param doorType The {@link DoorType} to delete.
     * @return True if deletion was successful.
     */
    boolean deleteDoorType(final @NotNull DoorType doorType);

    /**
     * Removes an owner of a door. Note that the original creator (= permission level 0) can never be removed.
     *
     * @param doorUID    The UID of the door to modify.
     * @param playerUUID The UUID of the player to remove as owner of the door.
     * @return True if an owner was removed.
     */
    boolean removeOwner(final long doorUID, final @NotNull String playerUUID);

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
    boolean addOwner(final long doorUID, final @NotNull PPlayerData player, final int permission);

    /**
     * Gets the flag value of various boolean properties of a {@link AbstractDoorBase}.
     *
     * @param door The {@link AbstractDoorBase}.
     * @return The flag value of a {@link AbstractDoorBase}.
     */
    default long getFlag(final @NotNull AbstractDoorBase door)
    {
        long flag = 0;
        flag = IBitFlag.changeFlag(DoorFlag.getFlagValue(DoorFlag.IS_OPEN), door.isOpen(), flag);
        flag = IBitFlag.changeFlag(DoorFlag.getFlagValue(DoorFlag.IS_LOCKED), door.isLocked(), flag);
        return flag;
    }

    /**
     * Gets the flag value of various boolean properties of a {@link AbstractDoorBase}.
     *
     * @param isOpen   Whether the door is currently open.
     * @param isLocked Whether the door is currently locked.
     * @return The flag value of a {@link AbstractDoorBase}.
     */
    default long getFlag(final boolean isOpen, final boolean isLocked)
    {
        long flag = 0;
        flag = IBitFlag.changeFlag(DoorFlag.getFlagValue(DoorFlag.IS_OPEN), isOpen, flag);
        flag = IBitFlag.changeFlag(DoorFlag.getFlagValue(DoorFlag.IS_LOCKED), isLocked, flag);
        return flag;
    }

    /**
     * Obtains {@link DatabaseState} the database is in.
     *
     * @return The {@link DatabaseState} the database is in.
     */
    @NotNull DatabaseState getDatabaseState();

    /**
     * Represents the status of the database.
     *
     * @author Pim
     */
    enum DatabaseState
    {
        /**
         * Everything is in order.
         */
        OK,

        /**
         * An error occurred somewhere along the way.
         */
        ERROR,

        /**
         * The database is out of date and needs to be upgraded before it can work.
         */
        OUT_OF_DATE,

        /**
         * The database version is newer than the maximum allowed version.
         */
        TOO_NEW,

        /**
         * The database version is older than the minimum allowed version and can therefore not be upgraded.
         */
        TOO_OLD,

        /**
         * The database has not been initialized yet.
         */
        UNINITIALIZED,

        /**
         * No driver could be found.
         */
        NO_DRIVER,
        ;
    }

    /**
     * Set of bit flags to represent various properties of doors.
     *
     * @author Pim
     */
    enum DoorFlag implements IBitFlag
    {
        /**
         * Consider a door to be opened if this flag is enabled.
         */
        IS_OPEN(0b00000001),

        /**
         * Consider a door to be locked if this flag is enabled.
         */
        IS_LOCKED(0b00000010),

        /**
         * Consider a door switched on if this flag is enabled. Used in cases of perpetual movement.
         */
        IS_SWITCHED_ON(0b00000100),
        ;

        /**
         * The bit value of the flag.
         */
        private final long flagValue;

        DoorFlag(final long flagValue)
        {
            this.flagValue = flagValue;
        }

        /**
         * Gets the flag value of a {@link DoorFlag}.
         *
         * @param flag The {@link DoorFlag}.
         * @return The flag value of a {@link DoorFlag}.
         */
        public static long getFlagValue(final @NotNull DoorFlag flag)
        {
            return flag.flagValue;
        }
    }
}
