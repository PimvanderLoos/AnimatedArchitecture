package nl.pim16aap2.bigdoors.doors.portcullis;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.Accessors;
import nl.pim16aap2.bigdoors.annotations.PersistentVariable;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.doors.AbstractDoorBase;
import nl.pim16aap2.bigdoors.doors.DoorOpeningUtility;
import nl.pim16aap2.bigdoors.doors.doorArchetypes.IBlocksToMoveArchetype;
import nl.pim16aap2.bigdoors.doors.doorArchetypes.IMovingDoorArchetype;
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
 * Represents a Portcullis doorType.
 *
 * @author Pim
 * @see AbstractDoorBase
 */
public class Portcullis extends AbstractDoorBase
    implements IMovingDoorArchetype, IBlocksToMoveArchetype, ITimerToggleableArchetype
{
    @NonNull
    private static final DoorType DOOR_TYPE = DoorTypePortcullis.get();

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

    public Portcullis(final @NonNull DoorData doorData, final int blocksToMove, final int autoCloseTime,
                      final int autoOpenTime)
    {
        super(doorData);
        this.blocksToMove = blocksToMove;
        this.autoCloseTime = autoCloseTime;
        this.autoOpenTime = autoOpenTime;
    }

    public Portcullis(final @NonNull DoorData doorData, final int blocksToMove)
    {
        this(doorData, blocksToMove, -1, -1);
    }

    private Portcullis(final @NonNull DoorData doorData)
    {
        this(doorData, -1); // Add tmp/default values
    }

    @Override
    public @NonNull DoorType getDoorType()
    {
        return DOOR_TYPE;
    }

    @Override
    public @NonNull RotateDirection cycleOpenDirection()
    {
        return getOpenDir().equals(RotateDirection.UP) ? RotateDirection.DOWN : RotateDirection.UP;
    }

    @Override
    public @NonNull Vector2Di[] calculateChunkRange()
    {
        return calculateCurrentChunkRange();
    }

    @Override
    public synchronized @NonNull RotateDirection getCurrentToggleDir()
    {
        return isOpen() ? RotateDirection.DOWN : RotateDirection.UP;
    }

    @Override
    public synchronized @NonNull Optional<Cuboid> getPotentialNewCoordinates()
    {
        final @NonNull Vector3DiConst vec = PBlockFace.getDirection(Util.getPBlockFace(getCurrentToggleDir()));
        return Optional.of(getCuboid().clone().move(getBlocksToMove() * vec.getX(), 0, getBlocksToMove() * vec.getZ()));
    }

    @Override
    protected @NonNull BlockMover constructBlockMover(final @NonNull DoorActionCause cause, final double time,
                                                      final boolean skipAnimation, final @NonNull CuboidConst newCuboid,
                                                      final @NonNull IPPlayer responsible,
                                                      final @NonNull DoorActionType actionType)
        throws Exception
    {
        final int blocksToMove = getOpenDir().equals(RotateDirection.UP) ? getBlocksToMove() : -getBlocksToMove();
        return new VerticalMover(this, time, skipAnimation, blocksToMove, DoorOpeningUtility.getMultiplier(this),
                                 responsible, newCuboid, cause, actionType);
    }

    @Override
    public boolean equals(@Nullable Object o)
    {
        if (!super.equals(o))
            return false;

        if (getClass() != o.getClass())
            return false;

        final @NonNull Portcullis other = (Portcullis) o;
        return blocksToMove == other.blocksToMove &&
            autoOpenTime == other.autoOpenTime &&
            autoCloseTime == other.autoCloseTime;
    }
}
