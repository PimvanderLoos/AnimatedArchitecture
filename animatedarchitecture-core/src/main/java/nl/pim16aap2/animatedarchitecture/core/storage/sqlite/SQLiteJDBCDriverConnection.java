package nl.pim16aap2.animatedarchitecture.core.storage.sqlite;

import com.google.common.flogger.StackSize;
import it.unimi.dsi.fastutil.ints.Int2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;
import lombok.Getter;
import lombok.Locked;
import lombok.extern.flogger.Flogger;
import nl.pim16aap2.animatedarchitecture.core.api.IPlayer;
import nl.pim16aap2.animatedarchitecture.core.api.IWorld;
import nl.pim16aap2.animatedarchitecture.core.api.LimitContainer;
import nl.pim16aap2.animatedarchitecture.core.api.PlayerData;
import nl.pim16aap2.animatedarchitecture.core.api.debugging.DebuggableRegistry;
import nl.pim16aap2.animatedarchitecture.core.api.debugging.IDebuggable;
import nl.pim16aap2.animatedarchitecture.core.api.factories.IWorldFactory;
import nl.pim16aap2.animatedarchitecture.core.managers.DatabaseManager;
import nl.pim16aap2.animatedarchitecture.core.managers.StructureTypeManager;
import nl.pim16aap2.animatedarchitecture.core.storage.DelayedPreparedStatement;
import nl.pim16aap2.animatedarchitecture.core.storage.FlywayManager;
import nl.pim16aap2.animatedarchitecture.core.storage.IDataSourceInfo;
import nl.pim16aap2.animatedarchitecture.core.storage.IStorage;
import nl.pim16aap2.animatedarchitecture.core.storage.SQLStatement;
import nl.pim16aap2.animatedarchitecture.core.structures.IStructureConst;
import nl.pim16aap2.animatedarchitecture.core.structures.PermissionLevel;
import nl.pim16aap2.animatedarchitecture.core.structures.Structure;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureBuilder;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureOwner;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureRegistry;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureType;
import nl.pim16aap2.animatedarchitecture.core.structures.properties.Property;
import nl.pim16aap2.animatedarchitecture.core.structures.properties.PropertyContainer;
import nl.pim16aap2.animatedarchitecture.core.structures.properties.PropertyContainerSerializer;
import nl.pim16aap2.animatedarchitecture.core.util.Cuboid;
import nl.pim16aap2.animatedarchitecture.core.util.IBitFlag;
import nl.pim16aap2.animatedarchitecture.core.util.Limit;
import nl.pim16aap2.animatedarchitecture.core.util.LocationUtil;
import nl.pim16aap2.animatedarchitecture.core.util.MathUtil;
import nl.pim16aap2.animatedarchitecture.core.util.MovementDirection;
import nl.pim16aap2.animatedarchitecture.core.util.functional.CheckedFunction;
import nl.pim16aap2.animatedarchitecture.core.util.vector.Vector3Di;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;
import org.sqlite.JDBC;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * An implementation of {@link IStorage} for SQLite.
 */
@Singleton
@Flogger
public final class SQLiteJDBCDriverConnection implements IStorage, IDebuggable
{
    /**
     * A fake UUID that cannot exist normally. To be used for storing transient data across server restarts.
     */
    @SuppressWarnings("unused")
    private static final String FAKE_UUID = "0000";

    @SuppressWarnings("unused") // Use by lombok.
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock(true);

    /**
     * The database file.
     */
    private final IDataSourceInfo dataSourceInfo;

    /**
     * The {@link DatabaseState} the database is in.
     */
    @Getter
    private volatile DatabaseState databaseState;

    private final StructureBuilder structureBuilder;

    private final StructureRegistry structureRegistry;

    private final StructureTypeManager structureTypeManager;

    private final IWorldFactory worldFactory;

    @Inject
    public SQLiteJDBCDriverConnection(
        DataSourceInfoSQLite dataSourceInfo,
        FlywayManager flywayManager,
        StructureBuilder structureBuilder,
        StructureRegistry structureRegistry,
        StructureTypeManager structureTypeManager,
        IWorldFactory worldFactory,
        DebuggableRegistry debuggableRegistry)
    {
        this.dataSourceInfo = dataSourceInfo;
        this.structureBuilder = structureBuilder;
        this.structureRegistry = structureRegistry;
        this.structureTypeManager = structureTypeManager;
        this.worldFactory = worldFactory;

        try
        {
            if (!loadDriver())
            {
                log.atWarning().log("Failed to load database driver!");
                databaseState = DatabaseState.NO_DRIVER;
                return;
            }

            try
            {
                flywayManager.migrate();
                databaseState = DatabaseState.OK;
            }
            catch (Exception e)
            {
                log.atSevere().withCause(e).log("Failed to initialize database!");
                databaseState = DatabaseState.ERROR;
            }

            log.atFine().log("Database initialized! Current state: %s", databaseState);
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
        final String driver = "org.sqlite.JDBC";
        try
        {
            Class.forName(driver);
            return true;
        }
        catch (ClassNotFoundException e)
        {
            log.atSevere().withCause(e).log("Failed to load database driver: %s!", driver);
        }
        return false;
    }

    /**
     * Opens a connection to the SQLite database.
     * <p>
     * The connection is opened with foreign keys enabled.
     *
     * @return The connection to the SQLite database.
     *
     * @throws SQLException
     *     If an error occurs while opening the connection.
     * @throws IllegalArgumentException
     *     If the path does not result in a valid URL. See {@link JDBC#isValidURL(String)}.
     * @throws NullPointerException
     *     If the connection could not be opened and somehow ended up being null.
     */
    private Connection openConnection()
        throws SQLException
    {
        return Objects.requireNonNull(
            dataSourceInfo.getDataSource().getConnection(),
            "Failed to open connection to SQLite database with data source: '" + dataSourceInfo + "'"
        );
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
            log.atSevere().withStackTrace(StackSize.FULL).log(
                "Database connection could not be created! " +
                    "Requested database for state '%s' while it is actually in state '%s'!",
                state.name(),
                databaseState.name()
            );
            return null;
        }

        try
        {
            return openConnection();
        }
        catch (SQLException e)
        {
            throw new RuntimeException("Failed to open database connection!", e);
        }
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

    private Optional<Structure> constructStructure(ResultSet structureRS)
        throws Exception
    {
        final @Nullable String structureTypeResult = structureRS.getString("type");
        final Optional<StructureType> structureTypeOpt = structureTypeManager.getFromFullName(structureTypeResult);

        if (!structureTypeOpt.map(structureTypeManager::isRegistered).orElse(false))
        {
            log.atSevere().withStackTrace(StackSize.FULL).log(
                "Type with ID: '%s' has not been registered (yet)!", structureTypeResult);
            return Optional.empty();
        }

        final long structureUID = structureRS.getLong("id");

        final Optional<Structure> registeredStructure = structureRegistry.getRegisteredStructure(structureUID);
        if (registeredStructure.isPresent())
            return registeredStructure;

        final StructureType structureType = structureTypeOpt.orElseThrow();

        final Optional<MovementDirection> animationDirection =
            Optional.ofNullable(MovementDirection.valueOf(structureRS.getInt("animationDirection")));

        if (animationDirection.isEmpty())
            return Optional.empty();

        final Vector3Di min = new Vector3Di(
            structureRS.getInt("xMin"),
            structureRS.getInt("yMin"),
            structureRS.getInt("zMin")
        );
        final Vector3Di max = new Vector3Di(
            structureRS.getInt("xMax"),
            structureRS.getInt("yMax"),
            structureRS.getInt("zMax")
        );
        final Vector3Di powerBlock = new Vector3Di(
            structureRS.getInt("powerBlockX"),
            structureRS.getInt("powerBlockY"),
            structureRS.getInt("powerBlockZ")
        );

        final IWorld world = worldFactory.create(structureRS.getString("world"));

        final long bitflag = structureRS.getLong("bitflag");
        final boolean isLocked = IBitFlag.hasFlag(StructureFlag.getFlagValue(StructureFlag.IS_LOCKED), bitflag);

        final String name = structureRS.getString("name");

        final PlayerData playerData = new PlayerData(
            UUID.fromString(structureRS.getString("playerUUID")),
            structureRS.getString("playerName"),
            new LimitContainer(
                getOptionalInt(structureRS, "limitStructureSize"),
                getOptionalInt(structureRS, "limitStructureCount"),
                getOptionalInt(structureRS, "limitPowerBlockDistance"),
                getOptionalInt(structureRS, "limitBlocksToMove")),
            structureRS.getLong("permissions")
        );

        final StructureOwner primeOwner = new StructureOwner(
            structureUID,
            Objects.requireNonNull(PermissionLevel.fromValue(structureRS.getInt("permission"))),
            playerData
        );

        // TODO: Use the type version to upgrade structures if necessary.
        // final int typeVersion = structureRS.getInt("typeVersion");

        final PropertyContainer properties;
        try
        {
            final String rawProperties = structureRS.getString("properties");
            properties = PropertyContainerSerializer.deserialize(structureType, rawProperties);
        }
        catch (Exception exception)
        {
            throw new RuntimeException(
                String.format(
                    "Failed to deserialize properties for structure with UID: '%d' of type: '%s'",
                    structureUID,
                    structureType.getFullKey()),
                exception
            );
        }

        final Structure structure;
        try
        {
            final Map<UUID, StructureOwner> ownersOfStructure = getOwnersOfStructure(structureUID);
            structure =
                structureBuilder
                    .builder(structureType)
                    .uid(AssignedUIDSqlite.getAssignedUID(structureUID))
                    .name(name)
                    .cuboid(new Cuboid(min, max))
                    .powerBlock(powerBlock)
                    .world(world)
                    .isLocked(isLocked)
                    .openDir(animationDirection.get())
                    .primeOwner(primeOwner)
                    .ownersOfStructure(ownersOfStructure)
                    .propertiesOfStructure(properties)
                    .build();
        }
        catch (Exception e)
        {
            throw new RuntimeException(String.format("""
                    Failed to construct structure:
                    - UID:            %d
                    - Structure Type: %s
                    - Properties:     %s
                    """,
                structureUID,
                structureType.getFullKey(),
                properties
            ), e);
        }

        final Optional<Structure> registered = structureRegistry.putIfAbsent(structure);
        // If the UID was not already registered, we return the now-registered structure we just created.
        if (registered.isEmpty())
            return Optional.of(structure);

        // If the UID was registered since last we checked, we discard the new structure and return the registered one.
        return registered;
    }

    @Override
    @Locked.Write("lock")
    public boolean deleteStructureType(StructureType structureType)
    {
        final boolean removed = executeTransaction(
            conn -> executeUpdate(
                SQLStatement.DELETE_STRUCTURE_TYPE
                    .constructDelayedPreparedStatement()
                    .setNextString(structureType.getFullKey())) > 0,
            false
        );

        if (removed)
            structureTypeManager.setEnabledState(structureType, false);
        return removed;
    }

    @Locked.Write("lock")
    private Long insert(
        Connection conn,
        Structure structure,
        StructureType structureType,
        String propertiesData)
    {
        final PlayerData playerData = structure.getPrimeOwner().playerData();
        insertOrIgnorePlayer(conn, playerData);

        final String worldName = structure.getWorld().worldName();

        final long structureUID = executeQuery(
            conn,
            SQLStatement.INSERT_STRUCTURE_BASE
                .constructDelayedPreparedStatement()
                .setNextString(structure.getName())
                .setNextString(worldName)
                .setNextInt(structure.getMinimum().x())
                .setNextInt(structure.getMinimum().y())
                .setNextInt(structure.getMinimum().z())
                .setNextInt(structure.getMaximum().x())
                .setNextInt(structure.getMaximum().y())
                .setNextInt(structure.getMaximum().z())
                .setNextLong(LocationUtil.getChunkId(structure.getCuboid().getCenterBlock()))
                .setNextInt(structure.getPowerBlock().x())
                .setNextInt(structure.getPowerBlock().y())
                .setNextInt(structure.getPowerBlock().z())
                .setNextLong(LocationUtil.getChunkId(structure.getPowerBlock()))
                .setNextInt(MovementDirection.getValue(structure.getOpenDirection()))
                .setNextLong(getFlag(structure))
                .setNextString(structureType.getFullKey())
                .setNextInt(structureType.getVersion())
                .setNextString(propertiesData),
            resultSet ->
            {
                if (resultSet.next())
                    return resultSet.getLong(1);
                log.atSevere().log("Failed to retrieve structure ID while inserting structure: %s", structure);
                return -1L;
            },
            -1L
        );

        if (structureUID < 0)
        {
            log.atSevere().log("Failed to insert structure: %s", structure);
            return -1L;
        }

        executeUpdate(
            conn,
            SQLStatement.INSERT_PRIME_OWNER
                .constructDelayedPreparedStatement()
                .setString(1, playerData.getUUID().toString())
                .setLong(2, structureUID)
        );

        return structureUID;
    }

    @Override
    @Locked.Write("lock")
    public Optional<Structure> insert(Structure structure)
    {
        try
        {
            final String properties = PropertyContainerSerializer.serialize(structure);

            final long structureUID = executeTransaction(
                conn -> insert(conn, structure, structure.getType(), properties),
                -1L
            );

            if (structureUID > 0)
            {
                final Structure recreated = structureBuilder
                    .builder(structure.getType())
                    .uid(AssignedUIDSqlite.getAssignedUID(structureUID))
                    .name(structure.getName())
                    .cuboid(structure.getCuboid())
                    .powerBlock(structure.getPowerBlock())
                    .world(structure.getWorld())
                    .isLocked(structure.isLocked())
                    .openDir(structure.getOpenDirection())
                    .primeOwner(remapStructureOwner(structure.getPrimeOwner(), structureUID))
                    .ownersOfStructure(remapStructureOwners(structure.getOwners(), structureUID))
                    .propertiesOfStructure(structure.getPropertyContainerSnapshot())
                    .build();

                final Optional<Structure> registered = structureRegistry.putIfAbsent(recreated);
                if (registered.isEmpty())
                    return Optional.of(recreated);

                throw new IllegalStateException(String.format(
                    """
                        Structure with UID: '%d' was already registered while creating a new structure!
                        Registered structure: %s
                        New structure:        %s
                        """,
                    structureUID,
                    registered,
                    recreated
                ));
            }
        }
        catch (Exception t)
        {
            log.atSevere().withCause(t).log("Failed to insert structure: %s", structure);
        }
        log.atSevere().withStackTrace(StackSize.FULL).log("Failed to insert structure: %s", structure);
        return Optional.empty();
    }

    /**
     * Remaps a collection of {@link StructureOwner}s to a new structure UID.
     *
     * @param current
     *     The current owners.
     * @param newUid
     *     The new UID.
     * @return A map of the new structure owners with their UUID as key.
     */
    static Map<UUID, StructureOwner> remapStructureOwners(Collection<StructureOwner> current, long newUid)
    {
        return current
            .stream()
            .map(owner -> remapStructureOwner(owner, newUid))
            .collect(Collectors.toMap(owner1 -> owner1.playerData().getUUID(), Function.identity()));
    }

    /**
     * Remaps a {@link StructureOwner} to a new structure UID.
     * <p>
     * All other data is copied as-is.
     *
     * @param current
     *     The current owner.
     * @param newUid
     *     The new UID.
     * @return The new structure owner with the new UID.
     */
    static StructureOwner remapStructureOwner(StructureOwner current, long newUid)
    {
        return new StructureOwner(newUid, current.permission(), current.playerData());
    }

    @Override
    @Locked.Write("lock")
    public boolean syncStructureData(IStructureConst structure)
    {
        final String serializedProperties = PropertyContainerSerializer.serialize(structure);

        return executeUpdate(SQLStatement.UPDATE_STRUCTURE_BASE
            .constructDelayedPreparedStatement()
            .setNextString(structure.getName())
            .setNextString(structure.getWorld().worldName())

            .setNextInt(structure.getCuboid().getMin().x())
            .setNextInt(structure.getCuboid().getMin().y())
            .setNextInt(structure.getCuboid().getMin().z())

            .setNextInt(structure.getCuboid().getMax().x())
            .setNextInt(structure.getCuboid().getMax().y())
            .setNextInt(structure.getCuboid().getMax().z())

            .setNextLong(LocationUtil.getChunkId(structure.getCuboid().getCenterBlock()))

            .setNextInt(structure.getPowerBlock().x())
            .setNextInt(structure.getPowerBlock().y())
            .setNextInt(structure.getPowerBlock().z())
            .setNextLong(LocationUtil.getChunkId(structure.getPowerBlock()))

            .setNextInt(MovementDirection.getValue(structure.getOpenDirection()))
            .setNextLong(getFlag(structure))
            .setNextInt(structure.getType().getVersion())
            .setNextString(serializedProperties)

            .setNextLong(structure.getUid())) > 0;
    }

    @Override
    @Locked.Read("lock")
    public List<DatabaseManager.StructureIdentifier> getPartialIdentifiers(
        String input,
        @Nullable IPlayer player,
        PermissionLevel maxPermission,
        Collection<Property<?>> properties
    )
    {
        final DelayedPreparedStatement query;
        if (MathUtil.isNumerical(input))
            query = SQLStatement.GET_IDENTIFIERS_FROM_PARTIAL_UID_MATCH_WITH_OWNER
                .constructDelayedPreparedStatement()
                .setNextLong(Long.parseLong(input));
        else
            query = SQLStatement.GET_IDENTIFIERS_FROM_PARTIAL_NAME_MATCH_WITH_OWNER
                .constructDelayedPreparedStatement()
                .setNextString(input);

        query.setNextInt(maxPermission.getValue());

        final @Nullable String uuid = player == null ? null : player.getUUID().toString();
        query.setNextString(uuid);
        query.setNextString(uuid);

        final var stringBuilder = new StringBuilder();
        for (final var property : properties)
            stringBuilder
                .append(" AND json_extract(properties, '$.")
                .append(property.getNamespacedKey().getFullKey())
                .append("') IS NOT NULL");

        query.setNextRawString(stringBuilder.toString());

        return executeQuery(query, this::collectIdentifiers, Collections.emptyList());
    }

    @Locked.Read("lock")
    private List<DatabaseManager.StructureIdentifier> collectIdentifiers(ResultSet resultSet)
        throws SQLException
    {
        final List<DatabaseManager.StructureIdentifier> ret = new ArrayList<>();

        while (resultSet.next())
        {
            final @Nullable String structureTypeResult = resultSet.getString("type");
            final Optional<StructureType> structureType = structureTypeManager.getFromFullName(structureTypeResult);

            if (!structureType.map(structureTypeManager::isRegistered).orElse(false))
            {
                log.atSevere().withStackTrace(StackSize.FULL).log(
                    "Type with ID: '%s' has not been registered (yet)!", structureTypeResult);
                continue;
            }

            ret.add(new DatabaseManager.StructureIdentifier(
                structureType.get(),
                resultSet.getLong("id"),
                resultSet.getString("name")
            ));
        }

        return ret;
    }

    @Locked.Write("lock")
    private void insertOrIgnorePlayer(Connection conn, PlayerData playerData)
    {
        executeUpdate(
            conn,
            SQLStatement.INSERT_OR_IGNORE_PLAYER_DATA
                .constructDelayedPreparedStatement()
                .setNextString(playerData.getUUID().toString())
                .setNextString(playerData.getName())
                .setNextObject(optionalIntToObject(playerData.getLimit(Limit.STRUCTURE_SIZE)), Types.INTEGER)
                .setNextObject(optionalIntToObject(playerData.getLimit(Limit.STRUCTURE_COUNT)), Types.INTEGER)
                .setNextObject(optionalIntToObject(playerData.getLimit(Limit.POWERBLOCK_DISTANCE)), Types.INTEGER)
                .setNextObject(optionalIntToObject(playerData.getLimit(Limit.BLOCKS_TO_MOVE)), Types.INTEGER)
                .setNextLong(playerData.getPermissionsFlag())
        );
    }

    /**
     * Gets the ID player in the "players" table. If the player isn't in the database yet, they are added first.
     *
     * @param conn
     *     The connection to the database.
     * @param structureOwner
     *     The owner of the structure whose player ID to retrieve.
     * @return The database ID of the player.
     */
    @Locked.Write("lock")
    private long getPlayerID(Connection conn, StructureOwner structureOwner)
    {
        insertOrIgnorePlayer(conn, structureOwner.playerData());

        return executeQuery(
            conn,
            SQLStatement.GET_PLAYER_ID
                .constructDelayedPreparedStatement()
                .setString(1, structureOwner.playerData().getUUID().toString()),
            rs -> rs.next() ? rs.getLong("id") : -1,
            -1L
        );
    }

    /**
     * Attempts to construct a subclass of {@link Structure} from a ResultSet containing all data pertaining the
     * {@link Structure} (as stored in the "Structure" table), as well as the owner (name, UUID, permission) and the
     * typeTableName.
     *
     * @param structureRS
     *     The {@link ResultSet} containing a row from the "Structure" table as well as a row from the
     *     "StructureOwnerPlayer" table.
     * @return An instance of a subclass of {@link Structure} if it could be created.
     */
    @Locked.Read("lock")
    private Optional<Structure> getStructure(ResultSet structureRS)
        throws Exception
    {
        // Make sure the ResultSet isn't empty.
        if (!structureRS.isBeforeFirst())
            return Optional.empty();

        return constructStructure(structureRS);
    }

    /**
     * Attempts to construct a list of subclasses of {@link Structure} from a ResultSet containing all data pertaining
     * to one or more {@link Structure}s (as stored in the "Structure" table), as well as the owner (name, UUID,
     * permission) and the typeTableName.
     *
     * @param structureRS
     *     The {@link ResultSet} containing one or more rows from the "Structure" table as well as matching rows from
     *     the "StructureOwnerPlayer" table.
     * @return An optional with a list of {@link Structure}s if any could be constructed. If none could be constructed,
     * an empty {@link Optional} is returned instead.
     */
    private List<Structure> getStructures(ResultSet structureRS)
        throws Exception
    {
        // Make sure the ResultSet isn't empty.
        if (!structureRS.isBeforeFirst())
            return List.of();

        final List<Structure> structures = new ArrayList<>();

        while (structureRS.next())
            constructStructure(structureRS).ifPresent(structures::add);

        return structures;
    }

    @Override
    @Locked.Read("lock")
    public Optional<Structure> getStructure(long structureUID)
    {
        return executeQuery(
            SQLStatement.GET_STRUCTURE_BASE_FROM_ID
                .constructDelayedPreparedStatement()
                .setLong(1, structureUID),
            this::getStructure,
            Optional.empty()
        );
    }

    @Override
    @Locked.Read("lock")
    public Optional<Structure> getStructure(UUID playerUUID, long structureUID)
    {
        return executeQuery(
            SQLStatement.GET_STRUCTURE_BASE_FROM_ID_FOR_PLAYER
                .constructDelayedPreparedStatement()
                .setLong(1, structureUID)
                .setString(2, playerUUID.toString()),
            this::getStructure,
            Optional.empty()
        );
    }

    @Override
    @Locked.Write("lock")
    public boolean removeStructure(long structureUID)
    {
        return executeUpdate(SQLStatement.DELETE_STRUCTURE
            .constructDelayedPreparedStatement()
            .setLong(1, structureUID)) > 0;
    }

    @Override
    @Locked.Write("lock")
    public boolean removeStructures(UUID playerUUID, String structureName)
    {
        return executeUpdate(SQLStatement.DELETE_NAMED_STRUCTURE_OF_PLAYER
            .constructDelayedPreparedStatement()
            .setString(1, playerUUID.toString())
            .setString(2, structureName)) > 0;
    }

    @Override
    @Locked.Read("lock")
    public boolean isAnimatedArchitectureWorld(String worldName)
    {
        return executeQuery(
            SQLStatement.IS_ANIMATE_ARCHITECTURE_WORLD
                .constructDelayedPreparedStatement()
                .setString(1, worldName),
            ResultSet::next,
            false
        );
    }

    @Override
    @Locked.Read("lock")
    public int getStructureCountForPlayer(UUID playerUUID)
    {
        return executeQuery(
            SQLStatement.GET_STRUCTURE_COUNT_FOR_PLAYER
                .constructDelayedPreparedStatement()
                .setString(1, playerUUID.toString()),
            resultSet -> resultSet.next() ? resultSet.getInt("total") : -1,
            -1
        );
    }

    @Override
    @Locked.Read("lock")
    public int getStructureCountForPlayer(UUID playerUUID, String structureName)
    {
        return executeQuery(
            SQLStatement.GET_PLAYER_STRUCTURE_COUNT
                .constructDelayedPreparedStatement()
                .setString(1, playerUUID.toString())
                .setString(2, structureName),
            resultSet -> resultSet.next() ? resultSet.getInt("total") : -1,
            -1
        );
    }

    @Override
    @Locked.Read("lock")
    public int getStructureCountByName(String structureName)
    {
        return executeQuery(
            SQLStatement.GET_STRUCTURE_COUNT_BY_NAME
                .constructDelayedPreparedStatement()
                .setString(1, structureName),
            resultSet -> resultSet.next() ? resultSet.getInt("total") : -1,
            -1
        );
    }

    @Override
    @Locked.Read("lock")
    public int getOwnerCountOfStructure(long structureUID)
    {
        return executeQuery(
            SQLStatement.GET_OWNER_COUNT_OF_STRUCTURE
                .constructDelayedPreparedStatement()
                .setLong(1, structureUID),
            resultSet -> resultSet.next() ? resultSet.getInt("total") : -1,
            -1
        );
    }

    @Override
    @Locked.Read("lock")
    public List<Structure> getStructures(UUID playerUUID, String structureName, PermissionLevel maxPermission)
    {
        return executeQuery(
            SQLStatement.GET_NAMED_STRUCTURES_OWNED_BY_PLAYER
                .constructDelayedPreparedStatement()
                .setString(1, playerUUID.toString())
                .setString(2, structureName)
                .setInt(3, maxPermission.getValue()),
            this::getStructures,
            Collections.emptyList()
        );
    }

    @Override
    @Locked.Read("lock")
    public List<Structure> getStructures(UUID playerUUID, String name)
    {
        return getStructures(playerUUID, name, PermissionLevel.CREATOR);
    }

    @Override
    @Locked.Read("lock")
    public List<Structure> getStructures(String name)
    {
        return executeQuery(
            SQLStatement.GET_STRUCTURES_WITH_NAME
                .constructDelayedPreparedStatement()
                .setString(1, name),
            this::getStructures,
            Collections.emptyList()
        );
    }

    @Override
    @Locked.Read("lock")
    public List<Structure> getStructures(UUID playerUUID, PermissionLevel maxPermission)
    {
        return executeQuery(
            SQLStatement.GET_STRUCTURES_OWNED_BY_PLAYER_WITH_LEVEL
                .constructDelayedPreparedStatement()
                .setString(1, playerUUID.toString())
                .setInt(2, maxPermission.getValue()),
            this::getStructures,
            Collections.emptyList()
        );
    }

    @Override
    @Locked.Read("lock")
    public List<Structure> getStructures(UUID playerUUID)
    {
        return getStructures(playerUUID, PermissionLevel.CREATOR);
    }

    @Override
    @Locked.Read("lock")
    public List<Structure> getStructuresOfType(String typeName)
    {
        return executeQuery(
            SQLStatement.GET_STRUCTURES_OF_TYPE
                .constructDelayedPreparedStatement()
                .setNextString(typeName),
            this::getStructures,
            Collections.emptyList()
        );
    }

    @Override
    @Locked.Read("lock")
    public List<Structure> getStructuresOfType(String typeName, int version)
    {
        return executeQuery(
            SQLStatement.GET_STRUCTURES_OF_VERSIONED_TYPE
                .constructDelayedPreparedStatement()
                .setNextInt(version)
                .setNextString(typeName),
            this::getStructures,
            Collections.emptyList()
        );
    }

    @Override
    @Locked.Write("lock")
    public boolean updatePlayerData(PlayerData playerData)
    {
        return executeUpdate(SQLStatement.UPDATE_PLAYER_DATA
            .constructDelayedPreparedStatement()
            .setNextString(playerData.getName())
            .setNextObject(optionalIntToObject(playerData.getLimit(Limit.STRUCTURE_SIZE)), Types.INTEGER)
            .setNextObject(optionalIntToObject(playerData.getLimit(Limit.STRUCTURE_COUNT)), Types.INTEGER)
            .setNextObject(optionalIntToObject(playerData.getLimit(Limit.POWERBLOCK_DISTANCE)), Types.INTEGER)
            .setNextObject(optionalIntToObject(playerData.getLimit(Limit.BLOCKS_TO_MOVE)), Types.INTEGER)
            .setNextLong(playerData.getPermissionsFlag())
            .setNextString(playerData.getUUID().toString())) > 0;
    }

    @Override
    @Locked.Read("lock")
    public Optional<PlayerData> getPlayerData(UUID uuid)
    {
        return executeQuery(
            SQLStatement.GET_PLAYER_DATA
                .constructDelayedPreparedStatement()
                .setNextString(uuid.toString()),
            resultSet -> Optional.of(new PlayerData(
                uuid,
                resultSet.getString("playerName"),
                new LimitContainer(
                    getOptionalInt(resultSet, "limitStructureSize"),
                    getOptionalInt(resultSet, "limitStructureCount"),
                    getOptionalInt(resultSet, "limitPowerBlockDistance"),
                    getOptionalInt(resultSet, "limitBlocksToMove")),
                resultSet.getLong("permissions"))),
            Optional.empty()
        );
    }

    @Override
    @Locked.Read("lock")
    public List<PlayerData> getPlayerData(String playerName)
    {

        return executeQuery(
            SQLStatement.GET_PLAYER_DATA_FROM_NAME
                .constructDelayedPreparedStatement()
                .setNextString(playerName),
            (resultSet) ->
            {
                final List<PlayerData> playerData = new ArrayList<>();
                while (resultSet.next())
                    playerData.add(new PlayerData(
                        UUID.fromString(resultSet.getString("playerUUID")),
                        playerName,
                        new LimitContainer(
                            getOptionalInt(resultSet, "limitStructureSize"),
                            getOptionalInt(resultSet, "limitStructureCount"),
                            getOptionalInt(resultSet, "limitPowerBlockDistance"),
                            getOptionalInt(resultSet, "limitBlocksToMove")),
                        resultSet.getLong("permissions"))
                    );
                return playerData;
            },
            Collections.emptyList()
        );
    }

    @Override
    @Locked.Read("lock")
    public Int2ObjectMap<LongList> getPowerBlockData(long chunkId)
    {
        return executeQuery(
            SQLStatement.GET_POWER_BLOCK_DATA_IN_CHUNK
                .constructDelayedPreparedStatement()
                .setLong(1, chunkId),
            resultSet ->
            {
                final Int2ObjectMap<LongList> structures = new Int2ObjectLinkedOpenHashMap<>();
                while (resultSet.next())
                {
                    final int locationHash = LocationUtil.simpleChunkSpaceLocationHash(
                        resultSet.getInt("powerBlockX"),
                        resultSet.getInt("powerBlockY"),
                        resultSet.getInt("powerBlockZ")
                    );

                    if (!structures.containsKey(locationHash))
                        structures.put(locationHash, new LongArrayList());
                    structures.get(locationHash).add(resultSet.getLong("id"));
                }
                return Int2ObjectMaps.unmodifiable(structures);
            },
            Int2ObjectMaps.emptyMap()
        );
    }

    @Override
    @Locked.Read("lock")
    public List<Structure> getStructuresInChunk(long chunkId)
    {
        return executeQuery(
            SQLStatement.GET_STRUCTURES_IN_CHUNK
                .constructDelayedPreparedStatement()
                .setLong(1, chunkId),
            this::getStructures,
            new ArrayList<>(0)
        );
    }

    @Override
    @Locked.Write("lock")
    public boolean removeOwner(long structureUID, UUID playerUUID)
    {
        return executeUpdate(SQLStatement.REMOVE_STRUCTURE_OWNER
            .constructDelayedPreparedStatement()
            .setString(1, playerUUID.toString())
            .setLong(2, structureUID)) > 0;
    }

    @Locked.Read("lock")
    private Map<UUID, StructureOwner> getOwnersOfStructure(long structureUID)
    {
        return executeQuery(
            SQLStatement.GET_STRUCTURE_OWNERS
                .constructDelayedPreparedStatement()
                .setLong(1, structureUID),
            resultSet ->
            {
                final Map<UUID, StructureOwner> ret = new HashMap<>();
                while (resultSet.next())
                {
                    final UUID uuid = UUID.fromString(resultSet.getString("playerUUID"));
                    final PlayerData playerData = new PlayerData(
                        uuid,
                        resultSet.getString("playerName"),
                        new LimitContainer(
                            getOptionalInt(resultSet, "limitStructureSize"),
                            getOptionalInt(resultSet, "limitStructureCount"),
                            getOptionalInt(resultSet, "limitPowerBlockDistance"),
                            getOptionalInt(resultSet, "limitBlocksToMove")),
                        resultSet.getLong("permissions")
                    );


                    ret.put(
                        uuid,
                        new StructureOwner(
                            resultSet.getLong("structureUID"),
                            Objects.requireNonNull(PermissionLevel.fromValue(resultSet.getInt("permission"))),
                            playerData)
                    );
                }
                return ret;
            },
            new HashMap<>(0)
        );
    }

    @Override
    @Locked.Write("lock")
    public boolean addOwner(long structureUID, PlayerData player, PermissionLevel permission)
    {
        // permission level 0 is reserved for the creator, and negative values are not allowed.
        if (permission.getValue() < 1 || permission == PermissionLevel.NO_PERMISSION)
        {
            log.atInfo().withStackTrace(StackSize.FULL).log(
                "Cannot add co-owner with permission level %d", permission.getValue());
            return false;
        }

        return executeTransaction(
            conn ->
            {
                final long playerID = getPlayerID(
                    conn,
                    new StructureOwner(structureUID, permission, player)
                );

                if (playerID == -1)
                    throw new IllegalArgumentException(
                        "Trying to add player \"" + player.getUUID() + "\" as owner of structure " + structureUID +
                            ", but that player is not registered in the database! Aborting...");

                return executeQuery(
                    conn,
                    SQLStatement.GET_STRUCTURE_OWNER_PLAYER
                        .constructDelayedPreparedStatement()
                        .setLong(1, playerID)
                        .setLong(2, structureUID),
                    rs ->
                    {
                        final SQLStatement statement =
                            (rs.next() && (rs.getInt("permission") != permission.getValue())) ?
                                SQLStatement.UPDATE_STRUCTURE_OWNER_PERMISSION :
                                SQLStatement.INSERT_STRUCTURE_OWNER;

                        return executeUpdate(
                            conn,
                            statement
                                .constructDelayedPreparedStatement()
                                .setInt(1, permission.getValue())
                                .setLong(2, playerID)
                                .setLong(3, structureUID)) > 0;
                    },
                    false
                );
            },
            false
        );
    }

    /**
     * Executes an update defined by a {@link DelayedPreparedStatement}.
     *
     * @param delayedPreparedStatement
     *     The {@link DelayedPreparedStatement}.
     * @return Either the number of rows modified by the update, or -1 if an error occurred.
     */
    @Locked.Write("lock")
    private int executeUpdate(DelayedPreparedStatement delayedPreparedStatement)
    {
        try (@Nullable Connection conn = getConnection())
        {
            if (conn == null)
            {
                log.atSevere().withStackTrace(StackSize.FULL).log("Failed to execute update: Connection is null!");
                logStatement(delayedPreparedStatement);
                return -1;
            }
            return executeUpdate(conn, delayedPreparedStatement);
        }
        catch (Exception e)
        {
            log.atSevere().withCause(e).log("Failed to execute update: %s", delayedPreparedStatement);
        }
        return -1;
    }

    /**
     * Executes an update defined by a {@link DelayedPreparedStatement}.
     *
     * @param conn
     *     A connection to the database.
     * @param delayedPreparedStatement
     *     The {@link DelayedPreparedStatement}.
     * @return Either the number of rows modified by the update, or -1 if an error occurred.
     */
    @Locked.Write("lock")
    private int executeUpdate(Connection conn, DelayedPreparedStatement delayedPreparedStatement)
    {
        logStatement(delayedPreparedStatement);
        try (PreparedStatement ps = delayedPreparedStatement.construct(conn))
        {
            return ps.executeUpdate();
        }
        catch (SQLException e)
        {
            log.atSevere().withCause(e).log("Failed to execute update: %s", delayedPreparedStatement);
        }
        return -1;
    }

    /**
     * Executes a query defined by a {@link DelayedPreparedStatement} and applies a function to the result.
     *
     * @param query
     *     The {@link DelayedPreparedStatement}.
     * @param fun
     *     The function to apply to the {@link ResultSet}.
     * @param fallback
     *     The value to return in case the result is null or if an error occurred.
     * @param <T>
     *     The type of the result to return.
     * @return The {@link ResultSet} of the query, or null in case an error occurred.
     */
    @Locked.Read("lock")
    @Contract(" _, _, !null -> !null;")
    private @Nullable <T> T executeQuery(
        DelayedPreparedStatement query,
        CheckedFunction<ResultSet, T, Exception> fun,
        @Nullable T fallback)
    {
        try (@Nullable Connection conn = getConnection())
        {
            if (conn == null)
            {
                log.atSevere().withStackTrace(StackSize.FULL).log("Failed to execute query: Connection is null!");
                logStatement(query);
                return fallback;
            }
            return executeQuery(conn, query, fun, fallback);
        }
        catch (Exception e)
        {
            log.atSevere().withCause(e).log("Failed to execute query: %s", query);
        }
        return fallback;
    }

    /**
     * Executes a batched query defined by a {@link DelayedPreparedStatement} and applies a function to the result.
     *
     * @param query
     *     The {@link DelayedPreparedStatement}.
     * @param fun
     *     The function to apply to the {@link ResultSet}.
     * @param fallback
     *     The value to return in case the result is null or if an error occurred.
     * @param <T>
     *     The type of the result to return.
     * @return The {@link ResultSet} of the query, or null in case an error occurred.
     */
    @Locked.Read("lock")
    @SuppressWarnings("unused")
    @Contract(" _, _, !null -> !null;")
    private @Nullable <T> T executeBatchQuery(
        DelayedPreparedStatement query,
        CheckedFunction<ResultSet, T, Exception> fun,
        @Nullable T fallback)
    {
        try (@Nullable Connection conn = getConnection())
        {
            if (conn == null)
            {
                log.atSevere().withStackTrace(StackSize.FULL).log("Failed to execute query: Connection is null!");
                logStatement(query);
                return fallback;
            }
            conn.setAutoCommit(false);
            final @Nullable T result = executeQuery(conn, query, fun, fallback);
            conn.commit();
            conn.setAutoCommit(true);
            return result;
        }
        catch (Exception e)
        {
            log.atSevere().withCause(e).log("Failed to execute batch query: %s", query);
        }
        return fallback;
    }

    /**
     * Executes a query defined by a {@link DelayedPreparedStatement} and applies a function to the result.
     *
     * @param conn
     *     A connection to the database.
     * @param delayedPreparedStatement
     *     The {@link DelayedPreparedStatement}.
     * @param fun
     *     The function to apply to the {@link ResultSet}.
     * @param fallback
     *     The value to return in case the result is null or if an error occurred.
     * @param <T>
     *     The type of the result to return.
     * @return The {@link ResultSet} of the query, or null in case an error occurred.
     */
    @Locked.Read("lock")
    @Contract(" _, _, _, !null -> !null")
    private @Nullable <T> T executeQuery(
        Connection conn,
        DelayedPreparedStatement delayedPreparedStatement,
        CheckedFunction<ResultSet, T, Exception> fun,
        @Nullable T fallback)
    {
        logStatement(delayedPreparedStatement);
        try (
            PreparedStatement ps = delayedPreparedStatement.construct(conn);
            ResultSet rs = ps.executeQuery())
        {
            return fun.apply(rs);
        }
        catch (Exception e)
        {
            log.atSevere().withCause(e).log("Failed to execute query: %s", delayedPreparedStatement);
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
    @Locked.Read("lock")
    @SuppressWarnings("unused")
    @Contract(" _, !null  -> !null")
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
    @Locked.Read("lock")
    @Contract(" _, !null, _ -> !null")
    private @Nullable <T> T execute(
        CheckedFunction<Connection, T, Exception> fun,
        @Nullable T fallback,
        FailureAction failureAction)
    {
        try (@Nullable Connection conn = getConnection())
        {
            try
            {
                if (conn == null)
                {
                    log.atSevere().withStackTrace(StackSize.FULL).log(
                        "Failed to execute function: Connection is null!");
                    return fallback;
                }
                return fun.apply(conn);
            }
            catch (Exception e)
            {
                log.atSevere().withCause(e).log(
                    "Failed to execute function! Using failure action: '%s'",
                    failureAction
                );

                if (conn != null && failureAction == FailureAction.ROLLBACK)
                    conn.rollback();
            }
        }
        catch (Exception e)
        {
            log.atSevere().withCause(e).log("Failed to execute function!");
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
    @Locked.Read("lock")
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
            },
            fallback,
            FailureAction.ROLLBACK
        );
    }

    /**
     * Logs a {@link DelayedPreparedStatement} to the logger.
     *
     * @param delayedPreparedStatement
     *     The {@link DelayedPreparedStatement} to log.
     */
    private static void logStatement(DelayedPreparedStatement delayedPreparedStatement)
    {
        log.atFinest().log("Executed statement: %s", delayedPreparedStatement);
    }

    @Override
    public String getDebugInformation()
    {
        return String.format("""
                Database state: %s
                Database file:  %s
                """,
            databaseState.name(),
            dataSourceInfo
        );
    }

    /**
     * Converts a {@link OptionalInt} to an {@link Integer} if it is present, otherwise to {@code null}.
     *
     * @param optionalInt
     *     The {@link OptionalInt} to convert.
     * @return The {@link Integer} if the {@link OptionalInt} is present, otherwise {@code null}.
     */
    private static @Nullable Integer optionalIntToObject(OptionalInt optionalInt)
    {
        return optionalInt.isPresent() ? optionalInt.getAsInt() : null;
    }

    /**
     * Returns an {@link OptionalInt} from the {@link ResultSet} for the given column. If the value is {@code null}, the
     * {@link OptionalInt} will be empty.
     *
     * @param resultSet
     *     The {@link ResultSet} to get the value from.
     * @param column
     *     The name of the column to get the value from.
     * @return An {@link OptionalInt} with the value from the {@link ResultSet} or empty if the value was {@code null}.
     *
     * @throws SQLException
     *     If an error occurs while retrieving the value from the {@link ResultSet}.
     */
    private OptionalInt getOptionalInt(ResultSet resultSet, String column)
        throws SQLException
    {
        final int value = resultSet.getInt(column);
        return resultSet.wasNull() ? OptionalInt.empty() : OptionalInt.of(value);
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
