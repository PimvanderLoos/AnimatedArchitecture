package nl.pim16aap2.animatedarchitecture.core.storage.sqlite;

import lombok.Getter;
import lombok.extern.flogger.Flogger;
import nl.pim16aap2.animatedarchitecture.core.storage.IDataSourceInfo;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.jetbrains.annotations.Nullable;
import org.sqlite.JDBC;
import org.sqlite.SQLiteDataSource;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.OptionalInt;

/**
 * Represents the data needed to connect to a {@code SQLite} DataSource.
 */
@Flogger
public class DataSourceInfoSQLite implements IDataSourceInfo
{
    private static final Type TYPE = Type.SQLITE;

    /**
     * Magic version number to indicate that the database is managed by Flyway.
     * <p>
     * This is higher than the highest number used before Flyway was introduced, so that Flyway-managed databases cannot
     * be used on older versions of the software.
     */
    private static final int FLYWAY_MANAGED_DATABASE_VERSION = 1000;

    @Getter
    private final SQLiteDataSource dataSource;

    private final Path databasePath;

    @Getter
    private final String url;

    @Inject
    public DataSourceInfoSQLite(@Named("databaseFile") Path databasePath)
    {
        this.databasePath = databasePath;
        this.url = JDBC.PREFIX + databasePath;

        if (!JDBC.isValidURL(this.url))
            throw new IllegalArgumentException("Invalid database URL: '" + this.url + "'");

        final var sqliteDataSource = new SQLiteDataSource();
        sqliteDataSource.setEnforceForeignKeys(true);
        sqliteDataSource.setUrl(String.format(this.url, databasePath));
        this.dataSource = sqliteDataSource;
    }

    @Override
    public void configureFlyway(FluentConfiguration config)
    {
        IDataSourceInfo.super.configureFlyway(config);

        final @Nullable BaselineVersion baselineVersion = handleOldDatabase();
        if (baselineVersion != null)
            // Because the SQLite database already existed before Flyway was introduced,
            // we use a baseline version to get it under Flyway's control.
            config
                .baselineVersion(baselineVersion.versionString())
                .baselineOnMigrate(true);
    }

    /**
     * Handles the old database if it exists.
     * <p>
     * If the 'new' database exists, the old database will be ignored.
     * <p>
     * If the old database exists, it will be copied to the new location, so it can be used as a baseline for Flyway.
     * <p>
     * The old database will be moved to a backup location after the baseline version has been set. Therefore, running
     * this method multiple times will not cause the old database to be used as a baseline multiple times.
     *
     * @return The baseline version to use for Flyway or {@code null} if the old database doesn't exist.
     */
    private @Nullable BaselineVersion handleOldDatabase()
    {
        if (Files.exists(databasePath))
        {
            log.atFinest().log("New database already exists. Not using existing database as baseline.");
            return null;
        }

        final Path oldDatabasePath = databasePath.resolveSibling("structures.db");
        if (!Files.exists(oldDatabasePath))
        {
            log.atFinest().log("Old database doesn't exist. Not using existing database as baseline.");
            return null;
        }

        // Copy the old database to the new location if it doesn't exist yet.
        copyOldDatabase(oldDatabasePath);

        final var currentVersion = getCurrentDatabaseVersion();
        if (currentVersion.isEmpty())
        {
            log.atWarning().log("Failed to get the current database version. Not using existing database as baseline.");
            return null;
        }

        final int version = currentVersion.getAsInt();
        log.atInfo().log("Found old database version: %d", version);

        // Move the old database, so it won't get processed again in future runs.
        moveOldDatabase(oldDatabasePath, version);

        if (version == FLYWAY_MANAGED_DATABASE_VERSION)
        {
            log.atWarning().log("Database is already managed by Flyway. Not using existing database as baseline.");
            return null;
        }

        // Create the baseline data object here to run validation before doing anything else.
        final var baselineVersion = new BaselineVersion(version);

        log.atInfo().log("Using old database version '%d' as baseline for Flyway.", version);

        // Set the current version to the Flyway-managed version to prevent future backups.
        setCurrentDatabaseVersion(FLYWAY_MANAGED_DATABASE_VERSION);

        return baselineVersion;
    }

    @Override
    public Type getType()
    {
        return TYPE;
    }

    /**
     * Gets the current version of the database.
     * <p>
     * This is not the version as managed by Flyway, but the manually-set version. This should be either 100 or 101.
     *
     * @return The current version of the database or an empty optional if not available.
     */
    private OptionalInt getCurrentDatabaseVersion()
    {
        try (
            var connection = dataSource.getConnection();
            var statement = connection.createStatement();
            var resultSet = statement.executeQuery("PRAGMA user_version;"))
        {
            if (resultSet.next())
                return OptionalInt.of(resultSet.getInt(1));
        }
        catch (Exception e)
        {
            throw new RuntimeException("Failed to get the current database version!", e);
        }
        return OptionalInt.empty();
    }

    /**
     * Sets the current version of the database using {@code SQLite}'s PRAGMA user_version.
     * <p>
     * This is not the version as managed by Flyway, but the manually-set version. This should be either 100 or 101.
     *
     * @param version
     *     The version to set the database to.
     */
    private void setCurrentDatabaseVersion(int version)
    {
        try (
            var connection = dataSource.getConnection();
            var statement = connection.createStatement())
        {
            statement.execute("PRAGMA user_version = " + version + ";");
        }
        catch (Exception e)
        {
            throw new RuntimeException("Failed to set the current database version!", e);
        }
    }

    @Override
    public void backupDatabase()
    {
        final Path backupPath = databasePath.resolveSibling(databasePath.getFileName() + ".backup");
        backupDatabase(backupPath);
    }

    /**
     * Creates a backup of the database.
     * <p>
     * The backup will be created in the same directory as the database with the same name, but with the extension
     * {@code .backup}.
     *
     * @param backupPath
     *     The path to create the backup at.
     */
    private void backupDatabase(Path backupPath)
    {
        if (!Files.exists(databasePath))
        {
            log.atFine().log("Database doesn't exist. Not creating a backup.");
            return;
        }
        try
        {
            // Only the most recent backup is kept, so replace any existing backups.
            Files.copy(databasePath, backupPath, StandardCopyOption.REPLACE_EXISTING);
        }
        catch (IOException e)
        {
            log.atSevere().withCause(e).log("Failed to create backup of the database!");
        }
    }

    /**
     * Copies the old database to the new location.
     * <p>
     * If the new database already exists, the old database will not be copied.
     *
     * @param oldDatabasePath
     *     The path to the old database.
     */
    private void copyOldDatabase(Path oldDatabasePath)
    {
        // If the old database doesn't exist, there is nothing to move.
        if (!Files.exists(oldDatabasePath))
        {
            log.atWarning().log("Old database doesn't exist. Not copying the old database.");
            return;
        }

        // If the new database doesn't exist, copy the old database to the new location.
        if (Files.exists(databasePath))
        {
            log.atWarning().log("New database already exists. Not copying the old database.");
            return;
        }

        try
        {
            Files.copy(oldDatabasePath, databasePath);
        }
        catch (IOException e)
        {
            throw new RuntimeException("Failed to move the old database to the new location!", e);
        }
    }

    /**
     * Moves the old database to a backup location.
     * <p>
     * The old database will be moved to the same directory as the new database with the same name, but with the version
     * appended to the name.
     *
     * @param oldDatabasePath
     *     The path to the old database.
     * @param version
     *     The version of the old database. This will be used in the backup name.
     */
    private void moveOldDatabase(Path oldDatabasePath, int version)
    {
        // If the old database doesn't exist, there is nothing to move.
        if (!Files.exists(oldDatabasePath))
        {
            log.atWarning().log("Old database doesn't exist. Not moving the old database.");
            return;
        }

        final Path targetPath = databasePath.resolveSibling(oldDatabasePath.getFileName() + ".v" + version + ".backup");
        try
        {
            Files.move(oldDatabasePath, targetPath);
        }
        catch (IOException e)
        {
            throw new RuntimeException("Failed to move the old database to the new location!", e);
        }
    }

    @Override
    public String toString()
    {
        return "DataSourceInfoSQLite{" +
            "url='" + url + '\'' +
            '}';
    }

    /**
     * Represents the baseline data for the database.
     * <p>
     * This is used to set the baseline version of the database when migrating to Flyway if the database already existed
     * before Flyway was introduced.
     *
     * @param version
     *     The version to baseline the database to. This should be either 100 or 101.
     */
    private record BaselineVersion(int version)
    {
        BaselineVersion
        {
            if (version != 100 && version != 101)
                throw new IllegalArgumentException("Invalid baseline version: " + version);
        }

        /**
         * Gets the version as a string.
         *
         * @return The version as a string.
         */
        String versionString()
        {
            return Integer.toString(version);
        }
    }
}
