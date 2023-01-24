package nl.pim16aap2.bigdoors.movable.windmill;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Locked;
import nl.pim16aap2.bigdoors.annotations.InheritedLockField;
import nl.pim16aap2.bigdoors.annotations.PersistentVariable;
import nl.pim16aap2.bigdoors.movable.AbstractMovable;
import nl.pim16aap2.bigdoors.movable.MovableBase;
import nl.pim16aap2.bigdoors.movable.drawbridge.Drawbridge;
import nl.pim16aap2.bigdoors.movable.movablearchetypes.IHorizontalAxisAligned;
import nl.pim16aap2.bigdoors.movable.movablearchetypes.IPerpetualMover;
import nl.pim16aap2.bigdoors.movabletypes.MovableType;
import nl.pim16aap2.bigdoors.moveblocks.BlockMover;
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
    private static final MovableType MOVABLE_TYPE = MovableTypeWindmill.get();

    @EqualsAndHashCode.Exclude
    @InheritedLockField
    private final ReentrantReadWriteLock lock;

    @Getter
    private final double longestAnimationCycleDistance;

    @Getter
    private final Rectangle animationRange;

    /**
     * The number of quarter circles (so 90 degree rotations) this movable will make before stopping.
     *
     * @return The number of quarter circles this movable will rotate.
     */
    @PersistentVariable
    @GuardedBy("lock")
    @Getter(onMethod_ = @Locked.Read)
    @Setter(onMethod_ = @Locked.Write)
    private int quarterCircles;

    public Windmill(MovableBase base, int quarterCircles)
    {
        super(base);
        this.lock = getLock();
        this.quarterCircles = quarterCircles;

        final double maxRadius = Drawbridge.getMaxRadius(isNorthSouthAligned(), getCuboid(), getRotationPoint());
        this.longestAnimationCycleDistance = maxRadius * MathUtil.HALF_PI;
        this.animationRange = Drawbridge.calculateAnimationRange(maxRadius, getCuboid());
    }

    public Windmill(MovableBase doorBase)
    {
        this(doorBase, 1);
    }

    @Override
    public MovableType getType()
    {
        return MOVABLE_TYPE;
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
    public MovementDirection cycleOpenDirection()
    {
        final MovementDirection openDir = getOpenDir();
        return openDir.equals(MovementDirection.NORTH) ? MovementDirection.EAST :
               openDir.equals(MovementDirection.EAST) ? MovementDirection.SOUTH :
               openDir.equals(MovementDirection.SOUTH) ? MovementDirection.WEST : MovementDirection.NORTH;
    }

    @Override
    @Locked.Read
    protected BlockMover constructBlockMover(MovementRequestData data)
        throws Exception
    {
        return new WindmillMover<>(this, data, getCurrentToggleDir());
    }
}
