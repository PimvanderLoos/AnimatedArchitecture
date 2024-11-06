package nl.pim16aap2.animatedarchitecture.structures.bigdoor;

import lombok.EqualsAndHashCode;
import lombok.Locked;
import lombok.ToString;
import lombok.extern.flogger.Flogger;
import nl.pim16aap2.animatedarchitecture.core.animation.AnimationRequestData;
import nl.pim16aap2.animatedarchitecture.core.animation.IAnimationComponent;
import nl.pim16aap2.animatedarchitecture.core.annotations.Deserialization;
import nl.pim16aap2.animatedarchitecture.core.annotations.PersistentVariable;
import nl.pim16aap2.animatedarchitecture.core.structures.AbstractStructure;
import nl.pim16aap2.animatedarchitecture.core.structures.properties.IStructureWithOpenStatus;
import nl.pim16aap2.animatedarchitecture.core.structures.properties.IStructureWithQuarterCircles;
import nl.pim16aap2.animatedarchitecture.core.structures.properties.IStructureWithRotationPoint;
import nl.pim16aap2.animatedarchitecture.core.util.Cuboid;
import nl.pim16aap2.animatedarchitecture.core.util.MathUtil;
import nl.pim16aap2.animatedarchitecture.core.util.MovementDirection;
import nl.pim16aap2.animatedarchitecture.core.util.Rectangle;
import nl.pim16aap2.animatedarchitecture.core.util.vector.Vector3Di;

import java.util.Optional;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Stream;

/**
 * Represents a Big Door structure type.
 *
 * @see AbstractStructure
 */
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@Flogger
public class BigDoor
    extends AbstractStructure
    implements
    IStructureWithOpenStatus,
    IStructureWithQuarterCircles,
    IStructureWithRotationPoint
{
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    @SuppressWarnings({"FieldCanBeLocal", "unused"})
    private final ReentrantReadWriteLock lock;

    @Deserialization
    public BigDoor(BaseHolder base)
    {
        super(base, StructureTypeBigDoor.get());
        this.lock = getLock();
    }

    /**
     * Deprecated constructor for deserialization of version 1 where {@code quarterCircles} was a persistent variable.
     */
    @Deprecated
    @Deserialization(version = 1)
    public BigDoor(BaseHolder base, @PersistentVariable(value = "quarterCircles") int quarterCircles)
    {
        this(base);
        setQuarterCircles(quarterCircles);
    }

    @Override
    public boolean canSkipAnimation()
    {
        return true;
    }

    @Override
    public MovementDirection getCycledOpenDirection()
    {
        return getOpenDir().equals(MovementDirection.CLOCKWISE) ?
            MovementDirection.COUNTERCLOCKWISE :
            MovementDirection.CLOCKWISE;
    }

    @Override
    @Locked.Read("lock")
    public MovementDirection getCurrentToggleDir()
    {
        return isOpen() ? MovementDirection.getOpposite(getOpenDir()) : getOpenDir();
    }

    @Override
    @Locked.Read("lock")
    public Optional<Cuboid> getPotentialNewCoordinates()
    {
        final MovementDirection movementDirection = getCurrentToggleDir();
        final int quarterCircles = getQuarterCircles();

        final double angle;
        if (movementDirection == MovementDirection.CLOCKWISE)
            angle = quarterCircles * MathUtil.HALF_PI;
        else if (movementDirection == MovementDirection.COUNTERCLOCKWISE)
            angle = quarterCircles * -MathUtil.HALF_PI;
        else
        {
            log.atSevere().log("Invalid movement direction '%s' for door: %d", movementDirection.name(), getUid());
            return Optional.empty();
        }

        return Optional.of(getCuboid().updatePositions(vec -> vec.rotateAroundYAxis(getRotationPoint(), angle)));
    }

    @Override
    @Locked.Read("lock")
    protected double calculateAnimationCycleDistance()
    {
        final double maxRadius = getMaxRadius(getCuboid(), getRotationPoint());
        return getQuarterCircles() * maxRadius * MathUtil.HALF_PI;
    }

    @Override
    @Locked.Read("lock")
    protected Rectangle calculateAnimationRange()
    {
        final double maxRadius = getMaxRadius(getCuboid(), getRotationPoint());
        return calculateAnimationRange(maxRadius, getCuboid());
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
    @Locked.Read("lock")
    protected IAnimationComponent constructAnimationComponent(AnimationRequestData data)
    {
        return new BigDoorAnimationComponent(data, getCurrentToggleDir(), getQuarterCircles());
    }
}
