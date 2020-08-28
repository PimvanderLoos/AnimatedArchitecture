package nl.pim16aap2.bigdoors.doors;

import lombok.Getter;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.doors.doorArchetypes.IStationaryDoorArchetype;
import nl.pim16aap2.bigdoors.doortypes.DoorType;
import nl.pim16aap2.bigdoors.doortypes.DoorTypeRevolvingDoor;
import nl.pim16aap2.bigdoors.events.dooraction.DoorActionCause;
import nl.pim16aap2.bigdoors.events.dooraction.DoorActionType;
import nl.pim16aap2.bigdoors.moveblocks.RevolvingDoorMover;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import nl.pim16aap2.bigdoors.util.vector.Vector3DiConst;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a Revolving Door doorType.
 *
 * @author Pim
 * @see AbstractDoorBase
 */
public class RevolvingDoor extends AbstractDoorBase implements IStationaryDoorArchetype
{
    private static final DoorType DOOR_TYPE = DoorTypeRevolvingDoor.get();

    /**
     * The number of quarter circles (so 90 degree rotations) this door will make before stopping.
     *
     * @return The number of quarter circles this door will rotate.
     */
    @Getter
    private int quarterCircles;

    public RevolvingDoor(final @NotNull DoorData doorData, final int quarterCircles)
    {
        super(doorData);
        this.quarterCircles = quarterCircles;
    }

    public RevolvingDoor(final @NotNull DoorData doorData)
    {
        this(doorData, 1);
    }

    @Override
    public @NotNull DoorType getDoorType()
    {
        return DOOR_TYPE;
    }

    @Override
    public @NotNull RotateDirection getCurrentToggleDir()
    {
        return getOpenDir();
    }

    @Override
    public @NotNull RotateDirection cycleOpenDirection()
    {
        return getOpenDir().equals(RotateDirection.CLOCKWISE) ?
               RotateDirection.COUNTERCLOCKWISE : RotateDirection.CLOCKWISE;
    }

    @Override
    protected void registerBlockMover(final @NotNull DoorActionCause cause, final double time,
                                      final boolean skipAnimation, final @NotNull Vector3DiConst newMin,
                                      final @NotNull Vector3DiConst newMax, final @NotNull IPPlayer responsible,
                                      final @NotNull DoorActionType actionType)
    {
        // TODO: Get rid of this.
        double fixedTime = time < 0.5 ? 5 : time;

        doorOpeningUtility.registerBlockMover(
            new RevolvingDoorMover(this, fixedTime, doorOpeningUtility.getMultiplier(this), getCurrentToggleDir(),
                                   responsible, quarterCircles, cause, actionType));
    }

    @Override
    public boolean equals(@Nullable Object o)
    {
        if (!super.equals(o))
            return false;

        if (getClass() != o.getClass())
            return false;

        final @NotNull RevolvingDoor other = (RevolvingDoor) o;
        return quarterCircles == other.quarterCircles;
    }
}
