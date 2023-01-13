package nl.pim16aap2.bigdoors.doors.revolvingdoor;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.flogger.Flogger;
import nl.pim16aap2.bigdoors.annotations.PersistentVariable;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.doors.AbstractDoor;
import nl.pim16aap2.bigdoors.doors.DoorBase;
import nl.pim16aap2.bigdoors.doors.bigdoor.BigDoor;
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

/**
 * Represents a Revolving Door doorType.
 *
 * @author Pim
 * @see DoorBase
 */
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@Flogger
public class RevolvingDoor extends AbstractDoor
{
    private static final DoorType DOOR_TYPE = DoorTypeRevolvingDoor.get();

    @Getter
    private final double longestAnimationCycleDistance;

    /**
     * The number of quarter circles (so 90 degree rotations) this door will make before stopping.
     *
     * @return The number of quarter circles this door will rotate.
     */
    @PersistentVariable
    @GuardedBy("this")
    private int quarterCircles;

    public RevolvingDoor(DoorBase doorBase, int quarterCircles)
    {
        super(doorBase);
        this.quarterCircles = quarterCircles;
        this.longestAnimationCycleDistance =
            BigDoor.calculateLongestAnimationCycleDistance(getCuboid(), getRotationPoint());
    }

    public RevolvingDoor(DoorBase doorBase)
    {
        this(doorBase, 1);
    }

    @Override
    public DoorType getDoorType()
    {
        return DOOR_TYPE;
    }

    @Override
    public Optional<Cuboid> getPotentialNewCoordinates()
    {
        final RotateDirection rotateDirection;
        final Vector3Di rotationPoint;
        final Cuboid cuboid;

        synchronized (getDoorBase())
        {
            rotateDirection = getCurrentToggleDir();
            rotationPoint = getRotationPoint();
            cuboid = getCuboid();
        }

        final double angle = rotateDirection == RotateDirection.CLOCKWISE ? Math.PI / 2 :
                             rotateDirection == RotateDirection.COUNTERCLOCKWISE ? -Math.PI / 2 : 0.0D;
        if (angle == 0.0D)
        {
            log.at(Level.SEVERE)
               .log("Invalid open direction '%s' for revolving door: %d", rotateDirection.name(), getDoorUID());
            return Optional.empty();
        }

        return Optional.of(cuboid.updatePositions(vec -> vec.rotateAroundYAxis(rotationPoint, angle)));
    }

    @Override
    public RotateDirection getCurrentToggleDir()
    {
        return getOpenDir();
    }

    @Override
    protected synchronized BlockMover constructBlockMover(
        BlockMover.Context context, DoorActionCause cause, double time,
        boolean skipAnimation, Cuboid newCuboid, IPPlayer responsible,
        DoorActionType actionType)
        throws Exception
    {
        return new RevolvingDoorMover(
            context, this, time, config.getAnimationSpeedMultiplier(getDoorType()), getCurrentToggleDir(), responsible,
            quarterCircles, cause, newCuboid, actionType);
    }

    @Override
    public boolean isOpenable()
    {
        return true;
    }

    @Override
    public boolean isCloseable()
    {
        return true;
    }

    public synchronized int getQuarterCircles()
    {
        return this.quarterCircles;
    }

    public synchronized void setQuarterCircles(int quarterCircles)
    {
        this.quarterCircles = quarterCircles;
    }
}
