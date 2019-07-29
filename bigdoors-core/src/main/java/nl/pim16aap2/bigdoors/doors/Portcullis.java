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
 * Represents a Portcullis doorType.
 *
 * @author Pim
 * @see DoorBase
 */
public class Portcullis extends DoorBase
{
    Portcullis(final @NotNull PLogger pLogger, final long doorUID, final @NotNull DoorType type)
    {
        super(pLogger, doorUID, type);
    }

    Portcullis(final @NotNull PLogger pLogger, final long doorUID)
    {
        this(pLogger, doorUID, DoorType.PORTCULLIS);
    }

    /**
     * {@inheritDoc}
     */
    @NotNull
    @Override
    public PBlockFace calculateCurrentDirection()
    {
        return isOpen ? PBlockFace.DOWN : PBlockFace.UP;
    }

    /**
     * {@inheritDoc}
     */
    @NotNull
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
        openDir = RotateDirection.UP;
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
        newMin.setX(min.getBlockX());
        newMin.setY(min.getBlockY() + blocksMoved);
        newMin.setZ(min.getBlockZ());

        newMax.setX(max.getBlockX());
        newMax.setY(max.getBlockY() + blocksMoved);
        newMax.setZ(max.getBlockZ());
    }
}
