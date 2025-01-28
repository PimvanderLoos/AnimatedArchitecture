package nl.pim16aap2.animatedarchitecture.structures.revolvingdoor;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.extern.flogger.Flogger;
import nl.pim16aap2.animatedarchitecture.core.animation.AnimationRequestData;
import nl.pim16aap2.animatedarchitecture.core.animation.IAnimationComponent;
import nl.pim16aap2.animatedarchitecture.core.structures.IStructureComponent;
import nl.pim16aap2.animatedarchitecture.core.structures.IStructureConst;
import nl.pim16aap2.animatedarchitecture.core.structures.properties.Property;
import nl.pim16aap2.animatedarchitecture.core.util.Cuboid;
import nl.pim16aap2.animatedarchitecture.core.util.Rectangle;
import nl.pim16aap2.animatedarchitecture.core.util.vector.Vector3Di;
import nl.pim16aap2.animatedarchitecture.structures.bigdoor.BigDoor;

import java.util.Optional;

/**
 * Represents a Revolving Door structure type.
 */
@Flogger
@ToString
@EqualsAndHashCode
public class RevolvingDoor implements IStructureComponent
{
    @Override
    public boolean canMovePerpetually(IStructureConst structure)
    {
        return true;
    }

    @Override
    public double calculateAnimationCycleDistance(IStructureConst structure)
    {
        final Vector3Di rotationPoint = structure.getRequiredPropertyValue(Property.ROTATION_POINT);

        return BigDoor.getMaxRadius(structure.getCuboid(), rotationPoint) * Math.TAU;
    }

    @Override
    public Rectangle calculateAnimationRange(IStructureConst structure)
    {
        final Vector3Di rotationPoint = structure.getRequiredPropertyValue(Property.ROTATION_POINT);
        final double maxRadius = BigDoor.getMaxRadius(structure.getCuboid(), rotationPoint);

        return BigDoor.calculateAnimationRange(maxRadius, structure.getCuboid());
    }

    @Override
    public Optional<Cuboid> getPotentialNewCoordinates(IStructureConst structure)
    {
        return Optional.of(structure.getCuboid());
    }

    @Override
    public IAnimationComponent constructAnimationComponent(IStructureConst structure, AnimationRequestData data)
    {
        return new RevolvingDoorAnimationComponent(data, getCurrentToggleDirection(structure));
    }
}
