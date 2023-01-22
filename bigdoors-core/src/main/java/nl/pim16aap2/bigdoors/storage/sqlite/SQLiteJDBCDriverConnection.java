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
import nl.pim16aap2.bigdoors.managers.DatabaseManager;
import nl.pim16aap2.bigdoors.managers.MovableRegistry;
import nl.pim16aap2.bigdoors.managers.MovableTypeManager;
import nl.pim16aap2.bigdoors.movable.AbstractMovable;
import nl.pim16aap2.bigdoors.movable.IMovableConst;
import nl.pim16aap2.bigdoors.movable.MovableBase;
import nl.pim16aap2.bigdoors.movable.MovableBaseBuilder;
import nl.pim16aap2.bigdoors.movable.MovableOwner;
import nl.pim16aap2.bigdoors.movable.MovableSerializer;
import nl.pim16aap2.bigdoors.movable.PermissionLevel;
import nl.pim16aap2.bigdoors.movabletypes.MovableType;
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
    private static final int DATABASE_VERSION = 13;
    private static final int MIN_DATABASE_VERSION = 12;

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

    private final MovableBaseBuilder movableBaseBuilder;

    private final MovableRegistry movableRegistry;

    private final MovableTypeManager movableTypeManager;

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
        @Named("databaseFile") Path dbFile, MovableBaseBuilder movableBaseBuilder, MovableRegistry movableRegistry,
        MovableTypeManager movableTypeManager, IPWorldFactory worldFactory, DebuggableRegistry debuggableRegistry)
    {
        this.dbFile = dbFile;
        this.movableBaseBuilder = movableBaseBuilder;
        this.movableRegistry = movableRegistry;
        this.movableTypeManager = movableTypeManager;
        this.worldFactory = worldFactory;

        try
        {
            if (!loadDriver())
            {
                log.atWarning().log("Failed to load database driver!");
                databaseState = DatabaseState.NO_DRIVER;
                return;
            }
            init();
            log.atFine().log("Database initialized! Current state: %s", databaseState);
            if (databaseState == DatabaseState.OUT_OF_DATE)
                upgrade();
        }
        catch (Exception e)
        {
            log.atSevere().withCause(e).log("Failed to initialize database!");
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
            log.atSevere().withCause(e).log("Failed to load database driver: %s!", DRIVER);
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
            log.atSevere().withStackTrace(StackSize.FULL)
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
                log.atInfo().log("New file created at: %s", dbFile);
            }
        }
        catch (IOException e)
        {
            log.atSevere().withCause(e).log("File write error: %s", dbFile);
            databaseState = DatabaseState.ERROR;
            return;
        }

        try
        {
            this.connection = openConnection();
        }
        catch (SQLException e)
        {
            log.atSevere().withCause(e).log("Failed to open SQLite connections!");
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

            // Check if the "movables" table already exists. If it does, assume the rest exists
            // as well and don't set it up.
            if (conn.getMetaData().getTables(null, null, "Movables", new String[]{"TABLE"}).next())
            {
                databaseState = DatabaseState.OUT_OF_DATE;
                verifyDatabaseVersion(conn);
            }
            else
            {
                executeUpdate(conn, SQLStatement.CREATE_TABLE_PLAYER.constructPPreparedStatement());
                executeUpdate(conn, SQLStatement.RESERVE_IDS_PLAYER.constructPPreparedStatement());

                executeUpdate(conn, SQLStatement.CREATE_TABLE_MOVABLE.constructPPreparedStatement());
                executeUpdate(conn, SQLStatement.RESERVE_IDS_MOVABLE.constructPPreparedStatement());

                executeUpdate(conn, SQLStatement.CREATE_TABLE_MOVABLE_OWNER_PLAYER.constructPPreparedStatement());
                executeUpdate(conn, SQLStatement.RESERVE_IDS_MOVABLE_OWNER_PLAYER.constructPPreparedStatement());

                updateDBVersion(conn);
                databaseState = DatabaseState.OK;
            }
        }
        catch (SQLException | NullPointerException e)
        {
            log.atSevere().withCause(e).log();
            databaseState = DatabaseState.ERROR;
        }
    }

    private Optional<AbstractMovable> constructMovable(ResultSet movableBaseRS)
        throws Exception
    {
        final @Nullable String movableTypeResult = movableBaseRS.getString("movableType");
        final Optional<MovableType> movableType = movableTypeManager.getMovableTypeFromFullName(movableTypeResult);

        if (!movableType.map(movableTypeManager::isRegistered).orElse(false))
        {
            log.atSevere()
               .withStackTrace(StackSize.FULL)
               .log("Type with ID: '%s' has not been registered (yet)!", movableTypeResult);
            return Optional.empty();
        }

        final long movableUID = movableBaseRS.getLong("id");

        // SonarLint assumes that movableType could be empty (S3655) (appears to miss the mapping operation above),
        // while this actually won't happen.
        @SuppressWarnings("squid:S3655") //
        final MovableSerializer<?> serializer = movableType.get().getMovableSerializer();

        final Optional<AbstractMovable> registeredMovable = movableRegistry.getRegisteredMovable(movableUID);
        if (registeredMovable.isPresent())
            return registeredMovable;

        final Optional<RotateDirection> openDirection =
            Optional.ofNullable(RotateDirection.valueOf(movableBaseRS.getInt("openDirection")));

        if (openDirection.isEmpty())
            return Optional.empty();

        final Vector3Di min = new Vector3Di(movableBaseRS.getInt("xMin"),
                                            movableBaseRS.getInt("yMin"),
                                            movableBaseRS.getInt("zMin"));
        final Vector3Di max = new Vector3Di(movableBaseRS.getInt("xMax"),
                                            movableBaseRS.getInt("yMax"),
                                            movableBaseRS.getInt("zMax"));
        final Vector3Di rotationPoint = new Vector3Di(movableBaseRS.getInt("rotationPointX"),
                                                      movableBaseRS.getInt("rotationPointY"),
                                                      movableBaseRS.getInt("rotationPointZ"));
        final Vector3Di powerBlock = new Vector3Di(movableBaseRS.getInt("powerBlockX"),
                                                   movableBaseRS.getInt("powerBlockY"),
                                                   movableBaseRS.getInt("powerBlockZ"));

        final IPWorld world = worldFactory.create(movableBaseRS.getString("world"));

        final long bitflag = movableBaseRS.getLong("bitflag");
        final boolean isOpen = IBitFlag.hasFlag(MovableFlag.getFlagValue(MovableFlag.IS_OPEN), bitflag);
        final boolean isLocked = IBitFlag.hasFlag(MovableFlag.getFlagValue(MovableFlag.IS_LOCKED), bitflag);

        final String name = movableBaseRS.getString("name");

        final PPlayerData playerData = new PPlayerData(UUID.fromString(movableBaseRS.getString("playerUUID")),
                                                       movableBaseRS.getString("playerName"),
                                                       movableBaseRS.getInt("sizeLimit"),
                                                       movableBaseRS.getInt("countLimit"),
                                                       movableBaseRS.getLong("permissions"));

        final MovableOwner primeOwner = new MovableOwner(
            movableUID, Objects.requireNonNull(PermissionLevel.fromValue(movableBaseRS.getInt("permission"))),
            playerData);

        final Map<UUID, MovableOwner> ownersOfMovable = getOwnersOfMovable(movableUID);
        final MovableBase movableData = movableBaseBuilder.builder()
                                                          .uid(movableUID)
                                                          .name(name)
                                                          .cuboid(new Cuboid(min, max))
                                                          .rotationPoint(rotationPoint)
                                                          .powerBlock(powerBlock)
                                                          .world(world)
                                                          .isOpen(isOpen)
                                                          .isLocked(isLocked)
                                                          .openDir(openDirection.get())
                                                          .primeOwner(primeOwner)
                                                          .ownersOfMovable(ownersOfMovable).build();

        final byte[] rawTypeData = movableBaseRS.getBytes("typeData");
        return Optional.of(serializer.deserialize(movableData, rawTypeData));
    }

    @Override
    public boolean deleteMovableType(MovableType movableType)
    {
        final boolean removed = executeTransaction(
            conn -> executeUpdate(SQLStatement.DELETE_MOVABLE_TYPE
                                      .constructPPreparedStatement()
                                      .setNextString(movableType.getFullName())) > 0, false);

        if (removed)
            movableTypeManager.unregisterMovableType(movableType);
        return removed;
    }

    private Long insert(Connection conn, AbstractMovable movable, String movableType, byte[] typeSpecificData)
    {
        final PPlayerData playerData = movable.getPrimeOwner().pPlayerData();
        insertOrIgnorePlayer(conn, playerData);

        final String worldName = movable.getWorld().worldName();
        executeUpdate(conn, SQLStatement.INSERT_MOVABLE_BASE.constructPPreparedStatement()
                                                            .setNextString(movable.getName())
                                                            .setNextString(worldName)
                                                            .setNextInt(movable.getMinimum().x())
                                                            .setNextInt(movable.getMinimum().y())
                                                            .setNextInt(movable.getMinimum().z())
                                                            .setNextInt(movable.getMaximum().x())
                                                            .setNextInt(movable.getMaximum().y())
                                                            .setNextInt(movable.getMaximum().z())
                                                            .setNextInt(movable.getRotationPoint().x())
                                                            .setNextInt(movable.getRotationPoint().y())
                                                            .setNextInt(movable.getRotationPoint().z())
                                                            .setNextLong(Util.getChunkId(movable.getRotationPoint()))
                                                            .setNextInt(movable.getPowerBlock().x())
                                                            .setNextInt(movable.getPowerBlock().y())
                                                            .setNextInt(movable.getPowerBlock().z())
                                                            .setNextLong(Util.getChunkId(movable.getPowerBlock()))
                                                            .setNextInt(RotateDirection.getValue(movable.getOpenDir()))
                                                            .setNextLong(getFlag(movable))
                                                            .setNextString(movableType)
                                                            .setNextBytes(typeSpecificData));

        // TODO: Just use the fact that the last-inserted movable has the current UID (that fact is already used by
        //       getTypeSpecificDataInsertStatement(MovableType)), so it can be done in a single statement.
        final long movableUID = executeQuery(conn,
                                             SQLStatement.SELECT_MOST_RECENT_MOVABLE.constructPPreparedStatement(),
                                             rs -> rs.next() ? rs.getLong("seq") : -1, -1L);

        executeUpdate(conn, SQLStatement.INSERT_PRIME_OWNER.constructPPreparedStatement()
                                                           .setString(1, playerData.getUUID().toString()));

        return movableUID;
    }

    @Override
    public Optional<AbstractMovable> insert(AbstractMovable movable)
    {
        final MovableSerializer<?> serializer = movable.getType().getMovableSerializer();

        final String typeName = movable.getType().getFullName();
        try
        {
            final byte[] typeData = serializer.serialize(movable);

            final long movableUID = executeTransaction(conn -> insert(conn, movable, typeName, typeData), -1L);
            if (movableUID > 0)
                return Optional.of(serializer.deserialize(
                    movableBaseBuilder.builder().uid(movableUID).name(movable.getName()).cuboid(movable.getCuboid())
                                      .rotationPoint(movable.getRotationPoint()).powerBlock(movable.getPowerBlock())
                                      .world(movable.getWorld())
                                      .isOpen(movable.isOpen()).isLocked(movable.isLocked())
                                      .openDir(movable.getOpenDir())
                                      .primeOwner(movable.getPrimeOwner()).build(), typeData));
        }
        catch (Exception t)
        {
            log.atSevere().withCause(t).log();
        }
        return Optional.empty();
    }

    @Override
    public boolean syncMovableData(IMovableConst movable, byte[] typeData)
    {
        return executeUpdate(SQLStatement.UPDATE_MOVABLE_BASE
                                 .constructPPreparedStatement()
                                 .setNextString(movable.getName())
                                 .setNextString(movable.getWorld().worldName())

                                 .setNextInt(movable.getCuboid().getMin().x())
                                 .setNextInt(movable.getCuboid().getMin().y())
                                 .setNextInt(movable.getCuboid().getMin().z())

                                 .setNextInt(movable.getCuboid().getMax().x())
                                 .setNextInt(movable.getCuboid().getMax().y())
                                 .setNextInt(movable.getCuboid().getMax().z())

                                 .setNextInt(movable.getRotationPoint().x())
                                 .setNextInt(movable.getRotationPoint().y())
                                 .setNextInt(movable.getRotationPoint().z())
                                 .setNextLong(Util.getChunkId(movable.getRotationPoint()))

                                 .setNextInt(movable.getPowerBlock().x())
                                 .setNextInt(movable.getPowerBlock().y())
                                 .setNextInt(movable.getPowerBlock().z())
                                 .setNextLong(Util.getChunkId(movable.getPowerBlock()))

                                 .setNextInt(RotateDirection.getValue(movable.getOpenDir()))
                                 .setNextLong(getFlag(movable.isOpen(), movable.isLocked()))
                                 .setNextBytes(typeData)

                                 .setNextLong(movable.getUid())) > 0;
    }

    @Override
    public List<DatabaseManager.MovableIdentifier> getPartialIdentifiers(
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

    private List<DatabaseManager.MovableIdentifier> collectIdentifiers(ResultSet resultSet)
        throws SQLException
    {
        final List<DatabaseManager.MovableIdentifier> ret = new ArrayList<>();

        while (resultSet.next())
            ret.add(new DatabaseManager.MovableIdentifier(resultSet.getLong("id"), resultSet.getString("name")));
        return ret;
    }

    private void insertOrIgnorePlayer(Connection conn, PPlayerData playerData)
    {
        executeUpdate(conn, SQLStatement.INSERT_OR_IGNORE_PLAYER_DATA.constructPPreparedStatement()
                                                                     .setNextString(playerData.getUUID().toString())
                                                                     .setNextString(playerData.getName())
                                                                     .setNextInt(playerData.getMovableSizeLimit())
                                                                     .setNextInt(playerData.getMovableCountLimit())
                                                                     .setNextLong(playerData.getPermissionsFlag()));
    }

    /**
     * Gets the ID player in the "players" table. If the player isn't in the database yet, they are added first.
     *
     * @param conn
     *     The connection to the database.
     * @param movableOwner
     *     The owner of the movable whose player ID to retrieve.
     * @return The database ID of the player.
     */
    private long getPlayerID(Connection conn, MovableOwner movableOwner)
    {
        insertOrIgnorePlayer(conn, movableOwner.pPlayerData());

        return executeQuery(conn, SQLStatement.GET_PLAYER_ID
                                .constructPPreparedStatement()
                                .setString(1, movableOwner.pPlayerData().getUUID().toString()),
                            rs -> rs.next() ? rs.getLong("id") : -1, -1L);
    }

    /**
     * Attempts to construct a subclass of {@link MovableBase} from a ResultSet containing all data pertaining the
     * {@link MovableBase} (as stored in the "movableBase" table), as well as the owner (name, UUID, permission) and the
     * typeTableName.
     *
     * @param movableBaseRS
     *     The {@link ResultSet} containing a row from the "movableBase" table as well as a row from the
     *     "MovableOwnerPlayer" table and "typeTableName" from the "MovableType" table.
     * @return An instance of a subclass of {@link MovableBase} if it could be created.
     */
    private Optional<AbstractMovable> getMovable(ResultSet movableBaseRS)
        throws Exception
    {
        // Make sure the ResultSet isn't empty.
        if (!movableBaseRS.isBeforeFirst())
            return Optional.empty();

        return constructMovable(movableBaseRS);
    }

    /**
     * Attempts to construct a list of subclasses of {@link MovableBase} from a ResultSet containing all data pertaining
     * to one or more {@link MovableBase}s (as stored in the "movableBase" table), as well as the owner (name, UUID,
     * permission) and the typeTableName.
     *
     * @param movableBaseRS
     *     The {@link ResultSet} containing one or more rows from the "movableBase" table as well as matching rows from
     *     the "MovableOwnerPlayer" table and "typeTableName" from the "MovableType" table.
     * @return An optional with a list of {@link MovableBase}s if any could be constructed. If none could be
     * constructed, an empty {@link Optional} is returned instead.
     */
    private List<AbstractMovable> getMovables(ResultSet movableBaseRS)
        throws Exception
    {
        // Make sure the ResultSet isn't empty.
        if (!movableBaseRS.isBeforeFirst())
            return Collections.emptyList();

        final List<AbstractMovable> movables = new ArrayList<>();
        while (movableBaseRS.next())
            constructMovable(movableBaseRS).ifPresent(movables::add);
        return movables;
    }

    @Override
    public Optional<AbstractMovable> getMovable(long movableUID)
    {
        return executeQuery(SQLStatement.GET_MOVABLE_BASE_FROM_ID.constructPPreparedStatement()
                                                                 .setLong(1, movableUID),
                            this::getMovable, Optional.empty());
    }

    @Override
    public Optional<AbstractMovable> getMovable(UUID playerUUID, long movableUID)
    {
        return executeQuery(SQLStatement.GET_MOVABLE_BASE_FROM_ID_FOR_PLAYER.constructPPreparedStatement()
                                                                            .setLong(1, movableUID)
                                                                            .setString(2, playerUUID.toString()),
                            this::getMovable, Optional.empty());
    }

    @Override
    public boolean removeMovable(long movableUID)
    {
        return executeUpdate(SQLStatement.DELETE_MOVABLE.constructPPreparedStatement()
                                                        .setLong(1, movableUID)) > 0;
    }

    @Override
    public boolean removeMovables(UUID playerUUID, String movableName)
    {
        return executeUpdate(SQLStatement.DELETE_NAMED_MOVABLE_OF_PLAYER.constructPPreparedStatement()
                                                                        .setString(1, playerUUID.toString())
                                                                        .setString(2, movableName)) > 0;
    }

    @Override
    public boolean isBigDoorsWorld(String worldName)
    {
        return executeQuery(SQLStatement.IS_BIGDOORS_WORLD.constructPPreparedStatement()
                                                          .setString(1, worldName),
                            ResultSet::next, false);
    }

    @Override
    public int getMovableCountForPlayer(UUID playerUUID)
    {
        return executeQuery(SQLStatement.GET_MOVABLE_COUNT_FOR_PLAYER.constructPPreparedStatement()
                                                                     .setString(1, playerUUID.toString()),
                            resultSet -> resultSet.next() ? resultSet.getInt("total") : -1, -1);
    }

    @Override
    public int getMovableCountForPlayer(UUID playerUUID, String movableName)
    {
        return executeQuery(SQLStatement.GET_PLAYER_MOVABLE_COUNT.constructPPreparedStatement()
                                                                 .setString(1, playerUUID.toString())
                                                                 .setString(2, movableName),
                            resultSet -> resultSet.next() ? resultSet.getInt("total") : -1, -1);
    }

    @Override
    public int getMovableCountByName(String movableName)
    {
        return executeQuery(SQLStatement.GET_MOVABLE_COUNT_BY_NAME.constructPPreparedStatement()
                                                                  .setString(1, movableName),
                            resultSet -> resultSet.next() ? resultSet.getInt("total") : -1, -1);
    }

    @Override
    public int getOwnerCountOfMovable(long movableUID)
    {
        return executeQuery(SQLStatement.GET_OWNER_COUNT_OF_MOVABLE.constructPPreparedStatement()
                                                                   .setLong(1, movableUID),
                            resultSet -> resultSet.next() ? resultSet.getInt("total") : -1, -1);
    }

    @Override
    public List<AbstractMovable> getMovables(UUID playerUUID, String movableName, PermissionLevel maxPermission)
    {
        return executeQuery(SQLStatement.GET_NAMED_MOVABLES_OWNED_BY_PLAYER.constructPPreparedStatement()
                                                                           .setString(1, playerUUID.toString())
                                                                           .setString(2, movableName)
                                                                           .setInt(3, maxPermission.getValue()),
                            this::getMovables, Collections.emptyList());
    }

    @Override
    public List<AbstractMovable> getMovables(UUID playerUUID, String name)
    {
        return getMovables(playerUUID, name, PermissionLevel.CREATOR);
    }

    @Override
    public List<AbstractMovable> getMovables(String name)
    {
        return executeQuery(SQLStatement.GET_MOVABLES_WITH_NAME.constructPPreparedStatement()
                                                               .setString(1, name),
                            this::getMovables, Collections.emptyList());
    }

    @Override
    public List<AbstractMovable> getMovables(UUID playerUUID, PermissionLevel maxPermission)
    {
        return executeQuery(SQLStatement.GET_MOVABLES_OWNED_BY_PLAYER_WITH_LEVEL.constructPPreparedStatement()
                                                                                .setString(1, playerUUID.toString())
                                                                                .setInt(2, maxPermission.getValue()),
                            this::getMovables, Collections.emptyList());
    }

    @Override
    public List<AbstractMovable> getMovables(UUID playerUUID)
    {
        return getMovables(playerUUID, PermissionLevel.CREATOR);
    }

    @Override
    public boolean updatePlayerData(PPlayerData playerData)
    {
        return executeUpdate(SQLStatement.UPDATE_PLAYER_DATA.constructPPreparedStatement()
                                                            .setNextString(playerData.getName())
                                                            .setNextInt(playerData.getMovableSizeLimit())
                                                            .setNextInt(playerData.getMovableCountLimit())
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
                                final ConcurrentHashMap<Integer, List<Long>> movables = new ConcurrentHashMap<>();
                                while (resultSet.next())
                                {
                                    final int locationHash =
                                        Util.simpleChunkSpaceLocationhash(resultSet.getInt("powerBlockX"),
                                                                          resultSet.getInt("powerBlockY"),
                                                                          resultSet.getInt("powerBlockZ"));
                                    if (!movables.containsKey(locationHash))
                                        movables.put(locationHash, new ArrayList<>());
                                    movables.get(locationHash).add(resultSet.getLong("id"));
                                }
                                return movables;
                            }, new ConcurrentHashMap<>());
    }

    @Override
    public List<AbstractMovable> getMovablesInChunk(long chunkId)
    {
        return executeQuery(SQLStatement.GET_MOVABLES_IN_CHUNK.constructPPreparedStatement()
                                                              .setLong(1, chunkId),
                            this::getMovables, new ArrayList<>(0));
    }

    @Override
    public boolean removeOwner(long movableUID, UUID playerUUID)
    {
        return executeUpdate(SQLStatement.REMOVE_MOVABLE_OWNER.constructPPreparedStatement()
                                                              .setString(1, playerUUID.toString())
                                                              .setLong(2, movableUID)) > 0;
    }

    private Map<UUID, MovableOwner> getOwnersOfMovable(long movableUID)
    {
        return executeQuery(SQLStatement.GET_MOVABLE_OWNERS.constructPPreparedStatement()
                                                           .setLong(1, movableUID),
                            resultSet ->
                            {
                                final Map<UUID, MovableOwner> ret = new HashMap<>();
                                while (resultSet.next())
                                {
                                    final UUID uuid = UUID.fromString(resultSet.getString("playerUUID"));
                                    final PPlayerData playerData =
                                        new PPlayerData(uuid,
                                                        resultSet.getString("playerName"),
                                                        resultSet.getInt("sizeLimit"),
                                                        resultSet.getInt("countLimit"),
                                                        resultSet.getLong("permissions"));

                                    ret.put(uuid, new MovableOwner(
                                        resultSet.getLong("movableUID"),
                                        Objects.requireNonNull(
                                            PermissionLevel.fromValue(resultSet.getInt("permission"))),
                                        playerData));
                                }
                                return ret;
                            }, new HashMap<>(0));
    }

    @Override
    public boolean addOwner(long movableUID, PPlayerData player, PermissionLevel permission)
    {
        // permission level 0 is reserved for the creator, and negative values are not allowed.
        if (permission.getValue() < 1 || permission == PermissionLevel.NO_PERMISSION)
        {
            log.atInfo().withStackTrace(StackSize.FULL)
               .log("Cannot add co-owner with permission level %d", permission.getValue());
            return false;
        }

        return executeTransaction(
            conn ->
            {
                final long playerID = getPlayerID(conn,
                                                  new MovableOwner(movableUID, permission, player.getPPlayerData()));

                if (playerID == -1)
                    throw new IllegalArgumentException(
                        "Trying to add player \"" + player.getUUID() + "\" as owner of movable " + movableUID +
                            ", but that player is not registered in the database! Aborting...");

                return executeQuery(
                    conn, SQLStatement.GET_MOVABLE_OWNER_PLAYER.constructPPreparedStatement()
                                                               .setLong(1, playerID)
                                                               .setLong(2, movableUID),
                    rs ->
                    {
                        final SQLStatement statement =
                            (rs.next() && (rs.getInt("permission") != permission.getValue())) ?
                            SQLStatement.UPDATE_MOVABLE_OWNER_PERMISSION :
                            SQLStatement.INSERT_MOVABLE_OWNER;

                        return
                            executeUpdate(conn, statement
                                .constructPPreparedStatement()
                                .setInt(1, permission.getValue())
                                .setLong(2, playerID)
                                .setLong(3, movableUID)) > 0;
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
            log.atSevere().log("Failed to obtain database version!");
            databaseState = DatabaseState.ERROR;
            return dbVersion;
        }

        if (dbVersion > DATABASE_VERSION)
        {
            log.atSevere()
               .log("Trying to load database version %s while the maximum allowed version is %s",
                    dbVersion, DATABASE_VERSION);
            databaseState = DatabaseState.TOO_NEW;
        }
        else if (dbVersion < MIN_DATABASE_VERSION)
        {
            log.atSevere()
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
                log.atSevere().withStackTrace(StackSize.FULL)
                   .log("Failed to upgrade database: Connection unavailable!");
                databaseState = DatabaseState.ERROR;
                return;
            }

            final int dbVersion = verifyDatabaseVersion(conn);
            log.atFine().log("Upgrading database from version %d to version %d.", dbVersion, DATABASE_VERSION);

            if (!makeBackup())
                return;

            if (dbVersion < 11)
                throw new IllegalStateException("Database version " + dbVersion + " is not supported!");

            updateDBVersion(conn);
            databaseState = DatabaseState.OK;
        }
        catch (Exception e)
        {
            log.atSevere().withCause(e).log("Failed to upgrade database!");
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
            log.atSevere().withCause(e)
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
            log.atSevere().withCause(e).log();
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
            log.atSevere().withCause(e).log("Failed to execute update: %s", pPreparedStatement);
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
            log.atSevere().withCause(e).log("Failed to execute update: %s", pPreparedStatement);
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
            log.atSevere().withCause(e).log("Failed to execute update: %s", pPreparedStatement);
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
                log.atSevere().withCause(ex)
                   .log("Failed to get generated key for statement: %s", pPreparedStatement);
            }
        }
        catch (SQLException e)
        {
            log.atSevere().withCause(e).log("Failed to execute update: %s", pPreparedStatement);
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
            log.atSevere().withCause(e).log("Failed to execute query: %s", pPreparedStatement);
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
            log.atSevere().withCause(e).log("Failed to execute batch query: %s", pPreparedStatement);
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
            log.atSevere().withCause(e).log("Failed to execute query: %s", pPreparedStatement);
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
                log.atSevere().withCause(e).log();
            }
        }
        catch (Exception e)
        {
            log.atSevere().withCause(e).log();
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
        log.atFinest().log("Executed statement: %s", pPreparedStatement);
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
