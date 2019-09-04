package nl.pim16aap2.bigdoors.doors;

import nl.pim16aap2.bigdoors.events.dooraction.DoorActionCause;
import nl.pim16aap2.bigdoors.moveblocks.CylindricalMover;
import nl.pim16aap2.bigdoors.util.PBlockFace;
import nl.pim16aap2.bigdoors.util.PLogger;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import nl.pim16aap2.bigdoors.util.Vector2D;
import nl.pim16aap2.bigdoors.util.Vector3D;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a Big Door doorType.
 *
 * @author Pim
 * @see DoorBase
 */
public class BigDoor extends DoorBase
{
    protected BigDoor(final @NotNull PLogger pLogger, final long doorUID, final @NotNull DoorData doorData,
                      final @NotNull DoorType type)
    {
        super(pLogger, doorUID, doorData, type);
    }

    protected BigDoor(final @NotNull PLogger pLogger, final long doorUID, final @NotNull DoorData doorData)
    {
        this(pLogger, doorUID, doorData, DoorType.BIGDOOR);
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
        // Yeah, radius might be too big, but it doesn't really matter.
        int radius = Math.max(dimensions.getX(), dimensions.getZ()) / 16 + 1;

        return new Vector2D[]{new Vector2D(getChunk().getX() - radius, getChunk().getZ() - radius),
                              new Vector2D(getChunk().getX() + radius, getChunk().getZ() + radius)};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setDefaultOpenDirection()
    {
        setOpenDir(RotateDirection.CLOCKWISE);
    }

    /**
     * {@inheritDoc}
     */
    @NotNull
    @Override
    public RotateDirection cycleOpenDirection()
    {
        return getOpenDir().equals(RotateDirection.CLOCKWISE) ?
               RotateDirection.COUNTERCLOCKWISE : RotateDirection.CLOCKWISE;
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
        PBlockFace newDir;
        RotateDirection rotateDirection = getCurrentToggleDir();
        switch (getCurrentDirection())
        {
            case NORTH:
                newDir = rotateDirection.equals(RotateDirection.CLOCKWISE) ? PBlockFace.EAST : PBlockFace.WEST;
                break;
            case EAST:
                newDir = rotateDirection.equals(RotateDirection.CLOCKWISE) ? PBlockFace.SOUTH : PBlockFace.NORTH;
                break;
            case SOUTH:
                newDir = rotateDirection.equals(RotateDirection.CLOCKWISE) ? PBlockFace.WEST : PBlockFace.EAST;
                break;
            case WEST:
                newDir = rotateDirection.equals(RotateDirection.CLOCKWISE) ? PBlockFace.NORTH : PBlockFace.SOUTH;
                break;
            default:
                pLogger.warn("Invalid currentDirection for BigDoor! \"" + getCurrentDirection().toString() + "\"");
                return false;
        }

        Vector3D newVec = PBlockFace.getDirection(newDir);
        int xMin = Math.min(engine.getBlockX(), engine.getBlockX() + dimensions.getZ() * newVec.getX());
        int xMax = Math.max(engine.getBlockX(), engine.getBlockX() + dimensions.getZ() * newVec.getX());

        int zMin = Math.min(engine.getBlockZ(), engine.getBlockZ() + dimensions.getX() * newVec.getZ());
        int zMax = Math.max(engine.getBlockZ(), engine.getBlockZ() + dimensions.getX() * newVec.getZ());

        newMin.setX(xMin);
        newMin.setY(newMin.getBlockY());
        newMin.setZ(zMin);

        newMax.setX(xMax);
        newMax.setY(newMax.getBlockY());
        newMax.setZ(zMax);
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
            new CylindricalMover(getCurrentToggleDir(), time, getCurrentDirection(), this, instantOpen,
                                 doorOpeningUtility.getMultiplier(this),
                                 cause == DoorActionCause.PLAYER ? getPlayerUUID() : null, newMin, newMax));
    }
}
