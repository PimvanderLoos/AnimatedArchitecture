package nl.pim16aap2.bigdoors.spigot.core.implementations;

import dagger.Binds;
import dagger.Module;
import nl.pim16aap2.bigdoors.core.api.debugging.DebugReporter;

import javax.inject.Singleton;

@Module
public interface DebugReporterSpigotModule
{
    @Binds
    @Singleton
    DebugReporter bindDebugReporter(DebugReporterSpigot debugReporterSpigot);
}
