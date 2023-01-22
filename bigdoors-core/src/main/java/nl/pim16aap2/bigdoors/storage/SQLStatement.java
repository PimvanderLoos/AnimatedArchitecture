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
        UPDATE Movables SET
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
        "UPDATE MovableOwnerPlayer SET permission = ? WHERE playerID = ? and movableUID = ?;"
    ),

    GET_MOVABLE_OWNER_PLAYER(
        "SELECT * FROM MovableOwnerPlayer WHERE playerID = ? AND movableUID = ?;"
    ),

    DELETE_NAMED_MOVABLE_OF_PLAYER(
        """
        DELETE FROM Movables
        WHERE Movables.id IN
            (SELECT D.id
            FROM Movables AS D INNER JOIN MovableOwnerPlayer AS U ON U.movableUID = D.id,
                (SELECT P.id FROM Player as P WHERE P.playerUUID = ?) AS R
                WHERE D.name = ? AND R.id = U.playerID);
        """
    ),

    DELETE_MOVABLE_TYPE(
        "DELETE FROM Movables WHERE Movables.movableType = ?;"
    ),

    GET_LATEST_ROW_ADDITION(
        "SELECT last_insert_rowid() AS lastId;"
    ),

    INSERT_MOVABLE_OWNER(
        "INSERT INTO MovableOwnerPlayer (permission, playerID, movableUID) VALUES (?,?,?);"
    ),

    REMOVE_MOVABLE_OWNER(
        """
        DELETE
        FROM MovableOwnerPlayer
        WHERE MovableOwnerPlayer.id IN
            (SELECT O.id
            FROM MovableOwnerPlayer AS O INNER JOIN Player AS P on O.playerID = P.id
            WHERE P.playerUUID = ? AND O.permission > '0' AND O.movableUID = ?);
        """
    ),

    GET_POWER_BLOCK_DATA_IN_CHUNK(
        "SELECT id, powerBlockX, powerBlockY, powerBlockZ, powerBlockChunkId FROM Movables WHERE powerBlockChunkId = ?;"
    ),

    /**
     * Gets all the movables that have their <b>rotationPoint</b> in the chunk with the given chunk hash.
     */
    GET_MOVABLE_IN_CHUNK(
        "SELECT * FROM Movables WHERE rotationPointChunkId = ?;"
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
        FROM Movables AS D
        INNER JOIN MovableOwnerPlayer AS O ON D.id = O.movableUID
        INNER JOIN Player AS P ON O.playerID = P.id
        WHERE D.name like ? || '%' AND O.permission <= ? AND (? IS NULL OR P.playerUUID IS ?)
        GROUP BY D.id;
        """
    ),

    GET_IDENTIFIERS_FROM_PARTIAL_UID_MATCH_WITH_OWNER(
        """
        SELECT D.id, D.name
        FROM Movables AS D
        INNER JOIN MovableOwnerPlayer AS O ON D.id = O.movableUID
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
        "SELECT COUNT(*) AS total FROM MovableOwnerPlayer WHERE movableUID = ?;"
    ),

    GET_MOVABLE_COUNT_BY_NAME(
        "SELECT COUNT(*) AS total FROM Movables WHERE name = ?;"
    ),

    GET_PLAYER_MOVABLE_COUNT(
        """
        SELECT COUNT(*) AS total
        FROM MovableOwnerPlayer AS U
        INNER JOIN Player AS P on U.playerID = P.id
        INNER JOIN Movables AS D ON U.movableUID = D.id
        WHERE P.playerUUID = ? AND D.name = ?;
        """
    ),

    GET_MOVABLE_COUNT_FOR_PLAYER(
        """
        SELECT COUNT(*) AS total
        FROM MovableOwnerPlayer AS U INNER JOIN Player AS P on U.playerID = P.id
        WHERE P.playerUUID = ?;
        """
    ),

    IS_BIGDOORS_WORLD(
        """
        SELECT world
        FROM Movables
        WHERE world = ?
        LIMIT 1;
        """
    ),

    DELETE_MOVABLE(
        "DELETE FROM Movables WHERE id = ?;"
    ),

    GET_PLAYER_ID(
        "SELECT id FROM Player WHERE playerUUID = ?;"
    ),

    GET_MOVABLE_BASE_FROM_ID(
        """
        SELECT Movables.*, Player.*, MovableOwnerPlayer.permission
        FROM Movables
        INNER JOIN MovableOwnerPlayer ON Movables.id = MovableOwnerPlayer.movableUID
        INNER JOIN Player ON MovableOwnerPlayer.playerID = Player.id
        WHERE Movables.id = ? AND MovableOwnerPlayer.permission = 0;
        """
    ),

    /**
     * Obtains the movables whose rotationPoint's chunk hash value has a certain value.
     */
    GET_MOVABLES_IN_CHUNK(
        """
        SELECT Movables.*, Player.*, MovableOwnerPlayer.permission
        FROM Movables
        INNER JOIN MovableOwnerPlayer ON Movables.id = MovableOwnerPlayer.movableUID
        INNER JOIN Player ON MovableOwnerPlayer.playerID = Player.id
        WHERE Movables.rotationPointChunkId = ?;
        """
    ),

    GET_MOVABLE_BASE_FROM_ID_FOR_PLAYER(
        """
        SELECT Movables.*, Player.*, MovableOwnerPlayer.permission
        FROM Movables
        INNER JOIN MovableOwnerPlayer ON Movables.id = MovableOwnerPlayer.movableUID
        INNER JOIN Player ON MovableOwnerPlayer.playerID = Player.id
        WHERE Movables.id = ? AND Player.playerUUID = ?;
        """
    ),

    GET_NAMED_MOVABLES_OWNED_BY_PLAYER(
        """
        SELECT Movables.*, Player.*, MovableOwnerPlayer.permission
        FROM Movables
        INNER JOIN MovableOwnerPlayer ON Movables.id = MovableOwnerPlayer.movableUID
        INNER JOIN Player ON MovableOwnerPlayer.playerID = Player.id
        WHERE Player.playerUUID = ? AND Movables.name = ? And MovableOwnerPlayer.permission <= ?;
        """
    ),

    GET_MOVABLES_WITH_NAME(
        """
        SELECT Movables.*, Player.*, MovableOwnerPlayer.permission
        FROM Movables
        INNER JOIN MovableOwnerPlayer ON Movables.id = MovableOwnerPlayer.movableUID
        INNER JOIN Player ON MovableOwnerPlayer.playerID = Player.id
        WHERE Movables.name = ? And MovableOwnerPlayer.permission = 0;
        """
    ),

    GET_MOVABLES_OWNED_BY_PLAYER_WITH_LEVEL(
        """
        SELECT Movables.*, Player.*, MovableOwnerPlayer.permission
        FROM Movables
        INNER JOIN MovableOwnerPlayer ON Movables.id = MovableOwnerPlayer.movableUID
        INNER JOIN Player ON MovableOwnerPlayer.playerID = Player.id
        WHERE Player.playerUUID = ? AND MovableOwnerPlayer.permission <= ?;
        """
    ),

    GET_MOVABLE_OWNERS(
        """
        SELECT O.movableUID, O.permission, P.*
        FROM MovableOwnerPlayer AS O INNER JOIN Player AS P ON O.playerID = P.id
        WHERE movableUID = ?;
        """
    ),

    INSERT_MOVABLE_BASE(
        """
        INSERT INTO Movables
        (name, world, xMin, yMin, zMin, xMax, yMax, zMax, rotationPointX, rotationPointY, rotationPointZ,
         rotationPointChunkId, powerBlockX, powerBlockY, powerBlockZ, powerBlockChunkId, openDirection,
         bitflag, movableType, typeData)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);
        """
    ),

    /**
     * Inserts a new movable creator. This is a movable owner with permission level 0.
     * <p>
     * This statement is intended to be used in the same transaction that inserted the Movables.
     */
    INSERT_PRIME_OWNER(
        """
        INSERT INTO MovableOwnerPlayer (permission, playerID, movableUID)
        VALUES (0,
            (SELECT id
            FROM Player
            WHERE Player.playerUUID = ?),
            (SELECT seq
            FROM sqlite_sequence
            WHERE sqlite_sequence.name = "Movables"));
        """
    ),

    SELECT_MOST_RECENT_MOVABLE(
        """
        SELECT seq
        FROM sqlite_sequence
        WHERE sqlite_sequence.name = "Movables";
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
        "UPDATE SQLITE_SEQUENCE SET seq = 100 WHERE name = 'Movables' and seq < 100;"
    ),

    RESERVE_IDS_MOVABLE_OWNER_PLAYER(
        "UPDATE SQLITE_SEQUENCE SET seq = 100 WHERE name = 'MovableOwnerPlayer' and seq < 100;"
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
        CREATE TABLE IF NOT EXISTS Movables
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
        movableType           TEXT       NOT NULL,
        typeData              BLOB       NOT NULL,
        bitflag               INTEGER    NOT NULL);
        """
    ),

    CREATE_TABLE_MOVABLE_OWNER_PLAYER(
        """
        CREATE TABLE IF NOT EXISTS MovableOwnerPlayer
        (id          INTEGER    PRIMARY KEY AUTOINCREMENT,
        permission   INTEGER    NOT NULL,
        playerID     REFERENCES Player(id)   ON UPDATE CASCADE ON DELETE CASCADE,
        movableUID   REFERENCES Movables(id) ON UPDATE CASCADE ON DELETE CASCADE,
        unique (playerID, movableUID));
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
