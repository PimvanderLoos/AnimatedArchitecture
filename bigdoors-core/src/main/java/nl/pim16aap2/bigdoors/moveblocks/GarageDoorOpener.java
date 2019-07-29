package nl.pim16aap2.bigdoors.moveblocks;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.doors.DoorBase;
import nl.pim16aap2.bigdoors.doors.DoorType;
import nl.pim16aap2.bigdoors.util.DoorToggleResult;
import nl.pim16aap2.bigdoors.util.PBlockFace;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import nl.pim16aap2.bigdoors.util.Vector3D;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class GarageDoorOpener extends Opener
{
    public GarageDoorOpener(final @NotNull BigDoors plugin)
    {
        super(plugin);
    }

    /**
     * Checks if the block on the north/east/south/west side of the location is free.
     *
     * @param playerUUID       The {@link UUID} of the player initiating the toggle or the original creator if the
     *                         {@link DoorBase} was not toggled by a {@link org.bukkit.entity.Player}.
     * @param door             The {@link DoorBase}.
     * @param currentDirection The current direction of the {@link DoorBase}.
     * @param rotateDirection  The rotation direction of the {@link DoorBase}.
     * @param min              The new minimum location of the {@link DoorBase} when it opens in the returned direction.
     *                         It is modified in the method.
     * @param max              The new maximum location of the {@link DoorBase} when it opens in the returned direction.
     *                         It is modified in the method.
     * @return True if the position is unobstructed.
     */
    private boolean isPosFree(final @Nullable UUID playerUUID, final @NotNull DoorBase door,
                              final @NotNull PBlockFace currentDirection,
                              final @NotNull RotateDirection rotateDirection, final @NotNull Location min,
                              final @NotNull Location max)
    {
        int minX = door.getMinimum().getBlockX();
        int minY = door.getMinimum().getBlockY();
        int minZ = door.getMinimum().getBlockZ();
        int maxX = door.getMaximum().getBlockX();
        int maxY = door.getMaximum().getBlockY();
        int maxZ = door.getMaximum().getBlockZ();
        int xLen = maxX - minX;
        int yLen = maxY - minY;
        int zLen = maxZ - minZ;

        Vector3D rotateVec;
        try
        {
            rotateVec = PBlockFace.getDirection(PBlockFace.valueOf(rotateDirection.toString()));
        }
        catch (Exception e)
        {
            plugin.getPLogger()
                  .severe("Failed to check if new position was free for garage door \"" + door.getDoorUID()
                              + "\" because of invalid rotateDirection \"" + rotateDirection.toString()
                              + "\". Please contact pim16aap2.");
            return false;
        }

        if (currentDirection.equals(PBlockFace.UP))
        {
            minY = maxY = door.getMaximum().getBlockY() + 1;

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

        return super.isLocationEmpty(min, max, playerUUID);
    }

    /**
     * Gets the rotation direction of this {@link DoorBase}.
     *
     * @param door       {@link DoorBase}.
     * @param currentDir The current direction.
     * @return The rotation direction of this {@link DoorBase}.
     */
    private RotateDirection getRotationDirection(final @NotNull DoorBase door, final @NotNull PBlockFace currentDir)
    {
        // When closed (standing up), open in the specified direction.
        // Otherwise, go in the close direction (opposite of openDir).
        RotateDirection rotDir = door.getOpenDir();
        if (currentDir.equals(PBlockFace.UP))
            return rotDir;
        return RotateDirection.valueOf(PBlockFace.getOpposite(currentDir).toString());
    }

    /**
     * Gets the current direction of this {@link DoorBase}.
     *
     * @param door {@link DoorBase}.
     * @return The current direction of this {@link DoorBase}.
     */
    private PBlockFace getCurrentDirection(final @NotNull DoorBase door)
    {
        int yLen = door.getMaximum().getBlockY() - door.getMinimum().getBlockY();

        // If the height is 1 or more, it means the garage door is currently standing
        // upright (closed).
        if (yLen > 0)
            return PBlockFace.UP;
        int dX = door.getEngine().getBlockX() - door.getMinimum().getBlockX();
        int dZ = door.getEngine().getBlockZ() - door.getMinimum().getBlockZ();

        return PBlockFace.faceFromDir(new Vector3D(Integer.compare(0, dX), 0, Integer.compare(0, dZ)));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public DoorToggleResult toggleDoor(final @Nullable UUID playerUUID, final @NotNull DoorBase door,
                                       final double time, boolean instantOpen, final boolean playerToggle)
    {
        DoorToggleResult isOpenable = super.canBeToggled(door, playerToggle);
        if (isOpenable != DoorToggleResult.SUCCESS)
            return abort(door, isOpenable);

        if (super.isTooBig(door))
            instantOpen = true;

        PBlockFace currentDirection = getCurrentDirection(door);
        if (currentDirection == null)
        {
            plugin.getPLogger()
                  .warn("Current direction is null for door " + door.getName() + " (" + door.getDoorUID() + ")!");
            return abort(door, DoorToggleResult.ERROR);
        }

        RotateDirection rotDirection = getRotationDirection(door, currentDirection);

        Location newMin = new Location(door.getWorld(), 0, 0, 0);
        Location newMax = new Location(door.getWorld(), 0, 0, 0);

        if (rotDirection == null || !isPosFree(playerUUID, door, currentDirection, rotDirection, newMin, newMax))
        {
            plugin.getPLogger()
                  .warn("Rotation direction is null for door " + door.getName() + " (" + door.getDoorUID() + ")!");
            return abort(door, DoorToggleResult.NODIRECTION);
        }

        // Check if the owner of the door has permission to edit blocks in the new area of the door.
        if (!super.canBreakBlocksBetweenLocs(door, newMin, newMax))
            return abort(door, DoorToggleResult.NOPERMISSION);

        // TODO: Get rid of this.
        double fixedTime = time < 0.5 ? 5 : time;

        plugin.addBlockMover(new GarageDoorMover(plugin, door.getWorld(), door, fixedTime,
                                                 plugin.getConfigLoader().getMultiplier(DoorType.BIGDOOR), instantOpen,
                                                 currentDirection, rotDirection, playerUUID));
        return DoorToggleResult.SUCCESS;
    }
}
