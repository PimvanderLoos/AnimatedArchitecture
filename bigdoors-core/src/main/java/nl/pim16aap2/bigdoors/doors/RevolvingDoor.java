package nl.pim16aap2.bigdoors.doors;

import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.doors.doorArchetypes.IStationaryDoorArchetype;
import nl.pim16aap2.bigdoors.doortypes.DoorType;
import nl.pim16aap2.bigdoors.doortypes.DoorTypeRevolvingDoor;
import nl.pim16aap2.bigdoors.events.dooraction.DoorActionCause;
import nl.pim16aap2.bigdoors.moveblocks.RevolvingDoorMover;
import nl.pim16aap2.bigdoors.util.PLogger;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import nl.pim16aap2.bigdoors.util.vector.Vector3Di;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

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
     */
    private int quarterCircles = 1;

    @NotNull
    public static Optional<AbstractDoorBase> constructor(final @NotNull DoorData doorData,
                                                         final @NotNull Object... args)
        throws Exception
    {
        final int qCircles = (int) args[0];
        return Optional.of(new RevolvingDoor(doorData, qCircles));
    }

    public static Object[] dataSupplier(final @NotNull AbstractDoorBase door)
        throws IllegalArgumentException
    {
        if (!(door instanceof RevolvingDoor))
            throw new IllegalArgumentException(
                "Trying to get the type-specific data for an RevolvingDoor from type: " +
                    door.getDoorType().toString());

        final @NotNull RevolvingDoor revolvingDoor = (RevolvingDoor) door;
        return new Object[]{revolvingDoor.getQuarterCircles()};
    }

    public RevolvingDoor(final @NotNull DoorData doorData, final int quarterCircles)
    {
        super(doorData);
        this.quarterCircles = quarterCircles;
    }

    @Deprecated
    protected RevolvingDoor(final @NotNull PLogger pLogger, final long doorUID, final @NotNull DoorData doorData,
                            final @NotNull EDoorType type)
    {
        super(pLogger, doorUID, doorData, type);
    }

    @Deprecated
    protected RevolvingDoor(final @NotNull PLogger pLogger, final long doorUID, final @NotNull DoorData doorData)
    {
        this(pLogger, doorUID, doorData, EDoorType.REVOLVINGDOOR);
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
    @Override
    public void setDefaultOpenDirection()
    {
        setOpenDir(RotateDirection.CLOCKWISE);
    }

    /**
     * {@inheritDoc}
     */
    @NotNull
    @Override
    public RotateDirection getCurrentToggleDir()
    {
        return isOpen() ? RotateDirection.getOpposite(getOpenDir()) : getOpenDir();
    }

    /**
     * {@inheritDoc}
     */
    @NotNull
    @Override
    public RotateDirection cycleOpenDirection()
    {
        return getOpenDir().equals(RotateDirection.CLOCKWISE) ?
               RotateDirection.COUNTERCLOCKWISE : RotateDirection.CLOCKWISE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void registerBlockMover(final @NotNull DoorActionCause cause, final double time,
                                      final boolean skipAnimation, final @NotNull Vector3Di newMin,
                                      final @NotNull Vector3Di newMax, final @Nullable IPPlayer initiator)
    {
        // TODO: Get rid of this.
        double fixedTime = time < 0.5 ? 5 : time;

        doorOpeningUtility.registerBlockMover(
            new RevolvingDoorMover(this, fixedTime, doorOpeningUtility.getMultiplier(this), getCurrentToggleDir(),
                                   initiator, quarterCircles));
    }

    /**
     * {@inheritDoc}
     */
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
