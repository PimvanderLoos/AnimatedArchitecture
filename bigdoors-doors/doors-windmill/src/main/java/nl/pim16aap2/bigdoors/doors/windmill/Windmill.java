package nl.pim16aap2.bigdoors.doors.windmill;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Locked;
import nl.pim16aap2.bigdoors.annotations.PersistentVariable;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.doors.AbstractDoor;
import nl.pim16aap2.bigdoors.doors.DoorBase;
import nl.pim16aap2.bigdoors.doors.DoorSnapshot;
import nl.pim16aap2.bigdoors.doors.doorarchetypes.IHorizontalAxisAligned;
import nl.pim16aap2.bigdoors.doors.doorarchetypes.IPerpetualMover;
import nl.pim16aap2.bigdoors.doors.drawbridge.Drawbridge;
import nl.pim16aap2.bigdoors.doortypes.DoorType;
import nl.pim16aap2.bigdoors.events.dooraction.DoorActionCause;
import nl.pim16aap2.bigdoors.events.dooraction.DoorActionType;
import nl.pim16aap2.bigdoors.moveblocks.BlockMover;
import nl.pim16aap2.bigdoors.util.Cuboid;
import nl.pim16aap2.bigdoors.util.RotateDirection;

import javax.annotation.concurrent.GuardedBy;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Represents a Windmill doorType.
 *
 * @author Pim
 */
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class Windmill extends AbstractDoor implements IHorizontalAxisAligned, IPerpetualMover
{
    private static final DoorType DOOR_TYPE = DoorTypeWindmill.get();

    @EqualsAndHashCode.Exclude
    private final ReentrantReadWriteLock lock;

    @Getter
    private final double longestAnimationCycleDistance;

    /**
     * The number of quarter circles (so 90 degree rotations) this door will make before stopping.
     *
     * @return The number of quarter circles this door will rotate.
     */
    @PersistentVariable
    @GuardedBy("lock")
    @Getter(onMethod_ = @Locked.Read)
    @Setter(onMethod_ = @Locked.Write)
    private int quarterCircles;

    public Windmill(DoorBase doorBase, int quarterCircles)
    {
        super(doorBase);
        this.lock = getLock();
        this.quarterCircles = quarterCircles;

        longestAnimationCycleDistance =
            Drawbridge.calculateLongestAnimationCycleDistance(isNorthSouthAligned(), getCuboid(), getRotationPoint());
    }

    public Windmill(DoorBase doorBase)
    {
        this(doorBase, 1);
    }

    @Override
    public DoorType getDoorType()
    {
        return DOOR_TYPE;
    }

    @Override
    public boolean canSkipAnimation()
    {
        return false;
    }

    @Override
    public Optional<Cuboid> getPotentialNewCoordinates()
    {
        return Optional.of(getCuboid());
    }

    @Override
    public RotateDirection getCurrentToggleDir()
    {
        return getOpenDir();
    }

    @Override
    public boolean isNorthSouthAligned()
    {
        final RotateDirection openDir = getOpenDir();
        return openDir == RotateDirection.EAST || openDir == RotateDirection.WEST;
    }

    @Override
    public RotateDirection cycleOpenDirection()
    {
        final RotateDirection openDir = getOpenDir();
        return openDir.equals(RotateDirection.NORTH) ? RotateDirection.EAST :
               openDir.equals(RotateDirection.EAST) ? RotateDirection.SOUTH :
               openDir.equals(RotateDirection.SOUTH) ? RotateDirection.WEST : RotateDirection.NORTH;
    }

    @Override
    @Locked.Read
    protected BlockMover constructBlockMover(
        BlockMover.Context context, DoorSnapshot doorSnapshot, DoorActionCause cause, double time,
        boolean skipAnimation, Cuboid newCuboid, IPPlayer responsible, DoorActionType actionType)
        throws Exception
    {
        return new WindmillMover<>(
            context, this, doorSnapshot, time, config.getAnimationSpeedMultiplier(getDoorType()), getCurrentToggleDir(),
            responsible, cause, actionType);
    }
}
