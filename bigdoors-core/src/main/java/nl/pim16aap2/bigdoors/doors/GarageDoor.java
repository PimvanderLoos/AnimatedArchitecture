package nl.pim16aap2.bigdoors.doors;

import nl.pim16aap2.bigdoors.util.Mutable;
import nl.pim16aap2.bigdoors.util.PBlockFace;
import nl.pim16aap2.bigdoors.util.PLogger;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import nl.pim16aap2.bigdoors.util.Vector2D;
import nl.pim16aap2.bigdoors.util.Vector3D;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a Garage Door doorType.
 *
 * @author Pim
 * @see HorizontalAxisAlignedBase
 */
public class GarageDoor extends HorizontalAxisAlignedBase
{
    GarageDoor(final @NotNull PLogger pLogger, final long doorUID, final @NotNull DoorType type)
    {
        super(pLogger, doorUID, type);
    }

    GarageDoor(final @NotNull PLogger pLogger, final long doorUID)
    {
        super(pLogger, doorUID, DoorType.GARAGEDOOR);
    }

    /**
     * {@inheritDoc}
     */
    @NotNull
    @Override
    public Vector2D[] calculateChunkRange()
    {
        int radius = 0;

        if (!isOpen)
            radius = dimensions.getY() / 16 + 1;
        else
            radius = Math.max(dimensions.getX(), dimensions.getZ()) / 16 + 1;

        return new Vector2D[]{new Vector2D(getChunk().getX() - radius, getChunk().getZ() - radius),
                              new Vector2D(getChunk().getX() + radius, getChunk().getZ() + radius)};
    }

    /**
     * {@inheritDoc}
     */
    @NotNull
    @Override
    public PBlockFace calculateCurrentDirection()
    {
        if (!isOpen)
            return PBlockFace.UP;

        int dX = engine.getBlockX() - min.getBlockX();
        int dZ = engine.getBlockZ() - min.getBlockZ();

        return PBlockFace.faceFromDir(new Vector3D(Integer.compare(0, dX), 0, Integer.compare(0, dZ)));
    }

    /**
     * {@inheritDoc}
     * <p>
     * Because garage doors can also lie flat (and therefore violate the 1-block depth requirement of {@link
     * HorizontalAxisAlignedBase}), garage doors need to implement additional checks if that is the case.
     */
    @Override
    protected boolean calculateNorthSouthAxis()
    {
        if (dimensions.getY() != 1)
            return super.calculateNorthSouthAxis();

        PBlockFace engineSide = getEngineSide();

        // The door is on the north/south axis if the engine is on the north or south
        // side
        // of the door; that's the rotation point.
        return engineSide.equals(PBlockFace.NORTH) || engineSide.equals(PBlockFace.SOUTH);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setDefaultOpenDirection()
    {
        if (onNorthSouthAxis())
            openDir = RotateDirection.EAST;
        else
            openDir = RotateDirection.NORTH;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void getNewLocations(final @Nullable PBlockFace openDirection,
                                final @Nullable RotateDirection rotateDirection, final @NotNull Location newMin,
                                final @NotNull Location newMax, final int blocksMoved,
                                final @Nullable Mutable<PBlockFace> newEngineSide)
    {
        int xMin = min.getBlockX();
        int yMin = min.getBlockY();
        int zMin = min.getBlockZ();

        int xMax = max.getBlockX();
        int yMax = max.getBlockY();
        int zMax = max.getBlockZ();

        Vector3D directionVec;
        switch (rotateDirection)
        {
            case NORTH:
                directionVec = PBlockFace.getDirection(PBlockFace.NORTH);
                break;
            case EAST:
                directionVec = PBlockFace.getDirection(PBlockFace.EAST);
                break;
            case SOUTH:
                directionVec = PBlockFace.getDirection(PBlockFace.SOUTH);
                break;
            case WEST:
                directionVec = PBlockFace.getDirection(PBlockFace.WEST);
                break;
            default:
                directionVec = null;
                pLogger.dumpStackTrace("Failed to get new locations of garage door \"" + getDoorUID()
                                           + "\". Reason: Invalid rotateDirection \"" +
                                           rotateDirection.toString() + "\"");
                return;
        }

        // This works fine, but it's disabled to make it easier to test other stuff.
        if (super.getCurrentDirection().equals(PBlockFace.UP))
        {
            yMin = yMax = max.getBlockY() + 1;

            xMin += 1 * directionVec.getX();
            xMax += (1 + dimensions.getY()) * directionVec.getX();
            zMin += 1 * directionVec.getZ();
            zMax += (1 + dimensions.getY()) * directionVec.getZ();
        }
        else
        {
            yMax = yMax - 1;
            yMin -= Math.abs(directionVec.getX() * dimensions.getX());
            yMin -= Math.abs(directionVec.getZ() * dimensions.getZ());
            yMin -= 1;

            if (rotateDirection.equals(RotateDirection.SOUTH))
            {
                zMax = zMax + 1;
                zMin = zMax;
            }
            else if (rotateDirection.equals(RotateDirection.NORTH))
            {
                zMax = zMin - 1;
                zMin = zMax;
            }
            if (rotateDirection.equals(RotateDirection.EAST))
            {
                xMax = xMax + 1;
                xMin = xMax;
            }
            else if (rotateDirection.equals(RotateDirection.WEST))
            {
                xMax = xMin - 1;
                xMin = xMax;
            }
        }

        if (xMin > xMax)
        {
            int tmp = xMin;
            xMin = xMax;
            xMax = tmp;
        }
        if (zMin > zMax)
        {
            int tmp = zMin;
            zMin = zMax;
            zMax = tmp;
        }

        newMin.setX(xMin);
        newMin.setY(yMin);
        newMin.setZ(zMin);

        newMax.setX(xMax);
        newMax.setY(yMax);
        newMax.setZ(zMax);
    }
}
