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
 * Represents a Flag doorType.
 *
 * @author Pim
 * @see DoorBase
 */
public class Flag extends DoorBase
{
    Flag(final @NotNull PLogger pLogger, final long doorUID, final @NotNull DoorType type)
    {
        super(pLogger, doorUID, type);
    }

    Flag(final @NotNull PLogger pLogger, final long doorUID)
    {
        super(pLogger, doorUID, DoorType.FLAG);
    }

    /**
     * {@inheritDoc}
     */
    @NotNull
    @Override
    public PBlockFace calculateCurrentDirection()
    {
        return engine.getBlockZ() != min.getBlockZ() ? PBlockFace.NORTH :
               engine.getBlockX() != max.getBlockX() ? PBlockFace.EAST :
               engine.getBlockZ() != max.getBlockZ() ? PBlockFace.SOUTH :
               engine.getBlockX() != min.getBlockX() ? PBlockFace.WEST : null;
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
     * <p>
     * Because flags do not actually open in any direction, the open direction simply the same as {@link
     * #getCurrentDirection()}.
     */
    @Override
    public void setDefaultOpenDirection()
    {
        setOpenDir(RotateDirection.valueOf(getCurrentDirection().toString()));
    }

    /**
     * {@inheritDoc}
     * <p>
     * Because flags do not actually open in any direction, cycling the openDirection is not possible.
     *
     * @return The current open direction.
     */
    @NotNull
    @Override
    public RotateDirection cycleOpenDirection()
    {
        return openDir;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Because flags do not move when toggled, newMin and newMax are simply equal to the current min and max.
     */
    @Override
    public void getNewLocations(final @Nullable PBlockFace openDirection,
                                final @Nullable RotateDirection rotateDirection, final @NotNull Location newMin,
                                final @NotNull Location newMax, final int blocksMoved,
                                final @Nullable Mutable<PBlockFace> newEngineSide)
    {
        newMin.setX(min.getBlockX());
        newMin.setY(min.getBlockY());
        newMin.setZ(min.getBlockZ());

        newMax.setX(max.getBlockX());
        newMax.setY(max.getBlockY());
        newMax.setZ(max.getBlockZ());
    }
}
