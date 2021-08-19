package nl.pim16aap2.bigdoors.doors.garagedoor;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.annotations.PersistentVariable;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.doors.AbstractDoor;
import nl.pim16aap2.bigdoors.doors.DoorBase;
import nl.pim16aap2.bigdoors.doors.DoorOpeningUtility;
import nl.pim16aap2.bigdoors.doors.doorArchetypes.IHorizontalAxisAligned;
import nl.pim16aap2.bigdoors.doors.doorArchetypes.ITimerToggleable;
import nl.pim16aap2.bigdoors.doortypes.DoorType;
import nl.pim16aap2.bigdoors.events.dooraction.DoorActionCause;
import nl.pim16aap2.bigdoors.events.dooraction.DoorActionType;
import nl.pim16aap2.bigdoors.moveblocks.BlockMover;
import nl.pim16aap2.bigdoors.util.Cuboid;
import nl.pim16aap2.bigdoors.util.PBlockFace;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import nl.pim16aap2.bigdoors.util.Util;
import nl.pim16aap2.bigdoors.util.vector.Vector3Di;

import java.util.Optional;

/**
 * Represents a Garage Door doorType.
 *
 * @author Pim
 */
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class GarageDoor extends AbstractDoor implements IHorizontalAxisAligned, ITimerToggleable
{
    @EqualsAndHashCode.Exclude
    private static final DoorType DOOR_TYPE = DoorTypeGarageDoor.get();

    /**
     * Describes if the {@link GarageDoor} is situated along the North/South axis <b>(= TRUE)</b> or along the East/West
     * axis
     * <b>(= FALSE)</b>.
     * <p>
     * To be situated along a specific axis means that the blocks move along that axis. For example, if the door moves
     * along the North/South <i>(= Z)</i> axis, all animated blocks will have a different Z-coordinate depending on the
     * time of day and a X-coordinate depending on the X-coordinate they originally started at.
     *
     * @return True if this door is animated along the North/South axis.
     */
    @Getter
    @PersistentVariable
    protected final boolean northSouthAligned;

    @Getter
    @Setter
    @PersistentVariable
    protected int autoCloseTime;

    @Getter
    @Setter
    @PersistentVariable
    protected int autoOpenTime;

    public GarageDoor(final DoorBase doorBase, final int autoCloseTime, final int autoOpenTime,
                      final boolean northSouthAligned)
    {
        super(doorBase);
        this.autoCloseTime = autoCloseTime;
        this.autoOpenTime = autoOpenTime;
        this.northSouthAligned = northSouthAligned;
    }

    public GarageDoor(final DoorBase doorBase, final boolean northSouthAligned)
    {
        this(doorBase, -1, -1, northSouthAligned);
    }

    @SuppressWarnings("unused")
    private GarageDoor(final DoorBase doorBase)
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
        RotateDirection rotDir = getOpenDir();
        if (isOpen())
            return RotateDirection.getOpposite(getOpenDir());
        return rotDir;
    }

    @Override
    public RotateDirection cycleOpenDirection()
    {
        if (isNorthSouthAligned())
            return getOpenDir().equals(RotateDirection.EAST) ? RotateDirection.WEST : RotateDirection.EAST;
        return getOpenDir().equals(RotateDirection.NORTH) ? RotateDirection.SOUTH : RotateDirection.NORTH;
    }

    @Override
    public synchronized Optional<Cuboid> getPotentialNewCoordinates()
    {
        final RotateDirection rotateDirection = getCurrentToggleDir();
        final Cuboid cuboid = getCuboid();

        final Vector3Di dimensions = cuboid.getDimensions();
        final Vector3Di minimum = cuboid.getMin();
        final Vector3Di maximum = cuboid.getMax();

        int minX = minimum.x();
        int minY = minimum.y();
        int minZ = minimum.z();
        int maxX = maximum.x();
        int maxY = maximum.y();
        int maxZ = maximum.z();
        int xLen = dimensions.x();
        int yLen = dimensions.y();
        int zLen = dimensions.z();

        final Vector3Di rotateVec;
        try
        {
            rotateVec = PBlockFace.getDirection(Util.getPBlockFace(rotateDirection));
        }
        catch (Exception e)
        {
            BigDoors.get().getPLogger().logThrowable(new IllegalArgumentException(
                "RotateDirection \"" + rotateDirection.name() + "\" is not a valid direction for a door of type \"" +
                    getDoorType() + "\""));
            return Optional.empty();
        }

        if (!isOpen())
        {
            minY = maxY = maximum.y() + 1;
            minX += rotateVec.x();
            maxX += (1 + yLen) * rotateVec.x();
            minZ += rotateVec.z();
            maxZ += (1 + yLen) * rotateVec.z();
        }
        else
        {
            maxY = maxY - 1;
            minY -= Math.abs(rotateVec.x() * xLen);
            minY -= Math.abs(rotateVec.z() * zLen);
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

        return Optional.of(new Cuboid(new Vector3Di(minX, minY, minZ),
                                      new Vector3Di(maxX, maxY, maxZ)));
    }

    @Override
    protected BlockMover constructBlockMover(final DoorActionCause cause, final double time,
                                             final boolean skipAnimation, final Cuboid newCuboid,
                                             final IPPlayer responsible,
                                             final DoorActionType actionType)
        throws Exception
    {
        // TODO: Get rid of this.
        double fixedTime = time < 0.5 ? 5 : time;

        return new GarageDoorMover(this, fixedTime, DoorOpeningUtility.getMultiplier(this), skipAnimation,
                                   getCurrentToggleDir(), responsible, newCuboid, cause, actionType);
    }
}
