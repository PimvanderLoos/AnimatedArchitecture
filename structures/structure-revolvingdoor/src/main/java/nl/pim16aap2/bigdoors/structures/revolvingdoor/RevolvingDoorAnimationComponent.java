package nl.pim16aap2.bigdoors.structures.revolvingdoor;

import nl.pim16aap2.bigdoors.core.moveblocks.AnimationRequestData;
import nl.pim16aap2.bigdoors.core.moveblocks.Animator;
import nl.pim16aap2.bigdoors.core.util.MovementDirection;
import nl.pim16aap2.bigdoors.core.util.vector.IVector3D;
import nl.pim16aap2.bigdoors.core.util.vector.Vector3Dd;
import nl.pim16aap2.bigdoors.structures.bigdoor.BigDoorAnimationComponent;

/**
 * Represents a {@link Animator} for {@link RevolvingDoor}s.
 *
 * @author Pim
 */
public class RevolvingDoorAnimationComponent extends BigDoorAnimationComponent
{
    public RevolvingDoorAnimationComponent(AnimationRequestData data, MovementDirection movementDirection)
    {
        super(data, movementDirection, 4);
    }

    @Override
    public Vector3Dd getFinalPosition(IVector3D startLocation, float radius)
    {
        return Vector3Dd.of(startLocation);
    }
}
