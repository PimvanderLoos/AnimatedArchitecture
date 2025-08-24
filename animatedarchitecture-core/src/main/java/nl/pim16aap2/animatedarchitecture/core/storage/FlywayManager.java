package nl.pim16aap2.animatedarchitecture.core.storage;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import lombok.CustomLog;
import nl.pim16aap2.animatedarchitecture.core.api.debugging.DebuggableRegistry;
import nl.pim16aap2.animatedarchitecture.core.api.debugging.IDebuggable;
import nl.pim16aap2.animatedarchitecture.core.util.StringUtil;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Manages the Flyway migrations.
 */
@CustomLog
@Singleton
public final class FlywayManager implements IDebuggable
{
    private final IDataSourceInfo dataSourceInfo;

    private final ClassLoader classLoader;

    private final AtomicBoolean isMigrationPerformed = new AtomicBoolean(false);

    private final Flyway flyway;

    @Inject
    FlywayManager(
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
            log.atWarn().log("Migration has already been performed. Skipping.");
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
                Migration performed: %s
                Current Migration:
                - %s
                Migrations Pending: %s
                Migrations Applied: %s
                """,
            isMigrationPerformed.get() ? "Yes" : "No",
            formatMigration(flyway.info().current()),
            StringUtil.formatCollection(List.of(flyway.info().pending()), this::formatMigration),
            StringUtil.formatCollection(List.of(flyway.info().applied()), this::formatMigration)
        );
    }

    private String formatMigration(MigrationInfo migration)
    {
        return String.format("%s: %s", migration.getVersion(), migration.getDescription());
    }
}
