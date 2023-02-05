package nl.pim16aap2.bigdoors.core.localization;

import dagger.Module;
import dagger.Provides;

import javax.inject.Singleton;

@Module
public class LocalizationModule
{
    @Provides
    @Singleton
    static ILocalizer getLocalizer(LocalizationManager generator)
    {
        return generator.getLocalizer();
    }
}
