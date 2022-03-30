package nl.pim16aap2.bigdoors.audio;

import nl.pim16aap2.bigdoors.api.animatedblock.IAnimatedBlock;
import nl.pim16aap2.bigdoors.api.animatedblock.IAnimation;
import nl.pim16aap2.bigdoors.api.animatedblock.IAnimationHook;
import nl.pim16aap2.bigdoors.api.factories.IAnimationHookFactory;

public class AnimationAudioHook implements IAnimationHook<IAnimatedBlock>
{
    private AnimationAudioHook()
    {
    }

    @Override
    public String getName()
    {
        return "BigDoors_Audio_Hook";
    }

    public static final class Factory implements IAnimationHookFactory<IAnimatedBlock>
    {
        @Override
        public IAnimationHook<IAnimatedBlock> newInstance(IAnimation<IAnimatedBlock> animation)
        {
            return new AnimationAudioHook();
        }
    }
}
