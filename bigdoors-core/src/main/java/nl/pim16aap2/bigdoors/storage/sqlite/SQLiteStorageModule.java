package nl.pim16aap2.bigdoors.storage.sqlite;

import dagger.Binds;
import dagger.Module;
import nl.pim16aap2.bigdoors.storage.IStorage;

import javax.inject.Singleton;

@Module
public interface SQLiteStorageModule
{
    @Binds
    @Singleton
    IStorage bindStorage(SQLiteJDBCDriverConnection storage);
}
