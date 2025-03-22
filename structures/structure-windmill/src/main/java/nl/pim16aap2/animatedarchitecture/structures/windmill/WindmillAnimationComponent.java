package nl.pim16aap2.animatedarchitecture.structures.windmill;

import lombok.ToString;
import nl.pim16aap2.animatedarchitecture.core.animation.AnimationRequestData;
import nl.pim16aap2.animatedarchitecture.core.animation.IAnimationComponent;
import nl.pim16aap2.animatedarchitecture.core.animation.RotatedPosition;
import nl.pim16aap2.animatedarchitecture.core.api.animatedblock.IAnimatedBlockData;
import nl.pim16aap2.animatedarchitecture.core.util.MovementDirection;
import nl.pim16aap2.animatedarchitecture.structures.drawbridge.DrawbridgeAnimationComponent;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

/**
 * Represents an {@link IAnimationComponent} for {@link Windmill} structure types.
 */
@ToString(callSuper = true)
public class WindmillAnimationComponent extends DrawbridgeAnimationComponent
{
    public WindmillAnimationComponent(
        AnimationRequestData data,
        MovementDirection movementDirection,
        boolean isNorthSouthAligned)
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
