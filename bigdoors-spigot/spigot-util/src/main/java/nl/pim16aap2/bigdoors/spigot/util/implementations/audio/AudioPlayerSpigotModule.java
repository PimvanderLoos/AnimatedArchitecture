package nl.pim16aap2.bigdoors.spigot.util.implementations.audio;

import dagger.Binds;
import dagger.Module;
import nl.pim16aap2.bigdoors.core.audio.IAudioPlayer;

import javax.inject.Singleton;

@Module
public interface AudioPlayerSpigotModule
{
    @Binds
    @Singleton
    IAudioPlayer getAudioPlayer(AudioPlayerSpigot audioPlayerSpigot);
}
