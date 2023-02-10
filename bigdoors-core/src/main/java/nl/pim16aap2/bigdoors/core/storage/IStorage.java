package nl.pim16aap2.bigdoors.core.storage;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.longs.LongList;
import nl.pim16aap2.bigdoors.core.api.IPlayer;
import nl.pim16aap2.bigdoors.core.api.PlayerData;
import nl.pim16aap2.bigdoors.core.managers.DatabaseManager;
import nl.pim16aap2.bigdoors.core.structures.AbstractStructure;
import nl.pim16aap2.bigdoors.core.structures.IStructureConst;
import nl.pim16aap2.bigdoors.core.structures.PermissionLevel;
import nl.pim16aap2.bigdoors.core.structures.StructureOwner;
import nl.pim16aap2.bigdoors.core.structuretypes.StructureType;
import nl.pim16aap2.bigdoors.core.util.IBitFlag;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Represents storage of all structure related stuff.
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
     * Delete the structure with the given structureUID from the database.
     *
     * @param structureUID
     *     The UID of the structure to delete.
     * @return True if at least 1 structure was successfully removed.
     */
    boolean removeStructure(long structureUID);

    /**
     * Delete all structures owned by the given player with the given name.
     *
     * @param playerUUID
     *     The player whose structures to delete.
     * @param structureName
     *     The name of the structures to delete.
     * @return True if at least 1 structure was successfully removed.
     */
    boolean removeStructures(UUID playerUUID, String structureName);

    /**
     * Checks whether there are any structures in a given world.
     *
     * @param worldName
     *     The name of the world.
     * @return True if there are more than 0 structures in the given world.
     */
    boolean isBigDoorsWorld(String worldName);

    /**
     * Gets the total number of structures own by the given player.
     *
     * @param playerUUID
     *     The uuid of the player whose structures to count.
     * @return The total number of structures own by the given player.
     */
    int getStructureCountForPlayer(UUID playerUUID);

    /**
     * Gets the number of structures own by the given player with the given name.
     *
     * @param playerUUID
     *     The uuid of the player whose structures to count.
     * @param structureName
     *     The name of the structure to search for.
     * @return The number of structures own by the given player with the given name.
     */
    int getStructureCountForPlayer(UUID playerUUID, String structureName);

    /**
     * Updates the {@link PlayerData} for a given player.
     *
     * @param playerData
     *     The {@link PlayerData} the represents a player.
     * @return True if at least 1 record was modified.
     */
    boolean updatePlayerData(PlayerData playerData);

    /**
     * Tries to find the {@link PlayerData} for a player with the given {@link UUID}.
     *
     * @param uuid
     *     The {@link UUID} of a player.
     * @return The {@link PlayerData} that represents the player.
     */
    Optional<PlayerData> getPlayerData(UUID uuid);

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
    List<PlayerData> getPlayerData(String playerName);

    /**
     * Gets the total number of structures with the given name regardless of who owns them.
     *
     * @param structureName
     *     The name of the structures to search for.
     * @return The total number of structures with the given name.
     */
    int getStructureCountByName(String structureName);

    /**
     * Gets the total number of owners of a structure.
     *
     * @param structureUID
     *     The {@link AbstractStructure}.
     * @return The total number of owners of this structure.
     */
    int getOwnerCountOfStructure(long structureUID);

    /**
     * Gets the structure with the given UID for the given player at the level of ownership this player has over the
     * structure, if any.
     *
     * @param playerUUID
     *     The UUID of the player.
     * @param structureUID
     *     The UID of the structure to retrieve.
     * @return The structure if it exists and if the player is an owner of it.
     */
    Optional<AbstractStructure> getStructure(UUID playerUUID, long structureUID);

    /**
     * Gets the structure with the given structureUID and the original creator as {@link StructureOwner};
     *
     * @param structureUID
     *     The UID of the structure to retrieve.
     * @return The structure with the given structureUID and the original creator.
     */
    Optional<AbstractStructure> getStructure(long structureUID);

    /**
     * Gets all the structures owned by the given player with the given name.
     *
     * @param playerUUID
     *     The UUID of the player to search for.
     * @param name
     *     The name of the structures to search for.
     * @return All structures owned by the given player with the given name.
     */
    List<AbstractStructure> getStructures(UUID playerUUID, String name);

    /**
     * Gets all the structures owned by the given player.
     *
     * @param playerUUID
     *     The UUID of the player to search for.
     * @return All structures owned by the given player.
     */
    List<AbstractStructure> getStructures(UUID playerUUID);

    /**
     * Gets all the structures with the given name, regardless of who owns them.
     *
     * @param name
     *     The name of the structures to search for.
     * @return All structures with the given name or an empty Optional if none exist.
     */
    List<AbstractStructure> getStructures(String name);

    /**
     * Gets all the structures with the given name, owned by the player with at least a certain permission level.
     *
     * @param playerUUID
     *     The name of the player who owns the structures.
     * @param structureName
     *     The name of the structures to search for.
     * @param maxPermission
     *     The maximum level of ownership (inclusive) this player has over the structures.
     * @return All the structures with the given name, owned the player with at least a certain permission level.
     */
    List<AbstractStructure> getStructures(UUID playerUUID, String structureName, PermissionLevel maxPermission);

    /**
     * Gets all the structures owned by a given player with at least a certain permission level.
     *
     * @param playerUUID
     *     The name of the player who owns the structures.
     * @param maxPermission
     *     The maximum level of ownership (inclusive) this player has over the structures.
     * @return All the structures owned by the player with at least a certain permission level.
     */
    List<AbstractStructure> getStructures(UUID playerUUID, PermissionLevel maxPermission);

    /**
     * Gets a map of location hashes and their connected powerblocks for all structures in a chunk.
     * <p>
     * The key is the hashed location in chunk space, the value is the list of UIDs of the structures whose powerblocks
     * occupies that location.
     *
     * @param chunkId
     *     The id of the chunk the structures are in.
     * @return A map of location hashes and their connected powerblocks for all structures in a chunk.
     */
    Int2ObjectMap<LongList> getPowerBlockData(long chunkId);

    /**
     * Gets a list of structure UIDs that have their rotation point in a given chunk.
     *
     * @param chunkId
     *     The id of the chunk the structures are in.
     * @return A list of structures that have their rotation point in a given chunk.
     */
    List<AbstractStructure> getStructuresInChunk(long chunkId);

    /**
     * Inserts a new structure in the database. If the insertion was successful, a new {@link AbstractStructure} will be
     * created with the correct structureUID.
     *
     * @param structure
     *     The structure to insert.
     * @return The {@link AbstractStructure} that was just inserted if insertion was successful. This is
     * <u><b>NOT!!</b></u> the same object as the one passed to this method.
     */
    Optional<AbstractStructure> insert(AbstractStructure structure);

    /**
     * Synchronizes an {@link AbstractStructure} structure with the database. This will synchronize both the base and
     * the type-specific data of the {@link AbstractStructure}.
     *
     * @param structure
     *     The {@link IStructureConst} that describes the data of structure.
     * @param typeData
     *     The type-specific data of this structure.
     * @return True if the update was successful.
     */
    boolean syncStructureData(IStructureConst structure, String typeData);

    /**
     * Retrieves all {@link DatabaseManager.StructureIdentifier}s that start with the provided input.
     * <p>
     * For example, this method can retrieve the identifiers "1", "10", "11", "100", etc. from an input of "1" or
     * "MyDoor", "MyPortcullis", "MyOtherDoor", etc. from an input of "My".
     *
     * @param input
     *     The partial identifier to look for.
     * @param player
     *     The player that should own the structures. May be null to disregard ownership.
     * @param maxPermission
     *     The maximum level of ownership (inclusive) this player has over the structures.
     * @return All {@link DatabaseManager.StructureIdentifier}s that start with the provided input.
     */
    List<DatabaseManager.StructureIdentifier> getPartialIdentifiers(
        String input, @Nullable IPlayer player, PermissionLevel maxPermission);

    /**
     * Deletes a {@link StructureType} and all {@link AbstractStructure}s of this type from the database.
     * <p>
     * Note that the {@link StructureType} has to be registered before it can be deleted! It doesn't need to be enabled,
     * though.
     *
     * @param structureType
     *     The {@link StructureType} to delete.
     * @return True if deletion was successful.
     */
    boolean deleteStructureType(StructureType structureType);

    /**
     * Removes an owner of a structure. Note that the original creator can never be removed.
     *
     * @param structureUID
     *     The UID of the structure to modify.
     * @param playerUUID
     *     The UUID of the player to remove as owner of the structure.
     * @return True if an owner was removed.
     */
    boolean removeOwner(long structureUID, UUID playerUUID);

    /**
     * Adds a player as owner of a structure with at a certain permission level to a structure.
     * <p>
     * Note that permission level 0 is reserved for the creator, and negative values are not allowed.
     *
     * @param structureUID
     *     The UID of the structure to modify.
     * @param player
     *     The player to add as owner.
     * @param permission
     *     The level of ownership the player will have over the structure.
     * @return True if the update was successful.
     */
    boolean addOwner(long structureUID, PlayerData player, PermissionLevel permission);

    /**
     * Gets the flag value of various boolean properties of a {@link AbstractStructure}.
     *
     * @param structure
     *     The {@link AbstractStructure}.
     * @return The flag value of a {@link AbstractStructure}.
     */
    default long getFlag(AbstractStructure structure)
    {
        long flag = 0;
        flag = IBitFlag.changeFlag(StructureFlag.getFlagValue(StructureFlag.IS_OPEN), structure.isOpen(), flag);
        flag = IBitFlag.changeFlag(StructureFlag.getFlagValue(StructureFlag.IS_LOCKED), structure.isLocked(), flag);
        return flag;
    }

    /**
     * Gets the flag value of various boolean properties of a {@link AbstractStructure}.
     *
     * @param isOpen
     *     Whether the structure is currently open.
     * @param isLocked
     *     Whether the structure is currently locked.
     * @return The flag value of a {@link AbstractStructure}.
     */
    default long getFlag(boolean isOpen, boolean isLocked)
    {
        long flag = 0;
        flag = IBitFlag.changeFlag(StructureFlag.getFlagValue(StructureFlag.IS_OPEN), isOpen, flag);
        flag = IBitFlag.changeFlag(StructureFlag.getFlagValue(StructureFlag.IS_LOCKED), isLocked, flag);
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
     * Set of bit flags to represent various properties of structures.
     *
     * @author Pim
     */
    enum StructureFlag implements IBitFlag
    {
        /**
         * Consider a structure to be opened if this flag is enabled.
         */
        IS_OPEN(0b00000001),

        /**
         * Consider a structure to be locked if this flag is enabled.
         */
        IS_LOCKED(0b00000010),

        /**
         * Consider a structure switched on if this flag is enabled. Used in cases of perpetual movement.
         */
        IS_SWITCHED_ON(0b00000100),
        ;

        /**
         * The bit value of the flag.
         */
        private final long flagValue;

        StructureFlag(long flagValue)
        {
            this.flagValue = flagValue;
        }

        /**
         * Gets the flag value of a {@link StructureFlag}.
         *
         * @param flag
         *     The {@link StructureFlag}.
         * @return The flag value of a {@link StructureFlag}.
         */
        public static long getFlagValue(StructureFlag flag)
        {
            return flag.flagValue;
        }
    }
}
