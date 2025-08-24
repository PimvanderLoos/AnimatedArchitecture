package nl.pim16aap2.animatedarchitecture.spigot.core.implementations;

import dagger.Binds;
import dagger.Module;
import jakarta.inject.Singleton;
import nl.pim16aap2.animatedarchitecture.core.api.debugging.DebugReporter;

@Module
public interface DebugReporterSpigotModule
{
    @Binds
    @Singleton
    DebugReporter bindDebugReporter(DebugReporterSpigot debugReporterSpigot);
}
