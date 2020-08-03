package nl.pim16aap2.bigdoors.storage;

import nl.pim16aap2.bigdoors.doortypes.DoorType;
import nl.pim16aap2.bigdoors.util.Util;
import org.jetbrains.annotations.NotNull;

/**
 * Represents an SQL statement.
 *
 * @author Pim
 */
public enum SQLStatement
{

//    UPDATE_DOOR_POWER_BLOCK_LOC(
//        "UPDATE DoorBase SET powerBlockX = ?, powerBlockY = ?, powerBlockZ = ?, \n" +
//            "chunkHash = ? WHERE id = ?;"),


    /*



    STATEMENTS FROM HERE ON OUT HAVE BEEN VERIFIED TO WORK WITH THE NEW SCHEMA.



     */
    UPDATE_DOOR_OWNER_PERMISSION(
        "UPDATE DoorOwnerPlayer SET permission = ? WHERE playerID = ? and doorUID = ?;"
    ),

    GET_DOOR_OWNER_PLAYER(
        "SELECT * FROM DoorOwnerPlayer WHERE playerID = ? AND doorUID = ?;"
    ),

    GET_DOOR_SPECIFIC_PLAYER(
        "SELECT D.*, P.playerUUID, P.playerName, U.permission \n" +
            "FROM DoorBase as D INNER JOIN DoorOwnerPlayer AS U ON U.doorUID = D.id INNER JOIN Player AS P ON P.id = U.playerID \n" +
            "WHERE P.playerUUID = ? AND D.id = ?;"
    ),

    DELETE_NAMED_DOOR_OF_PLAYER(
        "DELETE FROM DoorBase \n" +
            "WHERE DoorBase.id IN \n" +
            "      (SELECT D.id \n" +
            "       FROM DoorBase AS D INNER JOIN DoorOwnerPlayer AS U ON U.doorUID = D.id, \n" +
            "            (SELECT P.id FROM Player as P WHERE P.playerUUID = ?) AS R \n" +
            "      WHERE D.name = ? AND R.id = U.playerID);"
    ),

    UPDATE_DOOR_COORDS(
        "UPDATE DoorBase SET xMin = ?,yMin = ?,zMin = ?,xMax = ?\n" +
            ",yMax = ?,zMax = ? WHERE id = ?;"
    ),

    UPDATE_DOOR_OPEN_DIR(
        "UPDATE DoorBase SET openDirection = ? WHERE id = ?;"
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
            "(SELECT O.id \n" +
            "FROM DoorOwnerPlayer AS O INNER JOIN Player AS P on O.playerID = P.id \n" +
            "WHERE P.playerUUID = ? AND O.permission > '0' AND O.doorUID = ?);"
    ),

    GET_DOOR_FLAG(
        "SELECT bitflag FROM DoorBase WHERE id = ?;"
    ),

    /**
     * Adds a single flag to the bitflag column. Please only provide powers of two.
     */
    ADD_DOOR_FLAG(
        "UPDATE DoorBase SET bitflag = (bitflag | ?) WHERE id = ?;"
    ),

    /**
     * Removes a single flag to the bitflag column. Please only provide powers of two.
     */
    REMOVE_DOOR_FLAG(
        "UPDATE DoorBase SET bitflag = (bitflag &~ ?) WHERE id = ?;"
    ),

    GET_POWER_BLOCK_DATA_IN_CHUNK(
        "SELECT * FROM PowerBlock WHERE chunkHash = ?;"
    ),

    GET_DOOR_OWNER(
        "SELECT Player.*, DoorOwnerPlayer.doorUID, DoorOwnerPlayer.permission\n" +
            "FROM DoorOwnerPlayer \n" +
            "INNER JOIN Player ON Player.id = DoorOwnerPlayer.playerID\n" +
            "WHERE DoorOwnerPlayer.doorUID = ?;"
    ),

    GET_PLAYER_UUID(
        "SELECT P.playerUUID \n" +
            "FROM Player AS P \n" +
            "WHERE P.playerName = ?;"
    ),

    GET_PLAYER_NAME(
        "SELECT P.playerName \n" +
            "FROM Player AS P \n" +
            "WHERE P.playerUUID = ?;"
    ),

    UPDATE_PLAYER_NAME(
        "UPDATE Player \n" +
            "SET playerName = ? \n" +
            "WHERE playerUUID = ?;"
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
            "    INNER JOIN Player AS P on U.playerID = P.id \n" +
            "    INNER JOIN DoorBase AS D ON U.doorUID = D.id \n" +
            "WHERE P.playerUUID = ? AND D.name = ?;"
    ),

    GET_DOOR_COUNT_FOR_PLAYER(
        "SELECT COUNT(*) AS total \n" +
            "FROM DoorOwnerPlayer AS U INNER JOIN Player AS P on U.playerID = P.id \n" +
            "WHERE P.playerUUID = ?;"
    ),

    IS_BIGDOORS_WORLD(
        "SELECT World.id \n" +
            "FROM DoorBase \n" +
            "INNER JOIN World ON DoorBase.world = World.id \n" +
            "WHERE World.worldUUID = ? \n" +
            "LIMIT 1;"
    ),

    DELETE_DOOR(
        "DELETE FROM DoorBase WHERE id = ?;"
    ),

    INSERT_OR_IGNORE_PLAYER(
        "INSERT OR IGNORE INTO Player(playerUUID, playerName) VALUES(?, ?);"
    ),

    GET_PLAYER_ID(
        "SELECT id FROM Player WHERE playerUUID = ?;"
    ),

    GET_PLAYER_PERMISSION_OF_DOOR(
        "SELECT permission \n" +
            "FROM DoorOwnerPlayer INNER JOIN Player ON Player.id = DoorOwnerPlayer.playerID \n" +
            "WHERE Player.playerUUID = ? AND doorUID = ?;"
    ),

    GET_DOOR_BASE_FROM_ID(
        "SELECT DoorBase.*, Player.playerUUID, Player.playerName, DoorOwnerPlayer.permission, World.worldUUID, DoorType.typeTableName \n" +
            "    FROM DoorBase \n" +
            "    INNER JOIN DoorType ON DoorBase.doorType = DoorType.id \n" +
            "    INNER JOIN DoorOwnerPlayer ON DoorBase.id = DoorOwnerPlayer.doorUID \n" +
            "    INNER JOIN Player ON DoorOwnerPlayer.playerID = Player.id \n" +
            "    INNER JOIN World ON DoorBase.world = World.id\n" +
            "    WHERE DoorBase.id = ? AND DoorOwnerPlayer.permission = 0;"
    ),

    /**
     * Obtains the IDs of all doors whose engine's chunk hash value has a certain value.
     */
    GET_DOOR_IDS_IN_CHUNK(
        "SELECT DoorBase.id \n" +
            "    FROM DoorBase \n" +
            "    WHERE DoorBase.chunkHash = ?;"
    ),

    GET_DOOR_BASE_FROM_ID_FOR_PLAYER(
        "SELECT DoorBase.*, Player.playerUUID, Player.playerName, DoorOwnerPlayer.permission, World.worldUUID, DoorType.typeTableName \n" +
            "    FROM DoorBase \n" +
            "    INNER JOIN DoorType ON DoorBase.doorType = DoorType.id \n" +
            "    INNER JOIN DoorOwnerPlayer ON DoorBase.id = DoorOwnerPlayer.doorUID \n" +
            "    INNER JOIN Player ON DoorOwnerPlayer.playerID = Player.id \n" +
            "    INNER JOIN World ON DoorBase.world = World.id\n" +
            "    WHERE DoorBase.id = ? AND Player.playerUUID = ?;"
    ),

    GET_NAMED_DOORS_OWNED_BY_PLAYER(
        "SELECT DoorBase.*, Player.playerUUID, Player.playerName, DoorOwnerPlayer.permission, World.worldUUID, DoorType.typeTableName \n" +
            "    FROM DoorBase \n" +
            "    INNER JOIN DoorType ON DoorBase.doorType = DoorType.id \n" +
            "    INNER JOIN DoorOwnerPlayer ON DoorBase.id = DoorOwnerPlayer.doorUID \n" +
            "    INNER JOIN Player ON DoorOwnerPlayer.playerID = Player.id \n" +
            "    INNER JOIN World ON DoorBase.world = World.id\n" +
            "    WHERE Player.playerUUID = ? AND DoorBase.name = ? And DoorOwnerPlayer.permission <= ?;"
    ),

    GET_DOORS_WITH_NAME(
        "SELECT DoorBase.*, Player.playerUUID, Player.playerName, DoorOwnerPlayer.permission, World.worldUUID, DoorType.typeTableName \n" +
            "    FROM DoorBase \n" +
            "    INNER JOIN DoorType ON DoorBase.doorType = DoorType.id \n" +
            "    INNER JOIN DoorOwnerPlayer ON DoorBase.id = DoorOwnerPlayer.doorUID \n" +
            "    INNER JOIN Player ON DoorOwnerPlayer.playerID = Player.id \n" +
            "    INNER JOIN World ON DoorBase.world = World.id\n" +
            "    WHERE DoorBase.name = ? And DoorOwnerPlayer.permission = 0;"
    ),

    GET_DOORS_OWNED_BY_PLAYER_WITH_LEVEL(
        "SELECT DoorBase.*, Player.playerUUID, Player.playerName, DoorOwnerPlayer.permission, World.worldUUID, DoorType.typeTableName \n" +
            "    FROM DoorBase \n" +
            "    INNER JOIN DoorType ON DoorBase.doorType = DoorType.id \n" +
            "    INNER JOIN DoorOwnerPlayer ON DoorBase.id = DoorOwnerPlayer.doorUID \n" +
            "    INNER JOIN Player ON DoorOwnerPlayer.playerID = Player.id \n" +
            "    INNER JOIN World ON DoorBase.world = World.id\n" +
            "    WHERE Player.playerUUID = ? AND DoorOwnerPlayer.permission <= ?;"
    ),

    GET_TYPE_SPECIFIC_DATA(
        "SELECT TypeTable.id, TypeSpecificTable.*\n" +
            "    FROM ? AS TypeSpecificTable\n" +
            "    INNER JOIN \n" +
            "    (SELECT DoorType.id\n" +
            "         FROM DoorType\n" +
            "         WHERE DoorType.typeTableName = ?\n" +
            "    ) AS TypeTable\n" +
            "    WHERE TypeSpecificTable.doorUID = ?;"
    ),

    INSERT_OR_IGNORE_WORLD(
        "INSERT OR IGNORE INTO World(worldUUID) VALUES(?);"
    ),

    SELECT_WORLD_FROM_DATA(
        "SELECT * FROM World WHERE worldUUID = ?;"
    ),

    SELECT_WORLD_FROM_ID(
        "SELECT * FROM World WHERE id = ?;"
    ),

    /**
     * Insert the {@link DoorType} into the database if it doesn't exist already.
     */
    INSERT_OR_IGNORE_DOOR_TYPE(
        "INSERT OR IGNORE INTO DoorType(pluginName, typeName, typeVersion, typeTableName) VALUES(?,?,?,?);"
    ),

    /**
     * Deletes a DoorType. // TODO: Also delete the associated table.
     */
    DELETE_DOOR_TYPE(
        "DELETE FROM DoorType WHERE id = ?;"
    ),

    /**
     * Obtains both the typeTableName and the ID of an {@link DoorType}.
     */
    GET_DOOR_TYPE_TABLE_NAME_AND_ID(
        "SELECT id, typeTableName FROM DoorType WHERE pluginName = ? AND typeName = ? AND typeVersion = ?;"
    ),

    /**
     * Gets the typeTableName of an {@link DoorType}.
     */
    GET_DOOR_TYPE_TABLE_NAME(
        "SELECT typeTableName FROM DoorType WHERE pluginName = ? AND typeName = ? AND, typeVersion = ?;"
    ),

    /**
     * Obtains the ID value of the {@link DoorType} as described by its "typeTableName".
     */
    GET_DOOR_TYPE_ID(
        "SELECT id FROM DoorType WHERE typeTableName = \"?\";"
    ),

    GET_DOOR_OWNERS(
        "SELECT O.doorUID, O.permission, P.playerUUID, P.playerName \n" +
            "FROM DoorOwnerPlayer AS O INNER JOIN Player AS P ON O.playerID = P.id \n" +
            "WHERE doorUID = ?;"
    ),

    INSERT_DOOR_BASE(
        "INSERT INTO DoorBase\n" +
            "    (name, world, doorType, xMin, yMin, zMin, xMax, yMax, zMax, engineX, engineY, engineZ, bitflag, openDirection, chunkHash)\n" +
            "    VALUES (?,\n" +
            "    (SELECT id\n" +
            "     FROM World\n" +
            "     WHERE World.worldUUID = ?)\n" +
            "    , ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);"
    ),

    /**
     * Inserts a new door creator. This is a door owner with permission level 0.
     * <p>
     * This statement is intended to be used in the same transaction that inserted the DoorBase.
     */
    INSERT_DOOR_CREATOR(
        "INSERT INTO DoorOwnerPlayer\n" +
            "    (permission, playerID, doorUID)\n" +
            "    VALUES (0,\n" +
            "        (SELECT id\n" +
            "         FROM Player\n" +
            "         WHERE Player.playerUUID = ?),\n" +
            "        (SELECT seq\n" +
            "         FROM sqlite_sequence\n" +
            "         WHERE sqlite_sequence.name = \"DoorBase\"));"
    ),

    SELECT_MOST_RECENT_DOOR(
        "SELECT seq \n" +
            "    FROM sqlite_sequence \n" +
            "    WHERE sqlite_sequence.name = \"DoorBase\";"
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

    CREATE_TABLE_WORLD(
        "CREATE TABLE IF NOT EXISTS World\n" +
            "    (id             INTEGER    PRIMARY KEY AUTOINCREMENT,\n" +
            "     worldUUID      TEXT       NOT NULL,\n" +
            "     unique(worldUUID));"
    ),

    CREATE_TABLE_PLAYER(
        "CREATE TABLE IF NOT EXISTS Player\n" +
            "    (id             INTEGER    PRIMARY KEY AUTOINCREMENT,\n" +
            "     playerUUID     TEXT       NOT NULL,\n" +
            "     playerName     TEXT       NOT NULL,\n" +
            "     unique(playerUUID));"
    ),

    CREATE_TABLE_DOORTYPE(
        "CREATE TABLE IF NOT EXISTS DoorType\n" +
            "    (id             INTEGER    PRIMARY KEY AUTOINCREMENT,\n" +
            "     pluginName     TEXT       NOT NULL,\n" +
            "     typeName       TEXT       NOT NULL,\n" +
            "     typeVersion    INTEGER    NOT NULL,\n" +
            "     typeTableName  TEXT       NOT NULL,\n" +
            "     unique(pluginName, typeName, typeVersion), \n" +
            "     unique(typeTableName));"
    ),

    CREATE_TABLE_DOORBASE(
        "CREATE TABLE IF NOT EXISTS DoorBase\n" +
            "    (id             INTEGER    PRIMARY KEY AUTOINCREMENT,\n" +
            "     name           TEXT       NOT NULL,\n" +
            "     world          REFERENCES World(id)    ON UPDATE CASCADE ON DELETE CASCADE,\n" +
            "     doorType       REFERENCES DoorType(id) ON UPDATE CASCADE ON DELETE CASCADE,\n" +
            "     xMin           INTEGER    NOT NULL,\n" +
            "     yMin           INTEGER    NOT NULL,\n" +
            "     zMin           INTEGER    NOT NULL,\n" +
            "     xMax           INTEGER    NOT NULL,\n" +
            "     yMax           INTEGER    NOT NULL,\n" +
            "     zMax           INTEGER    NOT NULL,\n" +
            "     engineX        INTEGER    NOT NULL,\n" +
            "     engineY        INTEGER    NOT NULL,\n" +
            "     engineZ        INTEGER    NOT NULL,\n" +
            "     bitflag        INTEGER    NOT NULL,\n" +
            "     openDirection  INTEGER    NOT NULL,\n" +
            "     chunkHash      INTEGER    NOT NULL);"
    ),

    CREATE_TABLE_DOOROWNER_PLAYER(
        "CREATE TABLE IF NOT EXISTS DoorOwnerPlayer\n" +
            "    (id             INTEGER    PRIMARY KEY AUTOINCREMENT,\n" +
            "     permission     INTEGER    NOT NULL,\n" +
            "     playerID       REFERENCES Player(id)   ON UPDATE CASCADE ON DELETE CASCADE,\n" +
            "     doorUID        REFERENCES DoorBase(id) ON UPDATE CASCADE ON DELETE CASCADE,\n" +
            "     unique (playerID, doorUID));"
    ),

    CREATE_TABLE_POWERBLOCK(
        "CREATE TABLE IF NOT EXISTS PowerBlock\n" +
            "    (id             INTEGER    PRIMARY KEY AUTOINCREMENT,\n" +
            "     doorUID        REFERENCES DoorBase(id) ON UPDATE CASCADE ON DELETE CASCADE,\n" +
            "     x              INTEGER    NOT NULL,\n" +
            "     y              INTEGER    NOT NULL,\n" +
            "     z              INTEGER    NOT NULL,\n" +
            "     chunkHash      INTEGER    NOT NULL);"
    ),


    ;

    @NotNull
    final String statement;
    final int variableCount;

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
    @NotNull
    public static String getStatement(final @NotNull SQLStatement sqlStatement)
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
    public static PPreparedStatement constructPPreparedStatement(final @NotNull SQLStatement sqlStatement)
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
