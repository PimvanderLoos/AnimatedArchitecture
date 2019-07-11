package nl.pim16aap2.bigdoors.doors;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.util.Mutable;
import nl.pim16aap2.bigdoors.util.PBlockFace;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import nl.pim16aap2.bigdoors.util.Vector2D;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a Sliding Door doorType.
 *
 * @author pim
 * @see HorizontalAxisAlignedBase
 */
public class SlidingDoor extends HorizontalAxisAlignedBase
{
    SlidingDoor(BigDoors plugin, long doorUID, DoorType type)
    {
        super(plugin, doorUID, type);
    }

    SlidingDoor(BigDoors plugin, long doorUID)
    {
        super(plugin, doorUID, DoorType.SLIDINGDOOR);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Vector2D[] calculateChunkRange()
    {
        int distanceX = 0;
        int distanceZ = 0;
        if (getOpenDir().equals(RotateDirection.NORTH) || getOpenDir().equals(RotateDirection.SOUTH))
            distanceZ = (blocksToMove > 0 ? Math.max(dimensions.getZ(), blocksToMove) :
                         Math.min(-dimensions.getZ(), blocksToMove)) / 16 + 1;
        else
            distanceX = (blocksToMove > 0 ? Math.max(dimensions.getX(), blocksToMove) :
                         Math.min(-dimensions.getX(), blocksToMove)) / 16 + 1;

        return new Vector2D[]{new Vector2D(getChunk().getX() - distanceX, getChunk().getZ() - distanceZ),
                              new Vector2D(getChunk().getX() + distanceX, getChunk().getZ() + distanceZ)};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PBlockFace calculateCurrentDirection()
    {
        PBlockFace looking;
        switch (openDir)
        {
            case NORTH:
                looking = PBlockFace.NORTH;
                break;
            case EAST:
                looking = PBlockFace.EAST;
                break;
            case SOUTH:
                looking = PBlockFace.SOUTH;
                break;
            case WEST:
                looking = PBlockFace.WEST;
                break;
            default:
                return null;
        }
        return isOpen ? PBlockFace.getOpposite(looking) : looking;
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
    public RotateDirection cycleOpenDirection()
    {
        return openDir.equals(RotateDirection.NORTH) ? RotateDirection.EAST :
               openDir.equals(RotateDirection.EAST) ? RotateDirection.SOUTH :
               openDir.equals(RotateDirection.SOUTH) ? RotateDirection.WEST :
               openDir.equals(RotateDirection.WEST) ? RotateDirection.NORTH : null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void getNewLocations(PBlockFace openDirection, @NotNull RotateDirection rotateDirection,
                                @NotNull Location newMin,
                                @NotNull Location newMax, int blocksMoved, @Nullable Mutable<PBlockFace> newEngineSide)
    {
        int addX = 0, addZ = 0;

        if (rotateDirection.equals(RotateDirection.NORTH) || rotateDirection.equals(RotateDirection.SOUTH))
            addZ = blocksMoved;
        else
            addX = blocksMoved;

        newMin.setX(min.getBlockX() + addX);
        newMin.setY(min.getBlockY());
        newMin.setZ(min.getBlockZ() + addZ);

        newMax.setX(max.getBlockX() + addX);
        newMax.setY(max.getBlockY());
        newMax.setZ(max.getBlockZ() + addZ);
    }
}
