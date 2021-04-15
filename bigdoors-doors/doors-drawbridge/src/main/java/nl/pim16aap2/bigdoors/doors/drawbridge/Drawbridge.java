package nl.pim16aap2.bigdoors.doors.drawbridge;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.Accessors;
import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.annotations.PersistentVariable;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.doors.AbstractDoorBase;
import nl.pim16aap2.bigdoors.doors.DoorOpeningUtility;
import nl.pim16aap2.bigdoors.doors.doorArchetypes.IHorizontalAxisAlignedDoorArchetype;
import nl.pim16aap2.bigdoors.doors.doorArchetypes.IMovingDoorArchetype;
import nl.pim16aap2.bigdoors.doors.doorArchetypes.ITimerToggleableArchetype;
import nl.pim16aap2.bigdoors.doortypes.DoorType;
import nl.pim16aap2.bigdoors.events.dooraction.DoorActionCause;
import nl.pim16aap2.bigdoors.events.dooraction.DoorActionType;
import nl.pim16aap2.bigdoors.moveblocks.BlockMover;
import nl.pim16aap2.bigdoors.util.Cuboid;
import nl.pim16aap2.bigdoors.util.CuboidConst;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import nl.pim16aap2.bigdoors.util.vector.Vector2Di;
import nl.pim16aap2.bigdoors.util.vector.Vector3DiConst;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * Represents a DrawBrige doorType.
 *
 * @author Pim
 */
public class Drawbridge extends AbstractDoorBase
    implements IHorizontalAxisAlignedDoorArchetype, IMovingDoorArchetype, ITimerToggleableArchetype
{
    @NonNull
    private static final DoorType DOOR_TYPE = DoorTypeDrawbridge.get();

    @Getter(onMethod = @__({@Override}))
//    @Setter(onMethod = @__({@Override, @Synchronized("readLock")}))
    @Setter(onMethod = @__({@Override}))
    @Accessors(chain = true)
    @PersistentVariable
    protected int autoCloseTime;

    @Getter(onMethod = @__({@Override}))
    @Setter(onMethod = @__({@Override}))
    @Accessors(chain = true)
    @PersistentVariable
    protected int autoOpenTime;

    /**
     * Describes if this drawbridge's vertical position points (when taking the engine Y value as center) up <b>(=
     * TRUE)</b> or down <b>(= FALSE)</b>
     *
     * @return True if this {@link Drawbridge}'s vertical stance points up.
     */
    @Getter
    @PersistentVariable
    protected boolean modeUp;

    public Drawbridge(final @NonNull DoorData doorData, final int autoCloseTime, final int autoOpenTime,
                      final boolean modeUp)
    {
        super(doorData);
        this.autoOpenTime = autoOpenTime;
        this.autoCloseTime = autoCloseTime;
        this.modeUp = modeUp;
    }

    public Drawbridge(final @NonNull DoorData doorData, final boolean modeUp)
    {
        this(doorData, -1, -1, modeUp);
    }

    private Drawbridge(final @NonNull DoorData doorData)
    {
        this(doorData, false); // Add tmp/default values
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

        final int xLen = dimensions.getX();
        final int yLen = dimensions.getY();
        final int zLen = dimensions.getZ();

        final int radius;
        if (dimensions.getY() != 1)
            radius = yLen / 16 + 1;
        else
            radius = Math.max(xLen, zLen) / 16 + 1;

        return new Vector2Di[]{
            new Vector2Di(getEngineChunk().getX() - radius, getEngineChunk().getY() - radius),
            new Vector2Di(getEngineChunk().getX() + radius, getEngineChunk().getY() + radius)};
    }

    @Override
    public synchronized @NonNull RotateDirection getCurrentToggleDir()
    {
        return isOpen() ? RotateDirection.getOpposite(getOpenDir()) : getOpenDir();
    }

    @Override
    public synchronized @NonNull Optional<Cuboid> getPotentialNewCoordinates()
    {
        final @NonNull RotateDirection rotateDirection = getCurrentToggleDir();
        final double angle;
        if (rotateDirection == RotateDirection.NORTH || rotateDirection == RotateDirection.WEST)
            angle = -Math.PI / 2;
        else if (rotateDirection == RotateDirection.SOUTH || rotateDirection == RotateDirection.EAST)
            angle = Math.PI / 2;
        else
        {
            BigDoors.get().getPLogger()
                    .severe("Invalid open direction \"" + rotateDirection.name() + "\" for door: " + getDoorUID());
            return Optional.empty();
        }

        final @NonNull Cuboid cuboid = getCuboid().clone();
        if (rotateDirection == RotateDirection.NORTH || rotateDirection == RotateDirection.SOUTH)
            return Optional.of(cuboid.updatePositions(vec -> vec.rotateAroundXAxis(getEngine(), angle)));
        else
            return Optional.of(cuboid.updatePositions(vec -> vec.rotateAroundZAxis(getEngine(), angle)));
    }

    @Override
    protected @NonNull BlockMover constructBlockMover(final @NonNull DoorActionCause cause, final double time,
                                                      final boolean skipAnimation, final @NonNull CuboidConst newCuboid,
                                                      final @NonNull IPPlayer responsible,
                                                      final @NonNull DoorActionType actionType)
        throws Exception
    {
        return new BridgeMover<>(time, this, getCurrentToggleDir(), skipAnimation,
                                 DoorOpeningUtility.getMultiplier(this), responsible, newCuboid, cause, actionType);
    }

    @Override
    public boolean equals(@Nullable Object o)
    {
        if (!super.equals(o))
            return false;

        if (getClass() != o.getClass())
            return false;

        final @NonNull Drawbridge other = (Drawbridge) o;
        return autoCloseTime == other.autoCloseTime &&
            autoOpenTime == other.autoOpenTime &&
            modeUp == other.modeUp;
    }

    @Override
    public boolean isNorthSouthAligned()
    {
        final @NonNull RotateDirection openDir = getOpenDir();
        return openDir == RotateDirection.NORTH || openDir == RotateDirection.SOUTH;
    }
}
