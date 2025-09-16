package nl.pim16aap2.animatedarchitecture.core.storage.sqlite;

import dagger.Binds;
import dagger.Module;
import jakarta.inject.Singleton;
import nl.pim16aap2.animatedarchitecture.core.storage.IDataSourceInfo;
import nl.pim16aap2.animatedarchitecture.core.storage.IStorage;

@Module
public interface SQLiteStorageModule
{
    @Binds
    @Singleton
    IStorage bindStorage(SQLiteJDBCDriverConnection storage);

    @Binds
    @Singleton
    IDataSourceInfo bindDataSourceInfo(DataSourceInfoSQLite dataSourceInfoSQLite);
}
