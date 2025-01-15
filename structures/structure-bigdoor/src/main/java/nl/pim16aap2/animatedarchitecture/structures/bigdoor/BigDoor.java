package nl.pim16aap2.animatedarchitecture.structures.bigdoor;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.extern.flogger.Flogger;
import nl.pim16aap2.animatedarchitecture.core.animation.AnimationRequestData;
import nl.pim16aap2.animatedarchitecture.core.animation.IAnimationComponent;
import nl.pim16aap2.animatedarchitecture.core.structures.IStructureConst;
import nl.pim16aap2.animatedarchitecture.core.structures.Structure;
import nl.pim16aap2.animatedarchitecture.core.structures.properties.IStructureComponent;
import nl.pim16aap2.animatedarchitecture.core.structures.properties.Property;
import nl.pim16aap2.animatedarchitecture.core.util.Cuboid;
import nl.pim16aap2.animatedarchitecture.core.util.MathUtil;
import nl.pim16aap2.animatedarchitecture.core.util.MovementDirection;
import nl.pim16aap2.animatedarchitecture.core.util.Rectangle;
import nl.pim16aap2.animatedarchitecture.core.util.vector.Vector3Di;

import java.util.Optional;
import java.util.stream.Stream;

/**
 * Represents a Big Door structure component.
 *
 * @see Structure
 */
@Flogger
@ToString
@EqualsAndHashCode
public class BigDoor implements IStructureComponent
{
//    @Deserialization
//    public BigDoor(BaseHolder base)
//    {
//        super(base, StructureTypeBigDoor.get());
//        this.lock = getLock();
//    }
//
//    /**
//     * Deprecated constructor for deserialization of version 1 where {@code quarterCircles} was a persistent variable.
//     */
//    @Deprecated
//    @Deserialization(version = 1)
//    public BigDoor(BaseHolder base, @PersistentVariable(value = "quarterCircles") int quarterCircles)
//    {
//        this(base);
//        setQuarterCircles(quarterCircles);
//    }

    @Override
    public MovementDirection getCycledOpenDirection(IStructureConst structure)
    {
        return structure.getOpenDirection().equals(MovementDirection.CLOCKWISE) ?
            MovementDirection.COUNTERCLOCKWISE :
            MovementDirection.CLOCKWISE;
    }

    @Override
    public Optional<Cuboid> getPotentialNewCoordinates(IStructureConst structure)
    {
        final MovementDirection movementDirection = getCurrentToggleDirection(structure);
        final int quarterCircles = structure.getRequiredPropertyValue(Property.QUARTER_CIRCLES);

        final double angle;
        if (movementDirection == MovementDirection.CLOCKWISE)
        {
            angle = quarterCircles * MathUtil.HALF_PI;
        }
        else if (movementDirection == MovementDirection.COUNTERCLOCKWISE)
        {
            angle = quarterCircles * -MathUtil.HALF_PI;
        }
        else
        {
            log.atSevere().log(
                "Invalid movement direction '%s' for door: %d",
                movementDirection.name(),
                structure.getUid()
            );
            return Optional.empty();
        }

        final Vector3Di rotationPoint = structure.getRequiredPropertyValue(Property.ROTATION_POINT);
        return Optional.of(structure.getCuboid().updatePositions(vec -> vec.rotateAroundYAxis(rotationPoint, angle)));
    }

    @Override
    public double calculateAnimationCycleDistance(IStructureConst structure)
    {
        final Vector3Di rotationPoint = structure.getRequiredPropertyValue(Property.ROTATION_POINT);
        final int quarterCircles = structure.getRequiredPropertyValue(Property.QUARTER_CIRCLES);

        final double maxRadius = getMaxRadius(structure.getCuboid(), rotationPoint);
        return quarterCircles * maxRadius * MathUtil.HALF_PI;
    }

    @Override
    public Rectangle calculateAnimationRange(IStructureConst structure)
    {
        final Vector3Di rotationPoint = structure.getRequiredPropertyValue(Property.ROTATION_POINT);
        final Cuboid cuboid = structure.getCuboid();

        final double maxRadius = getMaxRadius(cuboid, rotationPoint);
        return calculateAnimationRange(maxRadius, cuboid);
    }

    /**
     * Calculates the animation range of a big door.
     *
     * @param maxRadius
     *     See {@link #getMaxRadius(Cuboid, Vector3Di)}.
     * @param cuboid
     *     The cuboid that describes this door.
     * @return The animation range.
     */
    public static Rectangle calculateAnimationRange(double maxRadius, Cuboid cuboid)
    {
        final int radius = MathUtil.ceil(maxRadius);
        return new Cuboid(
            cuboid.getMin().add(-radius, 0, -radius),
            cuboid.getMax().add(radius, 0, radius)
        ).asFlatRectangle();
    }

    /**
     * Calculates the longest possible radius from the rotation point of a big door to another point that is still part
     * of the big door.
     *
     * @param cuboid
     *     The cuboid that describes this door.
     * @param rotationPoint
     *     The rotation point of the door.
     * @return The radius between the rotation point of the door and the animated block furthest from it.
     */
    public static double getMaxRadius(Cuboid cuboid, Vector3Di rotationPoint)
    {
        final Vector3Di min = cuboid.getMin();
        final Vector3Di max = cuboid.getMax();
        final Vector3Di other0 = new Vector3Di(min.x(), min.y(), max.z());
        final Vector3Di other1 = new Vector3Di(max.x(), min.y(), min.z());

        return Stream.of(min, max, other0, other1)
            .mapToDouble(val -> BigDoorAnimationComponent.getRadius(rotationPoint, val.x(), val.z()))
            .max()
            .orElseThrow();
    }

    @Override
    public IAnimationComponent constructAnimationComponent(IStructureConst structure, AnimationRequestData data)
    {
        final int quarterCircles = structure.getRequiredPropertyValue(Property.QUARTER_CIRCLES);
        return new BigDoorAnimationComponent(data, getCurrentToggleDirection(structure), quarterCircles);
    }
}
