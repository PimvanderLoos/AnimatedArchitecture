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
    GET_OR_CREATE_PLAYER(
        "INSERT OR IGNORE INTO players(playerUUID, playerName) VALUES(?, ?)"),

    GET_PLAYER_ID(
        "SELECT id FROM players WHERE playerUUID = ?;"),

    UPDATE_DOOR_OWNER_PERMISSION(
        "UPDATE sqlUnion SET permission=? WHERE playerID=? and doorUID=?;"),

    GET_UNION_FROM_OWNER(
        "SELECT * FROM sqlUnion WHERE playerID=? AND doorUID=?;"),

    GET_DOORS_WITH_NAME(
        "SELECT *\n" +
            "FROM doors AS D INNER JOIN sqlUnion as U ON D.id = U.doorUID INNER JOIN players AS P ON U.playerID = P.id\n" +
            "WHERE name=? AND U.permission = \"0\";"),

    IS_BIGDOORS_WORLD(
        "SELECT world FROM doors WHERE world=? LIMIT 1;"),

    GET_DOOR_COUNT_FOR_PLAYER(
        "SELECT COUNT(*) AS total \n" +
            "FROM sqlUnion AS U INNER JOIN players AS P on U.playerID = P.id \n" +
            "WHERE P.playerUUID=?;"),

    GET_PLAYER_DOOR_COUNT(
        "SELECT COUNT(*) AS total \n" +
            "FROM sqlUnion AS U \n" +
            "    INNER JOIN players AS P on U.playerID = P.id \n" +
            "    INNER JOIN doors AS D ON U.doorUID = D.id \n" +
            "WHERE P.playerUUID=? AND D.name=?;"),

    GET_DOOR_COUNT_BY_NAME(
        "SELECT COUNT(*) AS total FROM doors WHERE name=?;"),

    GET_OWNER_COUNT_OF_DOOR(
        "SELECT COUNT(*) AS total FROM sqlUnion WHERE doorUID=?;"),

    GET_DOOR_SPECIFIC_PLAYER(
        "SELECT D.*, P.playerUUID, P.playerName, U.permission \n" +
            "FROM doors as D INNER JOIN sqlUnion AS U ON U.doorUID = D.id INNER JOIN players AS P ON P.id = U.playerID \n" +
            "WHERE P.playerUUID = ? AND D.id = ?;"),

    GET_DOOR_FROM_UID(
        "SELECT * FROM doors WHERE id=?;"),

    GET_NAMED_DOORS_OWNED_BY_PLAYER(
        "SELECT D.*, P.playerName, P.playerUUID, U.permission \n" +
            "FROM sqlUnion AS U INNER JOIN players AS P ON U.playerID = P.id INNER JOIN doors AS D ON U.doorUID = D.id \n" +
            "WHERE P.playerUUID = ? AND D.name = ? AND U.permission <= ?;"),

    GET_DOORS_OWNED_BY_PLAYER_WITH_LEVEL(
        "SELECT D.*, P.playerUUID, P.playerName, U.permission \n" +
            "FROM doors as D INNER JOIN sqlUnion AS U ON U.doorUID = D.id INNER JOIN players AS P ON P.id = U.playerID \n" +
            "WHERE P.playerUUID = ? AND permission <= ?;"),

    UPDATE_PLAYER_NAME(
        "UPDATE players " +
            "SET playerName=? " +
            "WHERE playerUUID=?;"),

    GET_PLAYER_UUID(
        "SELECT P.playerUUID " +
            "FROM players AS P " +
            "WHERE P.playerName=?;"),

    GET_PLAYER_NAME(
        "SELECT P.playerName " +
            "FROM players AS P " +
            "WHERE P.playerUUID=?;"),

    GET_POWER_BLOCK_DATA(
        "SELECT * FROM doors WHERE chunkHash=?;"),

    GET_DOORS_IN_CHUNK(
        "SELECT * FROM doors WHERE engineChunkHash=?;"),

    UPDATE_BLOCKS_TO_MOVE(
        "UPDATE doors SET blocksToMove=? WHERE id=?;"),

    GET_PLAYER_PERMISSION_OF_DOOR(
        "SELECT permission \n" +
            "FROM sqlUnion INNER JOIN players ON players.id = sqlUnion.playerID \n" +
            "WHERE players.playerUUID = ? AND doorUID = ?;"),

    DELETE_DOOR(
        "DELETE FROM doors WHERE id = ?;"),

    DELETE_NAMED_DOOR_OF_PLAYER(
        "DELETE FROM doors \n" +
            "WHERE doors.id IN \n" +
            "      (SELECT D.id \n" +
            "       FROM doors AS D INNER JOIN sqlUnion AS U ON U.doorUID = D.id, \n" +
            "            (SELECT P.id FROM players as P WHERE P.playerUUID=?) AS R \n" +
            "      WHERE D.name=? AND R.id = U.playerID);"),

    UPDATE_DOOR_COORDS(
        "UPDATE doors SET xMin=?,yMin=?,zMin=?,xMax=?" +
            ",yMax=?,zMax=? WHERE id =?;"),

    UPDATE_DOOR_AUTO_CLOSE(
        "UPDATE doors SET autoClose=? WHERE id=?;"),

    UPDATE_DOOR_OPEN_DIR(
        "UPDATE doors SET openDirection=? WHERE id=?;"),

    UPDATE_DOOR_POWER_BLOCK_LOC(
        "UPDATE doors SET powerBlockX=?, powerBlockY=?, powerBlockZ=?, " +
            "chunkHash=? WHERE id=?;"),

    GET_DOOR_FLAG(
        "SELECT bitflag FROM doors WHERE id=?;"),

    UPDATE_DOOR_FLAG(
        "UPDATE doors SET bitflag=? WHERE id=?;"),

    INSERT_DOOR(
        "INSERT INTO doors(name,world,xMin,yMin,zMin,xMax,yMax,zMax, \n" +
            "                  engineX,engineY,engineZ,bitflag,type, \n" +
            "                  powerBlockX,powerBlockY,powerBlockZ,openDirection, \n" +
            "                  autoClose,chunkHash,blocksToMove) \n" +
            "                  VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?);"),

    GET_LATEST_ROW_ADDITION(
        "SELECT last_insert_rowid() AS lastId"),

    INSERT_SQL_UNION(
        "INSERT INTO sqlUnion (permission, playerID, doorUID) VALUES (?,?,?);"),

    REMOVE_DOOR_OWNER(
        "DELETE " +
            "FROM sqlUnion " +
            "WHERE sqlUnion.id IN " +
            "(SELECT U.id " +
            "FROM sqlUnion AS U INNER JOIN players AS P on U.playerID=P.id " +
            "WHERE P.playerUUID=? AND U.permission > '0' AND U.doorUID=?);"),

    GET_DOOR_OWNERS(
        "SELECT U.doorUID, U.permission, P.playerUUID, P.playerName " +
            "FROM sqlUnion AS U INNER JOIN players AS P ON U.playerID = P.id " +
            "WHERE doorUID=?;"),

    LEGACY_ALTER_TABLE_ON(
        "PRAGMA legacy_alter_table=ON"
    ),

    LEGACY_ALTER_TABLE_OFF(
        "PRAGMA legacy_alter_table=OFF"
    ),

    FOREIGN_KEYS_ON(
        "PRAGMA foreign_keys=OFF"
    ),

    FOREIGN_KEYS_OFF(
        "PRAGMA foreign_keys=ON"
    ),
    ;

    @NotNull
    final String statement;
    final int variableCount;

    SQLStatement(final @NotNull String statement)
    {
        this.statement = statement;
        variableCount = Util.getQuestionMarkCount(statement);
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
