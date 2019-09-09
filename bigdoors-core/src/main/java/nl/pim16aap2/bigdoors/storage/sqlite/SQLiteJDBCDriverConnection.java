package nl.pim16aap2.bigdoors.storage.sqlite;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.api.IConfigLoader;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.api.IPWorld;
import nl.pim16aap2.bigdoors.doors.AbstractDoorBase;
import nl.pim16aap2.bigdoors.doors.DoorType;
import nl.pim16aap2.bigdoors.storage.IStorage;
import nl.pim16aap2.bigdoors.util.BitFlag;
import nl.pim16aap2.bigdoors.util.DoorOwner;
import nl.pim16aap2.bigdoors.util.PLogger;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import nl.pim16aap2.bigdoors.util.Util;
import nl.pim16aap2.bigdoors.util.vector.Vector3Di;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * SQLite implementation of {@link IStorage}.
 *
 * @author Pim
 */
public final class SQLiteJDBCDriverConnection implements IStorage
{
    private static final String DRIVER = "org.sqlite.JDBC";
    private static final int DATABASE_VERSION = 11;
    // TODO: Set this to 10. This cannot be done currently because the tests will fail for the upgrades, which
    //       are still useful when writing the code to upgrade the v1 database to v2.
    private static final int MIN_DATABASE_VERSION = 0;

    /**
     * A fake UUID that cannot exist normally. To be used for storing transient data across server restarts.
     */
    private static final String FAKEUUID = "0000";

    /**
     * Whether or not the database version is compatible with this version of BigDoors.
     */
    private boolean validVersion = true;

    /**
     * The database file.
     */
    private final File dbFile;

    /**
     * The URL of the database.
     */
    private final String url;

    /**
     * The logger used for error logging.
     */
    private final PLogger pLogger;

    /**
     * Whether or not the database is enabled. It gets disabled when it could not initialize the database properly or if
     * it failed to create a backup before updating.
     */
    private boolean enabled = false;

    /**
     * Whether the database is locked. It is locked during certain upgrade processes to prevent issues with concurrent
     * access and invalid data.
     */
    private AtomicBoolean locked = new AtomicBoolean(false);

    /**
     * The BigDoors configuration.
     */
    private final IConfigLoader config;

    /**
     * Constructor of the SQLite driver connection.
     *
     * @param dbFile  The file to store the database in.
     * @param pLogger The logger used for error logging.
     * @param config  The {@link IConfigLoader} containing options used in this class.
     */
    public SQLiteJDBCDriverConnection(final @NotNull File dbFile, final @NotNull PLogger pLogger,
                                      final @NotNull IConfigLoader config)
    {
        this.pLogger = pLogger;
        this.dbFile = dbFile;
        this.config = config;
        url = "jdbc:sqlite:" + dbFile;
        init();
        upgrade();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isSingleThreaded()
    {
        return true;
    }

    /**
     * Establishes a connection with the database.
     *
     * @return A database connection.
     */
    @NotNull
    private Connection getConnection()
    {
        if (locked.get())
        {
            IllegalStateException e = new IllegalStateException(
                "Database locked! Please try again later! " +
                    "Please contact pim16aap2 if the issue persists.");
            pLogger.logException(e);
            throw e;
        }

        if (!validVersion)
        {
            IllegalStateException e = new IllegalStateException(
                "Database disabled! Reason: Version too low! This is not a bug!\n" +
                    "If you want to use your old database, you'll have to upgrade it to v2 first.\n" +
                    "Please download the latest version of v1 first. For more info, go to the resource page.\n");
            pLogger.logException(e);
            throw e;
        }

        if (!enabled)
        {
            IllegalStateException e = new IllegalStateException(
                "Database disabled! This probably means an upgrade failed! " +
                    "Please contact pim16aap2.");
            pLogger.logException(e);
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
        {
            NullPointerException e = new NullPointerException("Could not open connection!");
            pLogger.logException(e);
            throw e;
        }
        return conn;
    }

    /**
     * Because SQLite is a PoS and decided to remove the admittedly odd behavior that just disabling foreign keys
     * suddenly ignored all the triggers etc attached to it without actually providing a proper alternative (perhaps
     * implement ALTER TABLE properly??), this method needs to be called now in order to safely modify stuff without
     * having the foreign keys get fucked up.
     *
     * @param conn The connection.
     */
    private void disableForeignKeys(final @NotNull Connection conn)
        throws SQLException
    {
        conn.createStatement().execute("PRAGMA foreign_keys=OFF");
        conn.createStatement().execute("PRAGMA legacy_alter_table=ON");
    }

    /**
     * The anti method of {@link #disableForeignKeys(Connection)}. Only needs to be called if that was called first.
     *
     * @param conn The connection.
     * @throws SQLException
     */
    private void reEnableForeignKeys(final @NotNull Connection conn)
        throws SQLException
    {
        conn.createStatement().execute("PRAGMA foreign_keys=ON");
        //noinspection
        conn.createStatement().execute("PRAGMA legacy_alter_table=OFF");
    }

    /**
     * Establishes a connection with the database.
     * <p>
     * This is "unsafe" because it bypasses all checks on if it is currently locked, or disabled or whatever.
     *
     * @return A database connection.
     */
    @NotNull
    private Connection getConnectionUnsafe()
    {
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
        {
            NullPointerException e = new NullPointerException("Could not open connection!");
            pLogger.logException(e);
            throw e;
        }
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
                if (!dbFile.getParentFile().exists() && !dbFile.getParentFile().mkdirs())
                {
                    pLogger.logException(
                        new IOException(
                            "Failed to create directory \"" + dbFile.getParentFile().toString() + "\""));
                    return;
                }
                if (!dbFile.createNewFile())
                {
                    pLogger.logException(new IOException("Failed to create file \"" + dbFile.toString() + "\""));
                    return;
                }
                pLogger.info("New file created at " + dbFile);
            }
            catch (IOException e)
            {
                pLogger.severe("File write error: " + dbFile);
                return;
            }
        enabled = true;

        // Table creation
        try (Connection conn = getConnection())
        {
            // Check if the doors table already exists. If it does, assume the rest exists
            // as well and don't set it up.
            if (!conn.getMetaData().getTables(null, null, "doors", new String[]{"TABLE"}).next())
            {
                Statement stmt1 = conn.createStatement();
                String sql1 = "CREATE TABLE IF NOT EXISTS doors \n" +
                    "(id            INTEGER    PRIMARY KEY autoincrement, \n" +
                    " name          TEXT       NOT NULL, \n" +
                    " world         TEXT       NOT NULL, \n" +
                    " xMin          INTEGER    NOT NULL, \n" +
                    " yMin          INTEGER    NOT NULL, \n" +
                    " zMin          INTEGER    NOT NULL, \n" +
                    " xMax          INTEGER    NOT NULL, \n" +
                    " yMax          INTEGER    NOT NULL, \n" +
                    " zMax          INTEGER    NOT NULL, \n" +
                    " engineX       INTEGER    NOT NULL, \n" +
                    " engineY       INTEGER    NOT NULL, \n" +
                    " engineZ       INTEGER    NOT NULL, \n" +
                    " bitflag       INTEGER    NOT NULL DEFAULT 0, \n" +
                    " type          INTEGER    NOT NULL DEFAULT  0, \n" +
                    " powerBlockX   INTEGER    NOT NULL DEFAULT -1, \n" +
                    " powerBlockY   INTEGER    NOT NULL DEFAULT -1, \n" +
                    " powerBlockZ   INTEGER    NOT NULL DEFAULT -1, \n" +
                    " openDirection INTEGER    NOT NULL DEFAULT  0, \n" +
                    " autoClose     INTEGER    NOT NULL DEFAULT -1, \n" +
                    " chunkHash     INTEGER    NOT NULL DEFAULT -1, \n" +
                    " blocksToMove  INTEGER    NOT NULL DEFAULT -1);";
                stmt1.executeUpdate(sql1);
                stmt1.close();

                Statement stmt2 = conn.createStatement();
                String sql2 = "CREATE TABLE IF NOT EXISTS players \n" +
                    "(id          INTEGER    PRIMARY KEY AUTOINCREMENT, \n" +
                    " playerUUID  TEXT       NOT NULL, \n" +
                    " playerName  TEXT       NOT NULL, \n" +
                    " unique(playerUUID));";
                stmt2.executeUpdate(sql2);
                stmt2.close();

                Statement stmt3 = conn.createStatement();
                String sql3 = "CREATE TABLE IF NOT EXISTS sqlUnion \n" +
                    "(id          INTEGER    PRIMARY KEY AUTOINCREMENT, \n" +
                    " permission  INTEGER    NOT NULL, \n" +
                    " playerID    REFERENCES players(id) ON UPDATE CASCADE ON DELETE CASCADE, \n" +
                    " doorUID     REFERENCES doors(id)   ON UPDATE CASCADE ON DELETE CASCADE, \n" +
                    " unique (playerID, doorUID));";
                stmt3.executeUpdate(sql3);
                stmt3.close();
                setDBVersion(conn, DATABASE_VERSION);
            }
        }
        catch (SQLException | NullPointerException e)
        {
            pLogger.logException(e);
            enabled = false;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getPermission(final @NotNull String playerUUID, final long doorUID)
    {
        int ret = -1;
        try (Connection conn = getConnection())
        {
            {
                String sql = "SELECT permission \n" +
                    "FROM sqlUnion INNER JOIN players ON players.id = sqlUnion.playerID \n" +
                    "WHERE players.playerUUID = ? AND doorUID = ?;";
                PreparedStatement ps = conn.prepareStatement(sql);
                ps.setString(1, playerUUID);
                ps.setLong(2, doorUID);
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

    /**
     * Construct a new Door from a resultset.
     *
     * @param rs        The resultset that contains the data of the animated object.
     * @param doorOwner The owner of this Door.
     * @return An Door if one could be found.
     */
    @NotNull
    private Optional<AbstractDoorBase> newDoorFromRS(final @NotNull ResultSet rs,
                                                     final @NotNull DoorOwner doorOwner)
    {
        AbstractDoorBase ret = null;
        try
        {
            AbstractDoorBase.DoorData doorData;
            {
                IPWorld world = BigDoors.get().getPlatform().getPWorldFactory()
                                        .create(UUID.fromString(rs.getString("world")));
                if (!world.exists())
                {
                    PLogger.get().logException(new NullPointerException(
                        "Failed to obtain the world of door \"" + doorOwner.getDoorUID() + "\"."));
                    return Optional.empty();
                }

                Vector3Di min = new Vector3Di(rs.getInt("xMin"), rs.getInt("yMin"), rs.getInt("zMin"));
                Vector3Di max = new Vector3Di(rs.getInt("xMax"), rs.getInt("yMax"), rs.getInt("zMax"));
                Vector3Di engine = new Vector3Di(rs.getInt("engineX"), rs.getInt("engineY"), rs.getInt("engineZ"));
                Vector3Di powerBlock = new Vector3Di(rs.getInt("powerBlockX"), rs.getInt("powerBlockY"),
                                                     rs.getInt("powerBlockZ"));
                boolean isOpen = BitFlag.hasFlag(DoorFlag.getFlagValue(DoorFlag.ISOPEN), rs.getInt("bitflag"));

                RotateDirection openDir = RotateDirection.valueOf(rs.getInt("openDirection"));
                if (openDir == null)
                {
                    PLogger.get().logException(
                        new NullPointerException("OpenDirection is null for door \"" + doorOwner.getDoorUID() + "\""));
                    return Optional.empty();
                }

                doorData = new AbstractDoorBase.DoorData(min, max, engine, powerBlock, world, isOpen, openDir);
            }
            AbstractDoorBase door = DoorType.valueOf(rs.getInt("type"))
                                            .getNewDoor(pLogger, doorOwner.getDoorUID(), doorData);

            door.setName(rs.getString("name"));
            door.setLock(BitFlag.hasFlag(DoorFlag.getFlagValue(DoorFlag.ISLOCKED), rs.getInt("bitflag")));
            door.setDoorOwner(doorOwner);
            door.setAutoClose(rs.getInt("autoClose"));
            door.setBlocksToMove(rs.getInt("blocksToMove"));

            ret = door;
        }
        catch (SQLException | NullPointerException | IllegalArgumentException e)
        {
            logMessage("282 " + e.getMessage(), e);
        }
        return Optional.ofNullable(ret);
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
            ps.setLong(1, doorUID);
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
    public boolean removeDoors(final @NotNull String playerUUID, final @NotNull String doorName)
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
    public boolean isBigDoorsWorld(@NotNull UUID worldUUID)
    {
        boolean result = false;
        try (Connection conn = getConnection())
        {
            String sql = "SELECT world FROM doors WHERE world=? LIMIT 1;";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, worldUUID.toString());
            result = ps.executeQuery().next();
        }
        catch (SQLException | NullPointerException e)
        {
            logMessage("438", e);
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getDoorCountForPlayer(final @NotNull UUID playerUUID)
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
    public int getDoorCountForPlayer(final @NotNull UUID playerUUID, final @NotNull String doorName)
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
    public int getDoorCountByName(final @NotNull String doorName)
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
    public int getOwnerCountOfDoor(final long doorUID)
    {
        try (Connection conn = getConnection())
        {
            String sql = "SELECT COUNT(*) AS total FROM sqlUnion WHERE doorUID=?;";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setLong(1, doorUID);
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
    @NotNull
    public Optional<AbstractDoorBase> getDoor(final @NotNull UUID playerUUID, final long doorUID)
    {
        Optional<AbstractDoorBase> door = Optional.empty();

        try (Connection conn = getConnection())
        {
            String sql = "SELECT D.*, P.playerUUID, P.playerName, U.permission \n" +
                "FROM doors as D INNER JOIN sqlUnion AS U ON U.doorUID = D.id INNER JOIN players AS P ON P.id = U.playerID \n" +
                "WHERE P.playerUUID = ? AND D.id = ?;";

            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, playerUUID.toString());
            ps.setLong(2, doorUID);
            ResultSet rs = ps.executeQuery();

            while (rs.next())
            {
                DoorOwner doorOwner = new DoorOwner(rs.getLong("id"), UUID.fromString(
                    rs.getString("playerUUID")),
                                                    rs.getString("playerName"),
                                                    rs.getInt("permission"));
                door = newDoorFromRS(rs, doorOwner);
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
    @NotNull
    public Optional<AbstractDoorBase> getDoor(final long doorUID)
    {
        Optional<AbstractDoorBase> door = Optional.empty();

        try (Connection conn = getConnection())
        {
            Optional<DoorOwner> doorOwner = getOwnerOfDoor(conn, doorUID);
            if (!doorOwner.isPresent())
                return door;
            String sql = "SELECT * FROM doors WHERE id=?;";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setLong(1, doorUID);
            ResultSet rs = ps.executeQuery();

            if (rs.next())
                door = newDoorFromRS(rs, doorOwner.get());

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
    @NotNull
    public Optional<List<AbstractDoorBase>> getDoors(final @NotNull UUID playerUUID, final @NotNull String name)
    {
        return getDoors(playerUUID.toString(), name, 0);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public Optional<List<AbstractDoorBase>> getDoors(final @NotNull UUID playerUUID)
    {
        return getDoors(playerUUID.toString(), 0);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public Optional<List<AbstractDoorBase>> getDoors(final @NotNull String name)
    {
        List<AbstractDoorBase> doors = new ArrayList<>();

        try (Connection conn = getConnection())
        {
            String sql = "SELECT * FROM doors WHERE name=?;";
            PreparedStatement ps1 = conn.prepareStatement(sql);
            ps1.setString(1, name);
            ResultSet rs1 = ps1.executeQuery();

            while (rs1.next())
                getOwnerOfDoor(conn, rs1.getLong("id")).flatMap(O -> newDoorFromRS(rs1, O)).ifPresent(doors::add);
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
    @NotNull
    public Optional<List<AbstractDoorBase>> getDoors(final @NotNull String playerUUID, final @NotNull String doorName,
                                                     int maxPermission)
    {
        List<AbstractDoorBase> doors = new ArrayList<>();

        try (Connection conn = getConnection())
        {
            String sql = "SELECT D.*, P.playerName, P.playerUUID, U.permission \n" +
                "FROM sqlUnion AS U INNER JOIN players AS P ON U.playerID = P.id INNER JOIN doors AS D ON U.doorUID = D.id \n" +
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
                newDoorFromRS(rs, doorOwner).ifPresent(doors::add);
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
    @NotNull
    public Optional<List<AbstractDoorBase>> getDoors(final @NotNull String playerUUID, int maxPermission)
    {
        List<AbstractDoorBase> ret = new ArrayList<>();
        try (Connection conn = getConnection())
        {
            String sql = "SELECT D.*, P.playerUUID, P.playerName, U.permission \n" +
                "FROM doors as D INNER JOIN sqlUnion AS U ON U.doorUID = D.id INNER JOIN players AS P ON P.id = U.playerID \n" +
                "WHERE P.playerUUID = ? AND permission <= ?;";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, playerUUID);
            ps.setInt(2, maxPermission);
            ResultSet rs = ps.executeQuery();

            while (rs.next())
            {
                DoorOwner doorOwner = new DoorOwner(rs.getLong("id"), UUID.fromString(rs.getString("playerUUID")),
                                                    rs.getString("playerName"), rs.getInt("permission"));
                newDoorFromRS(rs, doorOwner).ifPresent(ret::add);
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
    public boolean updatePlayerName(final @NotNull String playerUUID, final @NotNull String playerName)
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
    @NotNull
    public Optional<UUID> getPlayerUUID(final @NotNull String playerName)
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
    @NotNull
    public Optional<String> getPlayerName(final @NotNull String playerUUID)
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

    /**
     * Gets the original creator of a door using an established database connection.
     *
     * @param doorUID The door whose owner to get.
     * @return The original creator of a door.
     */
    @NotNull
    private Optional<DoorOwner> getOwnerOfDoor(final @NotNull Connection conn, final long doorUID)
        throws SQLException
    {
        DoorOwner doorOwner = null;
        String sql = "SELECT playerUUID, playerName \n" +
            "FROM players, \n" +
            "     (SELECT U.playerID, U.permission, doors.name \n" +
            "      FROM sqlUnion as U INNER JOIN doors ON doors.id = U.doorUID \n" +
            "      WHERE U.permission = '0' AND doors.id = ?) AS R \n" +
            "WHERE id = R.playerID;";
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setLong(1, doorUID);
        ResultSet rs = ps.executeQuery();
        if (rs.next())
            doorOwner = new DoorOwner(doorUID, UUID.fromString(rs.getString("playerUUID")), rs.getString("playerName"),
                                      0);
        ps.close();
        rs.close();

        return Optional.ofNullable(doorOwner);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public Optional<DoorOwner> getOwnerOfDoor(final long doorUID)
    {
        try (Connection conn = getConnection())
        {
            return getOwnerOfDoor(conn, doorUID);
        }
        catch (SQLException | NullPointerException e)
        {
            logMessage("788", e);
        }
        return Optional.empty();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public ConcurrentHashMap<Integer, List<Long>> getPowerBlockData(final long chunkHash)
    {
        ConcurrentHashMap<Integer, List<Long>> doors = new ConcurrentHashMap<>();

        try (Connection conn = getConnection())
        {
            // Get the door associated with the x/y/z location of the power block block.
            PreparedStatement ps = conn.prepareStatement("SELECT * FROM doors WHERE chunkHash=?;");
            ps.setLong(1, chunkHash);
            ResultSet rs = ps.executeQuery();
            while (rs.next())
            {
                int locationHash = Util.simpleChunkSpaceLocationhash(rs.getInt("powerBlockX"),
                                                                     rs.getInt("powerBlockY"),
                                                                     rs.getInt("powerBlockZ"));
                if (!doors.containsKey(locationHash))
                    doors.put(locationHash, new ArrayList<>());
                doors.get(locationHash).add(rs.getLong("id"));
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
            ps.setLong(2, doorUID);
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
                                 final int zMin, final int xMax, final int yMax, final int zMax)
    {
        try (Connection conn = getConnection())
        {
            conn.setAutoCommit(false);
            PreparedStatement ps = conn.prepareStatement("UPDATE doors SET xMin=?,yMin=?,zMin=?,xMax=?" +
                                                             ",yMax=?,zMax=? WHERE id =?;");
            int idx = 1;
            ps.setInt(idx++, xMin);
            ps.setInt(idx++, yMin);
            ps.setInt(idx++, zMin);
            ps.setInt(idx++, xMax);
            ps.setInt(idx++, yMax);
            ps.setInt(idx++, zMax);
            ps.setLong(idx++, doorUID);
            ps.executeUpdate();
            conn.commit();
        }
        catch (SQLException | NullPointerException e)
        {
            logMessage("976", e);
        }

        changeDoorFlag(doorUID, DoorFlag.ISOPEN, isOpen);
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
            ps.setLong(2, doorUID);
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
    public void updateDoorOpenDirection(final long doorUID, final @NotNull RotateDirection openDir)
    {
        try (Connection conn = getConnection())
        {
            conn.setAutoCommit(false);
            PreparedStatement ps = conn.prepareStatement("UPDATE doors SET openDirection=? WHERE id=?;");
            ps.setInt(1, RotateDirection.getValue(openDir));
            ps.setLong(2, doorUID);
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
                                        final @NotNull UUID worldUUID)
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
            ps.setLong(4, Util.simpleChunkHashFromLocation(xPos, zPos));
            ps.setLong(5, doorUID);
            ps.executeUpdate();
            conn.commit();
        }
        catch (SQLException | NullPointerException e)
        {
            logMessage("992", e);
        }
    }

    /**
     * Changes the flag status of a door.
     *
     * @param doorUID    The UID fo the door.
     * @param flag       The {@link DoorFlag} to change.
     * @param flagStatus Whether to enable or disable the {@link DoorFlag}.
     */
    private void changeDoorFlag(final long doorUID, final @NotNull DoorFlag flag, final boolean flagStatus)
    {

        try (Connection conn = getConnection())
        {
            PreparedStatement ps = conn.prepareStatement("SELECT bitflag FROM doors WHERE id=?;");
            ps.setLong(1, doorUID);
            ResultSet rs = ps.executeQuery();
            if (rs.next())
            {
                int newFlag = BitFlag.changeFlag(DoorFlag.getFlagValue(flag), flagStatus, rs.getInt(1));
                PreparedStatement ps1 = conn.prepareStatement("UPDATE doors SET bitflag=? WHERE id=?;");
                ps1.setInt(1, newFlag);
                ps1.setLong(2, doorUID);
                ps1.executeUpdate();
                ps1.close();
            }

            rs.close();
            ps.close();
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
    public void setLock(final long doorUID, final boolean newLockStatus)
    {
        changeDoorFlag(doorUID, DoorFlag.ISLOCKED, newLockStatus);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void insert(final @NotNull AbstractDoorBase door)
    {
        try (Connection conn = getConnection())
        {
            long playerID = getPlayerID(conn, door.getDoorOwner());
            if (playerID == -1)
            {
                pLogger.logException(new IllegalStateException(
                    "Failed to add player \"" + door.getDoorOwner().getPlayerUUID().toString() +
                        "\" to the database! Aborting door insertion!"));
                conn.close();
                return;
            }

            String doorInsertsql = "INSERT INTO doors(name,world,xMin,yMin,zMin,xMax,yMax,zMax, \n" +
                "                  engineX,engineY,engineZ,bitflag,type, \n" +
                "                  powerBlockX,powerBlockY,powerBlockZ,openDirection, \n" +
                "                  autoClose,chunkHash,blocksToMove) \n" +
                "                  VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?);";
            PreparedStatement doorstatement = conn.prepareStatement(doorInsertsql);

            int idx = 1;
            doorstatement.setString(idx++, door.getName());
            doorstatement.setString(idx++, door.getWorld().getUID().toString());
            doorstatement.setInt(idx++, door.getMinimum().getX());
            doorstatement.setInt(idx++, door.getMinimum().getY());
            doorstatement.setInt(idx++, door.getMinimum().getZ());
            doorstatement.setInt(idx++, door.getMaximum().getX());
            doorstatement.setInt(idx++, door.getMaximum().getY());
            doorstatement.setInt(idx++, door.getMaximum().getZ());
            doorstatement.setInt(idx++, door.getEngine().getX());
            doorstatement.setInt(idx++, door.getEngine().getY());
            doorstatement.setInt(idx++, door.getEngine().getZ());
            doorstatement.setInt(idx++, getFlag(door));
            doorstatement.setInt(idx++, DoorType.getValue(door.getType()));
            doorstatement.setInt(idx++, door.getPowerBlockLoc().getX());
            doorstatement.setInt(idx++, door.getPowerBlockLoc().getY());
            doorstatement.setInt(idx++, door.getPowerBlockLoc().getZ());
            doorstatement.setInt(idx++, RotateDirection.getValue(door.getOpenDir()));
            doorstatement.setInt(idx++, door.getAutoClose());
            doorstatement.setLong(idx++, door.getSimplePowerBlockChunkHash());
            doorstatement.setInt(idx++, door.getBlocksToMove());

            doorstatement.execute();
            doorstatement.close();

            String getDoorUID = "SELECT last_insert_rowid() AS lastId";
            PreparedStatement ps2 = conn.prepareStatement(getDoorUID);
            ResultSet rs2 = ps2.executeQuery();
            long doorUID = rs2.getLong("lastId");
            ps2.close();
            rs2.close();

            String sql3 = "INSERT INTO sqlUnion (permission, playerID, doorUID) VALUES (?,?,?);";
            PreparedStatement ps3 = conn.prepareStatement(sql3);
            ps3.setInt(1, door.getPermission());
            ps3.setLong(2, playerID);
            ps3.setLong(3, doorUID);
            ps3.execute();
            ps3.close();
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
    public boolean removeOwner(final long doorUID, final @NotNull String playerUUID)
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
            ps.setLong(2, doorUID);
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
            ps.setLong(1, doorUID);
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
     * Gets the ID player in the "players" table. If the player isn't in the database yet, they are added first.
     *
     * @param conn      The connection to the database.
     * @param doorOwner The doorOwner with the player to retrieve.
     * @return The database ID of the player.
     */
    private long getPlayerID(final @NotNull Connection conn, final @NotNull DoorOwner doorOwner)
        throws SQLException
    {
        String insertPlayerSql = "INSERT OR IGNORE INTO players(playerUUID, playerName) VALUES(?, ?)";
        PreparedStatement insertPlayer = conn.prepareStatement(insertPlayerSql);
        insertPlayer.setString(1, doorOwner.getPlayerUUID().toString());
        insertPlayer.setString(2, doorOwner.getPlayerName());
        insertPlayer.executeUpdate();
        insertPlayer.close();

        String getPlayerSQL = "SELECT id FROM players WHERE playerUUID = ?;";
        PreparedStatement getPlayer = conn.prepareStatement(getPlayerSQL);
        getPlayer.setString(1, doorOwner.getPlayerUUID().toString());
        ResultSet playerIDRS = getPlayer.executeQuery();

        long playerID = playerIDRS.next() ? playerIDRS.getLong("id") : -1;
        getPlayer.close();
        playerIDRS.close();
        return playerID;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addOwner(final long doorUID, final @NotNull IPPlayer player, final int permission)
    {
        // permission level 0 is reserved for the creator, and negative values are not allowed.
        if (permission < 1)
            return;

        try (Connection conn = getConnection())
        {
            @Nullable final String playerName = player.getName();
            if (playerName == null)
            {
                pLogger.logException(new NullPointerException(
                    "Trying to add player \"" + player.getUUID().toString() + "\" as owner of door " + doorUID +
                        ", but that player's name could not be retrieved! Aborting..."));
                conn.close();
                return;
            }
            long playerID = getPlayerID(conn, new DoorOwner(doorUID, player.getUUID(), playerName, permission));
            if (playerID == -1)
            {
                pLogger.logException(new IllegalArgumentException(
                    "Trying to add player \"" + player.getUUID().toString() + "\" as owner of door " + doorUID +
                        ", but that player is not registered in the database! Aborting..."));
                conn.close();
                return;
            }

            String sql2 = "SELECT * FROM sqlUnion WHERE playerID=? AND doorUID=?;";
            PreparedStatement ps2 = conn.prepareStatement(sql2);
            ps2.setLong(1, playerID);
            ps2.setLong(2, doorUID);
            ResultSet rs2 = ps2.executeQuery();

            // If it already exists, update the permission, if needed.
            if (rs2.next())
            {
                if (rs2.getInt("permission") != permission)
                {
                    String sql3 = "UPDATE sqlUnion SET permission=? WHERE playerID=? and doorUID=?;";
                    PreparedStatement ps3 = conn.prepareStatement(sql3);
                    ps3.setInt(1, permission);
                    ps3.setLong(2, playerID);
                    ps3.setLong(3, doorUID);
                    ps3.executeUpdate();
                    ps3.close();
                }
            }
            else
            {
                String sql4 = "INSERT INTO sqlUnion (permission, playerID, doorUID) values(?,?,?);";
                PreparedStatement ps4 = conn.prepareStatement(sql4);
                ps4.setInt(1, permission);
                ps4.setLong(2, playerID);
                ps4.setLong(3, doorUID);
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

    /**
     * Upgrades the database to the latest version if needed.
     */
    private void upgrade()
    {
        Connection conn = null;
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

            if (dbVersion < MIN_DATABASE_VERSION || dbVersion > DATABASE_VERSION)
            {
                pLogger.logMessage("Trying to load a database that is incompatible with this version of the plugin! " +
                                       "Database version = " + dbVersion + ". Please update the plugin.");
                conn.close();
                validVersion = false;
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

            if (dbVersion < 11)
                upgradeToV11(conn);

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
    }

    /**
     * Checks if the {@link SQLiteJDBCDriverConnection#FAKEUUID} exists in the database.
     *
     * @param conn The active connection to the database.
     * @return True if the {@link SQLiteJDBCDriverConnection#FAKEUUID} exists in the database.
     */
    private boolean fakeUUIDExists(final @NotNull Connection conn)
    {
        try
        {
            String sql1 = "SELECT COUNT(*) AS total FROM players WHERE playerUUID = ?;";
            PreparedStatement ps = conn.prepareStatement(sql1);
            ps.setString(1, FAKEUUID);
            ResultSet rs = ps.executeQuery();
            int count = rs.getInt("total");
            ps.close();
            rs.close();
            return count > 0;
        }
        catch (SQLException | NullPointerException e)
        {
            logMessage("1461", e);
        }
        return false;
    }

    /**
     * Makes a backup of the database file. Stored in a database with the same name, but with ".BACKUP" appended to it.
     *
     * @return True if backup creation was successful.
     */
    private boolean makeBackup()
    {
        File dbFileBackup = new File(dbFile.toString() + ".BACKUP");
        // Only the most recent backup is kept, so delete the old one if a new one needs
        // to be created.
        if (dbFileBackup.exists())
            if (!dbFileBackup.delete())
            {
                pLogger.logException(new IOException("Failed to delete old backup! Aborting backup creation!"));
                return false;
            }
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

    /**
     * Modifies the version of the database.
     *
     * @param conn    An active connection to the database.
     * @param version The new version of the database.
     */
    private void setDBVersion(final @NotNull Connection conn, final int version)
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

    /**
     * Upgrades the database to V11.
     *
     * @param conn Opened database connection.
     */
    /*
     * Changes in V11:
     * - Updating chunkHash of all doors because the algorithm was changed.
     */
    private void upgradeToV11(final @NotNull Connection conn)
    {
        try
        {
            pLogger.warn("Upgrading database to V11!");
            PreparedStatement ps1 = conn.prepareStatement("SELECT * FROM doors;");
            ResultSet rs1 = ps1.executeQuery();
            String update;

            while (rs1.next())
            {
                long UID = rs1.getLong("id");
                int x = rs1.getInt("powerBlockX");
                int z = rs1.getInt("powerBlockZ");

                update = "UPDATE doors SET chunkHash=? WHERE id=?;";
                PreparedStatement ps2 = conn.prepareStatement(update);
                ps2.setLong(1, Util.simpleChunkHashFromLocation(x, z));
                ps2.setLong(2, UID);
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

    /**
     * (Un)locks the database.
     *
     * @param lockStatus Whether or not the database will be locked.
     */
    private void setDatabaseLock(boolean lockStatus)
    {
        locked.set(lockStatus);
    }

    private void logMessage(String str, Exception e)
    {
        if (!locked.get())
            pLogger.logException(e, str);
        else
            pLogger.warn("Database locked! Failed at: " + str + ". Message: " + e.getMessage());
    }
}
