package nl.pim16aap2.animatedarchitecture.structures.windmill;

import nl.pim16aap2.animatedarchitecture.core.moveblocks.AnimationRequestData;
import nl.pim16aap2.animatedarchitecture.core.moveblocks.Animator;
import nl.pim16aap2.animatedarchitecture.core.moveblocks.RotatedPosition;
import nl.pim16aap2.animatedarchitecture.core.util.MovementDirection;
import nl.pim16aap2.animatedarchitecture.structures.drawbridge.DrawbridgeAnimationComponent;

/**
 * Represents a {@link Animator} for {@link Windmill}s.
 *
 * @author Pim
 */
public class WindmillAnimationComponent extends DrawbridgeAnimationComponent
{
    public WindmillAnimationComponent(
        AnimationRequestData data, MovementDirection movementDirection, boolean isNorthSouthAligned)
    {
        super(data, movementDirection, isNorthSouthAligned, 4);
    }

    @Override
    public RotatedPosition getFinalPosition(int xAxis, int yAxis, int zAxis)
    {
        return getStartPosition(xAxis, yAxis, zAxis);
    }
}
