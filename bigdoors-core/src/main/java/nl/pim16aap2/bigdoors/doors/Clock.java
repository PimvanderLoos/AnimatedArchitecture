package nl.pim16aap2.bigdoors.doors;

import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.doors.doorArchetypes.IPerpetualMoverArchetype;
import nl.pim16aap2.bigdoors.doors.doorArchetypes.IStationaryDoorArchetype;
import nl.pim16aap2.bigdoors.doortypes.DoorType;
import nl.pim16aap2.bigdoors.doortypes.DoorTypeClock;
import nl.pim16aap2.bigdoors.events.dooraction.DoorActionCause;
import nl.pim16aap2.bigdoors.moveblocks.ClockMover;
import nl.pim16aap2.bigdoors.util.PBlockFace;
import nl.pim16aap2.bigdoors.util.PLogger;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import nl.pim16aap2.bigdoors.util.vector.Vector3Di;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * Represents a Clock doorType.
 *
 * @author Pim
 * @see AbstractHorizontalAxisAlignedBase
 */
public class Clock extends AbstractHorizontalAxisAlignedBase
    implements IStationaryDoorArchetype, IPerpetualMoverArchetype
{
    private static final DoorType DOOR_TYPE = DoorTypeClock.get();

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
     * Describes on which side the hour arm is. If the clock is situated along the North/South axis see {@link
     * #onNorthSouthAxis}, then the hour arm can either be on the {@link PBlockFace#WEST} or the {@link PBlockFace#EAST}
     * side.
     * <p>
     * This is stored as a direction rather than an integer value (for example the X/Z axis value) so that it could also
     * work for {@link Clock}s that have arms that are more than 1 block deep.
     */
    protected PBlockFace hourArmSide;

    @NotNull
    public static Optional<AbstractDoorBase> constructor(final @NotNull DoorData doorData,
                                                         final @NotNull Object... args)
        throws Exception
    {
        @Nullable final PBlockFace hourArmSide = PBlockFace.valueOf((int) args[1]);
        if (hourArmSide == null)
            return Optional.empty();

        final boolean onNorthSouthAxis = ((int) args[0]) == 1;
        return Optional.of(new Clock(doorData, onNorthSouthAxis, hourArmSide));
    }

    public static Object[] dataSupplier(final @NotNull AbstractDoorBase door)
        throws IllegalArgumentException
    {
        if (!(door instanceof Clock))
            throw new IllegalArgumentException(
                "Trying to get the type-specific data for a Clock from type: " + door.getDoorType().toString());

        final @NotNull Clock clock = (Clock) door;
        return new Object[]{clock.getOnNorthSouthAxis() ? 1 : 0, PBlockFace.getValue(clock.getHourArmSide())};
    }

    public Clock(final @NotNull DoorData doorData, final boolean onNorthSouthAxis,
                 final @NotNull PBlockFace hourArmSide)
    {
        super(doorData);
        this.onNorthSouthAxis = onNorthSouthAxis;
        this.hourArmSide = hourArmSide;
    }

    @Deprecated
    protected Clock(final @NotNull PLogger pLogger, final long doorUID, final @NotNull DoorData doorData,
                    final @NotNull EDoorType type)
    {
        super(pLogger, doorUID, doorData, type);
        onNorthSouthAxis = false;
    }

    @Deprecated
    protected Clock(final @NotNull PLogger pLogger, final long doorUID, final @NotNull DoorData doorData)
    {
        this(pLogger, doorUID, doorData, EDoorType.CLOCK);
    }

    /**
     * Gets the side of the {@link Clock} that the hour arm is on. See {@link #hourArmSide}.
     *
     * @return The side of the {@link Clock} that the hour arm is on
     */
    @NotNull
    public PBlockFace getHourArmSide()
    {
        return hourArmSide;
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
     * {@inheritDoc}
     */
    @NotNull
    @Override
    public DoorType getDoorType()
    {
        return DOOR_TYPE;
    }

    /**
     * {@inheritDoc}
     */
    @NotNull
    @Override
    public RotateDirection cycleOpenDirection()
    {
        return getOpenDir();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setDefaultOpenDirection()
    {
        if (onNorthSouthAxis())
            setOpenDir(engine.getX() == min.getX() ? RotateDirection.SOUTH : RotateDirection.NORTH);
        else
            setOpenDir(engine.getZ() == min.getZ() ? RotateDirection.EAST : RotateDirection.WEST);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void registerBlockMover(final @NotNull DoorActionCause cause, final double time,
                                      final boolean skipAnimation, final @NotNull Vector3Di newMin,
                                      final @NotNull Vector3Di newMax, final @Nullable IPPlayer initiator)
    {
        doorOpeningUtility.registerBlockMover(new ClockMover(this, getCurrentToggleDir(), initiator));
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

        final @NotNull Clock other = (Clock) o;
        return hourArmSide.equals(other.hourArmSide) && onNorthSouthAxis == other.onNorthSouthAxis;
    }
}
