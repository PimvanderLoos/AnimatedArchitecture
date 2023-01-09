package nl.pim16aap2.bigdoors.doors.bigdoor;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
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
import org.jetbrains.annotations.Nullable;

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

    /**
     * The default speed of the animation in blocks/second, as measured by the fastest-moving block in the door.
     */
    private static final double DEFAULT_ANIMATION_SPEED = 1.5;

    @Getter
    @Setter
    @PersistentVariable
    protected int autoCloseTime;

    @Getter
    @Setter
    @PersistentVariable
    protected int autoOpenTime;

    @EqualsAndHashCode.Exclude
    private @Nullable Double maxRadius;

    public BigDoor(DoorBase doorBase, int autoCloseTime, int autoOpenTime)
    {
        super(doorBase);
        this.autoCloseTime = autoCloseTime;
        this.autoOpenTime = autoOpenTime;
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
        return isOpen() ? RotateDirection.getOpposite(getOpenDir()) : getOpenDir();
    }

    @Override
    public synchronized Optional<Cuboid> getPotentialNewCoordinates()
    {
        final RotateDirection rotateDirection = getCurrentToggleDir();
        final double angle = rotateDirection == RotateDirection.CLOCKWISE ? Math.PI / 2 :
                             rotateDirection == RotateDirection.COUNTERCLOCKWISE ? -Math.PI / 2 : 0.0D;
        if (angle == 0.0D)
        {
            log.at(Level.SEVERE).log("Invalid open direction '%s' for door: %d", rotateDirection.name(), getDoorUID());
            return Optional.empty();
        }

        return Optional.of(getCuboid().updatePositions(vec -> vec.rotateAroundYAxis(getRotationPoint(), angle)));
    }

    /**
     * @return The maximum distance from the rotation point to one of the corners of the door.
     */
    private synchronized double getMaxRadius()
    {
        // No need for double-checking locking or anything like that, as the result will always be
        // the same and is not too expensive to calculate.
        if (maxRadius != null)
            return maxRadius;

        final Cuboid cuboid = getCuboid();
        final Vector3Di rotationPoint = getRotationPoint();
        final Vector3Di min = cuboid.getMin();
        final Vector3Di max = cuboid.getMax();
        final Vector3Di other0 = new Vector3Di(min.x(), min.y(), max.z());
        final Vector3Di other1 = new Vector3Di(max.x(), min.y(), min.z());

        return maxRadius = Stream.of(min, max, other0, other1)
                                 .mapToDouble(val -> BigDoorMover.getRadius(rotationPoint, val.x(), val.z()))
                                 .max().orElseThrow();
    }

    @Override
    public double getMinimumAnimationTime()
    {
        final double distance = getMaxRadius() * HALF_PI;
        return distance / config.maxBlockSpeed();
    }

    @Override
    public double getBaseAnimationTime()
    {
        final double distance = getMaxRadius() * HALF_PI;
        return distance / Math.min(DEFAULT_ANIMATION_SPEED, config.maxBlockSpeed());
    }

    @Override
    protected BlockMover constructBlockMover(
        BlockMover.Context context, DoorActionCause cause, double time,
        boolean skipAnimation, Cuboid newCuboid, IPPlayer responsible,
        DoorActionType actionType)
        throws Exception
    {
        return new BigDoorMover(
            context, this, getCurrentToggleDir(), time, skipAnimation, responsible, newCuboid, cause, actionType);
    }
}
