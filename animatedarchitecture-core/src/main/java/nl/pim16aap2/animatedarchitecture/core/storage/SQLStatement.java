package nl.pim16aap2.animatedarchitecture.core.storage;

import it.unimi.dsi.fastutil.ints.IntImmutableList;
import lombok.Getter;
import nl.pim16aap2.animatedarchitecture.core.util.StringUtil;

/**
 * Represents an SQL statement.
 */
@SuppressWarnings("unused")
@Getter
public enum SQLStatement
{
    UPDATE_STRUCTURE_BASE("""
        UPDATE Structure SET
        name                 = ?,
        world                = ?,
        xMin                 = ?,
        yMin                 = ?,
        zMin                 = ?,
        xMax                 = ?,
        yMax                 = ?,
        zMax                 = ?,
        centerPointChunkId   = ?,
        powerBlockX          = ?,
        powerBlockY          = ?,
        powerBlockZ          = ?,
        powerBlockChunkId    = ?,
        animationDirection   = ?,
        bitflag              = ?,
        typeVersion          = ?,
        typeData             = ?,
        properties           = ?
        WHERE id             = ?;
        """
    ),

    UPDATE_STRUCTURE_OWNER_PERMISSION(
        "UPDATE StructureOwnerPlayer SET permission = ? WHERE playerID = ? and structureUID = ?;"
    ),

    GET_STRUCTURE_OWNER_PLAYER(
        "SELECT * FROM StructureOwnerPlayer WHERE playerID = ? AND structureUID = ?;"
    ),

    DELETE_NAMED_STRUCTURE_OF_PLAYER("""
        DELETE FROM Structure
        WHERE Structure.id IN
            (SELECT S.id
            FROM Structure AS S INNER JOIN StructureOwnerPlayer AS U ON U.structureUID = S.id,
                (SELECT P.id FROM Player as P WHERE P.playerUUID = ?) AS R
                WHERE S.name = ? AND R.id = U.playerID);
        """
    ),

    DELETE_STRUCTURE_TYPE(
        "DELETE FROM Structure WHERE Structure.type = ?;"
    ),

    INSERT_STRUCTURE_OWNER(
        "INSERT INTO StructureOwnerPlayer (permission, playerID, structureUID) VALUES (?,?,?);"
    ),

    REMOVE_STRUCTURE_OWNER("""
        DELETE
        FROM StructureOwnerPlayer
        WHERE StructureOwnerPlayer.id IN
            (SELECT O.id
            FROM StructureOwnerPlayer AS O INNER JOIN Player AS P on O.playerID = P.id
            WHERE P.playerUUID = ? AND O.permission > '0' AND O.structureUID = ?);
        """
    ),

    GET_POWER_BLOCK_DATA_IN_CHUNK("""
        SELECT id, powerBlockX, powerBlockY, powerBlockZ, powerBlockChunkId
        FROM Structure
        WHERE powerBlockChunkId = ?;
        """
    ),

    INSERT_OR_IGNORE_PLAYER_DATA("""
        INSERT OR IGNORE INTO Player
        (playerUUID,
         playerName,
         limitStructureSize,
         limitStructureCount,
         limitPowerBlockDistance,
         limitBlocksToMove,
         permissions)
        VALUES(?, ?, ?, ?, ?, ?, ?);
        """
    ),

    GET_IDENTIFIERS_FROM_PARTIAL_NAME_MATCH_WITH_OWNER("""
        SELECT S.type, S.id, S.name
        FROM Structure AS S
        INNER JOIN StructureOwnerPlayer AS O ON S.id = O.structureUID
        INNER JOIN Player AS P ON O.playerID = P.id
        WHERE S.name like ? || '%' AND O.permission <= ? AND (? IS NULL OR P.playerUUID IS ?)
        GROUP BY S.id;
        """
    ),

    GET_IDENTIFIERS_FROM_PARTIAL_UID_MATCH_WITH_OWNER("""
        SELECT S.type, S.id, S.name
        FROM Structure AS S
        INNER JOIN StructureOwnerPlayer AS O ON S.id = O.structureUID
        INNER JOIN Player AS P ON O.playerID = P.id
        WHERE S.id like ? || '%' AND O.permission <= ? AND (? IS NULL OR P.playerUUID IS ?)
        GROUP BY S.id;
        """
    ),

    UPDATE_PLAYER_DATA("""
        UPDATE Player SET
        playerName              = ?,
        limitStructureSize      = ?,
        limitStructureCount     = ?,
        limitPowerBlockDistance = ?,
        limitBlocksToMove       = ?,
        permissions             = ?
        WHERE playerUUID        = ?;
        """
    ),

    GET_PLAYER_DATA(
        "SELECT * FROM Player WHERE playerUUID = ?;"
    ),

    GET_PLAYER_DATA_FROM_NAME(
        "SELECT * FROM Player WHERE playerName = ?;"
    ),

    GET_OWNER_COUNT_OF_STRUCTURE(
        "SELECT COUNT(*) AS total FROM StructureOwnerPlayer WHERE structureUID = ?;"
    ),

    GET_STRUCTURE_COUNT_BY_NAME(
        "SELECT COUNT(*) AS total FROM Structure WHERE name = ?;"
    ),

    GET_PLAYER_STRUCTURE_COUNT("""
        SELECT COUNT(*) AS total
        FROM StructureOwnerPlayer AS U
        INNER JOIN Player AS P on U.playerID = P.id
        INNER JOIN Structure AS S ON U.structureUID = S.id
        WHERE P.playerUUID = ? AND S.name = ?;
        """
    ),

    GET_STRUCTURE_COUNT_FOR_PLAYER("""
        SELECT COUNT(*) AS total
        FROM StructureOwnerPlayer AS U INNER JOIN Player AS P on U.playerID = P.id
        WHERE P.playerUUID = ?;
        """
    ),

    IS_ANIMATE_ARCHITECTURE_WORLD("""
        SELECT world
        FROM Structure
        WHERE world = ?
        LIMIT 1;
        """
    ),

    DELETE_STRUCTURE(
        "DELETE FROM Structure WHERE id = ?;"
    ),

    GET_PLAYER_ID(
        "SELECT id FROM Player WHERE playerUUID = ?;"
    ),

    GET_STRUCTURE_BASE_FROM_ID("""
        SELECT Structure.*, Player.*, StructureOwnerPlayer.permission
        FROM Structure
        INNER JOIN StructureOwnerPlayer ON Structure.id = StructureOwnerPlayer.structureUID
        INNER JOIN Player ON StructureOwnerPlayer.playerID = Player.id
        WHERE Structure.id = ? AND StructureOwnerPlayer.permission = 0;
        """
    ),

    /**
     * Obtains the structures whose center point's chunk hash value has a certain value.
     */
    GET_STRUCTURES_IN_CHUNK("""
        SELECT Structure.*, Player.*, StructureOwnerPlayer.permission
        FROM Structure
        INNER JOIN StructureOwnerPlayer ON Structure.id = StructureOwnerPlayer.structureUID
        INNER JOIN Player ON StructureOwnerPlayer.playerID = Player.id
        WHERE Structure.centerPointChunkId = ?;
        """
    ),

    GET_STRUCTURE_BASE_FROM_ID_FOR_PLAYER("""
        SELECT Structure.*, Player.*, StructureOwnerPlayer.permission
        FROM Structure
        INNER JOIN StructureOwnerPlayer ON Structure.id = StructureOwnerPlayer.structureUID
        INNER JOIN Player ON StructureOwnerPlayer.playerID = Player.id
        WHERE Structure.id = ? AND Player.playerUUID = ?;
        """
    ),

    GET_NAMED_STRUCTURES_OWNED_BY_PLAYER("""
        SELECT Structure.*, Player.*, StructureOwnerPlayer.permission
        FROM Structure
        INNER JOIN StructureOwnerPlayer ON Structure.id = StructureOwnerPlayer.structureUID
        INNER JOIN Player ON StructureOwnerPlayer.playerID = Player.id
        WHERE Player.playerUUID = ? AND Structure.name = ? And StructureOwnerPlayer.permission <= ?;
        """
    ),

    GET_STRUCTURES_WITH_NAME("""
        SELECT Structure.*, Player.*, StructureOwnerPlayer.permission
        FROM Structure
        INNER JOIN StructureOwnerPlayer ON Structure.id = StructureOwnerPlayer.structureUID
        INNER JOIN Player ON StructureOwnerPlayer.playerID = Player.id
        WHERE Structure.name = ? And StructureOwnerPlayer.permission = 0;
        """
    ),

    GET_STRUCTURES_OWNED_BY_PLAYER_WITH_LEVEL("""
        SELECT Structure.*, Player.*, StructureOwnerPlayer.permission
        FROM Structure
        INNER JOIN StructureOwnerPlayer ON Structure.id = StructureOwnerPlayer.structureUID
        INNER JOIN Player ON StructureOwnerPlayer.playerID = Player.id
        WHERE Player.playerUUID = ? AND StructureOwnerPlayer.permission <= ?;
        """
    ),

    GET_STRUCTURE_OWNERS("""
        SELECT O.structureUID, O.permission, P.*
        FROM StructureOwnerPlayer AS O INNER JOIN Player AS P ON O.playerID = P.id
        WHERE structureUID = ?;
        """
    ),

    GET_STRUCTURES_OF_TYPE("""
        SELECT Structure.*, Player.*, StructureOwnerPlayer.permission
        FROM Structure
        INNER JOIN StructureOwnerPlayer ON Structure.id = StructureOwnerPlayer.structureUID
        INNER JOIN Player ON StructureOwnerPlayer.playerID = Player.id
        WHERE Structure.type = ? AND StructureOwnerPlayer.permission = 0;
        """
    ),

    GET_STRUCTURES_OF_VERSIONED_TYPE("""
        SELECT Structure.*, Player.*, StructureOwnerPlayer.permission
        FROM Structure
        INNER JOIN StructureOwnerPlayer ON Structure.id = StructureOwnerPlayer.structureUID
        INNER JOIN Player ON StructureOwnerPlayer.playerID = Player.id
        WHERE Structure.typeVersion = ? AND Structure.type = ? AND StructureOwnerPlayer.permission = 0;
        """
    ),

    INSERT_STRUCTURE_BASE("""
        INSERT INTO Structure
        (name, world, xMin, yMin, zMin, xMax, yMax, zMax, centerPointChunkId,
         powerBlockX, powerBlockY, powerBlockZ, powerBlockChunkId, animationDirection,
         bitflag, type, typeVersion, typeData, properties)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        RETURNING id;
        """
    ),

    /**
     * Inserts a new structure creator. This is a structure owner with permission level 0.
     */
    INSERT_PRIME_OWNER("""
        INSERT INTO StructureOwnerPlayer (permission, playerID, structureUID)
        VALUES (0,
            (SELECT id
            FROM Player
            WHERE Player.playerUUID = ?),
            ?);
        """
    ),

    ;

    /**
     * The statement this {@link SQLStatement} represents.
     */
    private final String statement;

    /**
     * The indices of the variables in the statement.
     */
    private final IntImmutableList variableIndices;

    /**
     * The number of variables in the statement.
     */
    private final int variableCount;

    SQLStatement(String statement)
    {
        this.statement = statement;
        this.variableIndices = StringUtil.getVariableIndices(statement, '?');
        this.variableCount = this.variableIndices.size();
    }

    /**
     * Gets the statement this {@link SQLStatement} represents.
     *
     * @param sqlStatement
     *     The {@link SQLStatement}.
     * @return The statement this {@link SQLStatement} represents.
     */
    public static String getStatement(SQLStatement sqlStatement)
    {
        return sqlStatement.statement;
    }

    /**
     * Gets the number of variables in the statement.
     *
     * @param sqlStatement
     *     The statement.
     * @return The number of variables in the statement.
     */
    public static int getVariableCountCount(SQLStatement sqlStatement)
    {
        return sqlStatement.variableCount;
    }

    /**
     * Constructs a new {@link DelayedPreparedStatement} from the {@link SQLStatement}.
     *
     * @param sqlStatement
     *     The {@link SQLStatement}.
     * @return A new {@link DelayedPreparedStatement}.
     */
    public static DelayedPreparedStatement constructDelayedPreparedStatement(SQLStatement sqlStatement)
    {
        return sqlStatement.constructDelayedPreparedStatement();
    }

    /**
     * Constructs a new {@link DelayedPreparedStatement} from this {@link SQLStatement}.
     *
     * @return A new {@link DelayedPreparedStatement}.
     */
    public DelayedPreparedStatement constructDelayedPreparedStatement()
    {
        return new DelayedPreparedStatement(this);
    }
}
