package nl.pim16aap2.bigdoors.storage;

import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.api.PPlayerData;
import nl.pim16aap2.bigdoors.managers.DatabaseManager;
import nl.pim16aap2.bigdoors.movable.AbstractMovable;
import nl.pim16aap2.bigdoors.movable.IMovableConst;
import nl.pim16aap2.bigdoors.movable.MovableOwner;
import nl.pim16aap2.bigdoors.movable.PermissionLevel;
import nl.pim16aap2.bigdoors.movabletypes.MovableType;
import nl.pim16aap2.bigdoors.util.IBitFlag;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * Represents storage of all movable related stuff.
 *
 * @author Pim
 */
public interface IStorage
{
    Pattern VALID_TABLE_NAME = Pattern.compile("^[a-zA-Z0-9_]*$");

    /**
     * Checks if a specific String would make for a valid name for a table.
     *
     * @param str
     *     The String to check.
     * @return True if this String is valid as a table name.
     */
    static boolean isValidTableName(String str)
    {
        return VALID_TABLE_NAME.matcher(str).find();
    }

    /**
     * Delete the movable with the given movableUID from the database.
     *
     * @param movableUID
     *     The UID of the movable to delete.
     * @return True if at least 1 movable was successfully removed.
     */
    boolean removeMovable(long movableUID);

    /**
     * Delete all movables owned by the given player with the given name.
     *
     * @param playerUUID
     *     The player whose movables to delete.
     * @param movableName
     *     The name of the movables to delete.
     * @return True if at least 1 movable was successfully removed.
     */
    boolean removeMovables(UUID playerUUID, String movableName);

    /**
     * Checks whether there are any movables in a given world.
     *
     * @param worldName
     *     The name of the world.
     * @return True if there are more than 0 movables in the given world.
     */
    boolean isBigDoorsWorld(String worldName);

    /**
     * Gets the total number of movables own by the given player.
     *
     * @param playerUUID
     *     The uuid of the player whose movables to count.
     * @return The total number of movables own by the given player.
     */
    int getMovableCountForPlayer(UUID playerUUID);

    /**
     * Gets the number of movables own by the given player with the given name.
     *
     * @param playerUUID
     *     The uuid of the player whose movables to count.
     * @param movableName
     *     The name of the movable to search for.
     * @return The number of movables own by the given player with the given name.
     */
    int getMovableCountForPlayer(UUID playerUUID, String movableName);

    /**
     * Updates the {@link PPlayerData} for a given player.
     *
     * @param playerData
     *     The {@link PPlayerData} the represents a player.
     * @return True if at least 1 record was modified.
     */
    boolean updatePlayerData(PPlayerData playerData);

    /**
     * Tries to find the {@link PPlayerData} for a player with the given {@link UUID}.
     *
     * @param uuid
     *     The {@link UUID} of a player.
     * @return The {@link PPlayerData} that represents the player.
     */
    Optional<PPlayerData> getPlayerData(UUID uuid);

    /**
     * Tries to get all the players with a given name. Because names are not unique, this may result in any number of
     * matches.
     * <p>
     * If you know the player's UUID, it is recommended to use {@link #getPlayerData(UUID)} instead.
     *
     * @param playerName
     *     The name of the player(s).
     * @return All the players with the given name.
     */
    List<PPlayerData> getPlayerData(String playerName);

    /**
     * Gets the total number of movables with the given name regardless of who owns them.
     *
     * @param movableName
     *     The name of the movables to search for.
     * @return The total number of movables with the given name.
     */
    int getMovableCountByName(String movableName);

    /**
     * Gets the total number of owners of a movable.
     *
     * @param movableUID
     *     The {@link AbstractMovable}.
     * @return The total number of owners of this movable.
     */
    int getOwnerCountOfMovable(long movableUID);

    /**
     * Gets the movable with the given UID for the given player at the level of ownership this player has over the
     * movable, if any.
     *
     * @param playerUUID
     *     The UUID of the player.
     * @param movableUID
     *     The UID of the movable to retrieve.
     * @return The movable if it exists and if the player is an owner of it.
     */
    Optional<AbstractMovable> getMovable(UUID playerUUID, long movableUID);

    /**
     * Gets the movable with the given movableUID and the original creator as {@link MovableOwner};
     *
     * @param movableUID
     *     The UID of the movable to retrieve.
     * @return The movable with the given movableUID and the original creator.
     */
    Optional<AbstractMovable> getMovable(long movableUID);

    /**
     * Gets all the movables owned by the given player with the given name.
     *
     * @param playerUUID
     *     The UUID of the player to search for.
     * @param name
     *     The name of the movables to search for.
     * @return All movables owned by the given player with the given name.
     */
    List<AbstractMovable> getMovables(UUID playerUUID, String name);

    /**
     * Gets all the movables owned by the given player.
     *
     * @param playerUUID
     *     The UUID of the player to search for.
     * @return All movables owned by the given player.
     */
    List<AbstractMovable> getMovables(UUID playerUUID);

    /**
     * Gets all the movables with the given name, regardless of who owns them.
     *
     * @param name
     *     The name of the movables to search for.
     * @return All movables with the given name or an empty Optional if none exist.
     */
    List<AbstractMovable> getMovables(String name);

    /**
     * Gets all the movables with the given name, owned by the player with at least a certain permission level.
     *
     * @param playerUUID
     *     The name of the player who owns the movables.
     * @param movableName
     *     The name of the movables to search for.
     * @param maxPermission
     *     The maximum level of ownership (inclusive) this player has over the movables.
     * @return All the movables with the given name, owned the player with at least a certain permission level.
     */
    List<AbstractMovable> getMovables(UUID playerUUID, String movableName, PermissionLevel maxPermission);

    /**
     * Gets all the movables owned by a given player with at least a certain permission level.
     *
     * @param playerUUID
     *     The name of the player who owns the movables.
     * @param maxPermission
     *     The maximum level of ownership (inclusive) this player has over the movables.
     * @return All the movables owned by the player with at least a certain permission level.
     */
    List<AbstractMovable> getMovables(UUID playerUUID, PermissionLevel maxPermission);

    /**
     * Gets a map of location hashes and their connected powerblocks for all movables in a chunk.
     * <p>
     * The key is the hashed location in chunk space, the value is the list of UIDs of the movables whose powerblocks
     * occupies that location.
     *
     * @param chunkId
     *     The id of the chunk the movables are in.
     * @return A map of location hashes and their connected powerblocks for all movables in a chunk.
     */
    ConcurrentHashMap<Integer, List<Long>> getPowerBlockData(long chunkId);

    /**
     * Gets a list of movable UIDs that have their rotation point in a given chunk.
     *
     * @param chunkId
     *     The id of the chunk the movables are in.
     * @return A list of movables that have their rotation point in a given chunk.
     */
    List<AbstractMovable> getMovablesInChunk(long chunkId);

    /**
     * Inserts a new movable in the database. If the insertion was successful, a new {@link AbstractMovable} will be
     * created with the correct movableUID.
     *
     * @param movable
     *     The movable to insert.
     * @return The {@link AbstractMovable} that was just inserted if insertion was successful. This is
     * <u><b>NOT!!</b></u> the same object as the one passed to this method.
     */
    Optional<AbstractMovable> insert(AbstractMovable movable);

    /**
     * Synchronizes an {@link AbstractMovable} movable with the database. This will synchronize both the base and the
     * type-specific data of the {@link AbstractMovable}.
     *
     * @param movable
     *     The {@link IMovableConst} that describes the data of movable.
     * @param typeData
     *     The type-specific data of this movable.
     * @return True if the update was successful.
     */
    boolean syncMovableData(IMovableConst movable, byte[] typeData);

    /**
     * Retrieves all {@link DatabaseManager.MovableIdentifier}s that start with the provided input.
     * <p>
     * For example, this method can retrieve the identifiers "1", "10", "11", "100", etc. from an input of "1" or
     * "MyDoor", "MyPortcullis", "MyOtherDoor", etc. from an input of "My".
     *
     * @param input
     *     The partial identifier to look for.
     * @param player
     *     The player that should own the movables. May be null to disregard ownership.
     * @param maxPermission
     *     The maximum level of ownership (inclusive) this player has over the movables.
     * @return All {@link DatabaseManager.MovableIdentifier}s that start with the provided input.
     */
    List<DatabaseManager.MovableIdentifier> getPartialIdentifiers(
        String input, @Nullable IPPlayer player, PermissionLevel maxPermission);

    /**
     * Deletes a {@link MovableType} and all {@link AbstractMovable}s of this type from the database.
     * <p>
     * Note that the {@link MovableType} has to be registered before it can be deleted! It doesn't need to be enabled,
     * though.
     *
     * @param movableType
     *     The {@link MovableType} to delete.
     * @return True if deletion was successful.
     */
    boolean deleteMovableType(MovableType movableType);

    /**
     * Removes an owner of a movable. Note that the original creator can never be removed.
     *
     * @param movableUID
     *     The UID of the movable to modify.
     * @param playerUUID
     *     The UUID of the player to remove as owner of the movable.
     * @return True if an owner was removed.
     */
    boolean removeOwner(long movableUID, UUID playerUUID);

    /**
     * Adds a player as owner of a movable with at a certain permission level to a movable.
     * <p>
     * Note that permission level 0 is reserved for the creator, and negative values are not allowed.
     *
     * @param movableUID
     *     The UID of the movable to modify.
     * @param player
     *     The player to add as owner.
     * @param permission
     *     The level of ownership the player will have over the movable.
     * @return True if the update was successful.
     */
    boolean addOwner(long movableUID, PPlayerData player, PermissionLevel permission);

    /**
     * Gets the flag value of various boolean properties of a {@link AbstractMovable}.
     *
     * @param movable
     *     The {@link AbstractMovable}.
     * @return The flag value of a {@link AbstractMovable}.
     */
    default long getFlag(AbstractMovable movable)
    {
        long flag = 0;
        flag = IBitFlag.changeFlag(MovableFlag.getFlagValue(MovableFlag.IS_OPEN), movable.isOpen(), flag);
        flag = IBitFlag.changeFlag(MovableFlag.getFlagValue(MovableFlag.IS_LOCKED), movable.isLocked(), flag);
        return flag;
    }

    /**
     * Gets the flag value of various boolean properties of a {@link AbstractMovable}.
     *
     * @param isOpen
     *     Whether the movable is currently open.
     * @param isLocked
     *     Whether the movable is currently locked.
     * @return The flag value of a {@link AbstractMovable}.
     */
    default long getFlag(boolean isOpen, boolean isLocked)
    {
        long flag = 0;
        flag = IBitFlag.changeFlag(MovableFlag.getFlagValue(MovableFlag.IS_OPEN), isOpen, flag);
        flag = IBitFlag.changeFlag(MovableFlag.getFlagValue(MovableFlag.IS_LOCKED), isLocked, flag);
        return flag;
    }

    /**
     * Obtains {@link DatabaseState} the database is in.
     *
     * @return The {@link DatabaseState} the database is in.
     */
    DatabaseState getDatabaseState();

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
    }

    /**
     * Set of bit flags to represent various properties of movables.
     *
     * @author Pim
     */
    enum MovableFlag implements IBitFlag
    {
        /**
         * Consider a movable to be opened if this flag is enabled.
         */
        IS_OPEN(0b00000001),

        /**
         * Consider a movable to be locked if this flag is enabled.
         */
        IS_LOCKED(0b00000010),

        /**
         * Consider a movable switched on if this flag is enabled. Used in cases of perpetual movement.
         */
        IS_SWITCHED_ON(0b00000100),
        ;

        /**
         * The bit value of the flag.
         */
        private final long flagValue;

        MovableFlag(long flagValue)
        {
            this.flagValue = flagValue;
        }

        /**
         * Gets the flag value of a {@link MovableFlag}.
         *
         * @param flag
         *     The {@link MovableFlag}.
         * @return The flag value of a {@link MovableFlag}.
         */
        public static long getFlagValue(MovableFlag flag)
        {
            return flag.flagValue;
        }
    }
}
