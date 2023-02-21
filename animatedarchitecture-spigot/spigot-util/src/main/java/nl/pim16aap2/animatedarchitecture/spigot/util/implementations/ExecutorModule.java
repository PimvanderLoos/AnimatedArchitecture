package nl.pim16aap2.animatedarchitecture.spigot.util.implementations;

import dagger.Binds;
import dagger.Module;
import nl.pim16aap2.animatedarchitecture.core.api.IExecutor;

import javax.inject.Singleton;

@Module
public interface ExecutorModule
{
    @Binds
    @Singleton
    IExecutor getExecutor(ExecutorSpigot executorSpigot);
}
