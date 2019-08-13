package nl.pim16aap2.bigdoors.doors;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.events.dooraction.DoorActionCause;
import nl.pim16aap2.bigdoors.moveblocks.SlidingMover;
import nl.pim16aap2.bigdoors.spigotutil.SpigotUtil;
import nl.pim16aap2.bigdoors.util.PBlockFace;
import nl.pim16aap2.bigdoors.util.PLogger;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import nl.pim16aap2.bigdoors.util.Vector2D;
import org.bukkit.Location;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a Sliding Door doorType.
 *
 * @author Pim
 * @see HorizontalAxisAlignedBase
 */
public class SlidingDoor extends HorizontalAxisAlignedBase
{
    SlidingDoor(final @NotNull PLogger pLogger, final long doorUID, final @NotNull DoorType type)
    {
        super(pLogger, doorUID, type);
    }

    SlidingDoor(final @NotNull PLogger pLogger, final long doorUID)
    {
        this(pLogger, doorUID, DoorType.SLIDINGDOOR);
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
    @NotNull
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
                return PBlockFace.NONE;
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
    @NotNull
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
    public void getNewLocations(final @Nullable PBlockFace openDirection,
                                final @Nullable RotateDirection rotateDirection, final @NotNull Location newMin,
                                final @NotNull Location newMax, final int blocksMoved)
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
     * Gets the number of blocks this door can move in the given direction. If set, it won't go further than {@link
     * #blocksToMove}
     *
     * @param slideDir Which direction to count the number of available blocks in. Must be one of the following: {@link
     *                 RotateDirection#NORTH}, {@link RotateDirection#EAST}, {@link RotateDirection#SOUTH}, or {@link
     *                 RotateDirection#WEST}.
     * @return Gets the number of blocks this door can move in the given direction.
     */
    private int getBlocksInDir(final @NotNull RotateDirection slideDir)
    {
        if (!(slideDir.equals(RotateDirection.NORTH) || slideDir.equals(RotateDirection.EAST) ||
            slideDir.equals(RotateDirection.SOUTH) || slideDir.equals(RotateDirection.WEST)))
        {
            pLogger.logException(new IllegalArgumentException(
                "RotateDirection \"" + slideDir.name() + "\" is not a valid direction for a door of type \"" +
                    getType().name() + "\""));
            return 0;
        }

        int xMin, xMax, zMin, zMax, yMin, yMax, xLen, zLen, moveBlocks = 0, step;
        xMin = getMinimum().getBlockX();
        yMin = getMinimum().getBlockY();
        zMin = getMinimum().getBlockZ();
        xMax = getMaximum().getBlockX();
        yMax = getMaximum().getBlockY();
        zMax = getMaximum().getBlockZ();

        // xLen and zLen describe the length of the door in the x and the z direction respectively.
        // If the rotation direction and the blocksToMove variable are defined, use the blocksToMove variable instead.
        xLen = getBlocksToMove() < 1 ? Math.abs(xMax - xMin) + 1 : getBlocksToMove();
        zLen = getBlocksToMove() < 1 ? Math.abs(zMax - zMin) + 1 : getBlocksToMove();

        int xAxis, yAxis, zAxis;
        step = slideDir == RotateDirection.NORTH || slideDir == RotateDirection.WEST ? -1 : 1;

        int startX, startY, startZ, endX, endY, endZ;
        startY = yMin;
        endY = yMax;
        if (slideDir == RotateDirection.NORTH)
        {
            startZ = zMin - 1;
            endZ = zMin - zLen - 1;
            startX = xMin;
            endX = xMax;
        }
        else if (slideDir == RotateDirection.SOUTH)
        {
            startZ = zMax + 1;
            endZ = zMax + zLen + 1;
            startX = xMin;
            endX = xMax;
        }
        else if (slideDir == RotateDirection.WEST)
        {
            startZ = zMin;
            endZ = zMax;
            startX = xMin - 1;
            endX = xMin - xLen - 1;
        }
        else if (slideDir == RotateDirection.EAST)
        {
            startZ = zMin;
            endZ = zMax;
            startX = xMax + 1;
            endX = xMax + xLen + 1;
        }
        else
            return 0;

        World world = getWorld();
        if (slideDir == RotateDirection.NORTH || slideDir == RotateDirection.SOUTH)
            for (zAxis = startZ; zAxis != endZ; zAxis += step)
            {
                for (xAxis = startX; xAxis != endX + 1; ++xAxis)
                    for (yAxis = startY; yAxis != endY + 1; ++yAxis)
                        if (!SpigotUtil.isAirOrLiquid(world.getBlockAt(xAxis, yAxis, zAxis)))
                            return moveBlocks;
                moveBlocks += step;
            }
        else
            for (xAxis = startX; xAxis != endX; xAxis += step)
            {
                for (zAxis = startZ; zAxis != endZ + 1; ++zAxis)
                    for (yAxis = startY; yAxis != endY + 1; ++yAxis)
                        if (!SpigotUtil.isAirOrLiquid(world.getBlockAt(xAxis, yAxis, zAxis)))
                            return moveBlocks;
                moveBlocks += step;
            }
        return moveBlocks;
    }

    /**
     * {@inheritDoc}
     */
    // TODO: Clean this shit up. Too much duplication.
    @Override
    protected boolean getPotentialNewCoordinates(final @NotNull Location min, final @NotNull Location max)
    {
        RotateDirection currentToggleDir = getCurrentToggleDir();
        int blocksToMove = getBlocksInDir(currentToggleDir);
        if (blocksToMove == 0)
            return false;

        int addX, addZ;
        switch (currentToggleDir)
        {
            case NORTH:
                addX = 0;
                addZ = -blocksToMove;
                break;
            case EAST:
                addX = blocksToMove;
                addZ = 0;
                break;
            case SOUTH:
                addX = 0;
                addZ = blocksToMove;
                break;
            case WEST:
                addX = -blocksToMove;
                addZ = 0;
                break;
            default:
                addX = 0;
                addZ = 0;
                break;
        }
        min.add(addX, 0, addZ);
        max.add(addX, 0, addZ);
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void registerBlockMover(final @NotNull DoorActionCause cause, final double time,
                                      final boolean instantOpen, final @NotNull Location newMin,
                                      final @NotNull Location newMax, final @NotNull BigDoors plugin)
    {
        RotateDirection currentToggleDir = getCurrentToggleDir();
        int blocksToMove =
            (currentToggleDir.equals(RotateDirection.NORTH) || currentToggleDir.equals(RotateDirection.SOUTH)) ?
            newMin.getBlockZ() - min.getBlockZ() : newMin.getBlockX() - min.getBlockX();

        doorOpener.registerBlockMover(
            new SlidingMover(plugin, getWorld(), time, this, instantOpen, blocksToMove, currentToggleDir,
                             plugin.getConfigLoader().getMultiplier(DoorType.SLIDINGDOOR),
                             cause == DoorActionCause.PLAYER ? getPlayerUUID() : null));
    }
}
