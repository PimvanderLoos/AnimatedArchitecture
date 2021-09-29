package nl.pim16aap2.bigdoors.storage.sqlite;

import lombok.Getter;
import lombok.extern.flogger.Flogger;
import nl.pim16aap2.bigdoors.api.IPWorld;
import nl.pim16aap2.bigdoors.api.PPlayerData;
import nl.pim16aap2.bigdoors.api.factories.IPWorldFactory;
import nl.pim16aap2.bigdoors.doors.AbstractDoor;
import nl.pim16aap2.bigdoors.doors.DoorBase;
import nl.pim16aap2.bigdoors.doors.DoorBaseBuilder;
import nl.pim16aap2.bigdoors.doors.DoorSerializer;
import nl.pim16aap2.bigdoors.doortypes.DoorType;
import nl.pim16aap2.bigdoors.managers.DoorRegistry;
import nl.pim16aap2.bigdoors.managers.DoorTypeManager;
import nl.pim16aap2.bigdoors.storage.IStorage;
import nl.pim16aap2.bigdoors.storage.PPreparedStatement;
import nl.pim16aap2.bigdoors.storage.SQLStatement;
import nl.pim16aap2.bigdoors.util.Cuboid;
import nl.pim16aap2.bigdoors.util.DoorOwner;
import nl.pim16aap2.bigdoors.util.IBitFlag;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import nl.pim16aap2.bigdoors.util.Util;
import nl.pim16aap2.bigdoors.util.functional.CheckedFunction;
import nl.pim16aap2.bigdoors.util.vector.Vector3Di;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;
import org.sqlite.SQLiteConfig;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * An implementation of {@link IStorage} for SQLite.
 *
 * @author Pim
 */
@Singleton
@Flogger
public final class SQLiteJDBCDriverConnection implements IStorage
{
    private static final String DRIVER = "org.sqlite.JDBC";
    private static final int DATABASE_VERSION = 11;
    private static final int MIN_DATABASE_VERSION = 10;

    @Getter
    private final SQLiteConfig configRW;
    @Getter
    private final SQLiteConfig configRO;

    /**
     * A fake UUID that cannot exist normally. To be used for storing transient data across server restarts.
     */
    @SuppressWarnings("unused")
    private static final String FAKE_UUID = "0000";

    /**
     * The database file.
     */
    private final Path dbFile;

    /**
     * The URL of the database.
     */
    private final String url;

    /**
     * The {@link DatabaseState} the database is in.
     */
    // 'volatile' is not a substitution for thread-safety. However, that is not the goal here.
    // The state is modified on a single thread (this class is not thread safe!), but may be read
    // from several other threads from the getter. 'volatile' here ensures those reads are up-to-date.
    @SuppressWarnings("squid:S3077")
    @Getter
    private volatile DatabaseState databaseState = DatabaseState.UNINITIALIZED;

    private final DoorBaseBuilder doorBaseBuilder;

    private final DoorRegistry doorRegistry;

    private final DoorTypeManager doorTypeManager;

    private final IPWorldFactory worldFactory;

    /**
     * Constructor of the SQLite driver connection.
     *
     * @param dbFile
     *     The file to store the database in.
     */
    @Inject
    public SQLiteJDBCDriverConnection(@Named("databaseFile") Path dbFile, DoorBaseBuilder doorBaseBuilder,
                                      DoorRegistry doorRegistry, DoorTypeManager doorTypeManager,
                                      IPWorldFactory worldFactory)
    {
        this.dbFile = dbFile;
        this.doorBaseBuilder = doorBaseBuilder;
        this.doorRegistry = doorRegistry;
        this.doorTypeManager = doorTypeManager;
        this.worldFactory = worldFactory;

        configRW = new SQLiteConfig();
        configRW.enforceForeignKeys(true);

        configRO = new SQLiteConfig(configRW.toProperties());
        configRO.setReadOnly(true);

        url = "jdbc:sqlite:" + dbFile;
        if (!loadDriver())
        {
            databaseState = DatabaseState.NO_DRIVER;
            return;
        }
        init();
        if (databaseState == DatabaseState.OUT_OF_DATE)
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

    @Override
    public boolean isSingleThreaded()
    {
        return true;
    }

    /**
     * Establishes a connection with the database.
     *
     * @param state
     *     The state from which the connection was requested.
     * @param readMode
     *     The {@link ReadMode} to use for the database.
     * @return A database connection.
     */
    private @Nullable Connection getConnection(DatabaseState state, ReadMode readMode)
    {
        if (!databaseState.equals(state))
        {
            log.at(Level.SEVERE).withCause(new IllegalStateException(
                "The database is in an incorrect state: " + databaseState.name() +
                    ". All database operations are disabled!")).log();
            return null;
        }

        @Nullable Connection conn = null;
        try
        {
            final SQLiteConfig config = readMode == ReadMode.READ_ONLY ? configRO : configRW;
            conn = config.createConnection(url);
        }
        catch (SQLException e)
        {
            log.at(Level.SEVERE).withCause(e).log("Failed to open connection!");
        }
        if (conn == null)
            log.at(Level.SEVERE).withCause(new NullPointerException("Could not open connection!")).log();
        return conn;
    }

    /**
     * Establishes a connection with the database, assuming a database state of {@link DatabaseState#OK}.
     *
     * @param readMode
     *     The {@link ReadMode} to use for the database.
     * @return A database connection.
     */
    private @Nullable Connection getConnection(ReadMode readMode)
    {
        return getConnection(DatabaseState.OK, readMode);
    }

    /**
     * Because SQLite is a PoS and decided to remove the admittedly odd behavior that just disabling foreign keys
     * suddenly ignored all the triggers etc. attached to it without actually providing a proper alternative (perhaps
     * implement ALTER TABLE properly??), this method needs to be called now in order to safely modify stuff without
     * having the foreign keys getting fucked up.
     *
     * @param conn
     *     The connection.
     */
    @SuppressWarnings("unused")
    private void disableForeignKeys(Connection conn)
        throws Exception
    {
        SQLStatement.FOREIGN_KEYS_OFF.constructPPreparedStatement().construct(conn).execute();
        SQLStatement.LEGACY_ALTER_TABLE_ON.constructPPreparedStatement().construct(conn).execute();
    }

    /**
     * The anti method of {@link #disableForeignKeys(Connection)}. Only needs to be called if that was called first.
     *
     * @param conn
     *     The connection.
     */
    @SuppressWarnings("unused")
    private void reEnableForeignKeys(Connection conn)
        throws Exception
    {
        SQLStatement.FOREIGN_KEYS_ON.constructPPreparedStatement().construct(conn).execute();
        SQLStatement.LEGACY_ALTER_TABLE_OFF.constructPPreparedStatement().construct(conn).execute();
    }

    /**
     * Initializes the database. I.e. create all the required files/tables.
     */
    private void init()
    {
        try
        {
            if (!Files.isRegularFile(dbFile))
            {
                if (!Files.isDirectory(dbFile.getParent()))
                    Files.createDirectories(dbFile.getParent());
                Files.createFile(dbFile);
                log.at(Level.INFO).log("New file created at: %s", dbFile);
            }
        }
        catch (IOException e)
        {
            log.at(Level.SEVERE).withCause(e).log("File write error: %s", dbFile);
            databaseState = DatabaseState.ERROR;
            return;
        }

        // Table creation
        try (@Nullable Connection conn = getConnection(DatabaseState.UNINITIALIZED, ReadMode.READ_WRITE))
        {
            if (conn == null)
            {
                databaseState = DatabaseState.ERROR;
                return;
            }

            // Check if the doors table already exists. If it does, assume the rest exists
            // as well and don't set it up.
            if (conn.getMetaData().getTables(null, null, "DoorBase", new String[]{"TABLE"}).next())
                databaseState = DatabaseState.OUT_OF_DATE; // Assume it's outdated if it isn't newly created.
            else
            {
                executeUpdate(conn, SQLStatement.CREATE_TABLE_PLAYER.constructPPreparedStatement());
                executeUpdate(conn, SQLStatement.RESERVE_IDS_PLAYER.constructPPreparedStatement());

                executeUpdate(conn, SQLStatement.CREATE_TABLE_DOORBASE.constructPPreparedStatement());
                executeUpdate(conn, SQLStatement.RESERVE_IDS_DOORBASE.constructPPreparedStatement());

                executeUpdate(conn, SQLStatement.CREATE_TABLE_DOOROWNER_PLAYER.constructPPreparedStatement());
                executeUpdate(conn, SQLStatement.RESERVE_IDS_DOOROWNER_PLAYER.constructPPreparedStatement());

                updateDBVersion(conn);
                databaseState = DatabaseState.OK;
            }

        }
        catch (SQLException | NullPointerException e)
        {
            log.at(Level.SEVERE).withCause(e).log();
            databaseState = DatabaseState.ERROR;
        }
    }

    private Optional<AbstractDoor> constructDoor(ResultSet doorBaseRS)
        throws Exception
    {
        final Optional<DoorType> doorType = doorTypeManager.getDoorTypeFromFullName(doorBaseRS.getString("doorType"));

        if (!doorType.map(doorTypeManager::isRegistered).orElse(false))
        {
            log.at(Level.SEVERE).withCause(new IllegalStateException("Type with ID: " + doorBaseRS.getInt("doorType") +
                                                                         " has not been registered (yet)!")).log();
            return Optional.empty();
        }

        final long doorUID = doorBaseRS.getLong("id");

        // SonarLint assumes that doorType could be empty (S3655) (appears to miss the mapping operation above),
        // while this actually won't happen.
        // IntelliJ Struggles with <?> and nullability... :(
        @SuppressWarnings({"squid:S3655", "NullableProblems"}) //
        final DoorSerializer<?> serializer = doorType.get().getDoorSerializer();

        final Optional<AbstractDoor> registeredDoor = doorRegistry.getRegisteredDoor(doorUID);
        if (registeredDoor.isPresent())
            return registeredDoor;

        final Optional<RotateDirection> openDirection =
            Optional.ofNullable(RotateDirection.valueOf(doorBaseRS.getInt("openDirection")));

        if (openDirection.isEmpty())
            return Optional.empty();

        final Vector3Di min = new Vector3Di(doorBaseRS.getInt("xMin"),
                                            doorBaseRS.getInt("yMin"),
                                            doorBaseRS.getInt("zMin"));
        final Vector3Di max = new Vector3Di(doorBaseRS.getInt("xMax"),
                                            doorBaseRS.getInt("yMax"),
                                            doorBaseRS.getInt("zMax"));
        final Vector3Di engine = new Vector3Di(doorBaseRS.getInt("engineX"),
                                               doorBaseRS.getInt("engineY"),
                                               doorBaseRS.getInt("engineZ"));
        final Vector3Di powerBlock = new Vector3Di(doorBaseRS.getInt("powerBlockX"),
                                                   doorBaseRS.getInt("powerBlockY"),
                                                   doorBaseRS.getInt("powerBlockZ"));

        final IPWorld world = worldFactory.create(doorBaseRS.getString("world"));

        final long bitflag = doorBaseRS.getLong("bitflag");
        final boolean isOpen = IBitFlag.hasFlag(DoorFlag.getFlagValue(DoorFlag.IS_OPEN), bitflag);
        final boolean isLocked = IBitFlag.hasFlag(DoorFlag.getFlagValue(DoorFlag.IS_LOCKED), bitflag);

        final String name = doorBaseRS.getString("name");

        final PPlayerData playerData = new PPlayerData(UUID.fromString(doorBaseRS.getString("playerUUID")),
                                                       doorBaseRS.getString("playerName"),
                                                       doorBaseRS.getInt("sizeLimit"),
                                                       doorBaseRS.getInt("countLimit"),
                                                       doorBaseRS.getLong("permissions"));

        final DoorOwner primeOwner = new DoorOwner(doorUID,
                                                   doorBaseRS.getInt("permission"),
                                                   playerData);

        final Map<UUID, DoorOwner> doorOwners = getOwnersOfDoor(doorUID);
        final DoorBase doorData = doorBaseBuilder.builder().uid(doorUID).name(name).cuboid(new Cuboid(min, max))
                                                 .engine(engine).powerBlock(powerBlock).world(world).isOpen(isOpen)
                                                 .isLocked(isLocked).openDir(openDirection.get()).primeOwner(primeOwner)
                                                 .doorOwners(doorOwners).build();

        final byte[] rawTypeData = doorBaseRS.getBytes("typeData");
        return Optional.of(serializer.deserialize(doorData, rawTypeData));
    }

    @Override
    public boolean deleteDoorType(DoorType doorType)
    {
        final boolean removed = executeTransaction(
            conn -> executeUpdate(SQLStatement.DELETE_DOOR_TYPE
                                      .constructPPreparedStatement()
                                      .setNextString(doorType.getFullName())) > 0, false);

        if (removed)
            doorTypeManager.unregisterDoorType(doorType);
        return removed;
    }

    private Long insert(Connection conn, AbstractDoor door, String doorType, byte[] typeSpecificData)
    {
        final PPlayerData playerData = door.getPrimeOwner().pPlayerData();
        insertOrIgnorePlayer(conn, playerData);

        final String worldName = door.getWorld().worldName();
        final long engineHash = Util.simpleChunkHashFromLocation(door.getEngine().x(), door.getEngine().z());
        final long powerBlockHash = Util
            .simpleChunkHashFromLocation(door.getPowerBlock().x(), door.getPowerBlock().z());
        executeUpdate(conn, SQLStatement.INSERT_DOOR_BASE.constructPPreparedStatement()
                                                         .setNextString(door.getName())
                                                         .setNextString(worldName)
                                                         .setNextInt(door.getMinimum().x())
                                                         .setNextInt(door.getMinimum().y())
                                                         .setNextInt(door.getMinimum().z())
                                                         .setNextInt(door.getMaximum().x())
                                                         .setNextInt(door.getMaximum().y())
                                                         .setNextInt(door.getMaximum().z())
                                                         .setNextInt(door.getEngine().x())
                                                         .setNextInt(door.getEngine().y())
                                                         .setNextInt(door.getEngine().z())
                                                         .setNextLong(engineHash)
                                                         .setNextInt(door.getPowerBlock().x())
                                                         .setNextInt(door.getPowerBlock().y())
                                                         .setNextInt(door.getPowerBlock().z())
                                                         .setNextLong(powerBlockHash)
                                                         .setNextInt(RotateDirection.getValue(door.getOpenDir()))
                                                         .setNextLong(getFlag(door))
                                                         .setNextString(doorType)
                                                         .setNextBytes(typeSpecificData));

        // TODO: Just use the fact that the last-inserted door has the current UID (that fact is already used by
        //       getTypeSpecificDataInsertStatement(DoorType)), so it can be done in a single statement.
        final long doorUID = executeQuery(conn, SQLStatement.SELECT_MOST_RECENT_DOOR.constructPPreparedStatement(),
                                          rs -> rs.next() ? rs.getLong("seq") : -1, -1L);

        executeUpdate(conn, SQLStatement.INSERT_PRIME_OWNER.constructPPreparedStatement()
                                                           .setString(1, playerData.getUUID().toString()));

        return doorUID;
    }

    @Override
    public Optional<AbstractDoor> insert(AbstractDoor door)
    {
        @SuppressWarnings("NullableProblems") // IntelliJ Struggles with <?> and nullability... :(
        final DoorSerializer<?> serializer = door.getDoorType().getDoorSerializer();

        final String typeName = door.getDoorType().getFullName();
        try
        {
            final byte[] typeData = serializer.serialize(door);

            final long doorUID = executeTransaction(conn -> insert(conn, door, typeName, typeData), -1L);
            if (doorUID > 0)
                return Optional.of(serializer.deserialize(
                    doorBaseBuilder.builder().uid(doorUID).name(door.getName()).cuboid(door.getCuboid())
                                   .engine(door.getEngine()).powerBlock(door.getPowerBlock()).world(door.getWorld())
                                   .isOpen(door.isOpen()).isLocked(door.isLocked()).openDir(door.getOpenDir())
                                   .primeOwner(door.getPrimeOwner()).build(), typeData));
        }
        catch (Exception t)
        {
            log.at(Level.SEVERE).withCause(t).log();
        }
        return Optional.empty();
    }

    @Override
    public boolean syncDoorData(DoorBase doorBase, byte[] typeData)
    {
        return executeUpdate(SQLStatement.UPDATE_DOOR_BASE
                                 .constructPPreparedStatement()
                                 .setNextString(doorBase.getName())
                                 .setNextString(doorBase.getWorld().worldName())

                                 .setNextInt(doorBase.getCuboid().getMin().x())
                                 .setNextInt(doorBase.getCuboid().getMin().y())
                                 .setNextInt(doorBase.getCuboid().getMin().z())

                                 .setNextInt(doorBase.getCuboid().getMax().x())
                                 .setNextInt(doorBase.getCuboid().getMax().y())
                                 .setNextInt(doorBase.getCuboid().getMax().z())

                                 .setNextInt(doorBase.getEngine().x())
                                 .setNextInt(doorBase.getEngine().y())
                                 .setNextInt(doorBase.getEngine().z())
                                 .setNextLong(Util.simpleChunkHashFromLocation(doorBase.getEngine().x(),
                                                                               doorBase.getEngine().z()))

                                 .setNextInt(doorBase.getPowerBlock().x())
                                 .setNextInt(doorBase.getPowerBlock().y())
                                 .setNextInt(doorBase.getPowerBlock().z())
                                 .setNextLong(Util.simpleChunkHashFromLocation(doorBase.getPowerBlock().x(),
                                                                               doorBase.getPowerBlock().z()))

                                 .setNextInt(RotateDirection.getValue(doorBase.getOpenDir()))
                                 .setNextLong(getFlag(doorBase.isOpen(), doorBase.isLocked()))
                                 .setNextBytes(typeData)

                                 .setNextLong(doorBase.getDoorUID())) > 0;
    }

    private void insertOrIgnorePlayer(Connection conn, PPlayerData playerData)
    {
        executeUpdate(conn, SQLStatement.INSERT_OR_IGNORE_PLAYER_DATA.constructPPreparedStatement()
                                                                     .setNextString(playerData.getUUID().toString())
                                                                     .setNextString(playerData.getName())
                                                                     .setNextInt(playerData.getDoorSizeLimit())
                                                                     .setNextInt(playerData.getDoorCountLimit())
                                                                     .setNextLong(playerData.getPermissionsFlag()));
    }

    /**
     * Gets the ID player in the "players" table. If the player isn't in the database yet, they are added first.
     *
     * @param conn
     *     The connection to the database.
     * @param doorOwner
     *     The doorOwner with the player to retrieve.
     * @return The database ID of the player.
     */
    private long getPlayerID(Connection conn, DoorOwner doorOwner)
    {
        insertOrIgnorePlayer(conn, doorOwner.pPlayerData());

        return executeQuery(conn, SQLStatement.GET_PLAYER_ID
                                .constructPPreparedStatement()
                                .setString(1, doorOwner.pPlayerData().getUUID().toString()),
                            rs -> rs.next() ? rs.getLong("id") : -1, -1L);
    }

    /**
     * Attempts to construct a subclass of {@link DoorBase} from a ResultSet containing all data pertaining the {@link
     * DoorBase} (as stored in the "DoorBase" table), as well as the owner (name, UUID, permission) and the
     * typeTableName.
     *
     * @param doorBaseRS
     *     The {@link ResultSet} containing a row from the "DoorBase" table as well as a row from the "DoorOwnerPlayer"
     *     table and "typeTableName" from the "DoorType" table.
     * @return An instance of a subclass of {@link DoorBase} if it could be created.
     */
    private Optional<AbstractDoor> getDoor(ResultSet doorBaseRS)
        throws Exception
    {
        // Make sure the ResultSet isn't empty.
        if (!doorBaseRS.isBeforeFirst())
            return Optional.empty();

        return constructDoor(doorBaseRS);
    }

    /**
     * Attempts to construct a list of subclasses of {@link DoorBase} from a ResultSet containing all data pertaining to
     * one or more {@link DoorBase}s (as stored in the "DoorBase" table), as well as the owner (name, UUID, permission)
     * and the typeTableName.
     *
     * @param doorBaseRS
     *     The {@link ResultSet} containing one or more rows from the "DoorBase" table as well as matching rows from the
     *     "DoorOwnerPlayer" table and "typeTableName" from the "DoorType" table.
     * @return An optional with a list of {@link DoorBase}s if any could be constructed. If none could be constructed,
     * an empty {@link Optional} is returned instead.
     */
    private List<AbstractDoor> getDoors(ResultSet doorBaseRS)
        throws Exception
    {
        // Make sure the ResultSet isn't empty.
        if (!doorBaseRS.isBeforeFirst())
            return Collections.emptyList();

        final List<AbstractDoor> doors = new ArrayList<>();
        while (doorBaseRS.next())
            constructDoor(doorBaseRS).ifPresent(doors::add);
        return doors;
    }

    @Override
    public Optional<AbstractDoor> getDoor(long doorUID)
    {
        return executeQuery(SQLStatement.GET_DOOR_BASE_FROM_ID.constructPPreparedStatement()
                                                              .setLong(1, doorUID),
                            this::getDoor, Optional.empty());
    }

    @Override
    public Optional<AbstractDoor> getDoor(UUID playerUUID, long doorUID)
    {
        return executeQuery(SQLStatement.GET_DOOR_BASE_FROM_ID_FOR_PLAYER.constructPPreparedStatement()
                                                                         .setLong(1, doorUID)
                                                                         .setString(2, playerUUID.toString()),
                            this::getDoor, Optional.empty());
    }

    @Override
    public boolean removeDoor(long doorUID)
    {
        return executeUpdate(SQLStatement.DELETE_DOOR.constructPPreparedStatement()
                                                     .setLong(1, doorUID)) > 0;
    }

    @Override
    public boolean removeDoors(UUID playerUUID, String doorName)
    {
        return executeUpdate(SQLStatement.DELETE_NAMED_DOOR_OF_PLAYER.constructPPreparedStatement()
                                                                     .setString(1, playerUUID.toString())
                                                                     .setString(2, doorName)) > 0;
    }

    @Override
    public boolean isBigDoorsWorld(String worldName)
    {
        return executeQuery(SQLStatement.IS_BIGDOORS_WORLD.constructPPreparedStatement()
                                                          .setString(1, worldName),
                            ResultSet::next, false);
    }

    @Override
    public int getDoorCountForPlayer(UUID playerUUID)
    {
        return executeQuery(SQLStatement.GET_DOOR_COUNT_FOR_PLAYER.constructPPreparedStatement()
                                                                  .setString(1, playerUUID.toString()),
                            resultSet -> resultSet.next() ? resultSet.getInt("total") : -1, -1);
    }

    @Override
    public int getDoorCountForPlayer(UUID playerUUID, String doorName)
    {
        return executeQuery(SQLStatement.GET_PLAYER_DOOR_COUNT.constructPPreparedStatement()
                                                              .setString(1, playerUUID.toString())
                                                              .setString(2, doorName),
                            resultSet -> resultSet.next() ? resultSet.getInt("total") : -1, -1);
    }

    @Override
    public int getDoorCountByName(String doorName)
    {
        return executeQuery(SQLStatement.GET_DOOR_COUNT_BY_NAME.constructPPreparedStatement()
                                                               .setString(1, doorName),
                            resultSet -> resultSet.next() ? resultSet.getInt("total") : -1, -1);
    }

    @Override
    public int getOwnerCountOfDoor(long doorUID)
    {
        return executeQuery(SQLStatement.GET_OWNER_COUNT_OF_DOOR.constructPPreparedStatement()
                                                                .setLong(1, doorUID),
                            resultSet -> resultSet.next() ? resultSet.getInt("total") : -1, -1);
    }

    @Override
    public List<AbstractDoor> getDoors(UUID playerUUID, String doorName, int maxPermission)
    {
        return executeQuery(SQLStatement.GET_NAMED_DOORS_OWNED_BY_PLAYER.constructPPreparedStatement()
                                                                        .setString(1, playerUUID.toString())
                                                                        .setString(2, doorName)
                                                                        .setInt(3, maxPermission),
                            this::getDoors, Collections.emptyList());
    }

    @Override
    public List<AbstractDoor> getDoors(UUID playerUUID, String name)
    {
        return getDoors(playerUUID, name, 0);
    }

    @Override
    public List<AbstractDoor> getDoors(String name)
    {
        return executeQuery(SQLStatement.GET_DOORS_WITH_NAME.constructPPreparedStatement()
                                                            .setString(1, name),
                            this::getDoors, Collections.emptyList());
    }

    @Override
    public List<AbstractDoor> getDoors(UUID playerUUID, int maxPermission)
    {
        return executeQuery(SQLStatement.GET_DOORS_OWNED_BY_PLAYER_WITH_LEVEL.constructPPreparedStatement()
                                                                             .setString(1, playerUUID.toString())
                                                                             .setInt(2, maxPermission),
                            this::getDoors, Collections.emptyList());
    }

    @Override
    public List<AbstractDoor> getDoors(UUID playerUUID)
    {
        return getDoors(playerUUID, 0);
    }

    @Override
    public boolean updatePlayerData(PPlayerData playerData)
    {
        return executeUpdate(SQLStatement.UPDATE_PLAYER_DATA.constructPPreparedStatement()
                                                            .setNextString(playerData.getName())
                                                            .setNextInt(playerData.getDoorSizeLimit())
                                                            .setNextInt(playerData.getDoorCountLimit())
                                                            .setNextLong(playerData.getPermissionsFlag())
                                                            .setNextString(playerData.getUUID().toString())) > 0;
    }

    @Override
    public Optional<PPlayerData> getPlayerData(UUID uuid)
    {
        return executeQuery(SQLStatement.GET_PLAYER_DATA.constructPPreparedStatement()
                                                        .setNextString(uuid.toString()),
                            resultSet ->
                                Optional.of(new PPlayerData(
                                    uuid,
                                    resultSet.getString("playerName"),
                                    resultSet.getInt("sizeLimit"),
                                    resultSet.getInt("countLimit"),
                                    resultSet.getLong("permissions")
                                )), Optional.empty());
    }

    @Override
    public List<PPlayerData> getPlayerData(String playerName)
    {

        return executeQuery(SQLStatement.GET_PLAYER_DATA_FROM_NAME.constructPPreparedStatement()
                                                                  .setNextString(playerName),
                            (resultSet) ->
                            {
                                final List<PPlayerData> playerData = new ArrayList<>();
                                while (resultSet.next())
                                    playerData.add(new PPlayerData(
                                        UUID.fromString(resultSet.getString("playerUUID")),
                                        playerName,
                                        resultSet.getInt("sizeLimit"),
                                        resultSet.getInt("countLimit"),
                                        resultSet.getLong("permissions")));
                                return playerData;
                            }, Collections.emptyList());
    }

    @Override
    public ConcurrentHashMap<Integer, List<Long>> getPowerBlockData(long chunkHash)
    {
        return executeQuery(SQLStatement.GET_POWER_BLOCK_DATA_IN_CHUNK.constructPPreparedStatement()
                                                                      .setLong(1, chunkHash),
                            resultSet ->
                            {
                                final ConcurrentHashMap<Integer, List<Long>> doors = new ConcurrentHashMap<>();
                                while (resultSet.next())
                                {
                                    final int locationHash =
                                        Util.simpleChunkSpaceLocationhash(resultSet.getInt("powerBlockX"),
                                                                          resultSet.getInt("powerBlockY"),
                                                                          resultSet.getInt("powerBlockZ"));
                                    if (!doors.containsKey(locationHash))
                                        doors.put(locationHash, new ArrayList<>());
                                    doors.get(locationHash).add(resultSet.getLong("id"));
                                }
                                return doors;
                            }, new ConcurrentHashMap<>());
    }

    @Override
    public List<Long> getDoorsInChunk(long chunkHash)
    {
        return executeQuery(SQLStatement.GET_DOOR_IDS_IN_CHUNK.constructPPreparedStatement()
                                                              .setLong(1, chunkHash),
                            resultSet ->
                            {
                                final List<Long> doors = new ArrayList<>();
                                while (resultSet.next())
                                    doors.add(resultSet.getLong("id"));
                                return doors;
                            }, new ArrayList<>(0));
    }

    @Override
    public boolean removeOwner(long doorUID, UUID playerUUID)
    {
        return executeUpdate(SQLStatement.REMOVE_DOOR_OWNER.constructPPreparedStatement()
                                                           .setString(1, playerUUID.toString())
                                                           .setLong(2, doorUID)) > 0;
    }

    private Map<UUID, DoorOwner> getOwnersOfDoor(long doorUID)
    {
        return executeQuery(SQLStatement.GET_DOOR_OWNERS.constructPPreparedStatement()
                                                        .setLong(1, doorUID),
                            resultSet ->
                            {
                                final Map<UUID, DoorOwner> ret = new HashMap<>();
                                while (resultSet.next())
                                {
                                    final UUID uuid = UUID.fromString(resultSet.getString("playerUUID"));
                                    final PPlayerData playerData =
                                        new PPlayerData(uuid,
                                                        resultSet.getString("playerName"),
                                                        resultSet.getInt("sizeLimit"),
                                                        resultSet.getInt("countLimit"),
                                                        resultSet.getLong("permissions"));

                                    ret.put(uuid, new DoorOwner(resultSet.getLong("doorUID"),
                                                                resultSet.getInt("permission"),
                                                                playerData));
                                }
                                return ret;
                            }, new HashMap<>(0));
    }

    @Override
    public boolean addOwner(long doorUID, PPlayerData player, int permission)
    {
        // permission level 0 is reserved for the creator, and negative values are not allowed.
        if (permission < 1)
            return false;

        return executeTransaction(
            conn ->
            {
                final long playerID = getPlayerID(conn, new DoorOwner(doorUID, permission, player.getPPlayerData()));

                if (playerID == -1)
                    throw new IllegalArgumentException(
                        "Trying to add player \"" + player.getUUID() + "\" as owner of door " + doorUID +
                            ", but that player is not registered in the database! Aborting...");

                return executeQuery(
                    conn, SQLStatement.GET_DOOR_OWNER_PLAYER.constructPPreparedStatement()
                                                            .setLong(1, playerID)
                                                            .setLong(2, doorUID),
                    rs ->
                    {
                        final SQLStatement statement = (rs.next() && (rs.getInt("permission") != permission)) ?
                                                       SQLStatement.UPDATE_DOOR_OWNER_PERMISSION :
                                                       SQLStatement.INSERT_DOOR_OWNER;

                        return
                            executeUpdate(conn, statement
                                .constructPPreparedStatement()
                                .setInt(1, permission)
                                .setLong(2, playerID)
                                .setLong(3, doorUID)) > 0;
                    }, false);
            }, false);
    }

    /**
     * Obtains and checks the version of the database.
     * <p>
     * If the database version is invalid for this version of the database class, an error will be printed and the
     * appropriate {@link #databaseState} will be set.
     *
     * @param conn
     *     A connection to the database.
     * @return The version of the database, or -1 if something went wrong.
     */
    private int verifyDatabaseVersion(Connection conn)
    {
        final int dbVersion = executeQuery(conn, new PPreparedStatement("PRAGMA user_version;"),
                                           rs -> rs.getInt(1), -1);
        if (dbVersion == -1)
        {
            log.at(Level.SEVERE).log("Failed to obtain database version!");
            databaseState = DatabaseState.ERROR;
            return dbVersion;
        }

        if (dbVersion == DATABASE_VERSION)
        {
            databaseState = DatabaseState.OK;
        }

        if (dbVersion < MIN_DATABASE_VERSION)
        {
            log.at(Level.SEVERE)
               .log("Trying to load database version %s while the minimum allowed version is %s",
                    dbVersion, MIN_DATABASE_VERSION);
            databaseState = DatabaseState.TOO_OLD;
        }

        if (dbVersion > DATABASE_VERSION)
        {
            log.at(Level.SEVERE)
               .log("Trying to load database version %s while the maximum allowed version is %s",
                    dbVersion, DATABASE_VERSION);
            databaseState = DatabaseState.TOO_NEW;
        }
        return dbVersion;
    }

    /**
     * Upgrades the database to the latest version if needed.
     */
    private void upgrade()
    {
        @Nullable Connection conn;
        try
        {
            conn = getConnection(DatabaseState.OUT_OF_DATE, ReadMode.READ_WRITE);
            if (conn == null)
                return;

            final int dbVersion = verifyDatabaseVersion(conn);
            if (databaseState != DatabaseState.OUT_OF_DATE)
            {
                conn.close();
                return;
            }

            conn.close();
            if (!makeBackup())
                return;
            conn = getConnection(DatabaseState.OUT_OF_DATE, ReadMode.READ_WRITE);
            if (conn == null)
                return;

            if (dbVersion < 11)
                upgradeToV11(conn);

            // Do this at the very end, so the db version isn't altered if anything fails.
            updateDBVersion(conn);
            databaseState = DatabaseState.OK;
        }
        catch (SQLException | NullPointerException e)
        {
            log.at(Level.SEVERE).withCause(e).log();
            databaseState = DatabaseState.ERROR;
        }
    }

    /**
     * Makes a backup of the database file. Stored in a database with the same name, but with ".BACKUP" appended to it.
     *
     * @return True if backup creation was successful.
     */
    private boolean makeBackup()
    {
        final Path dbFileBackup = dbFile.resolveSibling(dbFile.getFileName() + ".BACKUP");

        try
        {
            // Only the most recent backup is kept, so replace any existing backups.
            Files.copy(dbFile, dbFileBackup, StandardCopyOption.REPLACE_EXISTING);
        }
        catch (IOException e)
        {
            log.at(Level.SEVERE).withCause(e)
               .log("Failed to create backup of the database! Database upgrade aborted and access is disabled!");
            return false;
        }
        return true;
    }

    /**
     * Updates the version of the database and sets it to {@link #DATABASE_VERSION}.
     *
     * @param conn
     *     An active connection to the database.
     */
    private void updateDBVersion(Connection conn)
    {
        try (Statement statement = conn.createStatement())
        {
            statement.execute("PRAGMA user_version = " + DATABASE_VERSION + ";");
        }
        catch (SQLException | NullPointerException e)
        {
            log.at(Level.SEVERE).withCause(e).log();
        }
    }

    /**
     * Upgrades the database to V11.
     *
     * @param conn
     *     Opened database connection.
     */
    /*
     * Changes in V11:
     * - Updating chunkHash of all doors because the algorithm was changed.
     */
    private void upgradeToV11(Connection conn)
    {
        try (PreparedStatement ps1 = conn.prepareStatement("SELECT * FROM doors;");
             ResultSet rs1 = ps1.executeQuery())
        {
            log.at(Level.WARNING).log("Upgrading database to V11!");

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
            log.at(Level.SEVERE).withCause(e).log();
        }
    }

    /**
     * Executes an update defined by a {@link PPreparedStatement}.
     *
     * @param pPreparedStatement
     *     The {@link PPreparedStatement}.
     * @return Either the number of rows modified by the update, or -1 if an error occurred.
     */
    private int executeUpdate(PPreparedStatement pPreparedStatement)
    {
        try (@Nullable Connection conn = getConnection(ReadMode.READ_WRITE))
        {
            if (conn == null)
            {
                logStatement(pPreparedStatement);
                return -1;
            }
            return executeUpdate(conn, pPreparedStatement);
        }
        catch (SQLException e)
        {
            log.at(Level.SEVERE).withCause(e).log();
        }
        return -1;
    }

    /**
     * Executes an update defined by a {@link PPreparedStatement}.
     *
     * @param conn
     *     A connection to the database.
     * @param pPreparedStatement
     *     The {@link PPreparedStatement}.
     * @return Either the number of rows modified by the update, or -1 if an error occurred.
     */
    private int executeUpdate(Connection conn, PPreparedStatement pPreparedStatement)
    {
        logStatement(pPreparedStatement);
        try (PreparedStatement ps = pPreparedStatement.construct(conn))
        {
            return ps.executeUpdate();
        }
        catch (SQLException e)
        {
            log.at(Level.SEVERE).withCause(e).log();
        }
        return -1;
    }

    /**
     * Executes an update defined by a {@link PPreparedStatement} and returns the generated key index. See {@link
     * Statement#RETURN_GENERATED_KEYS}.
     *
     * @param pPreparedStatement
     *     The {@link PPreparedStatement}.
     * @return The generated key index if possible, otherwise -1.
     */
    @SuppressWarnings("unused")
    private int executeUpdateReturnGeneratedKeys(PPreparedStatement pPreparedStatement)
    {
        try (@Nullable Connection conn = getConnection(ReadMode.READ_WRITE))
        {
            if (conn == null)
            {
                logStatement(pPreparedStatement);
                return -1;
            }
            return executeUpdateReturnGeneratedKeys(conn, pPreparedStatement);
        }
        catch (SQLException e)
        {
            log.at(Level.SEVERE).withCause(e).log();
        }
        return -1;
    }

    /**
     * Executes an update defined by a {@link PPreparedStatement} and returns the generated key index. See {@link
     * Statement#RETURN_GENERATED_KEYS}.
     *
     * @param conn
     *     A connection to the database.
     * @param pPreparedStatement
     *     The {@link PPreparedStatement}.
     * @return The generated key index if possible, otherwise -1.
     */
    private int executeUpdateReturnGeneratedKeys(Connection conn, PPreparedStatement pPreparedStatement)
    {
        logStatement(pPreparedStatement);
        try (PreparedStatement ps = pPreparedStatement.construct(conn, Statement.RETURN_GENERATED_KEYS))
        {
            ps.executeUpdate();
            try (ResultSet resultSet = ps.getGeneratedKeys())
            {
                return resultSet.getInt(1);
            }
            catch (SQLException ex)
            {
                log.at(Level.SEVERE).withCause(ex).log();
            }
        }
        catch (SQLException e)
        {
            log.at(Level.SEVERE).withCause(e).log();
        }
        return -1;
    }

    /**
     * Executes a query defined by a {@link PPreparedStatement} and applies a function to the result.
     *
     * @param pPreparedStatement
     *     The {@link PPreparedStatement}.
     * @param fun
     *     The function to apply to the {@link ResultSet}.
     * @param fallback
     *     The value to return in case the result is null or if an error occurred.
     * @param <T>
     *     The type of the result to return.
     * @return The {@link ResultSet} of the query, or null in case an error occurred.
     */
    @Contract(" _, _, !null -> !null;")
    private @Nullable <T> T executeQuery(PPreparedStatement pPreparedStatement,
                                         CheckedFunction<ResultSet, T, Exception> fun,
                                         @Nullable T fallback)
    {
        try (@Nullable Connection conn = getConnection(ReadMode.READ_ONLY))
        {
            if (conn == null)
            {
                logStatement(pPreparedStatement);
                return fallback;
            }
            return executeQuery(conn, pPreparedStatement, fun, fallback);
        }
        catch (Exception e)
        {
            log.at(Level.SEVERE).withCause(e).log();
        }
        return fallback;
    }

    /**
     * Executes a batched query defined by a {@link PPreparedStatement} and applies a function to the result.
     *
     * @param pPreparedStatement
     *     The {@link PPreparedStatement}.
     * @param fun
     *     The function to apply to the {@link ResultSet}.
     * @param fallback
     *     The value to return in case the result is null or if an error occurred.
     * @param readMode
     *     The {@link ReadMode} to use for the database.
     * @param <T>
     *     The type of the result to return.
     * @return The {@link ResultSet} of the query, or null in case an error occurred.
     */
    @SuppressWarnings("unused") @Contract(" _, _, !null ,_ -> !null;")
    private @Nullable <T> T executeBatchQuery(PPreparedStatement pPreparedStatement,
                                              CheckedFunction<ResultSet, T, Exception> fun, @Nullable T fallback,
                                              ReadMode readMode)
    {
        try (@Nullable Connection conn = getConnection(readMode))
        {
            if (conn == null)
            {
                logStatement(pPreparedStatement);
                return fallback;
            }
            conn.setAutoCommit(false);
            final @Nullable T result = executeQuery(conn, pPreparedStatement, fun, fallback);
            conn.commit();
            conn.setAutoCommit(true);
            return result;
        }
        catch (Exception e)
        {
            log.at(Level.SEVERE).withCause(e).log();
        }
        return fallback;
    }

    /**
     * Executes a query defined by a {@link PPreparedStatement} and applies a function to the result.
     *
     * @param conn
     *     A connection to the database.
     * @param pPreparedStatement
     *     The {@link PPreparedStatement}.
     * @param fun
     *     The function to apply to the {@link ResultSet}.
     * @param fallback
     *     The value to return in case the result is null or if an error occurred.
     * @param <T>
     *     The type of the result to return.
     * @return The {@link ResultSet} of the query, or null in case an error occurred.
     */
    @Contract(" _, _, _, !null -> !null")
    private @Nullable <T> T executeQuery(Connection conn, PPreparedStatement pPreparedStatement,
                                         CheckedFunction<ResultSet, T, Exception> fun, @Nullable T fallback)
    {
        logStatement(pPreparedStatement);
        try (PreparedStatement ps = pPreparedStatement.construct(conn);
             ResultSet rs = ps.executeQuery())
        {
            return fun.apply(rs);
        }
        catch (Exception e)
        {
            log.at(Level.SEVERE).withCause(e).log();
        }
        return fallback;
    }

    /**
     * Executes a {@link CheckedFunction} given an active Connection.
     *
     * @param fun
     *     The function to execute.
     * @param fallback
     *     The fallback value to return in case of failure.
     * @param readMode
     *     The {@link ReadMode} to use for the database.
     * @param <T>
     *     The type of the result to return.
     * @return The result of the Function.
     */
    @SuppressWarnings("unused") @Contract(" _, !null, _  -> !null")
    private @Nullable <T> T execute(CheckedFunction<Connection, T, Exception> fun, @Nullable T fallback,
                                    ReadMode readMode)
    {
        return execute(fun, fallback, FailureAction.IGNORE, readMode);
    }

    /**
     * Executes a {@link CheckedFunction} given an active Connection.
     *
     * @param fun
     *     The function to execute.
     * @param fallback
     *     The fallback value to return in case of failure.
     * @param failureAction
     *     The action to take when an exception is caught.
     * @param readMode
     *     The {@link ReadMode} to use for the database.
     * @param <T>
     *     The type of the result to return.
     * @return The result of the Function.
     */
    @Contract(" _, !null, _, _ -> !null")
    private @Nullable <T> T execute(CheckedFunction<Connection, T, Exception> fun, @Nullable T fallback,
                                    FailureAction failureAction, ReadMode readMode)
    {
        try (@Nullable Connection conn = getConnection(readMode))
        {
            try
            {
                if (conn == null)
                    return fallback;
                return fun.apply(conn);
            }
            catch (Exception e)
            {
                if (failureAction == FailureAction.ROLLBACK)
                    conn.rollback();
                log.at(Level.SEVERE).withCause(e).log();
            }
        }
        catch (Exception e)
        {
            log.at(Level.SEVERE).withCause(e).log();
        }
        return fallback;
    }

    /**
     * Executes a {@link CheckedFunction} given an active Connection as a transaction. In case an error was caught, it
     * will attempt to roll back to the state before the action was applied.
     *
     * @param fun
     *     The function to execute.
     * @param fallback
     *     The fallback value to return in case of failure.
     * @param <T>
     *     The type of the result to return.
     * @return The result of the Function.
     */
    @Contract(" _, !null -> !null")
    private @Nullable <T> T executeTransaction(CheckedFunction<Connection, T, Exception> fun, @Nullable T fallback)
    {
        return execute(
            conn ->
            {
                conn.setAutoCommit(false);
                final T result = fun.apply(conn);
                conn.commit();
                return result;
            }, fallback, FailureAction.ROLLBACK, ReadMode.READ_WRITE);
    }

    /**
     * Logs a {@link PPreparedStatement} to the logger.
     *
     * @param pPreparedStatement
     *     The {@link PPreparedStatement} to log.
     */
    private void logStatement(PPreparedStatement pPreparedStatement)
    {
        log.at(Level.FINEST).log("Executed statement: %s", pPreparedStatement);
    }

    /**
     * Describes the action to take when an exception is caught.
     */
    private enum FailureAction
    {
        /**
         * Don't do anything special when an exception is caught. It'll still log the error.
         */
        IGNORE,

        /**
         * Attempt to roll back the database when an exception is caught.
         */
        ROLLBACK,
    }

    /**
     * Represents a reading mode for the database file.
     * <p>
     * Restricting the read mode to read only may result in better performance at the cost of the ability to write to
     * the database.
     */
    private enum ReadMode
    {
        /**
         * Allows writing to the database, at a performance cost.
         */
        READ_WRITE,

        /**
         * Does not allow writing to the database (surprise!), but may result in better performance.
         */
        READ_ONLY,
    }
}
