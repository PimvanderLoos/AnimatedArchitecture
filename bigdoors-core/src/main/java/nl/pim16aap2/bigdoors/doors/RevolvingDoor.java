package nl.pim16aap2.bigdoors.doors;

import nl.pim16aap2.bigdoors.util.Mutable;
import nl.pim16aap2.bigdoors.util.PBlockFace;
import nl.pim16aap2.bigdoors.util.PLogger;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import nl.pim16aap2.bigdoors.util.Vector2D;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a Revolving Door doorType.
 *
 * @author pim
 * @see DoorBase
 */
public class RevolvingDoor extends DoorBase
{
    RevolvingDoor(PLogger pLogger, long doorUID, DoorType type)
    {
        super(pLogger, doorUID, type);
    }

    RevolvingDoor(PLogger pLogger, long doorUID)
    {
        super(pLogger, doorUID, DoorType.REVOLVINGDOOR);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Vector2D[] calculateChunkRange()
    {
        Chunk minChunk = min.getChunk();
        Chunk maxChunk = max.getChunk();

        return new Vector2D[]{new Vector2D(minChunk.getX(), minChunk.getZ()),
                              new Vector2D(maxChunk.getX(), maxChunk.getZ())};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setDefaultOpenDirection()
    {
        openDir = RotateDirection.CLOCKWISE;
    }

    // This type never faces any direction.

    /**
     * {@inheritDoc}
     */
    @Override
    public PBlockFace calculateCurrentDirection()
    {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void getNewLocations(PBlockFace openDirection, RotateDirection rotateDirection, @NotNull Location newMin,
                                @NotNull Location newMax, int blocksMoved, @Nullable Mutable<PBlockFace> newEngineSide)
    {
        newMin.setX(min.getBlockX());
        newMin.setY(min.getBlockY());
        newMin.setZ(min.getBlockZ());

        newMax.setX(max.getBlockX());
        newMax.setY(max.getBlockY());
        newMax.setZ(max.getBlockZ());
    }
}
