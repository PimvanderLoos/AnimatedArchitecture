package nl.pim16aap2.bigdoors.doors;

import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.doors.doorArchetypes.IMovingDoorArchetype;
import nl.pim16aap2.bigdoors.events.dooraction.DoorActionCause;
import nl.pim16aap2.bigdoors.moveblocks.CylindricalMover;
import nl.pim16aap2.bigdoors.util.PBlockFace;
import nl.pim16aap2.bigdoors.util.PLogger;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import nl.pim16aap2.bigdoors.util.vector.Vector2Di;
import nl.pim16aap2.bigdoors.util.vector.Vector3Di;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a Big Door doorType.
 *
 * @author Pim
 * @see AbstractDoorBase
 */
public class BigDoor extends AbstractDoorBase implements IMovingDoorArchetype
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
    @NotNull
    @Override
    public PBlockFace calculateCurrentDirection()
    {
        return engine.getZ() != min.getZ() ? PBlockFace.NORTH :
               engine.getX() != max.getX() ? PBlockFace.EAST :
               engine.getZ() != max.getZ() ? PBlockFace.SOUTH :
               engine.getX() != min.getX() ? PBlockFace.WEST : PBlockFace.NONE;
    }

    /**
     * {@inheritDoc}
     */
    @NotNull
    @Override
    public Vector2Di[] calculateChunkRange()
    {
        // Yeah, radius might be too big, but it doesn't really matter.
        int radius = Math.max(dimensions.getX(), dimensions.getZ()) / 16 + 1;

        return new Vector2Di[]{new Vector2Di(getChunk().getX() - radius, getChunk().getY() - radius),
                               new Vector2Di(getChunk().getX() + radius, getChunk().getY() + radius)};
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
    public boolean getPotentialNewCoordinates(final @NotNull Vector3Di newMin, final @NotNull Vector3Di newMax)
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

        Vector3Di newVec = PBlockFace.getDirection(newDir);
        int xMin = Math.min(engine.getX(), engine.getX() + dimensions.getZ() * newVec.getX());
        int xMax = Math.max(engine.getX(), engine.getX() + dimensions.getZ() * newVec.getX());

        int zMin = Math.min(engine.getZ(), engine.getZ() + dimensions.getX() * newVec.getZ());
        int zMax = Math.max(engine.getZ(), engine.getZ() + dimensions.getX() * newVec.getZ());

        newMin.setX(xMin);
        newMin.setY(newMin.getY());
        newMin.setZ(zMin);

        newMax.setX(xMax);
        newMax.setY(newMax.getY());
        newMax.setZ(zMax);
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
        doorOpeningUtility.registerBlockMover(
            new CylindricalMover(getCurrentToggleDir(), time, getCurrentDirection(), this, skipAnimation,
                                 doorOpeningUtility.getMultiplier(this),
                                 initiator, newMin, newMax));
    }
}
