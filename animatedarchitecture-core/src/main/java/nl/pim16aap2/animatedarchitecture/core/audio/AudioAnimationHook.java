package nl.pim16aap2.animatedarchitecture.core.audio;

import nl.pim16aap2.animatedarchitecture.core.animation.Animation;
import nl.pim16aap2.animatedarchitecture.core.api.animatedblock.IAnimatedBlock;
import nl.pim16aap2.animatedarchitecture.core.api.animatedblock.IAnimationHook;
import nl.pim16aap2.animatedarchitecture.core.api.animatedblock.IAnimationHookFactory;
import nl.pim16aap2.animatedarchitecture.core.util.vector.Vector3Dd;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

/**
 * Represents an {@link IAnimationHook} that provides audio for animations.
 *
 * @author Pim
 */
public final class AudioAnimationHook implements IAnimationHook
{
    private final Animation<IAnimatedBlock> animation;
    private final IAudioPlayer audioPlayer;
    private final AudioSet audioSet;

    /**
     * The duration of the audio, measured in ticks.
     */
    private final int activeAudioDuration;

    /**
     * The number of steps that no audio has been played.
     */
    private volatile int skipped = -1;

    private AudioAnimationHook(
        Animation<IAnimatedBlock> animation, AudioSet audioSet, IAudioPlayer audioPlayer, int serverTickTime)
    {
        this.animation = animation;
        this.audioPlayer = audioPlayer;
        this.audioSet = audioSet;
        final int duration = audioSet.activeAudio() == null ? -1 : audioSet.activeAudio().duration();
        this.activeAudioDuration = Math.round((float) duration / serverTickTime);
    }

    @Override
    public void onPostAnimationStep()
    {
        if (animation.getState() != Animation.AnimationState.ACTIVE || activeAudioDuration == -1)
            return;

        final int skippedCount = skipped;
        if (skippedCount == -1 ||
            (skippedCount > activeAudioDuration && activeAudioDuration <= animation.getRemainingSteps()))
        {
            playAudio(animation.getRegion().getCenter(), audioSet.activeAudio());
            skipped = 0;
        }
        else
        {
            skipped = skippedCount + 1;
        }
    }

    @Override
    public void onAnimationEnding()
    {
        playAudio(animation.getRegion().getCenter(), audioSet.endAudio());
    }

    private void playAudio(Vector3Dd position, @Nullable AudioDescription audioDescription)
    {
        if (audioDescription == null)
            return;

        final double range = 15;

        audioPlayer.playSound(
            position.x(),
            position.y(),
            position.z(),
            animation.getStructureSnapshot().getWorld(),
            audioDescription.sound(),
            audioDescription.volume(),
            audioDescription.pitch(),
            range,
            distance -> (range - distance) / range);
    }

    @Override
    public String getName()
    {
        return "AnimatedArchitecture_Audio_Hook";
    }

    @Singleton
    public static final class Factory implements IAnimationHookFactory<IAnimatedBlock>
    {
        private final int serverTickTime;
        private final AudioConfigurator audioConfigurator;
        private final IAudioPlayer audioPlayer;

        @Inject
        public Factory(
            @Named("serverTickTime") int serverTickTime, AudioConfigurator audioConfigurator, IAudioPlayer audioPlayer)
        {
            this.serverTickTime = serverTickTime;
            this.audioConfigurator = audioConfigurator;
            this.audioPlayer = audioPlayer;
        }

        @Override
        public @Nullable IAnimationHook newInstance(Animation<IAnimatedBlock> animation)
        {
            final AudioSet audioSet = audioConfigurator.getAudioSet(animation.getStructureType());
            if (audioSet.isEmpty())
                return null;
            return new AudioAnimationHook(animation, audioSet, audioPlayer, getServerTickTime());
        }

        private int getServerTickTime()
        {
            if (serverTickTime <= 0)
                throw new IllegalArgumentException("Received illegal tick time value of '" + serverTickTime + "'!");
            return serverTickTime;
        }
    }
}
