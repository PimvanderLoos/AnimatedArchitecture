package nl.pim16aap2.bigdoors.doors;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.util.Mutable;
import nl.pim16aap2.bigdoors.util.PBlockFace;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import nl.pim16aap2.bigdoors.util.Vector2D;
import nl.pim16aap2.bigdoors.util.Vector3D;
import org.bukkit.Location;

/**
 * Represents a Big Door doorType.
 *
 * @author Pim
 * @see DoorBase
 */
public class BigDoor extends DoorBase
{
    BigDoor(final BigDoors plugin, final long doorUID, DoorType type)
    {
        super(plugin, doorUID, type);
    }

    BigDoor(final BigDoors plugin, final long doorUID)
    {
        this(plugin, doorUID, DoorType.BIGDOOR);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PBlockFace calculateCurrentDirection()
    {
        return engine.getBlockZ() != min.getBlockZ() ? PBlockFace.NORTH :
               engine.getBlockX() != max.getBlockX() ? PBlockFace.EAST :
               engine.getBlockZ() != max.getBlockZ() ? PBlockFace.SOUTH :
               engine.getBlockX() != min.getBlockX() ? PBlockFace.WEST : null;
    }

    /**
     * {@inheritDoc}
     */
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
        openDir = RotateDirection.CLOCKWISE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void getNewLocations(PBlockFace openDirection, RotateDirection rotateDirection, Location newMin,
                                Location newMax, int blocksMoved, Mutable<PBlockFace> newEngineSide)
    {
        PBlockFace newDir = null;
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
                plugin.getPLogger()
                      .warn("Invalid currentDirection for BigDoor! \"" + getCurrentDirection().toString() + "\"");
                return;
        }

        Vector3D newVec = PBlockFace.getDirection(newDir);
        int xMin = Math.min(engine.getBlockX(), engine.getBlockX() + dimensions.getZ() * newVec.getX());
        int xMax = Math.max(engine.getBlockX(), engine.getBlockX() + dimensions.getZ() * newVec.getX());

        int zMin = Math.min(engine.getBlockZ(), engine.getBlockZ() + dimensions.getX() * newVec.getZ());
        int zMax = Math.max(engine.getBlockZ(), engine.getBlockZ() + dimensions.getX() * newVec.getZ());

        newMin.setX(xMin);
        newMin.setY(min.getBlockY());
        newMin.setZ(zMin);

        newMax.setX(xMax);
        newMax.setY(max.getBlockY());
        newMax.setZ(zMax);
    }
}
