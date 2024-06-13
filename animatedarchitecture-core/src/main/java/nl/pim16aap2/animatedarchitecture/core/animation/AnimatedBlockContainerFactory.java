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

/**
 * A factory for creating {@link IAnimatedBlockContainer}s.
 * <p>
 * This class is responsible for creating new {@link IAnimatedBlockContainer}s based on the given
 * {@link AnimationType}.
 * <p>
 * F
 */
@Singleton
public class AnimatedBlockContainerFactory
{
    private final ILocationFactory locationFactory;
    private final IAnimatedBlockFactory animatedBlockFactory;
    private final IExecutor executor;
    private final HighlightedBlockSpawner glowingBlockSpawner;

    @Inject AnimatedBlockContainerFactory(
        ILocationFactory locationFactory,
        IAnimatedBlockFactory animatedBlockFactory,
        IExecutor executor,
        HighlightedBlockSpawner glowingBlockSpawner)
    {
        this.locationFactory = locationFactory;
        this.animatedBlockFactory = animatedBlockFactory;
        this.executor = executor;
        this.glowingBlockSpawner = glowingBlockSpawner;
    }

    /**
     * Creates a new {@link IAnimatedBlockContainer} based on the given {@link AnimationType}.
     *
     * @param animationType
     *     The type of animation to create the container for.
     * @param player
     *     The player that the container is created for. Can be null if the container is not player-specific.
     * @return A new {@link IAnimatedBlockContainer} based on the given {@link AnimationType}.
     *
     * @throws IllegalArgumentException
     *     If the player is null and the animation type requires a player. See {@link AnimationType#requiresPlayer()}.
     */
    public IAnimatedBlockContainer newContainer(AnimationType animationType, @Nullable IPlayer player)
    {
        if (player == null && animationType.requiresPlayer())
            throw new IllegalArgumentException("Player cannot be null for animation type '" + animationType + "'!");

        return switch (animationType)
        {
            case MOVE_BLOCKS -> newMoveBlockContainer();
            case PREVIEW -> newPreviewBlockContainer(player);
        };
    }

    private IAnimatedBlockContainer newPreviewBlockContainer(@Nullable IPlayer player)
    {
        return new AnimatedPreviewBlockContainer(
            locationFactory,
            glowingBlockSpawner,
            Util.requireNonNull(player, "Player for preview blocks")
        );
    }

    private IAnimatedBlockContainer newMoveBlockContainer()
    {
        return new AnimatedBlockContainer(animatedBlockFactory, executor);
    }
}
