package nl.pim16aap2.bigdoors.spigot.managers;

import dagger.Binds;
import dagger.Module;
import nl.pim16aap2.bigdoors.api.IPowerBlockRedstoneManager;

import javax.inject.Singleton;

@Module
public interface PowerBlockRedstoneManagerSpigotModule
{
    @Binds
    @Singleton
    IPowerBlockRedstoneManager getPowerBlockRedstoneManager(PowerBlockRedstoneManagerSpigot manager);
}
