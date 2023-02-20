package nl.pim16aap2.bigdoors.structures.garagedoor;

import com.google.common.flogger.StackSize;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.Locked;
import lombok.extern.flogger.Flogger;
import nl.pim16aap2.bigdoors.core.annotations.Deserialization;
import nl.pim16aap2.bigdoors.core.annotations.PersistentVariable;
import nl.pim16aap2.bigdoors.core.moveblocks.AnimationRequestData;
import nl.pim16aap2.bigdoors.core.moveblocks.IAnimationComponent;
import nl.pim16aap2.bigdoors.core.structures.AbstractStructure;
import nl.pim16aap2.bigdoors.core.structures.structurearchetypes.IHorizontalAxisAligned;
import nl.pim16aap2.bigdoors.core.util.Cuboid;
import nl.pim16aap2.bigdoors.core.util.MovementDirection;
import nl.pim16aap2.bigdoors.core.util.PBlockFace;
import nl.pim16aap2.bigdoors.core.util.Rectangle;
import nl.pim16aap2.bigdoors.core.util.Util;
import nl.pim16aap2.bigdoors.core.util.vector.Vector3Di;

import java.util.Optional;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Represents a Garage Door structure type.
 *
 * @author Pim
 */
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@Flogger
public class GarageDoor extends AbstractStructure implements IHorizontalAxisAligned
{
    @EqualsAndHashCode.Exclude
    @SuppressWarnings({"FieldCanBeLocal", "unused"})
    private final ReentrantReadWriteLock lock;

    @Getter
    @PersistentVariable(value = "northSouthAnimated")
    protected final boolean northSouthAnimated;

    @Deserialization
    public GarageDoor(BaseHolder base, @PersistentVariable(value = "northSouthAnimated") boolean northSouthAnimated)
    {
        super(base, StructureTypeGarageDoor.get());
        this.lock = getLock();
        this.northSouthAnimated = northSouthAnimated;
    }

    @Override
    @Locked.Read
    protected double calculateAnimationCycleDistance()
    {
        final Cuboid cuboid = getCuboid();
        final Vector3Di dims = cuboid.getDimensions();

        final double movement;
        if (isOpen())
            movement = isNorthSouthAnimated() ? dims.z() : dims.x();
        else
            movement = dims.y();
        // Not exactly correct, but much faster and pretty close.
        return 2 * movement;
    }

    @Override
    @Locked.Read
    protected Rectangle calculateAnimationRange()
    {
        final Cuboid cuboid = getCuboid();
        if (isOpen())
            return cuboid.grow(1, 1, 1).asFlatRectangle();

        final int vertical = cuboid.getDimensions().y();
        final Vector3Di min = cuboid.getMin();
        final Vector3Di max = cuboid.getMax();

        final Cuboid cuboidRange = switch (getCurrentToggleDir())
            {
                case NORTH -> new Cuboid(min.add(0, 0, -vertical), max.add(0, 1, 0)); // -z
                case EAST -> new Cuboid(min.add(0, 0, 0), max.add(vertical, 1, 0)); // +x
                case SOUTH -> new Cuboid(min.add(0, 0, 0), max.add(0, 1, vertical)); // +z
                case WEST -> new Cuboid(min.add(-vertical, 0, 0), max.add(0, 1, 0)); // -x
                default -> cuboid.grow(vertical, 0, vertical);
            };
        return cuboidRange.asFlatRectangle();
    }

    @Override
    @Locked.Read
    public MovementDirection getCurrentToggleDir()
    {
        final MovementDirection movementDirection = getOpenDir();
        if (isOpen())
            return MovementDirection.getOpposite(movementDirection);
        return movementDirection;
    }

    @Override
    public MovementDirection cycleOpenDirection()
    {
        if (isNorthSouthAnimated())
            return getOpenDir().equals(MovementDirection.EAST) ? MovementDirection.WEST : MovementDirection.EAST;
        return getOpenDir().equals(MovementDirection.NORTH) ? MovementDirection.SOUTH : MovementDirection.NORTH;
    }

    @Override
    @Locked.Read
    public Optional<Cuboid> getPotentialNewCoordinates()
    {
        final MovementDirection movementDirection = getCurrentToggleDir();
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
            rotateVec = PBlockFace.getDirection(Util.getPBlockFace(movementDirection));
        }
        catch (Exception e)
        {
            log.atSevere().withStackTrace(StackSize.FULL)
               .log("MovementDirection '%s' is not a valid direction for a structure of type '%s'",
                    movementDirection.name(), getType());
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

            if (movementDirection.equals(MovementDirection.SOUTH))
            {
                maxZ = maxZ + 1;
                minZ = maxZ;
            }
            else if (movementDirection.equals(MovementDirection.NORTH))
            {
                maxZ = minZ - 1;
                minZ = maxZ;
            }
            if (movementDirection.equals(MovementDirection.EAST))
            {
                maxX = maxX + 1;
                minX = maxX;
            }
            else if (movementDirection.equals(MovementDirection.WEST))
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
    protected IAnimationComponent constructAnimationComponent(AnimationRequestData data)
    {
        return new GarageDoorAnimationComponent(data, getCurrentToggleDir());
    }
}
