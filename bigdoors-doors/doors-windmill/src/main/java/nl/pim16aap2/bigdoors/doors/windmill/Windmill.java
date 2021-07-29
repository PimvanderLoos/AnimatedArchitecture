package nl.pim16aap2.bigdoors.doors.windmill;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import nl.pim16aap2.bigdoors.annotations.PersistentVariable;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.doors.AbstractDoor;
import nl.pim16aap2.bigdoors.doors.DoorBase;
import nl.pim16aap2.bigdoors.doors.DoorOpeningUtility;
import nl.pim16aap2.bigdoors.doors.doorArchetypes.IHorizontalAxisAligned;
import nl.pim16aap2.bigdoors.doors.doorArchetypes.IPerpetualMover;
import nl.pim16aap2.bigdoors.doortypes.DoorType;
import nl.pim16aap2.bigdoors.events.dooraction.DoorActionCause;
import nl.pim16aap2.bigdoors.events.dooraction.DoorActionType;
import nl.pim16aap2.bigdoors.moveblocks.BlockMover;
import nl.pim16aap2.bigdoors.util.Cuboid;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * Represents a Windmill doorType.
 *
 * @author Pim
 */
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class Windmill extends AbstractDoor implements IHorizontalAxisAligned, IPerpetualMover
{
    @EqualsAndHashCode.Exclude
    private static final @NotNull DoorType DOOR_TYPE = DoorTypeWindmill.get();

    /**
     * The number of quarter circles (so 90 degree rotations) this door will make before stopping.
     *
     * @return The number of quarter circles this door will rotate.
     */
    @Getter
    @PersistentVariable
    private int quarterCircles = 1;

    public Windmill(final @NotNull DoorBase doorBase, final int quarterCircles)
    {
        super(doorBase);
        this.quarterCircles = quarterCircles;
    }

    public Windmill(final @NotNull DoorBase doorBase)
    {
        this(doorBase, 1);
    }

    @Override
    public @NotNull DoorType getDoorType()
    {
        return DOOR_TYPE;
    }

    @Override
    public boolean canSkipAnimation()
    {
        return false;
    }

    @Override
    public @NotNull Optional<Cuboid> getPotentialNewCoordinates()
    {
        return Optional.of(getCuboid());
    }

    @Override
    public @NotNull RotateDirection getCurrentToggleDir()
    {
        return getOpenDir();
    }

    @Override
    public boolean isNorthSouthAligned()
    {
        return getOpenDir() == RotateDirection.EAST || getOpenDir() == RotateDirection.WEST;
    }

    @Override
    public @NotNull RotateDirection cycleOpenDirection()
    {
        return getOpenDir().equals(RotateDirection.NORTH) ? RotateDirection.EAST :
               getOpenDir().equals(RotateDirection.EAST) ? RotateDirection.SOUTH :
               getOpenDir().equals(RotateDirection.SOUTH) ? RotateDirection.WEST : RotateDirection.NORTH;
    }

    @Override
    protected @NotNull BlockMover constructBlockMover(final @NotNull DoorActionCause cause, final double time,
                                                      final boolean skipAnimation, final @NotNull Cuboid newCuboid,
                                                      final @NotNull IPPlayer responsible,
                                                      final @NotNull DoorActionType actionType)
        throws Exception
    {
        // TODO: Get rid of this.
        double fixedTime = time < 0.5 ? 5 : time;

        return new WindmillMover<>(this, fixedTime, DoorOpeningUtility.getMultiplier(this), getCurrentToggleDir(),
                                   responsible, cause, actionType);
    }
}
