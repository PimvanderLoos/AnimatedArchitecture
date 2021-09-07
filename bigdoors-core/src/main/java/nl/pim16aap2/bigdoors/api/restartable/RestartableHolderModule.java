package nl.pim16aap2.bigdoors.api.restartable;

import dagger.Module;
import dagger.Provides;

import javax.inject.Singleton;

@Module
public class RestartableHolderModule
{
    private final IRestartableHolder restartableHolder;

    public RestartableHolderModule(IRestartableHolder restartableHolder)
    {
        this.restartableHolder = restartableHolder;
    }

    @Provides
    @Singleton
    public IRestartableHolder restartableHolder()
    {
        return restartableHolder;
    }
}
