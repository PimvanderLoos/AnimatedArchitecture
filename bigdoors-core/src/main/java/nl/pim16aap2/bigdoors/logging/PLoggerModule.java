package nl.pim16aap2.bigdoors.logging;

import dagger.Binds;
import dagger.Module;

import javax.inject.Singleton;

@Module
public interface PLoggerModule
{
    @Binds
    @Singleton
    IPLogger getLogger(PLogger logger);
}
