package nl.pim16aap2.bigdoors.doors;

import nl.pim16aap2.bigdoors.events.dooraction.DoorActionCause;
import nl.pim16aap2.bigdoors.util.PBlockFace;
import nl.pim16aap2.bigdoors.util.PLogger;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import nl.pim16aap2.bigdoors.util.Vector2D;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;


/**
 * Represents a Clock doorType.
 *
 * @author Pim
 * @see HorizontalAxisAlignedBase
 */
public class Clock extends HorizontalAxisAlignedBase
{
    protected Clock(final @NotNull PLogger pLogger, final long doorUID, final @NotNull DoorData doorData,
                    final @NotNull DoorType type)
    {
        super(pLogger, doorUID, doorData, type);
    }

    protected Clock(final @NotNull PLogger pLogger, final long doorUID, final @NotNull DoorData doorData)
    {
        this(pLogger, doorUID, doorData, DoorType.CLOCK);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Always true for this type.
     */
    @Override
    public boolean isOpenable()
    {
        return true;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Always true for this type.
     */
    @Override
    public boolean isCloseable()
    {
        return true;
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
     * Always {@link PBlockFace#NONE} for this type.
     */
    @Override
    @NotNull
    public PBlockFace calculateCurrentDirection()
    {
        return PBlockFace.NONE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setDefaultOpenDirection()
    {
        if (onNorthSouthAxis())
            setOpenDir(RotateDirection.NORTH);
        else
            setOpenDir(RotateDirection.EAST);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void registerBlockMover(final @NotNull DoorActionCause cause, final double time,
                                      final boolean instantOpen, final @NotNull Location newMin,
                                      final @NotNull Location newMax)
    {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public @NotNull RotateDirection getCurrentToggleDir()
    {
        return getOpenDir();
    }

    /**
     * {@inheritDoc}
     * <p>
     * Always returns the current min and max coordinates of this door, as this type doesn't change the locations.
     */
    @Override
    protected boolean getPotentialNewCoordinates(final @NotNull Location newMin, final @NotNull Location newMax)
    {
        newMin.setX(newMin.getBlockX());
        newMin.setY(newMin.getBlockY());
        newMin.setZ(newMin.getBlockZ());

        newMax.setX(newMax.getBlockX());
        newMax.setY(newMax.getBlockY());
        newMax.setZ(newMax.getBlockZ());
        return true;
    }
}
