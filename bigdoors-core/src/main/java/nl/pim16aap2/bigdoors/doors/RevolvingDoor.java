package nl.pim16aap2.bigdoors.doors;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.events.dooraction.DoorActionCause;
import nl.pim16aap2.bigdoors.moveblocks.RevolvingDoorMover;
import nl.pim16aap2.bigdoors.util.PBlockFace;
import nl.pim16aap2.bigdoors.util.PLogger;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import nl.pim16aap2.bigdoors.util.Vector2D;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a Revolving Door doorType.
 *
 * @author Pim
 * @see DoorBase
 */
public class RevolvingDoor extends DoorBase
{
    RevolvingDoor(final @NotNull PLogger pLogger, final long doorUID, final @NotNull DoorType type)
    {
        super(pLogger, doorUID, type);
    }

    RevolvingDoor(final @NotNull PLogger pLogger, final long doorUID)
    {
        this(pLogger, doorUID, DoorType.REVOLVINGDOOR);
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
    @NotNull
    public PBlockFace calculateCurrentDirection()
    {
        return PBlockFace.NONE;
    }

    /**
     * {@inheritDoc}
     */
    @NotNull
    @Override
    public RotateDirection getCurrentToggleDir()
    {
        return isOpen() ? RotateDirection.getOpposite(getOpenDir()) : getOpenDir();
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
    @NotNull
    @Override
    public RotateDirection cycleOpenDirection()
    {
        return getOpenDir().equals(RotateDirection.CLOCKWISE) ? RotateDirection.COUNTERCLOCKWISE :
               RotateDirection.CLOCKWISE;
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
            new RevolvingDoorMover(plugin, getWorld(), this, fixedTime,
                                   plugin.getConfigLoader().getMultiplier(DoorType.REVOLVINGDOOR),
                                   getCurrentToggleDir(), cause == DoorActionCause.PLAYER ? getPlayerUUID() : null));
    }
}
