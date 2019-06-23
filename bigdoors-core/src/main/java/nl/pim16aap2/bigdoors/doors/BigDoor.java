package nl.pim16aap2.bigdoors.doors;

import org.bukkit.Location;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.util.*;

/**
 * Represents a Big Door doorType.
 *
 * @author Pim
 * @see DoorBase
 */
public class BigDoor extends DoorBase
{
    BigDoor(final BigDoors plugin, final long doorUID, DoorType type)
    {
        super(plugin, doorUID, type);
    }

    BigDoor(final BigDoors plugin, final long doorUID)
    {
        this(plugin, doorUID, DoorType.BIGDOOR);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MyBlockFace calculateCurrentDirection()
    {
        return engine.getBlockZ() != min.getBlockZ() ? MyBlockFace.NORTH :
            engine.getBlockX() != max.getBlockX() ? MyBlockFace.EAST :
            engine.getBlockZ() != max.getBlockZ() ? MyBlockFace.SOUTH :
            engine.getBlockX() != min.getBlockX() ? MyBlockFace.WEST : null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Vector2D[] calculateChunkRange()
    {
        // Yeah, radius might be too big, but it doesn't really matter.
        int radius = Math.max(dimensions.getX(), dimensions.getZ()) / 16 + 1;

        return new Vector2D[] { new Vector2D(getChunk().getX() - radius, getChunk().getZ() - radius),
                                new Vector2D(getChunk().getX() + radius, getChunk().getZ() + radius) };
    }

    /**
     * {@inheritDoc}
     *
     * @deprecated BigDoor shouldn't use ClockWise and CounterClockwise.
     */
    @Override
    @Deprecated
    public void setDefaultOpenDirection()
    {
        openDir = RotateDirection.CLOCKWISE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void getNewLocations(MyBlockFace openDirection, RotateDirection rotateDirection, Location newMin,
                                Location newMax, int blocksMoved, Mutable<MyBlockFace> newEngineSide)
    {
        MyBlockFace newDir = null;
        switch (getCurrentDirection())
        {
        case NORTH:
            newDir = rotateDirection.equals(RotateDirection.CLOCKWISE) ? MyBlockFace.EAST : MyBlockFace.WEST;
            break;
        case EAST:
            newDir = rotateDirection.equals(RotateDirection.CLOCKWISE) ? MyBlockFace.SOUTH : MyBlockFace.NORTH;
            break;
        case SOUTH:
            newDir = rotateDirection.equals(RotateDirection.CLOCKWISE) ? MyBlockFace.WEST : MyBlockFace.EAST;
            break;
        case WEST:
            newDir = rotateDirection.equals(RotateDirection.CLOCKWISE) ? MyBlockFace.NORTH : MyBlockFace.SOUTH;
            break;
        default:
            plugin.getMyLogger().warn("Invalid currentDirection for BigDoor! \"" + getCurrentDirection().toString() + "\"");
            return;
        }

        Vector3D newVec = MyBlockFace.getDirection(newDir);
        int xMin = Math.min(engine.getBlockX(), engine.getBlockX() + dimensions.getZ() * newVec.getX());
        int xMax = Math.max(engine.getBlockX(), engine.getBlockX() + dimensions.getZ() * newVec.getX());

        int zMin = Math.min(engine.getBlockZ(), engine.getBlockZ() + dimensions.getX() * newVec.getZ());
        int zMax = Math.max(engine.getBlockZ(), engine.getBlockZ() + dimensions.getX() * newVec.getZ());

        newMin.setX(xMin);
        newMin.setY(min.getBlockY());
        newMin.setZ(zMin);

        newMax.setX(xMax);
        newMax.setY(max.getBlockY());
        newMax.setZ(zMax);
    }
}
