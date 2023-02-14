package nl.pim16aap2.bigdoors.spigot.util.implementations;

import dagger.Binds;
import dagger.Module;
import nl.pim16aap2.bigdoors.core.api.IExecutor;

import javax.inject.Singleton;

@Module
public interface ExecutorModule
{
    @Binds
    @Singleton
    IExecutor getExecutor(ExecutorSpigot executorSpigot);
}
