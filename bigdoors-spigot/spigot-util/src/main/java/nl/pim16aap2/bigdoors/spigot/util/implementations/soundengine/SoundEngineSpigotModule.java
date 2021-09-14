package nl.pim16aap2.bigdoors.spigot.util.implementations.soundengine;

import dagger.Binds;
import dagger.Module;
import nl.pim16aap2.bigdoors.api.ISoundEngine;

import javax.inject.Singleton;

@Module
public interface SoundEngineSpigotModule
{
    @Binds
    @Singleton
    ISoundEngine getSoundEngine(SoundEngineSpigot engine);
}
