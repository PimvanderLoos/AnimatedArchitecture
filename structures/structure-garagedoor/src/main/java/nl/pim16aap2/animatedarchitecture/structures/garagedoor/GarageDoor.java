package nl.pim16aap2.animatedarchitecture.structures.garagedoor;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.Locked;
import lombok.extern.flogger.Flogger;
import nl.pim16aap2.animatedarchitecture.core.annotations.Deserialization;
import nl.pim16aap2.animatedarchitecture.core.annotations.PersistentVariable;
import nl.pim16aap2.animatedarchitecture.core.moveblocks.AnimationRequestData;
import nl.pim16aap2.animatedarchitecture.core.moveblocks.IAnimationComponent;
import nl.pim16aap2.animatedarchitecture.core.structures.AbstractStructure;
import nl.pim16aap2.animatedarchitecture.core.structures.structurearchetypes.IHorizontalAxisAligned;
import nl.pim16aap2.animatedarchitecture.core.util.Cuboid;
import nl.pim16aap2.animatedarchitecture.core.util.MathUtil;
import nl.pim16aap2.animatedarchitecture.core.util.MovementDirection;
import nl.pim16aap2.animatedarchitecture.core.util.Rectangle;
import nl.pim16aap2.animatedarchitecture.core.util.vector.Vector3Di;

import java.util.Optional;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Represents a Garage Door structure type.
 *
 * @author Pim
 */
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@Flogger
public class GarageDoor extends AbstractStructure implements IHorizontalAxisAligned
{
    private static final boolean USE_COUNTER_WEIGHT = true;

    @EqualsAndHashCode.Exclude @ToString.Exclude
    @SuppressWarnings({"FieldCanBeLocal", "unused"})
    private final ReentrantReadWriteLock lock;

    @Getter
    @PersistentVariable(value = "northSouthAnimated")
    protected final boolean northSouthAnimated;

    @Deserialization
    public GarageDoor(BaseHolder base, @PersistentVariable(value = "northSouthAnimated") boolean northSouthAnimated)
    {
        super(base, StructureTypeGarageDoor.get());
        this.lock = getLock();
        this.northSouthAnimated = northSouthAnimated;
    }

    /**
     * @return True if the garage door is currently vertical.
     */
    private boolean isVertical()
    {
        return getCuboid().getDimensions().y() > 1;
    }

    @Override
    @Locked.Read
    protected double calculateAnimationCycleDistance()
    {
        final Cuboid cuboid = getCuboid();
        final Vector3Di dims = cuboid.getDimensions();

        final double movement;
        if (isVertical())
            movement = dims.y();
        else
            movement = isNorthSouthAnimated() ? dims.z() : dims.x();
        // Not exactly correct, but much faster and pretty close.
        return 2 * movement;
    }

    @Override
    @Locked.Read
    protected Rectangle calculateAnimationRange()
    {
        final Cuboid cuboid = getCuboid();
        if (!isVertical())
            return cuboid.grow(1, 1, 1).asFlatRectangle();

        final int vertical = cuboid.getDimensions().y();
        final Vector3Di min = cuboid.getMin();
        final Vector3Di max = cuboid.getMax();

        final Cuboid cuboidRange = switch (getCurrentToggleDir())
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
    @Locked.Read
    public MovementDirection getCurrentToggleDir()
    {
        final MovementDirection movementDirection = getOpenDir();
        if (isOpen())
            return MovementDirection.getOpposite(movementDirection);
        return movementDirection;
    }

    @Override
    public MovementDirection cycleOpenDirection()
    {
        if (isNorthSouthAnimated())
            return getOpenDir().equals(MovementDirection.EAST) ? MovementDirection.WEST : MovementDirection.EAST;
        return getOpenDir().equals(MovementDirection.NORTH) ? MovementDirection.SOUTH : MovementDirection.NORTH;
    }

    @Override
    @Locked.Read
    public Optional<Cuboid> getPotentialNewCoordinates()
    {
        final MovementDirection movementDirection = getCurrentToggleDir();
        final double angle = switch (movementDirection)
            {
                case NORTH, WEST -> MathUtil.HALF_PI;
                case SOUTH, EAST -> -MathUtil.HALF_PI;
                default -> throw new IllegalArgumentException(
                    "Invalid movement direction '" + movementDirection + "'" + " for structure: " + this);
            };

        if (movementDirection == MovementDirection.NORTH || movementDirection == MovementDirection.SOUTH)
            return Optional.of(getCuboid().updatePositions(vec -> vec.rotateAroundXAxis(getRotationPoint(), angle)));
        else
            return Optional.of(getCuboid().updatePositions(vec -> vec.rotateAroundZAxis(getRotationPoint(), angle)));
    }

    @Override
    @Locked.Read
    protected IAnimationComponent constructAnimationComponent(AnimationRequestData data)
    {
        return USE_COUNTER_WEIGHT ?
               new CounterWeightGarageDoorAnimationComponent(data, getCurrentToggleDir()) :
               new SectionalGarageDoorAnimationComponent(data, getCurrentToggleDir());
    }
}
