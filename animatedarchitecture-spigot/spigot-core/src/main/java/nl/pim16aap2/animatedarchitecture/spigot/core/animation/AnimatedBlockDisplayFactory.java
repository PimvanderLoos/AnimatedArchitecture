package nl.pim16aap2.animatedarchitecture.spigot.core.animation;

import nl.pim16aap2.animatedarchitecture.core.api.IExecutor;
import nl.pim16aap2.animatedarchitecture.core.api.ILocation;
import nl.pim16aap2.animatedarchitecture.core.api.animatedblock.AnimationContext;
import nl.pim16aap2.animatedarchitecture.core.api.animatedblock.IAnimatedBlock;
import nl.pim16aap2.animatedarchitecture.core.api.animatedblock.IAnimatedBlockFactory;
import nl.pim16aap2.animatedarchitecture.core.moveblocks.Animator;
import nl.pim16aap2.animatedarchitecture.core.moveblocks.RotatedPosition;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Optional;

@Singleton
public class AnimatedBlockDisplayFactory implements IAnimatedBlockFactory
{
    private final IExecutor executor;

    @Inject public AnimatedBlockDisplayFactory(IExecutor executor)
    {
        this.executor = executor;
    }

    @Override
    public Optional<IAnimatedBlock> create(
        ILocation loc, RotatedPosition startPosition, float radius, boolean bottom, boolean onEdge,
        AnimationContext context, RotatedPosition finalPosition, Animator.MovementMethod movementMethod)
        throws Exception
    {
        return Optional.of(new AnimatedBlockDisplay(
            executor,
            startPosition,
            loc.getWorld(),
            new RotatedPosition(loc.getPositionDouble()),
            finalPosition,
            onEdge,
            radius));
    }
}
