package nl.pim16aap2.animatedarchitecture.structures.drawbridge;

import com.google.errorprone.annotations.concurrent.GuardedBy;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Locked;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.flogger.Flogger;
import nl.pim16aap2.animatedarchitecture.core.animation.AnimationRequestData;
import nl.pim16aap2.animatedarchitecture.core.animation.IAnimationComponent;
import nl.pim16aap2.animatedarchitecture.core.annotations.Deserialization;
import nl.pim16aap2.animatedarchitecture.core.annotations.PersistentVariable;
import nl.pim16aap2.animatedarchitecture.core.structures.AbstractStructure;
import nl.pim16aap2.animatedarchitecture.core.structures.structurearchetypes.IHorizontalAxisAligned;
import nl.pim16aap2.animatedarchitecture.core.util.Cuboid;
import nl.pim16aap2.animatedarchitecture.core.util.MathUtil;
import nl.pim16aap2.animatedarchitecture.core.util.MovementDirection;
import nl.pim16aap2.animatedarchitecture.core.util.Rectangle;
import nl.pim16aap2.animatedarchitecture.core.util.vector.Vector3Di;

import java.util.Optional;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Stream;

/**
 * Represents a Drawbridge structure type.
 */
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@Flogger
public class Drawbridge extends AbstractStructure implements IHorizontalAxisAligned
{
    @EqualsAndHashCode.Exclude @ToString.Exclude
    @SuppressWarnings({"FieldCanBeLocal", "unused"})
    private final ReentrantReadWriteLock lock;

    /**
     * The number of quarter circles (so 90 degree rotations) this structure will make before stopping.
     *
     * @return The number of quarter circles this structure will rotate.
     */
    @PersistentVariable(value = "quarterCircles")
    @GuardedBy("lock")
    @Getter(onMethod_ = @Locked.Read("lock"))
    @Setter(onMethod_ = @Locked.Write("lock"))
    private int quarterCircles;

    @Deserialization
    public Drawbridge(BaseHolder base, @PersistentVariable(value = "quarterCircles") int quarterCircles)
    {
        super(base, StructureTypeDrawbridge.get());
        this.lock = getLock();
        this.quarterCircles = quarterCircles;
    }

    public Drawbridge(BaseHolder base)
    {
        this(base, 1);
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
        final Cuboid cuboid = getCuboid();
        final Vector3Di rotationPoint = getRotationPoint();

        final double angle;
        if (movementDirection == MovementDirection.NORTH || movementDirection == MovementDirection.WEST)
            angle = quarterCircles * -MathUtil.HALF_PI;
        else if (movementDirection == MovementDirection.SOUTH || movementDirection == MovementDirection.EAST)
            angle = quarterCircles * MathUtil.HALF_PI;
        else
        {
            log.atSevere().log("Invalid open direction '%s' for door: %d", movementDirection.name(), getUid());
            return Optional.empty();
        }

        if (movementDirection == MovementDirection.NORTH || movementDirection == MovementDirection.SOUTH)
            return Optional.of(cuboid.updatePositions(vec -> vec.rotateAroundXAxis(rotationPoint, angle)));
        else
            return Optional.of(cuboid.updatePositions(vec -> vec.rotateAroundZAxis(rotationPoint, angle)));
    }

    @Override
    @Locked.Read("lock")
    protected IAnimationComponent constructAnimationComponent(AnimationRequestData data)
    {
        return new DrawbridgeAnimationComponent(data, getCurrentToggleDir(), isNorthSouthAnimated(), quarterCircles);
    }

    @Override
    public boolean isNorthSouthAnimated()
    {
        final MovementDirection openDir = getOpenDir();
        return openDir == MovementDirection.NORTH || openDir == MovementDirection.SOUTH;
    }

    @Override
    @Locked.Read("lock")
    protected double calculateAnimationCycleDistance()
    {
        final double maxRadius = getMaxRadius(isNorthSouthAnimated(), getCuboid(), getRotationPoint());
        return quarterCircles * maxRadius * MathUtil.HALF_PI;
    }

    @Override
    @Locked.Read("lock")
    protected Rectangle calculateAnimationRange()
    {
        final double maxRadius = getMaxRadius(isNorthSouthAnimated(), getCuboid(), getRotationPoint());
        return calculateAnimationRange(maxRadius, getCuboid());
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
