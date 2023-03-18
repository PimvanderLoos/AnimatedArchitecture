package nl.pim16aap2.animatedarchitecture.spigot.core.implementations;

import dagger.Binds;
import dagger.Module;
import nl.pim16aap2.animatedarchitecture.core.audio.IAudioPlayer;

import javax.inject.Singleton;

@Module
public interface AudioPlayerSpigotModule
{
    @Binds
    @Singleton
    IAudioPlayer getAudioPlayer(AudioPlayerSpigot audioPlayerSpigot);
}
