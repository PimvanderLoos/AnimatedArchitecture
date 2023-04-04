package nl.pim16aap2.animatedarchitecture.spigot.core.hooks;

import dagger.Binds;
import dagger.Module;
import nl.pim16aap2.animatedarchitecture.core.api.IProtectionHookManager;

import javax.inject.Singleton;

@Module
public abstract class ProtectionHookManagerModule
{
    @Binds
    @Singleton
    abstract IProtectionHookManager provideProtectionHookManager(ProtectionHookManagerSpigot manager);
}
