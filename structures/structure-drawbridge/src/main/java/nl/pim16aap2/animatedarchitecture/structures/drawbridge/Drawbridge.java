package nl.pim16aap2.animatedarchitecture.structures.drawbridge;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.extern.flogger.Flogger;
import nl.pim16aap2.animatedarchitecture.core.animation.AnimationRequestData;
import nl.pim16aap2.animatedarchitecture.core.animation.IAnimationComponent;
import nl.pim16aap2.animatedarchitecture.core.structures.IStructureComponent;
import nl.pim16aap2.animatedarchitecture.core.structures.IStructureConst;
import nl.pim16aap2.animatedarchitecture.core.structures.properties.Property;
import nl.pim16aap2.animatedarchitecture.core.util.Cuboid;
import nl.pim16aap2.animatedarchitecture.core.util.MathUtil;
import nl.pim16aap2.animatedarchitecture.core.util.MovementDirection;
import nl.pim16aap2.animatedarchitecture.core.util.Rectangle;
import nl.pim16aap2.animatedarchitecture.core.util.vector.Vector3Di;

import java.util.Optional;
import java.util.stream.Stream;

/**
 * Represents a Drawbridge structure type.
 */
@Flogger
@ToString
@EqualsAndHashCode
public class Drawbridge implements IStructureComponent
{
    @Override
    public Optional<Cuboid> getPotentialNewCoordinates(IStructureConst structure)
    {
        final MovementDirection movementDirection = getCurrentToggleDirection(structure);
        final Cuboid cuboid = structure.getCuboid();
        final Vector3Di rotationPoint = structure.getRequiredPropertyValue(Property.ROTATION_POINT);

        final int quarterCircles = structure.getRequiredPropertyValue(Property.QUARTER_CIRCLES);

        final double angle;
        if (movementDirection == MovementDirection.NORTH || movementDirection == MovementDirection.WEST)
        {
            angle = quarterCircles * -MathUtil.HALF_PI;
        }
        else if (movementDirection == MovementDirection.SOUTH || movementDirection == MovementDirection.EAST)
        {
            angle = quarterCircles * MathUtil.HALF_PI;
        }
        else
        {
            log.atSevere().log(
                "Invalid open direction '%s' for door: %d",
                movementDirection.name(),
                structure.getUid()
            );
            return Optional.empty();
        }

        if (movementDirection == MovementDirection.NORTH || movementDirection == MovementDirection.SOUTH)
        {
            return Optional.of(cuboid.updatePositions(vec -> vec.rotateAroundXAxis(rotationPoint, angle)));
        }
        else
        {
            return Optional.of(cuboid.updatePositions(vec -> vec.rotateAroundZAxis(rotationPoint, angle)));
        }
    }

    @Override
    public IAnimationComponent constructAnimationComponent(IStructureConst structure, AnimationRequestData data)
    {
        return new DrawbridgeAnimationComponent(
            data,
            getCurrentToggleDirection(structure),
            isNorthSouthAnimated(structure),
            structure.getRequiredPropertyValue(Property.QUARTER_CIRCLES)
        );
    }

    @Override
    public double calculateAnimationCycleDistance(IStructureConst structure)
    {
        final Vector3Di rotationPoint = structure.getRequiredPropertyValue(Property.ROTATION_POINT);
        final int quarterCircles = structure.getRequiredPropertyValue(Property.QUARTER_CIRCLES);

        final double maxRadius = getMaxRadius(isNorthSouthAnimated(structure), structure.getCuboid(), rotationPoint);
        return quarterCircles * maxRadius * MathUtil.HALF_PI;
    }

    @Override
    public Rectangle calculateAnimationRange(IStructureConst structure)
    {
        final Vector3Di rotationPoint = structure.getRequiredPropertyValue(Property.ROTATION_POINT);
        final Cuboid cuboid = structure.getCuboid();

        final double maxRadius = getMaxRadius(isNorthSouthAnimated(structure), cuboid, rotationPoint);
        return calculateAnimationRange(maxRadius, cuboid);
    }

    /**
     * Calculates the animation range of a drawbridge.
     *
     * @param maxRadius
     *     See {@link #getMaxRadius(boolean, Cuboid, Vector3Di)}.
     * @param cuboid
     *     The cuboid that describes this door.
     * @return The animation range.
     */
    public static Rectangle calculateAnimationRange(double maxRadius, Cuboid cuboid)
    {
        final int radius = MathUtil.ceil(maxRadius);
        return new Cuboid(cuboid.getMin().add(-radius), cuboid.getMin().add(radius)).asFlatRectangle();
    }

    /**
     * Calculates the longest possible radius from the rotation point of a drawbridge to another point that is still
     * part of the drawbridge.
     *
     * @param cuboid
     *     The cuboid that describes this drawbridge.
     * @param rotationPoint
     *     The rotation point of the drawbridge.
     * @return The radius between the rotation point of the drawbridge and the animated block furthest from it.
     */
    public static double getMaxRadius(boolean northSouthAligned, Cuboid cuboid, Vector3Di rotationPoint)
    {
        return Stream
            .of(cuboid.getCorners())
            .mapToDouble(val ->
                DrawbridgeAnimationComponent.getRadius(northSouthAligned, rotationPoint, val.x(), val.y(), val.z()))
            .max()
            .orElseThrow();
    }
}
