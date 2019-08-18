package nl.pim16aap2.bigdoors.doors;

import nl.pim16aap2.bigdoors.events.dooraction.DoorActionCause;
import nl.pim16aap2.bigdoors.moveblocks.BridgeMover;
import nl.pim16aap2.bigdoors.util.PBlockFace;
import nl.pim16aap2.bigdoors.util.PLogger;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import nl.pim16aap2.bigdoors.util.Util;
import nl.pim16aap2.bigdoors.util.Vector2D;
import nl.pim16aap2.bigdoors.util.Vector3D;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a DrawBrige doorType.
 *
 * @author Pim
 * @see HorizontalAxisAlignedBase
 */
public class Drawbridge extends HorizontalAxisAlignedBase
{
    protected Drawbridge(final @NotNull PLogger pLogger, final long doorUID, final @NotNull DoorData doorData,
                         final @NotNull DoorType type)
    {
        super(pLogger, doorUID, doorData, type);
    }

    protected Drawbridge(final @NotNull PLogger pLogger, final long doorUID, final @NotNull DoorData doorData)
    {
        this(pLogger, doorUID, doorData, DoorType.DRAWBRIDGE);
    }

    /**
     * {@inheritDoc}
     */
    @NotNull
    @Override
    public Vector2D[] calculateChunkRange()
    {
        int xLen = dimensions.getX();
        int yLen = dimensions.getY();
        int zLen = dimensions.getZ();

        int radius;
        if (dimensions.getY() != 1)
            radius = yLen / 16 + 1;
        else
            radius = Math.max(xLen, zLen) / 16 + 1;

        return new Vector2D[]{new Vector2D(getChunk().getX() - radius, getChunk().getZ() - radius),
                              new Vector2D(getChunk().getX() + radius, getChunk().getZ() + radius)};
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
        if (!isOpen())
            return PBlockFace.UP;
        return PBlockFace.getOpposite(Util.getPBlockFace(getCurrentToggleDir()));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setDefaultOpenDirection()
    {
        if (onNorthSouthAxis())
            setOpenDir(RotateDirection.EAST);
        else
            setOpenDir(RotateDirection.NORTH);
    }

    /**
     * {@inheritDoc}
     */
    @NotNull
    @Override
    public RotateDirection getCurrentToggleDir()
    {
        return isOpen() ? getOpenDir() : RotateDirection.getOpposite(getOpenDir());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean getPotentialNewCoordinates(final @NotNull Location newMin, final @NotNull Location newMax)
    {
        Vector3D vec = PBlockFace.getDirection(getCurrentDirection());
        RotateDirection currentToggleDir = getCurrentToggleDir();
        if (isOpen())
        {
            if (onNorthSouthAxis())
            {
                newMax.setY(newMin.getBlockY() + dimensions.getX());
                int newX = vec.getX() > 0 ? newMin.getBlockX() : newMax.getBlockX();
                newMin.setX(newX);
                newMax.setX(newX);
            }
            else
            {
                newMax.setY(newMin.getBlockY() + dimensions.getZ());
                int newZ = vec.getZ() > 0 ? newMin.getBlockZ() : newMax.getBlockZ();
                newMin.setZ(newZ);
                newMax.setZ(newZ);
            }
        }
        else
        {
            if (onNorthSouthAxis()) // On Z-axis, i.e. Z doesn't change
            {
                newMax.setY(newMin.getBlockY());
                newMin.add(currentToggleDir.equals(RotateDirection.WEST) ? -dimensions.getY() : 0, 0, 0);
                newMax.add(currentToggleDir.equals(RotateDirection.EAST) ? dimensions.getY() : 0, 0, 0);
            }
            else
            {
                newMax.setY(newMin.getBlockY());
                newMin.add(0, 0, currentToggleDir.equals(RotateDirection.NORTH) ? -dimensions.getY() : 0);
                newMax.add(0, 0, currentToggleDir.equals(RotateDirection.SOUTH) ? dimensions.getY() : 0);
            }
        }
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
        PBlockFace upDown =
            Math.abs(min.getBlockY() - max.getBlockY()) > 0 ? PBlockFace.DOWN : PBlockFace.UP;

        doorOpener.registerBlockMover(
            new BridgeMover(time, this, upDown, getCurrentToggleDir(), instantOpen, doorOpener.getMultiplier(this),
                            cause == DoorActionCause.PLAYER ? getPlayerUUID() : null, newMin, newMax));
    }
}
