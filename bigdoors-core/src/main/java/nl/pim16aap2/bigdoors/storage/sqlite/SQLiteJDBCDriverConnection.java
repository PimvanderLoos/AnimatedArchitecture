package nl.pim16aap2.bigdoors.storage.sqlite;

import lombok.NonNull;
import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.api.IPWorld;
import nl.pim16aap2.bigdoors.api.PPlayerData;
import nl.pim16aap2.bigdoors.doors.AbstractDoorBase;
import nl.pim16aap2.bigdoors.doortypes.DoorType;
import nl.pim16aap2.bigdoors.managers.DoorRegistry;
import nl.pim16aap2.bigdoors.managers.DoorTypeManager;
import nl.pim16aap2.bigdoors.storage.DoorTypeDataStatementMap;
import nl.pim16aap2.bigdoors.storage.IStorage;
import nl.pim16aap2.bigdoors.storage.PPreparedStatement;
import nl.pim16aap2.bigdoors.storage.SQLStatement;
import nl.pim16aap2.bigdoors.util.DoorOwner;
import nl.pim16aap2.bigdoors.util.Functional.CheckedFunction;
import nl.pim16aap2.bigdoors.util.Functional.CheckedSupplier;
import nl.pim16aap2.bigdoors.util.IBitFlag;
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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * SQLite implementation of {@link IStorage}.
 *
 * @author Pim
 */
public final class SQLiteJDBCDriverConnection implements IStorage
{
    @NotNull
    private static final String DRIVER = "org.sqlite.JDBC";
    private static final int DATABASE_VERSION = 11;
    // TODO: Set this to 10. This cannot be done currently because the tests will fail for the upgrades, which
    //       are still useful when writing the code to upgrade the v1 database to v2.
    private static final int MIN_DATABASE_VERSION = 0;

    @NotNull
    private final DoorTypeDataStatementMap doorTypeStatementMap = new DoorTypeDataStatementMap();

    /**
     * A fake UUID that cannot exist normally. To be used for storing transient data across server restarts.
     */
    @NotNull
    private static final String FAKEUUID = "0000";

    /**
     * The database file.
     */
    @NotNull
    private final File dbFile;

    /**
     * The URL of the database.
     */
    @NotNull
    private final String url;

    /**
     * The {@link DatabaseState} the database is in.
     */
    @NotNull
    private volatile DatabaseState databaseState = DatabaseState.UNINITIALIZED;

    /**
     * Constructor of the SQLite driver connection.
     *
     * @param dbFile The file to store the database in.
     */
    public SQLiteJDBCDriverConnection(final @NotNull File dbFile)
    {
        this.dbFile = dbFile;
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
     * @param state The state from which the connection was requested.
     * @return A database connection.
     */
    private @Nullable Connection getConnection(final @NotNull DatabaseState state)
    {
        if (!databaseState.equals(state))
        {
            PLogger.get().logThrowable(new IllegalStateException(
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
            PLogger.get().logThrowable(e, "Failed to open connection!");
        }
        if (conn == null)
            PLogger.get().logThrowable(new NullPointerException("Could not open connection!"));
        return conn;
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
     * suddenly ignored all the triggers etc attached to it without actually providing a proper alternative (perhaps
     * implement ALTER TABLE properly??), this method needs to be called now in order to safely modify stuff without
     * having the foreign keys get fucked up.
     *
     * @param conn The connection.
     */
    private void disableForeignKeys(final @NotNull Connection conn)
        throws Exception
    {
        SQLStatement.FOREIGN_KEYS_OFF.constructPPreparedStatement().construct(conn).execute();
        SQLStatement.LEGACY_ALTER_TABLE_ON.constructPPreparedStatement().construct(conn).execute();
    }

    /**
     * The anti method of {@link #disableForeignKeys(Connection)}. Only needs to be called if that was called first.
     *
     * @param conn The connection.
     */
    private void reEnableForeignKeys(final @NotNull Connection conn)
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
        if (!dbFile.exists())
            try
            {
                if (!dbFile.getParentFile().exists() && !dbFile.getParentFile().mkdirs())
                {
                    PLogger.get().logThrowable(
                        new IOException(
                            "Failed to create directory \"" + dbFile.getParentFile().toString() + "\""));
                    databaseState = DatabaseState.ERROR;
                    return;
                }
                if (!dbFile.createNewFile())
                {
                    PLogger.get().logThrowable(new IOException("Failed to create file \"" + dbFile.toString() + "\""));
                    databaseState = DatabaseState.ERROR;
                    return;
                }
                PLogger.get().info("New file created at " + dbFile);
            }
            catch (IOException e)
            {
                PLogger.get().severe("File write error: " + dbFile);
                PLogger.get().logThrowable(e);
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
                executeUpdate(conn, SQLStatement.CREATE_TABLE_PLAYER.constructPPreparedStatement());
                executeUpdate(conn, SQLStatement.CREATE_TABLE_DOORTYPE.constructPPreparedStatement());
                executeUpdate(conn, SQLStatement.CREATE_TABLE_DOORBASE.constructPPreparedStatement());
                executeUpdate(conn, SQLStatement.CREATE_TABLE_DOOROWNER_PLAYER.constructPPreparedStatement());

                setDBVersion(conn, DATABASE_VERSION);
                databaseState = DatabaseState.OK;
            }
            else
                databaseState = DatabaseState.OUT_OF_DATE; // Assume it's outdated if it isn't newly created.
        }
        catch (SQLException | NullPointerException e)
        {
            PLogger.get().logThrowable(e);
            databaseState = DatabaseState.ERROR;
        }
    }

    private @NotNull Optional<AbstractDoorBase> constructDoor(final @NotNull ResultSet doorBaseRS,
                                                              final @NotNull CheckedSupplier<Object[], Exception> typeDataSupplier)
        throws Exception
    {
        final long doorUID = doorBaseRS.getLong("id");

        final @NotNull Optional<DoorType> doorType = DoorTypeManager.get().getDoorType(doorBaseRS.getInt("doorType"));
        if (doorType.isEmpty())
        {
            PLogger.get().logThrowable(new NullPointerException(
                "Failed to obtain door type of door: " + doorUID + ", typeID: " + doorBaseRS.getInt("doorType")));
            return Optional.empty();
        }

        final @NotNull Optional<AbstractDoorBase> registeredDoor = DoorRegistry.get().getRegisteredDoor(doorUID);
        if (registeredDoor.isPresent())
            return registeredDoor;

        final @NotNull Optional<RotateDirection> openDirection =
            Optional.ofNullable(RotateDirection.valueOf(doorBaseRS.getInt("openDirection")));

        if (openDirection.isEmpty())
            return Optional.empty();

        final @NotNull Vector3Di min = new Vector3Di(doorBaseRS.getInt("xMin"),
                                                     doorBaseRS.getInt("yMin"),
                                                     doorBaseRS.getInt("zMin"));
        final @NotNull Vector3Di max = new Vector3Di(doorBaseRS.getInt("xMax"),
                                                     doorBaseRS.getInt("yMax"),
                                                     doorBaseRS.getInt("zMax"));
        final @NotNull Vector3Di eng = new Vector3Di(doorBaseRS.getInt("engineX"),
                                                     doorBaseRS.getInt("engineY"),
                                                     doorBaseRS.getInt("engineZ"));
        final @NotNull Vector3Di pow = new Vector3Di(doorBaseRS.getInt("powerBlockX"),
                                                     doorBaseRS.getInt("powerBlockY"),
                                                     doorBaseRS.getInt("powerBlockZ"));

        final @NotNull IPWorld world = BigDoors.get().getPlatform().getPWorldFactory()
                                               .create(doorBaseRS.getString("world"));

        final long bitflag = doorBaseRS.getLong("bitflag");
        final boolean isOpen = IBitFlag.hasFlag(DoorFlag.getFlagValue(DoorFlag.IS_OPEN), bitflag);
        final boolean isLocked = IBitFlag.hasFlag(DoorFlag.getFlagValue(DoorFlag.IS_LOCKED), bitflag);

        final @NotNull String name = doorBaseRS.getString("name");

        final @NonNull PPlayerData playerData = new PPlayerData(UUID.fromString(doorBaseRS.getString("playerUUID")),
                                                                doorBaseRS.getString("playerName"),
                                                                doorBaseRS.getInt("sizeLimit"),
                                                                doorBaseRS.getInt("countLimit"),
                                                                doorBaseRS.getLong("permissions"));

        final @NotNull DoorOwner primeOwner = new DoorOwner(doorUID,
                                                            doorBaseRS.getInt("permission"),
                                                            playerData);

        final @NotNull Map<@NotNull UUID, @NotNull DoorOwner> doorOwners = getOwnersOfDoor(doorUID);
        final @NotNull AbstractDoorBase.DoorData doorData = new AbstractDoorBase.DoorData(doorUID, name, min, max, eng,
                                                                                          pow, world, isOpen, isLocked,
                                                                                          openDirection.get(),
                                                                                          primeOwner, doorOwners);

        final @NotNull Optional<? extends AbstractDoorBase> door = doorType.get().constructDoor(doorData,
                                                                                                typeDataSupplier.get());

        return door.map(abstractDoorBase -> abstractDoorBase);
    }

    /**
     * Generate the type-specific data for a door, represented as an array of Objects. The objects are retrieved as
     * defined by {@link DoorType#getParameters()}.
     *
     * @param doorBaseRS The {@link ResultSet} containing the type-specific data.
     * @return The array of Objects containing the type-specific data for a door.
     */
    private @NotNull Object @NotNull [] createTypeData(final @NotNull ResultSet doorBaseRS)
        throws Exception
    {
        final @NotNull Optional<DoorType> doorType =
            // There are 2 columns named "id" here. The first one is the ID of the DoorType, the second one
            // is the ID of the type-specific-table.
            DoorTypeManager.get().getDoorType(doorBaseRS.getInt(1));

        if (doorType.isEmpty())
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

    @Override
    public long registerDoorType(final @NotNull DoorType doorType)
    {
        final @NotNull String typeTableName = getTableNameOfType(doorType);
        if (!IStorage.isValidTableName(typeTableName))
        {
            PLogger.get().logThrowable(new IllegalArgumentException(
                "Invalid table name in database: \"" + typeTableName + "\". Please use only alphanumeric characters."));
            return -1;
        }

        final int updateStatus = executeUpdate(
            SQLStatement.INSERT_OR_IGNORE_DOOR_TYPE.constructPPreparedStatement()
                                                   .setString(1, doorType.getPluginName())
                                                   .setString(2, doorType.getSimpleName())
                                                   .setInt(3, doorType.getTypeVersion())
                                                   .setString(4, typeTableName));
        if (updateStatus == -1)
        {
            PLogger.get().logThrowable(
                new SQLException("Failed to create definition table for door type: \"" + typeTableName + "\""));
            return -1;
        }

        // FIXME: Read the data of the table to make sure that the parameters that were passed on to this method
        //        Are still up to date. If not, throw some kind of version mismatch exception or something.

        final @NotNull Optional<Pair<Long, String>> result = Optional.ofNullable(executeQuery(
            SQLStatement.GET_DOOR_TYPE_TABLE_NAME_AND_ID.constructPPreparedStatement()
                                                        .setString(1, doorType.getPluginName())
                                                        .setString(2, doorType.getSimpleName())
                                                        .setInt(3, doorType.getTypeVersion()),
            resultSet -> new Pair<>(resultSet.getLong("id"), resultSet.getString("typeTableName"))));

        if (result.isEmpty())
        {
            PLogger.get().logThrowable(
                new SQLException("Failed to obtain ID and typeTableName for door type: \"" + typeTableName + "\""));
            return -1;
        }

        // Create a new table for this DoorType, if needed.
        final @NotNull StringBuilder tableCreationStatementBuilder = new StringBuilder();
        tableCreationStatementBuilder.append("CREATE TABLE IF NOT EXISTS ").append(result.get().second)
                                     .append("(id INTEGER PRIMARY KEY AUTOINCREMENT, ")
                                     .append("doorUID REFERENCES DoorBase(id) ON UPDATE CASCADE ON DELETE CASCADE");
        for (final DoorType.Parameter parameter : doorType.getParameters())
            tableCreationStatementBuilder.append(", ").append(parameter.getParameterName()).append(" ")
                                         .append(parameter.getParameterType()).append(" NOT NULL");
        tableCreationStatementBuilder.append(", UNIQUE(doorUID));");

        final int insertStatus = executeUpdate(new PPreparedStatement(0, tableCreationStatementBuilder.toString()));

        // If the table creation failed, delete the entry from the DoorType table.
        if (insertStatus == -1)
        {
            PLogger.get().logThrowable(
                new SQLException("Failed to create type table: \"" + typeTableName + "\""));
            executeUpdate(SQLStatement.DELETE_DOOR_TYPE.constructPPreparedStatement().setLong(1, result.get().first));
            return -1;
        }

        IStorage.addToStatementMap(doorTypeStatementMap, doorType,
                                   generateInsertStatement(doorType), generateUpdateStatement(doorType));
        return result.get().first;
    }

    @Override
    public boolean deleteDoorType(final @NotNull DoorType doorType)
    {
        final long typeID = DoorTypeManager.get().getDoorTypeID(doorType).orElse(-1L);
        if (typeID == -1)
        {
            PLogger.get().logThrowable(new IllegalStateException(
                "Trying to delete door type: " + doorType.toString() +
                    ", but it is not registered! Please register it first."));
            return false;
        }

        final @NotNull String typeTableName = getTableNameOfType(doorType);
        if (!IStorage.isValidTableName(typeTableName))
        {
            PLogger.get().logThrowable(new IllegalArgumentException(
                "Invalid table name in database: \"" + typeTableName + "\". Please use only alphanumeric characters."));
            return false;
        }

        boolean removed = executeTransaction(
            conn ->
            {
                boolean result = executeUpdate(SQLStatement.DELETE_DOOR_TYPE.constructPPreparedStatement()
                                                                            .setLong(1, typeID)) > 0;
                if (result)
                    // Ths doesn't appear to return a positive value no matter its effect.
                    executeUpdate(new PPreparedStatement(0, "DROP TABLE " + typeTableName + ";"));
                return result;
            }, false);

        if (removed)
            DoorTypeManager.get().unregisterDoorType(doorType);
        return removed;
    }

    /**
     * Generates the insert statement for a given {@link DoorType}.
     *
     * @param doorType The type of door to generate the insert statement for.
     * @return A pair containing the statement and the number of arguments.
     */
    private @NotNull Pair<String, Integer> generateInsertStatement(final @NotNull DoorType doorType)
    {
        final @NotNull String typeTableName = getTableNameOfType(doorType);
        final @NotNull StringBuilder parameterNames = new StringBuilder();
        final @NotNull StringBuilder parameterQuestionMarks = new StringBuilder();

        parameterNames.append("INSERT INTO ").append(typeTableName).append(" (doorUID, ");
        parameterQuestionMarks.append(
            "((SELECT seq FROM sqlite_sequence WHERE sqlite_sequence.name =\"DoorBase\"), ");

        final int parameterCount = doorType.getParameterCount();
        for (int parameterIDX = 0; parameterIDX < parameterCount; ++parameterIDX)
        {
            final @NotNull DoorType.Parameter parameter = doorType.getParameters().get(parameterIDX);
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
        return new Pair<>(insertString, parameterCount);
    }

    /**
     * Generates the update statement for a given {@link DoorType}.
     *
     * @param doorType The type of door to generate the update statement for.
     * @return A pair containing the statement and the number of arguments.
     */
    private @NotNull Pair<String, Integer> generateUpdateStatement(final @NotNull DoorType doorType)
    {
        final @NotNull StringBuilder updateStatementBuilder = new StringBuilder();
        updateStatementBuilder.append("UPDATE ").append(getTableNameOfType(doorType)).append(" SET ");

        final @NotNull List<DoorType.Parameter> parameters = doorType.getParameters();
        final int parameterCount = parameters.size();
        for (int idx = 0; idx < parameterCount; ++idx)
        {
            updateStatementBuilder.append(parameters.get(idx).getParameterName()).append(" = ?");
            if (idx < (parameterCount - 1))
                updateStatementBuilder.append(", ");
        }
        updateStatementBuilder.append(" WHERE doorUID = ?;");
        return new Pair<>(updateStatementBuilder.toString(), parameterCount);
    }

    @Override
    public boolean syncBaseData(final @NotNull AbstractDoorBase.SimpleDoorData simpleDoorData)
    {
        return execute(conn -> syncBaseData(conn, simpleDoorData), false);
    }

    private boolean syncBaseData(final @NotNull Connection conn,
                                 final @NotNull AbstractDoorBase.SimpleDoorData simpleDoorData)
    {
        return executeUpdate(conn, SQLStatement.UPDATE_DOOR_BASE
            .constructPPreparedStatement()
            .setNextString(simpleDoorData.getName())
            .setNextString(simpleDoorData.getWorld().getWorldName())

            .setNextInt(simpleDoorData.getCuboid().getMin().getX())
            .setNextInt(simpleDoorData.getCuboid().getMin().getY())
            .setNextInt(simpleDoorData.getCuboid().getMin().getZ())

            .setNextInt(simpleDoorData.getCuboid().getMax().getX())
            .setNextInt(simpleDoorData.getCuboid().getMax().getY())
            .setNextInt(simpleDoorData.getCuboid().getMax().getZ())

            .setNextInt(simpleDoorData.getEngine().getX())
            .setNextInt(simpleDoorData.getEngine().getY())
            .setNextInt(simpleDoorData.getEngine().getZ())
            .setNextLong(Util.simpleChunkHashFromLocation(simpleDoorData.getEngine().getX(),
                                                          simpleDoorData.getEngine().getZ()))

            .setNextInt(simpleDoorData.getPowerBlock().getX())
            .setNextInt(simpleDoorData.getPowerBlock().getY())
            .setNextInt(simpleDoorData.getPowerBlock().getZ())
            .setNextLong(Util.simpleChunkHashFromLocation(simpleDoorData.getPowerBlock().getX(),
                                                          simpleDoorData.getPowerBlock().getZ()))

            .setNextInt(RotateDirection.getValue(simpleDoorData.getOpenDirection()))
            .setNextLong(getFlag(simpleDoorData.isOpen(), simpleDoorData.isLocked()))

            .setNextLong(simpleDoorData.getUid())) > 0;
    }

    @Override
    public boolean syncAllData(final @NotNull AbstractDoorBase.SimpleDoorData simpleDoorData,
                               final @NotNull DoorType doorType, final @NotNull Object[] typeData)
    {
        return executeTransaction(
            conn ->
            {
                final long doorUID = simpleDoorData.getUid();
                if (!syncBaseData(conn, simpleDoorData))
                    throw new SQLException("Failed to update base data of door: " + doorUID);
                if (!syncTypeData(conn, simpleDoorData.getUid(), doorType, typeData))
                    throw new IllegalStateException("Failed to update type-specific data of door: " + doorUID);
                return true;
            }, false);
    }

    @Override
    public boolean syncTypeData(final long doorUID, final @NotNull DoorType doorType, final @NotNull Object[] typeData)
    {
        return execute(conn -> syncTypeData(conn, doorUID, doorType, typeData), false);
    }

    private boolean syncTypeData(final @NotNull Connection conn, final long doorUID, final @NotNull DoorType doorType,
                                 final @NotNull Object[] typeData)
    {
        final @NotNull OptionalLong doorTypeID = DoorTypeManager.get().getDoorTypeID(doorType);
        if (doorTypeID.isEmpty())
        {
            PLogger.get().logThrowable(new SQLException(
                "Failed to update type-data of door: \"" + doorUID + "\"! Reason: DoorType not registered!"));
            return false;
        }

        final @NotNull Optional<DoorTypeDataStatementMap.DoorTypeDataStatement> typeSpecificDataUpdateStatementOpt =
            doorTypeStatementMap.getStatement(doorType, DoorTypeDataStatementMap.SQLStatementType.UPDATE);
        if (typeSpecificDataUpdateStatementOpt.isEmpty())
        {
            PLogger.get().logThrowable(new NullPointerException("Failed to obtain type-specific update statement " +
                                                                    "for type with ID: " + doorTypeID));
            return false;
        }

        final @NotNull DoorTypeDataStatementMap.DoorTypeDataStatement typeSpecificDataUpdateStatement
            = typeSpecificDataUpdateStatementOpt.get();

        final @NotNull PPreparedStatement pPreparedStatement =
            new PPreparedStatement(typeSpecificDataUpdateStatement.getArgumentCount() + 1,
                                   typeSpecificDataUpdateStatement.getStatement());

        for (int idx = 0; idx < typeSpecificDataUpdateStatement.getArgumentCount(); ++idx)
            pPreparedStatement.setObject(idx + 1, typeData[idx]);
        pPreparedStatement.setLong(typeSpecificDataUpdateStatement.getArgumentCount() + 1, doorUID);

        return executeUpdate(conn, pPreparedStatement) > 0;
    }

    /**
     * Constructs the name of the table used for the type-specific data of a {@link DoorType}.
     *
     * @param doorType The {@link DoorType}.
     * @return The name of the table.
     */
    private @NotNull String getTableNameOfType(final @NotNull DoorType doorType)
    {
        return String.format("%s_%s_%d", doorType.getPluginName(), doorType.getSimpleName(), doorType.getTypeVersion());
    }

    private Long insert(final @NotNull Connection conn, final @NotNull AbstractDoorBase door, final long doorTypeID,
                        final @NotNull Object[] typeSpecificData)
        throws Exception
    {
        final @NotNull Optional<DoorTypeDataStatementMap.DoorTypeDataStatement> typeSpecificDataInsertStatementOpt
            = doorTypeStatementMap.getStatement(door.getDoorType(), DoorTypeDataStatementMap.SQLStatementType.INSERT);
        if (typeSpecificDataInsertStatementOpt.isEmpty())
            throw new NullPointerException(
                "Failed to obtain type-specific insertion statement " + "for type with ID: " + doorTypeID);

        final @NotNull DoorTypeDataStatementMap.DoorTypeDataStatement typeSpecificDataInsertStatement =
            typeSpecificDataInsertStatementOpt.get();

        final @NonNull PPlayerData playerData = door.getPrimeOwner().getPlayer().getPPlayerData();
        insertOrIgnorePlayer(conn, playerData);

        final @NotNull String worldName = door.getWorld().getWorldName();
        final long engineHash = Util.simpleChunkHashFromLocation(door.getEngine().getX(), door.getEngine().getZ());
        final long powerBlockHash = Util
            .simpleChunkHashFromLocation(door.getPowerBlock().getX(), door.getPowerBlock().getZ());
        executeUpdate(conn, SQLStatement.INSERT_DOOR_BASE.constructPPreparedStatement()
                                                         .setNextString(door.getName())
                                                         .setNextString(worldName)
                                                         .setNextLong(doorTypeID)
                                                         .setNextInt(door.getMinimum().getX())
                                                         .setNextInt(door.getMinimum().getY())
                                                         .setNextInt(door.getMinimum().getZ())
                                                         .setNextInt(door.getMaximum().getX())
                                                         .setNextInt(door.getMaximum().getY())
                                                         .setNextInt(door.getMaximum().getZ())
                                                         .setNextInt(door.getEngine().getX())
                                                         .setNextInt(door.getEngine().getY())
                                                         .setNextInt(door.getEngine().getZ())
                                                         .setNextLong(engineHash)
                                                         .setNextInt(door.getPowerBlock().getX())
                                                         .setNextInt(door.getPowerBlock().getY())
                                                         .setNextInt(door.getPowerBlock().getZ())
                                                         .setNextLong(powerBlockHash)
                                                         .setNextLong(getFlag(door))
                                                         .setNextInt(RotateDirection.getValue(door.getOpenDir())));

        final @NotNull PPreparedStatement pPreparedStatement
            = new PPreparedStatement(typeSpecificDataInsertStatement.getArgumentCount(),
                                     typeSpecificDataInsertStatement.getStatement());

        for (int parameterIDX = 0; parameterIDX < typeSpecificDataInsertStatement.getArgumentCount(); ++parameterIDX)
            pPreparedStatement.setObject(parameterIDX + 1, typeSpecificData[parameterIDX]);

        executeUpdate(conn, pPreparedStatement);

        // TODO: Just use the fact that the last-inserted door has the current UID (that fact is already used by
        //       getTypeSpecificDataInsertStatement(DoorType)), so it can be done in a single statement.
        long doorUID = executeQuery(conn, SQLStatement.SELECT_MOST_RECENT_DOOR.constructPPreparedStatement(),
                                    rs -> rs.next() ? rs.getLong("seq") : -1, -1L);

        executeUpdate(conn, SQLStatement.INSERT_PRIME_OWNER.constructPPreparedStatement()
                                                           .setString(1, playerData.getUUID().toString()));

        return doorUID;
    }

    @Override
    public @NotNull Optional<AbstractDoorBase> insert(final @NonNull AbstractDoorBase door)
    {
        final @NotNull Optional<Object[]> typeSpecificDataOpt = door.getDoorType().getTypeData(door);
        if (typeSpecificDataOpt.isEmpty())
        {
            PLogger.get().logThrowable(new IllegalArgumentException(
                "Could not get type-specific data for a new door of type: " + door.getDoorType().toString()));
            return Optional.empty();
        }

        final long doorTypeID = DoorTypeManager.get().getDoorTypeID(door.getDoorType()).orElse(-1L);
        if (doorTypeID == -1)
        {
            PLogger.get()
                   .logThrowable(new SQLException("Could not find DoorType: " + door.getDoorType().toString()));
            return Optional.empty();
        }

        final long doorUID = executeTransaction(conn -> insert(conn, door, doorTypeID, typeSpecificDataOpt.get()), -1L);
        if (doorUID > 0)
            return door.getDoorType().constructDoor(
                new AbstractDoorBase.DoorData(doorUID, door.getName(), door.getMinimum(), door.getMaximum(),
                                              door.getEngine(), door.getPowerBlock(), door.getWorld(), door.isOpen(),
                                              door.isLocked(), door.getOpenDir(), door.getPrimeOwner()),
                typeSpecificDataOpt.get());

        return Optional.empty();
    }

    private void insertOrIgnorePlayer(final @NonNull Connection conn, final @NonNull PPlayerData playerData)
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
     * @param conn      The connection to the database.
     * @param doorOwner The doorOwner with the player to retrieve.
     * @return The database ID of the player.
     */
    private long getPlayerID(final @NonNull Connection conn, final @NonNull DoorOwner doorOwner)
    {
        insertOrIgnorePlayer(conn, doorOwner.getPlayer().getPPlayerData());

        return executeQuery(conn, SQLStatement.GET_PLAYER_ID.constructPPreparedStatement()
                                                            .setString(1, doorOwner.getPlayer().getUUID().toString()),
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
     */
    private @NotNull Optional<AbstractDoorBase> getDoor(final @NotNull ResultSet doorBaseRS)
        throws Exception
    {
        // Make sure the resultset isn't empty.
        if (!doorBaseRS.isBeforeFirst())
            return Optional.empty();

        if (!DoorTypeManager.get().isRegistered(doorBaseRS.getInt("doorType")))
        {
            PLogger.get().logThrowable(new IllegalStateException("Type with ID: " + doorBaseRS.getInt("doorType") +
                                                                     " has not been registered (yet)!"));
            return Optional.empty();
        }

        final @NotNull CheckedSupplier<Object[], Exception> typeDataSupplier = () ->
            executeQuery(SQLStatement.GET_TYPE_SPECIFIC_DATA.constructPPreparedStatement()
                                                            .setRawString(1, doorBaseRS.getString("typeTableName"))
                                                            .setString(2, doorBaseRS.getString("typeTableName"))
                                                            .setLong(3, doorBaseRS.getLong("id")),
                         this::createTypeData, new Object[]{});

        return constructDoor(doorBaseRS, typeDataSupplier);
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
     */
    private @NotNull List<AbstractDoorBase> getDoors(final @NotNull ResultSet doorBaseRS)
        throws Exception
    {
        // Make sure the resultset isn't empty.
        if (!doorBaseRS.isBeforeFirst())
            return Collections.emptyList();

        final @NotNull List<AbstractDoorBase> doors = new ArrayList<>();
        while (doorBaseRS.next())
        {
            if (!DoorTypeManager.get().isRegistered(doorBaseRS.getInt("doorType")))
            {
                PLogger.get().logThrowable(Level.FINE,
                                           new IllegalStateException("Type with ID: " + doorBaseRS.getInt("doorType") +
                                                                         " has not been registered (yet)!"));
                continue;
            }
            final @NotNull CheckedSupplier<Object[], Exception> typeDataSupplier = () ->
                executeQuery(SQLStatement.GET_TYPE_SPECIFIC_DATA.constructPPreparedStatement()
                                                                .setRawString(1, doorBaseRS.getString("typeTableName"))
                                                                .setString(2, doorBaseRS.getString("typeTableName"))
                                                                .setLong(3, doorBaseRS.getLong("id")),
                             this::createTypeData, new Object[]{});

            constructDoor(doorBaseRS, typeDataSupplier).ifPresent(doors::add);
        }
        return doors;
    }

    @Override
    public @NotNull Optional<AbstractDoorBase> getDoor(final long doorUID)
    {
        return executeQuery(SQLStatement.GET_DOOR_BASE_FROM_ID.constructPPreparedStatement()
                                                              .setLong(1, doorUID),
                            this::getDoor, Optional.empty());
    }

    @Override
    public @NotNull Optional<AbstractDoorBase> getDoor(final @NotNull UUID playerUUID, final long doorUID)
    {
        return executeQuery(SQLStatement.GET_DOOR_BASE_FROM_ID_FOR_PLAYER.constructPPreparedStatement()
                                                                         .setLong(1, doorUID)
                                                                         .setString(2, playerUUID.toString()),
                            this::getDoor, Optional.empty());
    }

    @Override
    public boolean removeDoor(final long doorUID)
    {
        return executeUpdate(SQLStatement.DELETE_DOOR.constructPPreparedStatement()
                                                     .setLong(1, doorUID)) > 0;
    }

    @Override
    public boolean removeDoors(final @NotNull String playerUUID, final @NotNull String doorName)
    {
        return executeUpdate(SQLStatement.DELETE_NAMED_DOOR_OF_PLAYER.constructPPreparedStatement()
                                                                     .setString(1, playerUUID)
                                                                     .setString(2, doorName)) > 0;
    }

    @Override
    public boolean isBigDoorsWorld(final @NotNull String worldName)
    {
        return executeQuery(SQLStatement.IS_BIGDOORS_WORLD.constructPPreparedStatement()
                                                          .setString(1, worldName),
                            ResultSet::next, false);
    }

    @Override
    public int getDoorCountForPlayer(final @NotNull UUID playerUUID)
    {
        return executeQuery(SQLStatement.GET_DOOR_COUNT_FOR_PLAYER.constructPPreparedStatement()
                                                                  .setString(1, playerUUID.toString()),
                            resultSet -> resultSet.next() ? resultSet.getInt("total") : -1, -1);
    }

    @Override
    public int getDoorCountForPlayer(final @NotNull UUID playerUUID, final @NotNull String doorName)
    {
        return executeQuery(SQLStatement.GET_PLAYER_DOOR_COUNT.constructPPreparedStatement()
                                                              .setString(1, playerUUID.toString())
                                                              .setString(2, doorName),
                            resultSet -> resultSet.next() ? resultSet.getInt("total") : -1, -1);
    }

    @Override
    public int getDoorCountByName(final @NotNull String doorName)
    {
        return executeQuery(SQLStatement.GET_DOOR_COUNT_BY_NAME.constructPPreparedStatement()
                                                               .setString(1, doorName),
                            resultSet -> resultSet.next() ? resultSet.getInt("total") : -1, -1);
    }

    @Override
    public int getOwnerCountOfDoor(final long doorUID)
    {
        return executeQuery(SQLStatement.GET_OWNER_COUNT_OF_DOOR.constructPPreparedStatement()
                                                                .setLong(1, doorUID),
                            resultSet -> resultSet.next() ? resultSet.getInt("total") : -1, -1);
    }

    @Override
    public @NotNull List<AbstractDoorBase> getDoors(final @NotNull String playerUUID,
                                                    final @NotNull String doorName,
                                                    final int maxPermission)
    {
        return executeQuery(SQLStatement.GET_NAMED_DOORS_OWNED_BY_PLAYER.constructPPreparedStatement()
                                                                        .setString(1, playerUUID)
                                                                        .setString(2, doorName)
                                                                        .setInt(3, maxPermission),
                            this::getDoors, Collections.emptyList());
    }

    @Override
    public @NotNull List<AbstractDoorBase> getDoors(final @NotNull UUID playerUUID,
                                                    final @NotNull String name)
    {
        return getDoors(playerUUID.toString(), name, 0);
    }

    @Override
    public @NotNull List<AbstractDoorBase> getDoors(final @NotNull String name)
    {
        return executeQuery(SQLStatement.GET_DOORS_WITH_NAME.constructPPreparedStatement()
                                                            .setString(1, name),
                            this::getDoors, Collections.emptyList());
    }

    @Override
    public @NotNull List<AbstractDoorBase> getDoors(final @NotNull String playerUUID, int maxPermission)
    {
        return executeQuery(SQLStatement.GET_DOORS_OWNED_BY_PLAYER_WITH_LEVEL.constructPPreparedStatement()
                                                                             .setString(1, playerUUID)
                                                                             .setInt(2, maxPermission),
                            this::getDoors, Collections.emptyList());
    }

    @Override
    public @NotNull List<AbstractDoorBase> getDoors(final @NotNull UUID playerUUID)
    {
        return getDoors(playerUUID.toString(), 0);
    }

    @Override
    public boolean updatePlayerData(final @NonNull PPlayerData playerData)
    {
        return executeUpdate(SQLStatement.UPDATE_PLAYER_DATA.constructPPreparedStatement()
                                                            .setNextString(playerData.getName())
                                                            .setNextInt(playerData.getDoorSizeLimit())
                                                            .setNextInt(playerData.getDoorCountLimit())
                                                            .setNextLong(playerData.getPermissionsFlag())
                                                            .setNextString(playerData.getUUID().toString())) > 0;
    }

    @Override
    public @NonNull Optional<PPlayerData> getPlayerData(final @NonNull UUID uuid)
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
    public @NonNull List<PPlayerData> getPlayerData(final @NonNull String playerName)
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
    public @NotNull ConcurrentHashMap<Integer, List<Long>> getPowerBlockData(final long chunkHash)
    {
        return executeQuery(SQLStatement.GET_POWER_BLOCK_DATA_IN_CHUNK.constructPPreparedStatement()
                                                                      .setLong(1, chunkHash),
                            resultSet ->
                            {
                                final ConcurrentHashMap<Integer, List<Long>> doors = new ConcurrentHashMap<>();
                                while (resultSet.next())
                                {
                                    int locationHash =
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
    public @NotNull List<Long> getDoorsInChunk(final long chunkHash)
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
            PLogger.get().logThrowable(e);
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
            PLogger.get().logThrowable(e);
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
            PLogger.get().logThrowable(e);
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
                PLogger.get().logThrowable(ex);
            }
        }
        catch (SQLException e)
        {
            PLogger.get().logThrowable(e);
        }
        return -1;
    }

    /**
     * Executes a query defined by a {@link PPreparedStatement}.
     *
     * @param pPreparedStatement The {@link PPreparedStatement}.
     * @return The {@link ResultSet} of the query, or null in case an error occurred.
     */
    private @Nullable <T> T executeQuery(final @NotNull PPreparedStatement pPreparedStatement,
                                         final @NotNull CheckedFunction<ResultSet, T, Exception> fun)
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
    private @Nullable <T> T executeQuery(final @NotNull PPreparedStatement pPreparedStatement,
                                         final @NotNull CheckedFunction<ResultSet, T, Exception> fun,
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
            PLogger.get().logThrowable(e);
        }
        return fallback;
    }

    /**
     * Executes a batched query defined by a {@link PPreparedStatement} and applies a function to the result.
     *
     * @param pPreparedStatement The {@link PPreparedStatement}.
     * @param fun                The function to apply to the {@link ResultSet}.
     * @param fallback           The value to return in case the result is null or if an error occurred.
     * @param <T>                The type of the result to return.
     * @return The {@link ResultSet} of the query, or null in case an error occurred.
     */
    @Contract(" _, _, !null -> !null;")
    private @Nullable <T> T executeBatchQuery(final @NotNull PPreparedStatement pPreparedStatement,
                                              final @NotNull CheckedFunction<ResultSet, T, Exception> fun,
                                              final @Nullable T fallback)
    {
        try (final @Nullable Connection conn = getConnection())
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
            PLogger.get().logThrowable(e);
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
                               final @NotNull CheckedFunction<ResultSet, T, Exception> fun,
                               final T fallback)
    {
        logStatement(pPreparedStatement);
        try (final PreparedStatement ps = pPreparedStatement.construct(conn);
             final ResultSet rs = ps.executeQuery())
        {
            return fun.apply(rs);
        }
        catch (Exception e)
        {
            PLogger.get().logThrowable(e);
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
    private <T> T execute(final @NotNull CheckedFunction<Connection, T, Exception> fun, final T fallback)
    {
        return execute(fun, fallback, FailureAction.IGNORE);
    }

    /**
     * Executes a {@link CheckedFunction} given an active Connection.
     *
     * @param fun           The function to execute.
     * @param fallback      The fallback value to return in case of failure.
     * @param <T>           The type of the result to return.
     * @param failureAction The action to take when an exception is caught.
     * @return The result of the Function.
     */
    @Contract(" _, !null, _ -> !null")
    private <T> T execute(final @NotNull CheckedFunction<Connection, T, Exception> fun,
                          final T fallback, final FailureAction failureAction)
    {
        try (final @Nullable Connection conn = getConnection())
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
                PLogger.get().logThrowable(e);
            }
        }
        catch (Exception e)
        {
            PLogger.get().logThrowable(e);
        }
        return fallback;
    }

    /**
     * Executes a {@link CheckedFunction} given an active Connection as a transaction. In case an error was caught, it
     * will attempt to roll back to the state before the
     *
     * @param fun      The function to execute.
     * @param fallback The fallback value to return in case of failure.
     * @param <T>      The type of the result to return.
     * @return The result of the Function.
     */
    @Contract(" _, !null -> !null")
    private <T> T executeTransaction(final @NotNull CheckedFunction<Connection, T, Exception> fun, final T fallback)
    {
        return execute(
            conn ->
            {
                conn.setAutoCommit(false);
                T result = fun.apply(conn);
                conn.commit();
                return result;
            }, fallback, FailureAction.ROLLBACK);
    }

    /**
     * Logs a {@link PPreparedStatement} to the logger.
     *
     * @param pPreparedStatement The {@link PPreparedStatement} to log.
     */
    private void logStatement(final @NotNull PPreparedStatement pPreparedStatement)
    {
        PLogger.get().logMessage(Level.ALL, "Executed statement:", pPreparedStatement::toString);
    }

    @Override
    public boolean removeOwner(final long doorUID, final @NotNull String playerUUID)
    {
        return executeUpdate(SQLStatement.REMOVE_DOOR_OWNER.constructPPreparedStatement()
                                                           .setString(1, playerUUID)
                                                           .setLong(2, doorUID)) > 0;
    }

    private @NotNull Map<UUID, DoorOwner> getOwnersOfDoor(final long doorUID)
    {
        return executeQuery(SQLStatement.GET_DOOR_OWNERS.constructPPreparedStatement()
                                                        .setLong(1, doorUID),
                            resultSet ->
                            {
                                final Map<UUID, DoorOwner> ret = new HashMap<>();
                                while (resultSet.next())
                                {
                                    final @NotNull UUID uuid = UUID.fromString(resultSet.getString("playerUUID"));
                                    final @NonNull PPlayerData playerData =
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
    public boolean addOwner(final long doorUID, final @NonNull IPPlayer player, final int permission)
    {
        // permission level 0 is reserved for the creator, and negative values are not allowed.
        if (permission < 1)
            return false;

        return executeTransaction(
            conn ->
            {
                final long playerID = getPlayerID(conn, new DoorOwner(doorUID, permission, player));

                if (playerID == -1)
                    throw new IllegalArgumentException(
                        "Trying to add player \"" + player.getUUID().toString() + "\" as owner of door " + doorUID +
                            ", but that player is not registered in the database! Aborting...");

                return executeQuery(
                    conn, SQLStatement.GET_DOOR_OWNER_PLAYER.constructPPreparedStatement()
                                                            .setLong(1, playerID)
                                                            .setLong(2, doorUID),
                    rs ->
                    {
                        SQLStatement statement = (rs.next() && (rs.getInt("permission") != permission)) ?
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

    @Override
    public @NotNull DatabaseState getDatabaseState()
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
            PLogger.get().logMessage(Level.SEVERE, "Failed to obtain database version!");
            databaseState = DatabaseState.ERROR;
            return dbVersion;
        }

        if (dbVersion == DATABASE_VERSION)
        {
            databaseState = DatabaseState.OK;
        }

        if (dbVersion < MIN_DATABASE_VERSION)
        {
            PLogger.get().logMessage(Level.SEVERE, "Trying to load database version " + dbVersion +
                " while the minimum allowed version is " + MIN_DATABASE_VERSION);
            databaseState = DatabaseState.TOO_OLD;
        }

        if (dbVersion > DATABASE_VERSION)
        {
            PLogger.get().logMessage(Level.SEVERE, "Trying to load database version " + dbVersion +
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

            conn.close();
            if (!makeBackup())
                return;
            conn = getConnection(DatabaseState.OUT_OF_DATE);
            if (conn == null)
                return;

            if (dbVersion < 11)
                upgradeToV11(conn);

            // Do this at the very end, so the db version isn't altered if anything fails.
            setDBVersion(conn, DATABASE_VERSION);
            databaseState = DatabaseState.OK;
        }
        catch (SQLException | NullPointerException e)
        {
            PLogger.get().logThrowable(e);
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
        // Only the most recent backup is kept, so delete the old one if a new one needs to be created.
        if (dbFileBackup.exists() && !dbFileBackup.delete())
        {
            PLogger.get().logThrowable(new IOException("Failed to delete old backup! Aborting backup creation!"));
            return false;
        }
        try
        {
            Files.copy(dbFile.toPath(), dbFileBackup.toPath());
        }
        catch (IOException e)
        {
            PLogger.get().logThrowable(e, "Failed to create backup of the database! "
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
            PLogger.get().logThrowable(e);
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
            PLogger.get().logThrowable(e);
        }
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
        ;
    }
}
