package nl.pim16aap2.bigdoors.doors;

import lombok.Getter;
import lombok.Setter;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.doors.doorArchetypes.IHorizontalAxisAlignedDoorArchetype;
import nl.pim16aap2.bigdoors.doors.doorArchetypes.IMovingDoorArchetype;
import nl.pim16aap2.bigdoors.doors.doorArchetypes.ITimerToggleableArchetype;
import nl.pim16aap2.bigdoors.doortypes.DoorType;
import nl.pim16aap2.bigdoors.doortypes.DoorTypeGarageDoor;
import nl.pim16aap2.bigdoors.events.dooraction.DoorActionCause;
import nl.pim16aap2.bigdoors.events.dooraction.DoorActionType;
import nl.pim16aap2.bigdoors.moveblocks.GarageDoorMover;
import nl.pim16aap2.bigdoors.util.PBlockFace;
import nl.pim16aap2.bigdoors.util.PLogger;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import nl.pim16aap2.bigdoors.util.Util;
import nl.pim16aap2.bigdoors.util.vector.IVector3DiConst;
import nl.pim16aap2.bigdoors.util.vector.Vector2Di;
import nl.pim16aap2.bigdoors.util.vector.Vector3Di;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a Garage Door doorType.
 *
 * @author Pim
 */
public class GarageDoor extends AbstractDoorBase
    implements IHorizontalAxisAlignedDoorArchetype, IMovingDoorArchetype, ITimerToggleableArchetype
{
    private static final DoorType DOOR_TYPE = DoorTypeGarageDoor.get();

    /**
     * Describes if the {@link Clock} is situated along the North/South axis <b>(= TRUE)</b> or along the East/West
     * axis
     * <b>(= FALSE)</b>.
     * <p>
     * To be situated along a specific axis means that the blocks move along that axis. For example, if the door moves
     * along the North/South <i>(= Z)</i> axis, all animated blocks will have a different Z-coordinate depending on the
     * time of day and a X-coordinate depending on the X-coordinate they originally started at.
     *
     * @return True if this door is animated along the North/South axis.
     */
    @Getter(onMethod = @__({@Override}))
    protected final boolean northSouthAligned;

    @Getter(onMethod = @__({@Override}))
    @Setter(onMethod = @__({@Override}))
    protected int autoCloseTime;

    @Getter(onMethod = @__({@Override}))
    @Setter(onMethod = @__({@Override}))
    protected int autoOpenTime;

    public GarageDoor(final @NotNull DoorData doorData, final int autoCloseTime, final int autoOpenTime,
                      final boolean northSouthAligned)
    {
        super(doorData);
        this.autoCloseTime = autoCloseTime;
        this.autoOpenTime = autoOpenTime;
        this.northSouthAligned = northSouthAligned;
    }

    public GarageDoor(final @NotNull DoorData doorData, final boolean northSouthAligned)
    {
        this(doorData, -1, -1, northSouthAligned);
    }

    @Override
    public @NotNull DoorType getDoorType()
    {
        return DOOR_TYPE;
    }

    @Override
    public @NotNull Vector2Di[] calculateChunkRange()
    {
        int radius = 0;

        if (!isOpen())
            radius = dimensions.getY() / 16 + 1;
        else
            radius =
                Math.max(dimensions.getX(), dimensions.getZ()) / 16 + 1;

        return new Vector2Di[]{new Vector2Di(getChunk().getX() - radius, getChunk().getY() - radius),
                               new Vector2Di(getChunk().getX() + radius, getChunk().getY() + radius)};
    }

    @Override
    public @NotNull RotateDirection getCurrentToggleDir()
    {
        RotateDirection rotDir = getOpenDir();
        if (isOpen())
            return RotateDirection.getOpposite(getOpenDir());
        return rotDir;
    }

    @Override
    public boolean getPotentialNewCoordinates(final @NotNull Vector3Di newMin, final @NotNull Vector3Di newMax)
    {
        RotateDirection rotateDirection = getCurrentToggleDir();
        int minX = minimum.getX();
        int minY = minimum.getY();
        int minZ = minimum.getZ();
        int maxX = maximum.getX();
        int maxY = maximum.getY();
        int maxZ = maximum.getZ();
        int xLen = dimensions.getX();
        int yLen = dimensions.getY();
        int zLen = dimensions.getZ();

        IVector3DiConst rotateVec;
        try
        {
            rotateVec = PBlockFace.getDirection(Util.getPBlockFace(rotateDirection));
        }
        catch (Exception e)
        {
            PLogger.get().logException(new IllegalArgumentException(
                "RotateDirection \"" + rotateDirection.name() + "\" is not a valid direction for a door of type \"" +
                    getDoorType().toString() + "\""));
            return false;
        }

        if (!isOpen())
        {
            minY = maxY = maximum.getY() + 1;

            minX += rotateVec.getX();
            maxX += (1 + yLen) * rotateVec.getX();
            minZ += rotateVec.getZ();
            maxZ += (1 + yLen) * rotateVec.getZ();
        }
        else
        {
            maxY = maxY - 1;
            minY -= Math.abs(rotateVec.getX() * xLen);
            minY -= Math.abs(rotateVec.getZ() * zLen);
            minY -= 1;

            if (rotateDirection.equals(RotateDirection.SOUTH))
            {
                maxZ = maxZ + 1;
                minZ = maxZ;
            }
            else if (rotateDirection.equals(RotateDirection.NORTH))
            {
                maxZ = minZ - 1;
                minZ = maxZ;
            }
            if (rotateDirection.equals(RotateDirection.EAST))
            {
                maxX = maxX + 1;
                minX = maxX;
            }
            else if (rotateDirection.equals(RotateDirection.WEST))
            {
                maxX = minX - 1;
                minX = maxX;
            }
        }

        if (minX > maxX)
        {
            int tmp = minX;
            minX = maxX;
            maxX = tmp;
        }
        if (minZ > maxZ)
        {
            int tmp = minZ;
            minZ = maxZ;
            maxZ = tmp;
        }

        newMin.setX(minX);
        newMin.setY(minY);
        newMin.setZ(minZ);

        newMax.setX(maxX);
        newMax.setY(maxY);
        newMax.setZ(maxZ);

        return true;
    }

    @Override
    protected void registerBlockMover(final @NotNull DoorActionCause cause, final double time,
                                      final boolean skipAnimation, final @NotNull IVector3DiConst newMin,
                                      final @NotNull IVector3DiConst newMax, final @NotNull IPPlayer responsible,
                                      final @NotNull DoorActionType actionType)
    {
        // TODO: Get rid of this.
        double fixedTime = time < 0.5 ? 5 : time;

        doorOpeningUtility.registerBlockMover(
            new GarageDoorMover(this, fixedTime, doorOpeningUtility.getMultiplier(this), skipAnimation,
                                getCurrentToggleDir(), responsible, newMin, newMax, cause, actionType));
    }

    @Override
    public boolean equals(final @Nullable Object o)
    {
        if (!super.equals(o))
            return false;

        if (getClass() != o.getClass())
            return false;

        final @NotNull GarageDoor other = (GarageDoor) o;
        return northSouthAligned == other.northSouthAligned &&
            autoOpenTime == other.autoOpenTime &&
            autoCloseTime == other.autoCloseTime;
    }
}
