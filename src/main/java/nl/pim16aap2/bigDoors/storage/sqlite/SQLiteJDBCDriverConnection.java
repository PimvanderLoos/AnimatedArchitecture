package nl.pim16aap2.bigDoors.storage.sqlite;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.Nullable;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import com.google.common.io.Files;

import nl.pim16aap2.bigDoors.BigDoors;
import nl.pim16aap2.bigDoors.Door;
import nl.pim16aap2.bigDoors.moveBlocks.Opener;
import nl.pim16aap2.bigDoors.util.DoorDirection;
import nl.pim16aap2.bigDoors.util.DoorOwner;
import nl.pim16aap2.bigDoors.util.DoorType;
import nl.pim16aap2.bigDoors.util.RotateDirection;
import nl.pim16aap2.bigDoors.util.Util;

@SuppressWarnings("null") // Eclipse likes to complain about connections potentially being null,
                          // but it's not a problem.
public class SQLiteJDBCDriverConnection
{
    private final BigDoors plugin;
    private final File dbFile;
    private final String url;
    private static final String DRIVER = "org.sqlite.JDBC";

    // The highest database version. If the found db version matches or exceeds
    // this version, the database cannot be enabled.
    private static final int MAX_DATABASE_VERSION = 10;
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

    private static final int PLAYERS_ID = 1;
    private static final int PLAYERS_UUID = 2;
    private static final int PLAYERS_NAME = 3;

    @SuppressWarnings("unused")
    private static final int UNION_ID = 1;
    private static final int UNION_PERM = 2;
    private static final int UNION_PLAYER_ID = 3;
    private static final int UNION_DOOR_ID = 4;

    private final String dbName;
    private boolean enabled = true;
    private boolean validVersion = true;
    private AtomicBoolean locked = new AtomicBoolean(false);
    private static final String FAKEUUID = "0000";

    public SQLiteJDBCDriverConnection(final BigDoors plugin, final String dbName)
    {
        this.plugin = plugin;
        this.dbName = dbName;
        dbFile = new File(plugin.getDataFolder(), dbName);
        url = "jdbc:sqlite:" + dbFile;
        init();
        upgrade();
    }

    // Establish a connection.
    private Connection getConnection()
    {
        if (locked.get())
        {
            plugin.getMyLogger()
                .logMessage("Database locked! Please try again later! Please contact pim16aap2 if the issue persists.",
                            true, false);
            return null;
        }
        if (!validVersion)
        {
            plugin.getMyLogger().logMessage("Database disabled! Reason: Version too high! Please update the plugin!",
                                            true, false);
            return null;
        }
        if (!enabled)
        {
            plugin.getMyLogger()
                .logMessage("Database disabled! This probably means an upgrade failed! Please contact pim16aap2.", true,
                            false);
            return null;
        }

        Connection conn = null;
        try
        {
            Class.forName(DRIVER);
            conn = DriverManager.getConnection(url);
            conn.createStatement().execute("PRAGMA foreign_keys=ON");
        }
        catch (SQLException | NullPointerException ex)
        {
            plugin.getMyLogger().logMessage("53: Failed to open connection!", true, false);
        }
        catch (ClassNotFoundException e)
        {
            plugin.getMyLogger().logMessage("57: Failed to open connection: CLass not found!!", true, false);
        }
        return conn;
    }

    private Connection getConnectionUnsafe()
    {
        Connection conn = null;
        try
        {
            Class.forName(DRIVER);
            conn = DriverManager.getConnection(url);
            conn.createStatement().execute("PRAGMA foreign_keys=ON");
        }
        catch (SQLException | NullPointerException ex)
        {
            plugin.getMyLogger().logMessage("148: Failed to open connection!", true, false);
        }
        catch (ClassNotFoundException e)
        {
            plugin.getMyLogger().logMessage("152: Failed to open connection: CLass not found!!", true, false);
        }
        return conn;
    }

    // Initialize the tables.
    private void init()
    {
        if (!dbFile.exists())
            try
            {
                dbFile.createNewFile();
                plugin.getMyLogger().logMessageToLogFile("New file created at " + dbFile);
            }
            catch (IOException e)
            {
                plugin.getMyLogger().logMessageToLogFile("File write error: " + dbFile);
            }

        // Table creation
        Connection conn = null;
        try
        {
            conn = getConnection();

            // Check if the doors table already exists. If it does, assume the rest exists
            // as well
            // and don't set it up.
            if (!conn.getMetaData().getTables(null, null, "doors", new String[] { "TABLE" }).next())
            {
                Statement stmt1 = conn.createStatement();
                String sql1 = "CREATE TABLE IF NOT EXISTS doors "
                    + "(id            INTEGER    PRIMARY KEY autoincrement, " + " name          TEXT       NOT NULL, "
                    + " world         TEXT       NOT NULL, " + " isOpen        INTEGER    NOT NULL, "
                    + " xMin          INTEGER    NOT NULL, " + " yMin          INTEGER    NOT NULL, "
                    + " zMin          INTEGER    NOT NULL, " + " xMax          INTEGER    NOT NULL, "
                    + " yMax          INTEGER    NOT NULL, " + " zMax          INTEGER    NOT NULL, "
                    + " engineX       INTEGER    NOT NULL, " + " engineY       INTEGER    NOT NULL, "
                    + " engineZ       INTEGER    NOT NULL, " + " isLocked      INTEGER    NOT NULL, "
                    + " type          INTEGER    NOT NULL, " + " engineSide    INTEGER    NOT NULL, "
                    + " powerBlockX   INTEGER    NOT NULL, " + " powerBlockY   INTEGER    NOT NULL, "
                    + " powerBlockZ   INTEGER    NOT NULL, " + " openDirection INTEGER    NOT NULL, "
                    + " autoClose     INTEGER    NOT NULL, " + " chunkHash     INTEGER    NOT NULL, "
                    + " blocksToMove  INTEGER    NOT NULL) ";
                stmt1.executeUpdate(sql1);
                stmt1.close();

                Statement stmt2 = conn.createStatement();
                String sql2 = "CREATE TABLE IF NOT EXISTS players \n"
                    + "(id          INTEGER    PRIMARY KEY AUTOINCREMENT, \n" + " playerUUID  TEXT       NOT NULL, \n"
                    + " playerName  TEXT       NOT NULL, \n" + " unique(playerUUID));";
                stmt2.executeUpdate(sql2);
                stmt2.close();

                Statement stmt3 = conn.createStatement();
                String sql3 = "CREATE TABLE IF NOT EXISTS sqlUnion "
                    + "(id          INTEGER    PRIMARY KEY AUTOINCREMENT, " + " permission  INTEGER    NOT NULL, "
                    + " playerID    REFERENCES players(id) ON UPDATE CASCADE ON DELETE CASCADE, "
                    + " doorUID     REFERENCES doors(id)   ON UPDATE CASCADE ON DELETE CASCADE,"
                    + " unique (playerID, doorUID))";
                stmt3.executeUpdate(sql3);
                stmt3.close();
                setDBVersion(conn, DATABASE_VERSION);
            }
        }
        catch (SQLException | NullPointerException e)
        {
            logMessage("203", e);
        }
        finally
        {
            try
            {
                conn.close();
            }
            catch (SQLException | NullPointerException e)
            {
                logMessage("213", e);
            }
            catch (Exception e)
            {
                logMessage("217", e);
            }
        }
    }

    // Prepares the database for V2 of this plugin.
    // This means replacing all occurrences of OpenDirection == RotateDirection.NONE
    // for now.
    // This may change to include more changes in the future.
    public void prepareForV2()
    {
        if (getDatabaseVersion() != DATABASE_VERSION)
        {
            plugin.getMyLogger()
                .logMessage("Failed to upgrade database. Reason: Invalid version. Is the plugin up to date?", true,
                            true);
            return;
        }

        plugin.getMyLogger()
            .logMessage("Upgrading database to v2 of BigDoors! Creating backup first! (ignores settings)", true, true);
        if (!makeBackup(".BACKUPOFV1"))
        {
            plugin.getMyLogger()
                .logMessage("Failed to make a backup! Aborting upgrade process!! Please contact pim16aap2.", true,
                            true);
            return;
        }
        long startTime = System.nanoTime();
        locked.set(true);

        // Update the database version, to make sure it cannot be loaded again once the
        // upgrade has been started, even if it failed.
        try (Connection conn = getConnectionUnsafe())
        {
            setDBVersion(conn, MAX_DATABASE_VERSION);
        }
        catch (SQLException | NullPointerException e)
        {
            logMessage("486", e);
        }

        // Make sure there aren't any NONE or invalid open directions and upgrade ALL
        // drawbridge open directions.
        // v1 drawbridges use Clockwise/CounterClockwise while v2 drawbridges use
        // north/south/east/west.
        try (Connection conn = getConnectionUnsafe())
        {
            plugin.getMyLogger().warn("Upgrading database: Replacing now-invalid open directions!");
            // select all drawbridges. This is the only type that actually uses the
            // engineSide variable.
            PreparedStatement ps = conn.prepareStatement("SELECT * FROM doors;");
            ResultSet rs = ps.executeQuery();

            int totalCount = conn.prepareStatement("SELECT COUNT(*) AS count FROM doors").executeQuery()
                .getInt("count");
            plugin.getMyLogger().info("Going to process " + totalCount + " doors.");
            int processed = 0;
            int tenPercent = totalCount / 10; // Cast to an integer. Won't be exact, but close enough. It's just
                                              // cosmetic anyway.

            while (rs.next())
            {
                if ((++processed) % tenPercent == 0)
                    plugin.getMyLogger().info(String.format("Processing doors: %3d%%", (10 * processed / tenPercent)));

                if (rs.getInt("type") > 5) // This shouldn't do anything for regular users, but it's useful for me, as I
                                           // have future types enabled.
                    continue;

                // Disregard current open direction. It should be somewhat stored in the
                // engineSide.
                // When it is opened in the northern direction, the engineSide will be South,
                // even when closed (= upright).
                Door door = newDoorFromRS(rs, rs.getLong("id"), 0, null, null);
                RotateDirection currentOpenDir = door.getOpenDir();

                Opener opener = plugin.getDoorOpener(door.getType());
                if (door.getType().equals(DoorType.DRAWBRIDGE) || currentOpenDir.equals(RotateDirection.NONE) ||
                    !opener.isRotateDirectionValid(door))
                {
                    RotateDirection newOpenDir = opener.getRotateDirection(door);
                    door.setOpenDir(newOpenDir);

                    PreparedStatement ps2 = conn.prepareStatement("UPDATE doors SET openDirection=? WHERE id=?;");
                    ps2.setInt(1, RotateDirection.getValue(newOpenDir));
                    ps2.setString(2, Long.toString(rs.getLong("id")));
                    ps2.executeUpdate();
                    ps2.close();
                }

                if (door.getOpenDir().equals(RotateDirection.NONE))
                    plugin.getMyLogger().logMessage("Door " + rs.getLong("id")
                        + " is in an invalid state! Please contact pim16aap2 or delete this door!", true, false);
            }
            rs.close();
            ps.close();
            plugin.getMyLogger().info("All doors have been processed! Onto the next step!");
        }
        catch (SQLException | NullPointerException e)
        {
            logMessage("314", e);
        }

        try (Connection conn = getConnectionUnsafe())
        {
            plugin.getMyLogger().warn("Upgrading database: Adding BitFlag!");

            String addColumn = "ALTER TABLE doors ADD COLUMN bitflag INTEGER NOT NULL DEFAULT 0";
            conn.createStatement().execute(addColumn);

            PreparedStatement ps1 = conn.prepareStatement("SELECT * FROM doors;");
            ResultSet rs1 = ps1.executeQuery();
            String update;

            while (rs1.next())
            {
                long UID = rs1.getLong("id");
                boolean isOpen = rs1.getBoolean("isOpen");
                boolean isLocked = rs1.getBoolean("isLocked");

                // Hardcoded values because v1 doesn't have bitflags and the correct enums.
                int flag = 0;
                if (isOpen)
                    flag |= 1;
                if (isLocked)
                    flag |= 2;

                update = "UPDATE doors SET bitflag=? WHERE id=?;";
                PreparedStatement ps2 = conn.prepareStatement(update);
                ps2.setString(1, Integer.toString(flag));
                ps2.setString(2, Long.toString(UID));
                ps2.executeUpdate();
                ps2.close();
            }
            ps1.close();
            rs1.close();
        }
        catch (SQLException | NullPointerException e)
        {
            logMessage("410", e);
        }

        // Remove isOpen, isLocked, and engineSide from the doors table.
        Connection conn = null;
        try
        {
            plugin.getMyLogger().warn("Upgrading database: Removing isOpen, isLocked, engineSide!");
            Class.forName(DRIVER);
            conn = DriverManager.getConnection(url);
//            conn.createStatement().execute("PRAGMA foreign_keys=OFF");
            disableForeignKeys(conn);
            conn.setAutoCommit(false);
            conn.createStatement().execute("ALTER TABLE doors RENAME TO doors_old;");

            String newDoors = "CREATE TABLE IF NOT EXISTS doors\n"
                + "(id            INTEGER    PRIMARY KEY autoincrement,\n" + " name          TEXT       NOT NULL,\n"
                + " world         TEXT       NOT NULL,\n" + " xMin          INTEGER    NOT NULL,\n"
                + " yMin          INTEGER    NOT NULL,\n" + " zMin          INTEGER    NOT NULL,\n"
                + " xMax          INTEGER    NOT NULL,\n" + " yMax          INTEGER    NOT NULL,\n"
                + " zMax          INTEGER    NOT NULL,\n" + " engineX       INTEGER    NOT NULL,\n"
                + " engineY       INTEGER    NOT NULL,\n" + " engineZ       INTEGER    NOT NULL,\n"
                + " bitflag       INTEGER    NOT NULL DEFAULT 0,\n" + " type          INTEGER    NOT NULL DEFAULT 0,\n"
                + " powerBlockX   INTEGER    NOT NULL DEFAULT -1,\n"
                + " powerBlockY   INTEGER    NOT NULL DEFAULT -1,\n"
                + " powerBlockZ   INTEGER    NOT NULL DEFAULT -1,\n"
                + " openDirection INTEGER    NOT NULL DEFAULT  0,\n"
                + " autoClose     INTEGER    NOT NULL DEFAULT -1,\n"
                + " chunkHash     INTEGER    NOT NULL DEFAULT -1,\n"
                + " blocksToMove  INTEGER    NOT NULL DEFAULT -1);";
            conn.createStatement().execute(newDoors);

            String restoreData = "INSERT INTO doors\n" + "SELECT id, name, world, xMin, yMin, zMin, xMax, yMax, "
                + "zMax, engineX, engineY, engineZ, bitflag, type, \n"
                + "powerBlockX, powerBlockY, powerBlockZ, openDirection, autoClose, chunkHash, blocksToMove\n"
                + "FROM doors_old;";
            conn.createStatement().execute(restoreData);

            conn.createStatement().execute("DROP TABLE IF EXISTS 'doors_old';");
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
                catch (SQLException | NullPointerException e1)
                {
                    logMessage("391", e1);
                }
            logMessage("421", e);
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
                    logMessage("432", e);
                }
        }
        recreateTables();
        locked.set(false);
        validVersion = false;

        long endTime = System.nanoTime();
        long duration = (endTime - startTime) / 1000000;
        plugin.getMyLogger()
            .warn("Database upgrade completed in " + duration + "ms! Please update BigDoors to v2 now!");
    }

    /**
     * Because SQLite is a PoS and decided to remove the admittedly odd behavior
     * that just disabling foreign keys suddenly ignored all the triggers etc
     * attached to it without actually providing a proper alternative (perhaps
     * implement ALTER TABLE properly??), this method needs to be called now in
     * order to safely modify stuff without having the foreign keys get fucked up.
     *
     * @param conn The connection.
     */
    private void disableForeignKeys(final Connection conn) throws SQLException
    {
        conn.createStatement().execute("PRAGMA foreign_keys=OFF");
        conn.createStatement().execute("PRAGMA legacy_alter_table=ON");
    }

    /**
     * The anti method of {@link #disableForeignKeys(Connection)}. Only needs to be
     * called if that was called first.
     *
     * @param conn The connection.
     * @throws SQLException
     */
    @SuppressWarnings("unused")
    private void reEnableForeignKeys(final Connection conn) throws SQLException
    {
        conn.createStatement().execute("PRAGMA foreign_keys=ON");
        conn.createStatement().execute("PRAGMA legacy_alter_table=OFF");
    }

    // To make sure there aren't any string values hidden as integers and what-not,
    // recreate all tables before moving on to v2.
    public void recreateTables()
    {
        {
            // recreate the doors table.
            Connection conn = null;
            try
            {
                conn = DriverManager.getConnection(url);
                disableForeignKeys(conn);
                plugin.getMyLogger().warn("Upgrading database: Recreating doors table now!");

                conn.createStatement().execute("ALTER TABLE doors RENAME TO doors_old;");
                conn.createStatement()
                    .execute("CREATE TABLE IF NOT EXISTS doors\n"
                        + "(id            INTEGER    PRIMARY KEY autoincrement,\n"
                        + " name          TEXT       NOT NULL,\n" + " world         TEXT       NOT NULL,\n"
                        + " xMin          INTEGER    NOT NULL,\n" + " yMin          INTEGER    NOT NULL,\n"
                        + " zMin          INTEGER    NOT NULL,\n" + " xMax          INTEGER    NOT NULL,\n"
                        + " yMax          INTEGER    NOT NULL,\n" + " zMax          INTEGER    NOT NULL,\n"
                        + " engineX       INTEGER    NOT NULL,\n" + " engineY       INTEGER    NOT NULL,\n"
                        + " engineZ       INTEGER    NOT NULL,\n" + " bitflag       INTEGER    NOT NULL DEFAULT 0,\n"
                        + " type          INTEGER    NOT NULL DEFAULT  0,\n"
                        + " powerBlockX   INTEGER    NOT NULL DEFAULT -1,\n"
                        + " powerBlockY   INTEGER    NOT NULL DEFAULT -1,\n"
                        + " powerBlockZ   INTEGER    NOT NULL DEFAULT -1,\n"
                        + " openDirection INTEGER    NOT NULL DEFAULT  0,\n"
                        + " autoClose     INTEGER    NOT NULL DEFAULT -1,\n"
                        + " chunkHash     INTEGER    NOT NULL DEFAULT -1,\n"
                        + " blocksToMove  INTEGER    NOT NULL DEFAULT -1);");

                ResultSet rs1 = conn.prepareStatement("SELECT * FROM doors_old;").executeQuery();
                while (rs1.next())
                {
                    String insert = "INSERT INTO doors(id, name,world,xMin,yMin,zMin,xMax,yMax,zMax,\n"
                        + "                  engineX,engineY,engineZ,bitflag,type,\n"
                        + "                  powerBlockX,powerBlockY,powerBlockZ,openDirection,\n"
                        + "                  autoClose,chunkHash,blocksToMove) \n"
                        + "                  VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?);";
                    PreparedStatement insertStatement = conn.prepareStatement(insert);
                    insertStatement.setLong(1, rs1.getLong("id"));
                    insertStatement.setString(2, rs1.getString("name"));
                    insertStatement.setString(3, rs1.getString("world"));
                    insertStatement.setLong(4, rs1.getLong("xMin"));
                    insertStatement.setLong(5, rs1.getLong("yMin"));
                    insertStatement.setLong(6, rs1.getLong("zMin"));
                    insertStatement.setLong(7, rs1.getLong("xMax"));
                    insertStatement.setLong(8, rs1.getLong("yMax"));
                    insertStatement.setLong(9, rs1.getLong("zMax"));
                    insertStatement.setLong(10, rs1.getLong("engineX"));
                    insertStatement.setLong(11, rs1.getLong("engineY"));
                    insertStatement.setLong(12, rs1.getLong("engineZ"));
                    insertStatement.setLong(13, rs1.getLong("bitflag"));
                    insertStatement.setLong(14, rs1.getLong("type"));
                    insertStatement.setLong(15, rs1.getLong("powerBlockX"));
                    insertStatement.setLong(16, rs1.getLong("powerBlockY"));
                    insertStatement.setLong(17, rs1.getLong("powerBlockZ"));
                    insertStatement.setLong(18, rs1.getLong("openDirection"));
                    insertStatement.setLong(19, rs1.getLong("autoClose"));
                    insertStatement.setLong(20, rs1.getLong("chunkHash"));
                    insertStatement.setLong(21, rs1.getLong("blocksToMove"));
                    insertStatement.executeUpdate();
                    insertStatement.close();
                }
                rs1.close();
                conn.createStatement().execute("DROP TABLE IF EXISTS 'doors_old';");

                plugin.getMyLogger().info("Doors table has been recreated! On the the next step!");
            }
            catch (SQLException | NullPointerException e)
            {
                logMessage("314", e);
            }
            finally
            {
                try
                {
                    conn.close();
                }
                catch (SQLException e)
                {
                    logMessage("596", e);
                }
            }
        }

        {
            // Recreate the players table.
            Connection conn = null;
            try
            {
                conn = DriverManager.getConnection(url);
                disableForeignKeys(conn);
                plugin.getMyLogger().warn("Upgrading database: Recreating players table now!");

                conn.createStatement().execute("ALTER TABLE players RENAME TO players_old;");
                conn.createStatement().execute("CREATE TABLE IF NOT EXISTS players \n"
                    + "(id          INTEGER    PRIMARY KEY AUTOINCREMENT, \n" + " playerUUID  TEXT       NOT NULL, \n"
                    + " playerName  TEXT       NOT NULL, \n" + " unique(playerUUID));");

                ResultSet rs1 = conn.prepareStatement("SELECT * FROM players_old;").executeQuery();
                while (rs1.next())
                {
                    String insert = "INSERT INTO players(id, playerUUID, playerName) VALUES(?,?,?);";
                    PreparedStatement insertStatement = conn.prepareStatement(insert);
                    insertStatement.setLong(1, rs1.getLong("id"));
                    insertStatement.setString(2, rs1.getString("playerUUID"));
                    insertStatement.setString(3, rs1.getString("playerName"));
                    insertStatement.executeUpdate();
                    insertStatement.close();
                }
                rs1.close();
                conn.createStatement().execute("DROP TABLE IF EXISTS 'players_old';");

                plugin.getMyLogger().info("Players table has been recreated! On the the next step!");
            }
            // Recreate sqlUnion table. This is done last, because of the FK's.
            catch (SQLException | NullPointerException e)
            {
                logMessage("314", e);
            }
            finally
            {
                try
                {
                    conn.close();
                }
                catch (SQLException e)
                {
                    logMessage("618", e);
                }
            }
        }

        {
            Connection conn = null;
            try
            {
                conn = DriverManager.getConnection(url);
                disableForeignKeys(conn);
                plugin.getMyLogger().warn("Upgrading database: Recreating slqUnion table now!");

                conn.createStatement().execute("ALTER TABLE sqlUnion RENAME TO sqlUnion_old;");

                conn.createStatement()
                    .execute("CREATE TABLE IF NOT EXISTS sqlUnion\n"
                        + "(id          INTEGER    PRIMARY KEY AUTOINCREMENT,\n" + " permission  INTEGER    NOT NULL,\n"
                        + " playerID    REFERENCES players(id) ON UPDATE CASCADE ON DELETE CASCADE,\n"
                        + " doorUID     REFERENCES doors(id)   ON UPDATE CASCADE ON DELETE CASCADE, \n"
                        + " unique (playerID, doorUID));");

                ResultSet rs1 = conn.prepareStatement("SELECT * FROM sqlUnion_old;").executeQuery();
                while (rs1.next())
                {
                    String insert = "INSERT INTO sqlUnion(id, permission, playerID, doorUID) VALUES(?,?,?,?);";
                    PreparedStatement insertStatement = conn.prepareStatement(insert);
                    insertStatement.setLong(1, rs1.getLong("id"));
                    insertStatement.setLong(2, rs1.getLong("permission"));
                    insertStatement.setLong(3, rs1.getLong("playerID"));
                    insertStatement.setLong(4, rs1.getLong("doorUID"));
                    insertStatement.executeUpdate();
                    insertStatement.close();
                }
                rs1.close();

                conn.createStatement().execute("DROP TABLE IF EXISTS 'sqlUnion_old';");

                plugin.getMyLogger().info("slqUnion table has been recreated! On the the next step!");
            }
            catch (SQLException | NullPointerException e)
            {
                logMessage("314", e);
            }
            finally
            {
                try
                {
                    conn.close();
                }
                catch (SQLException e)
                {
                    logMessage("670", e);
                }
            }
        }
    }

    private long getPlayerID(final Connection conn, final String playerUUID) throws SQLException
    {
        long playerID = -1;
        PreparedStatement ps1 = conn.prepareStatement("SELECT * FROM players WHERE playerUUID = '" + playerUUID + "';");
        ResultSet rs1 = ps1.executeQuery();
        while (rs1.next())
            playerID = rs1.getLong(PLAYERS_ID);
        ps1.close();
        rs1.close();
        return playerID;
    }

    // Get the permission level for a given player for a given door.
    public int getPermission(final String playerUUID, final long doorUID)
    {
        Connection conn = null;
        int ret = -1;
        try
        {
            conn = getConnection();
            long playerID = getPlayerID(conn, playerUUID);
            if (playerID == -1)
                return -1;

            // Select all doors from the sqlUnion table that have the previously found
            // player as owner.
            PreparedStatement ps2 = conn.prepareStatement("SELECT * FROM sqlUnion WHERE playerID = '" + playerID
                + "' AND doorUID = '" + doorUID + "';");
            ResultSet rs2 = ps2.executeQuery();
            while (rs2.next())
                ret = rs2.getInt(UNION_PERM);
            ps2.close();
            rs2.close();
        }
        catch (SQLException | NullPointerException e)
        {
            logMessage("244", e);
        }
        finally
        {
            try
            {
                conn.close();
            }
            catch (SQLException | NullPointerException e)
            {
                logMessage("254", e);
            }
        }

        return ret;
    }

    // Gets a histogram of number of doors per type.
    public Map<DoorType, Integer> getDatabaseStatistics()
    {
        Map<DoorType, Integer> stats = new EnumMap<>(DoorType.class);

        try (Connection conn = getConnection();
            PreparedStatement ps = conn
                .prepareStatement("SELECT type, COUNT(type) AS count FROM doors GROUP BY type ORDER BY type;");
            ResultSet rs = ps.executeQuery();)
        {
            while (rs.next())
            {
                DoorType type = DoorType.valueOf(rs.getInt("type"));
                if (type == null)
                    continue;

                int count = rs.getInt("count");
                stats.put(type, count);
            }
        }
        catch (SQLException | NullPointerException e)
        {
            logMessage("747", e);
        }

        return stats;
    }

    // Construct a new door from a resultset.
    private Door newDoorFromRS(final ResultSet rs, final long doorUID, final int permission, final UUID playerUUID,
                               final String playerName)
    {
        try
        {
            World world = Bukkit.getServer().getWorld(UUID.fromString(rs.getString(DOOR_WORLD)));
            Location min = new Location(world, rs.getInt(DOOR_MIN_X), rs.getInt(DOOR_MIN_Y), rs.getInt(DOOR_MIN_Z));
            Location max = new Location(world, rs.getInt(DOOR_MAX_X), rs.getInt(DOOR_MAX_Y), rs.getInt(DOOR_MAX_Z));
            Location engine = new Location(world, rs.getInt(DOOR_ENG_X), rs.getInt(DOOR_ENG_Y), rs.getInt(DOOR_ENG_Z));
            Location powerB = new Location(world, rs.getInt(DOOR_POWER_X), rs.getInt(DOOR_POWER_Y),
                                           rs.getInt(DOOR_POWER_Z));

            Door door = new Door(playerUUID, playerName, world, min, max, engine, rs.getString(DOOR_NAME),
                                 (rs.getInt(DOOR_OPEN) == 1 ? true : false), doorUID,
                                 (rs.getInt(DOOR_LOCKED) == 1 ? true : false), permission,
                                 DoorType.valueOf(rs.getInt(DOOR_TYPE)),
                                 DoorDirection.valueOf(rs.getInt(DOOR_ENG_SIDE)), powerB,
                                 RotateDirection.valueOf(rs.getInt(DOOR_OPEN_DIR)), rs.getInt(DOOR_AUTO_CLOSE));

            door.setBlocksToMove(rs.getInt(DOOR_BLOCKS_TO_MOVE));
            return door;
        }
        catch (SQLException | NullPointerException e)
        {
            logMessage("282", e);
            return null;
        }
    }

    // Remove a door with a given ID.
    public long removeDoor(final long doorID)
    {
        long hash = 0;
        Connection conn = null;
        try
        {
            conn = getConnection();
            conn.setAutoCommit(false);
            PreparedStatement ps0 = conn.prepareStatement("SELECT chunkHash FROM doors WHERE id = ?;");
            ps0.setLong(1, doorID);
            ResultSet rs0 = ps0.executeQuery();
            if (rs0.next())
            {
                hash = rs0.getLong("chunkHash");
                String deleteDoor = "DELETE FROM doors WHERE id = '" + doorID + "';";
                PreparedStatement ps = conn.prepareStatement(deleteDoor);
                ps.executeUpdate();
                ps.close();
            }

            ps0.close();
            rs0.close();
            conn.commit();
        }
        catch (SQLException | NullPointerException e)
        {
            plugin.getMyLogger().logMessageToLogFile("271: " + Util.exceptionToString(e));
        }
        finally
        {
            try
            {
                conn.close();
            }
            catch (SQLException | NullPointerException e)
            {
                logMessage("311", e);
            }
        }
        return hash;
    }

    // Remove all doors from a given world.
    public List<Long> removeDoorsFromWorld(final World world)
    {
        List<Long> hashes = new ArrayList<>();
        Connection conn = null;
        try
        {
            conn = getConnection();
            conn.setAutoCommit(false);

            PreparedStatement ps0 = conn.prepareStatement("SELECT chunkHash doors WHERE world = ?;");
            ps0.setString(1, world.getUID().toString());
            ResultSet rs0 = ps0.executeQuery();

            while (rs0.next())
                hashes.add(rs0.getLong("chunkHash"));

            ps0.close();
            rs0.close();

            PreparedStatement ps = conn.prepareStatement("DELETE FROM doors WHERE world = ?;");
            ps.setString(1, world.getUID().toString());

            ps.executeUpdate();
            ps.close();

            conn.commit();
        }
        catch (SQLException | NullPointerException e)
        {
            plugin.getMyLogger().logMessageToLogFile("271: " + Util.exceptionToString(e));
        }
        finally
        {
            try
            {
                conn.close();
            }
            catch (SQLException | NullPointerException e)
            {
                logMessage("807", e);
            }
        }
        return hashes;
    }

    // Remove a door with a given name, owned by a certain player.
    public void removeDoor(final String playerUUID, final String doorName)
    {
        Connection conn = null;
        try
        {
            conn = getConnection();
            long playerID = getPlayerID(conn, playerUUID);
            if (playerID == -1)
                return;

            // Select all doors from the sqlUnion table that have the previously found
            // player as owner.
            PreparedStatement ps2 = conn
                .prepareStatement("SELECT * FROM sqlUnion WHERE playerID = '" + playerID + "';");
            ResultSet rs2 = ps2.executeQuery();
            while (rs2.next())
            {
                // Delete all doors with the provided name owned by the provided player.
                PreparedStatement ps3 = conn.prepareStatement("DELETE FROM doors WHERE id = '"
                    + rs2.getInt(UNION_DOOR_ID) + "' AND name = '" + doorName + "';");
                ps3.executeUpdate();
                ps3.close();
            }
            ps2.close();
            rs2.close();
        }
        catch (SQLException | NullPointerException e)
        {
            logMessage("343", e);
        }
        finally
        {
            try
            {
                conn.close();
            }
            catch (SQLException | NullPointerException e)
            {
                logMessage("353", e);
            }
        }
    }

    // Get Door from a doorID.
    public Door getDoor(@Nullable UUID playerUUID, final long doorUID)
    {
        Door door = null;

        Connection conn = null;
        try
        {
            conn = getConnection();
            int permission = -1;
            String playerName;

            // If no player is specified, get the lowest tier permission and the original
            // creator.
            if (playerUUID == null)
            {
                permission = 2;
                DoorOwner doorOwner = getOwnerOfDoor(conn, doorUID);
                playerUUID = doorOwner.getPlayerUUID();
                playerName = doorOwner.getPlayerName();
            }
            else
            {
                long playerID = getPlayerID(conn, playerUUID.toString());

                if (playerID == -1)
                    return null;

                playerName = getPlayerName(playerUUID);

                // Select all doors from the sqlUnion table that have the previously found
                // player as owner.
                PreparedStatement ps2 = conn.prepareStatement("SELECT * FROM sqlUnion WHERE playerID = '" + playerID
                    + "' AND doorUID = '" + doorUID + "';");
                ResultSet rs2 = ps2.executeQuery();

                while (rs2.next())
                    permission = rs2.getInt(UNION_PERM);

                ps2.close();
                rs2.close();

                if (permission == -1)
                    return null;
            }

            PreparedStatement ps3 = conn.prepareStatement("SELECT * FROM doors WHERE id = '" + doorUID + "';");
            ResultSet rs3 = ps3.executeQuery();

            while (rs3.next())
                door = newDoorFromRS(rs3, doorUID, permission, playerUUID, playerName);

            ps3.close();
            rs3.close();
        }
        catch (SQLException | NullPointerException e)
        {
            logMessage("521", e);
        }
        finally
        {
            try
            {
                conn.close();
            }
            catch (SQLException | NullPointerException e)
            {
                logMessage("531", e);
            }
        }
        return door;
    }

    // Get a set of all doors in the database. This set is defined by doorUID.
    // Since a door can have more than one owner the creator is returned as player
    public Set<Door> getDoors()
    {
        Set<Door> doors = new HashSet<>();
        try (Connection conn = getConnection();
             PreparedStatement stmp = conn.prepareStatement("SELECT p.playername, p.playeruuid, d.* from (SELECT DISTINCT(d.id), u.playerid, d.* FROM doors d left join sqlUnion u on d.id = u.doorUID where u.permission = 0) d left join players p on p.id = d.playerid"))
        {
            ResultSet rs = stmp.executeQuery();
            while (rs.next())
            {
                doors.add(newDoorFromRS(rs, rs.getInt(DOOR_ID), 0,
                        UUID.fromString(rs.getString("playerUUID")), rs.getString("playername")));
            }
        } catch (SQLException | NullPointerException e)
        {
            // TODO: Add appropriate error code
            logMessage("", e);
            return Collections.emptySet();
        }
        return doors;
    }


    // Get ALL doors owned by a given playerUUID.
    public ArrayList<Door> getDoors(final String playerUUID, final String name)
    {
        return getDoors(playerUUID, name, 0, Long.MAX_VALUE);
    }

    // Get all doors with a given name.
    public ArrayList<Door> getDoors(final String name)
    {
        ArrayList<Door> doors = new ArrayList<>();

        Connection conn = null;
        try
        {
            conn = getConnection();

            PreparedStatement ps1 = conn.prepareStatement("SELECT * FROM doors WHERE name = '" + name + "';");
            ResultSet rs1 = ps1.executeQuery();

            while (rs1.next())
            {
                String foundPlayerUUID = null;
                String playerName = null;
                int permission = -1;
                PreparedStatement ps2 = conn
                    .prepareStatement("SELECT * FROM sqlUnion WHERE doorUID = '" + rs1.getLong(DOOR_ID) + "';");
                ResultSet rs2 = ps2.executeQuery();
                while (rs2.next())
                {
                    permission = rs2.getInt(UNION_PERM);
                    PreparedStatement ps3 = conn
                        .prepareStatement("SELECT * FROM players WHERE id = '" + rs2.getInt(UNION_PLAYER_ID) + "';");
                    ResultSet rs3 = ps3.executeQuery();
                    while (rs3.next())
                    {
                        foundPlayerUUID = rs3.getString(PLAYERS_UUID);
                        playerName = rs3.getString(PLAYERS_NAME);
                    }
                    ps3.close();
                    rs3.close();
                }
                ps2.close();
                rs2.close();

                doors.add(newDoorFromRS(rs1, rs1.getLong(DOOR_ID), permission, UUID.fromString(foundPlayerUUID),
                                        playerName));
            }
            ps1.close();
            rs1.close();
        }
        catch (SQLException | NullPointerException e)
        {
            logMessage("582", e);
        }
        finally
        {
            try
            {
                conn.close();
            }
            catch (SQLException | NullPointerException e)
            {
                logMessage("592", e);
            }
        }
        return doors;
    }

    // Get all doors associated with this player in a given range. Name can be null
    public ArrayList<Door> getDoors(final String playerUUID, final String name, final long start, final long end)
    {
        ArrayList<Door> doors = new ArrayList<>();

        Connection conn = null;
        try
        {
            conn = getConnection();
            long playerID = getPlayerID(conn, playerUUID);
            String playerName = getPlayerName(UUID.fromString(playerUUID));

            PreparedStatement ps2 = conn
                .prepareStatement("SELECT * FROM sqlUnion WHERE playerID = '" + playerID + "';");
            ResultSet rs2 = ps2.executeQuery();
            int count = 0;
            while (rs2.next())
            {
                PreparedStatement ps3 = conn
                    .prepareStatement("SELECT * FROM doors WHERE id = '" + rs2.getInt(UNION_DOOR_ID) + "';");
                ResultSet rs3 = ps3.executeQuery();

                while (rs3.next())
                {
                    if ((name == null || rs3.getString(DOOR_NAME).equals(name)) && count >= start && count <= end)
                        doors.add(newDoorFromRS(rs3, rs3.getLong(DOOR_ID), rs2.getInt(UNION_PERM),
                                                UUID.fromString(playerUUID), playerName));
                    ++count;
                }
                ps3.close();
                rs3.close();
            }
            ps2.close();
            rs2.close();
        }
        catch (SQLException | NullPointerException e)
        {
            logMessage("631", e);
        }
        finally
        {
            try
            {
                conn.close();
            }
            catch (SQLException | NullPointerException e)
            {
                logMessage("641", e);
            }
        }
        return doors;
    }

    // Get all doors associated with this player in a given range. Name can be null
    public ArrayList<Door> getDoorsInWorld(final World world)
    {
        ArrayList<Door> doors = new ArrayList<>();

        Connection conn = null;
        try
        {
            conn = getConnection();
            PreparedStatement ps = conn.prepareStatement("SELECT * FROM doors WHERE world = ?;");
            ps.setString(1, world.getUID().toString());

            ResultSet rs = ps.executeQuery();
            while (rs.next())
            {
                long doorUID = rs.getLong(DOOR_ID);
                DoorOwner doorOwner = getOwnerOfDoor(conn, doorUID);
                if (doorOwner == null)
                {
                    plugin.getMyLogger().logMessageToConsole("Failed to obtain doorOwner of door: " + doorUID
                        + ". This door cannot be constructed!");
                    continue;
                }

                doors.add(newDoorFromRS(rs, doorUID, doorOwner.getPermission(), doorOwner.getPlayerUUID(),
                                        doorOwner.getPlayerName()));
            }
            ps.close();
            rs.close();
        }
        catch (SQLException | NullPointerException e)
        {
            logMessage("1051", e);
        }
        finally
        {
            try
            {
                conn.close();
            }
            catch (SQLException | NullPointerException e)
            {
                logMessage("1061", e);
            }
        }
        return doors;
    }

    public void updatePlayerName(final UUID playerUUID, final String playerName)
    {
        Connection conn = null;
        try
        {
            conn = getConnection();

            PreparedStatement ps = conn
                .prepareStatement("SELECT * FROM players WHERE playerUUID = '" + playerUUID.toString() + "';");
            ResultSet rs = ps.executeQuery();
            while (rs.next())
                if (rs.getString(PLAYERS_NAME) == null || !rs.getString(PLAYERS_NAME).equals(playerName))
                {
                    conn.setAutoCommit(false);
                    String update = "UPDATE players SET " + "playerName='" + playerName + "' WHERE playerUUID = '"
                        + playerUUID.toString() + "';";
                    conn.prepareStatement(update).executeUpdate();
                    conn.commit();
                }
            ps.close();
            rs.close();
        }
        catch (SQLException | NullPointerException e)
        {
            logMessage("671", e);
        }
        finally
        {
            try
            {
                conn.close();
            }
            catch (SQLException | NullPointerException e)
            {
                logMessage("681", e);
            }
        }
    }

    public UUID getUUIDFromName(final String playerName)
    {
        UUID uuid = null;
        Connection conn = null;
        try
        {
            conn = getConnection();
            // Get the door associated with the x/y/z location of the power block block.
            PreparedStatement ps = conn
                .prepareStatement("SELECT * FROM players WHERE playerName = '" + playerName + "';");
            ResultSet rs = ps.executeQuery();
            while (rs.next())
                uuid = UUID.fromString(rs.getString(PLAYERS_UUID));
            ps.close();
            rs.close();
        }
        catch (SQLException | NullPointerException e)
        {
            logMessage("703", e);
        }
        finally
        {
            try
            {
                conn.close();
            }
            catch (SQLException | NullPointerException e)
            {
                logMessage("713", e);
            }
        }
        return uuid;
    }

    public String getPlayerName(final UUID playerUUID)
    {
        String playerName = null;
        Connection conn = null;
        try
        {
            conn = getConnection();
            // Get the door associated with the x/y/z location of the power block block.
            PreparedStatement ps = conn
                .prepareStatement("SELECT * FROM players WHERE playerUUID = '" + playerUUID.toString() + "';");
            ResultSet rs = ps.executeQuery();
            while (rs.next())
                playerName = rs.getString(PLAYERS_NAME);
            ps.close();
            rs.close();
        }
        catch (SQLException | NullPointerException e)
        {
            logMessage("736", e);
        }
        finally
        {
            try
            {
                conn.close();
            }
            catch (SQLException | NullPointerException e)
            {
                logMessage("746", e);
            }
        }
        return playerName;
    }

    private DoorOwner getOwnerOfDoor(final Connection conn, final long doorUID) throws SQLException
    {
        DoorOwner doorOwner = null;
        String command = "SELECT * FROM sqlUnion WHERE doorUID = '" + doorUID + "' AND permission = '" + 0 + "';";
        PreparedStatement ps1 = conn.prepareStatement(command);
        ResultSet rs1 = ps1.executeQuery();

        while (rs1.next())
        {
            PreparedStatement ps2 = conn
                .prepareStatement("SELECT * FROM players WHERE id = '" + rs1.getInt(UNION_PLAYER_ID) + "';");
            ResultSet rs2 = ps2.executeQuery();
            while (rs2.next())
                doorOwner = new DoorOwner(plugin, doorUID, UUID.fromString(rs2.getString(PLAYERS_UUID)),
                                          rs1.getInt(UNION_PERM), rs2.getString(PLAYERS_NAME));
            ps2.close();
            rs2.close();
        }
        ps1.close();
        rs1.close();
        return doorOwner;
    }

    public DoorOwner getOwnerOfDoor(final long doorUID)
    {
        DoorOwner doorOwner = null;

        Connection conn = null;
        try
        {
            conn = getConnection();
            doorOwner = getOwnerOfDoor(conn, doorUID);
        }
        catch (SQLException | NullPointerException e)
        {
            logMessage("788", e);
        }
        finally
        {
            try
            {
                conn.close();
            }
            catch (SQLException | NullPointerException e)
            {
                logMessage("798", e);
            }
        }
        return doorOwner;
    }

    public HashMap<Long, Long> getPowerBlockData(final long chunkHash)
    {
        HashMap<Long, Long> doors = new HashMap<>();

        Connection conn = null;
        try
        {
            conn = getConnection();
            // Get the door associated with the x/y/z location of the power block block.
            PreparedStatement ps = conn.prepareStatement("SELECT * FROM doors WHERE chunkHash = '" + chunkHash + "';");
            ResultSet rs = ps.executeQuery();
            while (rs.next())
            {
                long locationHash = Util.locationHash(rs.getInt(DOOR_POWER_X), rs.getInt(DOOR_POWER_Y),
                                                      rs.getInt(DOOR_POWER_Z),
                                                      UUID.fromString(rs.getString(DOOR_WORLD)));
                doors.put(locationHash, rs.getLong(DOOR_ID));
            }
            ps.close();
            rs.close();
        }
        catch (SQLException | NullPointerException e)
        {
            logMessage("828", e);
        }
        finally
        {
            try
            {
                conn.close();
            }
            catch (SQLException | NullPointerException e)
            {
                logMessage("838", e);
            }
        }
        return doors;
    }

    public void recalculatePowerBlockHashes()
    {
        Connection conn = null;
        try
        {
            conn = getConnection();
            PreparedStatement ps1 = conn.prepareStatement("SELECT * FROM doors;");
            ResultSet rs1 = ps1.executeQuery();
            String update;

            while (rs1.next())
            {
                long UID = rs1.getLong(DOOR_ID);
                UUID worldUUID = UUID.fromString(rs1.getString(DOOR_WORLD));
                int x = rs1.getInt(DOOR_POWER_X);
                int z = rs1.getInt(DOOR_POWER_Z);

                update = "UPDATE doors SET " + "chunkHash='" + Util.chunkHashFromLocation(x, z, worldUUID)
                    + "' WHERE id = '" + UID + "';";
                conn.prepareStatement(update).executeUpdate();
            }
            ps1.close();
            rs1.close();
        }
        catch (SQLException | NullPointerException e)
        {
            logMessage("893", e);
        }
        finally
        {
            try
            {
                conn.close();
            }
            catch (SQLException | NullPointerException e)
            {
                logMessage("903", e);
            }
        }
    }

    public void updateDoorBlocksToMove(final long doorID, final int blocksToMove)
    {
        Connection conn = null;
        try
        {
            conn = getConnection();
            conn.setAutoCommit(false);
            String update = "UPDATE doors SET " + "blocksToMove='" + blocksToMove + "' WHERE id = '" + doorID + "';";
            conn.prepareStatement(update).executeUpdate();
            conn.commit();
        }
        catch (SQLException | NullPointerException e)
        {
            logMessage("859", e);
        }
        finally
        {
            try
            {
                conn.close();
            }
            catch (SQLException | NullPointerException e)
            {
                logMessage("869", e);
            }
        }
    }

    // Update the door at doorUID with the provided coordinates and open status.
    public void updateDoorCoords(final long doorID, final boolean isOpen, final int xMin, final int yMin,
                                 final int zMin, final int xMax, final int yMax, final int zMax,
                                 final DoorDirection engSide)
    {
        Connection conn = null;
        try
        {
            conn = getConnection();
            conn.setAutoCommit(false);
            String update = "UPDATE doors SET " + "xMin='" + xMin + "',yMin='" + yMin + "',zMin='" + zMin + "',xMax='"
                + xMax + "',yMax='" + yMax + "',zMax='" + zMax + "',isOpen='" + (isOpen == true ? 1 : 0)
                + "',engineSide='" + (engSide == null ? -1 : DoorDirection.getValue(engSide)) + "' WHERE id = '"
                + doorID + "';";
            conn.prepareStatement(update).executeUpdate();
            conn.commit();
        }
        catch (SQLException | NullPointerException e)
        {
            logMessage("897", e);
        }
        finally
        {
            try
            {
                conn.close();
            }
            catch (SQLException | NullPointerException e)
            {
                logMessage("907", e);
            }
        }
    }

    // Update the door with UID doorUID's Power Block Location with the provided
    // coordinates and open status.
    public void updateDoorAutoClose(final long doorID, final int autoClose)
    {
        Connection conn = null;
        try
        {
            conn = getConnection();
            conn.setAutoCommit(false);
            String update = "UPDATE doors SET " + "autoClose='" + autoClose + "' WHERE id = '" + doorID + "';";
            conn.prepareStatement(update).executeUpdate();
            conn.commit();
        }
        catch (SQLException | NullPointerException e)
        {
            logMessage("928", e);
        }
        finally
        {
            try
            {
                conn.close();
            }
            catch (SQLException | NullPointerException e)
            {
                logMessage("938", e);
            }
        }
    }

    // Update the door with UID doorUID's Power Block Location with the provided
    // coordinates and open status.
    public void updateDoorOpenDirection(final long doorID, final RotateDirection openDir)
    {
        Connection conn = null;
        try
        {
            conn = getConnection();
            conn.setAutoCommit(false);
            String update = "UPDATE doors SET " + "openDirection='" + RotateDirection.getValue(openDir)
                + "' WHERE id = '" + doorID + "';";
            conn.prepareStatement(update).executeUpdate();
            conn.commit();
        }
        catch (SQLException | NullPointerException e)
        {
            logMessage("958", e);
        }
        finally
        {
            try
            {
                conn.close();
            }
            catch (SQLException | NullPointerException e)
            {
                logMessage("966", e);
            }
        }
    }

    // Update the door with UID doorUID's Power Block Location with the provided
    // coordinates and open status.
    public void updateDoorPowerBlockLoc(final long doorID, final int xPos, final int yPos, final int zPos,
                                        final UUID worldUUID)
    {
        Connection conn = null;
        try
        {
            conn = getConnection();
            conn.setAutoCommit(false);
            String update = "UPDATE doors SET " + "powerBlockX='" + xPos + "',powerBlockY='" + yPos + "',powerBlockZ='"
                + zPos + "',chunkHash='" + Util.chunkHashFromLocation(xPos, zPos, worldUUID) + "' WHERE id = '" + doorID
                + "';";
            conn.prepareStatement(update).executeUpdate();
            conn.commit();
        }
        catch (SQLException | NullPointerException e)
        {
            logMessage("992", e);
        }
        finally
        {
            try
            {
                conn.close();
            }
            catch (SQLException | NullPointerException e)
            {
                logMessage("1002", e);
            }
        }
    }

    // Check if a given location already contains a power block or not.
    // Returns false if it's already occupied.
    public boolean isPowerBlockLocationEmpty(final Location loc)
    {
        // Prepare door and connection.
        Connection conn = null;
        try
        {
            conn = getConnection();
            // Get the door associated with the x/y/z location of the power block block.
            PreparedStatement ps = conn.prepareStatement("SELECT * FROM doors WHERE powerBlockX = '" + loc.getBlockX()
                + "' AND powerBlockY = '" + loc.getBlockY() + "' AND powerBlockZ = '" + loc.getBlockZ()
                + "' AND world = '" + loc.getWorld().getUID().toString() + "';");
            ResultSet rs = ps.executeQuery();
            boolean isAvailable = true;

            if (rs.next())
                isAvailable = false;

            ps.close();
            rs.close();
            return isAvailable;
        }
        catch (SQLException | NullPointerException e)
        {
            logMessage("1033", e);
        }
        finally
        {
            try
            {
                conn.close();
            }
            catch (SQLException | NullPointerException e)
            {
                logMessage("1043", e);
            }
        }

        return false;
    }

    // Update the door at doorUID with the provided new lockstatus.
    public void setLock(final long doorID, final boolean newLockStatus)
    {
        Connection conn = null;
        try
        {
            conn = getConnection();
            conn.setAutoCommit(false);
            String update = "UPDATE doors SET " + "isLocked='" + (newLockStatus == true ? 1 : 0) + "' WHERE id='"
                + doorID + "';";
            conn.prepareStatement(update).executeUpdate();
            conn.commit();
        }
        catch (SQLException | NullPointerException e)
        {
            logMessage("1066", e);
        }
        finally
        {
            try
            {
                conn.close();
            }
            catch (SQLException | NullPointerException e)
            {
                logMessage("1076", e);
            }
        }
    }

    // Insert a new door in the db.
    public long insert(final Door door)
    {
        Connection conn = null;
        try
        {
            conn = getConnection();

            long playerID = getPlayerID(conn, door.getPlayerUUID().toString());

            if (playerID == -1)
            {
                Statement stmt2 = conn.createStatement();
                String sql2 = "INSERT INTO players (playerUUID, playerName) " + "VALUES ('"
                    + door.getPlayerUUID().toString() + "', '" + Util.nameFromUUID(door.getPlayerUUID()) + "');";
                stmt2.executeUpdate(sql2);
                stmt2.close();

                String query = "SELECT last_insert_rowid() AS lastId";
                PreparedStatement ps2 = conn.prepareStatement(query);
                ResultSet rs2 = ps2.executeQuery();
                playerID = rs2.getLong("lastId");
                ps2.close();
                rs2.close();
            }

            String doorInsertsql = "INSERT INTO doors(name,world,isOpen,xMin,yMin,zMin,xMax,yMax,zMax,engineX,engineY,engineZ,isLocked,type,engineSide,powerBlockX,powerBlockY,powerBlockZ,openDirection,autoClose,chunkHash,blocksToMove) "
                + "VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
            PreparedStatement doorstatement = conn.prepareStatement(doorInsertsql);

            doorstatement.setString(DOOR_NAME - 1, door.getName());
            doorstatement.setString(DOOR_WORLD - 1, door.getWorld().getUID().toString());
            doorstatement.setInt(DOOR_OPEN - 1, door.isOpen() == true ? 1 : 0);
            doorstatement.setInt(DOOR_MIN_X - 1, door.getMinimum().getBlockX());
            doorstatement.setInt(DOOR_MIN_Y - 1, door.getMinimum().getBlockY());
            doorstatement.setInt(DOOR_MIN_Z - 1, door.getMinimum().getBlockZ());
            doorstatement.setInt(DOOR_MAX_X - 1, door.getMaximum().getBlockX());
            doorstatement.setInt(DOOR_MAX_Y - 1, door.getMaximum().getBlockY());
            doorstatement.setInt(DOOR_MAX_Z - 1, door.getMaximum().getBlockZ());
            doorstatement.setInt(DOOR_ENG_X - 1, door.getEngine().getBlockX());
            doorstatement.setInt(DOOR_ENG_Y - 1, door.getEngine().getBlockY());
            doorstatement.setInt(DOOR_ENG_Z - 1, door.getEngine().getBlockZ());
            doorstatement.setInt(DOOR_LOCKED - 1, door.isLocked() == true ? 1 : 0);
            doorstatement.setInt(DOOR_TYPE - 1, DoorType.getValue(door.getType()));
            // Set -1 if the door has no engineSide (normal doors don't use it)
            doorstatement.setInt(DOOR_ENG_SIDE - 1,
                                 door.getEngSide() == null ? -1 : DoorDirection.getValue(door.getEngSide()));
            doorstatement.setInt(DOOR_POWER_X - 1, door.getEngine().getBlockX());
            doorstatement.setInt(DOOR_POWER_Y - 1, door.getEngine().getBlockY() - 1); // Power Block Location is 1 block
                                                                                      // below the engine, by default.
            doorstatement.setInt(DOOR_POWER_Z - 1, door.getEngine().getBlockZ());
            doorstatement.setInt(DOOR_OPEN_DIR - 1, RotateDirection.getValue(door.getOpenDir()));
            doorstatement.setInt(DOOR_AUTO_CLOSE - 1, door.getAutoClose());
            doorstatement.setLong(DOOR_CHUNK_HASH - 1, door.getPowerBlockChunkHash());
            doorstatement.setLong(DOOR_BLOCKS_TO_MOVE - 1, door.getBlocksToMove());

            doorstatement.executeUpdate();
            doorstatement.close();

            String query = "SELECT last_insert_rowid() AS lastId";
            PreparedStatement ps2 = conn.prepareStatement(query);
            ResultSet rs2 = ps2.executeQuery();
            Long doorUID = rs2.getLong("lastId");
            ps2.close();
            rs2.close();

            Statement stmt3 = conn.createStatement();
            String sql3 = "INSERT INTO sqlUnion (permission, playerID, doorUID) " + "VALUES ('" + door.getPermission()
                + "', '" + playerID + "', '" + doorUID + "');";
            stmt3.executeUpdate(sql3);
            stmt3.close();

            return doorUID;
        }
        catch (SQLException | NullPointerException e)
        {
            logMessage("1153", e);
        }
        finally
        {
            try
            {
                conn.close();
            }
            catch (SQLException | NullPointerException e)
            {
                logMessage("1163", e);
            }
        }
        return -1;
    }

    // Insert a new door in the db.
    public boolean removeOwner(final long doorUID, final UUID playerUUID)
    {
        Connection conn = null;
        try
        {
            conn = getConnection();
            long playerID = getPlayerID(conn, playerUUID.toString());

            if (playerID == -1)
                plugin.getMyLogger().logMessage(
                                                "Trying to remove player " + playerUUID.toString()
                                                    + " as ownwer of door " + doorUID + ". But player does not exist!",
                                                true, false);
            else
            {
                PreparedStatement ps2 = conn.prepareStatement("DELETE FROM sqlUnion WHERE " + "playerID = '" + playerID
                    + "' AND doorUID = '" + doorUID + "' AND permission > '" + 0 + "';"); // The creator cannot be
                                                                                          // removed as owner
                ps2.execute();
                ps2.close();
            }

        }
        catch (SQLException | NullPointerException e)
        {
            logMessage("1193", e);
            return false;
        }
        finally
        {
            try
            {
                conn.close();
            }
            catch (SQLException | NullPointerException e)
            {
                logMessage("1204", e);
                return false;
            }
        }
        return true;
    }

    public ArrayList<DoorOwner> getOwnersOfDoor(final long doorUID, @Nullable final UUID playerUUID)
    {
        ArrayList<DoorOwner> ret = new ArrayList<>();
        Connection conn = null;
        try
        {
            conn = getConnection();
            PreparedStatement ps1 = conn.prepareStatement("SELECT * FROM sqlUnion WHERE doorUID = '" + doorUID + "';");
            ResultSet rs1 = ps1.executeQuery();
            while (rs1.next())
            {
                PreparedStatement ps2 = conn
                    .prepareStatement("SELECT * FROM players WHERE id = '" + rs1.getInt(UNION_PLAYER_ID) + "';");
                ResultSet rs2 = ps2.executeQuery();
                if (playerUUID == null || !UUID.fromString(rs2.getString(PLAYERS_UUID)).equals(playerUUID))
                    ret.add(new DoorOwner(plugin, doorUID, UUID.fromString(rs2.getString(PLAYERS_UUID)),
                                          rs1.getInt(UNION_PERM), rs2.getString(PLAYERS_NAME)));
                ps2.close();
                rs2.close();
            }
            ps1.close();
            rs1.close();
        }
        catch (SQLException | NullPointerException e)
        {
            logMessage("1234", e);
        }
        finally
        {
            try
            {
                conn.close();
            }
            catch (SQLException | NullPointerException e)
            {
                logMessage("1244", e);
            }
        }

        return ret;
    }

    // Insert a new door in the db.
    public void addOwner(final long doorUID, final UUID playerUUID, final int permission)
    {
        Connection conn = null;
        try
        {
            conn = getConnection();
            long playerID = getPlayerID(conn, playerUUID.toString());

            if (playerID == -1)
            {
                Statement stmt2 = conn.createStatement();
                String sql2 = "INSERT INTO players (playerUUID, playerName) " + "VALUES ('" + playerUUID.toString()
                    + "', '" + Util.nameFromUUID(playerUUID) + "');";
                stmt2.executeUpdate(sql2);
                stmt2.close();

                String query = "SELECT last_insert_rowid() AS lastId";
                PreparedStatement ps2 = conn.prepareStatement(query);
                ResultSet rs2 = ps2.executeQuery();
                playerID = rs2.getLong("lastId");
                ps2.close();
                rs2.close();
            }

            PreparedStatement ps3 = conn.prepareStatement("SELECT * FROM sqlUnion WHERE playerID = '" + playerID
                + "' AND doorUID = '" + doorUID + "';");
            ResultSet rs3 = ps3.executeQuery();

            // If it already exists, update the permission, if needed.
            if (rs3.next())
            {
                if (rs3.getInt(UNION_PERM) != permission)
                {
                    Statement stmt4 = conn.createStatement();
                    String sql4 = "UPDATE sqlUnion SET permission = '" + permission + "' WHERE playerID = '" + playerID
                        + "' AND doorUID = '" + doorUID + "';";
                    stmt4.executeUpdate(sql4);
                    stmt4.close();
                }
            }
            else
            {
                Statement stmt4 = conn.createStatement();
                String sql4 = "INSERT INTO sqlUnion (permission, playerID, doorUID) " + "VALUES ('" + permission
                    + "', '" + playerID + "', '" + doorUID + "');";
                stmt4.executeUpdate(sql4);
                stmt4.close();
            }
            ps3.close();
            rs3.close();
        }
        catch (SQLException | NullPointerException e)
        {
            logMessage("1306", e);
        }
        finally
        {
            try
            {
                conn.close();
            }
            catch (SQLException | NullPointerException e)
            {
                logMessage("1316", e);
            }
        }
    }

    // Get the number of doors owned by this player.
    // If name is null, it will ignore door names, otherwise it will return the
    // number of doors with the provided name.
    public long countDoors(final String playerUUID, final String name)
    {
        long count = 0;
        Connection conn = null;
        try
        {
            conn = getConnection();
            long playerID = getPlayerID(conn, playerUUID.toString());

            // Select all doors from the sqlUnion table that have the previously found
            // player as owner.
            PreparedStatement ps2 = conn
                .prepareStatement("SELECT * FROM sqlUnion WHERE playerID = '" + playerID + "';");
            ResultSet rs2 = ps2.executeQuery();
            while (rs2.next())
            {
                // Retrieve the door with the provided ID.
                PreparedStatement ps3 = conn
                    .prepareStatement("SELECT * FROM doors WHERE id = '" + rs2.getInt(UNION_DOOR_ID) + "';");
                ResultSet rs3 = ps3.executeQuery();
                // Check if this door matches the provided name, if a name was provided.
                while (rs3.next())
                    if (name == null || rs3.getString(DOOR_NAME).equals(name))
                        ++count;
                ps3.close();
                rs3.close();
            }
            ps2.close();
            rs2.close();
        }
        catch (SQLException | NullPointerException e)
        {
            logMessage("1352", e);
        }
        finally
        {
            try
            {
                conn.close();
            }
            catch (SQLException | NullPointerException e)
            {
                logMessage("1362", e);
            }
        }
        return count;
    }

    private int getDatabaseVersion(final Connection conn)
    {
        try
        {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("PRAGMA user_version;");
            int dbVersion = rs.getInt(1);
            stmt.close();
            rs.close();
            return dbVersion;
        }
        catch (SQLException | NullPointerException e)
        {
            logMessage("1503", e);
        }
        return Integer.MAX_VALUE;
    }

    private int getDatabaseVersion()
    {
        try (Connection conn = getConnection())
        {
            return getDatabaseVersion(conn);
        }
        catch (SQLException | NullPointerException e)
        {
            logMessage("1498", e);
        }
        return Integer.MAX_VALUE;
    }

    // Add columns and such when needed (e.g. upgrades from older versions).
    private void upgrade()
    {
        Connection conn = null;
        boolean replaceTempPlayerNames = false;
        try
        {
            conn = DriverManager.getConnection(url);
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

            if (dbVersion > DATABASE_VERSION)
            {
                plugin.getMyLogger()
                    .logMessage("Trying to load a database that is incompatible with this version of the plugin! "
                        + "Database version = " + dbVersion + ". Please update the plugin.", true, false);
                conn.close();
                validVersion = false;
                return;
            }

            // If an update is required and backups are enabled, make a backup.
            if (dbVersion != DATABASE_VERSION && plugin.getConfigLoader().dbBackup())
            {
                conn.close();
                if (!makeBackup())
                    return;
                conn = getConnectionUnsafe();
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
                conn = getConnectionUnsafe();
            }

            if (dbVersion < 6)
            {
                conn.close();
                upgradeToV6();
                conn = getConnectionUnsafe();
            }

            // If the database upgrade to V5 got interrupted in a previous attempt, the
            // fakeUUID
            // will still be in the database. If so, simply continue filling in player names
            // in the db.
            if (!replaceTempPlayerNames && fakeUUIDExists(conn))
                replaceTempPlayerNames = true;

            // Do this at the very end, so the db version isn't altered if anything fails.
            if (dbVersion != DATABASE_VERSION)
                setDBVersion(conn, DATABASE_VERSION);
        }
        catch (SQLException | NullPointerException e)
        {
            logMessage("1414", e);
        }
        finally
        {
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
            replaceTempPlayerNames();
    }

    private boolean fakeUUIDExists(final Connection conn)
    {
        try
        {
            return (conn.createStatement().executeQuery("SELECT * FROM players WHERE playerUUID='" + FAKEUUID + "';")
                .next());
        }
        catch (SQLException | NullPointerException e)
        {
            logMessage("1461", e);
        }
        return false;
    }

    private boolean makeBackup()
    {
        return makeBackup(".BACKUP");
    }

    private boolean makeBackup(final String extension)
    {
        File dbFileBackup = new File(plugin.getDataFolder(), dbName + extension);
        // Only the most recent backup is kept, so delete the old one if a new one needs
        // to be created.
        if (dbFileBackup.exists())
            dbFileBackup.delete();
        try
        {
            Files.copy(dbFile, dbFileBackup);
        }
        catch (IOException e)
        {
            plugin.getMyLogger().logMessage("Failed to create backup of the database! "
                + "Database upgrade aborted and access is disabled!" + Util.exceptionToString(e), true, true);
            e.printStackTrace();
            enabled = false;
            return false;
        }
        return true;
    }

    private void setDBVersion(final Connection conn, final int version)
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

    private void upgradeToV1(final Connection conn)
    {
        try
        {
            String addColumn;

            DatabaseMetaData md = conn.getMetaData();
            ResultSet rs = md.getColumns(null, null, "doors", "type");

            if (!rs.next())
            {
                plugin.getMyLogger().logMessage("Upgrading database! Adding type!", true, true);
                addColumn = "ALTER TABLE doors " + "ADD COLUMN type int NOT NULL DEFAULT 0";
                conn.createStatement().execute(addColumn);
            }
            rs.close();

            rs = md.getColumns(null, null, "doors", "engineSide");
            if (!rs.next())
            {
                plugin.getMyLogger().logMessage("Upgrading database! Adding engineSide!", true, true);
                addColumn = "ALTER TABLE doors " + "ADD COLUMN engineSide int NOT NULL DEFAULT -1";
                conn.createStatement().execute(addColumn);
            }
            rs.close();

            rs = md.getColumns(null, null, "doors", "powerBlockX");
            if (!rs.next())
            {
                plugin.getMyLogger().logMessage("Upgrading database! Adding powerBlockLoc!", true, true);
                addColumn = "ALTER TABLE doors " + "ADD COLUMN powerBlockX int NOT NULL DEFAULT -1";
                conn.createStatement().execute(addColumn);
                addColumn = "ALTER TABLE doors " + "ADD COLUMN powerBlockY int NOT NULL DEFAULT -1";
                conn.createStatement().execute(addColumn);
                addColumn = "ALTER TABLE doors " + "ADD COLUMN powerBlockZ int NOT NULL DEFAULT -1";
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
                    update = "UPDATE doors SET " + "powerBlockX='" + x + "',powerBlockY='" + y + "',powerBlockZ='" + z
                        + "' WHERE id = '" + UID + "';";
                    conn.prepareStatement(update).executeUpdate();
                }
                ps1.close();
                rs1.close();
            }
            rs.close();

            rs = md.getColumns(null, null, "doors", "openDirection");
            if (!rs.next())
            {
                plugin.getMyLogger().logMessage("Upgrading database! Adding openDirection!", true, true);
                addColumn = "ALTER TABLE doors " + "ADD COLUMN openDirection int NOT NULL DEFAULT 0";
                conn.createStatement().execute(addColumn);

                plugin.getMyLogger()
                    .logMessage("Upgrading database! Swapping open-status of drawbridges to conform to the new standard!",
                                true, true);
                String update = "UPDATE doors SET " + "isOpen='" + 2 + "' WHERE isOpen = '" + 0 + "' AND type = '"
                    + DoorType.getValue(DoorType.DRAWBRIDGE) + "';";
                conn.createStatement().execute(update);
                update = "UPDATE doors SET " + "isOpen='" + 0 + "' WHERE isOpen = '" + 1 + "' AND type = '"
                    + DoorType.getValue(DoorType.DRAWBRIDGE) + "';";
                conn.createStatement().execute(update);
                update = "UPDATE doors SET " + "isOpen='" + 1 + "' WHERE isOpen = '" + 2 + "' AND type = '"
                    + DoorType.getValue(DoorType.DRAWBRIDGE) + "';";
                conn.createStatement().execute(update);
            }
            rs.close();

            rs = md.getColumns(null, null, "doors", "autoClose");
            if (!rs.next())
            {
                plugin.getMyLogger().logMessage("Upgrading database! Adding autoClose!", true, true);
                addColumn = "ALTER TABLE doors " + "ADD COLUMN autoClose int NOT NULL DEFAULT -1";
                conn.createStatement().execute(addColumn);
            }
            rs.close();

            rs = md.getColumns(null, null, "doors", "chunkHash");
            if (!rs.next())
            {
                plugin.getMyLogger().logMessage("Upgrading database! Adding chunkHash!", true, true);
                addColumn = "ALTER TABLE doors " + "ADD COLUMN chunkHash int NOT NULL DEFAULT -1";
                conn.createStatement().execute(addColumn);

                PreparedStatement ps1 = conn.prepareStatement("SELECT * FROM doors;");
                ResultSet rs1 = ps1.executeQuery();
                String update;

                while (rs1.next())
                {
                    long UID = rs1.getLong(DOOR_ID);
                    UUID worldUUID = UUID.fromString(rs1.getString(DOOR_WORLD));
                    int x = rs1.getInt(DOOR_POWER_X);
                    int z = rs1.getInt(DOOR_POWER_Z);

                    update = "UPDATE doors SET " + "chunkHash='" + Util.chunkHashFromLocation(x, z, worldUUID)
                        + "' WHERE id = '" + UID + "';";
                    conn.prepareStatement(update).executeUpdate();
                }
                ps1.close();
                rs1.close();
            }
            rs.close();
            plugin.getMyLogger().logMessage("Database has been upgraded to V1!", true, true);
        }
        catch (SQLException | NullPointerException e)
        {
            logMessage("1593", e);
        }
    }

    private void upgradeToV2(final Connection conn)
    {
        try
        {
            String addColumn;
            plugin.getMyLogger().logMessage("Upgrading database to V2! Adding blocksToMove!", true, true);
            addColumn = "ALTER TABLE doors " + "ADD COLUMN blocksToMove int NOT NULL DEFAULT 0";
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
    private void upgradeToV3(final Connection conn)
    {
        try
        {
            plugin.getMyLogger().logMessage("Upgrading database to V3! Recreating sqlUnion!", true, true);
            conn.setAutoCommit(false);
            disableForeignKeys(conn);

            // Rename sqlUnion.
            conn.createStatement().execute("ALTER TABLE sqlUnion RENAME TO sqlUnion_old;");

            // Create updated version of sqlUnion.
            conn.createStatement()
                .execute("CREATE TABLE IF NOT EXISTS sqlUnion " + "(id          INTEGER    PRIMARY KEY AUTOINCREMENT, "
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
                reEnableForeignKeys(conn);
                conn.rollback();
            }
            catch (SQLException | NullPointerException e1)
            {
                logMessage("1285", e1);
            }
            logMessage("1287", e);
        }
    }

    private void upgradeToV4(final Connection conn)
    {
        try
        {
            String addColumn;
            plugin.getMyLogger().logMessage("Upgrading database to V4! Adding playerName!", true, true);
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
        Connection conn = null;
        try
        {
            conn = getConnection();
            plugin.getMyLogger().logMessageToLogFile("Upgrading database to V5!");

            String countStr = "SELECT COUNT(*) AS total FROM players WHERE playerName IS NULL";
            int count = conn.createStatement().executeQuery(countStr).getInt("total");

            // Make sure there aren't any NULL values in the players database.
            if (count > 0)
            {
                // First, find a name that does not exist in the database already.
                // Do this by generating random strings and checking if it exists until
                // We encounter one that doesn't.
                String fakeName = Util.randomString(12);
                boolean exists = true;
                while (exists)
                {
                    PreparedStatement ps = conn
                        .prepareStatement("SELECT * FROM players WHERE playerName = '" + fakeName + "';");
                    ResultSet rs = ps.executeQuery();
                    if (!rs.next())
                        exists = false;
                    ps.close();
                    rs.close();
                    if (exists)
                        fakeName = Util.randomString(12);
                }
                plugin.getMyLogger().logMessageToLogFile("UpgradeToV5: Using fakeName = " + fakeName);

                Statement stmt = conn.createStatement();
                String sql = "INSERT INTO players (playerUUID, playerName) " + "VALUES ('" + FAKEUUID + "', '"
                    + fakeName + "');";
                stmt.executeUpdate(sql);
                stmt.close();

                String update = "UPDATE players SET playerName='" + fakeName + "' WHERE playerName IS NULL;";
                conn.prepareStatement(update).executeUpdate();
            }
        }
        catch (SQLException | NullPointerException e)
        {
            logMessage("1745", e);
        }
        finally
        {
            try
            {
                conn.close();
            }
            catch (SQLException | NullPointerException e)
            {
                logMessage("1755", e);
            }
        }

        Connection con = null;
        try
        {
            con = DriverManager.getConnection(url);
            disableForeignKeys(con);
            con.setAutoCommit(false);
            // Rename sqlUnion.
            con.createStatement().execute("ALTER TABLE players RENAME TO players_old;");
            con.createStatement()
                .execute("CREATE TABLE IF NOT EXISTS players " + "(id          INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + " playerUUID  TEXT    NOT NULL," + " playerName  TEXT    NOT NULL)");
            con.createStatement().execute("INSERT INTO players SELECT * FROM players_old;");
            con.createStatement().execute("DROP TABLE IF EXISTS 'players_old';");
            con.commit();
            con.setAutoCommit(true);
        }
        catch (SQLException | NullPointerException e)
        {
            try
            {
                con.rollback();
            }
            catch (SQLException | NullPointerException e1)
            {
                logMessage("1770", e1);
            }
            logMessage("1772", e);
        }
        finally
        {
            try
            {
                con.close();
            }
            catch (SQLException | NullPointerException e)
            {
                logMessage("1781", e);
            }
        }
    }

    /*
     * Make playerUUID column unique
     */
    private void upgradeToV6()
    {
        Connection conn = null;
        try
        {
            conn = DriverManager.getConnection(url);
            disableForeignKeys(conn);
            plugin.getMyLogger().logMessage("Upgrading database to V6! Recreating table \"players\"!", true, true);
            conn.setAutoCommit(false);

            conn.createStatement().execute("ALTER TABLE players RENAME TO players_old;");
            conn.createStatement()
                .execute("CREATE TABLE IF NOT EXISTS players \n"
                    + "(id          INTEGER    PRIMARY KEY AUTOINCREMENT, \n" + " playerUUID  TEXT       NOT NULL, \n"
                    + " playerName  TEXT       NOT NULL, \n" + " unique(playerUUID));");

            ResultSet rs1 = conn.prepareStatement("SELECT * FROM players_old;").executeQuery();
            while (rs1.next())
            {
                String insert = "INSERT INTO players(id, playerUUID, playerName) VALUES(?,?,?);";
                PreparedStatement insertStatement = conn.prepareStatement(insert);
                insertStatement.setLong(1, rs1.getLong("id"));
                insertStatement.setString(2, rs1.getString("playerUUID"));
                insertStatement.setString(3, rs1.getString("playerName"));
                insertStatement.executeUpdate();
                insertStatement.close();
            }
            rs1.close();
            conn.createStatement().execute("DROP TABLE IF EXISTS 'players_old';");

            conn.commit();
        }
        catch (SQLException | NullPointerException e)
        {
            try
            {
                conn.setAutoCommit(true);
                reEnableForeignKeys(conn);
                conn.rollback();
            }
            catch (SQLException | NullPointerException e1)
            {
                logMessage("2257", e1);
            }
            logMessage("2259", e);
        }
        finally
        {
            try
            {
                conn.close();
            }
            catch (SQLException e)
            {
                logMessage("2280", e);
            }
        }
    }

    /*
     * Part of the upgrade to V5 of the database. First lock the database so nothing
     * else can interfere. Then find the temporary name from the fake player (that
     * was stored in the db). All fake names then get replaced by the real names. Or
     * the last ones they used, anyway.
     */
    private void replaceTempPlayerNames()
    {
        locked.set(true);
        final Thread thread = new Thread(() ->
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
                    String playerName = Bukkit.getOfflinePlayer(playerUUID).getName();

                    String update = "UPDATE players SET playerName='" + playerName + "' WHERE playerUUID='"
                        + playerUUID.toString() + "';";
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
                try
                {
                    conn.close();
                }
                catch (SQLException | NullPointerException e)
                {
                    logMessage("1739", e);
                }
                locked.set(false);
            }
        });
        thread.start();
    }

    private void logMessage(String str, Exception e)
    {
        if (!locked.get())
            plugin.getMyLogger().logMessageToLogFile(str + " " + Util.exceptionToString(e));
        else if (!validVersion)
            plugin.getMyLogger()
                .logMessageToLogFile("This version of the database is not supported by this version of the plugin!");
        else
            plugin.getMyLogger()
                .logMessageToLogFile("Database locked! Failed at: " + str + ". Message: " + e.getMessage());

    }
}
