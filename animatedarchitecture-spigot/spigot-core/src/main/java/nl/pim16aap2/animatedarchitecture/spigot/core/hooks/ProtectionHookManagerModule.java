package nl.pim16aap2.animatedarchitecture.spigot.core.hooks;

import dagger.Binds;
import dagger.Module;
import jakarta.inject.Singleton;
import nl.pim16aap2.animatedarchitecture.core.api.IProtectionHookManager;

@Module
public abstract class ProtectionHookManagerModule
{
    @Binds
    @Singleton
    abstract IProtectionHookManager provideProtectionHookManager(ProtectionHookManagerSpigot manager);
}
