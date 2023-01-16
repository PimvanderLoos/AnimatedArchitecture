package nl.pim16aap2.bigdoors.storage;

import nl.pim16aap2.bigdoors.util.Util;

/**
 * Represents an SQL statement.
 *
 * @author Pim
 */
@SuppressWarnings("unused")
public enum SQLStatement
{
    UPDATE_MOVABLE_BASE(
        """
        UPDATE DoorBase SET
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
        typeData       = ?
        WHERE id       = ?;
        """
    ),

    UPDATE_MOVABLE_OWNER_PERMISSION(
        "UPDATE DoorOwnerPlayer SET permission = ? WHERE playerID = ? and doorUID = ?;"
    ),

    GET_MOVABLE_OWNER_PLAYER(
        "SELECT * FROM DoorOwnerPlayer WHERE playerID = ? AND doorUID = ?;"
    ),

    DELETE_NAMED_MOVABLE_OF_PLAYER(
        """
        DELETE FROM DoorBase
        WHERE DoorBase.id IN
            (SELECT D.id
            FROM DoorBase AS D INNER JOIN DoorOwnerPlayer AS U ON U.doorUID = D.id,
                (SELECT P.id FROM Player as P WHERE P.playerUUID = ?) AS R
                WHERE D.name = ? AND R.id = U.playerID);
        """
    ),

    DELETE_MOVABLE_TYPE(
        "DELETE FROM DoorBase WHERE DoorBase.doorType = ?;"
    ),

    GET_LATEST_ROW_ADDITION(
        "SELECT last_insert_rowid() AS lastId;"
    ),

    INSERT_MOVABLE_OWNER(
        "INSERT INTO DoorOwnerPlayer (permission, playerID, doorUID) VALUES (?,?,?);"
    ),

    REMOVE_MOVABLE_OWNER(
        """
        DELETE
        FROM DoorOwnerPlayer
        WHERE DoorOwnerPlayer.id IN
            (SELECT O.id
            FROM DoorOwnerPlayer AS O INNER JOIN Player AS P on O.playerID = P.id
            WHERE P.playerUUID = ? AND O.permission > '0' AND O.doorUID = ?);
        """
    ),

    GET_POWER_BLOCK_DATA_IN_CHUNK(
        "SELECT id, powerBlockX, powerBlockY, powerBlockZ, powerBlockChunkId FROM DoorBase WHERE powerBlockChunkId = ?;"
    ),

    /**
     * Gets all the movables that have their <b>rotationPoint</b> in the chunk with the given chunk hash.
     */
    GET_MOVABLE_IN_CHUNK(
        "SELECT * FROM DoorBase WHERE rotationPointChunkId = ?;"
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
        SELECT D.id, D.name
        FROM DoorBase AS D
        INNER JOIN DoorOwnerPlayer AS O ON D.id = O.doorUID
        INNER JOIN Player AS P ON O.playerID = P.id
        WHERE D.name like ? || '%' AND O.permission <= ? AND (? IS NULL OR P.playerUUID IS ?)
        GROUP BY D.id;
        """
    ),

    GET_IDENTIFIERS_FROM_PARTIAL_UID_MATCH_WITH_OWNER(
        """
        SELECT D.id, D.name
        FROM DoorBase AS D
        INNER JOIN DoorOwnerPlayer AS O ON D.id = O.doorUID
        INNER JOIN Player AS P ON O.playerID = P.id
        WHERE D.id like ? || '%' AND O.permission <= ? AND (? IS NULL OR P.playerUUID IS ?)
        GROUP BY D.id;
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

    GET_OWNER_COUNT_OF_MOVABLE(
        "SELECT COUNT(*) AS total FROM DoorOwnerPlayer WHERE doorUID = ?;"
    ),

    GET_MOVABLE_COUNT_BY_NAME(
        "SELECT COUNT(*) AS total FROM DoorBase WHERE name = ?;"
    ),

    GET_PLAYER_MOVABLE_COUNT(
        """
        SELECT COUNT(*) AS total
        FROM DoorOwnerPlayer AS U
        INNER JOIN Player AS P on U.playerID = P.id
        INNER JOIN DoorBase AS D ON U.doorUID = D.id
        WHERE P.playerUUID = ? AND D.name = ?;
        """
    ),

    GET_MOVABLE_COUNT_FOR_PLAYER(
        """
        SELECT COUNT(*) AS total
        FROM DoorOwnerPlayer AS U INNER JOIN Player AS P on U.playerID = P.id
        WHERE P.playerUUID = ?;
        """
    ),

    IS_BIGDOORS_WORLD(
        """
        SELECT world
        FROM DoorBase
        WHERE world = ?
        LIMIT 1;
        """
    ),

    DELETE_MOVABLE(
        "DELETE FROM DoorBase WHERE id = ?;"
    ),

    GET_PLAYER_ID(
        "SELECT id FROM Player WHERE playerUUID = ?;"
    ),

    GET_MOVABLE_BASE_FROM_ID(
        """
        SELECT DoorBase.*, Player.*, DoorOwnerPlayer.permission
        FROM DoorBase
        INNER JOIN DoorOwnerPlayer ON DoorBase.id = DoorOwnerPlayer.doorUID
        INNER JOIN Player ON DoorOwnerPlayer.playerID = Player.id
        WHERE DoorBase.id = ? AND DoorOwnerPlayer.permission = 0;
        """
    ),

    /**
     * Obtains the IDs of all movables whose rotationPoint's chunk hash value has a certain value.
     */
    GET_MOVABLE_IDS_IN_CHUNK(
        """
        SELECT DoorBase.id
        FROM DoorBase
        WHERE DoorBase.rotationPointChunkId = ?;
        """
    ),

    GET_MOVABLE_BASE_FROM_ID_FOR_PLAYER(
        """
        SELECT DoorBase.*, Player.*, DoorOwnerPlayer.permission
        FROM DoorBase
        INNER JOIN DoorOwnerPlayer ON DoorBase.id = DoorOwnerPlayer.doorUID
        INNER JOIN Player ON DoorOwnerPlayer.playerID = Player.id
        WHERE DoorBase.id = ? AND Player.playerUUID = ?;
        """
    ),

    GET_NAMED_MOVABLES_OWNED_BY_PLAYER(
        """
        SELECT DoorBase.*, Player.*, DoorOwnerPlayer.permission
        FROM DoorBase
        INNER JOIN DoorOwnerPlayer ON DoorBase.id = DoorOwnerPlayer.doorUID
        INNER JOIN Player ON DoorOwnerPlayer.playerID = Player.id
        WHERE Player.playerUUID = ? AND DoorBase.name = ? And DoorOwnerPlayer.permission <= ?;
        """
    ),

    GET_MOVABLES_WITH_NAME(
        """
        SELECT DoorBase.*, Player.*, DoorOwnerPlayer.permission
        FROM DoorBase
        INNER JOIN DoorOwnerPlayer ON DoorBase.id = DoorOwnerPlayer.doorUID
        INNER JOIN Player ON DoorOwnerPlayer.playerID = Player.id
        WHERE DoorBase.name = ? And DoorOwnerPlayer.permission = 0;
        """
    ),

    GET_MOVABLES_OWNED_BY_PLAYER_WITH_LEVEL(
        """
        SELECT DoorBase.*, Player.*, DoorOwnerPlayer.permission
        FROM DoorBase
        INNER JOIN DoorOwnerPlayer ON DoorBase.id = DoorOwnerPlayer.doorUID
        INNER JOIN Player ON DoorOwnerPlayer.playerID = Player.id
        WHERE Player.playerUUID = ? AND DoorOwnerPlayer.permission <= ?;
        """
    ),

    GET_MOVABLE_OWNERS(
        """
        SELECT O.doorUID, O.permission, P.*
        FROM DoorOwnerPlayer AS O INNER JOIN Player AS P ON O.playerID = P.id
        WHERE doorUID = ?;
        """
    ),

    INSERT_MOVABLE_BASE(
        """
        INSERT INTO DoorBase
        (name, world, xMin, yMin, zMin, xMax, yMax, zMax, rotationPointX, rotationPointY, rotationPointZ,
         rotationPointChunkId, powerBlockX, powerBlockY, powerBlockZ, powerBlockChunkId, openDirection,
         bitflag, doorType, typeData)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);
        """
    ),

    /**
     * Inserts a new movable creator. This is a movable owner with permission level 0.
     * <p>
     * This statement is intended to be used in the same transaction that inserted the DoorBase.
     */
    INSERT_PRIME_OWNER(
        """
        INSERT INTO DoorOwnerPlayer (permission, playerID, doorUID)
        VALUES (0,
            (SELECT id
            FROM Player
            WHERE Player.playerUUID = ?),
            (SELECT seq
            FROM sqlite_sequence
            WHERE sqlite_sequence.name = "DoorBase"));
        """
    ),

    SELECT_MOST_RECENT_MOVABLE(
        """
        SELECT seq
        FROM sqlite_sequence
        WHERE sqlite_sequence.name = "DoorBase";
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

    RESERVE_IDS_PLAYER(
        "UPDATE SQLITE_SEQUENCE SET seq = 100 WHERE name = 'Player' and seq < 100;"
    ),

    RESERVE_IDS_MOVABLE(
        "UPDATE SQLITE_SEQUENCE SET seq = 100 WHERE name = 'DoorBase' and seq < 100;"
    ),

    RESERVE_IDS_MOVABLE_OWNER_PLAYER(
        "UPDATE SQLITE_SEQUENCE SET seq = 100 WHERE name = 'DoorOwnerPlayer' and seq < 100;"
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

    CREATE_TABLE_MOVABLE(
        """
        CREATE TABLE IF NOT EXISTS DoorBase
        (id               INTEGER    PRIMARY KEY AUTOINCREMENT,
        name              TEXT       NOT NULL,
        world             TEXT       NOT NULL,
        xMin              INTEGER    NOT NULL,
        yMin              INTEGER    NOT NULL,
        zMin              INTEGER    NOT NULL,
        xMax              INTEGER    NOT NULL,
        yMax              INTEGER    NOT NULL,
        zMax              INTEGER    NOT NULL,
        rotationPointX           INTEGER    NOT NULL,
        rotationPointY           INTEGER    NOT NULL,
        rotationPointZ           INTEGER    NOT NULL,
        rotationPointChunkId     INTEGER    NOT NULL,
        powerBlockX       INTEGER    NOT NULL,
        powerBlockY       INTEGER    NOT NULL,
        powerBlockZ       INTEGER    NOT NULL,
        powerBlockChunkId INTEGER    NOT NULL,
        openDirection     INTEGER    NOT NULL,
        doorType          TEXT       NOT NULL,
        typeData          BLOB       NOT NULL,
        bitflag           INTEGER    NOT NULL);
        """
    ),

    CREATE_TABLE_MOVABLE_OWNER_PLAYER(
        """
        CREATE TABLE IF NOT EXISTS DoorOwnerPlayer
        (id            INTEGER    PRIMARY KEY AUTOINCREMENT,
        permission     INTEGER    NOT NULL,
        playerID       REFERENCES Player(id)   ON UPDATE CASCADE ON DELETE CASCADE,
        doorUID        REFERENCES DoorBase(id) ON UPDATE CASCADE ON DELETE CASCADE,
        unique (playerID, doorUID));
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
