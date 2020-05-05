package nl.pim16aap2.bigdoors.storage.sqlite;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.api.IConfigLoader;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.api.IPWorld;
import nl.pim16aap2.bigdoors.doors.AbstractDoorBase;
import nl.pim16aap2.bigdoors.doortypes.DoorType;
import nl.pim16aap2.bigdoors.managers.DoorTypeManager;
import nl.pim16aap2.bigdoors.storage.IStorage;
import nl.pim16aap2.bigdoors.storage.PPreparedStatement;
import nl.pim16aap2.bigdoors.storage.SQLStatement;
import nl.pim16aap2.bigdoors.util.BitFlag;
import nl.pim16aap2.bigdoors.util.DoorOwner;
import nl.pim16aap2.bigdoors.util.Functional.CheckedFunction;
import nl.pim16aap2.bigdoors.util.PLogger;
import nl.pim16aap2.bigdoors.util.Pair;
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
import java.sql.ResultSetMetaData;
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
     * The database file.
     */
    private final File dbFile;

    /**
     * The URL of the database.
     */
    private final String url;

    /**
     * The BigDoors configuration.
     */
    private final IConfigLoader config;

    /**
     * Log ALL statements to the log file.
     */
    private boolean logStatements = false;

    /**
     * The {@link DatabaseState} the database is in.
     */
    private volatile DatabaseState databaseState = DatabaseState.UNINITIALIZED;

    /**
     * Constructor of the SQLite driver connection.
     *
     * @param dbFile The file to store the database in.
     * @param config The {@link IConfigLoader} containing options used in this class.
     */
    public SQLiteJDBCDriverConnection(final @NotNull File dbFile, final @NotNull IConfigLoader config)
    {
        this.dbFile = dbFile;
        this.config = config;
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
     * @param state The state from which the connection was requested.
     * @return A database connection.
     */
    @Nullable
    private Connection getConnection(final @NotNull DatabaseState state)
    {
        if (!databaseState.equals(state))
        {
            PLogger.get().logException(new IllegalStateException(
                "The database is in an incorrect state: " + databaseState.name() +
                    ". All database operations are disabled!"));
            return null;
        }

        Connection conn = null;
        try
        {
            conn = DriverManager.getConnection(url);
            SQLStatement.FOREIGN_KEYS_ON.constructPPreparedStatement().construct(conn).execute();
        }
        catch (SQLException e)
        {
            PLogger.get().logException(e, "Failed to open connection!");
        }
        if (conn == null)
            PLogger.get().logException(new NullPointerException("Could not open connection!"));
        return conn;
    }

    /**
     * Establishes a connection with the database, assuming a database state of {@link DatabaseState#OK}.
     *
     * @return A database connection.
     */
    @Nullable
    private Connection getConnection()
    {
        return getConnection(DatabaseState.OK);
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
        if (dbFile.exists())
        {
            dbFile.delete();
        }
        if (!dbFile.exists())
            try
            {
                if (!dbFile.getParentFile().exists() && !dbFile.getParentFile().mkdirs())
                {
                    PLogger.get().logException(
                        new IOException(
                            "Failed to create directory \"" + dbFile.getParentFile().toString() + "\""));
                    databaseState = DatabaseState.ERROR;
                    return;
                }
                if (!dbFile.createNewFile())
                {
                    PLogger.get().logException(new IOException("Failed to create file \"" + dbFile.toString() + "\""));
                    databaseState = DatabaseState.ERROR;
                    return;
                }
                PLogger.get().info("New file created at " + dbFile);
            }
            catch (IOException e)
            {
                PLogger.get().severe("File write error: " + dbFile);
                PLogger.get().logException(e);
                databaseState = DatabaseState.ERROR;
                return;
            }

        // Table creation
        try (final Connection conn = getConnection(DatabaseState.UNINITIALIZED))
        {
            if (conn == null)
            {
                databaseState = DatabaseState.ERROR;
                return;
            }

            // Check if the doors table already exists. If it does, assume the rest exists
            // as well and don't set it up.
            if (!conn.getMetaData().getTables(null, null, "DoorBase", new String[]{"TABLE"}).next())
            {
                executeUpdate(conn, SQLStatement.CREATE_TABLE_WORLD.constructPPreparedStatement());
                executeUpdate(conn, SQLStatement.CREATE_TABLE_PLAYER.constructPPreparedStatement());
                executeUpdate(conn, SQLStatement.CREATE_TABLE_DOORTYPE.constructPPreparedStatement());
                executeUpdate(conn, SQLStatement.CREATE_TABLE_DOORBASE.constructPPreparedStatement());
                executeUpdate(conn, SQLStatement.CREATE_TABLE_DOOROWNER_PLAYER.constructPPreparedStatement());
                executeUpdate(conn, SQLStatement.CREATE_TABLE_POWERBLOCK.constructPPreparedStatement());

                setDBVersion(conn, DATABASE_VERSION);
                databaseState = DatabaseState.OK;
            }
            else
                databaseState = DatabaseState.OUT_OF_DATE; // Assume it's outdated if it isn't newly created.
        }
        catch (SQLException | NullPointerException e)
        {
            PLogger.get().logException(e);
            databaseState = DatabaseState.ERROR;
        }
    }


    /*
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     */

    @NotNull
    private Optional<AbstractDoorBase> constructDoor(final @NotNull ResultSet doorBaseRS,
                                                     final @NotNull Object[] typeData)
        throws SQLException
    {
        final @NotNull Optional<DoorType> doorType = DoorTypeManager.get().getDoorType(doorBaseRS.getInt("doorType"));

        final @NotNull Optional<RotateDirection> openDirection =
            Optional.ofNullable(RotateDirection.valueOf(doorBaseRS.getInt("openDirection")));

        if (!doorType.isPresent() || !openDirection.isPresent())
            return Optional.empty();

        final long doorUID = doorBaseRS.getLong("id");
        final @NotNull Vector3Di min = new Vector3Di(doorBaseRS.getInt("xMin"),
                                                     doorBaseRS.getInt("yMin"),
                                                     doorBaseRS.getInt("zMin"));
        final @NotNull Vector3Di max = new Vector3Di(doorBaseRS.getInt("xMax"),
                                                     doorBaseRS.getInt("yMax"),
                                                     doorBaseRS.getInt("zMax"));
        final @NotNull Vector3Di eng = new Vector3Di(doorBaseRS.getInt("engineX"),
                                                     doorBaseRS.getInt("engineY"),
                                                     doorBaseRS.getInt("engineZ"));

        final @NotNull IPWorld world = BigDoors.get().getPlatform().getPWorldFactory()
                                               .create(UUID.fromString(doorBaseRS.getString("worldUUID")));

        final long bitflag = doorBaseRS.getLong("bitflag");
        final boolean isOpen = BitFlag.hasFlag(DoorFlag.getFlagValue(DoorFlag.ISOPEN), bitflag);
        final boolean isLocked = BitFlag.hasFlag(DoorFlag.getFlagValue(DoorFlag.ISLOCKED), bitflag);

        final @NotNull DoorOwner doorOwner = new DoorOwner(doorUID, UUID.fromString(doorBaseRS.getString("playerUUID")),
                                                           doorBaseRS.getString("playerName"),
                                                           doorBaseRS.getInt("permission"));

        final @NotNull Vector3Di powerBlock = new Vector3Di(0, 0, 0);
        final @NotNull String name = doorBaseRS.getString("name");

        final @NotNull AbstractDoorBase.DoorData doorData = new AbstractDoorBase.DoorData(doorUID, name, min, max, eng,
                                                                                          powerBlock, world, isOpen,
                                                                                          openDirection.get(),
                                                                                          doorOwner, isLocked);

        final @NotNull Optional<? extends AbstractDoorBase> door = doorType.get().constructDoor(doorData, typeData);

        return door.map(abstractDoorBase -> (AbstractDoorBase) abstractDoorBase);
    }

    /**
     * Generate the type-specific data for a door, represented as an array of Objects. The objects are retrieved as
     * defined by {@link DoorType#getParameters()}.
     *
     * @param doorBaseRS The {@link ResultSet} containing the type-specific data.
     * @return The array of Objects containing the type-specific data for a door.
     *
     * @throws SQLException
     */
    @NotNull
    private Object[] createTypeData(final @NotNull ResultSet doorBaseRS)
        throws SQLException
    {
        final @NotNull Optional<DoorType> doorType =
            // There are 2 columns named "id" here. The first one is the ID of the DoorType, the second one
            // is the ID of the type-specific-table.
            DoorTypeManager.get().getDoorType(doorBaseRS.getInt(1));

        if (!doorType.isPresent())
            return new Object[]{};

        final @NotNull List<DoorType.Parameter> parameters = doorType.get().getParameters();
        if (parameters.isEmpty())
            return new Object[]{};

        final int parameterCount = parameters.size();
        final @NotNull ResultSetMetaData rsmd = doorBaseRS.getMetaData();
        final int columnsNumber = rsmd.getColumnCount();

        if (columnsNumber != (parameterCount + 3)) // The ID and the doorUID values aren't custom parameters.
            return new Object[]{};

        final @NotNull Object[] typeData = new Object[parameterCount];
        for (int idx = 0; idx < parameterCount; ++idx)
            typeData[idx] = doorBaseRS.getObject(parameters.get(idx).getParameterName());
        return typeData;
    }

    /**
     * {@inheritDoc}
     */
    // TODO: Clean up this method. Split it up into smaller ones or something. This is getting too confusing.
    @Override
    public long registerDoorType(final @NotNull DoorType doorType)
    {
        final @NotNull String typeTableName = getTableNameOfType(doorType);
        if (!IStorage.isValidTableName(typeTableName))
        {
            PLogger.get().logException(new IllegalArgumentException(
                "Invalid table name in database: \"" + typeTableName + "\". Please use only alphanumeric characters."));
            return -1;
        }

        int updateStatus = executeUpdate(SQLStatement.INSERT_OR_IGNORE_DOOR_TYPE.constructPPreparedStatement()
                                                                                .setString(1, doorType.getPluginName())
                                                                                .setString(2, doorType.getTypeName())
                                                                                .setInt(3, doorType.getVersion())
                                                                                .setString(4, typeTableName));
        if (updateStatus == -1)
            return -1;

        // FIXME: Read the data of the table to make sure that the parameters that were passed on to this method
        //        Are still up to date. If not, throw some kind of version mismatch exception or something.

        final @NotNull Optional<Pair<Long, String>> result = Optional.ofNullable(executeQuery(
            SQLStatement.GET_DOOR_TYPE_TABLE_NAME_AND_ID.constructPPreparedStatement()
                                                        .setString(1, doorType.getPluginName())
                                                        .setString(2, doorType.getTypeName())
                                                        .setInt(3, doorType.getVersion()),
            resultSet -> new Pair<>(resultSet.getLong("id"), resultSet.getString("typeTableName"))));

        if (!result.isPresent())
            return -1;

        // Create a new table for this DoorType, if needed.
        final @NotNull StringBuilder tableCreationStatementBuilder = new StringBuilder();
        tableCreationStatementBuilder.append("CREATE TABLE IF NOT EXISTS ").append(result.get().value())
                                     .append("(id INTEGER PRIMARY KEY AUTOINCREMENT, ")
                                     .append("doorUID REFERENCES DoorBase(id) ON UPDATE CASCADE ON DELETE CASCADE");
        for (final DoorType.Parameter parameter : doorType.getParameters())
            tableCreationStatementBuilder.append(", ").append(parameter.getParameterName()).append(" ")
                                         .append(parameter.getParameterType()).append(" NOT NULL");
        tableCreationStatementBuilder.append(", UNIQUE(doorUID));");

        // If the table creation failed, delete the entry from the DoorType table.
        int insertStatus = executeUpdate(new PPreparedStatement(0, tableCreationStatementBuilder.toString()));

        if (insertStatus == -1)
        {
            executeUpdate(SQLStatement.DELETE_DOOR_TYPE.constructPPreparedStatement().setLong(1, result.get().key()));
            return -1;
        }
        return result.get().key();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean deleteDoorType(final @NotNull DoorType doorType)
    {
        final long typeID = DoorTypeManager.get().getDoorType(doorType).orElse(-1L);
        if (typeID == -1)
        {
            PLogger.get().logException(new IllegalStateException(
                "Trying to delete door type: " + doorType.toString() +
                    ", but it is not registered! Please register it first."));
            return false;
        }
        final @NotNull String typeTableName = getTableNameOfType(doorType);
        if (!IStorage.isValidTableName(typeTableName))
        {
            PLogger.get().logException(new IllegalArgumentException(
                "Invalid table name in database: \"" + typeTableName + "\". Please use only alphanumeric characters."));
            return false;
        }
        // TODO: Use a transaction.
        int result = executeUpdate(SQLStatement.DELETE_DOOR_TYPE.constructPPreparedStatement().setLong(1, typeID));
        result += executeUpdate(new PPreparedStatement(0, "DROP TABLE " + typeTableName + ";"));
        return result == 2; // TODO: This isn't particularly safe. If it's 1, for example, which data is still there??
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean updateTypeData(final @NotNull AbstractDoorBase door)
    {
        final @NotNull Optional<Object[]> typeDataOpt = door.getDoorType().getTypeData(door);
        if (!typeDataOpt.isPresent())
        {
            PLogger.get().logException(new IllegalArgumentException(
                "Failed to update door " + door.getDoorUID() + ": Could not get type-specific data!"));
            return false;
        }

        if (!DoorTypeManager.get().isRegistered(door.getDoorType()))
        {
            PLogger.get().logException(new SQLException(
                "Failed to update type-data of door: \"" + door.getDoorUID() + "\"! Reason: DoorType not registered!"));
            return false;
        }

        final @NotNull StringBuilder updateStatementBuilder = new StringBuilder();
        updateStatementBuilder.append("UPDATE ").append(getTableNameOfType(door.getDoorType())).append(" SET ");

        final @NotNull List<DoorType.Parameter> parameters = door.getDoorType().getParameters();
        final int parameterCount = parameters.size();
        for (int idx = 0; idx < parameterCount; ++idx)
        {
            updateStatementBuilder.append(parameters.get(idx).getParameterName()).append(" = ?");
            if (idx < (parameterCount - 1))
                updateStatementBuilder.append(", ");
        }
        updateStatementBuilder.append(" WHERE doorUID = ?;");

        // TODO: Caching
        final @NotNull PPreparedStatement pPreparedStatement =
            new PPreparedStatement(parameterCount + 1, updateStatementBuilder.toString());
        final @NotNull Object[] typeData = typeDataOpt.get();

        for (int idx = 0; idx < parameterCount; ++idx)
            pPreparedStatement.setObject(idx + 1, typeData[idx]);
        pPreparedStatement.setLong(parameterCount + 1, door.getDoorUID());

        return executeUpdate(pPreparedStatement) > 0;
    }

    /**
     * Constructs the name of the table used for the type-specific data of a {@link DoorType}.
     *
     * @param doorType The {@link DoorType}.
     * @return The name of the table.
     */
    private String getTableNameOfType(final @NotNull DoorType doorType)
    {
        return String.format("%s_%s_%d", doorType.getPluginName(), doorType.getTypeName(), doorType.getVersion());
    }

    private long insert(final @NotNull Connection conn, final @NotNull AbstractDoorBase door, final long doorTypeID,
                        final @NotNull Object[] typeSpecificData)
        throws SQLException
    {
        conn.setAutoCommit(false);
        final @NotNull String playerUUID = door.getDoorOwner().getPlayerUUID().toString();
        final @NotNull String playerName = door.getDoorOwner().getPlayerName();
        final @NotNull String worldUUID = door.getWorld().getUID().toString();

        executeUpdate(conn, SQLStatement.INSERT_OR_IGNORE_WORLD.constructPPreparedStatement().setString(1, worldUUID));

        executeUpdate(conn, SQLStatement.INSERT_OR_IGNORE_PLAYER.constructPPreparedStatement()
                                                                .setString(1, playerUUID)
                                                                .setString(2, playerName));

        final long chunkHash = Util.simpleChunkHashFromLocation(door.getEngine().getX(), door.getEngine().getZ());
        executeUpdate(conn, SQLStatement.INSERT_DOOR_BASE.constructPPreparedStatement()
                                                         .setString(1, door.getName())
                                                         .setString(2, worldUUID)
                                                         .setLong(3, doorTypeID)
                                                         .setInt(4, door.getMinimum().getX())
                                                         .setInt(5, door.getMinimum().getY())
                                                         .setInt(6, door.getMinimum().getZ())
                                                         .setInt(7, door.getMaximum().getX())
                                                         .setInt(8, door.getMaximum().getY())
                                                         .setInt(9, door.getMaximum().getZ())
                                                         .setInt(10, door.getEngine().getX())
                                                         .setInt(11, door.getEngine().getY())
                                                         .setInt(12, door.getEngine().getZ())
                                                         .setLong(13, getFlag(door))
                                                         .setInt(14, RotateDirection.getValue(door.getOpenDir()))
                                                         .setLong(15, chunkHash));

        // TODO: Caching.
        final @NotNull String typeTableName = getTableNameOfType(door.getDoorType());
        final @NotNull StringBuilder parameterNames = new StringBuilder();
        final @NotNull StringBuilder parameterQuestionMarks = new StringBuilder();

        parameterNames.append("INSERT INTO ").append(typeTableName).append(" (doorUID, ");
        parameterQuestionMarks.append("((SELECT seq FROM sqlite_sequence WHERE sqlite_sequence.name =\"DoorBase\"), ");

        final int parameterCount = door.getDoorType().getParameterCount();
        for (int parameterIDX = 0; parameterIDX < parameterCount; ++parameterIDX)
        {
            final @NotNull DoorType.Parameter parameter = door.getDoorType().getParameters().get(parameterIDX);
            parameterNames.append(parameter.getParameterName());
            parameterQuestionMarks.append("?");

            if (parameterIDX == (parameterCount - 1))
            {
                parameterNames.append(") VALUES ");
                parameterQuestionMarks.append(");");
            }
            else
            {
                parameterNames.append(", ");
                parameterQuestionMarks.append(", ");
            }
        }

        final @NotNull String insertString = parameterNames.toString() + parameterQuestionMarks.toString();

        final @NotNull PPreparedStatement pPreparedStatement = new PPreparedStatement(parameterCount, insertString);
        for (int parameterIDX = 0; parameterIDX < parameterCount; ++parameterIDX)
            pPreparedStatement.setObject(parameterIDX + 1, typeSpecificData[parameterIDX]);
        executeUpdate(conn, pPreparedStatement);

        long doorUID = executeQuery(conn, SQLStatement.SELECT_MOST_RECENT_DOOR.constructPPreparedStatement(),
                                    rs -> rs.next() ? rs.getLong("seq") : -1, -1L);

        executeUpdate(conn, SQLStatement.INSERT_DOOR_CREATOR.constructPPreparedStatement().setString(1, playerUUID));

        conn.commit();
        return doorUID;
    }

    /**
     * {@inheritDoc}
     */
    // TODO: Return the UID of the door.
    @Override
    public boolean insert(final @NotNull AbstractDoorBase door)
    {
        final @NotNull Optional<Object[]> typeSpecificDataOpt = door.getDoorType().getTypeData(door);
        if (!typeSpecificDataOpt.isPresent())
        {
            PLogger.get().logException(new IllegalArgumentException(
                "Could not get type-specific data for a new door of type: " + door.getDoorType().toString()));
            return false;
        }

        final long doorTypeID = DoorTypeManager.get().getDoorType(door.getDoorType()).orElse(-1L);
        if (doorTypeID == -1)
        {
            PLogger.get()
                   .logException(new SQLException("Could not find DoorType: " + door.getDoorType().toString()));
            return false;
        }

        try (final @Nullable Connection conn = getConnection())
        {
            if (conn == null)
                return false;
            return insert(conn, door, doorTypeID, typeSpecificDataOpt.get()) > 0;

//            long doorUID = insert(conn, door, doorTypeID, typeSpecificDataOpt.get());
//
//            if (doorUID % 100 == 0)
//                System.out.println("Added door: " + doorUID);
//
//            return doorUID > 0;
        }
        catch (Exception e)
        {
            PLogger.get().logException(e);
        }
        return false;
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
        executeUpdate(conn, SQLStatement.INSERT_OR_IGNORE_PLAYER.constructPPreparedStatement()
                                                                .setString(1, doorOwner.getPlayerUUID().toString())
                                                                .setString(2, doorOwner.getPlayerName()));

        return executeQuery(conn, SQLStatement.GET_PLAYER_ID.constructPPreparedStatement()
                                                            .setString(1, doorOwner.getPlayerUUID().toString()),
                            rs -> rs.next() ? rs.getLong("id") : -1, -1L);
    }

    /**
     * Attempts to construct a subclass of {@link AbstractDoorBase} from a resultset containing all data pertaining the
     * {@link AbstractDoorBase} (as stored in the "DoorBase" table), as well as the owner (name, UUID, permission) and
     * the typeTableName.
     *
     * @param doorBaseRS The {@link ResultSet} containing a row from the "DoorBase" table as well as a row from the
     *                   "DoorOwnerPlayer" table and "typeTableName" from the "DoorType" table.
     * @return An instance of a subclass of {@link AbstractDoorBase} if it could be created.
     *
     * @throws SQLException
     */
    private Optional<AbstractDoorBase> getDoor(final @NotNull ResultSet doorBaseRS)
        throws SQLException
    {
        // Make sure the resultset isn't empty.
        if (!doorBaseRS.isBeforeFirst())
            return Optional.empty();

        if (!DoorTypeManager.get().isRegistered(doorBaseRS.getInt("doorType")))
        {
            PLogger.get().logException(new IllegalStateException("Type with ID: " + doorBaseRS.getInt("doorType") +
                                                                     " has not been registered (yet)!"));
            return Optional.empty();
        }
        final @NotNull Object[] typeData =
            executeQuery(SQLStatement.GET_TYPE_SPECIFIC_DATA.constructPPreparedStatement()
                                                            .setTableName(1, doorBaseRS.getString("typeTableName"))
                                                            .setString(2, doorBaseRS.getString("typeTableName"))
                                                            .setLong(3, doorBaseRS.getLong("id")),
                         this::createTypeData, new Object[]{});
        return constructDoor(doorBaseRS, typeData);
    }

    /**
     * Attempts to construct a list of subclasses of {@link AbstractDoorBase} from a resultset containing all data
     * pertaining to one or more {@link AbstractDoorBase}s (as stored in the "DoorBase" table), as well as the owner
     * (name, UUID, permission) and the typeTableName.
     *
     * @param doorBaseRS The {@link ResultSet} containing one or more rows from the "DoorBase" table as well as matching
     *                   rows from the "DoorOwnerPlayer" table and "typeTableName" from the "DoorType" table.
     * @return An optional with a list of {@link AbstractDoorBase}s if any could be constructed. If none could be
     * constructed, an empty {@link Optional} is returned instead.
     *
     * @throws SQLException
     */
    private Optional<List<AbstractDoorBase>> getDoors(final @NotNull ResultSet doorBaseRS)
        throws SQLException
    {
        // Make sure the resultset isn't empty.
        if (!doorBaseRS.isBeforeFirst())
            return Optional.empty();

        final List<AbstractDoorBase> doors = new ArrayList<>();
        while (doorBaseRS.next())
        {
            if (!DoorTypeManager.get().isRegistered(doorBaseRS.getInt("doorType")))
            {
                PLogger.get().logException(new IllegalStateException("Type with ID: " + doorBaseRS.getInt("doorType") +
                                                                         " has not been registered (yet)!"));
                continue;
            }
            final @NotNull Object[] typeData =
                executeQuery(SQLStatement.GET_TYPE_SPECIFIC_DATA.constructPPreparedStatement()
                                                                .setTableName(1, doorBaseRS.getString("typeTableName"))
                                                                .setString(2, doorBaseRS.getString("typeTableName"))
                                                                .setLong(3, doorBaseRS.getLong("id")),
                             this::createTypeData, new Object[]{});

            constructDoor(doorBaseRS, typeData).ifPresent(doors::add);
        }
        return doors.isEmpty() ? Optional.empty() : Optional.of(doors);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public Optional<AbstractDoorBase> getDoor(final long doorUID)
    {
        return executeQuery(SQLStatement.GET_DOOR_BASE_FROM_ID.constructPPreparedStatement()
                                                              .setLong(1, doorUID),
                            this::getDoor, Optional.empty());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public Optional<AbstractDoorBase> getDoor(final @NotNull UUID playerUUID, final long doorUID)
    {
        return executeQuery(SQLStatement.GET_DOOR_BASE_FROM_ID_FOR_PLAYER.constructPPreparedStatement()
                                                                         .setLong(1, doorUID)
                                                                         .setString(2, playerUUID.toString()),
                            this::getDoor, Optional.empty());
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
                            resultSet -> resultSet.next() ? resultSet.getInt("permission") : -1, -1);
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
    public boolean isBigDoorsWorld(final @NotNull UUID worldUUID)
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
                            resultSet -> resultSet.next() ? resultSet.getInt("total") : -1, -1);
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
                            resultSet -> resultSet.next() ? resultSet.getInt("total") : -1, -1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getDoorCountByName(final @NotNull String doorName)
    {
        return executeQuery(SQLStatement.GET_DOOR_COUNT_BY_NAME.constructPPreparedStatement()
                                                               .setString(1, doorName),
                            resultSet -> resultSet.next() ? resultSet.getInt("total") : -1, -1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getOwnerCountOfDoor(final long doorUID)
    {
        return executeQuery(SQLStatement.GET_OWNER_COUNT_OF_DOOR.constructPPreparedStatement()
                                                                .setLong(1, doorUID),
                            resultSet -> resultSet.next() ? resultSet.getInt("total") : -1, -1);
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
                            this::getDoors, Optional.empty());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public Optional<List<AbstractDoorBase>> getDoors(final @NotNull UUID playerUUID,
                                                     final @NotNull String name)
    {
        return getDoors(playerUUID.toString(), name, 0);
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
                            this::getDoors, Optional.empty());
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
                            this::getDoors, Optional.empty());
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
                            resultSet ->
                            {
                                UUID playerUUID = null;
                                int count = 0;
                                while (resultSet.next())
                                {
                                    ++count;
                                    if (count > 1)
                                    {
                                        playerUUID = null;
                                        break;
                                    }
                                    playerUUID = UUID.fromString(resultSet.getString("playerUUID"));
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
                            resultSet -> Optional
                                .ofNullable(resultSet.next() ? resultSet.getString("playerName") : null),
                            Optional.empty());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public Optional<DoorOwner> getCreatorOfDoor(final long doorUID)
    {
        return executeQuery(SQLStatement.GET_DOOR_OWNER.constructPPreparedStatement().setLong(1, doorUID),
                            (resultSet -> Optional.of(new DoorOwner(doorUID,
                                                                    UUID.fromString(resultSet.getString("playerUUID")),
                                                                    resultSet.getString("playerName"),
                                                                    resultSet.getInt("permission")))),
                            Optional.empty());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public ConcurrentHashMap<Integer, List<Long>> getPowerBlockData(final long chunkHash)
    {
        return null;
//        return executeQuery(SQLStatement.GET_POWER_BLOCK_DATA_IN_CHUNK.constructPPreparedStatement()
//                                                                      .setLong(1, chunkHash),
//                            resultSet ->
//                            {
//                                final ConcurrentHashMap<Integer, List<Long>> doors = new ConcurrentHashMap<>();
//                                while (resultSet.next())
//                                {
//                                    int locationHash = Util.simpleChunkSpaceLocationhash(resultSet.getInt("x"),
//                                                                                         resultSet.getInt("y"),
//                                                                                         resultSet.getInt("z"));
//                                    if (!doors.containsKey(locationHash))
//                                        doors.put(locationHash, new ArrayList<>());
//                                    doors.get(locationHash).add(resultSet.getLong("id"));
//                                }
//                                return doors;
//                            }, new ConcurrentHashMap<>());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public List<Long> getDoorsInChunk(final long chunkHash)
    {
        return executeQuery(SQLStatement.GET_DOOR_IDS_IN_CHUNK.constructPPreparedStatement()
                                                              .setLong(1, chunkHash),
                            resultSet ->
                            {
                                final List<Long> doors = new ArrayList<>();
                                while (resultSet.next())
                                    doors.add(resultSet.getLong("id"));
                                return doors;
                            }, new ArrayList<>());
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
        return false;
//        return executeUpdate(SQLStatement.UPDATE_DOOR_POWER_BLOCK_LOC.constructPPreparedStatement()
//                                                                     .setInt(1, xPos)
//                                                                     .setInt(2, yPos)
//                                                                     .setInt(3, zPos)
//                                                                     .setLong(4, Util.simpleChunkHashFromLocation(xPos,
//                                                                                                                  zPos))
//                                                                     .setLong(5, doorUID)) > 0;
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
                                                           resultSet -> resultSet.getInt(1));
        if (currentFlag == null)
        {
            PLogger.get().logException(new SQLException("Could not get flag value of door: " + doorUID));
            return false;
        }

        final long newFlag = BitFlag.changeFlag(DoorFlag.getFlagValue(flag), flagStatus, currentFlag);
        return executeUpdate(SQLStatement.UPDATE_DOOR_FLAG.constructPPreparedStatement()
                                                          .setLong(1, newFlag)
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
     * Executes an update defined by a {@link PPreparedStatement}.
     *
     * @param pPreparedStatement The {@link PPreparedStatement}.
     * @return Either the number of rows modified by the update, or -1 if an error occurred.
     */
    private int executeUpdate(final @NotNull PPreparedStatement pPreparedStatement)
    {
        try (final Connection conn = getConnection())
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
        logStatement(pPreparedStatement);
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
            if (conn == null)
            {
                logStatement(pPreparedStatement);
                return -1;
            }
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
        logStatement(pPreparedStatement);
        try (final PreparedStatement ps = pPreparedStatement.construct(conn, Statement.RETURN_GENERATED_KEYS))
        {
            ps.executeUpdate();
            try (final ResultSet resultSet = ps.getGeneratedKeys())
            {
                return resultSet.getInt(1);
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
    @Contract(" _, _, !null -> !null;")
    private <T> T executeQuery(final @NotNull PPreparedStatement pPreparedStatement,
                               final @NotNull CheckedFunction<ResultSet, T, SQLException> fun,
                               final @Nullable T fallback)
    {
        try (final @Nullable Connection conn = getConnection())
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
            PLogger.get().logException(e);
        }
        return fallback;
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
                               // TODO: Store connection in the function, so chained queries don't have to open multiple connections.
                               final @NotNull CheckedFunction<ResultSet, T, SQLException> fun,
                               final @Nullable T fallback)
    {
        logStatement(pPreparedStatement);
        try (final PreparedStatement ps = pPreparedStatement.construct(conn);
             final ResultSet rs = ps.executeQuery())
        {
            final @Nullable T result = fun.apply(rs);
            return result == null ? fallback : result;
        }
        catch (SQLException e)
        {
            PLogger.get().logException(e);
        }
        return fallback;
    }

    /**
     * Executes a {@link CheckedFunction} given an active Connection.
     *
     * @param fun      The function to execute.
     * @param fallback The fallback value to return in case of failure.
     * @param <T>      The type of the result to return.
     * @return The result of the Function.
     */
    @Contract(" _, !null -> !null")
    private <T> T execute(final @NotNull CheckedFunction<Connection, T, SQLException> fun,
                          final @Nullable T fallback)
    {
        try (final @Nullable Connection conn = getConnection())
        {
            if (conn == null)
                return fallback;
            return fun.apply(conn);
        }
        catch (Exception e)
        {
            PLogger.get().logException(e);
        }
        return fallback;
    }

    /**
     * Logs a {@link PPreparedStatement} to the logger as INFO. If {@link #logStatements} is false, nothing is logged
     * instead.
     *
     * @param pPreparedStatement The {@link PPreparedStatement} to log.
     */
    private void logStatement(final @NotNull PPreparedStatement pPreparedStatement)
    {
        if (!logStatements)
            return;
        PLogger.get().info("Executed statement:\n" + pPreparedStatement.toString() + "\n");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setStatementLogging(final boolean enabled)
    {
        logStatements = enabled;
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
                            resultSet ->
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

        return executeUpdate(conn, SQLStatement.INSERT_DOOR_OWNER.constructPPreparedStatement()
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
        // TODO: Use transaction
        // permission level 0 is reserved for the creator, and negative values are not allowed.
        if (permission < 1)
            return false;

        try (final Connection conn = getConnection())
        {
            if (conn == null)
                return false;

            final String playerName = player.getName();
            final long playerID = getPlayerID(conn, new DoorOwner(doorUID, player.getUUID(), playerName, permission));
            if (playerID == -1)
            {
                PLogger.get().logException(new IllegalArgumentException(
                    "Trying to add player \"" + player.getUUID().toString() + "\" as owner of door " + doorUID +
                        ", but that player is not registered in the database! Aborting..."));
                return false;
            }

            return executeQuery(conn, SQLStatement.GET_DOOR_OWNER_PLAYER.constructPPreparedStatement()
                                                                        .setLong(1, playerID)
                                                                        .setLong(2, doorUID),
                                rs -> addOrUpdateOwner(conn, rs, doorUID, playerID, permission), false);
        }
        catch (SQLException | NullPointerException e)
        {
            PLogger.get().logException(e);
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DatabaseState getDatabaseState()
    {
        return databaseState;
    }

    /**
     * Obtains and checks the version of the database.
     * <p>
     * If the database version is invalid for this version of the database class, an error will be printed and the
     * appropriate {@link #databaseState} will be set.
     *
     * @param conn A connection to the database.
     * @return The version of the database, or -1 if something went wrong.
     */
    private int verifyDatabaseVersion(final @NotNull Connection conn)
    {
        int dbVersion = executeQuery(conn, new PPreparedStatement("PRAGMA user_version;"),
                                     rs -> rs.getInt(1), -1);
        if (dbVersion == -1)
        {
            PLogger.get().logMessage("Failed to obtain database version!");
            databaseState = DatabaseState.ERROR;
            return dbVersion;
        }

        if (dbVersion == DATABASE_VERSION)
        {
            databaseState = DatabaseState.OK;
        }

        if (dbVersion < MIN_DATABASE_VERSION)
        {
            PLogger.get().logMessage("Trying to load database version " + dbVersion +
                                         " while the minimum allowed version is " + MIN_DATABASE_VERSION);
            databaseState = DatabaseState.TOO_OLD;
        }

        if (dbVersion > DATABASE_VERSION)
        {
            PLogger.get().logMessage("Trying to load database version " + dbVersion +
                                         " while the maximum allowed version is " + DATABASE_VERSION);
            databaseState = DatabaseState.TOO_NEW;
        }
        return dbVersion;
    }

    /**
     * Upgrades the database to the latest version if needed.
     */
    private void upgrade()
    {
        Connection conn;
        try
        {
            conn = getConnection(DatabaseState.OUT_OF_DATE);
            if (conn == null)
                return;

            final int dbVersion = verifyDatabaseVersion(conn);
            if (databaseState != DatabaseState.OUT_OF_DATE)
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
                conn = getConnection(DatabaseState.OUT_OF_DATE);
                if (conn == null)
                    return;
            }

            if (dbVersion < 11)
                upgradeToV11(conn);

            // Do this at the very end, so the db version isn't altered if anything fails.
            setDBVersion(conn, DATABASE_VERSION);
            databaseState = DatabaseState.OK;
        }
        catch (SQLException | NullPointerException e)
        {
            PLogger.get().logException(e);
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
        final File dbFileBackup = new File(dbFile.toString() + ".BACKUP");
        // Only the most recent backup is kept, so delete the old one if a new one needs
        // to be created.
        if (dbFileBackup.exists() && !dbFileBackup.delete())
        {
            PLogger.get().logException(new IOException("Failed to delete old backup! Aborting backup creation!"));
            return false;
        }
        try
        {
            Files.copy(dbFile.toPath(), dbFileBackup.toPath());
        }
        catch (IOException e)
        {
            PLogger.get().logException(e, "Failed to create backup of the database! "
                + "Database upgrade aborted and access is disabled!");
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
            PLogger.get().logException(e);
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
            PLogger.get().warn("Upgrading database to V11!");

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
            PLogger.get().logException(e);
        }
    }
}
