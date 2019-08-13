package nl.pim16aap2.bigdoors.doors;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.events.dooraction.DoorActionCause;
import nl.pim16aap2.bigdoors.moveblocks.GarageDoorMover;
import nl.pim16aap2.bigdoors.util.PBlockFace;
import nl.pim16aap2.bigdoors.util.PLogger;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import nl.pim16aap2.bigdoors.util.Vector2D;
import nl.pim16aap2.bigdoors.util.Vector3D;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a Garage Door doorType.
 *
 * @author Pim
 * @see HorizontalAxisAlignedBase
 */
public class GarageDoor extends HorizontalAxisAlignedBase
{
    GarageDoor(final @NotNull PLogger pLogger, final long doorUID, final @NotNull DoorType type)
    {
        super(pLogger, doorUID, type);
    }

    GarageDoor(final @NotNull PLogger pLogger, final long doorUID)
    {
        this(pLogger, doorUID, DoorType.GARAGEDOOR);
    }

    /**
     * {@inheritDoc}
     */
    @NotNull
    @Override
    public Vector2D[] calculateChunkRange()
    {
        int radius = 0;

        if (!isOpen)
            radius = dimensions.getY() / 16 + 1;
        else
            radius = Math.max(dimensions.getX(), dimensions.getZ()) / 16 + 1;

        return new Vector2D[]{new Vector2D(getChunk().getX() - radius, getChunk().getZ() - radius),
                              new Vector2D(getChunk().getX() + radius, getChunk().getZ() + radius)};
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
        if (!isOpen)
            return PBlockFace.UP;

        int dX = engine.getBlockX() - min.getBlockX();
        int dZ = engine.getBlockZ() - min.getBlockZ();

        return PBlockFace.faceFromDir(new Vector3D(Integer.compare(0, dX), 0, Integer.compare(0, dZ)));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setDefaultOpenDirection()
    {
        if (onNorthSouthAxis())
            openDir = RotateDirection.EAST;
        else
            openDir = RotateDirection.NORTH;
    }

    /**
     * {@inheritDoc}
     */
    @NotNull
    @Override
    public RotateDirection getCurrentToggleDir()
    {
        RotateDirection rotDir = getOpenDir();
        if (getCurrentDirection().equals(PBlockFace.UP))
            return rotDir;
        // TODO: Make this less dumb.
        return RotateDirection.valueOf(PBlockFace.getOpposite(getCurrentDirection()).toString());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean getPotentialNewCoordinates(final @NotNull Location min, final @NotNull Location max)
    {
        RotateDirection rotateDirection = getCurrentToggleDir();
        int minX = getMinimum().getBlockX();
        int minY = getMinimum().getBlockY();
        int minZ = getMinimum().getBlockZ();
        int maxX = getMaximum().getBlockX();
        int maxY = getMaximum().getBlockY();
        int maxZ = getMaximum().getBlockZ();
        int xLen = dimensions.getX();
        int yLen = dimensions.getY();
        int zLen = dimensions.getZ();

        Vector3D rotateVec;
        try
        {
            rotateVec = PBlockFace.getDirection(PBlockFace.valueOf(rotateDirection.toString()));
        }
        catch (Exception e)
        {
            pLogger.logException(new IllegalArgumentException(
                "RotateDirection \"" + rotateDirection.name() + "\" is not a valid direction for a door of type \"" +
                    getType().name() + "\""));
            return false;
        }

        if (getCurrentDirection().equals(PBlockFace.UP))
        {
            minY = maxY = getMaximum().getBlockY() + 1;

            minX += rotateVec.getX();
            maxX += (1 + yLen) * rotateVec.getX();
            minZ += rotateVec.getZ();
            maxZ += (1 + yLen) * rotateVec.getZ();
        }
        else
        {
            maxY = maxY - 1;
            minY -= Math.abs(rotateVec.getX() * xLen);
            minY -= Math.abs(rotateVec.getZ() * zLen);
            minY -= 1;

            if (rotateDirection.equals(RotateDirection.SOUTH))
            {
                maxZ = maxZ + 1;
                minZ = maxZ;
            }
            else if (rotateDirection.equals(RotateDirection.NORTH))
            {
                maxZ = minZ - 1;
                minZ = maxZ;
            }
            if (rotateDirection.equals(RotateDirection.EAST))
            {
                maxX = maxX + 1;
                minX = maxX;
            }
            else if (rotateDirection.equals(RotateDirection.WEST))
            {
                maxX = minX - 1;
                minX = maxX;
            }
        }

        if (minX > maxX)
        {
            int tmp = minX;
            minX = maxX;
            maxX = tmp;
        }
        if (minZ > maxZ)
        {
            int tmp = minZ;
            minZ = maxZ;
            maxZ = tmp;
        }

        min.setX(minX);
        min.setY(minY);
        min.setZ(minZ);

        max.setX(maxX);
        max.setY(maxY);
        max.setZ(maxZ);

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
        // TODO: Get rid of this.
        double fixedTime = time < 0.5 ? 5 : time;

        doorOpener.registerBlockMover(
            new GarageDoorMover(plugin, getWorld(), this, fixedTime,
                                plugin.getConfigLoader().getMultiplier(DoorType.BIGDOOR), instantOpen,
                                getCurrentDirection(), getCurrentToggleDir(),
                                cause == DoorActionCause.PLAYER ? getPlayerUUID() : null, newMin, newMax));
    }
}
