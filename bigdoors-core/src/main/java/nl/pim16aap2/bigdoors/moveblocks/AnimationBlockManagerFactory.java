package nl.pim16aap2.bigdoors.moveblocks;

import nl.pim16aap2.bigdoors.api.IPExecutor;
import nl.pim16aap2.bigdoors.api.factories.IAnimatedBlockFactory;
import nl.pim16aap2.bigdoors.api.factories.IPLocationFactory;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class AnimationBlockManagerFactory
{
    private final IPLocationFactory locationFactory;
    private final IAnimatedBlockFactory animatedBlockFactory;
    private final IPExecutor executor;

    @Inject AnimationBlockManagerFactory(
        IPLocationFactory locationFactory,
        IAnimatedBlockFactory animatedBlockFactory,
        IPExecutor executor)
    {
        this.locationFactory = locationFactory;
        this.animatedBlockFactory = animatedBlockFactory;
        this.executor = executor;
    }

    public IAnimationBlockManager newManager(AnimationType animationType)
    {
        return switch (animationType)
            {
                case MOVE_BLOCKS -> newMoveBlockManager();
                case PREVIEW -> newPreviewBlockManager();
            };
    }

    private IAnimationBlockManager newPreviewBlockManager()
    {
        throw new UnsupportedOperationException();
    }

    private IAnimationBlockManager newMoveBlockManager()
    {
        return new AnimationBlockManager(locationFactory, animatedBlockFactory, executor);
    }
}
