package nl.pim16aap2.bigdoors.audio;

import nl.pim16aap2.bigdoors.api.animatedblock.IAnimatedBlock;
import nl.pim16aap2.bigdoors.api.animatedblock.IAnimation;
import nl.pim16aap2.bigdoors.api.animatedblock.IAnimationHook;
import nl.pim16aap2.bigdoors.api.factories.IAnimationHookFactory;
import nl.pim16aap2.bigdoors.util.vector.Vector3Dd;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Represents an {@link IAnimationHook} that provides audio for animations.
 *
 * @author Pim
 */
public class AudioAnimationHook implements IAnimationHook<IAnimatedBlock>
{
    private final IAnimation<IAnimatedBlock> animation;
    private final IAudioPlayer audioPlayer;
    private final AudioSet audioSet;
    private final int activeAudioDuration;

    /**
     * The number of steps that no audio has been played.
     */
    private volatile int skipped = -1;

    private AudioAnimationHook(
        IAnimation<IAnimatedBlock> animation, AudioSet audioSet, IAudioPlayer audioPlayer)
    {
        this.animation = animation;
        this.audioPlayer = audioPlayer;
        this.audioSet = audioSet;
        this.activeAudioDuration = audioSet.activeAudio() == null ? -1 : audioSet.activeAudio().duration();
    }

    @Override
    public void onPostAnimationStep()
    {
        if (animation.getState() != IAnimation.AnimationState.ACTIVE || activeAudioDuration == -1)
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
        audioPlayer.playSound(position, animation.getDoor().getWorld(), audioDescription.sound(),
                              audioDescription.volume(), audioDescription.pitch());
    }

    @Override
    public String getName()
    {
        return "BigDoors_Audio_Hook";
    }

    @Singleton
    public static final class Factory implements IAnimationHookFactory<IAnimatedBlock>
    {
        private final AudioConfigurator audioConfigurator;
        private final IAudioPlayer audioPlayer;

        @Inject
        public Factory(AudioConfigurator audioConfigurator, IAudioPlayer audioPlayer)
        {
            this.audioConfigurator = audioConfigurator;
            this.audioPlayer = audioPlayer;
        }

        @Override
        public @Nullable IAnimationHook<IAnimatedBlock> newInstance(IAnimation<IAnimatedBlock> animation)
        {
            final AudioSet audioSet = audioConfigurator.getAudioSet(animation.getDoor());
            if (audioSet.isEmpty())
                return null;
            return new AudioAnimationHook(animation, audioSet, audioPlayer);
        }
    }
}
