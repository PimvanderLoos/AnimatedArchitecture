package nl.pim16aap2.animatedarchitecture.spigot.util.implementations;

import dagger.Binds;
import dagger.Module;
import jakarta.inject.Singleton;
import nl.pim16aap2.animatedarchitecture.core.api.IExecutor;

@Module
public interface ExecutorModule
{
    @Binds
    @Singleton
    IExecutor getExecutor(ExecutorSpigot executorSpigot);
}
