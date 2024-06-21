package nl.pim16aap2.animatedarchitecture.core.storage;

import lombok.Getter;
import org.flywaydb.core.api.configuration.FluentConfiguration;

import javax.sql.DataSource;

/**
 * Represents the data needed to connect to a DataSource.
 */
public interface IDataSourceInfo
{
    /**
     * The location of the migration files for the DataSource.
     * <p>
     * This is a format string that takes the type of the DataSource as an argument.
     * <p>
     * To get the final location for the implementation, use {@link #getMigrationFilesLocation()} instead.
     */
    String DB_MIGRATION_LOCATION = "classpath:db/migration/%s";

    /**
     * Gets the DataSource.
     *
     * @return The DataSource.
     */
    DataSource getDataSource();

    /**
     * Gets the location of the migration files for the DataSource.
     *
     * @return The location of the migration files for the DataSource.
     */
    default String getMigrationFilesLocation()
    {
        return String.format(DB_MIGRATION_LOCATION, getType().getTypeName());
    }

    /**
     * Gets the type of the DataSource.
     *
     * @return The type of the DataSource.
     */
    Type getType();

    /**
     * Backs up the database.
     * <p>
     * This method is optional for implementations.
     * <p>
     * On implementations that do not support this method, this method should do nothing.
     */
    default void backupDatabase()
    {
    }

    /**
     * Updates the Flyway configuration with configuration specific to the DataSource.
     * <p>
     * By default, this method sets the DataSource and the location of the migration files.
     *
     * @param config
     *     The Flyway configuration to update.
     */
    default void configureFlyway(FluentConfiguration config)
    {
        config
            .dataSource(getDataSource())
            .locations(getMigrationFilesLocation());
    }

    /**
     * The type of the DataSource.
     */
    enum Type
    {
        SQLITE("sqlite"),
        ;

        /**
         * The name of the type.
         * <p>
         * This is the name that should be used in the migration files location.
         */
        @Getter
        private final String typeName;

        Type(String typeName)
        {
            this.typeName = typeName;
        }
    }
}
