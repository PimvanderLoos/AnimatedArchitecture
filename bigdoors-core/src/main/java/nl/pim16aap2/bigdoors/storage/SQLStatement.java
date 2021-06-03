package nl.pim16aap2.bigdoors.storage;

import nl.pim16aap2.bigdoors.util.Util;
import org.jetbrains.annotations.NotNull;

/**
 * Represents an SQL statement.
 *
 * @author Pim
 */
public enum SQLStatement
{
    UPDATE_DOOR_BASE(
        "UPDATE DoorBase SET\n" +
            "name           = ?,\n" +
            "world          = ?,\n" +
            "xMin           = ?,\n" +
            "yMin           = ?,\n" +
            "zMin           = ?,\n" +
            "xMax           = ?,\n" +
            "yMax           = ?,\n" +
            "zMax           = ?,\n" +
            "engineX        = ?,\n" +
            "engineY        = ?,\n" +
            "engineZ        = ?,\n" +
            "engineHash     = ?,\n" +
            "powerBlockX    = ?,\n" +
            "powerBlockY    = ?,\n" +
            "powerBlockZ    = ?,\n" +
            "powerBlockHash = ?,\n" +
            "openDirection  = ?,\n" +
            "bitflag        = ?,\n" +
            "typeData       = ? \n" +
            "WHERE id = ?;"
    ),

    UPDATE_DOOR_OWNER_PERMISSION(
        "UPDATE DoorOwnerPlayer SET permission = ? WHERE playerID = ? and doorUID = ?;"
    ),

    GET_DOOR_OWNER_PLAYER(
        "SELECT * FROM DoorOwnerPlayer WHERE playerID = ? AND doorUID = ?;"
    ),

    DELETE_NAMED_DOOR_OF_PLAYER(
        "DELETE FROM DoorBase \n" +
            "WHERE DoorBase.id IN \n" +
            "    (SELECT D.id \n" +
            "    FROM DoorBase AS D INNER JOIN DoorOwnerPlayer AS U ON U.doorUID = D.id, \n" +
            "        (SELECT P.id FROM Player as P WHERE P.playerUUID = ?) AS R \n" +
            "        WHERE D.name = ? AND R.id = U.playerID);"
    ),

    DELETE_DOOR_TYPE(
        "DELETE FROM DoorBase WHERE DoorBase.doorType = ?;"
    ),

    GET_LATEST_ROW_ADDITION(
        "SELECT last_insert_rowid() AS lastId;"
    ),

    INSERT_DOOR_OWNER(
        "INSERT INTO DoorOwnerPlayer (permission, playerID, doorUID) VALUES (?,?,?);"
    ),

    REMOVE_DOOR_OWNER(
        "DELETE \n" +
            "FROM DoorOwnerPlayer \n" +
            "WHERE DoorOwnerPlayer.id IN \n" +
            "    (SELECT O.id \n" +
            "    FROM DoorOwnerPlayer AS O INNER JOIN Player AS P on O.playerID = P.id \n" +
            "    WHERE P.playerUUID = ? AND O.permission > '0' AND O.doorUID = ?);"
    ),

    GET_POWER_BLOCK_DATA_IN_CHUNK(
        "SELECT id, powerBlockX, powerBlockY, powerBlockZ, powerBlockHash FROM DoorBase WHERE powerBlockHash = ?;"
    ),

    /**
     * Gets all the doors that have their <b>engine</b> in the chunk with the given chunk hash.
     */
    GET_DOORS_IN_CHUNK(
        "SELECT * FROM DoorBase WHERE engineHash = ?;"
    ),

    INSERT_OR_IGNORE_PLAYER_DATA(
        "INSERT OR IGNORE INTO Player\n" +
            "(playerUUID, playerName, sizeLimit, countLimit, permissions)\n" +
            "VALUES(?, ?, ?, ?, ?);"
    ),

    UPDATE_PLAYER_DATA(
        "UPDATE Player SET \n" +
            "playerName       = ?,\n" +
            "sizeLimit        = ?,\n" +
            "countLimit       = ?,\n" +
            "permissions      = ?\n" +
            "WHERE playerUUID = ?;"
    ),

    GET_PLAYER_DATA(
        "SELECT * FROM Player WHERE playerUUID = ?;"
    ),

    GET_PLAYER_DATA_FROM_NAME(
        "SELECT * FROM Player WHERE playerName = ?;"
    ),

    GET_OWNER_COUNT_OF_DOOR(
        "SELECT COUNT(*) AS total FROM DoorOwnerPlayer WHERE doorUID = ?;"
    ),

    GET_DOOR_COUNT_BY_NAME(
        "SELECT COUNT(*) AS total FROM DoorBase WHERE name = ?;"
    ),

    GET_PLAYER_DOOR_COUNT(
        "SELECT COUNT(*) AS total \n" +
            "FROM DoorOwnerPlayer AS U \n" +
            "INNER JOIN Player AS P on U.playerID = P.id \n" +
            "INNER JOIN DoorBase AS D ON U.doorUID = D.id \n" +
            "WHERE P.playerUUID = ? AND D.name = ?;"
    ),

    GET_DOOR_COUNT_FOR_PLAYER(
        "SELECT COUNT(*) AS total \n" +
            "FROM DoorOwnerPlayer AS U INNER JOIN Player AS P on U.playerID = P.id \n" +
            "WHERE P.playerUUID = ?;"
    ),

    IS_BIGDOORS_WORLD(
        "SELECT world \n" +
            "FROM DoorBase \n" +
            "WHERE world = ? \n" +
            "LIMIT 1;\n"
    ),

    DELETE_DOOR(
        "DELETE FROM DoorBase WHERE id = ?;"
    ),

    GET_PLAYER_ID(
        "SELECT id FROM Player WHERE playerUUID = ?;"
    ),

    GET_DOOR_BASE_FROM_ID(
        "SELECT DoorBase.*, Player.*, DoorOwnerPlayer.permission\n" +
            "FROM DoorBase \n" +
            "INNER JOIN DoorOwnerPlayer ON DoorBase.id = DoorOwnerPlayer.doorUID \n" +
            "INNER JOIN Player ON DoorOwnerPlayer.playerID = Player.id \n" +
            "WHERE DoorBase.id = ? AND DoorOwnerPlayer.permission = 0;"
    ),

    /**
     * Obtains the IDs of all doors whose engine's chunk hash value has a certain value.
     */
    GET_DOOR_IDS_IN_CHUNK(
        "SELECT DoorBase.id \n" +
            "FROM DoorBase \n" +
            "WHERE DoorBase.engineHash = ?;"
    ),

    GET_DOOR_BASE_FROM_ID_FOR_PLAYER(
        "SELECT DoorBase.*, Player.*, DoorOwnerPlayer.permission \n" +
            "FROM DoorBase \n" +
            "INNER JOIN DoorOwnerPlayer ON DoorBase.id = DoorOwnerPlayer.doorUID \n" +
            "INNER JOIN Player ON DoorOwnerPlayer.playerID = Player.id \n" +
            "WHERE DoorBase.id = ? AND Player.playerUUID = ?;"
    ),

    GET_NAMED_DOORS_OWNED_BY_PLAYER(
        "SELECT DoorBase.*, Player.*, DoorOwnerPlayer.permission \n" +
            "FROM DoorBase \n" +
            "INNER JOIN DoorOwnerPlayer ON DoorBase.id = DoorOwnerPlayer.doorUID \n" +
            "INNER JOIN Player ON DoorOwnerPlayer.playerID = Player.id \n" +
            "WHERE Player.playerUUID = ? AND DoorBase.name = ? And DoorOwnerPlayer.permission <= ?;"
    ),

    GET_DOORS_WITH_NAME(
        "SELECT DoorBase.*, Player.*, DoorOwnerPlayer.permission \n" +
            "FROM DoorBase \n" +
            "INNER JOIN DoorOwnerPlayer ON DoorBase.id = DoorOwnerPlayer.doorUID \n" +
            "INNER JOIN Player ON DoorOwnerPlayer.playerID = Player.id \n" +
            "WHERE DoorBase.name = ? And DoorOwnerPlayer.permission = 0;"
    ),

    GET_DOORS_OWNED_BY_PLAYER_WITH_LEVEL(
        "SELECT DoorBase.*, Player.*, DoorOwnerPlayer.permission \n" +
            "FROM DoorBase \n" +
            "INNER JOIN DoorOwnerPlayer ON DoorBase.id = DoorOwnerPlayer.doorUID \n" +
            "INNER JOIN Player ON DoorOwnerPlayer.playerID = Player.id \n" +
            "WHERE Player.playerUUID = ? AND DoorOwnerPlayer.permission <= ?;"
    ),

    GET_DOOR_OWNERS(
        "SELECT O.doorUID, O.permission, P.* \n" +
            "FROM DoorOwnerPlayer AS O INNER JOIN Player AS P ON O.playerID = P.id \n" +
            "WHERE doorUID = ?;"
    ),

    INSERT_DOOR_BASE(
        "INSERT INTO DoorBase\n" +
            "(name, world, xMin, yMin, zMin, xMax, yMax, zMax, engineX, engineY, engineZ, engineHash, " +
            "powerBlockX, powerBlockY, powerBlockZ, powerBlockHash, openDirection, bitflag, doorType, typeData)\n" +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);"
    ),

    /**
     * Inserts a new door creator. This is a door owner with permission level 0.
     * <p>
     * This statement is intended to be used in the same transaction that inserted the DoorBase.
     */
    INSERT_PRIME_OWNER(
        "INSERT INTO DoorOwnerPlayer\n" +
            "(permission, playerID, doorUID)\n" +
            "VALUES (0,\n" +
            "    (SELECT id\n" +
            "    FROM Player\n" +
            "    WHERE Player.playerUUID = ?),\n" +
            "    (SELECT seq\n" +
            "    FROM sqlite_sequence\n" +
            "    WHERE sqlite_sequence.name = \"DoorBase\"));"
    ),

    SELECT_MOST_RECENT_DOOR(
        "SELECT seq \n" +
            "FROM sqlite_sequence \n" +
            "WHERE sqlite_sequence.name = \"DoorBase\";"
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

    CREATE_TABLE_PLAYER(
        "CREATE TABLE IF NOT EXISTS Player\n" +
            "(id             INTEGER    PRIMARY KEY AUTOINCREMENT,\n" +
            " playerUUID     TEXT       NOT NULL,\n" +
            " playerName     TEXT       NOT NULL,\n" +
            " sizeLimit      INTEGER    NOT NULL,\n" +
            " countLimit     INTEGER    NOT NULL,\n" +
            " permissions    INTEGER    NOT NULL,\n" +
            " unique(playerUUID));"
    ),

    CREATE_TABLE_DOORBASE(
        "CREATE TABLE IF NOT EXISTS DoorBase\n" +
            "(id             INTEGER    PRIMARY KEY AUTOINCREMENT,\n" +
            " name           TEXT       NOT NULL,\n" +
            " world          TEXT       NOT NULL,\n" +
            " xMin           INTEGER    NOT NULL,\n" +
            " yMin           INTEGER    NOT NULL,\n" +
            " zMin           INTEGER    NOT NULL,\n" +
            " xMax           INTEGER    NOT NULL,\n" +
            " yMax           INTEGER    NOT NULL,\n" +
            " zMax           INTEGER    NOT NULL,\n" +
            " engineX        INTEGER    NOT NULL,\n" +
            " engineY        INTEGER    NOT NULL,\n" +
            " engineZ        INTEGER    NOT NULL,\n" +
            " engineHash     INTEGER    NOT NULL,\n" +
            " powerBlockX    INTEGER    NOT NULL,\n" +
            " powerBlockY    INTEGER    NOT NULL,\n" +
            " powerBlockZ    INTEGER    NOT NULL,\n" +
            " powerBlockHash INTEGER    NOT NULL,\n" +
            " openDirection  INTEGER    NOT NULL,\n" +
            " doorType       TEXT       NOT NULL,\n" +
            " typeData       BLOB       NOT NULL,\n" +
            " bitflag        INTEGER    NOT NULL);"
    ),

    CREATE_TABLE_DOOROWNER_PLAYER(
        "CREATE TABLE IF NOT EXISTS DoorOwnerPlayer\n" +
            "(id             INTEGER    PRIMARY KEY AUTOINCREMENT,\n" +
            " permission     INTEGER    NOT NULL,\n" +
            " playerID       REFERENCES Player(id)   ON UPDATE CASCADE ON DELETE CASCADE,\n" +
            " doorUID        REFERENCES DoorBase(id) ON UPDATE CASCADE ON DELETE CASCADE,\n" +
            " unique (playerID, doorUID));"
    ),


    ;

    private final @NotNull String statement;
    private final int variableCount;

    SQLStatement(final @NotNull String statement)
    {
        this.statement = statement;
        variableCount = Util.countPatternOccurrences(PPreparedStatement.QUESTION_MARK, statement);
    }

    /**
     * Gets the statement this {@link SQLStatement} represents.
     *
     * @param sqlStatement The {@link SQLStatement}.
     * @return The statement this {@link SQLStatement} represents.
     */
    public static @NotNull String getStatement(final @NotNull SQLStatement sqlStatement)
    {
        return sqlStatement.statement;
    }

    /**
     * Gets the number of variables in the statement.
     *
     * @param sqlStatement The statement.
     * @return The number of variables in the statement.
     */
    public static int getVariableCountCount(final @NotNull SQLStatement sqlStatement)
    {
        return sqlStatement.variableCount;
    }

    /**
     * Constructs a new {@link PPreparedStatement} from the {@link SQLStatement}.
     *
     * @param sqlStatement The {@link SQLStatement}.
     * @return A new {@link PPreparedStatement}.
     */
    public static @NotNull PPreparedStatement constructPPreparedStatement(final @NotNull SQLStatement sqlStatement)
    {
        return sqlStatement.constructPPreparedStatement();
    }

    /**
     * Constructs a new {@link PPreparedStatement} from this {@link SQLStatement}.
     *
     * @return A new {@link PPreparedStatement}.
     */
    public @NotNull PPreparedStatement constructPPreparedStatement()
    {
        return new PPreparedStatement(variableCount, statement);
    }
}
