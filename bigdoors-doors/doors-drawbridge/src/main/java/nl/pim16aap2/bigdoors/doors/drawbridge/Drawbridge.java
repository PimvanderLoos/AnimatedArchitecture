package nl.pim16aap2.bigdoors.doors.drawbridge;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.flogger.Flogger;
import nl.pim16aap2.bigdoors.annotations.PersistentVariable;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.doors.AbstractDoor;
import nl.pim16aap2.bigdoors.doors.DoorBase;
import nl.pim16aap2.bigdoors.doors.doorarchetypes.IHorizontalAxisAligned;
import nl.pim16aap2.bigdoors.doors.doorarchetypes.ITimerToggleable;
import nl.pim16aap2.bigdoors.doortypes.DoorType;
import nl.pim16aap2.bigdoors.events.dooraction.DoorActionCause;
import nl.pim16aap2.bigdoors.events.dooraction.DoorActionType;
import nl.pim16aap2.bigdoors.moveblocks.BlockMover;
import nl.pim16aap2.bigdoors.util.Cuboid;
import nl.pim16aap2.bigdoors.util.RotateDirection;

import java.util.Optional;
import java.util.logging.Level;

/**
 * Represents a DrawBrige doorType.
 *
 * @author Pim
 */
@EqualsAndHashCode(callSuper = true)
@Flogger
public class Drawbridge extends AbstractDoor implements IHorizontalAxisAligned, ITimerToggleable
{
    @EqualsAndHashCode.Exclude
    private static final DoorType DOOR_TYPE = DoorTypeDrawbridge.get();

    @Getter
    @Setter
    @PersistentVariable
    protected int autoCloseTime;

    @Getter
    @Setter
    @PersistentVariable
    protected int autoOpenTime;

    /**
     * Describes if this drawbridge's vertical position points (when taking the rotation point Y value as center) up
     * <b>(= TRUE)</b> or down <b>(= FALSE)</b>
     *
     * @return True if this {@link Drawbridge}'s vertical stance points up.
     */
    @Getter
    @PersistentVariable
    protected boolean modeUp;

    public Drawbridge(DoorBase doorBase, int autoCloseTime, int autoOpenTime, boolean modeUp)
    {
        super(doorBase);
        this.autoOpenTime = autoOpenTime;
        this.autoCloseTime = autoCloseTime;
        this.modeUp = modeUp;
    }

    public Drawbridge(DoorBase doorBase, boolean modeUp)
    {
        this(doorBase, -1, -1, modeUp);
    }

    @SuppressWarnings("unused")
    private Drawbridge(DoorBase doorBase)
    {
        this(doorBase, false); // Add tmp/default values
    }

    @Override
    public DoorType getDoorType()
    {
        return DOOR_TYPE;
    }

    @Override
    public synchronized RotateDirection getCurrentToggleDir()
    {
        return isOpen() ? RotateDirection.getOpposite(getOpenDir()) : getOpenDir();
    }

    @Override
    public synchronized Optional<Cuboid> getPotentialNewCoordinates()
    {
        final RotateDirection rotateDirection = getCurrentToggleDir();
        final double angle;
        if (rotateDirection == RotateDirection.NORTH || rotateDirection == RotateDirection.WEST)
            angle = -Math.PI / 2;
        else if (rotateDirection == RotateDirection.SOUTH || rotateDirection == RotateDirection.EAST)
            angle = Math.PI / 2;
        else
        {

            log.at(Level.SEVERE).log("Invalid open direction '%s' for door: %d", rotateDirection.name(), getDoorUID());
            return Optional.empty();
        }

        final Cuboid cuboid = getCuboid();
        if (rotateDirection == RotateDirection.NORTH || rotateDirection == RotateDirection.SOUTH)
            return Optional.of(cuboid.updatePositions(vec -> vec.rotateAroundXAxis(getRotationPoint(), angle)));
        else
            return Optional.of(cuboid.updatePositions(vec -> vec.rotateAroundZAxis(getRotationPoint(), angle)));
    }

    @Override
    protected BlockMover constructBlockMover(BlockMover.Context context, DoorActionCause cause, double time,
                                             boolean skipAnimation, Cuboid newCuboid, IPPlayer responsible,
                                             DoorActionType actionType)
        throws Exception
    {
        return new BridgeMover<>(context, time, this, getCurrentToggleDir(), skipAnimation,
                                 doorOpeningHelper.getAnimationTime(this), responsible, newCuboid, cause, actionType);
    }

    @Override
    public boolean isNorthSouthAligned()
    {
        final RotateDirection openDir = getOpenDir();
        return openDir == RotateDirection.NORTH || openDir == RotateDirection.SOUTH;
    }
}
