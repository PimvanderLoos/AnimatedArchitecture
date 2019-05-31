package nl.pim16aap2.bigdoors.moveblocks;

import org.bukkit.Location;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.Door;
import nl.pim16aap2.bigdoors.util.DoorOpenResult;
import nl.pim16aap2.bigdoors.util.DoorType;
import nl.pim16aap2.bigdoors.util.MyBlockFace;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import nl.pim16aap2.bigdoors.util.Util;
import nl.pim16aap2.bigdoors.util.Vector3D;

public class GarageDoorOpener extends Opener
{
    public GarageDoorOpener(BigDoors plugin)
    {
        super(plugin);
    }

    // Check if the block on the north/east/south/west side of the location is free.
    private boolean isPosFree(Door door, MyBlockFace currentDirection, RotateDirection rotateDirection, Location min,
                              Location max)
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

        Vector3D rotateVec = null;
        try
        {
            rotateVec = MyBlockFace.getDirection(MyBlockFace.valueOf(rotateDirection.toString()));
        }
        catch (Exception e)
        {
            plugin.getMyLogger()
                .logMessage("Failed to check if new position was free for garage door \"" + door.getDoorUID()
                    + "\" because of invalid rotateDirection \"" + rotateDirection.toString()
                    + "\". Please contact pim16aap2.", true);
            return false;
        }

        if (currentDirection.equals(MyBlockFace.UP))
        {
            minY = maxY = door.getMaximum().getBlockY() + 1;

            minX += 1 * rotateVec.getX();
            maxX += (1 + yLen) * rotateVec.getX();
            minZ += 1 * rotateVec.getZ();
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

        max.setX(maxX);
        max.setY(maxY);
        max.setZ(maxZ);

        min.setX(minX);
        min.setY(minY);
        min.setZ(minZ);

        for (int xAxis = minX; xAxis <= maxX; ++xAxis)
            for (int yAxis = minY; yAxis <= maxY; ++yAxis)
                for (int zAxis = minZ; zAxis <= maxZ; ++zAxis)
                    if (!Util.isAirOrWater(door.getWorld().getBlockAt(xAxis, yAxis, zAxis)))
                        return false;
        return true;
    }

    // When closed (standing up), open in the specified direction.
    // Otherwise, go in the close direction (opposite of openDir).
    private RotateDirection getRotationDirection(Door door, MyBlockFace currentDir)
    {
        RotateDirection rotDir = door.getOpenDir();
        if (currentDir.equals(MyBlockFace.UP))
            return rotDir;
        return RotateDirection.valueOf(MyBlockFace.getOpposite(currentDir).toString());
    }

    // Get the direction the door is currently facing as seen from the engine to the
    // end of the door.
    private MyBlockFace getCurrentDirection(Door door)
    {
        int yLen = door.getMaximum().getBlockY() - door.getMinimum().getBlockY();

        // If the height is 1 or more, it means the garage door is currently standing
        // upright (closed).
        if (yLen > 0)
            return MyBlockFace.UP;
        int dX = door.getEngine().getBlockX() - door.getMinimum().getBlockX();
        int dZ = door.getEngine().getBlockZ() - door.getMinimum().getBlockZ();

        return MyBlockFace.faceFromDir(new Vector3D(dX < 0 ? 1 : dX > 0 ? -1 : 0, 0, dZ < 0 ? 1 : dZ > 0 ? -1 : 0));
    }

    @Override
    public DoorOpenResult openDoor(Door door, double time, boolean instantOpen, boolean silent)
    {
        DoorOpenResult isOpenable = super.isOpenable(door, silent);
        if (isOpenable != DoorOpenResult.SUCCESS)
            return abort(door, isOpenable);
        super.setBusy(door);

        if (super.isTooBig(door))
            instantOpen = true;

        MyBlockFace currentDirection = getCurrentDirection(door);
        if (currentDirection == null)
        {
            plugin.getMyLogger()
                .logMessage("Current direction is null for door " + door.getName() + " (" + door.getDoorUID() + ")!",
                            true);
            return abort(door, DoorOpenResult.ERROR);
        }

        RotateDirection rotDirection = getRotationDirection(door, currentDirection);
        Util.broadcastMessage("curDir: " + currentDirection.toString() + ", rotDir: " + rotDirection.toString());
        Location newMin = new Location(door.getWorld(), 0, 0, 0);
        Location newMax = new Location(door.getWorld(), 0, 0, 0);

        if (rotDirection == null || !isPosFree(door, currentDirection, rotDirection, newMin, newMax))
        {
            plugin.getMyLogger()
                .logMessage("Rotation direction is null for door " + door.getName() + " (" + door.getDoorUID() + ")!",
                            true);
            return abort(door, DoorOpenResult.NODIRECTION);
        }

        // The door's owner does not have permission to move the door into the new
        // position (e.g. worldguard doens't allow it.
        String canBreakResult = plugin.canBreakBlocksBetweenLocs(door.getPlayerUUID(), newMin, newMax);
        if (canBreakResult != null)
        {
            plugin.getMyLogger()
                .logMessage("Player \"" + door.getPlayerUUID().toString() + "\" is not allowed to open door "
                    + door.getName() + " (" + door.getDoorUID() + ") here! " + "Reason: " + canBreakResult, true);
            return abort(door, DoorOpenResult.NOPERMISSION);
        }

        // TODO: Get rid of this.
        if (time < 0.5)
            time = 10;

        plugin.addBlockMover(new GarageDoorMover(plugin, door.getWorld(), door, time,
                                                 plugin.getConfigLoader().getMultiplier(DoorType.DOOR),
                                                 currentDirection, rotDirection));
        return DoorOpenResult.SUCCESS;
    }
}
