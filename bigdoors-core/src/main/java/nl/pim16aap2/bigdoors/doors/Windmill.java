package nl.pim16aap2.bigdoors.doors;

import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.doors.doorArchetypes.IPerpetualMoverArchetype;
import nl.pim16aap2.bigdoors.doors.doorArchetypes.IStationaryDoorArchetype;
import nl.pim16aap2.bigdoors.doortypes.DoorType;
import nl.pim16aap2.bigdoors.doortypes.DoorTypeWindmill;
import nl.pim16aap2.bigdoors.events.dooraction.DoorActionCause;
import nl.pim16aap2.bigdoors.events.dooraction.DoorActionType;
import nl.pim16aap2.bigdoors.moveblocks.WindmillMover;
import nl.pim16aap2.bigdoors.util.PLogger;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import nl.pim16aap2.bigdoors.util.vector.Vector3Di;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a Windmill doorType.
 *
 * @author Pim
 * @see AbstractHorizontalAxisAlignedBase
 */
public class Windmill extends AbstractHorizontalAxisAlignedBase
    implements IStationaryDoorArchetype, IPerpetualMoverArchetype
{
    private static final DoorType DOOR_TYPE = DoorTypeWindmill.get();

    /**
     * Describes if the {@link Clock} is situated along the North/South axis <b>(= TRUE)</b> or along the East/West
     * axis
     * <b>(= FALSE)</b>.
     * <p>
     * To be situated along a specific axis means that the blocks move along that axis. For example, if the door moves
     * along the North/South <i>(= Z)</i> axis, all animated blocks will have a different Z-coordinate depending on the
     * time of day and a X-coordinate depending on the X-coordinate they originally started at.
     */
    protected final boolean onNorthSouthAxis;

    /**
     * The number of quarter circles (so 90 degree rotations) this door will make before stopping.
     */
    private int quarterCircles = 1;

    public Windmill(final @NotNull DoorData doorData, final boolean onNorthSouthAxis, final int quarterCircles)
    {
        super(doorData);
        this.onNorthSouthAxis = onNorthSouthAxis;
        this.quarterCircles = quarterCircles;
    }

    @Deprecated
    protected Windmill(final @NotNull PLogger pLogger, final long doorUID, final @NotNull DoorData doorData,
                       final @NotNull EDoorType type)
    {
        super(pLogger, doorUID, doorData, type);
        onNorthSouthAxis = false;
    }

    @Deprecated
    protected Windmill(final @NotNull PLogger pLogger, final long doorUID, final @NotNull DoorData doorData)
    {
        this(pLogger, doorUID, doorData, EDoorType.WINDMILL);
    }

    /**
     * {@inheritDoc}
     */
    @NotNull
    @Override
    public DoorType getDoorType()
    {
        return DOOR_TYPE;
    }

    /**
     * Checks if this {@link Clock} is on the North/South axis or not. See {@link #onNorthSouthAxis}.
     *
     * @return True if this door is animated along the North/South axis.
     */
    public boolean getOnNorthSouthAxis()
    {
        return onNorthSouthAxis;
    }

    /**
     * Gets the number of quarter circles this door will rotate.
     *
     * @return The number of quarter circles this door will rotate.
     */
    public int getQuarterCircles()
    {
        return quarterCircles;
    }

    /**
     * {@inheritDoc}
     */
    @NotNull
    @Override
    public RotateDirection cycleOpenDirection()
    {
        // This type goes exactly the other way as most usual axis aligned ones.
        if (!onNorthSouthAxis())
            return getOpenDir().equals(RotateDirection.EAST) ? RotateDirection.WEST : RotateDirection.EAST;
        return getOpenDir().equals(RotateDirection.NORTH) ? RotateDirection.SOUTH : RotateDirection.NORTH;
    }

    /**
     * {@inheritDoc}
     */
    @NotNull
    @Override
    public RotateDirection getDefaultOpenDirection()
    {
        if (onNorthSouthAxis())
            return RotateDirection.NORTH;
        else
            return RotateDirection.EAST;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void registerBlockMover(final @NotNull DoorActionCause cause, final double time,
                                      final boolean skipAnimation, final @NotNull Vector3Di newMin,
                                      final @NotNull Vector3Di newMax, final @NotNull IPPlayer initiator,
                                      final @NotNull DoorActionType actionType)
    {
        // TODO: Get rid of this.
        double fixedTime = time < 0.5 ? 5 : time;

        doorOpeningUtility.registerBlockMover(new WindmillMover(this, fixedTime, doorOpeningUtility.getMultiplier(this),
                                                                getCurrentToggleDir(), initiator, cause, actionType));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(final @Nullable Object o)
    {
        if (!super.equals(o))
            return false;
        if (getClass() != o.getClass())
            return false;

        final @NotNull Windmill other = (Windmill) o;
        return onNorthSouthAxis == other.onNorthSouthAxis;
    }
}
