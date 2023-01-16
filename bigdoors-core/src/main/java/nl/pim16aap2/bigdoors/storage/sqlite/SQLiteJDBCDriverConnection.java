package nl.pim16aap2.bigdoors.storage.sqlite;

import com.google.common.flogger.StackSize;
import lombok.Getter;
import lombok.extern.flogger.Flogger;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.api.IPWorld;
import nl.pim16aap2.bigdoors.api.PPlayerData;
import nl.pim16aap2.bigdoors.api.debugging.DebuggableRegistry;
import nl.pim16aap2.bigdoors.api.debugging.IDebuggable;
import nl.pim16aap2.bigdoors.api.factories.IPWorldFactory;
import nl.pim16aap2.bigdoors.doors.AbstractDoor;
import nl.pim16aap2.bigdoors.doors.DoorBase;
import nl.pim16aap2.bigdoors.doors.DoorBaseBuilder;
import nl.pim16aap2.bigdoors.doors.DoorOwner;
import nl.pim16aap2.bigdoors.doors.DoorSerializer;
import nl.pim16aap2.bigdoors.doors.IDoorConst;
import nl.pim16aap2.bigdoors.doors.PermissionLevel;
import nl.pim16aap2.bigdoors.doortypes.DoorType;
import nl.pim16aap2.bigdoors.managers.DatabaseManager;
import nl.pim16aap2.bigdoors.managers.DoorRegistry;
import nl.pim16aap2.bigdoors.managers.DoorTypeManager;
import nl.pim16aap2.bigdoors.storage.IStorage;
import nl.pim16aap2.bigdoors.storage.PPreparedStatement;
import nl.pim16aap2.bigdoors.storage.SQLStatement;
import nl.pim16aap2.bigdoors.util.Cuboid;
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
import java.util.Objects;
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
public final class SQLiteJDBCDriverConnection implements IStorage, IDebuggable
{
    private static final String DRIVER = "org.sqlite.JDBC";
    private static final int DATABASE_VERSION = 12;
    private static final int MIN_DATABASE_VERSION = 10;

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

    private @Nullable Connection connection;

    /**
     * Constructor of the SQLite driver connection.
     *
     * @param dbFile
     *     The file to store the database in.
     */
    @Inject
    public SQLiteJDBCDriverConnection(
        @Named("databaseFile") Path dbFile, DoorBaseBuilder doorBaseBuilder, DoorRegistry doorRegistry,
        DoorTypeManager doorTypeManager, IPWorldFactory worldFactory, DebuggableRegistry debuggableRegistry)
    {
        this.dbFile = dbFile;
        this.doorBaseBuilder = doorBaseBuilder;
        this.doorRegistry = doorRegistry;
        this.doorTypeManager = doorTypeManager;
        this.worldFactory = worldFactory;

        try
        {
            if (!loadDriver())
            {
                log.at(Level.WARNING).log("Failed to load database driver!");
                databaseState = DatabaseState.NO_DRIVER;
                return;
            }
            init();
            log.at(Level.FINE).log("Database initialized! Current state: %s", databaseState);
            if (databaseState == DatabaseState.OUT_OF_DATE)
                upgrade();
        }
        catch (Exception e)
        {
            log.at(Level.SEVERE).withCause(e).log("Failed to initialize database!");
            databaseState = DatabaseState.ERROR;
        }
        debuggableRegistry.registerDebuggable(this);
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
            log.at(Level.SEVERE).withCause(e).log("Failed to load database driver: %s!", DRIVER);
        }
        return false;
    }

    /**
     * Establishes a connection with the database.
     *
     * @param state
     *     The state from which the connection was requested.
     * @return A database connection.
     */
    private @Nullable Connection getConnection(DatabaseState state)
    {
        if (!databaseState.equals(state))
        {
            log.at(Level.SEVERE).withStackTrace(StackSize.FULL)
               .log("Database connection could not be created! " +
                        "Requested database for state '%s' while it is actually in state '%s'!",
                    state.name(), databaseState.name());
            return null;
        }

        return connection;
    }

    /**
     * Establishes a connection with the database, assuming a database state of {@link DatabaseState#OK}.
     *
     * @return A database connection.
     */
    private @Nullable Connection getConnection()
    {
        return getConnection(DatabaseState.OK);
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

    private @Nullable Connection openConnection()
        throws SQLException
    {
        final SQLiteConfig configRW = new SQLiteConfig();
        configRW.enforceForeignKeys(true);

        final String url = "jdbc:sqlite:" + dbFile;
        return configRW.createConnection(url);
    }

    /**
     * Initializes the database. I.e. create all the required files/tables.
     */
    private synchronized void init()
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

        try
        {
            this.connection = openConnection();
        }
        catch (SQLException e)
        {
            log.at(Level.SEVERE).withCause(e).log("Failed to open SQLite connections!");
            return;
        }

        // Table creation
        try
        {
            final @Nullable Connection conn = getConnection(DatabaseState.UNINITIALIZED);
            if (conn == null)
            {
                databaseState = DatabaseState.ERROR;
                return;
            }

            // Check if the "doors" table already exists. If it does, assume the rest exists
            // as well and don't set it up.
            if (conn.getMetaData().getTables(null, null, "DoorBase", new String[]{"TABLE"}).next())
            {
                databaseState = DatabaseState.OUT_OF_DATE;
                verifyDatabaseVersion(conn);
            }
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
        @SuppressWarnings("squid:S3655") //
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
        final Vector3Di rotationPoint = new Vector3Di(doorBaseRS.getInt("rotationPointX"),
                                                      doorBaseRS.getInt("rotationPointY"),
                                                      doorBaseRS.getInt("rotationPointZ"));
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

        final DoorOwner primeOwner = new DoorOwner(
            doorUID, Objects.requireNonNull(PermissionLevel.fromValue(doorBaseRS.getInt("permission"))), playerData);

        final Map<UUID, DoorOwner> doorOwners = getOwnersOfDoor(doorUID);
        final DoorBase doorData = doorBaseBuilder.builder().uid(doorUID).name(name).cuboid(new Cuboid(min, max))
                                                 .rotationPoint(rotationPoint).powerBlock(powerBlock).world(world)
                                                 .isOpen(isOpen)
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
        executeUpdate(conn, SQLStatement.INSERT_DOOR_BASE.constructPPreparedStatement()
                                                         .setNextString(door.getName())
                                                         .setNextString(worldName)
                                                         .setNextInt(door.getMinimum().x())
                                                         .setNextInt(door.getMinimum().y())
                                                         .setNextInt(door.getMinimum().z())
                                                         .setNextInt(door.getMaximum().x())
                                                         .setNextInt(door.getMaximum().y())
                                                         .setNextInt(door.getMaximum().z())
                                                         .setNextInt(door.getRotationPoint().x())
                                                         .setNextInt(door.getRotationPoint().y())
                                                         .setNextInt(door.getRotationPoint().z())
                                                         .setNextLong(Util.getChunkId(door.getRotationPoint()))
                                                         .setNextInt(door.getPowerBlock().x())
                                                         .setNextInt(door.getPowerBlock().y())
                                                         .setNextInt(door.getPowerBlock().z())
                                                         .setNextLong(Util.getChunkId(door.getPowerBlock()))
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
        final DoorSerializer<?> serializer = door.getDoorType().getDoorSerializer();

        final String typeName = door.getDoorType().getFullName();
        try
        {
            final byte[] typeData = serializer.serialize(door);

            final long doorUID = executeTransaction(conn -> insert(conn, door, typeName, typeData), -1L);
            if (doorUID > 0)
                return Optional.of(serializer.deserialize(
                    doorBaseBuilder.builder().uid(doorUID).name(door.getName()).cuboid(door.getCuboid())
                                   .rotationPoint(door.getRotationPoint()).powerBlock(door.getPowerBlock())
                                   .world(door.getWorld())
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
    public boolean syncDoorData(IDoorConst doorBase, byte[] typeData)
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

                                 .setNextInt(doorBase.getRotationPoint().x())
                                 .setNextInt(doorBase.getRotationPoint().y())
                                 .setNextInt(doorBase.getRotationPoint().z())
                                 .setNextLong(Util.getChunkId(doorBase.getRotationPoint()))

                                 .setNextInt(doorBase.getPowerBlock().x())
                                 .setNextInt(doorBase.getPowerBlock().y())
                                 .setNextInt(doorBase.getPowerBlock().z())
                                 .setNextLong(Util.getChunkId(doorBase.getPowerBlock()))

                                 .setNextInt(RotateDirection.getValue(doorBase.getOpenDir()))
                                 .setNextLong(getFlag(doorBase.isOpen(), doorBase.isLocked()))
                                 .setNextBytes(typeData)

                                 .setNextLong(doorBase.getDoorUID())) > 0;
    }

    @Override
    public List<DatabaseManager.DoorIdentifier> getPartialIdentifiers(
        String input, @Nullable IPPlayer player, PermissionLevel maxPermission)
    {
        final PPreparedStatement query = Util.isNumerical(input) ?
                                         SQLStatement.GET_IDENTIFIERS_FROM_PARTIAL_UID_MATCH_WITH_OWNER
                                             .constructPPreparedStatement().setNextLong(Long.parseLong(input)) :
                                         SQLStatement.GET_IDENTIFIERS_FROM_PARTIAL_NAME_MATCH_WITH_OWNER
                                             .constructPPreparedStatement().setNextString(input);

        query.setNextInt(maxPermission.getValue());

        final @Nullable String uuid = player == null ? null : player.getUUID().toString();
        query.setNextString(uuid);
        query.setNextString(uuid);

        return executeQuery(query, this::collectIdentifiers, Collections.emptyList());
    }

    private List<DatabaseManager.DoorIdentifier> collectIdentifiers(ResultSet resultSet)
        throws SQLException
    {
        final List<DatabaseManager.DoorIdentifier> ret = new ArrayList<>();

        while (resultSet.next())
            ret.add(new DatabaseManager.DoorIdentifier(resultSet.getLong("id"), resultSet.getString("name")));
        return ret;
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
     * Attempts to construct a subclass of {@link DoorBase} from a ResultSet containing all data pertaining the
     * {@link DoorBase} (as stored in the "DoorBase" table), as well as the owner (name, UUID, permission) and the
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
    public List<AbstractDoor> getDoors(UUID playerUUID, String doorName, PermissionLevel maxPermission)
    {
        return executeQuery(SQLStatement.GET_NAMED_DOORS_OWNED_BY_PLAYER.constructPPreparedStatement()
                                                                        .setString(1, playerUUID.toString())
                                                                        .setString(2, doorName)
                                                                        .setInt(3, maxPermission.getValue()),
                            this::getDoors, Collections.emptyList());
    }

    @Override
    public List<AbstractDoor> getDoors(UUID playerUUID, String name)
    {
        return getDoors(playerUUID, name, PermissionLevel.CREATOR);
    }

    @Override
    public List<AbstractDoor> getDoors(String name)
    {
        return executeQuery(SQLStatement.GET_DOORS_WITH_NAME.constructPPreparedStatement()
                                                            .setString(1, name),
                            this::getDoors, Collections.emptyList());
    }

    @Override
    public List<AbstractDoor> getDoors(UUID playerUUID, PermissionLevel maxPermission)
    {
        return executeQuery(SQLStatement.GET_DOORS_OWNED_BY_PLAYER_WITH_LEVEL.constructPPreparedStatement()
                                                                             .setString(1, playerUUID.toString())
                                                                             .setInt(2, maxPermission.getValue()),
                            this::getDoors, Collections.emptyList());
    }

    @Override
    public List<AbstractDoor> getDoors(UUID playerUUID)
    {
        return getDoors(playerUUID, PermissionLevel.CREATOR);
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
    public ConcurrentHashMap<Integer, List<Long>> getPowerBlockData(long chunkId)
    {
        return executeQuery(SQLStatement.GET_POWER_BLOCK_DATA_IN_CHUNK.constructPPreparedStatement()
                                                                      .setLong(1, chunkId),
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
    public List<Long> getDoorsInChunk(long chunkId)
    {
        return executeQuery(SQLStatement.GET_DOOR_IDS_IN_CHUNK.constructPPreparedStatement()
                                                              .setLong(1, chunkId),
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

                                    ret.put(uuid, new DoorOwner(
                                        resultSet.getLong("doorUID"),
                                        Objects.requireNonNull(
                                            PermissionLevel.fromValue(resultSet.getInt("permission"))),
                                        playerData));
                                }
                                return ret;
                            }, new HashMap<>(0));
    }

    @Override
    public boolean addOwner(long doorUID, PPlayerData player, PermissionLevel permission)
    {
        // permission level 0 is reserved for the creator, and negative values are not allowed.
        if (permission.getValue() < 1 || permission == PermissionLevel.NO_PERMISSION)
        {
            log.at(Level.INFO).withStackTrace(StackSize.FULL)
               .log("Cannot add co-owner with permission level %d", permission.getValue());
            return false;
        }

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
                        final SQLStatement statement =
                            (rs.next() && (rs.getInt("permission") != permission.getValue())) ?
                            SQLStatement.UPDATE_DOOR_OWNER_PERMISSION :
                            SQLStatement.INSERT_DOOR_OWNER;

                        return
                            executeUpdate(conn, statement
                                .constructPPreparedStatement()
                                .setInt(1, permission.getValue())
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

        if (dbVersion > DATABASE_VERSION)
        {
            log.at(Level.SEVERE)
               .log("Trying to load database version %s while the maximum allowed version is %s",
                    dbVersion, DATABASE_VERSION);
            databaseState = DatabaseState.TOO_NEW;
        }
        else if (dbVersion < MIN_DATABASE_VERSION)
        {
            log.at(Level.SEVERE)
               .log("Trying to load database version %s while the minimum allowed version is %s",
                    dbVersion, MIN_DATABASE_VERSION);
            databaseState = DatabaseState.TOO_OLD;
        }
        else if (dbVersion < DATABASE_VERSION)
        {
            databaseState = DatabaseState.OUT_OF_DATE;
        }
        else
        {
            databaseState = DatabaseState.OK;
        }
        return dbVersion;
    }

    /**
     * Upgrades the database to the latest version if needed.
     */
    private void upgrade()
    {
        try
        {
            final @Nullable Connection conn = getConnection(DatabaseState.OUT_OF_DATE);
            if (conn == null)
            {
                log.at(Level.SEVERE).withStackTrace(StackSize.FULL)
                   .log("Failed to upgrade database: Connection unavailable!");
                databaseState = DatabaseState.ERROR;
                return;
            }

            final int dbVersion = verifyDatabaseVersion(conn);
            log.at(Level.FINE).log("Upgrading database from version %d to version %d.", dbVersion, DATABASE_VERSION);

            if (!makeBackup())
                return;

            if (dbVersion < 11)
                throw new IllegalStateException("Database version " + dbVersion + " is not supported!");

            updateDBVersion(conn);
            databaseState = DatabaseState.OK;
        }
        catch (Exception e)
        {
            log.at(Level.SEVERE).withCause(e).log("Failed to upgrade database!");
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
     * Executes an update defined by a {@link PPreparedStatement}.
     *
     * @param pPreparedStatement
     *     The {@link PPreparedStatement}.
     * @return Either the number of rows modified by the update, or -1 if an error occurred.
     */
    private int executeUpdate(PPreparedStatement pPreparedStatement)
    {
        try
        {
            final @Nullable Connection conn = getConnection();
            if (conn == null)
            {
                logStatement(pPreparedStatement);
                return -1;
            }
            return executeUpdate(conn, pPreparedStatement);
        }
        catch (Exception e)
        {
            log.at(Level.SEVERE).withCause(e).log("Failed to execute update: %s", pPreparedStatement);
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
            log.at(Level.SEVERE).withCause(e).log("Failed to execute update: %s", pPreparedStatement);
        }
        return -1;
    }

    /**
     * Executes an update defined by a {@link PPreparedStatement} and returns the generated key index. See
     * {@link Statement#RETURN_GENERATED_KEYS}.
     *
     * @param pPreparedStatement
     *     The {@link PPreparedStatement}.
     * @return The generated key index if possible, otherwise -1.
     */
    @SuppressWarnings("unused")
    private int executeUpdateReturnGeneratedKeys(PPreparedStatement pPreparedStatement)
    {
        try
        {
            final @Nullable Connection conn = getConnection();
            if (conn == null)
            {
                logStatement(pPreparedStatement);
                return -1;
            }
            return executeUpdateReturnGeneratedKeys(conn, pPreparedStatement);
        }
        catch (Exception e)
        {
            log.at(Level.SEVERE).withCause(e).log("Failed to execute update: %s", pPreparedStatement);
        }
        return -1;
    }

    /**
     * Executes an update defined by a {@link PPreparedStatement} and returns the generated key index. See
     * {@link Statement#RETURN_GENERATED_KEYS}.
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
                log.at(Level.SEVERE).withCause(ex)
                   .log("Failed to get generated key for statement: %s", pPreparedStatement);
            }
        }
        catch (SQLException e)
        {
            log.at(Level.SEVERE).withCause(e).log("Failed to execute update: %s", pPreparedStatement);
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
    private @Nullable <T> T executeQuery(
        PPreparedStatement pPreparedStatement, CheckedFunction<ResultSet, T, Exception> fun, @Nullable T fallback)
    {
        try
        {
            final @Nullable Connection conn = getConnection();
            if (conn == null)
            {
                logStatement(pPreparedStatement);
                return fallback;
            }
            return executeQuery(conn, pPreparedStatement, fun, fallback);
        }
        catch (Exception e)
        {
            log.at(Level.SEVERE).withCause(e).log("Failed to execute query: %s", pPreparedStatement);
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
     * @param <T>
     *     The type of the result to return.
     * @return The {@link ResultSet} of the query, or null in case an error occurred.
     */
    @SuppressWarnings("unused") @Contract(" _, _, !null -> !null;")
    private @Nullable <T> T executeBatchQuery(
        PPreparedStatement pPreparedStatement, CheckedFunction<ResultSet, T, Exception> fun, @Nullable T fallback)
    {
        try
        {
            final @Nullable Connection conn = getConnection();
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
            log.at(Level.SEVERE).withCause(e).log("Failed to execute batch query: %s", pPreparedStatement);
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
    private @Nullable <T> T executeQuery(
        Connection conn, PPreparedStatement pPreparedStatement, CheckedFunction<ResultSet, T, Exception> fun,
        @Nullable T fallback)
    {
        logStatement(pPreparedStatement);
        try (PreparedStatement ps = pPreparedStatement.construct(conn);
             ResultSet rs = ps.executeQuery())
        {
            return fun.apply(rs);
        }
        catch (Exception e)
        {
            log.at(Level.SEVERE).withCause(e).log("Failed to execute query: %s", pPreparedStatement);
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
     * @param <T>
     *     The type of the result to return.
     * @return The result of the Function.
     */
    @SuppressWarnings("unused") @Contract(" _, !null  -> !null")
    private @Nullable <T> T execute(CheckedFunction<Connection, T, Exception> fun, @Nullable T fallback)
    {
        return execute(fun, fallback, FailureAction.IGNORE);
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
     * @param <T>
     *     The type of the result to return.
     * @return The result of the Function.
     */
    @Contract(" _, !null, _ -> !null")
    private @Nullable <T> T execute(
        CheckedFunction<Connection, T, Exception> fun, @Nullable T fallback,
        FailureAction failureAction)
    {
        try
        {
            final @Nullable Connection conn = getConnection();
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
            }, fallback, FailureAction.ROLLBACK);
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

    @Override
    public String getDebugInformation()
    {
        return "Database state: " + databaseState.name() +
            "\nDatabase version: " + DATABASE_VERSION +
            "\nDatabase file: " + dbFile;
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
}
