package nl.pim16aap2.animatedarchitecture.spigot.core.implementations;

import dagger.Binds;
import dagger.Module;
import jakarta.inject.Singleton;
import nl.pim16aap2.animatedarchitecture.core.audio.IAudioPlayer;

@Module
public interface AudioPlayerSpigotModule
{
    @Binds
    @Singleton
    IAudioPlayer getAudioPlayer(AudioPlayerSpigot audioPlayerSpigot);
}
