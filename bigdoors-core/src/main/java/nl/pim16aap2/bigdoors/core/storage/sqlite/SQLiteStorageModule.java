package nl.pim16aap2.bigdoors.core.storage.sqlite;

import dagger.Binds;
import dagger.Module;
import nl.pim16aap2.bigdoors.core.storage.IStorage;

import javax.inject.Singleton;

@Module
public interface SQLiteStorageModule
{
    @Binds
    @Singleton
    IStorage bindStorage(SQLiteJDBCDriverConnection storage);
}
