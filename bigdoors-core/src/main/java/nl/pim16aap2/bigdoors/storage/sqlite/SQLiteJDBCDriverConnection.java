package nl.pim16aap2.bigdoors.storage.sqlite;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.api.IConfigLoader;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.api.IPWorld;
import nl.pim16aap2.bigdoors.doors.AbstractDoorBase;
import nl.pim16aap2.bigdoors.doors.DoorType;
import nl.pim16aap2.bigdoors.storage.IStorage;
import nl.pim16aap2.bigdoors.storage.PPreparedStatement;
import nl.pim16aap2.bigdoors.storage.SQLStatement;
import nl.pim16aap2.bigdoors.util.BitFlag;
import nl.pim16aap2.bigdoors.util.DoorOwner;
import nl.pim16aap2.bigdoors.util.Functional.CheckedFunction;
import nl.pim16aap2.bigdoors.util.PLogger;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import nl.pim16aap2.bigdoors.util.Util;
import nl.pim16aap2.bigdoors.util.vector.Vector3Di;
import org.jetbrains.annotations.Contract;
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
        if (!loadDriver())
        {
            // TODO: Handle this properly.
            enabled = false;
            return;
        }
        init();
        upgrade();
    }

    /**
     * Loads the driver's class file into memory at runtime.
     *
     * @return True if the driver was loaded successfully.
     */
    private boolean loadDriver()
    {
        try
        {
            Class.forName(DRIVER);
            return true;
        }
        catch (ClassNotFoundException e)
        {
            e.printStackTrace();
        }
        return false;
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
        if (!validVersion)
        {
            final IllegalStateException e = new IllegalStateException(
                "Database disabled! Reason: Version too low! This is not a bug!\n" +
                    "If you want to use your old database, you'll have to upgrade it to v2 first.\n" +
                    "Please download the latest version of v1 first. For more info, go to the resource page.\n");
            pLogger.logException(e);
            throw e;
        }

        if (!enabled)
        {
            final IllegalStateException e = new IllegalStateException(
                "Database disabled! This probably means an upgrade failed! " +
                    "Please contact pim16aap2.");
            pLogger.logException(e);
            throw e;
        }

        Connection conn = null;
        try
        {
            conn = DriverManager.getConnection(url);
            SQLStatement.FOREIGN_KEYS_ON.constructPPreparedStatement().construct(conn).execute();
        }
        catch (SQLException e)
        {
            pLogger.logException(e, "Failed to open connection!");
        }
        if (conn == null)
        {
            final NullPointerException e = new NullPointerException("Could not open connection!");
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
        SQLStatement.FOREIGN_KEYS_OFF.constructPPreparedStatement().construct(conn).execute();
        SQLStatement.LEGACY_ALTER_TABLE_ON.constructPPreparedStatement().construct(conn).execute();
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
        SQLStatement.FOREIGN_KEYS_ON.constructPPreparedStatement().construct(conn).execute();
        SQLStatement.LEGACY_ALTER_TABLE_OFF.constructPPreparedStatement().construct(conn).execute();
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
                PLogger.get().logException(e);
                return;
            }
        enabled = true;

        // Table creation
        try (final Connection conn = getConnection())
        {
            // Check if the doors table already exists. If it does, assume the rest exists
            // as well and don't set it up.
            if (!conn.getMetaData().getTables(null, null, "doors", new String[]{"TABLE"}).next())
            {
                final Statement stmt1 = conn.createStatement();
                final String sql1 = "CREATE TABLE IF NOT EXISTS doors \n" +
                    "(id              INTEGER    PRIMARY KEY autoincrement, \n" +
                    " name            TEXT       NOT NULL, \n" +
                    " world           TEXT       NOT NULL, \n" +
                    " xMin            INTEGER    NOT NULL, \n" +
                    " yMin            INTEGER    NOT NULL, \n" +
                    " zMin            INTEGER    NOT NULL, \n" +
                    " xMax            INTEGER    NOT NULL, \n" +
                    " yMax            INTEGER    NOT NULL, \n" +
                    " zMax            INTEGER    NOT NULL, \n" +
                    " engineX         INTEGER    NOT NULL, \n" +
                    " engineY         INTEGER    NOT NULL, \n" +
                    " engineZ         INTEGER    NOT NULL, \n" +
                    " bitflag         INTEGER    NOT NULL DEFAULT 0, \n" +
                    " type            INTEGER    NOT NULL DEFAULT  0, \n" +
                    " powerBlockX     INTEGER    NOT NULL DEFAULT -1, \n" +
                    " powerBlockY     INTEGER    NOT NULL DEFAULT -1, \n" +
                    " powerBlockZ     INTEGER    NOT NULL DEFAULT -1, \n" +
                    " openDirection   INTEGER    NOT NULL DEFAULT  0, \n" +
                    " autoClose       INTEGER    NOT NULL DEFAULT -1, \n" +
                    " chunkHash       INTEGER    NOT NULL DEFAULT -1, \n" +
                    " engineChunkHash INTEGER    NOT NULL DEFAULT -1, \n" +
                    " blocksToMove    INTEGER    NOT NULL DEFAULT -1);";
                stmt1.executeUpdate(sql1);
                stmt1.close();

                final Statement stmt2 = conn.createStatement();
                final String sql2 = "CREATE TABLE IF NOT EXISTS players \n" +
                    "(id          INTEGER    PRIMARY KEY AUTOINCREMENT, \n" +
                    " playerUUID  TEXT       NOT NULL, \n" +
                    " playerName  TEXT       NOT NULL, \n" +
                    " unique(playerUUID));";
                stmt2.executeUpdate(sql2);
                stmt2.close();

                final Statement stmt3 = conn.createStatement();
                final String sql3 = "CREATE TABLE IF NOT EXISTS sqlUnion \n" +
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
        return executeQuery(SQLStatement.GET_PLAYER_PERMISSION_OF_DOOR.constructPPreparedStatement()
                                                                      .setString(1, playerUUID)
                                                                      .setLong(2, doorUID),
                            (resultSet) -> resultSet.next() ? resultSet.getInt("permission") : -1, -1);
    }

    /**
     * Construct a new Door from a resultset.
     *
     * @param rs The resultset that contains the data of the animated object.
     * @return An Door if one could be found.
     */
    @NotNull
    private Optional<AbstractDoorBase> newDoorFromRS(final @NotNull ResultSet rs)
        throws SQLException
    {
        return newDoorFromRS(rs, new DoorOwner(rs.getLong("id"), UUID.fromString(rs.getString("playerUUID")),
                                               rs.getString("playerName"), rs.getInt("permission")));
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
                final IPWorld world = BigDoors.get().getPlatform().getPWorldFactory()
                                              .create(UUID.fromString(rs.getString("world")));
                if (!world.exists())
                {
                    PLogger.get().logException(new NullPointerException(
                        "Failed to obtain the world of door \"" + doorOwner.getDoorUID() + "\"."));
                    return Optional.empty();
                }

                final Vector3Di min = new Vector3Di(rs.getInt("xMin"), rs.getInt("yMin"), rs.getInt("zMin"));
                final Vector3Di max = new Vector3Di(rs.getInt("xMax"), rs.getInt("yMax"), rs.getInt("zMax"));
                final Vector3Di engine = new Vector3Di(rs.getInt("engineX"), rs.getInt("engineY"),
                                                       rs.getInt("engineZ"));
                final Vector3Di powerBlock = new Vector3Di(rs.getInt("powerBlockX"), rs.getInt("powerBlockY"),
                                                           rs.getInt("powerBlockZ"));
                final boolean isOpen = BitFlag.hasFlag(DoorFlag.getFlagValue(DoorFlag.ISOPEN), rs.getInt("bitflag"));

                final RotateDirection openDir = RotateDirection.valueOf(rs.getInt("openDirection"));
                if (openDir == null)
                {
                    PLogger.get().logException(
                        new NullPointerException("OpenDirection is null for door \"" + doorOwner.getDoorUID() + "\""));
                    return Optional.empty();
                }

                doorData = new AbstractDoorBase.DoorData(min, max, engine, powerBlock, world, isOpen, openDir);
            }
            final AbstractDoorBase door = DoorType.valueOf(rs.getInt("type"))
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
            pLogger.logException(e);
        }
        return Optional.ofNullable(ret);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean removeDoor(final long doorUID)
    {
        return executeUpdate(SQLStatement.DELETE_DOOR.constructPPreparedStatement()
                                                     .setLong(1, doorUID)) > 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean removeDoors(final @NotNull String playerUUID, final @NotNull String doorName)
    {
        return executeUpdate(SQLStatement.DELETE_NAMED_DOOR_OF_PLAYER.constructPPreparedStatement()
                                                                     .setString(1, playerUUID)
                                                                     .setString(2, doorName)) > 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isBigDoorsWorld(@NotNull UUID worldUUID)
    {
        return executeQuery(SQLStatement.IS_BIGDOORS_WORLD.constructPPreparedStatement()
                                                          .setString(1, worldUUID.toString()),
                            ResultSet::next, false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getDoorCountForPlayer(final @NotNull UUID playerUUID)
    {
        return executeQuery(SQLStatement.GET_DOOR_COUNT_FOR_PLAYER.constructPPreparedStatement()
                                                                  .setString(1, playerUUID.toString()),
                            (resultSet) -> resultSet.next() ? resultSet.getInt("total") : -1, -1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getDoorCountForPlayer(final @NotNull UUID playerUUID, final @NotNull String doorName)
    {
        return executeQuery(SQLStatement.GET_PLAYER_DOOR_COUNT.constructPPreparedStatement()
                                                              .setString(1, playerUUID.toString())
                                                              .setString(2, doorName),
                            (resultSet) -> resultSet.next() ? resultSet.getInt("total") : -1, -1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getDoorCountByName(final @NotNull String doorName)
    {
        return executeQuery(SQLStatement.GET_DOOR_COUNT_BY_NAME.constructPPreparedStatement()
                                                               .setString(1, doorName),
                            (resultSet) -> resultSet.next() ? resultSet.getInt("total") : -1, -1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getOwnerCountOfDoor(final long doorUID)
    {
        return executeQuery(SQLStatement.GET_OWNER_COUNT_OF_DOOR.constructPPreparedStatement()
                                                                .setLong(1, doorUID),
                            (resultSet) -> resultSet.next() ? resultSet.getInt("total") : -1, -1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public Optional<AbstractDoorBase> getDoor(final @NotNull UUID playerUUID, final long doorUID)
    {
        return executeQuery(SQLStatement.GET_DOOR_SPECIFIC_PLAYER.constructPPreparedStatement()
                                                                 .setString(1, playerUUID.toString())
                                                                 .setLong(2, doorUID),
                            (rs) -> rs.next() ? newDoorFromRS(rs) : Optional.empty(),
                            Optional.empty());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public Optional<AbstractDoorBase> getDoor(final long doorUID)
    {
        Optional<AbstractDoorBase> door = Optional.empty();

        try (final Connection conn = getConnection();
             final PreparedStatement ps = SQLStatement.GET_DOOR_FROM_UID.constructPPreparedStatement()
                                                                        .setLong(1, doorUID).construct(conn);
             final ResultSet rs = ps.executeQuery())
        {
            final Optional<DoorOwner> doorOwner = getOwnerOfDoor(conn, doorUID);
            if (!doorOwner.isPresent())
                return door;

            if (rs.next())
                door = newDoorFromRS(rs, doorOwner.get());
        }
        catch (Exception e)
        {
            pLogger.logException(e);
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
        return executeQuery(SQLStatement.GET_DOORS_WITH_NAME.constructPPreparedStatement()
                                                            .setString(1, name),
                            this::parseDoors, Optional.empty());
    }

    /**
     * Constructs a list of doors from a {@link ResultSet}.
     *
     * @param rs The {@link ResultSet}.
     * @return A list of doors that can be parsed from the {@link ResultSet} if any could be found. Otherwise, an empty
     * Optional is returned.
     *
     * @throws SQLException When something went wrong processing the {@link ResultSet}.
     */
    @NotNull
    private Optional<List<AbstractDoorBase>> parseDoors(final @NotNull ResultSet rs)
        throws SQLException
    {
        final List<AbstractDoorBase> doors = new ArrayList<>();
        while (rs.next())
        {
            final DoorOwner doorOwner = new DoorOwner(rs.getLong("id"),
                                                      UUID.fromString(
                                                          rs.getString("playerUUID")),
                                                      rs.getString("playerName"),
                                                      rs.getInt("permission"));
            newDoorFromRS(rs, doorOwner).ifPresent(doors::add);
        }
        return Optional.ofNullable(doors.isEmpty() ? null : doors);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public Optional<List<AbstractDoorBase>> getDoors(final @NotNull String playerUUID,
                                                     final @NotNull String doorName,
                                                     final int maxPermission)
    {
        return executeQuery(SQLStatement.GET_NAMED_DOORS_OWNED_BY_PLAYER.constructPPreparedStatement()
                                                                        .setString(1, playerUUID)
                                                                        .setString(2, doorName)
                                                                        .setInt(3, maxPermission),
                            this::parseDoors, Optional.empty());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public Optional<List<AbstractDoorBase>> getDoors(final @NotNull String playerUUID, int maxPermission)
    {
        return executeQuery(SQLStatement.GET_DOORS_OWNED_BY_PLAYER_WITH_LEVEL.constructPPreparedStatement()
                                                                             .setString(1, playerUUID)
                                                                             .setInt(2, maxPermission),
                            this::parseDoors, Optional.empty());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean updatePlayerName(final @NotNull String playerUUID, final @NotNull String playerName)
    {
        return executeUpdate(SQLStatement.UPDATE_PLAYER_NAME.constructPPreparedStatement()
                                                            .setString(1, playerName)
                                                            .setString(2, playerUUID)) > 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public Optional<UUID> getPlayerUUID(final @NotNull String playerName)
    {
        return executeQuery(SQLStatement.GET_PLAYER_UUID.constructPPreparedStatement()
                                                        .setString(1, playerName),
                            (rs) ->
                            {
                                UUID playerUUID = null;
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
                                return Optional.ofNullable(playerUUID);
                            }, Optional.empty());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public Optional<String> getPlayerName(final @NotNull String playerUUID)
    {
        return executeQuery(SQLStatement.GET_PLAYER_NAME.constructPPreparedStatement()
                                                        .setString(1, playerUUID),
                            (rs) -> Optional.ofNullable(rs.next() ? rs.getString("playerName") : null),
                            Optional.empty());
    }

    /**
     * Gets the original creator of a door using an established database connection.
     *
     * @param doorUID The door whose owner to get.
     * @return The original creator of a door.
     */
    @NotNull
    // TODO: Delete this. I want to do all this in a single statement.
    private Optional<DoorOwner> getOwnerOfDoor(final @NotNull Connection conn, final long doorUID)
        throws SQLException
    {
        DoorOwner doorOwner = null;
        final String sql = "SELECT playerUUID, playerName \n" +
            "FROM players, \n" +
            "     (SELECT U.playerID, U.permission, doors.name \n" +
            "      FROM sqlUnion as U INNER JOIN doors ON doors.id = U.doorUID \n" +
            "      WHERE U.permission = '0' AND doors.id = ?) AS R \n" +
            "WHERE id = R.playerID;";
        final PreparedStatement ps = conn.prepareStatement(sql);
        ps.setLong(1, doorUID);
        final ResultSet rs = ps.executeQuery();
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
            pLogger.logException(e);
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
        return executeQuery(SQLStatement.GET_POWER_BLOCK_DATA.constructPPreparedStatement()
                                                             .setLong(1, chunkHash),
                            (rs) ->
                            {
                                final ConcurrentHashMap<Integer, List<Long>> doors = new ConcurrentHashMap<>();
                                while (rs.next())
                                {
                                    int locationHash = Util.simpleChunkSpaceLocationhash(rs.getInt("powerBlockX"),
                                                                                         rs.getInt("powerBlockY"),
                                                                                         rs.getInt("powerBlockZ"));
                                    if (!doors.containsKey(locationHash))
                                        doors.put(locationHash, new ArrayList<>());
                                    doors.get(locationHash).add(rs.getLong("id"));
                                }
                                return doors;
                            }, new ConcurrentHashMap<>());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public List<Long> getDoorsInChunk(final long chunkHash)
    {
        return executeQuery(SQLStatement.GET_DOORS_IN_CHUNK.constructPPreparedStatement()
                                                           .setLong(1, chunkHash),
                            (rs) ->
                            {
                                final List<Long> doors = new ArrayList<>();
                                while (rs.next())
                                    doors.add(rs.getLong("id"));
                                return doors;
                            }, new ArrayList<>());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean updateDoorBlocksToMove(final long doorUID, final int blocksToMove)
    {
        return executeUpdate(SQLStatement.UPDATE_BLOCKS_TO_MOVE.constructPPreparedStatement()
                                                               .setInt(1, blocksToMove)
                                                               .setLong(2, doorUID)) > 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean updateDoorCoords(final long doorUID, final boolean isOpen, final int xMin, final int yMin,
                                    final int zMin, final int xMax, final int yMax, final int zMax)
    {
        boolean result = executeUpdate(SQLStatement.UPDATE_DOOR_COORDS.constructPPreparedStatement()
                                                                      .setInt(1, xMin)
                                                                      .setInt(2, yMin)
                                                                      .setInt(3, zMin)
                                                                      .setInt(4, xMax)
                                                                      .setInt(5, yMax)
                                                                      .setInt(6, zMax)
                                                                      .setLong(7, doorUID)) > 0;
        return result && changeDoorFlag(doorUID, DoorFlag.ISOPEN, isOpen);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean updateDoorAutoClose(final long doorUID, final int autoClose)
    {
        return executeUpdate(SQLStatement.UPDATE_DOOR_AUTO_CLOSE.constructPPreparedStatement()
                                                                .setInt(1, autoClose)
                                                                .setLong(2, doorUID)) > 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean updateDoorOpenDirection(final long doorUID, final @NotNull RotateDirection openDir)
    {
        return executeUpdate(SQLStatement.UPDATE_DOOR_OPEN_DIR.constructPPreparedStatement()
                                                              .setInt(1, RotateDirection.getValue(openDir))
                                                              .setLong(2, doorUID)) > 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean updateDoorPowerBlockLoc(final long doorUID, final int xPos, final int yPos, final int zPos)
    {
        return executeUpdate(SQLStatement.UPDATE_DOOR_POWER_BLOCK_LOC.constructPPreparedStatement()
                                                                     .setInt(1, xPos)
                                                                     .setInt(2, yPos)
                                                                     .setInt(3, zPos)
                                                                     .setLong(4, Util.simpleChunkHashFromLocation(xPos,
                                                                                                                  zPos))
                                                                     .setLong(5, doorUID)) > 0;
    }

    /**
     * Changes the flag status of a door.
     *
     * @param doorUID    The UID fo the door.
     * @param flag       The {@link DoorFlag} to change.
     * @param flagStatus Whether to enable or disable the {@link DoorFlag}.
     */
    private boolean changeDoorFlag(final long doorUID, final @NotNull DoorFlag flag, final boolean flagStatus)
    {
        @Nullable final Integer currentFlag = executeQuery(SQLStatement.GET_DOOR_FLAG.constructPPreparedStatement()
                                                                                     .setLong(1, doorUID),
                                                           (rs) -> rs.getInt(1));
        if (currentFlag == null)
        {
            PLogger.get().logException(new SQLException("Could not get flag value of door: " + doorUID));
            return false;
        }

        final int newFlag = BitFlag.changeFlag(DoorFlag.getFlagValue(flag), flagStatus, currentFlag);
        return executeUpdate(SQLStatement.UPDATE_DOOR_FLAG.constructPPreparedStatement()
                                                          .setInt(1, newFlag)
                                                          .setLong(2, doorUID)) > 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean setLock(final long doorUID, final boolean newLockStatus)
    {
        return changeDoorFlag(doorUID, DoorFlag.ISLOCKED, newLockStatus);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean insert(final @NotNull AbstractDoorBase door)
    {
        final long playerID = getPlayerID(door.getDoorOwner());
        if (playerID == -1)
            return false;

        int idx = 1;
        int doorUID =
            executeUpdateReturnGeneratedKeys(SQLStatement
                                                 .INSERT_DOOR.constructPPreparedStatement()
                                                             .setString(idx++, door.getName())
                                                             .setString(idx++, door.getWorld().getUID().toString())
                                                             .setInt(idx++, door.getMinimum().getX())
                                                             .setInt(idx++, door.getMinimum().getY())
                                                             .setInt(idx++, door.getMinimum().getZ())
                                                             .setInt(idx++, door.getMaximum().getX())
                                                             .setInt(idx++, door.getMaximum().getY())
                                                             .setInt(idx++, door.getMaximum().getZ())
                                                             .setInt(idx++, door.getEngine().getX())
                                                             .setInt(idx++, door.getEngine().getY())
                                                             .setInt(idx++, door.getEngine().getZ())
                                                             .setInt(idx++, getFlag(door))
                                                             .setInt(idx++, DoorType.getValue(door.getType()))
                                                             .setInt(idx++, door.getPowerBlockLoc().getX())
                                                             .setInt(idx++, door.getPowerBlockLoc().getY())
                                                             .setInt(idx++, door.getPowerBlockLoc().getZ())
                                                             .setInt(idx++, RotateDirection.getValue(door.getOpenDir()))
                                                             .setInt(idx++, door.getAutoClose())
                                                             .setLong(idx++, door.getSimplePowerBlockChunkHash())
                                                             .setInt(idx++, door.getBlocksToMove()));

        if (doorUID == -1)
        {
            PLogger.get().logException(new SQLException("Could not find newly added door!"));
            return false;
        }

        final boolean success = executeUpdate(SQLStatement.INSERT_SQL_UNION.constructPPreparedStatement()
                                                                           .setInt(1, door.getPermission())
                                                                           .setLong(2, playerID)
                                                                           .setLong(3, doorUID)) > 0;
        if (!success)
        {
            PLogger.get().logException(new SQLException("Failed to add DoorOwner!"));
            removeDoor(doorUID);
        }
        return success;
    }

    /**
     * Executes an update defined by a {@link PPreparedStatement}.
     *
     * @param pPreparedStatement The {@link PPreparedStatement}.
     * @return Either the number of rows modified by the update, or -1 if an error occurred.
     */
    private int executeUpdate(final @NotNull PPreparedStatement pPreparedStatement)
    {
        try (final Connection conn = getConnection())
        {
            return executeUpdate(conn, pPreparedStatement);
        }
        catch (SQLException e)
        {
            PLogger.get().logException(e);
        }
        return -1;
    }

    /**
     * Executes an update defined by a {@link PPreparedStatement}.
     *
     * @param conn               A connection to the database.
     * @param pPreparedStatement The {@link PPreparedStatement}.
     * @return Either the number of rows modified by the update, or -1 if an error occurred.
     */
    private int executeUpdate(final @NotNull Connection conn, final @NotNull PPreparedStatement pPreparedStatement)
    {
        try (final PreparedStatement ps = pPreparedStatement.construct(conn))
        {
            return ps.executeUpdate();
        }
        catch (SQLException e)
        {
            PLogger.get().logException(e);
        }
        return -1;
    }

    /**
     * Executes an update defined by a {@link PPreparedStatement} and returns the generated key index. See {@link
     * Statement#RETURN_GENERATED_KEYS}.
     *
     * @param pPreparedStatement The {@link PPreparedStatement}.
     * @return The generated key index if possible, otherwise -1.
     */
    private int executeUpdateReturnGeneratedKeys(final @NotNull PPreparedStatement pPreparedStatement)
    {
        try (final Connection conn = getConnection())
        {
            return executeUpdateReturnGeneratedKeys(conn, pPreparedStatement);
        }
        catch (SQLException e)
        {
            PLogger.get().logException(e);
        }
        return -1;
    }

    /**
     * Executes an update defined by a {@link PPreparedStatement} and returns the generated key index. See {@link
     * Statement#RETURN_GENERATED_KEYS}.
     *
     * @param conn               A connection to the database.
     * @param pPreparedStatement The {@link PPreparedStatement}.
     * @return The generated key index if possible, otherwise -1.
     */
    private int executeUpdateReturnGeneratedKeys(final @NotNull Connection conn,
                                                 final @NotNull PPreparedStatement pPreparedStatement)
    {
        try (final PreparedStatement ps = pPreparedStatement.construct(conn, Statement.RETURN_GENERATED_KEYS))
        {
            ps.executeUpdate();
            try (final ResultSet rs = ps.getGeneratedKeys())
            {
                return rs.getInt(1);
            }
            catch (SQLException ex)
            {
                PLogger.get().logException(ex);
            }
        }
        catch (SQLException e)
        {
            PLogger.get().logException(e);
        }
        return -1;
    }

    /**
     * Executes a query defined by a {@link PPreparedStatement}.
     *
     * @param pPreparedStatement The {@link PPreparedStatement}.
     * @return The {@link ResultSet} of the query, or null in case an error occurred.
     */
    @Nullable
    private <T> T executeQuery(final @NotNull PPreparedStatement pPreparedStatement,
                               final @NotNull CheckedFunction<ResultSet, T, SQLException> fun)
    {
        return executeQuery(pPreparedStatement, fun, null);
    }

    /**
     * Executes a query defined by a {@link PPreparedStatement} and applies a function to the result.
     *
     * @param pPreparedStatement The {@link PPreparedStatement}.
     * @param fun                The function to apply to the {@link ResultSet}.
     * @param fallback           The value to return in case the result is null or if an error occurred.
     * @param <T>                The type of the result to return.
     * @return The {@link ResultSet} of the query, or null in case an error occurred.
     */
    @Contract(" _, _, !null -> !null")
    private <T> T executeQuery(final @NotNull PPreparedStatement pPreparedStatement,
                               final @NotNull CheckedFunction<ResultSet, T, SQLException> fun,
                               final @Nullable T fallback)
    {
        try (final Connection conn = getConnection())
        {
            return executeQuery(conn, pPreparedStatement, fun, fallback);
        }
        catch (Exception e)
        {
            PLogger.get().logException(e);
        }
        return null;
    }

    /**
     * Executes a query defined by a {@link PPreparedStatement} and applies a function to the result.
     *
     * @param conn               A connection to the database.
     * @param pPreparedStatement The {@link PPreparedStatement}.
     * @param fun                The function to apply to the {@link ResultSet}.
     * @param fallback           The value to return in case the result is null or if an error occurred.
     * @param <T>                The type of the result to return.
     * @return The {@link ResultSet} of the query, or null in case an error occurred.
     */
    @Contract(" _, _, _, !null -> !null")
    private <T> T executeQuery(final @NotNull Connection conn,
                               final @NotNull PPreparedStatement pPreparedStatement,
                               final @NotNull CheckedFunction<ResultSet, T, SQLException> fun,
                               final @Nullable T fallback)
    {
        try (final PreparedStatement ps = pPreparedStatement.construct(conn);
             final ResultSet rs = ps.executeQuery())
        {
            final T result = fun.apply(rs);
            return result == null ? fallback : result;
        }
        catch (SQLException e)
        {
            PLogger.get().logException(e);
        }
        return fallback;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean removeOwner(final long doorUID, final @NotNull String playerUUID)
    {
        return executeUpdate(SQLStatement.REMOVE_DOOR_OWNER.constructPPreparedStatement()
                                                           .setString(1, playerUUID)
                                                           .setLong(2, doorUID)) > 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public List<DoorOwner> getOwnersOfDoor(final long doorUID)
    {
        return executeQuery(SQLStatement.GET_DOOR_OWNERS.constructPPreparedStatement()
                                                        .setLong(1, doorUID),
                            (resultSet) ->
                            {
                                final List<DoorOwner> ret = new ArrayList<>();
                                while (resultSet.next())
                                    ret.add(new DoorOwner(resultSet.getLong("doorUID"),
                                                          UUID.fromString(resultSet.getString("playerUUID")),
                                                          resultSet.getString("playerName"),
                                                          resultSet.getInt("permission")));
                                return ret;
                            },
                            new ArrayList<>());
    }

    /**
     * Gets the ID player in the "players" table. If the player isn't in the database yet, they are added first.
     *
     * @param conn      The connection to the database.
     * @param doorOwner The doorOwner with the player to retrieve.
     * @return The database ID of the player.
     */
    private long getPlayerID(final @NotNull Connection conn, final @NotNull DoorOwner doorOwner)
    {
        executeUpdate(conn, SQLStatement.GET_OR_CREATE_PLAYER.constructPPreparedStatement()
                                                             .setString(1, doorOwner.getPlayerUUID().toString())
                                                             .setString(2, doorOwner.getPlayerName()));

        return executeQuery(conn, SQLStatement.GET_PLAYER_ID.constructPPreparedStatement()
                                                            .setString(1, doorOwner.getPlayerUUID().toString()),
                            (rs) -> rs.next() ? rs.getLong("id") : -1, -1L);
    }

    /**
     * Gets the ID player in the "players" table. If the player isn't in the database yet, they are added first.
     *
     * @param doorOwner The doorOwner with the player to retrieve.
     * @return The database ID of the player or -1 if it failed.
     */
    private long getPlayerID(final @NotNull DoorOwner doorOwner)
    {
        try (final Connection conn = getConnection())
        {
            return getPlayerID(conn, doorOwner);
        }
        catch (Exception e)
        {
            PLogger.get().logException(e);
        }
        return -1;
    }

    /**
     * Adds an owner or updates an existing owner's permission level.
     *
     * @param conn       A connection to the database.
     * @param rs         The {@link ResultSet} containing the current door owner (or not).
     * @param doorUID    The UID of the door.
     * @param playerID   The ID of the player.
     * @param permission The new level of ownership this player has over the door.
     * @return True if a change was made.
     *
     * @throws SQLException
     */
    private boolean addOrUpdateOwner(final @NotNull Connection conn, final @NotNull ResultSet rs, final long doorUID,
                                     final long playerID, final int permission)
        throws SQLException
    {
        if (rs.next() && (rs.getInt("permission") != permission))
            return executeUpdate(conn, SQLStatement.UPDATE_DOOR_OWNER_PERMISSION.constructPPreparedStatement()
                                                                                .setInt(1, permission)
                                                                                .setLong(2, playerID)
                                                                                .setLong(3, doorUID)) > 0;

        return executeUpdate(conn, SQLStatement.INSERT_SQL_UNION.constructPPreparedStatement()
                                                                .setInt(1, permission)
                                                                .setLong(2, playerID)
                                                                .setLong(3, doorUID)) > 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean addOwner(final long doorUID, final @NotNull IPPlayer player, final int permission)
    {
        // permission level 0 is reserved for the creator, and negative values are not allowed.
        if (permission < 1)
            return false;

        try (final Connection conn = getConnection())
        {
            final String playerName = player.getName();
            final long playerID = getPlayerID(conn, new DoorOwner(doorUID, player.getUUID(), playerName, permission));
            if (playerID == -1)
            {
                pLogger.logException(new IllegalArgumentException(
                    "Trying to add player \"" + player.getUUID().toString() + "\" as owner of door " + doorUID +
                        ", but that player is not registered in the database! Aborting..."));
                return false;
            }

            return executeQuery(conn, SQLStatement.GET_UNION_FROM_OWNER.constructPPreparedStatement()
                                                                       .setLong(1, playerID)
                                                                       .setLong(2, doorUID),
                                (rs) -> addOrUpdateOwner(conn, rs, doorUID, playerID, permission), false);
        }
        catch (SQLException | NullPointerException e)
        {
            pLogger.logException(e);
        }
        return false;
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
            final Statement stmt = conn.createStatement();
            final ResultSet rs = stmt.executeQuery("PRAGMA user_version;");
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
            pLogger.logException(e);
        }
    }

    /**
     * Makes a backup of the database file. Stored in a database with the same name, but with ".BACKUP" appended to it.
     *
     * @return True if backup creation was successful.
     */
    private boolean makeBackup()
    {
        final File dbFileBackup = new File(dbFile.toString() + ".BACKUP");
        // Only the most recent backup is kept, so delete the old one if a new one needs
        // to be created.
        if (dbFileBackup.exists() && !dbFileBackup.delete())
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
            pLogger.logException(e);
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
        try (final PreparedStatement ps1 = conn.prepareStatement("SELECT * FROM doors;");
             final ResultSet rs1 = ps1.executeQuery())
        {
            pLogger.warn("Upgrading database to V11!");

            while (rs1.next())
            {
                final long UID = rs1.getLong("id");
                final int x = rs1.getInt("powerBlockX");
                final int z = rs1.getInt("powerBlockZ");

                executeUpdate(conn, new PPreparedStatement(2, "UPDATE doors SET chunkHash=? WHERE id=?;")
                    .setLong(1, Util.simpleChunkHashFromLocation(x, z))
                    .setLong(2, UID));
            }
        }
        catch (SQLException | NullPointerException e)
        {
            pLogger.logException(e);
        }
    }
}
