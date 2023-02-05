package nl.pim16aap2.bigdoors.core.storage;

import nl.pim16aap2.bigdoors.core.util.Util;

/**
 * Represents an SQL statement.
 *
 * @author Pim
 */
@SuppressWarnings("unused")
public enum SQLStatement
{
    UPDATE_STRUCTURE_BASE(
        """
        UPDATE Structure SET
        name           = ?,
        world          = ?,
        xMin           = ?,
        yMin           = ?,
        zMin           = ?,
        xMax           = ?,
        yMax           = ?,
        zMax           = ?,
        rotationPointX = ?,
        rotationPointY = ?,
        rotationPointZ = ?,
        rotationPointChunkId = ?,
        powerBlockX    = ?,
        powerBlockY    = ?,
        powerBlockZ    = ?,
        powerBlockChunkId = ?,
        openDirection  = ?,
        bitflag        = ?,
        typeVersion    = ?,
        typeData       = ?
        WHERE id       = ?;
        """
    ),

    UPDATE_STRUCTURE_OWNER_PERMISSION(
        "UPDATE StructureOwnerPlayer SET permission = ? WHERE playerID = ? and structureUID = ?;"
    ),

    GET_STRUCTURE_OWNER_PLAYER(
        "SELECT * FROM StructureOwnerPlayer WHERE playerID = ? AND structureUID = ?;"
    ),

    DELETE_NAMED_STRUCTURE_OF_PLAYER(
        """
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

    GET_LATEST_ROW_ADDITION(
        "SELECT last_insert_rowid() AS lastId;"
    ),

    INSERT_STRUCTURE_OWNER(
        "INSERT INTO StructureOwnerPlayer (permission, playerID, structureUID) VALUES (?,?,?);"
    ),

    REMOVE_STRUCTURE_OWNER(
        """
        DELETE
        FROM StructureOwnerPlayer
        WHERE StructureOwnerPlayer.id IN
            (SELECT O.id
            FROM StructureOwnerPlayer AS O INNER JOIN Player AS P on O.playerID = P.id
            WHERE P.playerUUID = ? AND O.permission > '0' AND O.structureUID = ?);
        """
    ),

    GET_POWER_BLOCK_DATA_IN_CHUNK(
        """
        SELECT id, powerBlockX, powerBlockY, powerBlockZ, powerBlockChunkId
        FROM Structure
        WHERE powerBlockChunkId = ?;
        """
    ),

    /**
     * Gets all the structures that have their <b>rotationPoint</b> in the chunk with the given chunk hash.
     */
    GET_STRUCTURE_IN_CHUNK(
        "SELECT * FROM Structure WHERE rotationPointChunkId = ?;"
    ),

    INSERT_OR_IGNORE_PLAYER_DATA(
        """
        INSERT OR IGNORE INTO Player
        (playerUUID, playerName, sizeLimit, countLimit, permissions)
        VALUES(?, ?, ?, ?, ?);
        """
    ),

    GET_IDENTIFIERS_FROM_PARTIAL_NAME_MATCH_WITH_OWNER(
        """
        SELECT S.id, S.name
        FROM Structure AS S
        INNER JOIN StructureOwnerPlayer AS O ON S.id = O.structureUID
        INNER JOIN Player AS P ON O.playerID = P.id
        WHERE S.name like ? || '%' AND O.permission <= ? AND (? IS NULL OR P.playerUUID IS ?)
        GROUP BY S.id;
        """
    ),

    GET_IDENTIFIERS_FROM_PARTIAL_UID_MATCH_WITH_OWNER(
        """
        SELECT S.id, S.name
        FROM Structure AS S
        INNER JOIN StructureOwnerPlayer AS O ON S.id = O.structureUID
        INNER JOIN Player AS P ON O.playerID = P.id
        WHERE S.id like ? || '%' AND O.permission <= ? AND (? IS NULL OR P.playerUUID IS ?)
        GROUP BY S.id;
        """
    ),

    UPDATE_PLAYER_DATA(
        """
        UPDATE Player SET
        playerName       = ?,
        sizeLimit        = ?,
        countLimit       = ?,
        permissions      = ?
        WHERE playerUUID = ?;
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

    GET_PLAYER_STRUCTURE_COUNT(
        """
        SELECT COUNT(*) AS total
        FROM StructureOwnerPlayer AS U
        INNER JOIN Player AS P on U.playerID = P.id
        INNER JOIN Structure AS S ON U.structureUID = S.id
        WHERE P.playerUUID = ? AND S.name = ?;
        """
    ),

    GET_STRUCTURE_COUNT_FOR_PLAYER(
        """
        SELECT COUNT(*) AS total
        FROM StructureOwnerPlayer AS U INNER JOIN Player AS P on U.playerID = P.id
        WHERE P.playerUUID = ?;
        """
    ),

    IS_BIGDOORS_WORLD(
        """
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

    GET_STRUCTURE_BASE_FROM_ID(
        """
        SELECT Structure.*, Player.*, StructureOwnerPlayer.permission
        FROM Structure
        INNER JOIN StructureOwnerPlayer ON Structure.id = StructureOwnerPlayer.structureUID
        INNER JOIN Player ON StructureOwnerPlayer.playerID = Player.id
        WHERE Structure.id = ? AND StructureOwnerPlayer.permission = 0;
        """
    ),

    /**
     * Obtains the structures whose rotationPoint's chunk hash value has a certain value.
     */
    GET_STRUCTURES_IN_CHUNK(
        """
        SELECT Structure.*, Player.*, StructureOwnerPlayer.permission
        FROM Structure
        INNER JOIN StructureOwnerPlayer ON Structure.id = StructureOwnerPlayer.structureUID
        INNER JOIN Player ON StructureOwnerPlayer.playerID = Player.id
        WHERE Structure.rotationPointChunkId = ?;
        """
    ),

    GET_STRUCTURE_BASE_FROM_ID_FOR_PLAYER(
        """
        SELECT Structure.*, Player.*, StructureOwnerPlayer.permission
        FROM Structure
        INNER JOIN StructureOwnerPlayer ON Structure.id = StructureOwnerPlayer.structureUID
        INNER JOIN Player ON StructureOwnerPlayer.playerID = Player.id
        WHERE Structure.id = ? AND Player.playerUUID = ?;
        """
    ),

    GET_NAMED_STRUCTURES_OWNED_BY_PLAYER(
        """
        SELECT Structure.*, Player.*, StructureOwnerPlayer.permission
        FROM Structure
        INNER JOIN StructureOwnerPlayer ON Structure.id = StructureOwnerPlayer.structureUID
        INNER JOIN Player ON StructureOwnerPlayer.playerID = Player.id
        WHERE Player.playerUUID = ? AND Structure.name = ? And StructureOwnerPlayer.permission <= ?;
        """
    ),

    GET_STRUCTURES_WITH_NAME(
        """
        SELECT Structure.*, Player.*, StructureOwnerPlayer.permission
        FROM Structure
        INNER JOIN StructureOwnerPlayer ON Structure.id = StructureOwnerPlayer.structureUID
        INNER JOIN Player ON StructureOwnerPlayer.playerID = Player.id
        WHERE Structure.name = ? And StructureOwnerPlayer.permission = 0;
        """
    ),

    GET_STRUCTURES_OWNED_BY_PLAYER_WITH_LEVEL(
        """
        SELECT Structure.*, Player.*, StructureOwnerPlayer.permission
        FROM Structure
        INNER JOIN StructureOwnerPlayer ON Structure.id = StructureOwnerPlayer.structureUID
        INNER JOIN Player ON StructureOwnerPlayer.playerID = Player.id
        WHERE Player.playerUUID = ? AND StructureOwnerPlayer.permission <= ?;
        """
    ),

    GET_STRUCTURE_OWNERS(
        """
        SELECT O.structureUID, O.permission, P.*
        FROM StructureOwnerPlayer AS O INNER JOIN Player AS P ON O.playerID = P.id
        WHERE structureUID = ?;
        """
    ),

    INSERT_STRUCTURE_BASE(
        """
        INSERT INTO Structure
        (name, world, xMin, yMin, zMin, xMax, yMax, zMax, rotationPointX, rotationPointY, rotationPointZ,
         rotationPointChunkId, powerBlockX, powerBlockY, powerBlockZ, powerBlockChunkId, openDirection,
         bitflag, type, typeVersion, typeData)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);
        """
    ),

    /**
     * Inserts a new structure creator. This is a structure owner with permission level 0.
     * <p>
     * This statement is intended to be used in the same transaction that inserted the Structures.
     */
    INSERT_PRIME_OWNER(
        """
        INSERT INTO StructureOwnerPlayer (permission, playerID, structureUID)
        VALUES (0,
            (SELECT id
            FROM Player
            WHERE Player.playerUUID = ?),
            (SELECT seq
            FROM sqlite_sequence
            WHERE sqlite_sequence.name = "Structure"));
        """
    ),

    SELECT_MOST_RECENT_STRUCTURE(
        """
        SELECT seq
        FROM sqlite_sequence
        WHERE sqlite_sequence.name = "Structure";
        """
    ),

    LEGACY_ALTER_TABLE_ON(
        "PRAGMA legacy_alter_table = ON;"
    ),

    LEGACY_ALTER_TABLE_OFF(
        "PRAGMA legacy_alter_table = OFF;"
    ),

    FOREIGN_KEYS_ON(
        "PRAGMA foreign_keys = ON;"
    ),

    FOREIGN_KEYS_OFF(
        "PRAGMA foreign_keys = OFF;"
    ),

    INSERT_SQLITE_SEQ(
        "INSERT OR IGNORE INTO SQLITE_SEQUENCE (name, seq) VALUES (?, ?);"
    ),

    RESERVE_IDS_PLAYER(
        "INSERT OR IGNORE INTO SQLITE_SEQUENCE (name, seq) VALUES ('Player', 10);"
    ),

    RESERVE_IDS_STRUCTURE(
        "INSERT OR IGNORE INTO SQLITE_SEQUENCE (name, seq) VALUES ('Structure', 10);"
    ),

    RESERVE_IDS_STRUCTURE_OWNER_PLAYER(
        "INSERT OR IGNORE INTO SQLITE_SEQUENCE (name, seq) VALUES ('StructureOwnerPlayer', 10);"
    ),

    CREATE_TABLE_PLAYER(
        """
        CREATE TABLE IF NOT EXISTS Player
        (id            INTEGER    PRIMARY KEY AUTOINCREMENT,
        playerUUID     TEXT       NOT NULL,
        playerName     TEXT       NOT NULL,
        sizeLimit      INTEGER    NOT NULL,
        countLimit     INTEGER    NOT NULL,
        permissions    INTEGER    NOT NULL,
        unique(playerUUID));
        """
    ),

    CREATE_TABLE_STRUCTURE(
        """
        CREATE TABLE IF NOT EXISTS Structure
        (id                   INTEGER    PRIMARY KEY AUTOINCREMENT,
        name                  TEXT       NOT NULL,
        world                 TEXT       NOT NULL,
        xMin                  INTEGER    NOT NULL,
        yMin                  INTEGER    NOT NULL,
        zMin                  INTEGER    NOT NULL,
        xMax                  INTEGER    NOT NULL,
        yMax                  INTEGER    NOT NULL,
        zMax                  INTEGER    NOT NULL,
        rotationPointX        INTEGER    NOT NULL,
        rotationPointY        INTEGER    NOT NULL,
        rotationPointZ        INTEGER    NOT NULL,
        rotationPointChunkId  INTEGER    NOT NULL,
        powerBlockX           INTEGER    NOT NULL,
        powerBlockY           INTEGER    NOT NULL,
        powerBlockZ           INTEGER    NOT NULL,
        powerBlockChunkId     INTEGER    NOT NULL,
        openDirection         INTEGER    NOT NULL,
        type                  TEXT       NOT NULL,
        typeVersion           INTEGER    NOT NULL,
        typeData              TEXT       NOT NULL,
        bitflag               INTEGER    NOT NULL);
        """
    ),

    CREATE_TABLE_STRUCTURE_OWNER_PLAYER(
        """
        CREATE TABLE IF NOT EXISTS StructureOwnerPlayer
        (id          INTEGER    PRIMARY KEY AUTOINCREMENT,
        permission   INTEGER    NOT NULL,
        playerID     REFERENCES Player(id)    ON UPDATE CASCADE ON DELETE CASCADE,
        structureUID REFERENCES Structure(id) ON UPDATE CASCADE ON DELETE CASCADE,
        unique (playerID, structureUID));
        """
    ),

    ;

    private final String statement;
    private final int variableCount;

    SQLStatement(String statement)
    {
        this.statement = statement;
        variableCount = Util.countPatternOccurrences(PPreparedStatement.QUESTION_MARK, statement);
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
     * Constructs a new {@link PPreparedStatement} from the {@link SQLStatement}.
     *
     * @param sqlStatement
     *     The {@link SQLStatement}.
     * @return A new {@link PPreparedStatement}.
     */
    public static PPreparedStatement constructPPreparedStatement(SQLStatement sqlStatement)
    {
        return sqlStatement.constructPPreparedStatement();
    }

    /**
     * Constructs a new {@link PPreparedStatement} from this {@link SQLStatement}.
     *
     * @return A new {@link PPreparedStatement}.
     */
    public PPreparedStatement constructPPreparedStatement()
    {
        return new PPreparedStatement(variableCount, statement);
    }
}
