package nl.pim16aap2.bigdoors.doors;

import nl.pim16aap2.bigdoors.events.dooraction.DoorActionCause;
import nl.pim16aap2.bigdoors.moveblocks.SlidingMover;
import nl.pim16aap2.bigdoors.util.PBlockFace;
import nl.pim16aap2.bigdoors.util.PLogger;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import nl.pim16aap2.bigdoors.util.Util;
import nl.pim16aap2.bigdoors.util.Vector2D;
import nl.pim16aap2.bigdoors.util.Vector3D;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a Sliding Door doorType.
 *
 * @author Pim
 * @see HorizontalAxisAlignedBase
 */
public class SlidingDoor extends HorizontalAxisAlignedBase
{
    protected SlidingDoor(final @NotNull PLogger pLogger, final long doorUID, final @NotNull DoorData doorData,
                          final @NotNull DoorType type)
    {
        super(pLogger, doorUID, doorData, type);
    }

    protected SlidingDoor(final @NotNull PLogger pLogger, final long doorUID, final @NotNull DoorData doorData)
    {
        this(pLogger, doorUID, doorData, DoorType.SLIDINGDOOR);
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
    public Vector2D[] calculateChunkRange()
    {
        int distanceX = 0;
        int distanceZ = 0;
        if (getOpenDir().equals(RotateDirection.NORTH) || getOpenDir().equals(RotateDirection.SOUTH))
            distanceZ = (getBlocksToMove() > 0 ? Math.max(dimensions.getZ(), getBlocksToMove()) :
                         Math.min(-dimensions.getZ(), getBlocksToMove())) / 16 + 1;
        else
            distanceX = (getBlocksToMove() > 0 ? Math.max(dimensions.getX(), getBlocksToMove()) :
                         Math.min(-dimensions.getX(), getBlocksToMove())) / 16 + 1;

        return new Vector2D[]{new Vector2D(getChunk().getX() - distanceX, getChunk().getZ() - distanceZ),
                              new Vector2D(getChunk().getX() + distanceX, getChunk().getZ() + distanceZ)};
    }

    /**
     * {@inheritDoc}
     */
    @NotNull
    @Override
    public PBlockFace calculateCurrentDirection()
    {
        return Util.getPBlockFace(isOpen() ? RotateDirection.getOpposite(getOpenDir()) : getOpenDir());
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
    @NotNull
    @Override
    public RotateDirection cycleOpenDirection()
    {
        return getOpenDir().equals(RotateDirection.NORTH) ? RotateDirection.EAST :
               getOpenDir().equals(RotateDirection.EAST) ? RotateDirection.SOUTH :
               getOpenDir().equals(RotateDirection.SOUTH) ? RotateDirection.WEST : RotateDirection.NORTH;
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
    protected boolean getPotentialNewCoordinates(final @NotNull Location newMin, final @NotNull Location newMax)
    {
        Vector3D vec = PBlockFace.getDirection(Util.getPBlockFace(getCurrentToggleDir()));

        int blocksToMove = getBlocksToMove() > 0 ? getBlocksToMove() :
                           1 + Math.abs(vec.getX() * dimensions.getX() + vec.getZ() * dimensions.getZ());

        newMin.setX(min.getBlockX() + blocksToMove * vec.getX());
        newMin.setY(min.getBlockY());
        newMin.setZ(min.getBlockZ() + blocksToMove * vec.getZ());

        newMax.setX(max.getBlockX() + blocksToMove * vec.getX());
        newMax.setY(max.getBlockY());
        newMax.setZ(max.getBlockZ() + blocksToMove * vec.getZ());
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
        RotateDirection currentToggleDir = getCurrentToggleDir();
        int blocksToMove =
            (currentToggleDir.equals(RotateDirection.NORTH) || currentToggleDir.equals(RotateDirection.SOUTH)) ?
            newMin.getBlockZ() - min.getBlockZ() : newMin.getBlockX() - min.getBlockX();

        doorOpener.registerBlockMover(
            new SlidingMover(time, this, instantOpen, blocksToMove, currentToggleDir, doorOpener.getMultiplier(this),
                             cause == DoorActionCause.PLAYER ? getPlayerUUID() : null, newMin, newMax));
    }
}
