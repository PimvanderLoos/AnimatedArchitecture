package nl.pim16aap2.bigdoors.doors.bigdoor;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.flogger.Flogger;
import nl.pim16aap2.bigdoors.annotations.PersistentVariable;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.doors.AbstractDoor;
import nl.pim16aap2.bigdoors.doors.DoorBase;
import nl.pim16aap2.bigdoors.doors.doorarchetypes.ITimerToggleable;
import nl.pim16aap2.bigdoors.doortypes.DoorType;
import nl.pim16aap2.bigdoors.events.dooraction.DoorActionCause;
import nl.pim16aap2.bigdoors.events.dooraction.DoorActionType;
import nl.pim16aap2.bigdoors.moveblocks.BlockMover;
import nl.pim16aap2.bigdoors.util.Cuboid;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import nl.pim16aap2.bigdoors.util.vector.Vector3Di;

import javax.annotation.concurrent.GuardedBy;
import java.util.Optional;
import java.util.logging.Level;
import java.util.stream.Stream;

/**
 * Represents a Big Door doorType.
 *
 * @author Pim
 * @see AbstractDoor
 */
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@Flogger
public class BigDoor extends AbstractDoor implements ITimerToggleable
{
    private static final DoorType DOOR_TYPE = DoorTypeBigDoor.get();
    private static final double HALF_PI = Math.PI / 2;

    @Getter
    private final double longestAnimationCycleDistance;

    @PersistentVariable
    @GuardedBy("this")
    protected int autoCloseTime;

    @PersistentVariable
    @GuardedBy("this")
    protected int autoOpenTime;

    public BigDoor(DoorBase doorBase, int autoCloseTime, int autoOpenTime)
    {
        super(doorBase);
        this.autoCloseTime = autoCloseTime;
        this.autoOpenTime = autoOpenTime;
        this.longestAnimationCycleDistance =
            calculateLongestAnimationCycleDistance(getCuboid(), getRotationPoint());
    }

    public BigDoor(DoorBase doorBase)
    {
        this(doorBase, -1, -1); // Add tmp/default values
    }

    @Override
    public DoorType getDoorType()
    {
        return DOOR_TYPE;
    }

    @Override
    public boolean canSkipAnimation()
    {
        return true;
    }

    @Override
    public RotateDirection cycleOpenDirection()
    {
        return getOpenDir().equals(RotateDirection.CLOCKWISE) ?
               RotateDirection.COUNTERCLOCKWISE : RotateDirection.CLOCKWISE;
    }

    @Override
    public RotateDirection getCurrentToggleDir()
    {
        synchronized (getDoorBase())
        {
            return isOpen() ? RotateDirection.getOpposite(getOpenDir()) : getOpenDir();
        }
    }

    @Override
    public synchronized Optional<Cuboid> getPotentialNewCoordinates()
    {
        final Vector3Di rotationPoint;
        final RotateDirection rotateDirection;
        final Cuboid cuboid;

        synchronized (getDoorBase())
        {
            rotationPoint = getRotationPoint();
            rotateDirection = getCurrentToggleDir();
            cuboid = getCuboid();
        }

        final double angle = rotateDirection == RotateDirection.CLOCKWISE ? Math.PI / 2 :
                             rotateDirection == RotateDirection.COUNTERCLOCKWISE ? -Math.PI / 2 : 0.0D;
        if (angle == 0.0D)
        {
            log.at(Level.SEVERE).log("Invalid open direction '%s' for door: %d", rotateDirection.name(), getDoorUID());
            return Optional.empty();
        }

        return Optional.of(cuboid.updatePositions(vec -> vec.rotateAroundYAxis(rotationPoint, angle)));
    }

    /**
     * @return The maximum distance from the rotation point to one of the corners of the door.
     */
    public static double calculateLongestAnimationCycleDistance(Cuboid cuboid, Vector3Di rotationPoint)
    {
        final Vector3Di min = cuboid.getMin();
        final Vector3Di max = cuboid.getMax();
        final Vector3Di other0 = new Vector3Di(min.x(), min.y(), max.z());
        final Vector3Di other1 = new Vector3Di(max.x(), min.y(), min.z());

        return Stream.of(min, max, other0, other1)
                     .mapToDouble(val -> BigDoorMover.getRadius(rotationPoint, val.x(), val.z()))
                     .max().orElseThrow() * HALF_PI;
    }

    @Override
    protected synchronized BlockMover constructBlockMover(
        BlockMover.Context context, DoorActionCause cause, double time,
        boolean skipAnimation, Cuboid newCuboid, IPPlayer responsible,
        DoorActionType actionType)
        throws Exception
    {
        return new BigDoorMover(
            context, this, getCurrentToggleDir(), time, skipAnimation, responsible, newCuboid, cause, actionType);
    }

    @Override
    public synchronized void setAutoCloseTime(int autoCloseTime)
    {
        this.autoCloseTime = autoCloseTime;
    }

    @Override
    public synchronized void setAutoOpenTime(int autoOpenTime)
    {
        this.autoOpenTime = autoOpenTime;
    }

    @Override
    public synchronized int getAutoCloseTime()
    {
        return this.autoCloseTime;
    }

    @Override
    public synchronized int getAutoOpenTime()
    {
        return this.autoOpenTime;
    }
}
