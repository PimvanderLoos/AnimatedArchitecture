package nl.pim16aap2.animatedarchitecture.spigot.core.managers;

import dagger.Binds;
import dagger.Module;
import jakarta.inject.Singleton;
import nl.pim16aap2.animatedarchitecture.core.api.IRedstoneManager;

@Module
public interface PowerBlockRedstoneManagerSpigotModule
{
    @Binds
    @Singleton
    IRedstoneManager getPowerBlockRedstoneManager(RedstoneManagerSpigot manager);
}
