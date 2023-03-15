package nl.pim16aap2.animatedarchitecture.spigot.core.animation;

import nl.pim16aap2.animatedarchitecture.core.api.IExecutor;
import nl.pim16aap2.animatedarchitecture.core.api.ILocation;
import nl.pim16aap2.animatedarchitecture.core.api.animatedblock.AnimationContext;
import nl.pim16aap2.animatedarchitecture.core.api.animatedblock.IAnimatedBlock;
import nl.pim16aap2.animatedarchitecture.core.api.animatedblock.IAnimatedBlockFactory;
import nl.pim16aap2.animatedarchitecture.core.moveblocks.Animator;
import nl.pim16aap2.animatedarchitecture.core.moveblocks.RotatedPosition;
import nl.pim16aap2.animatedarchitecture.core.util.vector.Vector3Dd;

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
        ILocation loc, float radius, float startAngle, boolean bottom, boolean onEdge, Vector3Dd rotationPoint,
        AnimationContext context, Vector3Dd finalPosition, Animator.MovementMethod movementMethod)
        throws Exception
    {
//        return Optional.of(new AnimatedBlockDisplay(
//            executor,
//            loc.getWorld(),
//            new RotatedPosition(loc.getPositionDouble()),
//            new RotatedPosition(finalPosition.add(-0.5, 0, -0.5)),
//            startAngle,
//            radius));
        return Optional.of(new AnimatedBlockDisplay(
            executor,
            loc.getWorld(),
            rotationPoint,
            new RotatedPosition(loc.getPositionDouble()),
            new RotatedPosition(finalPosition),
            startAngle,
            radius));
    }
}
