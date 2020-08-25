package nl.pim16aap2.bigdoors.doors;

import lombok.Getter;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.doors.doorArchetypes.IHorizontalAxisAlignedDoorArchetype;
import nl.pim16aap2.bigdoors.doors.doorArchetypes.IPerpetualMoverArchetype;
import nl.pim16aap2.bigdoors.doors.doorArchetypes.IStationaryDoorArchetype;
import nl.pim16aap2.bigdoors.doortypes.DoorType;
import nl.pim16aap2.bigdoors.doortypes.DoorTypeClock;
import nl.pim16aap2.bigdoors.events.dooraction.DoorActionCause;
import nl.pim16aap2.bigdoors.events.dooraction.DoorActionType;
import nl.pim16aap2.bigdoors.moveblocks.ClockMover;
import nl.pim16aap2.bigdoors.util.PBlockFace;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import nl.pim16aap2.bigdoors.util.vector.IVector3DiConst;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a Clock doorType.
 *
 * @author Pim
 */
public class Clock extends AbstractDoorBase
    implements IHorizontalAxisAlignedDoorArchetype, IStationaryDoorArchetype, IPerpetualMoverArchetype
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
     *
     * @return True if this clock is situated along the north/south axis.
     */
    @Getter(onMethod = @__({@Override}))
    protected final boolean northSouthAligned;

    /**
     * Describes on which side the hour arm is. If the clock is situated along the North/South axis see {@link
     * #northSouthAligned}, then the hour arm can either be on the {@link PBlockFace#WEST} or the {@link
     * PBlockFace#EAST} side.
     * <p>
     * This is stored as a direction rather than an integer value (for example the X/Z axis value) so that it could also
     * work for {@link Clock}s that have arms that are more than 1 block deep.
     *
     * @return The side of the hour arm relative to the minute arm.
     */
    @Getter
    @NotNull
    protected PBlockFace hourArmSide;

    public Clock(final @NotNull DoorData doorData, final boolean northSouthAligned,
                 final @NotNull PBlockFace hourArmSide)
    {
        super(doorData);
        this.northSouthAligned = northSouthAligned;
        this.hourArmSide = hourArmSide;
    }

    /** {@inheritDoc} */
    @NotNull
    @Override
    public DoorType getDoorType()
    {
        return DOOR_TYPE;
    }

    /** {@inheritDoc} */
    @NotNull
    @Override
    public RotateDirection cycleOpenDirection()
    {
        return getOpenDir();
    }

//    @NotNull
//    @Override
//    public RotateDirection getDefaultOpenDirection()
//    {
//        if (isNorthSouthAligned())
//            return engine.getX() == minimum.getX() ? RotateDirection.SOUTH : RotateDirection.NORTH;
//        else
//            return engine.getZ() == minimum.getZ() ? RotateDirection.EAST : RotateDirection.WEST;
//    }

    @Override
    protected void registerBlockMover(final @NotNull DoorActionCause cause, final double time,
                                      final boolean skipAnimation, final @NotNull IVector3DiConst newMin,
                                      final @NotNull IVector3DiConst newMax, final @NotNull IPPlayer responsible,
                                      final @NotNull DoorActionType actionType)
    {
        doorOpeningUtility
            .registerBlockMover(new ClockMover(this, getCurrentToggleDir(), responsible, cause, actionType));
    }

    @Override
    public boolean equals(@Nullable Object o)
    {
        if (!super.equals(o))
            return false;
        if (getClass() != o.getClass())
            return false;

        final @NotNull Clock other = (Clock) o;
        return hourArmSide.equals(other.hourArmSide) && northSouthAligned == other.northSouthAligned;
    }
}
