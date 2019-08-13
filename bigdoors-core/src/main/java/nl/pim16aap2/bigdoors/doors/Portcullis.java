package nl.pim16aap2.bigdoors.doors;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.events.dooraction.DoorActionCause;
import nl.pim16aap2.bigdoors.moveblocks.VerticalMover;
import nl.pim16aap2.bigdoors.spigotutil.SpigotUtil;
import nl.pim16aap2.bigdoors.util.PBlockFace;
import nl.pim16aap2.bigdoors.util.PLogger;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import nl.pim16aap2.bigdoors.util.Vector2D;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;

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
    public RotateDirection cycleOpenDirection()
    {
        return getOpenDir().equals(RotateDirection.UP) ? RotateDirection.DOWN : RotateDirection.UP;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isOpenable()
    {
        return !isOpen;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isCloseable()
    {
        return isOpen;
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
    @NotNull
    @Override
    public RotateDirection getCurrentToggleDir()
    {
        return isOpen() ? RotateDirection.DOWN : RotateDirection.UP;
    }

    /**
     * Gets the number of blocks this door can move in the given direction. If set, it won't go further than {@link
     * #blocksToMove}
     *
     * @param upDown Whether to count the available number of blocks above or under the door. Only {@link
     *               RotateDirection#UP} and {@link RotateDirection#DOWN} are supported.
     * @return Gets the number of blocks this door can move in the given direction.
     */
    private int getBlocksInDir(final @NotNull RotateDirection upDown)
    {
        if ((!(upDown.equals(RotateDirection.UP) || upDown.equals(RotateDirection.DOWN))))
        {
            pLogger.logException(new IllegalArgumentException(
                "RotateDirection \"" + upDown.name() + "\" is not a valid direction for a door of type \"" +
                    getType().name() + "\""));
            return 0;
        }
        int xMin, xMax, zMin, zMax, yMin, yMax, yLen, blocksMoved = 0, delta;
        xMin = getMinimum().getBlockX();
        yMin = getMinimum().getBlockY();
        zMin = getMinimum().getBlockZ();
        xMax = getMaximum().getBlockX();
        yMax = getMaximum().getBlockY();
        zMax = getMaximum().getBlockZ();
        yLen = yMax - yMin + 1;

        int distanceToCheck = getBlocksToMove() < 1 ? yLen : getBlocksToMove();

        int xAxis, yAxis, zAxis, yGoal;
        World world = getWorld();
        delta = upDown == RotateDirection.DOWN ? -1 : 1;
        yAxis = upDown == RotateDirection.DOWN ? yMin - 1 : yMax + 1;
        yGoal = upDown == RotateDirection.DOWN ? yMin - distanceToCheck - 1 : yMax + distanceToCheck + 1;

        while (yAxis != yGoal)
        {
            for (xAxis = xMin; xAxis <= xMax; ++xAxis)
                for (zAxis = zMin; zAxis <= zMax; ++zAxis)
                    if (!SpigotUtil.isAirOrLiquid(world.getBlockAt(xAxis, yAxis, zAxis)))
                        return blocksMoved;
            yAxis += delta;
            blocksMoved += delta;
        }
        return blocksMoved;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean getPotentialNewCoordinates(final @NotNull Location min, final @NotNull Location max)
    {
        int blocksToMove = getBlocksInDir(getCurrentToggleDir());
        min.add(0, blocksToMove, 0);
        max.add(0, blocksToMove, 0);
        return blocksToMove != 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void registerBlockMover(final @NotNull DoorActionCause cause, final double time,
                                      final boolean instantOpen, final @NotNull Location newMin,
                                      final @NotNull Location newMax, final @NotNull BigDoors plugin)
    {
        int blocksToMove = newMin.getBlockY() - min.getBlockY();
        doorOpener.registerBlockMover(
            new VerticalMover(plugin, getWorld(), time, this, instantOpen, blocksToMove, doorOpener.getMultiplier(this),
                              cause == DoorActionCause.PLAYER ? getPlayerUUID() : null, newMin, newMax));
    }
}
