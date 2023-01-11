package nl.pim16aap2.bigdoors.doors.garagedoor;

import com.google.common.flogger.StackSize;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
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
import nl.pim16aap2.bigdoors.util.PBlockFace;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import nl.pim16aap2.bigdoors.util.Util;
import nl.pim16aap2.bigdoors.util.vector.Vector3Di;

import javax.annotation.concurrent.GuardedBy;
import java.util.Optional;
import java.util.logging.Level;

/**
 * Represents a Garage Door doorType.
 *
 * @author Pim
 */
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@Flogger
public class GarageDoor extends AbstractDoor implements IHorizontalAxisAligned, ITimerToggleable
{
    private static final DoorType DOOR_TYPE = DoorTypeGarageDoor.get();

    /**
     * Describes if the {@link GarageDoor} is situated along the North/South axis <b>(= TRUE)</b> or along the East/West
     * axis
     * <b>(= FALSE)</b>.
     * <p>
     * To be situated along a specific axis means that the blocks move along that axis. For example, if the door moves
     * along the North/South <i>(= Z)</i> axis, all animated blocks will have a different Z-coordinate depending on the
     * time of day and an X-coordinate depending on the X-coordinate they originally started at.
     *
     * @return True if this door is animated along the North/South axis.
     */
    @Getter
    @PersistentVariable
    protected final boolean northSouthAligned;

    @PersistentVariable
    @GuardedBy("this")
    protected int autoCloseTime;

    @PersistentVariable
    @GuardedBy("this")
    protected int autoOpenTime;

    public GarageDoor(DoorBase doorBase, int autoCloseTime, int autoOpenTime, boolean northSouthAligned)
    {
        super(doorBase);
        this.autoCloseTime = autoCloseTime;
        this.autoOpenTime = autoOpenTime;
        this.northSouthAligned = northSouthAligned;
    }

    public GarageDoor(DoorBase doorBase, boolean northSouthAligned)
    {
        this(doorBase, -1, -1, northSouthAligned);
    }

    @SuppressWarnings("unused")
    private GarageDoor(DoorBase doorBase)
    {
        this(doorBase, false); // Add tmp/default values
    }

    @Override
    public DoorType getDoorType()
    {
        return DOOR_TYPE;
    }

    @Override
    protected double getLongestAnimationCycleDistance()
    {
        final Cuboid cuboid = getCuboid();
        final Vector3Di dims = cuboid.getDimensions();

        final double movement;
        if (isOpen())
            movement = isNorthSouthAligned() ? dims.z() : dims.x();
        else
            movement = dims.y();
        // Not exactly correct, but much faster and pretty close.
        return 2 * movement;
    }

    @Override
    public RotateDirection getCurrentToggleDir()
    {
        synchronized (getDoorBase())
        {
            final RotateDirection rotDir = getOpenDir();
            if (isOpen())
                return RotateDirection.getOpposite(rotDir);
            return rotDir;
        }
    }

    @Override
    public RotateDirection cycleOpenDirection()
    {
        if (isNorthSouthAligned())
            return getOpenDir().equals(RotateDirection.EAST) ? RotateDirection.WEST : RotateDirection.EAST;
        return getOpenDir().equals(RotateDirection.NORTH) ? RotateDirection.SOUTH : RotateDirection.NORTH;
    }

    @Override
    public Optional<Cuboid> getPotentialNewCoordinates()
    {
        final RotateDirection rotateDirection;
        final Cuboid cuboid;

        synchronized (getDoorBase())
        {
            rotateDirection = getCurrentToggleDir();
            cuboid = getCuboid();
        }

        final Vector3Di dimensions = cuboid.getDimensions();
        final Vector3Di minimum = cuboid.getMin();
        final Vector3Di maximum = cuboid.getMax();

        int minX = minimum.x();
        int minY = minimum.y();
        int minZ = minimum.z();
        int maxX = maximum.x();
        int maxY = maximum.y();
        int maxZ = maximum.z();
        final int xLen = dimensions.x();
        final int yLen = dimensions.y();
        final int zLen = dimensions.z();

        final Vector3Di rotateVec;
        try
        {
            rotateVec = PBlockFace.getDirection(Util.getPBlockFace(rotateDirection));
        }
        catch (Exception e)
        {
            log.at(Level.SEVERE).withStackTrace(StackSize.FULL)
               .log("RotateDirection '%s' is not a valid direction for a door of type '%s'",
                    rotateDirection.name(), getDoorType());
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
            final int tmp = minX;
            minX = maxX;
            maxX = tmp;
        }
        if (minZ > maxZ)
        {
            final int tmp = minZ;
            minZ = maxZ;
            maxZ = tmp;
        }

        return Optional.of(new Cuboid(new Vector3Di(minX, minY, minZ),
                                      new Vector3Di(maxX, maxY, maxZ)));
    }

    @Override
    protected synchronized BlockMover constructBlockMover(
        BlockMover.Context context, DoorActionCause cause, double time,
        boolean skipAnimation, Cuboid newCuboid, IPPlayer responsible,
        DoorActionType actionType)
        throws Exception
    {
        return new GarageDoorMover(
            context, this, time, config.getAnimationSpeedMultiplier(getDoorType()), skipAnimation,
            getCurrentToggleDir(), responsible, newCuboid, cause, actionType);
    }

    @Override
    public synchronized void setAutoCloseTime(int autoCloseTime)
    {
        this.autoCloseTime = autoCloseTime;
    }

    @Override
    public synchronized void setAutoOpenTime(int autoOpenTime)
    {
        this.autoOpenTime = autoOpenTime;
    }

    @Override
    public synchronized int getAutoCloseTime()
    {
        return this.autoCloseTime;
    }

    @Override
    public synchronized int getAutoOpenTime()
    {
        return this.autoOpenTime;
    }
}
