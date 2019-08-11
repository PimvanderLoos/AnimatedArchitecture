package nl.pim16aap2.bigdoors.doors;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.events.dooraction.DoorActionCause;
import nl.pim16aap2.bigdoors.moveblocks.BridgeMover;
import nl.pim16aap2.bigdoors.util.Mutable;
import nl.pim16aap2.bigdoors.util.PBlockFace;
import nl.pim16aap2.bigdoors.util.PLogger;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import nl.pim16aap2.bigdoors.util.Vector2D;
import nl.pim16aap2.bigdoors.util.Vector3D;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a DrawBrige doorType.
 *
 * @author Pim
 * @see HorizontalAxisAlignedBase
 */
public class Drawbridge extends HorizontalAxisAlignedBase
{
    private RotateDirection currentToggleDir = null;

    Drawbridge(final @NotNull PLogger pLogger, final long doorUID, final @NotNull DoorType type)
    {
        super(pLogger, doorUID, type);
    }

    Drawbridge(final @NotNull PLogger pLogger, final long doorUID)
    {
        this(pLogger, doorUID, DoorType.DRAWBRIDGE);
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

        int radius = 0;

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
        Bukkit.broadcastMessage("calculateCurrentDirection: isOpen: " + isOpen + ", openDir: " +
                                    getOpenDir() + ", engSide: " + getEngineSide());
        if (!isOpen)
            return PBlockFace.UP;

        // TODO: REMOVE THIS. NONE should not be a valid open direction anymore. Needs to be changed in the database
        //  first, though.
        if (getOpenDir().equals(RotateDirection.NONE))
            return PBlockFace.getOpposite(getEngineSide());

        Bukkit.broadcastMessage("OpenDirection: " + getOpenDir().name());
        return PBlockFace.valueOf(getOpenDir().toString());
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
    @Override
    public void getNewLocations(final @Nullable PBlockFace openDirection,
                                final @Nullable RotateDirection rotateDirection, final @NotNull Location newMin,
                                final @NotNull Location newMax, final int blocksMoved,
                                final @Nullable Mutable<PBlockFace> newEngineSide)
    {
        throw new IllegalStateException("THIS SHOULD NOT HAVE BEEN REACHED");
    }

    @NotNull
    private RotateDirection calculateCurrentToggleDir()
    {
        RotateDirection ret;
        if (isOpen)
        {
            switch (getCurrentDirection())
            {
                case NORTH:
                    ret = RotateDirection.SOUTH;
                    break;
                case EAST:
                    ret = RotateDirection.WEST;
                    break;
                case SOUTH:
                    ret = RotateDirection.NORTH;
                    break;
                case WEST:
                    ret = RotateDirection.EAST;
                    break;
                default:
                    ret = RotateDirection.NONE;
            }
        }
        else
        {
            ret = getOpenDir();
            // TODO: This should be fixed in the database when upgrading.
            //       Additionally, considering checking input in the DatabaseCommander.
            if (onNorthSouthAxis())
                return ret.equals(RotateDirection.EAST) || ret.equals(RotateDirection.WEST) ? ret :
                       RotateDirection.EAST;
            return ret.equals(RotateDirection.NORTH) || ret.equals(RotateDirection.SOUTH) ? ret :
                   RotateDirection.NORTH;
        }
        return ret;


//        if (isOpen)
//            return RotateDirection.valueOf(PBlockFace.getOpposite(getCurrentDirection()).name());
//        RotateDirection rotDir = getOpenDir();
//
//        // TODO: This should be fixed in the database when upgrading.
//        //       Additionally, considering checking input in the DatabaseCommander.
//        if (onNorthSouthAxis())
//            return rotDir.equals(RotateDirection.EAST) || rotDir.equals(RotateDirection.WEST) ? rotDir :
//                   RotateDirection.EAST;
//        return rotDir.equals(RotateDirection.NORTH) || rotDir.equals(RotateDirection.SOUTH) ? rotDir :
//               RotateDirection.NORTH;
    }

    /**
     * {@inheritDoc}
     */
    @NotNull
    @Override
    public RotateDirection getCurrentToggleDir()
    {
        if (currentToggleDir == null)
            currentToggleDir = calculateCurrentToggleDir();
        return currentToggleDir;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean getPotentialNewCoordinates(final @NotNull Location min, final @NotNull Location max)
    {
        Vector3D vec = PBlockFace.getDirection(getCurrentDirection());
        Bukkit.broadcastMessage(
            "DOORBASE: on NS axis: " + onNorthSouthAxis() + ", vector: " + vec.toString() + ", currentDirection: " +
                getCurrentDirection().name());
        RotateDirection currentToggleDir = getCurrentToggleDir();
        if (isOpen)
        {
            if (onNorthSouthAxis())
            {
                max.setY(min.getBlockY() + dimensions.getX());
                int newX = vec.getX() > 0 ? min.getBlockX() : max.getBlockX();
                min.setX(newX);
                max.setX(newX);
            }
            else
            {
                max.setY(min.getBlockY() + dimensions.getZ());
                int newZ = vec.getZ() > 0 ? min.getBlockZ() : max.getBlockZ();
                min.setZ(newZ);
                max.setZ(newZ);
            }
        }
        else
        {
            if (onNorthSouthAxis()) // On Z-axis, i.e. Z doesn't change
            {
                max.setY(min.getBlockY());
                min.add(currentToggleDir.equals(RotateDirection.WEST) ? -dimensions.getY() : 0, 0, 0);
                max.add(currentToggleDir.equals(RotateDirection.EAST) ? dimensions.getY() : 0, 0, 0);
            }
            else
            {
                max.setY(min.getBlockY());
                min.add(0, 0, currentToggleDir.equals(RotateDirection.NORTH) ? -dimensions.getY() : 0);
                max.add(0, 0, currentToggleDir.equals(RotateDirection.SOUTH) ? dimensions.getY() : 0);
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
                                      final @NotNull Location newMax, final @NotNull BigDoors plugin)
    {
        PBlockFace upDown =
            Math.abs(getMinimum().getBlockY() - getMaximum().getBlockY()) > 0 ? PBlockFace.DOWN : PBlockFace.UP;

        Bukkit.broadcastMessage(
            "DRAWBRIDGE: IsOpen: " + isOpen + ", upDown: " + upDown.name() + ", currentToggleDir: " +
                getCurrentToggleDir().name() +
                ", currentDirection = " + getCurrentDirection());

        doorOpener.registerBlockMover(
            new BridgeMover(plugin, getWorld(), time, this, upDown, getCurrentToggleDir(), instantOpen,
                            plugin.getConfigLoader().getMultiplier(DoorType.DRAWBRIDGE),
                            cause == DoorActionCause.PLAYER ? getPlayerUUID() : null, newMin, newMax));
    }
}
