package nl.pim16aap2.bigdoors.doors;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.util.Mutable;
import nl.pim16aap2.bigdoors.util.MyBlockFace;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import nl.pim16aap2.bigdoors.util.Vector2D;
import org.bukkit.Chunk;
import org.bukkit.Location;

/**
 * Represents a Windmill doorType.
 *
 * @author pim
 * @see HorizontalAxisAlignedBase
 */
public class Windmill extends HorizontalAxisAlignedBase
{
    Windmill(BigDoors plugin, long doorUID, DoorType type)
    {
        super(plugin, doorUID, type);
    }

    Windmill(BigDoors plugin, long doorUID)
    {
        this(plugin, doorUID, DoorType.WINDMILL);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Vector2D[] calculateChunkRange()
    {
        Chunk minChunk = min.getChunk();
        Chunk maxChunk = max.getChunk();

        return new Vector2D[] { new Vector2D(minChunk.getX(), minChunk.getZ()),
                                new Vector2D(maxChunk.getX(), maxChunk.getZ()) };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MyBlockFace calculateCurrentDirection()
    {
        switch (openDir)
        {
        case NORTH:
            return MyBlockFace.NORTH;
        case EAST:
            return MyBlockFace.EAST;
        case SOUTH:
            return MyBlockFace.SOUTH;
        case WEST:
            return MyBlockFace.WEST;
        default:
            return null;
        }
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
        newMin.setX(min.getBlockX());
        newMin.setY(min.getBlockY());
        newMin.setZ(min.getBlockZ());

        newMax.setX(max.getBlockX());
        newMax.setY(max.getBlockY());
        newMax.setZ(max.getBlockZ());
    }
}
