package nl.pim16aap2.bigdoors.movable.bigdoor;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.Locked;
import lombok.extern.flogger.Flogger;
import nl.pim16aap2.bigdoors.movable.AbstractMovable;
import nl.pim16aap2.bigdoors.movable.serialization.DeserializationConstructor;
import nl.pim16aap2.bigdoors.movabletypes.MovableType;
import nl.pim16aap2.bigdoors.moveblocks.IAnimationComponent;
import nl.pim16aap2.bigdoors.moveblocks.MovementRequestData;
import nl.pim16aap2.bigdoors.util.Cuboid;
import nl.pim16aap2.bigdoors.util.MathUtil;
import nl.pim16aap2.bigdoors.util.MovementDirection;
import nl.pim16aap2.bigdoors.util.Rectangle;
import nl.pim16aap2.bigdoors.util.vector.Vector3Di;

import java.util.Optional;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Stream;

/**
 * Represents a Big Door movable type.
 *
 * @author Pim
 * @see AbstractMovable
 */
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@Flogger
public class BigDoor extends AbstractMovable
{
    private static final MovableType MOVABLE_TYPE = MovableBigDoor.get();

    @EqualsAndHashCode.Exclude
    @SuppressWarnings({"FieldCanBeLocal", "unused"})
    private final ReentrantReadWriteLock lock;

    @DeserializationConstructor
    public BigDoor(AbstractMovable.MovableBaseHolder base)
    {
        super(base);
        this.lock = getLock();
    }

    @Override
    public MovableType getType()
    {
        return MOVABLE_TYPE;
    }

    @Override
    public boolean canSkipAnimation()
    {
        return true;
    }

    @Override
    public MovementDirection cycleOpenDirection()
    {
        return getOpenDir().equals(MovementDirection.CLOCKWISE) ?
               MovementDirection.COUNTERCLOCKWISE : MovementDirection.CLOCKWISE;
    }

    @Override
    @Locked.Read
    public MovementDirection getCurrentToggleDir()
    {
        return isOpen() ? MovementDirection.getOpposite(getOpenDir()) : getOpenDir();
    }

    @Override
    @Locked.Read
    public Optional<Cuboid> getPotentialNewCoordinates()
    {
        final MovementDirection movementDirection = getCurrentToggleDir();
        final double angle = movementDirection == MovementDirection.CLOCKWISE ? MathUtil.HALF_PI :
                             movementDirection == MovementDirection.COUNTERCLOCKWISE ? -MathUtil.HALF_PI : 0.0D;
        if (angle == 0.0D)
        {
            log.atSevere()
               .log("Invalid movement direction '%s' for door: %d", movementDirection.name(), getUid());
            return Optional.empty();
        }

        return Optional.of(getCuboid().updatePositions(vec -> vec.rotateAroundYAxis(getRotationPoint(), angle)));
    }

    @Override
    @Locked.Read
    protected double calculateAnimationCycleDistance()
    {
        final double maxRadius = getMaxRadius(getCuboid(), getRotationPoint());
        return maxRadius * MathUtil.HALF_PI;
    }

    @Override
    @Locked.Read
    protected Rectangle calculateAnimationRange()
    {
        final double maxRadius = getMaxRadius(getCuboid(), getRotationPoint());
        return calculateAnimationRange(maxRadius, getCuboid());
    }

    /**
     * @param maxRadius
     *     See {@link #getMaxRadius(Cuboid, Vector3Di)}.
     * @param cuboid
     *     The cuboid that describes this door.
     * @return The animation range.
     */
    public static Rectangle calculateAnimationRange(double maxRadius, Cuboid cuboid)
    {
        final int radius = (int) Math.ceil(maxRadius);
        return new Cuboid(cuboid.getMin().add(-radius, 0, -radius),
                          cuboid.getMax().add(radius, 0, radius))
            .asFlatRectangle();
    }

    /**
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
                     .max().orElseThrow();
    }

    @Override
    @Locked.Read
    protected IAnimationComponent constructAnimationComponent(MovementRequestData data)
    {
        return new BigDoorAnimationComponent(data, getCurrentToggleDir());
    }
}
