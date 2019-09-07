package nl.pim16aap2.bigdoors.doors;

import nl.pim16aap2.bigdoors.events.dooraction.DoorActionCause;
import nl.pim16aap2.bigdoors.moveblocks.VerticalMover;
import nl.pim16aap2.bigdoors.util.PBlockFace;
import nl.pim16aap2.bigdoors.util.PLogger;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import nl.pim16aap2.bigdoors.util.Util;
import nl.pim16aap2.bigdoors.util.vector.Vector2Di;
import nl.pim16aap2.bigdoors.util.vector.Vector3Di;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a Portcullis doorType.
 *
 * @author Pim
 * @see DoorBase
 */
public class Portcullis extends DoorBase
{
    protected Portcullis(final @NotNull PLogger pLogger, final long doorUID, final @NotNull DoorData doorData,
                         final @NotNull DoorType type)
    {
        super(pLogger, doorUID, doorData, type);
    }

    protected Portcullis(final @NotNull PLogger pLogger, final long doorUID, final @NotNull DoorData doorData)
    {
        this(pLogger, doorUID, doorData, DoorType.PORTCULLIS);
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
        return !isOpen();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isCloseable()
    {
        return isOpen();
    }

    /**
     * {@inheritDoc}
     */
    @NotNull
    @Override
    public PBlockFace calculateCurrentDirection()
    {
        return isOpen() ? PBlockFace.DOWN : PBlockFace.UP;
    }

    /**
     * {@inheritDoc}
     */
    @NotNull
    @Override
    public Vector2Di[] calculateChunkRange()
    {
        Chunk minChunk = min.getChunk();
        Chunk maxChunk = max.getChunk();

        return new Vector2Di[]{new Vector2Di(minChunk.getX(), minChunk.getZ()),
                               new Vector2Di(maxChunk.getX(), maxChunk.getZ())};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setDefaultOpenDirection()
    {
        setOpenDir(RotateDirection.UP);
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
     * {@inheritDoc}
     */
    @Override
    protected boolean getPotentialNewCoordinates(final @NotNull Location newMin, final @NotNull Location newMax)
    {
        Vector3Di vec = PBlockFace.getDirection(Util.getPBlockFace(getCurrentToggleDir()));

        int blocksToMove = getBlocksToMove() > 0 ? getBlocksToMove() :
                           1 + Math.abs(vec.getY() * dimensions.getY());

        newMin.setX(min.getBlockX());
        newMin.setY(min.getBlockY() + blocksToMove * vec.getY());
        newMin.setZ(min.getBlockZ());

        newMax.setX(max.getBlockX());
        newMax.setY(max.getBlockY() + blocksToMove * vec.getY());
        newMax.setZ(max.getBlockZ());
        return true;

//        Vector3D vec = PBlockFace.getDirection(Util.getPBlockFace(getCurrentToggleDir()));
//
//        int blocksToMove = getBlocksToMove() > 0 ? getBlocksToMove() :
//                           1 + Math.abs(vec.getX() * dimensions.getX() + vec.getY() * dimensions.getY() +
//                                            vec.getZ() * dimensions.getZ());
//
//        newMin.setX(min.getBlockX() + blocksToMove * vec.getX());
//        newMin.setY(min.getBlockY() + blocksToMove * vec.getY());
//        newMin.setZ(min.getBlockZ() + blocksToMove * vec.getZ());
//
//        newMax.setX(max.getBlockX() + blocksToMove * vec.getX());
//        newMax.setY(max.getBlockY() + blocksToMove * vec.getY());
//        newMax.setZ(max.getBlockZ() + blocksToMove * vec.getZ());
//        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void registerBlockMover(final @NotNull DoorActionCause cause, final double time,
                                      final boolean instantOpen, final @NotNull Location newMin,
                                      final @NotNull Location newMax)
    {
        int blocksToMove = newMin.getBlockY() - min.getBlockY();
        doorOpeningUtility.registerBlockMover(
            new VerticalMover(time, this, instantOpen, blocksToMove, doorOpeningUtility.getMultiplier(this),
                              cause == DoorActionCause.PLAYER ? getPlayerUUID() : null, newMin, newMax));
    }
}
