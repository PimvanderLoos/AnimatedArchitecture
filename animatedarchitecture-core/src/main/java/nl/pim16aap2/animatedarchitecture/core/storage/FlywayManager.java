package nl.pim16aap2.animatedarchitecture.core.storage;

import lombok.extern.flogger.Flogger;
import nl.pim16aap2.animatedarchitecture.core.api.debugging.DebuggableRegistry;
import nl.pim16aap2.animatedarchitecture.core.api.debugging.IDebuggable;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Manages the Flyway migrations.
 */
@Flogger
@Singleton
public final class FlywayManager implements IDebuggable
{
    private final IDataSourceInfo dataSourceInfo;

    private final ClassLoader classLoader;

    private final AtomicBoolean isMigrationPerformed = new AtomicBoolean(false);

    private final Flyway flyway;

    @Inject
    FlywayManager(
        @Named("pluginBaseDirectory") Path baseDirectory,
        @Named("pluginClassLoader") ClassLoader classLoader,
        IDataSourceInfo dataSourceInfo,
        DebuggableRegistry debuggableRegistry
    )
    {
        this.classLoader = classLoader;
        this.dataSourceInfo = dataSourceInfo;

        this.flyway = configureFlyway();

        debuggableRegistry.registerDebuggable(this);
    }

    public void migrate()
    {
        if (isMigrationPerformed.getAndSet(true))
        {
            log.atWarning().log("Migration has already been performed. Skipping.");
            return;
        }

        // Make a backup of the database before migrating if there are migrations.
        if (flyway.info().pending().length > 0)
            dataSourceInfo.backupDatabase();

        flyway.migrate();
        flyway.validate();
    }

    private Flyway configureFlyway()
    {
        final var config = Flyway
            .configure(classLoader)
            .failOnMissingLocations(true)
            .loggers("slf4j")
            .validateMigrationNaming(true);

        dataSourceInfo.configureFlyway(config);

        return config.load();
    }

    @Override
    public String getDebugInformation()
    {
        return String.format(
            """
                - Migration performed: %s
                - Migrations Applied (%d): %s
                - Migrations Pending (%d): %s
                - Current Migration: %s
                """,
            isMigrationPerformed.get() ? "Yes" : "No",
            flyway.info().applied().length,
            formatMigrations(flyway.info().applied()),
            flyway.info().pending().length,
            formatMigrations(flyway.info().pending()),
            formatMigrations(flyway.info().current())
        );
    }

    private String formatMigrations(MigrationInfo... all)
    {
        if (all.length == 0)
            return "";
        final var sb = new StringBuilder("\n");
        Arrays.stream(all)
            .map(migration -> String.format("  - %s: %s\n", migration.getVersion(), migration.getDescription()))
            .forEach(sb::append);

        // Return the string without the trailing newline.
        return sb.substring(0, sb.length() - 1);
    }
}
