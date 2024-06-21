package nl.pim16aap2.animatedarchitecture.core.storage.sqlite;

import lombok.Getter;
import lombok.extern.flogger.Flogger;
import nl.pim16aap2.animatedarchitecture.core.storage.IDataSourceInfo;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.sqlite.JDBC;
import org.sqlite.SQLiteDataSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.OptionalInt;

/**
 * Represents the data needed to connect to a SQLite DataSource.
 */
@Flogger
public class DataSourceInfoSQLite implements IDataSourceInfo
{
    private static final String TYPE = "sqlite";
    private static final String MIGRATION_FILES_LOCATION = String.format(DB_MIGRATION_LOCATION, TYPE);

    @Getter
    private final SQLiteDataSource dataSource;

    private final Path databasePath;

    @Getter
    private final String url;

    public DataSourceInfoSQLite(Path databasePath)
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

        final var currentVersion = getCurrentDatabaseVersion();

        if (currentVersion.isEmpty())
            return;

        final int version = currentVersion.getAsInt();
        if (version != 100 && version != 101)
        {
            log.atWarning().log("Unknown database version: %d. Skipping migration.", version);
            return;
        }

        log.atInfo().log("Migrating database to Flyway with baseline version '%d'.", version);

        // Make a special pre-flyway migration backup to prevent
        // the backup being overridden by future migration backups.
        backupDatabase(databasePath.resolveSibling(databasePath.getFileName() + ".v" + version + ".backup"));

        // Because the SQLite database already existed before Flyway was introduced,
        // we use a baseline version to get it under Flyway's control.
        config
            .baselineVersion(Integer.toString(version))
            .baselineOnMigrate(true);
    }

    @Override
    public String getMigrationFilesLocation()
    {
        return MIGRATION_FILES_LOCATION;
    }

    @Override
    public String getType()
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
            log.atSevere().withCause(e).log("Failed to get the current database version!");
        }
        return OptionalInt.empty();
    }

    @Override
    public void backupDatabase()
    {
        final Path backupPath = databasePath.resolveSibling(databasePath.getFileName() + ".backup");
        backupDatabase(backupPath);
    }

    private void backupDatabase(Path backupPath)
    {
        if (!Files.exists(databasePath))
            return;

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
}
