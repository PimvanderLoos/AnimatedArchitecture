package nl.pim16aap2.animatedarchitecture.structures.revolvingdoor;

import lombok.ToString;
import nl.pim16aap2.animatedarchitecture.core.animation.AnimationRequestData;
import nl.pim16aap2.animatedarchitecture.core.animation.IAnimationComponent;
import nl.pim16aap2.animatedarchitecture.core.animation.RotatedPosition;
import nl.pim16aap2.animatedarchitecture.core.api.animatedblock.IAnimatedBlockData;
import nl.pim16aap2.animatedarchitecture.core.util.MovementDirection;
import nl.pim16aap2.animatedarchitecture.core.util.vector.Vector3Dd;
import nl.pim16aap2.animatedarchitecture.structures.bigdoor.BigDoorAnimationComponent;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

/**
 * Represents an {@link IAnimationComponent} for {@link RevolvingDoor} structure types.
 */
@ToString(callSuper = true)
public class RevolvingDoorAnimationComponent extends BigDoorAnimationComponent
{
    public RevolvingDoorAnimationComponent(AnimationRequestData data, MovementDirection movementDirection)
    {
        super(data, movementDirection, 4);
    }

    @Override
    public RotatedPosition getFinalPosition(int xAxis, int yAxis, int zAxis)
    {
        return new RotatedPosition(new Vector3Dd(xAxis, yAxis, zAxis));
    }

    @Override
    public @Nullable Consumer<IAnimatedBlockData> getBlockDataRotator()
    {
        return null;
    }
}
