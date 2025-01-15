package nl.pim16aap2.animatedarchitecture.structures.garagedoor;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.extern.flogger.Flogger;
import nl.pim16aap2.animatedarchitecture.core.animation.AnimationRequestData;
import nl.pim16aap2.animatedarchitecture.core.animation.IAnimationComponent;
import nl.pim16aap2.animatedarchitecture.core.structures.IStructureConst;
import nl.pim16aap2.animatedarchitecture.core.structures.properties.IStructureComponent;
import nl.pim16aap2.animatedarchitecture.core.structures.properties.Property;
import nl.pim16aap2.animatedarchitecture.core.util.Cuboid;
import nl.pim16aap2.animatedarchitecture.core.util.MathUtil;
import nl.pim16aap2.animatedarchitecture.core.util.MovementDirection;
import nl.pim16aap2.animatedarchitecture.core.util.Rectangle;
import nl.pim16aap2.animatedarchitecture.core.util.vector.Vector3Di;

import java.util.Optional;

/**
 * Represents a Garage Door structure type.
 */
@Flogger
@ToString
@EqualsAndHashCode
public class GarageDoor implements IStructureComponent
{
    private static final boolean USE_COUNTER_WEIGHT = true;

//    @Getter
//    @PersistentVariable(value = "northSouthAnimated")
//    protected final boolean northSouthAnimated;
//
//    @Deserialization
//    public GarageDoor(BaseHolder base, @PersistentVariable(value = "northSouthAnimated") boolean northSouthAnimated)
//    {
//        super(base, StructureTypeGarageDoor.get());
//        this.lock = getLock();
//        this.northSouthAnimated = northSouthAnimated;
//    }

    /**
     * @return True if the garage door is currently vertical.
     */
    private boolean isVertical(IStructureConst structure)
    {
        return structure.getCuboid().getDimensions().y() > 1;
    }

    @Override
    public double calculateAnimationCycleDistance(IStructureConst structure)
    {
        final Cuboid cuboid = structure.getCuboid();
        final Vector3Di dims = cuboid.getDimensions();

        final double movement;
        if (isVertical(structure))
            movement = dims.y();
        else
            movement = isNorthSouthAnimated(structure) ? dims.z() : dims.x();
        // Not exactly correct, but much faster and pretty close.
        return 2 * movement;
    }

    @Override
    public Rectangle calculateAnimationRange(IStructureConst structure)
    {
        final Cuboid cuboid = structure.getCuboid();
        if (!isVertical(structure))
            return cuboid.grow(1, 1, 1).asFlatRectangle();

        final int vertical = cuboid.getDimensions().y();
        final Vector3Di min = cuboid.getMin();
        final Vector3Di max = cuboid.getMax();

        final Cuboid cuboidRange = switch (getCurrentToggleDirection(structure))
        {
            case NORTH -> new Cuboid(min.add(0, 0, -vertical), max.add(0, 1, 0)); // -z
            case EAST -> new Cuboid(min.add(0, 0, 0), max.add(vertical, 1, 0)); // +x
            case SOUTH -> new Cuboid(min.add(0, 0, 0), max.add(0, 1, vertical)); // +z
            case WEST -> new Cuboid(min.add(-vertical, 0, 0), max.add(0, 1, 0)); // -x
            default -> cuboid.grow(vertical, 0, vertical);
        };
        return cuboidRange.asFlatRectangle();
    }

    @Override
    public MovementDirection getCycledOpenDirection(IStructureConst structure)
    {
        final MovementDirection openDirection = structure.getOpenDirection();

        if (isNorthSouthAnimated(structure))
            return openDirection.equals(MovementDirection.EAST) ? MovementDirection.WEST : MovementDirection.EAST;
        return openDirection.equals(MovementDirection.NORTH) ? MovementDirection.SOUTH : MovementDirection.NORTH;
    }

    @Override
    public Optional<Cuboid> getPotentialNewCoordinates(IStructureConst structure)
    {
        final MovementDirection movementDirection = getCurrentToggleDirection(structure);
        final double angle = switch (movementDirection)
        {
            case NORTH, WEST -> MathUtil.HALF_PI;
            case SOUTH, EAST -> -MathUtil.HALF_PI;
            default -> throw new IllegalArgumentException(
                "Invalid movement direction '" + movementDirection + "'" + " for structure: " + this);
        };

        final Vector3Di rotationPoint = structure.getRequiredPropertyValue(Property.ROTATION_POINT);
        final Cuboid cuboid = structure.getCuboid();

        if (movementDirection == MovementDirection.NORTH || movementDirection == MovementDirection.SOUTH)
            return Optional.of(cuboid.updatePositions(vec -> vec.rotateAroundXAxis(rotationPoint, angle)));
        else
            return Optional.of(cuboid.updatePositions(vec -> vec.rotateAroundZAxis(rotationPoint, angle)));
    }

    @Override
    public IAnimationComponent constructAnimationComponent(IStructureConst structure, AnimationRequestData data)
    {
        return USE_COUNTER_WEIGHT ?
            new CounterWeightGarageDoorAnimationComponent(data, getCurrentToggleDirection(structure)) :
            new SectionalGarageDoorAnimationComponent(data, getCurrentToggleDirection(structure));
    }
}
