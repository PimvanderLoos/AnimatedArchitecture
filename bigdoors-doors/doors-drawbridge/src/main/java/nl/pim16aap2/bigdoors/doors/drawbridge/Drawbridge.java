package nl.pim16aap2.bigdoors.doors.drawbridge;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Locked;
import lombok.extern.flogger.Flogger;
import nl.pim16aap2.bigdoors.annotations.PersistentVariable;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.doors.AbstractDoor;
import nl.pim16aap2.bigdoors.doors.DoorBase;
import nl.pim16aap2.bigdoors.doors.DoorSnapshot;
import nl.pim16aap2.bigdoors.doors.doorarchetypes.IHorizontalAxisAligned;
import nl.pim16aap2.bigdoors.doors.doorarchetypes.ITimerToggleable;
import nl.pim16aap2.bigdoors.doortypes.DoorType;
import nl.pim16aap2.bigdoors.events.dooraction.DoorActionCause;
import nl.pim16aap2.bigdoors.events.dooraction.DoorActionType;
import nl.pim16aap2.bigdoors.moveblocks.BlockMover;
import nl.pim16aap2.bigdoors.util.Cuboid;
import nl.pim16aap2.bigdoors.util.MathUtil;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import nl.pim16aap2.bigdoors.util.vector.Vector3Di;

import javax.annotation.concurrent.GuardedBy;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.stream.Stream;

/**
 * Represents a Drawbridge doorType.
 *
 * @author Pim
 */
@EqualsAndHashCode(callSuper = true)
@Flogger
public class Drawbridge extends AbstractDoor implements IHorizontalAxisAligned, ITimerToggleable
{
    private static final DoorType DOOR_TYPE = DoorTypeDrawbridge.get();

    @EqualsAndHashCode.Exclude
    private final ReentrantReadWriteLock lock;

    @Getter
    private final double longestAnimationCycleDistance;

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

    public Drawbridge(DoorBase doorBase, int autoCloseTime, int autoOpenTime, boolean modeUp)
    {
        super(doorBase);
        this.lock = getLock();
        this.autoOpenTime = autoOpenTime;
        this.autoCloseTime = autoCloseTime;
        this.modeUp = modeUp;
        this.longestAnimationCycleDistance =
            calculateLongestAnimationCycleDistance(isNorthSouthAligned(), getCuboid(), getRotationPoint());
    }

    public Drawbridge(DoorBase doorBase, boolean modeUp)
    {
        this(doorBase, -1, -1, modeUp);
    }

    @SuppressWarnings("unused")
    private Drawbridge(DoorBase doorBase)
    {
        this(doorBase, false); // Add tmp/default values
    }

    @Override
    public DoorType getDoorType()
    {
        return DOOR_TYPE;
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
            log.at(Level.SEVERE).log("Invalid open direction '%s' for door: %d", rotateDirection.name(), getDoorUID());
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
        BlockMover.Context context, DoorSnapshot doorSnapshot, DoorActionCause cause, double time,
        boolean skipAnimation, Cuboid newCuboid, IPPlayer responsible,
        DoorActionType actionType)
        throws Exception
    {
        return new BridgeMover<>(
            context, this, doorSnapshot, time, getCurrentToggleDir(), skipAnimation,
            config.getAnimationSpeedMultiplier(getDoorType()), responsible, newCuboid, cause, actionType);
    }

    @Override
    public boolean isNorthSouthAligned()
    {
        final RotateDirection openDir = getOpenDir();
        return openDir == RotateDirection.NORTH || openDir == RotateDirection.SOUTH;
    }

    /**
     * @return The maximum distance from the rotation point to one of the corners of the door.
     */
    public static double calculateLongestAnimationCycleDistance(
        boolean northSouthAligned, Cuboid cuboid, Vector3Di rotationPoint)
    {
        final Vector3Di min = cuboid.getMin();
        final Vector3Di max = cuboid.getMax();
        final Vector3Di other0 = new Vector3Di(min.x(), min.y(), max.z());
        final Vector3Di other1 = new Vector3Di(max.x(), min.y(), min.z());

        return Stream
            .of(min, max, other0, other1)
            .mapToDouble(val -> BridgeMover.getRadius(northSouthAligned, rotationPoint, val.x(), val.y(), val.z()))
            .max().orElseThrow() * MathUtil.HALF_PI;
    }
}
