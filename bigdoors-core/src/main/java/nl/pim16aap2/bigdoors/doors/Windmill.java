package nl.pim16aap2.bigdoors.doors;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.events.dooraction.DoorActionCause;
import nl.pim16aap2.bigdoors.moveblocks.WindmillMover;
import nl.pim16aap2.bigdoors.util.PBlockFace;
import nl.pim16aap2.bigdoors.util.PLogger;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import nl.pim16aap2.bigdoors.util.Vector2D;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a Windmill doorType.
 *
 * @author Pim
 * @see HorizontalAxisAlignedBase
 */
public class Windmill extends HorizontalAxisAlignedBase
{
    Windmill(final @NotNull PLogger pLogger, final long doorUID, final @NotNull DoorType type)
    {
        super(pLogger, doorUID, type);
    }

    Windmill(final @NotNull PLogger pLogger, final long doorUID)
    {
        this(pLogger, doorUID, DoorType.WINDMILL);
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
    @NotNull
    @Override
    public RotateDirection cycleOpenDirection()
    {
        // This type goes exactly the other way as most usual axis aligned ones.
        if (!onNorthSouthAxis())
            return getOpenDir().equals(RotateDirection.EAST) ? RotateDirection.WEST : RotateDirection.EAST;
        return getOpenDir().equals(RotateDirection.NORTH) ? RotateDirection.SOUTH : RotateDirection.NORTH;
    }

    /**
     * {@inheritDoc}
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
            openDir = RotateDirection.NORTH;
        else
            openDir = RotateDirection.EAST;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void getNewLocations(final @Nullable PBlockFace openDirection,
                                final @Nullable RotateDirection rotateDirection, final @NotNull Location newMin,
                                final @NotNull Location newMax, final int blocksMoved)
    {
        newMin.setX(min.getBlockX());
        newMin.setY(min.getBlockY());
        newMin.setZ(min.getBlockZ());

        newMax.setX(max.getBlockX());
        newMax.setY(max.getBlockY());
        newMax.setZ(max.getBlockZ());
    }

    /**
     * {@inheritDoc}
     */
    @NotNull
    @Override
    public RotateDirection getCurrentToggleDir()
    {
        return getOpenDir();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean getPotentialNewCoordinates(final @NotNull Location min, final @NotNull Location max)
    {
        // get some invalid coordinates, to make sure there aren't any blocks in the way.
        max.setX(min.getBlockX());
        max.setY(1000);
        min.setY(1000);
        max.setY(min.getBlockZ());
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
        // TODO: Get rid of this.
        double fixedTime = time < 0.5 ? 5 : time;

        doorOpener.registerBlockMover(
            new WindmillMover(plugin, getWorld(), this, fixedTime,
                              plugin.getConfigLoader().getMultiplier(DoorType.WINDMILL), getCurrentToggleDir(),
                              cause == DoorActionCause.PLAYER ? getPlayerUUID() : null));
    }
}
