package nl.pim16aap2.bigdoors.spigot.compatiblity;

import dagger.Binds;
import dagger.Module;
import nl.pim16aap2.bigdoors.api.IProtectionCompatManager;

import javax.inject.Singleton;

@Module
public abstract class ProtectionCompatManagerModule
{
    @Binds
    @Singleton
    abstract IProtectionCompatManager provideProtectionCompatManager(ProtectionCompatManagerSpigot manager);
}
