package nl.pim16aap2.bigdoors.doors.revolvingdoor;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.flogger.Flogger;
import nl.pim16aap2.bigdoors.annotations.PersistentVariable;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.doors.AbstractDoor;
import nl.pim16aap2.bigdoors.doors.DoorBase;
import nl.pim16aap2.bigdoors.doortypes.DoorType;
import nl.pim16aap2.bigdoors.events.dooraction.DoorActionCause;
import nl.pim16aap2.bigdoors.events.dooraction.DoorActionType;
import nl.pim16aap2.bigdoors.moveblocks.BlockMover;
import nl.pim16aap2.bigdoors.util.Cuboid;
import nl.pim16aap2.bigdoors.util.RotateDirection;

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
    @EqualsAndHashCode.Exclude
    private static final DoorType DOOR_TYPE = DoorTypeRevolvingDoor.get();

    /**
     * The number of quarter circles (so 90 degree rotations) this door will make before stopping.
     *
     * @return The number of quarter circles this door will rotate.
     */
    @Getter
    @Setter
    @PersistentVariable
    private int quarterCircles;

    public RevolvingDoor(DoorBase doorBase, int quarterCircles)
    {
        super(doorBase);
        this.quarterCircles = quarterCircles;
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
        final RotateDirection rotateDirection = getCurrentToggleDir();
        final double angle = rotateDirection == RotateDirection.CLOCKWISE ? Math.PI / 2 :
                             rotateDirection == RotateDirection.COUNTERCLOCKWISE ? -Math.PI / 2 : 0.0D;
        if (angle == 0.0D)
        {
            log.at(Level.SEVERE)
               .log("Invalid open direction '%s' for revolving door: %d", rotateDirection.name(), getDoorUID());
            return Optional.empty();
        }

        return Optional.of(getCuboid().updatePositions(vec -> vec.rotateAroundYAxis(getRotationPoint(), angle)));
    }

    @Override
    public synchronized RotateDirection getCurrentToggleDir()
    {
        return getOpenDir();
    }

    @Override
    protected BlockMover constructBlockMover(BlockMover.Context context, DoorActionCause cause, double time,
                                             boolean skipAnimation, Cuboid newCuboid, IPPlayer responsible,
                                             DoorActionType actionType)
        throws Exception
    {
        // TODO: Get rid of this.
        final double fixedTime = time < 0.5 ? 5 : time;

        return new RevolvingDoorMover(context, this, fixedTime, doorOpeningHelper.getAnimationTime(this),
                                      getCurrentToggleDir(), responsible, quarterCircles, cause, newCuboid, actionType);
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
}
