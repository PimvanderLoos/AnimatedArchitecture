package nl.pim16aap2.bigdoors.storage.sqlite;

import nl.pim16aap2.bigdoors.config.ConfigLoader;
import nl.pim16aap2.bigdoors.doors.DoorBase;
import nl.pim16aap2.bigdoors.doors.DoorType;
import nl.pim16aap2.bigdoors.exceptions.TooManyDoorsException;
import nl.pim16aap2.bigdoors.spigotutil.PlayerRetriever;
import nl.pim16aap2.bigdoors.spigotutil.WorldRetriever;
import nl.pim16aap2.bigdoors.storage.IStorage;
import nl.pim16aap2.bigdoors.util.DoorOwner;
import nl.pim16aap2.bigdoors.util.PBlockFace;
import nl.pim16aap2.bigdoors.util.PLogger;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import nl.pim16aap2.bigdoors.util.Util;
import org.bukkit.Location;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * SQLite implementation of {@link IStorage}.
 *
 * @author Pim
 */
public class SQLiteJDBCDriverConnection implements IStorage
{
    private static final String DRIVER = "org.sqlite.JDBC";
    private static final int DATABASE_VERSION = 6;
    private static final int DOOR_ID = 1;
    private static final int DOOR_NAME = 2;
    private static final int DOOR_WORLD = 3;
    private static final int DOOR_OPEN = 4;
    private static final int DOOR_MIN_X = 5;
    private static final int DOOR_MIN_Y = 6;
    private static final int DOOR_MIN_Z = 7;
    private static final int DOOR_MAX_X = 8;
    private static final int DOOR_MAX_Y = 9;
    private static final int DOOR_MAX_Z = 10;
    private static final int DOOR_ENG_X = 11;
    private static final int DOOR_ENG_Y = 12;
    private static final int DOOR_ENG_Z = 13;
    private static final int DOOR_LOCKED = 14;
    private static final int DOOR_TYPE = 15;
    private static final int DOOR_ENG_SIDE = 16;
    private static final int DOOR_POWER_X = 17;
    private static final int DOOR_POWER_Y = 18;
    private static final int DOOR_POWER_Z = 19;
    private static final int DOOR_OPEN_DIR = 20;
    private static final int DOOR_AUTO_CLOSE = 21;
    private static final int DOOR_CHUNK_HASH = 22;
    private static final int DOOR_BLOCKS_TO_MOVE = 23;

//    private static final String PLAYERS_ID = "";
//    private static final String PLAYERS_UUID = "";
//    private static final String PLAYERS_NAME = "";
//    private static final String UNION_ID = "";
//    private static final String UNION_PERM = "";
//    private static final String UNION_PLAYER_ID = "";
//    private static final String UNION_DOOR_ID = "";

    private static final String FAKEUUID = "0000";
    private final File dbFile;
    private final String url;
    private final PLogger pLogger;
    private boolean enabled = true;
    private AtomicBoolean locked = new AtomicBoolean(false);
    private final ConfigLoader config;
    private final WorldRetriever worldRetriever;
    private final PlayerRetriever playerRetriever;

    /**
     * Constructor of the SQLite driver connection.
     *
     * @param dbFile          The file to store the database in.
     * @param pLogger         The logger used for error logging.
     * @param config          The {@link ConfigLoader} containing options used in this class.
     * @param worldRetriever  Object that converts a UUID to a World.
     * @param playerRetriever Object that converts a UUID to an OfflinePlayer.
     */
    public SQLiteJDBCDriverConnection(final File dbFile, final PLogger pLogger, final ConfigLoader config,
                                      final WorldRetriever worldRetriever,
                                      final PlayerRetriever playerRetriever)
    {
        this.pLogger = pLogger;
        this.dbFile = dbFile;
        this.config = config;
        this.worldRetriever = worldRetriever;
        this.playerRetriever = playerRetriever;
        url = "jdbc:sqlite:" + dbFile;
        init();
        upgrade();
    }

    /**
     * Establishes a connection with the database.
     *
     * @return A database connection.
     */
    private @NotNull Connection getConnection()
    {
        if (!enabled)
        {
            IllegalStateException e = new IllegalStateException();
            pLogger.logException(e, "Database disabled! This probably means an upgrade failed! " +
                    "Please contact pim16aap2.");
            throw e;
        }
        if (locked.get())
        {
            IllegalStateException e = new IllegalStateException();
            pLogger.logException(e, "Database locked! Please try again later! " +
                    "Please contact pim16aap2 if the issue persists.");
            throw e;
        }
        Connection conn = null;
        try
        {
            Class.forName(DRIVER);
            conn = DriverManager.getConnection(url);
            conn.createStatement().execute("PRAGMA foreign_keys=ON");
        }
        catch (SQLException | ClassNotFoundException e)
        {
            pLogger.logException(e, "Failed to open connection!");
        }
        if (conn == null)
            throw new NullPointerException();
        return conn;
    }

    /**
     * Initializes the database. I.e. create all the required tables.
     */
    private void init()
    {
        if (!dbFile.exists())
            try
            {
                dbFile.getParentFile().mkdirs();
                dbFile.createNewFile();
                pLogger.warn("New file created at " + dbFile);
            }
            catch (IOException e)
            {
                pLogger.warn("File write error: " + dbFile);
            }

        // Table creation
        try (Connection conn = getConnection())
        {
            // Check if the doors table already exists. If it does, assume the rest exists
            // as well and don't set it up.
            if (!conn.getMetaData().getTables(null, null, "doors", new String[]{"TABLE"}).next())
            {
                Statement stmt1 = conn.createStatement();
                String sql1 = "CREATE TABLE IF NOT EXISTS doors\n" +
                        "(id            INTEGER    PRIMARY KEY autoincrement,\n" +
                        " name          TEXT       NOT NULL,\n" +
                        " world         TEXT       NOT NULL,\n" +
                        " isOpen        INTEGER    NOT NULL,\n" +
                        " xMin          INTEGER    NOT NULL,\n" +
                        " yMin          INTEGER    NOT NULL,\n" +
                        " zMin          INTEGER    NOT NULL,\n" +
                        " xMax          INTEGER    NOT NULL,\n" +
                        " yMax          INTEGER    NOT NULL,\n" +
                        " zMax          INTEGER    NOT NULL,\n" +
                        " engineX       INTEGER    NOT NULL,\n" +
                        " engineY       INTEGER    NOT NULL,\n" +
                        " engineZ       INTEGER    NOT NULL,\n" +
                        " isLocked      INTEGER    NOT NULL,\n" +
                        " type          INTEGER    NOT NULL DEFAULT  0,\n" +
                        " engineSide    INTEGER    NOT NULL DEFAULT -1,\n" +
                        " powerBlockX   INTEGER    NOT NULL DEFAULT -1,\n" +
                        " powerBlockY   INTEGER    NOT NULL DEFAULT -1,\n" +
                        " powerBlockZ   INTEGER    NOT NULL DEFAULT -1,\n" +
                        " openDirection INTEGER    NOT NULL DEFAULT  0,\n" +
                        " autoClose     INTEGER    NOT NULL DEFAULT -1,\n" +
                        " chunkHash     INTEGER    NOT NULL DEFAULT -1,\n" +
                        " blocksToMove  INTEGER    NOT NULL DEFAULT -1);";
                stmt1.executeUpdate(sql1);
                stmt1.close();

                Statement stmt2 = conn.createStatement();
                String sql2 = "CREATE TABLE IF NOT EXISTS players\n" +
                        "(id          INTEGER    PRIMARY KEY AUTOINCREMENT,\n" +
                        " playerUUID  TEXT       NOT NULL,\n" +
                        " playerName  TEXT       NOT NULL);";
                stmt2.executeUpdate(sql2);
                stmt2.close();

                Statement stmt3 = conn.createStatement();
                String sql3 = "CREATE TABLE IF NOT EXISTS sqlUnion\n" +
                        "(id          INTEGER    PRIMARY KEY AUTOINCREMENT,\n" +
                        " permission  INTEGER    NOT NULL,\n" +
                        " playerID    REFERENCES players(id) ON UPDATE CASCADE ON DELETE CASCADE,\n" +
                        " doorUID     REFERENCES doors(id)   ON UPDATE CASCADE ON DELETE CASCADE, \n" +
                        " unique (playerID, doorUID));";
                stmt3.executeUpdate(sql3);
                stmt3.close();
                setDBVersion(conn, DATABASE_VERSION);
            }
        }
        catch (SQLException | NullPointerException e)
        {
            logMessage("203", e);
        }
    }

    /**
     * Gets the ID of a player in the database.
     *
     * @param conn       A database connection.
     * @param playerUUID The UUID of the player to check.
     * @return The ID of a player in the database.
     *
     * @throws SQLException
     */
    // TODO: Delete this.
    private long getPlayerID(@NotNull final Connection conn, @NotNull final String playerUUID) throws SQLException
    {
        long playerID = -1;
        PreparedStatement ps1 = conn.prepareStatement("SELECT id FROM players WHERE playerUUID = ?;");
        ps1.setString(1, playerUUID);
        ResultSet rs1 = ps1.executeQuery();
        if (rs1.next())
            playerID = rs1.getLong("id");
        ps1.close();
        rs1.close();
        return playerID;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getPermission(@NotNull final String playerUUID, final long doorUID)
    {
        int ret = -1;
        try (Connection conn = getConnection())
        {
            {
                String sql = "SELECT permission \n" +
                        "FROM sqlUnion INNER JOIN players ON players.id = sqlUnion.playerID\n" +
                        "WHERE players.playerUUID = ? AND doorUID = ?;";
                PreparedStatement ps = conn.prepareStatement(sql);
                ps.setString(1, playerUUID);
                ps.setString(2, Long.toString(doorUID));
                ResultSet rs = ps.executeQuery();
                if (rs.next())
                    ret = rs.getInt("permission");

                ps.close();
                rs.close();
            }
        }
        catch (SQLException | NullPointerException e)
        {
            logMessage("244", e);
        }
        return ret;
    }

    private @Nullable DoorBase newDoorBaseFromRS(@NotNull final ResultSet rs, @NotNull final DoorOwner doorOwner)
    {
        try
        {
            DoorBase door = DoorType.valueOf(rs.getInt("type")).getNewDoor(pLogger, doorOwner.getDoorUID());

            World world = worldRetriever.worldFromString(UUID.fromString(rs.getString("world")));
            door.setWorld(world);
            door.setMinimum(new Location(world, rs.getInt("xMin"), rs.getInt("yMin"), rs.getInt("zMin")));
            door.setMaximum(new Location(world, rs.getInt("xMax"), rs.getInt("yMax"), rs.getInt("zMax")));
            door.setEngineSide(PBlockFace.valueOf(rs.getInt("engineSide")));
            door.setEngineLocation(new Location(world, rs.getInt("engineX"), rs.getInt("engineY"),
                                                rs.getInt("engineZ")));
            door.setPowerBlockLocation(new Location(world, rs.getInt("powerBlockX"), rs.getInt("powerBlockY"),
                                                    rs.getInt("powerBlockZ")));
            door.setName(rs.getString("name"));
            door.setOpenStatus(rs.getInt("isOpen") == 1);
            door.setLock(rs.getInt("isLocked") == 1);
            door.setDoorOwner(doorOwner);
            door.setOpenDir(RotateDirection.valueOf(rs.getInt("openDirection")));
            door.setAutoClose(rs.getInt("autoClose"));
            door.setBlocksToMove(rs.getInt("blocksToMove"));

            return door;
        }
        catch (SQLException | NullPointerException e)
        {
            logMessage("282", e);
            return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean removeDoor(final long doorUID)
    {
        try (Connection conn = getConnection())
        {
            PreparedStatement ps = conn.prepareStatement("DELETE FROM doors WHERE id = ?;");
            ps.setString(1, Long.toString(doorUID));
            int rowsAffected = ps.executeUpdate();
            ps.close();
            return rowsAffected > 0;
        }
        catch (SQLException | NullPointerException e)
        {
            pLogger.logException(e, String.valueOf(271));
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean removeDoors(@NotNull final String playerUUID, @NotNull final String doorName)
    {
        try (Connection conn = getConnection())
        {
            String sql = "DELETE FROM doors \n" +
                    "WHERE doors.id IN \n" +
                    "      (SELECT D.id \n" +
                    "       FROM doors AS D INNER JOIN sqlUnion AS U ON U.doorUID = D.id, \n" +
                    "            (SELECT P.id FROM players as P WHERE P.playerUUID=?) AS R \n" +
                    "      WHERE D.name=? AND R.id = U.playerID);";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, playerUUID);
            ps.setString(2, doorName);
            int rowsAffected = ps.executeUpdate();
            ps.close();
            return rowsAffected > 0;
        }
        catch (SQLException | NullPointerException e)
        {
            logMessage("343", e);
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getDoorCountForPlayer(@NotNull UUID playerUUID)
    {
        try (Connection conn = getConnection())
        {
            int count = 0;
            String sql = "SELECT COUNT(*) AS total \n" +
                    "FROM sqlUnion AS U INNER JOIN players AS P on U.playerID = P.id \n" +
                    "WHERE P.playerUUID=?;";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, playerUUID.toString());
            ResultSet rs = ps.executeQuery();

            if (rs.next())
                count = rs.getInt("total");

            ps.close();
            rs.close();
            return count;
        }
        catch (SQLException | NullPointerException e)
        {
            logMessage("378", e);
        }
        return -1;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getDoorCountForPlayer(@NotNull UUID playerUUID, @NotNull String doorName)
    {
        try (Connection conn = getConnection())
        {
            String sql = "SELECT COUNT(*) AS total \n" +
                    "FROM sqlUnion AS U \n" +
                    "    INNER JOIN players AS P on U.playerID = P.id \n" +
                    "    INNER JOIN doors AS D ON U.doorUID = D.id \n" +
                    "WHERE P.playerUUID=? AND D.name=?;";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, playerUUID.toString());
            ps.setString(2, doorName);
            ResultSet rs = ps.executeQuery();

            int count = rs.next() ? rs.getInt("total") : 0;
            ps.close();
            rs.close();
            return count;
        }
        catch (SQLException | NullPointerException e)
        {
            logMessage("378", e);
        }
        return -1;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getDoorCountByName(@NotNull String doorName)
    {
        try (Connection conn = getConnection())
        {
            String sql = "SELECT COUNT(*) AS total FROM doors WHERE name=?;";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, doorName);
            ResultSet rs = ps.executeQuery();
            int count = rs.next() ? rs.getInt("total") : 0;
            ps.close();
            rs.close();
            return count;
        }
        catch (SQLException | NullPointerException e)
        {
            logMessage("402", e);
        }
        return -1;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public @NotNull Optional<DoorBase> getDoor(@NotNull UUID playerUUID, final long doorUID)
    {
        Optional<DoorBase> door = Optional.empty();

        try (Connection conn = getConnection())
        {
            String sql = "SELECT D.*, P.playerUUID, P.playerName, U.permission\n" +
                    "FROM doors as D INNER JOIN sqlUnion AS U ON U.doorUID = D.id INNER JOIN players AS P ON P.id = U.playerID\n" +
                    "WHERE P.playerUUID = ? AND D.id = ?;";

            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, playerUUID.toString());
            ps.setString(2, Long.toString(doorUID));
            ResultSet rs = ps.executeQuery();

            while (rs.next())
            {
                DoorOwner doorOwner = new DoorOwner(rs.getLong("id"), UUID.fromString(rs.getString("playerUUID")),
                                                    rs.getString("playerName"), rs.getInt("permission"));
                door = Optional.ofNullable(newDoorBaseFromRS(rs, doorOwner));
            }

            ps.close();
            rs.close();
        }
        catch (SQLException | NullPointerException e)
        {
            logMessage("521", e);
        }
        return door;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public @NotNull Optional<DoorBase> getDoor(final long doorUID)
    {
        Optional<DoorBase> door = Optional.empty();

        try (Connection conn = getConnection())
        {
            DoorOwner doorOwner = getOwnerOfDoor(conn, doorUID);
            String sql = "SELECT * FROM doors WHERE id=?;";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, Long.toString(doorUID));
            ResultSet rs = ps.executeQuery();

            if (rs.next())
                door = Optional.ofNullable(newDoorBaseFromRS(rs, doorOwner));

            ps.close();
            rs.close();
        }
        catch (SQLException | NullPointerException e)
        {
            logMessage("521", e);
        }
        return door;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public @NotNull Optional<List<DoorBase>> getDoors(@NotNull final UUID playerUUID, @NotNull final String name)
    {
        return getDoors(playerUUID.toString(), name, 0);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public @NotNull Optional<List<DoorBase>> getDoors(@NotNull final UUID playerUUID)
    {
        return getDoors(playerUUID.toString(), 0);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public @NotNull Optional<List<DoorBase>> getDoors(@NotNull final String name)
    {
        List<DoorBase> doors = new ArrayList<>();

        try (Connection conn = getConnection())
        {
            String sql = "SELECT * FROM doors WHERE name=?;";
            PreparedStatement ps1 = conn.prepareStatement(sql);
            ps1.setString(1, name);
            ResultSet rs1 = ps1.executeQuery();

            while (rs1.next())
            {
                DoorOwner owner = getOwnerOfDoor(conn, rs1.getLong(DOOR_ID));
                if (owner != null)
                    doors.add(newDoorBaseFromRS(rs1, owner));
            }
            ps1.close();
            rs1.close();
        }
        catch (SQLException | NullPointerException e)
        {
            logMessage("582", e);
        }
        return Optional.ofNullable(doors.size() > 0 ? doors : null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public @NotNull Optional<List<DoorBase>> getDoors(@NotNull final String playerUUID, @NotNull final String doorName,
                                                      int maxPermission)
    {
        List<DoorBase> doors = new ArrayList<>();

        try (Connection conn = getConnection())
        {
            String sql = "SELECT D.*, P.playerName, P.playerUUID, U.permission\n" +
                    "FROM sqlUnion AS U INNER JOIN players AS P ON U.playerID = P.id INNER JOIN doors AS D ON U.doorUID = D.id\n" +
                    "WHERE P.playerUUID = ? AND D.name = ? AND U.permission <= ?;";

            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, playerUUID);
            ps.setString(2, doorName);
            ps.setInt(3, maxPermission);
            ResultSet rs = ps.executeQuery();
            while (rs.next())
            {
                DoorOwner doorOwner = new DoorOwner(rs.getLong("id"), UUID.fromString(rs.getString("playerUUID")),
                                                    rs.getString("playerName"), rs.getInt("permission"));
                doors.add(newDoorBaseFromRS(rs, doorOwner));
            }
        }
        catch (SQLException | NullPointerException e)
        {
            logMessage("631", e);
        }
        return Optional.ofNullable(doors.size() > 0 ? doors : null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public @NotNull Optional<List<DoorBase>> getDoors(@NotNull String playerUUID, int maxPermission)
    {
        List<DoorBase> ret = new ArrayList<>();
        try (Connection conn = getConnection())
        {
            String sql = "SELECT D.*, P.playerUUID, P.playerName, U.permission\n" +
                    "FROM doors as D INNER JOIN sqlUnion AS U ON U.doorUID = D.id INNER JOIN players AS P ON P.id = U.playerID\n" +
                    "WHERE P.playerUUID = ? AND permission <= ?;";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, playerUUID);
            ps.setInt(2, maxPermission);
            ResultSet rs = ps.executeQuery();

            while (rs.next())
            {
                DoorOwner doorOwner = new DoorOwner(rs.getLong("id"), UUID.fromString(rs.getString("playerUUID")),
                                                    rs.getString("playerName"), rs.getInt("permission"));
                ret.add(newDoorBaseFromRS(rs, doorOwner));
            }

            ps.close();
            rs.close();
        }
        catch (SQLException | NullPointerException e)
        {
            logMessage("613", e);
        }
        return Optional.of(ret);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public @NotNull Optional<DoorBase> getDoor(@NotNull final String playerUUID, @NotNull final String doorName)
            throws TooManyDoorsException
    {
        return getDoor(playerUUID, doorName, Integer.MAX_VALUE);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public @NotNull Optional<DoorBase> getDoor(@NotNull final String playerUUID, @NotNull final String doorName,
                                               int maxPermission)
            throws TooManyDoorsException
    {
        DoorBase door = null;
        int count = 0;

        try (Connection conn = getConnection())
        {
            String sql = "SELECT D.*, P.playerUUID, P.playerName, U.permission\n" +
                    "FROM doors as D INNER JOIN sqlUnion AS U ON U.doorUID = D.id INNER JOIN players AS P ON P.id = U.playerID\n" +
                    "WHERE P.playerUUID = ? AND permission <= ? AND D.name = ?;";

            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, playerUUID);
            ps.setInt(2, maxPermission);
            ps.setString(3, doorName);
            ResultSet rs = ps.executeQuery();

            while (rs.next())
            {
                ++count;
                if (count > 1)
                    break;
                DoorOwner doorOwner = new DoorOwner(rs.getLong("id"), UUID.fromString(rs.getString("playerUUID")),
                                                    rs.getString("playerName"), rs.getInt("permission"));
                door = newDoorBaseFromRS(rs, doorOwner);
            }

            ps.close();
            rs.close();
        }
        catch (SQLException | NullPointerException e)
        {
            logMessage("621", e);
        }
        if (count > 1)
            throw new TooManyDoorsException();
        return Optional.ofNullable(door);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean updatePlayerName(@NotNull final String playerUUID, @NotNull final String playerName)
    {
        try (Connection conn = getConnection())
        {
            String sql = "UPDATE players " +
                    "SET playerName=? " +
                    "WHERE playerUUID=?;";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, playerName);
            ps.setString(2, playerUUID);
            int rowsAffected = ps.executeUpdate();
            ps.close();
            return rowsAffected > 0;
        }
        catch (SQLException | NullPointerException e)
        {
            logMessage("671", e);
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public @NotNull Optional<UUID> getPlayerUUID(@NotNull final String playerName)
    {
        UUID playerUUID = null;
        try (Connection conn = getConnection())
        {
            String sql = "SELECT P.playerUUID " +
                    "FROM players AS P " +
                    "WHERE P.playerName=?;";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, playerName);
            ResultSet rs = ps.executeQuery();
            int count = 0;
            while (rs.next())
            {
                ++count;
                if (count > 1)
                {
                    playerUUID = null;
                    break;
                }
                playerUUID = UUID.fromString(rs.getString("playerUUID"));
            }
            ps.close();
            rs.close();
        }
        catch (SQLException | NullPointerException e)
        {
            logMessage("736", e);
        }
        return Optional.ofNullable(playerUUID);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public @NotNull Optional<String> getPlayerName(@NotNull final String playerUUID)
    {
        String playerName = null;
        try (Connection conn = getConnection())
        {
            String sql = "SELECT P.playerName " +
                    "FROM players AS P " +
                    "WHERE P.playerUUID=?;";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, playerUUID);
            ResultSet rs = ps.executeQuery();
            playerName = rs.next() ? rs.getString("playerName") : null;
            ps.close();
            rs.close();
        }
        catch (SQLException | NullPointerException e)
        {
            logMessage("736", e);
        }
        return Optional.ofNullable(playerName);
    }

    private DoorOwner getOwnerOfDoor(@NotNull final Connection conn, final long doorUID) throws SQLException
    {
        DoorOwner doorOwner = null;
        String sql = "SELECT playerUUID, playerName\n" +
                "FROM players,\n" +
                "     (SELECT U.playerID, U.permission, doors.name\n" +
                "      FROM sqlUnion as U INNER JOIN doors ON doors.id = U.doorUID\n" +
                "      WHERE U.permission = '0' AND doors.id = ?) AS R\n" +
                "WHERE id = R.playerID;";
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setString(1, Long.toString(doorUID));
        ResultSet rs = ps.executeQuery();
        if (rs.next())
        {
            doorOwner = new DoorOwner(doorUID, UUID.fromString(rs.getString("playerUUID")),
                                      rs.getString("playerName"), 0);
        }
        ps.close();
        rs.close();

        return doorOwner;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public @NotNull Optional<DoorOwner> getOwnerOfDoor(final long doorUID)
    {
        DoorOwner doorOwner = null;

        try (Connection conn = getConnection())
        {
            doorOwner = getOwnerOfDoor(conn, doorUID);
        }
        catch (SQLException | NullPointerException e)
        {
            logMessage("788", e);
        }
        return Optional.ofNullable(doorOwner);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public @NotNull Map<Long, Long> getPowerBlockData(final long chunkHash)
    {
        Map<Long, Long> doors = new HashMap<>();

        try (Connection conn = getConnection())
        {
            // Get the door associated with the x/y/z location of the power block block.
            PreparedStatement ps = conn.prepareStatement("SELECT * FROM doors WHERE chunkHash=?;");
            ps.setString(1, Long.toString(chunkHash));
            ResultSet rs = ps.executeQuery();
            while (rs.next())
            {
                long locationHash = Util.simpleLocationhash(rs.getInt(DOOR_POWER_X),
                                                            rs.getInt(DOOR_POWER_Y),
                                                            rs.getInt(DOOR_POWER_Z));
                System.out.println("FOUND DOOR: " + rs.getLong(DOOR_ID) + ", LocHash = " + locationHash +
                                           ", x = " + rs.getInt(DOOR_POWER_X) +
                                           ", y = " + rs.getInt(DOOR_POWER_Y) +
                                           ", z = " + rs.getInt(DOOR_POWER_Z)
                );
                doors.put(locationHash, rs.getLong(DOOR_ID));
            }
            ps.close();
            rs.close();
        }
        catch (SQLException | NullPointerException e)
        {
            logMessage("828", e);
        }
        return doors;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateDoorBlocksToMove(final long doorUID, final int blocksToMove)
    {
        try (Connection conn = getConnection())
        {
            conn.setAutoCommit(false);
            PreparedStatement ps = conn.prepareStatement("UPDATE doors SET blocksToMove=? WHERE id=?;");
            ps.setInt(1, blocksToMove);
            ps.setString(2, Long.toString(doorUID));
            ps.executeUpdate();
            conn.commit();
        }
        catch (SQLException | NullPointerException e)
        {
            logMessage("859", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateDoorCoords(final long doorUID, final boolean isOpen, final int xMin, final int yMin,
                                 final int zMin, final int xMax, final int yMax, final int zMax,
                                 @NotNull final PBlockFace engSide)
    {
        try (Connection conn = getConnection())
        {
            conn.setAutoCommit(false);
            PreparedStatement ps = conn.prepareStatement("UPDATE doors SET xMin=?,yMin=?,zMin=?,xMax=?" +
                                                                 ",yMax=?,zMax=?,isOpen=?,engineSide=? WHERE id =?;");
            ps.setInt(1, xMin);
            ps.setInt(2, yMin);
            ps.setInt(3, zMin);
            ps.setInt(4, xMax);
            ps.setInt(5, yMax);
            ps.setInt(6, zMax);
            ps.setBoolean(7, isOpen);
            ps.setInt(8, PBlockFace.getValue(engSide));
            ps.setString(9, Long.toString(doorUID));
            ps.executeUpdate();
            conn.commit();
        }
        catch (SQLException | NullPointerException e)
        {
            logMessage("897", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateDoorCoords(final long doorUID, final boolean isOpen, final int xMin, final int yMin,
                                 final int zMin, final int xMax, final int yMax, final int zMax)
    {
        try (Connection conn = getConnection())
        {
            conn.setAutoCommit(false);
            PreparedStatement ps = conn.prepareStatement("UPDATE doors SET xMin=?,yMin=?,zMin=?,xMax=?" +
                                                                 ",yMax=?,zMax=?,isOpen=? WHERE id =?;");
            ps.setInt(1, xMin);
            ps.setInt(2, yMin);
            ps.setInt(3, zMin);
            ps.setInt(4, xMax);
            ps.setInt(5, yMax);
            ps.setInt(6, zMax);
            ps.setBoolean(7, isOpen);
            ps.setString(8, Long.toString(doorUID));
            ps.executeUpdate();
            conn.commit();
        }
        catch (SQLException | NullPointerException e)
        {
            logMessage("976", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateDoorAutoClose(final long doorUID, final int autoClose)
    {
        try (Connection conn = getConnection())
        {
            conn.setAutoCommit(false);
            PreparedStatement ps = conn.prepareStatement("UPDATE doors SET autoClose=? WHERE id=?;");
            ps.setInt(1, autoClose);
            ps.setString(2, Long.toString(doorUID));
            ps.executeUpdate();
            conn.commit();
        }
        catch (SQLException | NullPointerException e)
        {
            logMessage("928", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateDoorOpenDirection(final long doorUID, @NotNull final RotateDirection openDir)
    {
        try (Connection conn = getConnection())
        {
            conn.setAutoCommit(false);
            PreparedStatement ps = conn.prepareStatement("UPDATE doors SET openDirection=? WHERE id=?;");
            ps.setInt(1, RotateDirection.getValue(openDir));
            ps.setString(2, Long.toString(doorUID));
            ps.executeUpdate();
            conn.commit();
        }
        catch (SQLException | NullPointerException e)
        {
            logMessage("958", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateDoorPowerBlockLoc(final long doorUID, final int xPos, final int yPos, final int zPos,
                                        @NotNull final UUID worldUUID)
    {
        try (Connection conn = getConnection())
        {
            conn.setAutoCommit(false);
            PreparedStatement ps = conn
                    .prepareStatement("UPDATE doors SET powerBlockX=?, powerBlockY=?, powerBlockZ=?, " +
                                              "chunkHash=? WHERE id=?;");
            ps.setInt(1, xPos);
            ps.setInt(2, yPos);
            ps.setInt(3, zPos);
            ps.setString(4, Long.toString(Util.simpleChunkHashFromLocation(xPos, zPos)));
            ps.setString(5, Long.toString(doorUID));
            ps.executeUpdate();
            conn.commit();
        }
        catch (SQLException | NullPointerException e)
        {
            logMessage("992", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isPowerBlockLocationEmpty(@NotNull final Location loc)
    {
        try (Connection conn = getConnection())
        {
            PreparedStatement ps = conn
                    .prepareStatement(
                            "SELECT COUNT(*) as sum FROM doors WHERE powerBlockX=? AND powerBlockY=? AND powerBlockZ=? " +
                                    " AND world=?;");
            ps.setInt(1, loc.getBlockX());
            ps.setInt(2, loc.getBlockY());
            ps.setInt(3, loc.getBlockZ());
            ps.setString(4, loc.getWorld().getUID().toString());

            ps.executeQuery();
            ResultSet rs = ps.executeQuery();

            boolean isLocationEmpty = true;
            if (rs.next())
                isLocationEmpty = rs.getInt("sum") == 0;

            ps.close();
            rs.close();
            return isLocationEmpty;
        }
        catch (SQLException | NullPointerException e)
        {
            logMessage("1033", e);
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setLock(final long doorUID, final boolean newLockStatus)
    {
        try (Connection conn = getConnection())
        {
            conn.setAutoCommit(false);
            PreparedStatement ps = conn.prepareStatement("UPDATE doors SET isLocked=? WHERE id=?;");
            ps.setBoolean(1, newLockStatus);
            ps.setString(2, Long.toString(doorUID));
            ps.executeUpdate();
            conn.commit();

        }
        catch (SQLException | NullPointerException e)
        {
            logMessage("1066", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void insert(@NotNull final DoorBase door)
    {
        try (Connection conn = getConnection())
        {
            long playerID = getPlayerID(conn, door.getPlayerUUID().toString());

            if (playerID == -1)
            {
                String sql1 = "INSERT INTO players (playerUUID, playerName) VALUES (?,?);";
                PreparedStatement ps = conn.prepareStatement(sql1);
                ps.setString(1, door.getPlayerUUID().toString());
                ps.setString(2, door.getDoorOwner().getPlayerName());
                ps.executeUpdate();
                ps.close();

                String query = "SELECT last_insert_rowid() AS lastId";
                PreparedStatement ps2 = conn.prepareStatement(query);
                ResultSet rs2 = ps2.executeQuery();
                playerID = rs2.getLong("lastId");
                ps2.close();
                rs2.close();
            }

            String doorInsertsql = "INSERT INTO doors(name,world,isOpen,xMin,yMin,zMin,xMax,yMax,zMax,"
                    + "engineX,engineY,engineZ,isLocked,type,engineSide,powerBlockX,powerBlockY,powerBlockZ,"
                    + "openDirection,autoClose,chunkHash,blocksToMove) "
                    + "VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
            PreparedStatement doorstatement = conn.prepareStatement(doorInsertsql);

            doorstatement.setString(DOOR_NAME - 1, door.getName());
            doorstatement.setString(DOOR_WORLD - 1, door.getWorld().getUID().toString());
            doorstatement.setInt(DOOR_OPEN - 1, door.isOpen() ? 1 : 0);
            doorstatement.setInt(DOOR_MIN_X - 1, door.getMinimum().getBlockX());
            doorstatement.setInt(DOOR_MIN_Y - 1, door.getMinimum().getBlockY());
            doorstatement.setInt(DOOR_MIN_Z - 1, door.getMinimum().getBlockZ());
            doorstatement.setInt(DOOR_MAX_X - 1, door.getMaximum().getBlockX());
            doorstatement.setInt(DOOR_MAX_Y - 1, door.getMaximum().getBlockY());
            doorstatement.setInt(DOOR_MAX_Z - 1, door.getMaximum().getBlockZ());
            doorstatement.setInt(DOOR_ENG_X - 1, door.getEngine().getBlockX());
            doorstatement.setInt(DOOR_ENG_Y - 1, door.getEngine().getBlockY());
            doorstatement.setInt(DOOR_ENG_Z - 1, door.getEngine().getBlockZ());
            doorstatement.setInt(DOOR_LOCKED - 1, door.isLocked() ? 1 : 0);
            doorstatement.setInt(DOOR_TYPE - 1, DoorType.getValue(door.getType()));
            // Set -1 if the door has no engineSide (normal doors don't use it)
            doorstatement.setInt(DOOR_ENG_SIDE - 1,
                                 door.getEngineSide() == null ? -1 : PBlockFace.getValue(door.getEngineSide()));
            doorstatement.setInt(DOOR_POWER_X - 1, door.getPowerBlockLoc().getBlockX());
            doorstatement.setInt(DOOR_POWER_Y - 1, door.getPowerBlockLoc().getBlockY());
            doorstatement.setInt(DOOR_POWER_Z - 1, door.getPowerBlockLoc().getBlockZ());
            doorstatement.setInt(DOOR_OPEN_DIR - 1, RotateDirection.getValue(door.getOpenDir()));
            doorstatement.setInt(DOOR_AUTO_CLOSE - 1, door.getAutoClose());
            doorstatement.setString(DOOR_CHUNK_HASH - 1, Long.toString(door.getSimplePowerBlockChunkHash()));
            doorstatement.setString(DOOR_BLOCKS_TO_MOVE - 1, Long.toString(door.getBlocksToMove()));

            doorstatement.execute();
            doorstatement.close();

            String getDoorUID = "SELECT last_insert_rowid() AS lastId";
            PreparedStatement ps2 = conn.prepareStatement(getDoorUID);
            ResultSet rs2 = ps2.executeQuery();
            long doorUID = rs2.getLong("lastId");
            ps2.close();
            rs2.close();

            Statement stmt3 = conn.createStatement();
            String sql3 = "INSERT INTO sqlUnion (permission, playerID, doorUID) "
                    + "VALUES ('" + door.getPermission() + "', '" + playerID + "', '" + doorUID + "');";
            stmt3.executeUpdate(sql3);
            stmt3.close();
        }
        catch (SQLException | NullPointerException e)
        {
            logMessage("1153", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean removeOwner(final long doorUID, @NotNull final String playerUUID)
    {
        try (Connection conn = getConnection())
        {
            String sql = "DELETE " +
                    "FROM sqlUnion " +
                    "WHERE sqlUnion.id IN " +
                    "(SELECT U.id " +
                    "FROM sqlUnion AS U INNER JOIN players AS P on U.playerID=P.id " +
                    "WHERE P.playerUUID=? AND U.permission > '0' AND U.doorUID=?);";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, playerUUID);
            ps.setString(2, Long.toString(doorUID));
            int rowsAffected = ps.executeUpdate();
            ps.close();
            return rowsAffected > 0;
        }
        catch (SQLException | NullPointerException e)
        {
            logMessage("1193", e);
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public List<DoorOwner> getOwnersOfDoor(final long doorUID)
    {
        List<DoorOwner> ret = new ArrayList<>();
        try (Connection conn = getConnection())
        {
            String sql = "SELECT U.permission, P.playerUUID, P.playerName " +
                    "FROM sqlUnion AS U INNER JOIN players AS P ON U.playerID = P.id " +
                    "WHERE doorUID=?;";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, Long.toString(doorUID));
            ResultSet rs = ps.executeQuery();

            while (rs.next())
                ret.add(new DoorOwner(doorUID, UUID.fromString(rs.getString("playerUUID")),
                                      rs.getString("playerName"), rs.getInt("permission")));
            ps.close();
            rs.close();
        }
        catch (SQLException | NullPointerException e)
        {
            logMessage("1234", e);
        }
        return ret;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addOwner(final long doorUID, @NotNull final UUID playerUUID, final int permission)
    {
        // permission level 0 is reserved for the creator, and negative values are not allowed.
        if (permission < 1)
            return;

        try (Connection conn = getConnection())
        {
            long playerID = getPlayerID(conn, playerUUID.toString());

            if (playerID == -1)
            {
                String playerName = playerRetriever.nameFromUUID(playerUUID);
                if (playerName == null)
                    return;
                String sql = "INSERT INTO players (playerUUID, playerName) VALUES (?,?);";
                PreparedStatement ps = conn.prepareStatement(sql);
                ps.setString(1, playerUUID.toString());
                ps.setString(2, playerName);
                ps.executeUpdate();
                ps.close();

                PreparedStatement ps2 = conn.prepareStatement("SELECT last_insert_rowid() AS lastId");
                ResultSet rs2 = ps2.executeQuery();
                playerID = rs2.getLong("lastId");
                ps2.close();
                rs2.close();
            }

            String sql2 = "SELECT * FROM sqlUnion WHERE playerID=? AND doorUID=?;";
            PreparedStatement ps2 = conn.prepareStatement(sql2);
            ps2.setString(1, Long.toString(playerID));
            ps2.setString(2, Long.toString(doorUID));
            ResultSet rs2 = ps2.executeQuery();

            // If it already exists, update the permission, if needed.
            if (rs2.next())
            {
                if (rs2.getInt("permission") != permission)
                {
                    String sql3 = "UPDATE sqlUnion SET permission=? WHERE playerID=? and doorUID=?;";
                    PreparedStatement ps3 = conn.prepareStatement(sql3);
                    ps3.setInt(1, permission);
                    ps3.setString(2, Long.toString(playerID));
                    ps3.setString(3, Long.toString(doorUID));
                    ps3.executeUpdate();
                    ps3.close();
                }
            }
            else
            {
                String sql4 = "INSERT INTO sqlUnion (permission, playerID, doorUID) values(?,?,?);";
                PreparedStatement ps4 = conn.prepareStatement(sql4);
                ps4.setInt(1, permission);
                ps4.setString(2, Long.toString(playerID));
                ps4.setString(3, Long.toString(doorUID));
                ps4.executeUpdate();
                ps4.close();
            }
            ps2.close();
            rs2.close();
        }
        catch (SQLException | NullPointerException e)
        {
            logMessage("1306", e);
        }
    }

    private void upgrade()
    {
        Connection conn = null;
        boolean replaceTempPlayerNames = false;
        try
        {
            conn = getConnection();
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("PRAGMA user_version;");
            int dbVersion = rs.getInt(1);
            stmt.close();
            rs.close();

            if (dbVersion == DATABASE_VERSION)
            {
                conn.close();
                return;
            }

            // If an update is required and backups are enabled, make a backup.
            // First close the connection to the database. Reopen it when possible.
            if (config.dbBackup())
            {
                conn.close();
                if (!makeBackup())
                    return;
                conn = getConnection();
            }

            if (dbVersion < 1)
                upgradeToV1(conn);

            if (dbVersion < 2)
                upgradeToV2(conn);

            if (dbVersion < 3)
                upgradeToV3(conn);

            if (dbVersion < 4)
                upgradeToV4(conn);

            if (dbVersion < 5)
            {
                conn.close();
                upgradeToV5();
                replaceTempPlayerNames = true;
                conn = getConnection();
            }

            if (dbVersion < 6)
                upgradeToV6(conn);

            // If the database upgrade to V5 got interrupted in a previous attempt, the
            // fakeUUID
            // will still be in the database. If so, simply continue filling in player names
            // in the db.
            if (!replaceTempPlayerNames && fakeUUIDExists(conn))
                replaceTempPlayerNames = true;

            // Do this at the very end, so the db version isn't altered if anything fails.
            setDBVersion(conn, DATABASE_VERSION);
        }
        catch (SQLException | NullPointerException e)
        {
            logMessage("1414", e);
        }
        finally
        {
            if (conn != null)
                try
                {
                    conn.close();
                }
                catch (SQLException | NullPointerException e)
                {
                    logMessage("1424", e);
                }
        }
        if (replaceTempPlayerNames)
            startReplaceTempPlayerNames();
    }

    private boolean fakeUUIDExists(@NotNull final Connection conn)
    {
        try
        {
            return conn.createStatement()
                       .executeQuery("SELECT COUNT(*) AS total FROM players WHERE playerUUID = '" + FAKEUUID + "';")
                       .getInt("total") > 0;
        }
        catch (SQLException | NullPointerException e)
        {
            logMessage("1461", e);
        }
        return false;
    }

    private boolean makeBackup()
    {
        File dbFileBackup = new File(dbFile.toString() + ".BACKUP");
        // Only the most recent backup is kept, so delete the old one if a new one needs
        // to be created.
        if (dbFileBackup.exists())
            dbFileBackup.delete();
        try
        {
            Files.copy(dbFile.toPath(), dbFileBackup.toPath());
        }
        catch (IOException e)
        {
            pLogger.logException(e, "Failed to create backup of the database! "
                    + "Database upgrade aborted and access is disabled!");
            enabled = false;
            return false;
        }
        return true;
    }

    private void setDBVersion(@NotNull final Connection conn, final int version)
    {
        try
        {
            conn.createStatement().execute("PRAGMA user_version = " + version + ";");
        }
        catch (SQLException | NullPointerException e)
        {
            logMessage("1458", e);
        }
    }

    private void upgradeToV1(@NotNull final Connection conn)
    {
        try
        {
            String addColumn;

            DatabaseMetaData md = conn.getMetaData();
            ResultSet rs = md.getColumns(null, null, "doors", "type");

            if (!rs.next())
            {
                pLogger.warn("Upgrading database! Adding type!");
                addColumn = "ALTER TABLE doors ADD COLUMN type int NOT NULL DEFAULT 0";
                conn.createStatement().execute(addColumn);
            }
            rs.close();

            rs = md.getColumns(null, null, "doors", "engineSide");
            if (!rs.next())
            {
                pLogger.warn("Upgrading database! Adding engineSide!");
                addColumn = "ALTER TABLE doors ADD COLUMN engineSide int NOT NULL DEFAULT -1";
                conn.createStatement().execute(addColumn);
            }
            rs.close();

            rs = md.getColumns(null, null, "doors", "powerBlockX");
            if (!rs.next())
            {
                pLogger.warn("Upgrading database! Adding powerBlockLoc!");
                addColumn = "ALTER TABLE doors ADD COLUMN powerBlockX int NOT NULL DEFAULT -1";
                conn.createStatement().execute(addColumn);
                addColumn = "ALTER TABLE doors ADD COLUMN powerBlockY int NOT NULL DEFAULT -1";
                conn.createStatement().execute(addColumn);
                addColumn = "ALTER TABLE doors ADD COLUMN powerBlockZ int NOT NULL DEFAULT -1";
                conn.createStatement().execute(addColumn);

                PreparedStatement ps1 = conn.prepareStatement("SELECT * FROM doors;");
                ResultSet rs1 = ps1.executeQuery();
                String update;

                while (rs1.next())
                {
                    long UID = rs1.getLong(DOOR_ID);
                    int x = rs1.getInt(DOOR_ENG_X);
                    int y = rs1.getInt(DOOR_ENG_Y) - 1;
                    int z = rs1.getInt(DOOR_ENG_Z);

                    update = "UPDATE doors SET powerBlockX=?, powerBlockY=?, powerBlockZ=? WHERE id=?;";
                    PreparedStatement ps2 = conn.prepareStatement(update);
                    ps2.setInt(1, x);
                    ps2.setInt(2, y);
                    ps2.setInt(3, z);
                    ps2.setString(4, Long.toString(UID));
                    ps2.executeUpdate();
                    ps2.close();
                }
                ps1.close();
                rs1.close();
            }
            rs.close();

            rs = md.getColumns(null, null, "doors", "openDirection");
            if (!rs.next())
            {
                pLogger.warn("Upgrading database! Adding openDirection!");
                addColumn = "ALTER TABLE doors ADD COLUMN openDirection int NOT NULL DEFAULT 0";
                conn.createStatement().execute(addColumn);
                pLogger
                        .warn("Upgrading database! Swapping open-status of drawbridges to conform to the new standard!");

                String update;
                {
                    update = "UPDATE doors SET isOpen='2' WHERE isOpen='0' AND type=?;";
                    PreparedStatement ps = conn.prepareStatement(update);
                    ps.setInt(1, DoorType.getValue(DoorType.DRAWBRIDGE));
                    ps.executeUpdate();
                    ps.close();
                }
                {
                    update = "UPDATE doors SET isOpen='0' WHERE isOpen='1' AND type=?;";
                    PreparedStatement ps = conn.prepareStatement(update);
                    ps.setInt(1, DoorType.getValue(DoorType.DRAWBRIDGE));
                    ps.executeUpdate();
                    ps.close();
                }
                {
                    update = "UPDATE doors SET isOpen='1' WHERE isOpen='2' AND type=?;";
                    PreparedStatement ps = conn.prepareStatement(update);
                    ps.setInt(1, DoorType.getValue(DoorType.DRAWBRIDGE));
                    ps.executeUpdate();
                    ps.close();
                }
            }
            rs.close();

            rs = md.getColumns(null, null, "doors", "autoClose");
            if (!rs.next())
            {
                pLogger.warn("Upgrading database! Adding autoClose!");
                addColumn = "ALTER TABLE doors ADD COLUMN autoClose int NOT NULL DEFAULT -1";
                conn.createStatement().execute(addColumn);
            }
            rs.close();

            rs = md.getColumns(null, null, "doors", "chunkHash");
            if (!rs.next())
            {
                pLogger.warn("Upgrading database! Adding chunkHash!");
                addColumn = "ALTER TABLE doors ADD COLUMN chunkHash int NOT NULL DEFAULT -1";
                conn.createStatement().execute(addColumn);

                PreparedStatement ps1 = conn.prepareStatement("SELECT * FROM doors;");
                ResultSet rs1 = ps1.executeQuery();
                String update;

                while (rs1.next())
                {
                    long UID = rs1.getLong(DOOR_ID);
                    int x = rs1.getInt(DOOR_POWER_X);
                    int z = rs1.getInt(DOOR_POWER_Z);

                    update = "UPDATE doors SET chunkHash=? WHERE id=?;";
                    PreparedStatement ps2 = conn.prepareStatement(update);
                    ps2.setString(1, Long.toString(Util.simpleChunkHashFromLocation(x, z)));
                    ps2.setString(2, Long.toString(UID));
                    ps2.executeUpdate();
                    ps2.close();
                }
                ps1.close();
                rs1.close();
            }
            rs.close();
            pLogger.warn("Database has been upgraded to V1!");
        }
        catch (SQLException | NullPointerException e)
        {
            logMessage("1593", e);
        }
    }

    private void upgradeToV2(@NotNull final Connection conn)
    {
        try
        {
            String addColumn;
            pLogger.warn("Upgrading database to V2! Adding blocksToMove!");
            addColumn = "ALTER TABLE doors ADD COLUMN blocksToMove int NOT NULL DEFAULT -1;";
            conn.createStatement().execute(addColumn);
        }
        catch (SQLException | NullPointerException e)
        {
            logMessage("1238", e);
        }
    }

    /*
     * Right, so this is quite annoying. SQLite only supports a
     * "limited subset of ALTER TABLE". Source:
     * https://www.sqlite.org/lang_altertable.html
     *
     * The main "ALTER TABLE" instruction I need, is to add the UNIQUE constraint to
     * the playerID and doorUID columns of the sqlUnion. V2 did not have this
     * constraint, while v3+ does require it.
     *
     * So instead of simply updating the table, the table will be recreated with the
     * uniqueness constraint added. Why change something something small when you
     * can just copy all the data, right? I hate speed. Maybe I should just copy all
     * database data every 5 seconds.
     */
    private void upgradeToV3(@NotNull final Connection conn)
    {
        try
        {
            pLogger.warn("Upgrading database to V3! Recreating sqlUnion!");
            conn.setAutoCommit(false);
            // Rename sqlUnion.
            conn.createStatement().execute("ALTER TABLE sqlUnion RENAME TO sqlUnion_old;");

            // Create updated version of sqlUnion.
            conn.createStatement()
                .execute("CREATE TABLE IF NOT EXISTS sqlUnion "
                                 + "(id          INTEGER    PRIMARY KEY AUTOINCREMENT, "
                                 + " permission  INTEGER    NOT NULL, "
                                 + " playerID    REFERENCES players(id) ON UPDATE CASCADE ON DELETE CASCADE, "
                                 + " doorUID     REFERENCES doors(id)   ON UPDATE CASCADE ON DELETE CASCADE,"
                                 + " unique (playerID, doorUID));");

            // Copy data from old sqlUnion to new sqlUnion.
            conn.createStatement().execute("INSERT INTO sqlUnion SELECT * FROM sqlUnion_old;");

            // Get rid of old sqlUnion.
            conn.createStatement().execute("DROP TABLE IF EXISTS 'sqlUnion_old';");
            conn.commit();
            conn.setAutoCommit(true);
        }
        catch (SQLException | NullPointerException e)
        {
            try
            {
                conn.rollback();
            }
            catch (SQLException e1)
            {
                logMessage("1285", e1);
            }
            logMessage("1287", e);
        }
    }

    private void upgradeToV4(@NotNull final Connection conn)
    {
        try
        {
            String addColumn;
            pLogger.warn("Upgrading database to V4! Adding playerName!");
            addColumn = "ALTER TABLE players " + "ADD COLUMN playerName TEXT DEFAULT NULL";
            conn.createStatement().execute(addColumn);
        }
        catch (SQLException | NullPointerException e)
        {
            logMessage("1420", e);
        }
    }

    /*
     * In V5 of the DB, the NOT NULL constraint has been added to the playerName
     * attribute of the players table. All NULL values get replaced by a temporary,
     * randomly chosen unique player name. All these temporary names then get
     * replaced by their actual names on a separate thread. While updating on the
     * secondary thread, the DB is locked.
     */
    private void upgradeToV5()
    {
        try (Connection conn = getConnection())
        {
            pLogger.warn("Upgrading database to V5!");

            String countStr = "SELECT COUNT(*) AS total FROM players WHERE playerName IS NULL";
            int count = conn.createStatement().executeQuery(countStr).getInt("total");

            // Make sure there aren't any NULL values in the players database.
            if (count > 0)
            {
                // First, find a name that does not exist in the database already.
                // Do this by generating random strings and checking if it exists until
                // We encounter one that doesn't.
                String fakeName = Util.randomInsecureString(12);
                boolean exists = true;
                while (exists)
                {
                    String sql = "SELECT * FROM players WHERE playerName=?;";
                    PreparedStatement ps = conn.prepareStatement(sql);
                    ps.setString(1, fakeName);

                    ResultSet rs = ps.executeQuery();
                    if (!rs.next())
                        exists = false;
                    ps.close();
                    rs.close();
                    if (exists)
                        fakeName = Util.randomInsecureString(12);
                }
                pLogger.warn("UpgradeToV5: Using fakeName = " + fakeName);

                {
                    String sql = "INSERT INTO players (playerUUID, playerName) VALUES(?,?);";
                    PreparedStatement ps1 = conn.prepareStatement(sql);
                    ps1.setString(1, FAKEUUID);
                    ps1.setString(2, fakeName);
                    ps1.executeUpdate();
                    ps1.close();
                }

                {
                    String sql = "UPDATE players SET playerName=? WHERE playerName IS NULL;";
                    PreparedStatement ps1 = conn.prepareStatement(sql);
                    ps1.setString(1, fakeName);
                    ps1.executeUpdate();
                    ps1.close();
                }
            }
        }
        catch (SQLException | NullPointerException e)
        {
            logMessage("1745", e);
        }

        Connection conn = null;
        try
        {
            Class.forName(DRIVER);
            conn = DriverManager.getConnection(url);
            conn.createStatement().execute("PRAGMA foreign_keys=OFF");
            conn.setAutoCommit(false);
            // Rename sqlUnion.
            conn.createStatement().execute("ALTER TABLE players RENAME TO players_old;");

            // Create updated version of sqlUnion.
            conn.createStatement()
                .execute("CREATE TABLE IF NOT EXISTS players "
                                 + "(id          INTEGER PRIMARY KEY AUTOINCREMENT, "
                                 + " playerUUID  TEXT    NOT NULL,"
                                 + " playerName  TEXT    NOT NULL)");

            // Copy data from old sqlUnion to new sqlUnion.
            conn.createStatement().execute("INSERT INTO players SELECT * FROM players_old;");

            // Get rid of old sqlUnion.
            conn.createStatement().execute("DROP TABLE IF EXISTS 'players_old';");
            conn.commit();
            conn.setAutoCommit(true);
        }
        catch (SQLException | NullPointerException | ClassNotFoundException e)
        {
            if (conn != null)
                try
                {
                    conn.rollback();
                }
                catch (SQLException e1)
                {
                    logMessage("1770", e1);
                }
            logMessage("1772", e);
        }
        finally
        {
            if (conn != null)
                try
                {
                    conn.close();
                }
                catch (SQLException | NullPointerException e)
                {
                    logMessage("1781", e);
                }
        }
    }

    /**
     * Pre-V6 used a different hashing system, so recalculate them.
     *
     * @param conn Opened database connection.
     */
    private void upgradeToV6(@NotNull final Connection conn)
    {
        try
        {
            pLogger.warn("Upgrading database to V6!");
            PreparedStatement ps1 = conn.prepareStatement("SELECT * FROM doors;");
            ResultSet rs1 = ps1.executeQuery();
            String update;

            while (rs1.next())
            {
                long UID = rs1.getLong(DOOR_ID);
                int x = rs1.getInt(DOOR_POWER_X);
                int z = rs1.getInt(DOOR_POWER_Z);

                update = "UPDATE doors SET chunkHash=? WHERE id=?;";
                PreparedStatement ps2 = conn.prepareStatement(update);
                ps2.setString(1, Long.toString(Util.simpleChunkHashFromLocation(x, z)));
                ps2.setString(2, Long.toString(UID));
                ps2.executeUpdate();
                ps2.close();
            }
            ps1.close();
            rs1.close();
        }
        catch (SQLException | NullPointerException e)
        {
            logMessage("1420", e);
        }
    }

    private void setDatabaseLock(boolean lockStatus)
    {
        locked.set(lockStatus);
    }

    /*
     * Part of the upgrade to V5 of the database. First lock the database so nothing
     * else can interfere. Then find the temporary name from the fake player (that
     * was stored in the db). All fake names then get replaced by the real names. Or
     * the last ones they used, anyway.
     */
    private void replaceTempPlayerNames()
    {
        Connection conn = null;
        try
        {
            // Database is in a locked state, so a simple getConnection() call won't work.
            // So do it again manually here.
            Class.forName(DRIVER);
            conn = DriverManager.getConnection(url);
            conn.createStatement().execute("PRAGMA foreign_keys=ON");

            ResultSet rs1 = conn.createStatement()
                                .executeQuery("SELECT * FROM players WHERE playerUUID='" + FAKEUUID + "';");
            String fakeName = null;
            while (rs1.next())
                fakeName = rs1.getString("playerName");
            rs1.close();

            ResultSet rs2 = conn.createStatement()
                                .executeQuery("SELECT * FROM players WHERE playerName='" + fakeName + "';");
            while (rs2.next())
            {
                if (rs2.getString("playerUUID").equals(FAKEUUID))
                    continue;
                UUID playerUUID = UUID.fromString(rs2.getString("playerUUID"));
                String playerName = playerRetriever.getOfflinePlayer(playerUUID).getName();

                String update = "UPDATE players SET playerName='" + playerName + "' \n" +
                        "WHERE playerUUID='" + playerUUID.toString() + "';";
                conn.prepareStatement(update).executeUpdate();
            }
            rs2.close();

            String deleteFakePlayer = "DELETE FROM players WHERE playerUUID = '" + FAKEUUID + "';";
            conn.createStatement().executeUpdate(deleteFakePlayer);
        }
        catch (SQLException | ClassNotFoundException e)
        {
            logMessage("1729", e);
        }
        finally
        {
            if (conn != null)
                try
                {
                    conn.close();
                }
                catch (SQLException e)
                {
                    logMessage("1739", e);
                }
            setDatabaseLock(false);
        }
    }

    /**
     * Runs the task to replace all fake player names by their actual names on a separate thread, as it may be a tad
     * slow.
     */
    private void startReplaceTempPlayerNames()
    {
        setDatabaseLock(true);
        new Thread(this::replaceTempPlayerNames).start();
    }

    private void logMessage(String str, Exception e)
    {
        if (!locked.get())
            pLogger.logException(e, str);
        else
            pLogger.warn("Database locked! Failed at: " + str + ". Message: " + e.getMessage());
    }

    /**
     * Remove all new features from the database and reset it to version 0. Useful for testing stuff.
     */
    private void stripToV0()
    {
        Connection conn = null;
        try
        {
            Class.forName(DRIVER);
            conn = DriverManager.getConnection(url);
            conn.createStatement().execute("PRAGMA foreign_keys=OFF");
            conn.setAutoCommit(false);

            // Reset table 'doors'
            {
                conn.createStatement().execute("ALTER TABLE doors RENAME TO doors_old;");

                conn.createStatement()
                    .execute("CREATE TABLE doors" +
                                     "(id            INTEGER    PRIMARY KEY autoincrement," +
                                     " name          TEXT       NOT NULL," +
                                     " world         TEXT       NOT NULL," +
                                     " isOpen        INTEGER    NOT NULL," +
                                     " xMin          INTEGER    NOT NULL," +
                                     " yMin          INTEGER    NOT NULL," +
                                     " zMin          INTEGER    NOT NULL," +
                                     " xMax          INTEGER    NOT NULL," +
                                     " yMax          INTEGER    NOT NULL," +
                                     " zMax          INTEGER    NOT NULL," +
                                     " engineX       INTEGER    NOT NULL," +
                                     " engineY       INTEGER    NOT NULL," +
                                     " engineZ       INTEGER    NOT NULL," +
                                     " isLocked      INTEGER    NOT NULL);");


                conn.createStatement().execute("INSERT INTO doors \n" +
                                                       "SELECT id, name, world, isOpen, xMin, yMin, zMin, xMax, yMax, " +
                                                       "zMax, engineX, engineY, engineZ, isLocked \n" +
                                                       "FROM doors_old;");

                conn.createStatement().execute("DROP TABLE IF EXISTS doors_old;");
                conn.commit();
            }

            // Reset table 'players'
            {
                conn.createStatement().execute("ALTER TABLE players RENAME TO players_old;");

                conn.createStatement()
                    .execute("CREATE TABLE players\n" +
                                     "(id          INTEGER    PRIMARY KEY AUTOINCREMENT,\n" +
                                     " playerUUID  TEXT       NOT NULL);");


                conn.createStatement().execute("INSERT INTO players\n" +
                                                       "SELECT id, playerUUID\n" +
                                                       "FROM players_old;");

                conn.createStatement().execute("DROP TABLE players_old;");
                conn.commit();
            }

            // Reset table 'sqlUnion'
            {
                conn.createStatement().execute("ALTER TABLE sqlUnion RENAME TO sqlUnion_old;");

                conn.createStatement()
                    .execute("CREATE TABLE sqlUnion\n" +
                                     "(id          INTEGER    PRIMARY KEY AUTOINCREMENT,\n" +
                                     " permission  INTEGER    NOT NULL,\n" +
                                     " playerID    REFERENCES players(id) ON UPDATE CASCADE ON DELETE CASCADE,\n" +
                                     " doorUID     REFERENCES doors(id)   ON UPDATE CASCADE ON DELETE CASCADE);");


                conn.createStatement().execute("INSERT INTO sqlUnion\n" +
                                                       "SELECT *\n" +
                                                       "FROM sqlUnion_old;");

                conn.createStatement().execute("DROP TABLE IF EXISTS sqlUnion_old;");
                conn.commit();
            }
            conn.setAutoCommit(true);
            conn.createStatement().execute("PRAGMA foreign_keys=ON");
            setDBVersion(conn, 0);
        }
        catch (SQLException | NullPointerException | ClassNotFoundException e)
        {
            logMessage("1849", e);
        }
        finally
        {
            if (conn != null)
            {
                try
                {
                    conn.close();
                }
                catch (SQLException e)
                {
                    e.printStackTrace();
                }
            }
        }
    }
}
