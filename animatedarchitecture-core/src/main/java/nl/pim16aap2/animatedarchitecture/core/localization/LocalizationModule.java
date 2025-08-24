package nl.pim16aap2.animatedarchitecture.core.localization;

import dagger.Module;
import dagger.Provides;
import jakarta.inject.Singleton;

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
