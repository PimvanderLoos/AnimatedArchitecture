package nl.pim16aap2.bigdoors.structures.windmill;

import nl.pim16aap2.bigdoors.core.moveblocks.Animator;
import nl.pim16aap2.bigdoors.core.moveblocks.StructureRequestData;
import nl.pim16aap2.bigdoors.core.util.MovementDirection;
import nl.pim16aap2.bigdoors.core.util.vector.IVector3D;
import nl.pim16aap2.bigdoors.core.util.vector.Vector3Dd;
import nl.pim16aap2.bigdoors.structures.drawbridge.DrawbridgeAnimationComponent;

/**
 * Represents a {@link Animator} for {@link Windmill}s.
 *
 * @author Pim
 */
public class WindmillAnimationComponent extends DrawbridgeAnimationComponent
{
    public WindmillAnimationComponent(
        StructureRequestData data, MovementDirection movementDirection, boolean isNorthSouthAligned)
    {
        super(data, movementDirection, isNorthSouthAligned, 4);
    }

    @Override
    public Vector3Dd getFinalPosition(IVector3D startLocation, float radius)
    {
        return Vector3Dd.of(startLocation);
    }
}
