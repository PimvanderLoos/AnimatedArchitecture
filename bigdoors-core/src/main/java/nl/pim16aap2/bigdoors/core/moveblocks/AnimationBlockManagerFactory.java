package nl.pim16aap2.bigdoors.core.moveblocks;

import nl.pim16aap2.bigdoors.core.api.GlowingBlockSpawner;
import nl.pim16aap2.bigdoors.core.api.IPExecutor;
import nl.pim16aap2.bigdoors.core.api.IPPlayer;
import nl.pim16aap2.bigdoors.core.api.factories.IAnimatedBlockFactory;
import nl.pim16aap2.bigdoors.core.api.factories.IPLocationFactory;
import nl.pim16aap2.bigdoors.core.util.Util;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class AnimationBlockManagerFactory
{
    private final IPLocationFactory locationFactory;
    private final IAnimatedBlockFactory animatedBlockFactory;
    private final IPExecutor executor;
    private final GlowingBlockSpawner glowingBlockSpawner;

    @Inject AnimationBlockManagerFactory(
        IPLocationFactory locationFactory,
        IAnimatedBlockFactory animatedBlockFactory,
        IPExecutor executor, GlowingBlockSpawner glowingBlockSpawner)
    {
        this.locationFactory = locationFactory;
        this.animatedBlockFactory = animatedBlockFactory;
        this.executor = executor;
        this.glowingBlockSpawner = glowingBlockSpawner;
    }

    public IAnimationBlockManager newManager(AnimationType animationType, @Nullable IPPlayer player)
    {
        return switch (animationType)
            {
                case MOVE_BLOCKS -> newMoveBlockManager();
                case PREVIEW -> newPreviewBlockManager(player);
            };
    }

    private IAnimationBlockManager newPreviewBlockManager(@Nullable IPPlayer player)
    {
        return new AnimationPreviewBlockManager(
            locationFactory, glowingBlockSpawner, Util.requireNonNull(player, "Player for preview blocks"));
    }

    private IAnimationBlockManager newMoveBlockManager()
    {
        return new AnimationBlockManager(locationFactory, animatedBlockFactory, executor);
    }
}
