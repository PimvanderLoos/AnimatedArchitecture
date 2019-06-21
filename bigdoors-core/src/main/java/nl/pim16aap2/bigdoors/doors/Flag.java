package nl.pim16aap2.bigdoors.doors;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.util.Mutable;
import nl.pim16aap2.bigdoors.util.MyBlockFace;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import nl.pim16aap2.bigdoors.util.Vector2D;
import org.bukkit.Chunk;
import org.bukkit.Location;

/**
 * Represents a Flag doorType.
 *
 * @author Pim
 * @see DoorBase
 */
public class Flag extends DoorBase
{
    Flag(BigDoors plugin, long doorUID, DoorType type)
    {
        super(plugin, doorUID, type);
    }

    Flag(BigDoors plugin, long doorUID)
    {
        super(plugin, doorUID, DoorType.FLAG);
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
        Chunk minChunk = min.getChunk();
        Chunk maxChunk = max.getChunk();

        return new Vector2D[] { new Vector2D(minChunk.getX(), minChunk.getZ()),
                                new Vector2D(maxChunk.getX(), maxChunk.getZ()) };
    }

    /**
     * {@inheritDoc}
     * <p>
     * Because flags do not actually open in any direction, the open direction
     * simply the same as {@link #getCurrentDirection()}.
     */
    @Override
    public void setDefaultOpenDirection()
    {
        setOpenDir(RotateDirection.valueOf(getCurrentDirection().toString()));
    }

    /**
     * {@inheritDoc}
     * <p>
     * Because flags do not actually open in any direction, cycling the
     * openDirection is not possible.
     *
     * @return The current open direction.
     */
    @Override
    public RotateDirection cycleOpenDirection()
    {
        return openDir;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Because flags do not move when toggled, newMin and newMax are simply equal to
     * the current min and max.
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
