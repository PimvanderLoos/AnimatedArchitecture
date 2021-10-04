package nl.pim16aap2.bigdoors.spigot.util;

import dagger.Binds;
import dagger.Module;
import nl.pim16aap2.bigdoors.api.debugging.DebugReporter;

import javax.inject.Singleton;

@Module
public interface DebugReporterSpigotModule
{
    @Binds
    @Singleton
    DebugReporter bindDebugReporter(DebugReporterSpigot debugReporterSpigot);
}
