package nl.pim16aap2.bigdoors.doors;

import nl.pim16aap2.bigdoors.events.dooraction.DoorActionCause;
import nl.pim16aap2.bigdoors.moveblocks.FlagMover;
import nl.pim16aap2.bigdoors.util.PBlockFace;
import nl.pim16aap2.bigdoors.util.PLogger;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import nl.pim16aap2.bigdoors.util.Util;
import nl.pim16aap2.bigdoors.util.Vector2D;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a Flag doorType.
 *
 * @author Pim
 * @see DoorBase
 */
public class Flag extends DoorBase
{
    protected Flag(final @NotNull PLogger pLogger, final long doorUID, final @NotNull DoorData doorData,
                   final @NotNull DoorType type)
    {
        super(pLogger, doorUID, doorData, type);
    }

    protected Flag(final @NotNull PLogger pLogger, final long doorUID, final @NotNull DoorData doorData)
    {
        this(pLogger, doorUID, doorData, DoorType.FLAG);
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
    public PBlockFace calculateCurrentDirection()
    {
        return engine.getBlockZ() != min.getBlockZ() ? PBlockFace.NORTH :
               engine.getBlockX() != max.getBlockX() ? PBlockFace.EAST :
               engine.getBlockZ() != max.getBlockZ() ? PBlockFace.SOUTH :
               engine.getBlockX() != min.getBlockX() ? PBlockFace.WEST : PBlockFace.NONE;
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
        setOpenDir(Util.getRotateDirection(getCurrentDirection()));
    }

    /**
     * {@inheritDoc}
     * <p>
     * Because flags do not actually open in any direction, cycling the openDirection does not do anything.
     *
     * @return The current open direction.
     */
    @NotNull
    @Override
    public RotateDirection cycleOpenDirection()
    {
        return getOpenDir();
    }

    /**
     * {@inheritDoc}
     * <p>
     * Always the same as {@link #getOpenDir()}, as this type makes no distinction between opening and closing.
     */
    @NotNull
    @Override
    public RotateDirection getCurrentToggleDir()
    {
        return RotateDirection.NONE;
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

    /**
     * {@inheritDoc}
     */
    @Override
    protected void registerBlockMover(final @NotNull DoorActionCause cause, final double time,
                                      final boolean instantOpen, final @NotNull Location newMin,
                                      final @NotNull Location newMax)
    {
        doorOpeningUtility.registerBlockMover(
            new FlagMover(60, this, doorOpeningUtility.getMultiplier(this),
                          cause.equals(DoorActionCause.PLAYER) ? getPlayerUUID() : null));
    }
}
