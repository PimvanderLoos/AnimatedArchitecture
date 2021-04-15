package nl.pim16aap2.bigdoors.doors.revolvingdoor;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import nl.pim16aap2.bigdoors.annotations.PersistentVariable;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.doors.AbstractDoorBase;
import nl.pim16aap2.bigdoors.doors.DoorOpeningUtility;
import nl.pim16aap2.bigdoors.doors.doorArchetypes.IStationaryDoorArchetype;
import nl.pim16aap2.bigdoors.doortypes.DoorType;
import nl.pim16aap2.bigdoors.events.dooraction.DoorActionCause;
import nl.pim16aap2.bigdoors.events.dooraction.DoorActionType;
import nl.pim16aap2.bigdoors.moveblocks.BlockMover;
import nl.pim16aap2.bigdoors.util.CuboidConst;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a Revolving Door doorType.
 *
 * @author Pim
 * @see AbstractDoorBase
 */
public class RevolvingDoor extends AbstractDoorBase implements IStationaryDoorArchetype
{
    @NonNull
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

    public RevolvingDoor(final @NonNull DoorData doorData, final int quarterCircles)
    {
        super(doorData);
        this.quarterCircles = quarterCircles;
    }

    public RevolvingDoor(final @NonNull DoorData doorData)
    {
        this(doorData, 1);
    }

    @Override
    public @NonNull DoorType getDoorType()
    {
        return DOOR_TYPE;
    }

    @Override
    public synchronized @NonNull RotateDirection getCurrentToggleDir()
    {
        return getOpenDir();
    }

    @Override
    public @NonNull RotateDirection cycleOpenDirection()
    {
        return getOpenDir().equals(RotateDirection.CLOCKWISE) ?
               RotateDirection.COUNTERCLOCKWISE : RotateDirection.CLOCKWISE;
    }

    @Override
    protected @NonNull BlockMover constructBlockMover(final @NonNull DoorActionCause cause, final double time,
                                                      final boolean skipAnimation, final @NonNull CuboidConst newCuboid,
                                                      final @NonNull IPPlayer responsible,
                                                      final @NonNull DoorActionType actionType)
        throws Exception
    {
        // TODO: Get rid of this.
        double fixedTime = time < 0.5 ? 5 : time;

        return new RevolvingDoorMover(this, fixedTime, DoorOpeningUtility.getMultiplier(this), getCurrentToggleDir(),
                                      responsible, quarterCircles, cause, newCuboid, actionType);
    }

    @Override
    public boolean equals(@Nullable Object o)
    {
        if (!super.equals(o))
            return false;

        if (getClass() != o.getClass())
            return false;

        final @NonNull RevolvingDoor other = (RevolvingDoor) o;
        return quarterCircles == other.quarterCircles;
    }
}
