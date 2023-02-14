package nl.pim16aap2.bigdoors.spigot.core.managers;

import dagger.Binds;
import dagger.Module;
import nl.pim16aap2.bigdoors.core.api.IRedstoneManager;

import javax.inject.Singleton;

@Module
public interface PowerBlockRedstoneManagerSpigotModule
{
    @Binds
    @Singleton
    IRedstoneManager getPowerBlockRedstoneManager(RedstoneManagerSpigot manager);
}