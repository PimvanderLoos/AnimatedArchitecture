package nl.pim16aap2.bigdoors.spigot.util.implementations.pexecutor;

import dagger.Binds;
import dagger.Module;
import nl.pim16aap2.bigdoors.api.IPExecutor;

import javax.inject.Singleton;

@Module
public interface PExecutorModule
{
    @Binds
    @Singleton
    IPExecutor getExecutor(PExecutorSpigot executorSpigot);
}
