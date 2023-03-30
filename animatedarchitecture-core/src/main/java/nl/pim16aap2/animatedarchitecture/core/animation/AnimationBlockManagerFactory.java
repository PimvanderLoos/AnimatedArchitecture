package nl.pim16aap2.animatedarchitecture.core.animation;

import nl.pim16aap2.animatedarchitecture.core.api.HighlightedBlockSpawner;
import nl.pim16aap2.animatedarchitecture.core.api.IExecutor;
import nl.pim16aap2.animatedarchitecture.core.api.IPlayer;
import nl.pim16aap2.animatedarchitecture.core.api.animatedblock.IAnimatedBlockFactory;
import nl.pim16aap2.animatedarchitecture.core.api.factories.ILocationFactory;
import nl.pim16aap2.animatedarchitecture.core.util.Util;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class AnimationBlockManagerFactory
{
    private final ILocationFactory locationFactory;
    private final IAnimatedBlockFactory animatedBlockFactory;
    private final IExecutor executor;
    private final HighlightedBlockSpawner glowingBlockSpawner;

    @Inject AnimationBlockManagerFactory(
        ILocationFactory locationFactory,
        IAnimatedBlockFactory animatedBlockFactory,
        IExecutor executor, HighlightedBlockSpawner glowingBlockSpawner)
    {
        this.locationFactory = locationFactory;
        this.animatedBlockFactory = animatedBlockFactory;
        this.executor = executor;
        this.glowingBlockSpawner = glowingBlockSpawner;
    }

    public IAnimationBlockManager newManager(AnimationType animationType, @Nullable IPlayer player)
    {
        return switch (animationType)
        {
            case MOVE_BLOCKS -> newMoveBlockManager();
            case PREVIEW -> newPreviewBlockManager(player);
        };
    }

    private IAnimationBlockManager newPreviewBlockManager(@Nullable IPlayer player)
    {
        return new AnimationPreviewBlockManager(
            locationFactory, glowingBlockSpawner, Util.requireNonNull(player, "Player for preview blocks"));
    }

    private IAnimationBlockManager newMoveBlockManager()
    {
        return new AnimationBlockManager(animatedBlockFactory, executor);
    }
}
