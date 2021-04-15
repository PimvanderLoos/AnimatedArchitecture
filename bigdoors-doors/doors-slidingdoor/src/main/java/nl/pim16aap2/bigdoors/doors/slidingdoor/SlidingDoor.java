package nl.pim16aap2.bigdoors.doors.slidingdoor;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.Accessors;
import nl.pim16aap2.bigdoors.annotations.PersistentVariable;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.doors.AbstractDoorBase;
import nl.pim16aap2.bigdoors.doors.DoorOpeningUtility;
import nl.pim16aap2.bigdoors.doors.doorArchetypes.IBlocksToMoveArchetype;
import nl.pim16aap2.bigdoors.doors.doorArchetypes.IStationaryDoorArchetype;
import nl.pim16aap2.bigdoors.doors.doorArchetypes.ITimerToggleableArchetype;
import nl.pim16aap2.bigdoors.doortypes.DoorType;
import nl.pim16aap2.bigdoors.events.dooraction.DoorActionCause;
import nl.pim16aap2.bigdoors.events.dooraction.DoorActionType;
import nl.pim16aap2.bigdoors.moveblocks.BlockMover;
import nl.pim16aap2.bigdoors.util.Cuboid;
import nl.pim16aap2.bigdoors.util.CuboidConst;
import nl.pim16aap2.bigdoors.util.PBlockFace;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import nl.pim16aap2.bigdoors.util.Util;
import nl.pim16aap2.bigdoors.util.vector.Vector2Di;
import nl.pim16aap2.bigdoors.util.vector.Vector3DiConst;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * Represents a Sliding Door doorType.
 *
 * @author Pim
 */
public class SlidingDoor extends AbstractDoorBase
    implements IStationaryDoorArchetype, IBlocksToMoveArchetype, ITimerToggleableArchetype
{
    @NonNull
    private static final DoorType DOOR_TYPE = DoorTypeSlidingDoor.get();

    @Getter(onMethod = @__({@Override}))
    @Setter(onMethod = @__({@Override}))
    @Accessors(chain = true)
    @PersistentVariable
    protected int blocksToMove;

    @Getter(onMethod = @__({@Override}))
    @Setter(onMethod = @__({@Override}))
    @Accessors(chain = true)
    @PersistentVariable
    protected int autoCloseTime;

    @Getter(onMethod = @__({@Override}))
    @Setter(onMethod = @__({@Override}))
    @Accessors(chain = true)
    @PersistentVariable
    protected int autoOpenTime;

    public SlidingDoor(final @NonNull DoorData doorData, final int blocksToMove, final int autoCloseTime,
                       final int autoOpenTime)
    {
        super(doorData);
        this.blocksToMove = blocksToMove;
        this.autoCloseTime = autoCloseTime;
        this.autoOpenTime = autoOpenTime;
    }

    public SlidingDoor(final @NonNull DoorData doorData, final int blocksToMove)
    {
        this(doorData, blocksToMove, -1, -1);
    }

    private SlidingDoor(final @NonNull DoorData doorData)
    {
        this(doorData, -1); // Add tmp/default values
    }

    @Override
    public @NonNull DoorType getDoorType()
    {
        return DOOR_TYPE;
    }

    @Override
    public @NonNull Vector2Di[] calculateChunkRange()
    {
        final @NonNull Vector3DiConst dimensions = getDimensions();

        int distanceX = 0;
        int distanceZ = 0;
        if (getOpenDir().equals(RotateDirection.NORTH) || getOpenDir().equals(RotateDirection.SOUTH))
            distanceZ = (getBlocksToMove() > 0 ? Math.max(dimensions.getZ(), getBlocksToMove()) :
                         Math.min(-dimensions.getZ(), getBlocksToMove())) / 16 + 1;
        else
            distanceX = (getBlocksToMove() > 0 ? Math.max(dimensions.getX(), getBlocksToMove()) :
                         Math.min(-dimensions.getX(), getBlocksToMove())) / 16 + 1;

        return new Vector2Di[]{
            new Vector2Di(getEngineChunk().getX() - distanceX, getEngineChunk().getY() - distanceZ),
            new Vector2Di(getEngineChunk().getX() + distanceX, getEngineChunk().getY() + distanceZ)};
    }

    @Override
    public @NonNull RotateDirection cycleOpenDirection()
    {
        return getOpenDir().equals(RotateDirection.NORTH) ? RotateDirection.EAST :
               getOpenDir().equals(RotateDirection.EAST) ? RotateDirection.SOUTH :
               getOpenDir().equals(RotateDirection.SOUTH) ? RotateDirection.WEST : RotateDirection.NORTH;
    }

    @Override
    public synchronized @NonNull RotateDirection getCurrentToggleDir()
    {
        return isOpen() ? RotateDirection.getOpposite(getOpenDir()) : getOpenDir();
    }

    @Override
    public synchronized @NonNull Optional<Cuboid> getPotentialNewCoordinates()
    {
        final @NonNull Vector3DiConst vec = PBlockFace.getDirection(Util.getPBlockFace(getCurrentToggleDir()));
        return Optional.of(getCuboid().clone().move(0, getBlocksToMove() * vec.getY(), 0));
    }

    @Override
    protected @NonNull BlockMover constructBlockMover(final @NonNull DoorActionCause cause, final double time,
                                                      final boolean skipAnimation, final @NonNull CuboidConst newCuboid,
                                                      final @NonNull IPPlayer responsible,
                                                      final @NonNull DoorActionType actionType)
        throws Exception
    {
        final @NonNull RotateDirection currentToggleDir = getCurrentToggleDir();
        return new SlidingMover(this, time, skipAnimation, getBlocksToMove(), currentToggleDir,
                                DoorOpeningUtility.getMultiplier(this), responsible, newCuboid, cause, actionType);
    }

    @Override
    public boolean equals(final @Nullable Object o)
    {
        if (!super.equals(o))
            return false;
        if (getClass() != o.getClass())
            return false;

        final @NonNull SlidingDoor other = (SlidingDoor) o;
        return blocksToMove == other.blocksToMove;
    }
}
