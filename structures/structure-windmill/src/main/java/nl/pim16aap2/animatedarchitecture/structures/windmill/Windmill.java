package nl.pim16aap2.animatedarchitecture.structures.windmill;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.extern.flogger.Flogger;
import nl.pim16aap2.animatedarchitecture.core.animation.AnimationRequestData;
import nl.pim16aap2.animatedarchitecture.core.animation.IAnimationComponent;
import nl.pim16aap2.animatedarchitecture.core.structures.IStructureComponent;
import nl.pim16aap2.animatedarchitecture.core.structures.IStructureConst;
import nl.pim16aap2.animatedarchitecture.core.structures.properties.Property;
import nl.pim16aap2.animatedarchitecture.core.util.Cuboid;
import nl.pim16aap2.animatedarchitecture.core.util.MovementDirection;
import nl.pim16aap2.animatedarchitecture.core.util.Rectangle;
import nl.pim16aap2.animatedarchitecture.core.util.vector.Vector3Di;
import nl.pim16aap2.animatedarchitecture.structures.drawbridge.Drawbridge;

import java.util.Optional;

/**
 * Represents a Windmill structure type.
 */
@Flogger
@ToString
@EqualsAndHashCode
public class Windmill implements IStructureComponent
{
//    @Deserialization
//    public Windmill(BaseHolder base)
//    {
//        super(base, StructureTypeWindmill.get());
//        this.lock = getLock();
//    }

    @Override
    public boolean canMovePerpetually(IStructureConst structure)
    {
        return true;
    }

    @Override
    public Optional<Cuboid> getPotentialNewCoordinates(IStructureConst structure)
    {
        return Optional.of(structure.getCuboid());
    }

    @Override
    public double calculateAnimationCycleDistance(IStructureConst structure)
    {
        final Vector3Di rotationPoint = structure.getRequiredPropertyValue(Property.ROTATION_POINT);
        final Cuboid cuboid = structure.getCuboid();

        return Drawbridge.getMaxRadius(isNorthSouthAnimated(structure), cuboid, rotationPoint) * Math.TAU;
    }

    @Override
    public Rectangle calculateAnimationRange(IStructureConst structure)
    {
        final Vector3Di rotationPoint = structure.getRequiredPropertyValue(Property.ROTATION_POINT);
        final Cuboid cuboid = structure.getCuboid();

        final double maxRadius = Drawbridge.getMaxRadius(isNorthSouthAnimated(structure), cuboid, rotationPoint);
        return Drawbridge.calculateAnimationRange(maxRadius, cuboid);
    }

    @Override
    public MovementDirection getCycledOpenDirection(IStructureConst structure)
    {
        return IStructureComponent.cycleCardinalDirection(structure.getOpenDirection());
    }

    @Override
    public IAnimationComponent constructAnimationComponent(IStructureConst structure, AnimationRequestData data)
    {
        return new WindmillAnimationComponent(
            data,
            getCurrentToggleDirection(structure),
            isNorthSouthAnimated(structure)
        );
    }
}
