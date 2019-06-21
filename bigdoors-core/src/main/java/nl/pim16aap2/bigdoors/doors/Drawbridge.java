package nl.pim16aap2.bigdoors.doors;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.util.*;
import org.bukkit.Location;

/**
 * Represents a DrawBrige doorType.
 *
 * @author pim
 * @see HorizontalAxisAlignedBase
 */
public class Drawbridge extends HorizontalAxisAlignedBase
{
    Drawbridge(BigDoors plugin, long doorUID, DoorType type)
    {
        super(plugin, doorUID, type);
    }

    Drawbridge(BigDoors plugin, long doorUID)
    {
        super(plugin, doorUID, DoorType.DRAWBRIDGE);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Because drawbridges can also lie flat (and therefore violate the 1-block
     * depth requirement of {@link HorizontalAxisAlignedBase}), drawbridges need to
     * implement additional checks if that is the case.
     */
    @Override
    protected boolean calculateNorthSouthAxis()
    {
        if (dimensions.getY() != 0)
            return super.calculateNorthSouthAxis();

        MyBlockFace engineSide = getEngineSide();

        // The door is on the north/south axis if the engine is on the north or south
        // side of the door; that's the rotation point.
        return engineSide.equals(MyBlockFace.NORTH) || engineSide.equals(MyBlockFace.SOUTH);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Vector2D[] calculateChunkRange()
    {
        int xLen = max.getBlockX() - min.getBlockX();
        int yLen = max.getBlockY() - min.getBlockY();
        int zLen = max.getBlockZ() - min.getBlockZ();

        int radius = 0;

        if (dimensions.getY() != 1)
            radius = yLen / 16 + 1;
        else
            radius = Math.max(xLen, zLen) / 16 + 1;

        return new Vector2D[] { new Vector2D(getChunk().getX() - radius, getChunk().getZ() - radius),
                                new Vector2D(getChunk().getX() + radius, getChunk().getZ() + radius) };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MyBlockFace calculateCurrentDirection()
    {
        if (dimensions.getY() != 0)
            return MyBlockFace.UP;

        int dirX = 0;
        int dirZ = 0;

        if (onNorthSouthAxis())
        {
            int dZ = engine.getBlockZ() - min.getBlockZ();
            if (dZ == 0)
                dZ = engine.getBlockZ() - max.getBlockZ();
            dirZ = dZ < 0 ? 1 : dZ > 0 ? -1 : 0;
        }
        else
        {
            int dX = engine.getBlockX() - min.getBlockX();
            if (dX == 0)
                dX = engine.getBlockX() - max.getBlockX();
            dirX = dX < 0 ? 1 : dX > 0 ? -1 : 0;
        }

        return MyBlockFace.faceFromDir(new Vector3D(dirX, 0, dirZ));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setDefaultOpenDirection()
    {
        if (onNorthSouthAxis())
            openDir = RotateDirection.NORTH;
        else
            openDir = RotateDirection.EAST;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void getNewLocations(MyBlockFace openDirection, RotateDirection rotateDirection, Location newMin,
                                Location newMax, int blocksMoved, Mutable<MyBlockFace> newEngineSide)
    {
        int xLen = dimensions.getX();
        int yLen = dimensions.getY();
        int zLen = dimensions.getZ();

        int xMin = min.getBlockX();
        int yMin = min.getBlockY();
        int zMin = min.getBlockY();

        int xMax = max.getBlockX();
        int zMax = max.getBlockY();

        int newXMin = min.getBlockX();
        int newYMin = min.getBlockY();
        int newZMin = min.getBlockZ();
        int newXMax = max.getBlockX();
        int newYMax = max.getBlockY();
        int newZMax = max.getBlockZ();

        switch (rotateDirection)
        {
        case NORTH:
            if (openDirection == MyBlockFace.UP)
            {
                newEngineSide.setVal(MyBlockFace.NORTH);
//                newMin = new Location(world, xMin, yMin, zMin);
//                newMax = new Location(world, xMax, yMin + zLen, zMin);
                newYMax = yMin + dimensions.getZ();
                newZMax = zMin;
            }
            else
            {
                newEngineSide.setVal(MyBlockFace.SOUTH);
//                newMin = new Location(world, xMin, yMin, zMin - yLen);
//                newMax = new Location(world, xMax, yMin, zMin);
                newZMin = zMin - dimensions.getY();
                newYMax = yMin;
                newZMax = zMin;
            }
            break;

        case EAST:
            if (openDirection == MyBlockFace.UP)
            {
                newEngineSide.setVal(MyBlockFace.EAST);
//                newMin = new Location(world, xMax, yMin, zMin);
//                newMax = new Location(world, xMax, yMin + xLen, zMax);
                newXMin = xMax;
                newYMax = yMin + xLen;
            }
            else
            {
                newEngineSide.setVal(MyBlockFace.WEST);
//                newMin = new Location(world, xMax, yMin, zMin);
//                newMax = new Location(world, xMax + yLen, yMin, zMax);
                newXMin = xMax;
                newXMax = xMax + yLen;
                newYMax = yMin;
            }
            break;

        case SOUTH:
            if (openDirection == MyBlockFace.UP)
            {
                newEngineSide.setVal(MyBlockFace.SOUTH);
//                newMin = new Location(world, xMin, yMin, zMax);
//                newMax = new Location(world, xMax, yMin + zLen, zMax);
                newZMin = zMax;
                newYMax = yMin + zLen;
            }
            else
            {
                newEngineSide.setVal(MyBlockFace.NORTH);
//                newMin = new Location(world, xMin, yMin, zMax);
//                newMax = new Location(world, xMax, yMin, zMax + yLen);
                newZMin = zMax;
                newYMax = yMin;
                newZMax = zMax + yLen;
            }
            break;

        case WEST:
            if (openDirection == MyBlockFace.UP)
            {
                newEngineSide.setVal(MyBlockFace.WEST);
//                newMin = new Location(world, xMin, yMin, zMin);
//                newMax = new Location(world, xMin, yMin + xLen, zMax);
                newXMax = xMin;
                newYMax = yMin + xLen;
            }
            else
            {
                newEngineSide.setVal(MyBlockFace.EAST);
//                newMin = new Location(world, xMin - yLen, yMin, zMin);
//                newMax = new Location(world, xMin, yMin, zMax);
                newXMin = xMin - yLen;
                newXMax = xMin;
                newYMax = yMin;
            }
            break;
        default:
            plugin.getMyLogger().dumpStackTrace("Invalid openDirection for bridge mover: " + openDirection.toString());
            return;
        }
        newMin.setX(newXMin);
        newMin.setY(newYMin);
        newMin.setZ(newZMin);

        newMax.setX(newXMax);
        newMax.setY(newYMax);
        newMax.setZ(newZMax);
    }
}
