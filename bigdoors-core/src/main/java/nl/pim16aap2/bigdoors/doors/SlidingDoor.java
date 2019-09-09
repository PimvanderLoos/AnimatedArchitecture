package nl.pim16aap2.bigdoors.doors;

import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.doors.doorArchetypes.IStationaryDoorArchetype;
import nl.pim16aap2.bigdoors.events.dooraction.DoorActionCause;
import nl.pim16aap2.bigdoors.moveblocks.SlidingMover;
import nl.pim16aap2.bigdoors.util.PBlockFace;
import nl.pim16aap2.bigdoors.util.PLogger;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import nl.pim16aap2.bigdoors.util.Util;
import nl.pim16aap2.bigdoors.util.vector.Vector2Di;
import nl.pim16aap2.bigdoors.util.vector.Vector3Di;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a Sliding Door doorType.
 *
 * @author Pim
 * @see HorizontalAxisAlignedBase
 */
public class SlidingDoor extends HorizontalAxisAlignedBase implements IStationaryDoorArchetype
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
    @NotNull
    @Override
    public Vector2Di[] calculateChunkRange()
    {
        int distanceX = 0;
        int distanceZ = 0;
        if (getOpenDir().equals(RotateDirection.NORTH) || getOpenDir().equals(RotateDirection.SOUTH))
            distanceZ = (getBlocksToMove() > 0 ? Math.max(dimensions.getZ(), getBlocksToMove()) :
                         Math.min(-dimensions.getZ(), getBlocksToMove())) / 16 + 1;
        else
            distanceX = (getBlocksToMove() > 0 ? Math.max(dimensions.getX(), getBlocksToMove()) :
                         Math.min(-dimensions.getX(), getBlocksToMove())) / 16 + 1;

        return new Vector2Di[]{new Vector2Di(getChunk().getX() - distanceX, getChunk().getY() - distanceZ),
                               new Vector2Di(getChunk().getX() + distanceX, getChunk().getY() + distanceZ)};
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
    public boolean getPotentialNewCoordinates(final @NotNull Vector3Di newMin, final @NotNull Vector3Di newMax)
    {
        Vector3Di vec = PBlockFace.getDirection(Util.getPBlockFace(getCurrentToggleDir()));

        int blocksToMove = getBlocksToMove() > 0 ? getBlocksToMove() :
                           1 + Math.abs(vec.getX() * dimensions.getX() + vec.getZ() * dimensions.getZ());

        newMin.setX(min.getX() + blocksToMove * vec.getX());
        newMin.setY(min.getY());
        newMin.setZ(min.getZ() + blocksToMove * vec.getZ());

        newMax.setX(max.getX() + blocksToMove * vec.getX());
        newMax.setY(max.getY());
        newMax.setZ(max.getZ() + blocksToMove * vec.getZ());
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void registerBlockMover(final @NotNull DoorActionCause cause, final double time,
                                      final boolean skipAnimation, final @NotNull Vector3Di newMin,
                                      final @NotNull Vector3Di newMax, final @Nullable IPPlayer initiator)
    {
        RotateDirection currentToggleDir = getCurrentToggleDir();
        int blocksToMove =
            (currentToggleDir.equals(RotateDirection.NORTH) || currentToggleDir.equals(RotateDirection.SOUTH)) ?
            newMin.getZ() - min.getZ() : newMin.getX() - min.getX();

        doorOpeningUtility.registerBlockMover(
            new SlidingMover(time, this, skipAnimation, blocksToMove, currentToggleDir,
                             doorOpeningUtility.getMultiplier(this), initiator, newMin, newMax));
    }
}
