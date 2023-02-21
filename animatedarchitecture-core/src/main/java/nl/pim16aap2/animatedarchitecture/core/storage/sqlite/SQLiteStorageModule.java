package nl.pim16aap2.animatedarchitecture.core.storage.sqlite;

import dagger.Binds;
import dagger.Module;
import nl.pim16aap2.animatedarchitecture.core.storage.IStorage;

import javax.inject.Singleton;

@Module
public interface SQLiteStorageModule
{
    @Binds
    @Singleton
    IStorage bindStorage(SQLiteJDBCDriverConnection storage);
}
