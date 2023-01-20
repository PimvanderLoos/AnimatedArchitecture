package nl.pim16aap2.bigdoors.movable.drawbridge;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Locked;
import lombok.extern.flogger.Flogger;
import nl.pim16aap2.bigdoors.annotations.PersistentVariable;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.events.movableaction.MovableActionCause;
import nl.pim16aap2.bigdoors.events.movableaction.MovableActionType;
import nl.pim16aap2.bigdoors.movable.AbstractMovable;
import nl.pim16aap2.bigdoors.movable.MovableBase;
import nl.pim16aap2.bigdoors.movable.MovableSnapshot;
import nl.pim16aap2.bigdoors.movable.movablearchetypes.IHorizontalAxisAligned;
import nl.pim16aap2.bigdoors.movable.movablearchetypes.ITimerToggleable;
import nl.pim16aap2.bigdoors.movabletypes.MovableType;
import nl.pim16aap2.bigdoors.moveblocks.BlockMover;
import nl.pim16aap2.bigdoors.util.Cuboid;
import nl.pim16aap2.bigdoors.util.MathUtil;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import nl.pim16aap2.bigdoors.util.vector.Vector3Di;

import javax.annotation.concurrent.GuardedBy;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Stream;

/**
 * Represents a Drawbridge movable type.
 *
 * @author Pim
 */
@EqualsAndHashCode(callSuper = true)
@Flogger
public class Drawbridge extends AbstractMovable implements IHorizontalAxisAligned, ITimerToggleable
{
    private static final MovableType MOVABLE_TYPE = MovableTypeDrawbridge.get();

    @EqualsAndHashCode.Exclude
    private final ReentrantReadWriteLock lock;

    @Getter
    private final double longestAnimationCycleDistance;

    @Getter
    private final Cuboid animationRange;

    @PersistentVariable
    @GuardedBy("lock")
    @Getter(onMethod_ = @Locked.Read)
    @Setter(onMethod_ = @Locked.Write)
    protected int autoCloseTime;

    @PersistentVariable
    @GuardedBy("lock")
    @Getter(onMethod_ = @Locked.Read)
    @Setter(onMethod_ = @Locked.Write)
    protected int autoOpenTime;

    /**
     * Describes if this drawbridge's vertical position points (when taking the rotation point Y value as center) up
     * <b>(= TRUE)</b> or down <b>(= FALSE)</b>
     *
     * @return True if this {@link Drawbridge}'s vertical stance points up.
     */
    @PersistentVariable
    @GuardedBy("lock")
    @Getter(onMethod_ = @Locked.Read)
    @Setter(onMethod_ = @Locked.Write)
    protected boolean modeUp;

    public Drawbridge(MovableBase base, int autoCloseTime, int autoOpenTime, boolean modeUp)
    {
        super(base);
        this.lock = getLock();
        this.autoOpenTime = autoOpenTime;
        this.autoCloseTime = autoCloseTime;
        this.modeUp = modeUp;

        final double maxRadius = getMaxRadius(isNorthSouthAligned(), getCuboid(), getRotationPoint());
        this.longestAnimationCycleDistance = maxRadius * MathUtil.HALF_PI;
        this.animationRange = calculateAnimationRange(maxRadius, getCuboid());
    }

    public Drawbridge(MovableBase base, boolean modeUp)
    {
        this(base, -1, -1, modeUp);
    }

    @SuppressWarnings("unused")
    private Drawbridge(MovableBase base)
    {
        this(base, false); // Add tmp/default values
    }

    @Override
    public MovableType getType()
    {
        return MOVABLE_TYPE;
    }

    @Override
    @Locked.Read
    public RotateDirection getCurrentToggleDir()
    {
        return isOpen() ? RotateDirection.getOpposite(getOpenDir()) : getOpenDir();
    }

    @Override
    @Locked.Read
    public Optional<Cuboid> getPotentialNewCoordinates()
    {
        final RotateDirection rotateDirection = getCurrentToggleDir();
        final Cuboid cuboid = getCuboid();
        final Vector3Di rotationPoint = getRotationPoint();

        final double angle;
        if (rotateDirection == RotateDirection.NORTH || rotateDirection == RotateDirection.WEST)
            angle = -MathUtil.HALF_PI;
        else if (rotateDirection == RotateDirection.SOUTH || rotateDirection == RotateDirection.EAST)
            angle = MathUtil.HALF_PI;
        else
        {
            log.atSevere()
               .log("Invalid open direction '%s' for door: %d", rotateDirection.name(), getUid());
            return Optional.empty();
        }

        if (rotateDirection == RotateDirection.NORTH || rotateDirection == RotateDirection.SOUTH)
            return Optional.of(cuboid.updatePositions(vec -> vec.rotateAroundXAxis(rotationPoint, angle)));
        else
            return Optional.of(cuboid.updatePositions(vec -> vec.rotateAroundZAxis(rotationPoint, angle)));
    }

    @Override
    @Locked.Read
    protected BlockMover constructBlockMover(
        BlockMover.Context context, MovableSnapshot movableSnapshot, MovableActionCause cause, double time,
        boolean skipAnimation, Cuboid newCuboid, IPPlayer responsible,
        MovableActionType actionType)
        throws Exception
    {
        return new BridgeMover<>(
            context, this, movableSnapshot, time, getCurrentToggleDir(), skipAnimation,
            config.getAnimationSpeedMultiplier(getType()), responsible, newCuboid, cause, actionType);
    }

    @Override
    public boolean isNorthSouthAligned()
    {
        final RotateDirection openDir = getOpenDir();
        return openDir == RotateDirection.NORTH || openDir == RotateDirection.SOUTH;
    }

    /**
     * @param maxRadius
     *     See {@link #getMaxRadius(boolean, Cuboid, Vector3Di)}.
     * @param cuboid
     *     The cuboid that describes this door.
     * @return The animation range.
     */
    public static Cuboid calculateAnimationRange(double maxRadius, Cuboid cuboid)
    {
        final int radius = (int) Math.ceil(maxRadius);
        return new Cuboid(cuboid.getMin().add(-radius), cuboid.getMin().add(radius));
    }

    /**
     * @param cuboid
     *     The cuboid that describes this door.
     * @param rotationPoint
     *     The rotation point of the door.
     * @return The radius between the rotation point of the door and the animated block furthest from it.
     */
    public static double getMaxRadius(boolean northSouthAligned, Cuboid cuboid, Vector3Di rotationPoint)
    {
        return Stream
            .of(cuboid.getCorners())
            .mapToDouble(val -> BridgeMover.getRadius(northSouthAligned, rotationPoint, val.x(), val.y(), val.z()))
            .max().orElseThrow();
    }
}
