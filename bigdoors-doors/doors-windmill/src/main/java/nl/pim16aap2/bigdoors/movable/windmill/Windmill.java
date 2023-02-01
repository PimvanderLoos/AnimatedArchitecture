package nl.pim16aap2.bigdoors.movable.windmill;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Locked;
import nl.pim16aap2.bigdoors.movable.AbstractMovable;
import nl.pim16aap2.bigdoors.movable.drawbridge.Drawbridge;
import nl.pim16aap2.bigdoors.movable.movablearchetypes.IHorizontalAxisAligned;
import nl.pim16aap2.bigdoors.movable.movablearchetypes.IPerpetualMover;
import nl.pim16aap2.bigdoors.movable.serialization.DeserializationConstructor;
import nl.pim16aap2.bigdoors.movable.serialization.PersistentVariable;
import nl.pim16aap2.bigdoors.moveblocks.IAnimationComponent;
import nl.pim16aap2.bigdoors.moveblocks.MovementRequestData;
import nl.pim16aap2.bigdoors.util.Cuboid;
import nl.pim16aap2.bigdoors.util.MathUtil;
import nl.pim16aap2.bigdoors.util.MovementDirection;
import nl.pim16aap2.bigdoors.util.Rectangle;

import javax.annotation.concurrent.GuardedBy;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Represents a Windmill movable type.
 *
 * @author Pim
 */
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class Windmill extends AbstractMovable implements IHorizontalAxisAligned, IPerpetualMover
{
    @EqualsAndHashCode.Exclude
    @SuppressWarnings({"FieldCanBeLocal", "unused"})
    private final ReentrantReadWriteLock lock;

    /**
     * The number of quarter circles (so 90 degree rotations) this movable will make before stopping.
     *
     * @return The number of quarter circles this movable will rotate.
     */
    @PersistentVariable("quarterCircles")
    @GuardedBy("lock")
    @Getter(onMethod_ = @Locked.Read)
    @Setter(onMethod_ = @Locked.Write)
    private int quarterCircles;

    @DeserializationConstructor
    public Windmill(AbstractMovable.MovableBaseHolder base, @PersistentVariable("quarterCircles") int quarterCircles)
    {
        super(base, MovableTypeWindmill.get());
        this.lock = getLock();
        this.quarterCircles = quarterCircles;
    }

    public Windmill(AbstractMovable.MovableBaseHolder doorBase)
    {
        this(doorBase, 1);
    }

    @Override
    public boolean canSkipAnimation()
    {
        return false;
    }

    @Override
    public Optional<Cuboid> getPotentialNewCoordinates()
    {
        return Optional.of(getCuboid());
    }

    @Override
    public MovementDirection getCurrentToggleDir()
    {
        return getOpenDir();
    }

    @Override
    public boolean isNorthSouthAligned()
    {
        final MovementDirection openDir = getOpenDir();
        return openDir == MovementDirection.EAST || openDir == MovementDirection.WEST;
    }

    @Override
    @Locked.Read
    protected double calculateAnimationCycleDistance()
    {
        final double maxRadius = Drawbridge.getMaxRadius(isNorthSouthAligned(), getCuboid(), getRotationPoint());
        return maxRadius * MathUtil.HALF_PI;
    }

    @Override
    @Locked.Read
    protected Rectangle calculateAnimationRange()
    {
        final double maxRadius = Drawbridge.getMaxRadius(isNorthSouthAligned(), getCuboid(), getRotationPoint());
        return Drawbridge.calculateAnimationRange(maxRadius, getCuboid());
    }

    @Override
    public MovementDirection cycleOpenDirection()
    {
        final MovementDirection openDir = getOpenDir();
        return openDir.equals(MovementDirection.NORTH) ? MovementDirection.EAST :
               openDir.equals(MovementDirection.EAST) ? MovementDirection.SOUTH :
               openDir.equals(MovementDirection.SOUTH) ? MovementDirection.WEST : MovementDirection.NORTH;
    }

    @Override
    @Locked.Read
    protected IAnimationComponent constructAnimationComponent(MovementRequestData data)
    {
        return new WindmillAnimationComponent(data, getCurrentToggleDir(), isNorthSouthAligned());
    }
}
