package nl.pim16aap2.bigdoors.movable.garagedoor;

import com.google.common.flogger.StackSize;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Locked;
import lombok.extern.flogger.Flogger;
import nl.pim16aap2.bigdoors.annotations.PersistentVariable;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.events.movableaction.MovableActionCause;
import nl.pim16aap2.bigdoors.events.movableaction.MovableActionType;
import nl.pim16aap2.bigdoors.movable.AbstractMovable;
import nl.pim16aap2.bigdoors.movable.MovableBase;
import nl.pim16aap2.bigdoors.movable.MovableSnapshot;
import nl.pim16aap2.bigdoors.movable.movablearchetypes.IHorizontalAxisAligned;
import nl.pim16aap2.bigdoors.movable.movablearchetypes.ITimerToggleable;
import nl.pim16aap2.bigdoors.movabletypes.MovableType;
import nl.pim16aap2.bigdoors.moveblocks.BlockMover;
import nl.pim16aap2.bigdoors.util.Cuboid;
import nl.pim16aap2.bigdoors.util.PBlockFace;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import nl.pim16aap2.bigdoors.util.Util;
import nl.pim16aap2.bigdoors.util.vector.Vector3Di;

import javax.annotation.concurrent.GuardedBy;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Represents a Garage Door movable type.
 *
 * @author Pim
 */
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@Flogger
public class GarageDoor extends AbstractMovable implements IHorizontalAxisAligned, ITimerToggleable
{
    private static final MovableType MOVABLE_TYPE = MovableGarageDoor.get();

    @EqualsAndHashCode.Exclude
    private final ReentrantReadWriteLock lock;

    /**
     * Describes if the {@link GarageDoor} is situated along the North/South axis <b>(= TRUE)</b> or along the East/West
     * axis
     * <b>(= FALSE)</b>.
     * <p>
     * To be situated along a specific axis means that the blocks move along that axis. For example, if the movable
     * moves along the North/South <i>(= Z)</i> axis, all animated blocks will have a different Z-coordinate depending
     * on the time of day and an X-coordinate depending on the X-coordinate they originally started at.
     *
     * @return True if this movable is animated along the North/South axis.
     */
    @Getter
    @PersistentVariable
    protected final boolean northSouthAligned;

    @PersistentVariable
    @GuardedBy("lock")
    @Getter(onMethod_ = @Locked.Read)
    @Setter(onMethod_ = @Locked.Write)
    protected int autoCloseTime;

    @PersistentVariable
    @GuardedBy("lock")
    @Getter(onMethod_ = @Locked.Read)
    @Setter(onMethod_ = @Locked.Write)
    protected int autoOpenTime;

    public GarageDoor(MovableBase base, int autoCloseTime, int autoOpenTime, boolean northSouthAligned)
    {
        super(base);
        this.lock = getLock();
        this.autoCloseTime = autoCloseTime;
        this.autoOpenTime = autoOpenTime;
        this.northSouthAligned = northSouthAligned;
    }

    public GarageDoor(MovableBase base, boolean northSouthAligned)
    {
        this(base, -1, -1, northSouthAligned);
    }

    @SuppressWarnings("unused")
    private GarageDoor(MovableBase base)
    {
        this(base, false); // Add tmp/default values
    }

    @Override
    public MovableType getType()
    {
        return MOVABLE_TYPE;
    }

    @Override
    @Locked.Read
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
    public Cuboid getAnimationRange()
    {
        final Cuboid cuboid = getCuboid();
        if (isOpen())
            return cuboid.grow(1, 1, 1);

        final int vertical = cuboid.getDimensions().y();
        final Vector3Di min = cuboid.getMin();
        final Vector3Di max = cuboid.getMax();

        return switch (getCurrentToggleDir())
            {
                case NORTH -> new Cuboid(min.add(0, 0, -vertical), max.add(0, 1, 0)); // -z
                case EAST -> new Cuboid(min.add(0, 0, 0), max.add(vertical, 1, 0)); // +x
                case SOUTH -> new Cuboid(min.add(0, 0, 0), max.add(0, 1, vertical)); // +z
                case WEST -> new Cuboid(min.add(-vertical, 0, 0), max.add(0, 1, 0)); // -x
                default -> cuboid.grow(vertical, 0, vertical);
            };
    }

    @Override
    @Locked.Read
    public RotateDirection getCurrentToggleDir()
    {
        final RotateDirection rotDir = getOpenDir();
        if (isOpen())
            return RotateDirection.getOpposite(rotDir);
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
    @Locked.Read
    public Optional<Cuboid> getPotentialNewCoordinates()
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
            log.atSevere().withStackTrace(StackSize.FULL)
               .log("RotateDirection '%s' is not a valid direction for a movable of type '%s'",
                    rotateDirection.name(), getType());
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

        return Optional.of(new Cuboid(new Vector3Di(minX, minY, minZ),
                                      new Vector3Di(maxX, maxY, maxZ)));
    }

    @Override
    @Locked.Read
    protected BlockMover constructBlockMover(
        BlockMover.Context context, MovableSnapshot movableSnapshot, MovableActionCause cause, double time,
        boolean skipAnimation, Cuboid newCuboid, IPPlayer responsible,
        MovableActionType actionType)
        throws Exception
    {
        return new GarageDoorMover(
            context, this, movableSnapshot, time, config.getAnimationSpeedMultiplier(getType()), skipAnimation,
            getCurrentToggleDir(), responsible, newCuboid, cause, actionType);
    }
}
