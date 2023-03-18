package nl.pim16aap2.animatedarchitecture.structures.windmill;

import nl.pim16aap2.animatedarchitecture.core.api.animatedblock.IAnimatedBlockData;
import nl.pim16aap2.animatedarchitecture.core.moveblocks.AnimationRequestData;
import nl.pim16aap2.animatedarchitecture.core.moveblocks.Animator;
import nl.pim16aap2.animatedarchitecture.core.moveblocks.RotatedPosition;
import nl.pim16aap2.animatedarchitecture.core.util.MovementDirection;
import nl.pim16aap2.animatedarchitecture.structures.drawbridge.DrawbridgeAnimationComponent;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

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

    @Override
    public @Nullable Consumer<IAnimatedBlockData> getBlockDataRotator()
    {
        return null;
    }
}
