package nl.pim16aap2.bigdoors.storage.sqlite;

import nl.pim16aap2.bigdoors.config.ConfigLoader;
import nl.pim16aap2.bigdoors.doors.DoorBase;
import nl.pim16aap2.bigdoors.doors.DoorType;
import nl.pim16aap2.bigdoors.exceptions.TooManyDoorsException;
import nl.pim16aap2.bigdoors.spigotutil.PlayerRetriever;
import nl.pim16aap2.bigdoors.spigotutil.WorldRetriever;
import nl.pim16aap2.bigdoors.storage.IStorage;
import nl.pim16aap2.bigdoors.util.BitFlag;
import nl.pim16aap2.bigdoors.util.DoorOwner;
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
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
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
    private final ConfigLoader config;

    /**
     * Object that can retrieve a {@link World} from a String.
     */
    private final WorldRetriever worldRetriever;

    /**
     * Object that can retrieve a {@link org.bukkit.entity.Player} from a String.
     */
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
    public SQLiteJDBCDriverConnection(final @NotNull File dbFile, final @NotNull PLogger pLogger,
                                      final @NotNull ConfigLoader config, final @NotNull WorldRetriever worldRetriever,
                                      final @NotNull PlayerRetriever playerRetriever)
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
    private void disableForeignKeys(final @NotNull Connection conn) throws SQLException
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
    private void reEnableForeignKeys(final @NotNull Connection conn) throws SQLException
    {
        conn.createStatement().execute("PRAGMA foreign_keys=ON");
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
    private Optional<DoorBase> newDoorFromRS(final @NotNull ResultSet rs,
                                             final @NotNull DoorOwner doorOwner)
    {
        DoorBase ret = null;
        try
        {
//            DoorBase door = DoorType.valueOf(rs.getInt("type")).getNewDoor(pLogger, doorOwner.getDoorUID());


            DoorBase.DoorData doorData;
            {
                World world = Objects
                    .requireNonNull(worldRetriever.worldFromString(UUID.fromString(rs.getString("world"))),
                                    "Failed to obtain the world of door \"" + doorOwner.getDoorUID() + "\".");
                Location min = new Location(world, rs.getInt("xMin"), rs.getInt("yMin"), rs.getInt("zMin"));
                Location max = new Location(world, rs.getInt("xMax"), rs.getInt("yMax"), rs.getInt("zMax"));
                Location engine = new Location(world, rs.getInt("engineX"), rs.getInt("engineY"), rs.getInt("engineZ"));
                Location powerBlock = new Location(world, rs.getInt("powerBlockX"), rs.getInt("powerBlockY"),
                                                   rs.getInt("powerBlockZ"));
                boolean isOpen = BitFlag.hasFlag(DoorFlag.getFlagValue(DoorFlag.ISOPEN), rs.getInt("bitflag"));
                RotateDirection openDir = Objects.requireNonNull(RotateDirection.valueOf(rs.getInt("openDirection")),
                                                                 "Failed to obtain the open direction of door \"" +
                                                                     doorOwner.getDoorUID() + "\".");
                doorData = new DoorBase.DoorData(min, max, engine, powerBlock, world, isOpen, openDir);
            }
            DoorBase door = DoorType.valueOf(rs.getInt("type")).getNewDoor(pLogger, doorOwner.getDoorUID(), doorData);

            door.setName(rs.getString("name"));
            door.setLock(BitFlag.hasFlag(DoorFlag.getFlagValue(DoorFlag.ISLOCKED), rs.getInt("bitflag")));
            door.setDoorOwner(doorOwner);
            door.setAutoClose(rs.getInt("autoClose"));
            door.setBlocksToMove(rs.getInt("blocksToMove"));

            ret = door;
        }
        catch (SQLException | NullPointerException | IllegalArgumentException e)
        {
            logMessage("282", e);
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
    public Optional<DoorBase> getDoor(final @NotNull UUID playerUUID, final long doorUID)
    {
        Optional<DoorBase> door = Optional.empty();

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
    public Optional<DoorBase> getDoor(final long doorUID)
    {
        Optional<DoorBase> door = Optional.empty();

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
    public Optional<List<DoorBase>> getDoors(final @NotNull UUID playerUUID, final @NotNull String name)
    {
        return getDoors(playerUUID.toString(), name, 0);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public Optional<List<DoorBase>> getDoors(final @NotNull UUID playerUUID)
    {
        return getDoors(playerUUID.toString(), 0);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public Optional<List<DoorBase>> getDoors(final @NotNull String name)
    {
        List<DoorBase> doors = new ArrayList<>();

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
    public Optional<List<DoorBase>> getDoors(final @NotNull String playerUUID,
                                             final @NotNull String doorName,
                                             int maxPermission)
    {
        List<DoorBase> doors = new ArrayList<>();

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
    public Optional<List<DoorBase>> getDoors(final @NotNull String playerUUID, int maxPermission)
    {
        List<DoorBase> ret = new ArrayList<>();
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
    @NotNull
    public Optional<DoorBase> getDoor(final @NotNull String playerUUID, final @NotNull String doorName)
        throws TooManyDoorsException
    {
        return getDoor(playerUUID, doorName, Integer.MAX_VALUE);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public Optional<DoorBase> getDoor(final @NotNull String playerUUID, final @NotNull String doorName,
                                      int maxPermission)
        throws TooManyDoorsException
    {
        Optional<DoorBase> door = Optional.empty();
        int count = 0;

        try (Connection conn = getConnection())
        {
            String sql = "SELECT D.*, P.playerUUID, P.playerName, U.permission \n" +
                "FROM doors as D INNER JOIN sqlUnion AS U ON U.doorUID = D.id INNER JOIN players AS P ON P.id = U.playerID \n" +
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
                door = newDoorFromRS(rs, doorOwner);
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
        return door;
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
    public Map<Integer, List<Long>> getPowerBlockData(final long chunkHash)
    {
        Map<Integer, List<Long>> doors = new HashMap<>();

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
    public void insert(final @NotNull DoorBase door)
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
            doorstatement.setInt(idx++, door.getMinimum().getBlockX());
            doorstatement.setInt(idx++, door.getMinimum().getBlockY());
            doorstatement.setInt(idx++, door.getMinimum().getBlockZ());
            doorstatement.setInt(idx++, door.getMaximum().getBlockX());
            doorstatement.setInt(idx++, door.getMaximum().getBlockY());
            doorstatement.setInt(idx++, door.getMaximum().getBlockZ());
            doorstatement.setInt(idx++, door.getEngine().getBlockX());
            doorstatement.setInt(idx++, door.getEngine().getBlockY());
            doorstatement.setInt(idx++, door.getEngine().getBlockZ());
            doorstatement.setInt(idx++, getFlag(door));
            doorstatement.setInt(idx++, DoorType.getValue(door.getType()));
            doorstatement.setInt(idx++, door.getPowerBlockLoc().getBlockX());
            doorstatement.setInt(idx++, door.getPowerBlockLoc().getBlockY());
            doorstatement.setInt(idx++, door.getPowerBlockLoc().getBlockZ());
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
     *
     * @throws SQLException
     */
    private long getPlayerID(final @NotNull Connection conn, final @NotNull DoorOwner doorOwner) throws SQLException
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
    public void addOwner(final long doorUID, final @NotNull UUID playerUUID, final int permission)
    {
        // permission level 0 is reserved for the creator, and negative values are not allowed.
        if (permission < 1)
            return;

        try (Connection conn = getConnection())
        {
            @Nullable final String playerName = playerRetriever.getOfflinePlayer(playerUUID).getName();
            if (playerName == null)
            {
                pLogger.logException(new NullPointerException(
                    "Trying to add player \"" + playerUUID.toString() + "\" as owner of door " + doorUID +
                        ", but that player's name could not be retrieved! Aborting..."));
                conn.close();
                return;
            }
            long playerID = getPlayerID(conn, new DoorOwner(doorUID, playerUUID, playerName, permission));
            if (playerID == -1)
            {
                pLogger.logException(new IllegalArgumentException(
                    "Trying to add player \"" + playerUUID.toString() + "\" as owner of door " + doorUID +
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
                conn = getConnection();
                replaceTempPlayerNames = fakeUUIDExists(conn);
            }

            if (dbVersion < 6)
            {
                conn.close();
                upgradeToV6();
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
        // Replacing temporary player names is handled on a separate thread, so do it after.
        if (replaceTempPlayerNames)
            startReplaceTempPlayerNames();
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
     * Upgrades the database from V0 to V1.
     *
     * @param conn An active connection to the database.
     */
    /*
     * Changes in V1:
     * - New columns "type", "engineSide", "powerBlockX", "powerBlockY", "powerBlockZ", "openDirection", "autoClose",
     *   "chunkHash" to table "doors".
     * - Whether or not a drawbridge is considered open or closed is changed in this version.
     */
    private void upgradeToV1(final @NotNull Connection conn)
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
                    long UID = rs1.getLong("id");
                    int x = rs1.getInt("engineX");
                    int y = rs1.getInt("engineY") - 1;
                    int z = rs1.getInt("engineZ");

                    update = "UPDATE doors SET powerBlockX=?, powerBlockY=?, powerBlockZ=? WHERE id=?;";
                    PreparedStatement ps2 = conn.prepareStatement(update);
                    ps2.setInt(1, x);
                    ps2.setInt(2, y);
                    ps2.setInt(3, z);
                    ps2.setLong(4, UID);
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
            rs.close();
            pLogger.warn("Database has been upgraded to V1!");
        }
        catch (SQLException | NullPointerException e)
        {
            logMessage("1593", e);
        }
    }

    /**
     * Upgrades the database from V1 to V2.
     *
     * @param conn An active connection to the database.
     */
    /*
     * Changes in V2:
     * - Added column "blocksToMove" to table "doors".
     */
    private void upgradeToV2(final @NotNull Connection conn)
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

    /**
     * Upgrades the database from V2 to V3.
     *
     * @param conn An active connection to the database.
     */
    /*
     * Changes in V3:
     * - Added uniqueness constraint to the playerID and doorUID combination of the table "sqlUnion".
     *   Every player can own a single door at most once!
     *
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
    private void upgradeToV3(final @NotNull Connection conn)
    {
        try
        {
            pLogger.warn("Upgrading database to V3! Recreating sqlUnion!");
            conn.setAutoCommit(false);

//            conn.createStatement().execute("PRAGMA foreign_keys=OFF");
            disableForeignKeys(conn);
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
//                conn.createStatement().execute("PRAGMA foreign_keys=ON");
                reEnableForeignKeys(conn);
                conn.rollback();
            }
            catch (SQLException e1)
            {
                logMessage("1285", e1);
            }
            logMessage("1287", e);
        }
    }

    /**
     * Upgrades the database from V3 to V4.
     *
     * @param conn An active connection to the database.
     */
    /*
     * Changes in V4:
     * - Added column "playerName" to table "players".
     */
    private void upgradeToV4(final @NotNull Connection conn)
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

    /**
     * Upgrades the database from V4 to V5.
     */
    /*
     * Changes in V5:
     * - Now storing the names of players for ALL players.
     * - NOT NULL constraint added to column "playerName" in table "players".
     *
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

            // If there aren't any null values, there's no need to fill them in.
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

                // Store the fake playername in the entry with the fake UUID.
                {
                    String sql = "INSERT INTO players (playerUUID, playerName) VALUES(?,?);";
                    PreparedStatement ps1 = conn.prepareStatement(sql);
                    ps1.setString(1, FAKEUUID);
                    ps1.setString(2, fakeName);
                    ps1.executeUpdate();
                    ps1.close();
                }

                // Replace all "NULL" entries of the "playerName" column in the "players" table.
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
//            conn.createStatement().execute("PRAGMA foreign_keys=OFF");
            disableForeignKeys(conn);
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
     * Upgrades the database from V5 to V11.
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
     * Upgrades the database from V5 to V6.
     */
    /*
     * Changes in V6:
     * - Merged isOpen and isLocked into a single bit flag.
     */
    private void upgradeToV6()
    {
        try (Connection conn = getConnection())
        {
            pLogger.warn("Upgrading database to V6!");

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

                int flag = 0;
                flag = BitFlag.changeFlag(DoorFlag.getFlagValue(DoorFlag.ISOPEN), isOpen, flag);
                flag = BitFlag.changeFlag(DoorFlag.getFlagValue(DoorFlag.ISLOCKED), isLocked, flag);

                update = "UPDATE doors SET bitflag=? WHERE id=?;";
                PreparedStatement ps2 = conn.prepareStatement(update);
                ps2.setInt(1, flag);
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

        Connection conn = null;
        try
        {
            Class.forName(DRIVER);
            conn = DriverManager.getConnection(url);
//            conn.createStatement().execute("PRAGMA foreign_keys=OFF");
            disableForeignKeys(conn);
            conn.setAutoCommit(false);
            conn.createStatement().execute("ALTER TABLE doors RENAME TO doors_old;");

            String newDoors = "CREATE TABLE IF NOT EXISTS doors \n" +
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
                " type          INTEGER    NOT NULL DEFAULT 0, \n" +
                " powerBlockX   INTEGER    NOT NULL DEFAULT -1, \n" +
                " powerBlockY   INTEGER    NOT NULL DEFAULT -1, \n" +
                " powerBlockZ   INTEGER    NOT NULL DEFAULT -1, \n" +
                " openDirection INTEGER    NOT NULL DEFAULT  0, \n" +
                " autoClose     INTEGER    NOT NULL DEFAULT -1, \n" +
                " chunkHash     INTEGER    NOT NULL DEFAULT -1, \n" +
                " blocksToMove  INTEGER    NOT NULL DEFAULT -1);";
            conn.createStatement().execute(newDoors);

            String restoreData = "INSERT INTO doors \n" +
                "SELECT id, name, world, xMin, yMin, zMin, xMax, yMax, " +
                "zMax, engineX, engineY, engineZ, bitflag, type, \n" +
                "powerBlockX, powerBlockY, powerBlockZ, openDirection, autoClose, chunkHash, blocksToMove \n" +
                "FROM doors_old;";
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
                catch (SQLException e1)
                {
                    logMessage("1770", e1);
                }
            logMessage("2013", e);
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
     * (Un)locks the database.
     *
     * @param lockStatus Whether or not the database will be locked.
     */
    private void setDatabaseLock(boolean lockStatus)
    {
        locked.set(lockStatus);
    }

    /**
     * Replace all temporary playerNames. This means all rows in the table "players" that contain the same name as the
     * row whose "playerUUID" value matches {@link SQLiteJDBCDriverConnection#FAKEUUID}.
     */
    /*
     * Part of the upgrade to V5 of the database. First lock the database so nothing
     * else can interfere. Then find the temporary name from the fake player (that
     * was stored in the db). All fake names then get replaced by the real names. Or
     * the last ones they used, anyway.
     */
    private void replaceTempPlayerNames()
    {
        try (Connection conn = getConnectionUnsafe())
        {
            String sql1 = "SELECT * FROM players WHERE playerUUID = ?;";
            PreparedStatement ps1 = conn.prepareStatement(sql1);
            ps1.setString(1, FAKEUUID);
            ResultSet rs1 = ps1.executeQuery();

            String fakeName = null;
            while (rs1.next())
                fakeName = rs1.getString("playerName");
            rs1.close();
            ps1.close();

            String sql2 = "SELECT * FROM players WHERE playerName = ?;";
            PreparedStatement ps2 = conn.prepareStatement(sql2);
            ps2.setString(1, fakeName);
            ResultSet rs2 = ps2.executeQuery();

            List<String> UUIDs = new ArrayList<>();
            while (rs2.next())
            {
                if (rs2.getString("playerUUID").equals(FAKEUUID))
                    continue;
                UUIDs.add(rs2.getString("playerUUID"));
            }
            ps2.close();
            rs2.close();

            for (String uuidStr : UUIDs)
            {
                String playerName = playerRetriever.getOfflinePlayer(UUID.fromString(uuidStr)).getName();

                String sql3 = "UPDATE players SET playerName = ? WHERE playerUUID = ?;";
                PreparedStatement ps3 = conn.prepareStatement(sql3);
                ps3.setString(1, playerName);
                ps3.setString(2, uuidStr);
                ps3.executeUpdate();
                ps3.close();
            }

//            while (rs2.next())
//            {
//                if (rs2.getString("playerUUID").equals(FAKEUUID))
//                    continue;
//                UUID playerUUID = UUID.fromString(rs2.getString("playerUUID"));
//                String playerName = playerRetriever.getOfflinePlayer(playerUUID).getName();
//
//                String sql3 = "UPDATE players SET playerName = ? WHERE playerUUID = ?;";
//                PreparedStatement ps3 = conn.prepareStatement(sql3);
//                ps3.setString(1, playerName);
//                ps3.setString(2, playerUUID.toString());
//                ps3.executeUpdate();
//                ps3.close();
//            }
//            ps2.close();
//            rs2.close();

            String sql4 = "DELETE FROM players WHERE playerUUID = ?;";
            PreparedStatement ps4 = conn.prepareStatement(sql4);
            ps4.setString(1, FAKEUUID);
            ps4.executeUpdate();
            ps4.close();
        }
        catch (SQLException e)
        {
            pLogger.logException(e);
        }
        setDatabaseLock(false);
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
//            conn.createStatement().execute("PRAGMA foreign_keys=OFF");
            disableForeignKeys(conn);
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


                String selectOldDoors = "SELECT id, name, world, xMin, yMin, zMin, xMax, yMax," +
                    "zMax, engineX, engineY, engineZ, bitflag \n" +
                    "FROM doors_old;";
                PreparedStatement ps = conn.prepareStatement(selectOldDoors);
                ResultSet rs = ps.executeQuery();

                while (rs.next())
                {
                    String doorInsertsql =
                        "INSERT INTO doors(id, name, world, isOpen, xMin, yMin, zMin, xMax, yMax, zMax, \n" +
                            "                  engineX, engineY, engineZ, isLocked) \n" +
                            "                  VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?);";
                    PreparedStatement ps1 = conn.prepareStatement(doorInsertsql);

                    ps1.setString(1, rs.getString("id"));
                    ps1.setString(2, rs.getString("name"));
                    ps1.setString(3, rs.getString("world"));
                    ps1.setInt(4,
                               BitFlag.hasFlag(DoorFlag.getFlagValue(DoorFlag.ISOPEN), rs.getInt("bitflag")) ? 1 : 0);
                    ps1.setInt(5, rs.getInt("xMin"));
                    ps1.setInt(6, rs.getInt("yMin"));
                    ps1.setInt(7, rs.getInt("zMin"));
                    ps1.setInt(8, rs.getInt("xMax"));
                    ps1.setInt(9, rs.getInt("yMax"));
                    ps1.setInt(10, rs.getInt("zMax"));
                    ps1.setInt(11, rs.getInt("engineX"));
                    ps1.setInt(12, rs.getInt("engineY"));
                    ps1.setInt(13, rs.getInt("engineZ"));
                    ps1.setInt(14,
                               BitFlag.hasFlag(DoorFlag.getFlagValue(DoorFlag.ISLOCKED), rs.getInt("bitflag")) ? 1 : 0);

                    ps1.execute();

                }

                rs.close();
                ps.close();

                conn.createStatement().execute("DROP TABLE IF EXISTS doors_old;");
                conn.commit();
            }

            // Reset table 'players'
            {
                conn.createStatement().execute("ALTER TABLE players RENAME TO players_old;");

                conn.createStatement()
                    .execute("CREATE TABLE players \n" +
                                 "(id          INTEGER    PRIMARY KEY AUTOINCREMENT, \n" +
                                 " playerUUID  TEXT       NOT NULL);");


                conn.createStatement().execute("INSERT INTO players \n" +
                                                   "SELECT id, playerUUID \n" +
                                                   "FROM players_old;");

                conn.createStatement().execute("DROP TABLE players_old;");
                conn.commit();
            }

            // Reset table 'sqlUnion'
            {
                conn.createStatement().execute("ALTER TABLE sqlUnion RENAME TO sqlUnion_old;");

                conn.createStatement()
                    .execute("CREATE TABLE sqlUnion \n" +
                                 "(id          INTEGER    PRIMARY KEY AUTOINCREMENT, \n" +
                                 " permission  INTEGER    NOT NULL, \n" +
                                 " playerID    REFERENCES players(id) ON UPDATE CASCADE ON DELETE CASCADE, \n" +
                                 " doorUID     REFERENCES doors(id)   ON UPDATE CASCADE ON DELETE CASCADE);");

                conn.createStatement().execute("INSERT INTO sqlUnion \n" +
                                                   "SELECT * \n" +
                                                   "FROM sqlUnion_old;");

                conn.createStatement().execute("DROP TABLE IF EXISTS sqlUnion_old;");
                conn.commit();
            }
            conn.setAutoCommit(true);
//            conn.createStatement().execute("PRAGMA foreign_keys=ON");
            reEnableForeignKeys(conn);
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
