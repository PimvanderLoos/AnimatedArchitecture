package nl.pim16aap2.bigdoors.doors;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.events.dooraction.DoorActionCause;
import nl.pim16aap2.bigdoors.moveblocks.FlagMover;
import nl.pim16aap2.bigdoors.util.PBlockFace;
import nl.pim16aap2.bigdoors.util.PLogger;
import nl.pim16aap2.bigdoors.util.RotateDirection;
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
    Flag(final @NotNull PLogger pLogger, final long doorUID, final @NotNull DoorType type)
    {
        super(pLogger, doorUID, type);
    }

    Flag(final @NotNull PLogger pLogger, final long doorUID)
    {
        this(pLogger, doorUID, DoorType.FLAG);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isOpenable()
    {
        return true;
    }

    /**
     * {@inheritDoc}
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
     */
    @NotNull
    @Override
    public RotateDirection getCurrentToggleDir()
    {
        return RotateDirection.NONE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean getPotentialNewCoordinates(final @NotNull Location min, final @NotNull Location max)
    {
        min.setX(min.getBlockX());
        min.setY(min.getBlockY());
        min.setZ(min.getBlockZ());

        max.setX(max.getBlockX());
        max.setY(max.getBlockY());
        max.setZ(max.getBlockZ());
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void registerBlockMover(final @NotNull DoorActionCause cause, final double time,
                                      final boolean instantOpen, final @NotNull Location newMin,
                                      final @NotNull Location newMax, final @NotNull BigDoors plugin)
    {
        doorOpener.registerBlockMover(
            new FlagMover(plugin, getWorld(), 60, this, plugin.getConfigLoader().getMultiplier(DoorType.FLAG),
                          cause.equals(DoorActionCause.PLAYER) ? getPlayerUUID() : null));
    }
}
