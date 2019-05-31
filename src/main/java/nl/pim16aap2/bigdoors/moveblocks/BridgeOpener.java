package nl.pim16aap2.bigdoors.moveblocks;

import org.bukkit.Location;
import org.bukkit.World;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.Door;
import nl.pim16aap2.bigdoors.util.DoorOpenResult;
import nl.pim16aap2.bigdoors.util.DoorType;
import nl.pim16aap2.bigdoors.util.MyBlockFace;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import nl.pim16aap2.bigdoors.util.Util;

public class BridgeOpener extends Opener
{
    public BridgeOpener(BigDoors plugin)
    {
        super(plugin);
    }

    // Check if the new position is free.
    private boolean isNewPosFree(Door door, MyBlockFace upDown, MyBlockFace cardinal)
    {
        int startX = 0, startY = 0, startZ = 0;
        int endX = 0, endY = 0, endZ = 0;
        World world = door.getWorld();

        if (upDown.equals(MyBlockFace.UP))
            switch (cardinal)
            {
            // North West = Min X, Min Z
            // South West = Min X, Max Z
            // North East = Max X, Min Z
            // South East = Max X, Max X
            case NORTH:
                startX = door.getMinimum().getBlockX();
                endX = door.getMaximum().getBlockX();

                startY = door.getMinimum().getBlockY() + 1;
                endY = door.getMinimum().getBlockY() + door.getMaximum().getBlockZ() - door.getMinimum().getBlockZ();

                startZ = door.getMinimum().getBlockZ();
                endZ = door.getMinimum().getBlockZ();
                break;

            case SOUTH:
                startX = door.getMinimum().getBlockX();
                endX = door.getMaximum().getBlockX();

                startY = door.getMinimum().getBlockY() + 1;
                endY = door.getMinimum().getBlockY() + door.getMaximum().getBlockZ() - door.getMinimum().getBlockZ();

                startZ = door.getMaximum().getBlockZ();
                endZ = door.getMaximum().getBlockZ();
                break;

            case EAST:
                startX = door.getMaximum().getBlockX();
                endX = door.getMaximum().getBlockX();

                startY = door.getMinimum().getBlockY() + 1;
                endY = door.getMinimum().getBlockY() + door.getMaximum().getBlockX() - door.getMinimum().getBlockX();

                startZ = door.getMinimum().getBlockZ();
                endZ = door.getMaximum().getBlockZ();
                break;

            case WEST:
                startX = door.getMinimum().getBlockX();
                endX = door.getMinimum().getBlockX();

                startY = door.getMinimum().getBlockY() + 1;
                endY = door.getMinimum().getBlockY() + door.getMaximum().getBlockX() - door.getMinimum().getBlockX();

                startZ = door.getMinimum().getBlockZ();
                endZ = door.getMaximum().getBlockZ();
                break;
            default:
                plugin.getMyLogger().dumpStackTrace("Invalid rotation for bridge opener: " + cardinal.toString());
                break;
            }
        else
            switch (cardinal)
            {
            // North West = Min X, Min Z
            // South West = Min X, Max Z
            // North East = Max X, Min Z
            // South East = Max X, Max X
            case NORTH:
                startX = door.getMinimum().getBlockX();
                endX = door.getMaximum().getBlockX();

                startY = door.getMinimum().getBlockY();
                endY = door.getMinimum().getBlockY();

                startZ = door.getMinimum().getBlockZ() - door.getMaximum().getBlockY() + door.getMinimum().getBlockY();
                endZ = door.getMinimum().getBlockZ() - 1;
                break;

            case SOUTH:
                startX = door.getMinimum().getBlockX();
                endX = door.getMaximum().getBlockX();

                startY = door.getMinimum().getBlockY();
                endY = door.getMinimum().getBlockY();

                startZ = door.getMinimum().getBlockZ() + 1;
                endZ = door.getMinimum().getBlockZ() + door.getMaximum().getBlockY() - door.getMinimum().getBlockY();
                break;

            case EAST:
                startX = door.getMinimum().getBlockX() + 1;
                endX = door.getMaximum().getBlockX() + door.getMaximum().getBlockY() - door.getMinimum().getBlockY();

                startY = door.getMinimum().getBlockY();
                endY = door.getMinimum().getBlockY();

                startZ = door.getMinimum().getBlockZ();
                endZ = door.getMaximum().getBlockZ();
                break;

            case WEST:
                startX = door.getMinimum().getBlockX() - door.getMaximum().getBlockY() + door.getMinimum().getBlockY();
                endX = door.getMinimum().getBlockX() - 1;

                startY = door.getMinimum().getBlockY();
                endY = door.getMinimum().getBlockY();

                startZ = door.getMinimum().getBlockZ();
                endZ = door.getMaximum().getBlockZ();
                break;
            default:
                plugin.getMyLogger().dumpStackTrace("Invalid rotation for bridge opener: " + cardinal.toString());
                break;
            }

        for (int xAxis = startX; xAxis <= endX; ++xAxis)
            for (int yAxis = startY; yAxis <= endY; ++yAxis)
                for (int zAxis = startZ; zAxis <= endZ; ++zAxis)
                    if (!Util.isAirOrWater(world.getBlockAt(xAxis, yAxis, zAxis)))
                        return false;

        door.setNewMin(new Location(door.getWorld(), startX, startY, startZ));
        door.setNewMax(new Location(door.getWorld(), endX, endY, endZ));

        return true;
    }

    // Check if the bridge should go up or down.
    public MyBlockFace getUpDown(Door door)
    {
        int height = Math.abs(door.getMinimum().getBlockY() - door.getMaximum().getBlockY());
        if (height > 0)
            return MyBlockFace.DOWN;
        return MyBlockFace.UP;
    }

    // Figure out which way the bridge should go.
    private RotateDirection getOpenDirection(Door door)
    {
        MyBlockFace upDown = getUpDown(door);
        MyBlockFace cDir = getCurrentDirection(door);
        boolean NS = cDir == MyBlockFace.NORTH || cDir == MyBlockFace.SOUTH;

        if (upDown.equals(MyBlockFace.UP))
            return isNewPosFree(door, upDown, door.getEngSide()) ?
                RotateDirection.valueOf(door.getEngSide().toString()) : null;

        if (door.getOpenDir().equals(RotateDirection.CLOCKWISE) && !door.isOpen() ||
            door.getOpenDir().equals(RotateDirection.COUNTERCLOCKWISE) && door.isOpen())
        {
            return NS && isNewPosFree(door, upDown, MyBlockFace.SOUTH) ? RotateDirection.SOUTH :
                !NS && isNewPosFree(door, upDown, MyBlockFace.EAST) ? RotateDirection.EAST : null;
        }
        if (door.getOpenDir().equals(RotateDirection.CLOCKWISE) && door.isOpen() ||
            door.getOpenDir().equals(RotateDirection.COUNTERCLOCKWISE) && !door.isOpen())
        {
            return NS && isNewPosFree(door, upDown, MyBlockFace.NORTH) ? RotateDirection.NORTH :
                !NS && isNewPosFree(door, upDown, MyBlockFace.WEST) ? RotateDirection.WEST : null;
        }

        return NS && isNewPosFree(door, upDown, MyBlockFace.NORTH) ? RotateDirection.NORTH :
            !NS && isNewPosFree(door, upDown, MyBlockFace.EAST) ? RotateDirection.EAST :
            NS && isNewPosFree(door, upDown, MyBlockFace.SOUTH) ? RotateDirection.SOUTH :
            !NS && isNewPosFree(door, upDown, MyBlockFace.WEST) ? RotateDirection.WEST : null;
    }

    // Get the "current direction". In this context this means on which side of the
    // drawbridge the engine is.
    private MyBlockFace getCurrentDirection(Door door)
    {
        return door.getEngSide();
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
                .logMessage("Current direction is null for bridge " + door.getName() + " (" + door.getDoorUID() + ")!",
                            true, false);
            return abort(door, DoorOpenResult.ERROR);
        }

        MyBlockFace upDown = getUpDown(door);
        if (upDown == null)
        {
            plugin.getMyLogger()
                .logMessage("UpDown direction is null for bridge " + door.getName() + " (" + door.getDoorUID() + ")!",
                            true, false);
            return abort(door, DoorOpenResult.ERROR);
        }

        RotateDirection openDirection = getOpenDirection(door);
        if (openDirection == null)
        {
            plugin.getMyLogger().logMessage("OpenDirection direction is null for bridge " + door.getName() + " ("
                + door.getDoorUID() + ")!", true, false);
            return abort(door, DoorOpenResult.NODIRECTION);
        }

        // The door's owner does not have permission to move the door into the new
        // position (e.g. worldguard doens't allow it.
        if (plugin.canBreakBlocksBetweenLocs(door.getPlayerUUID(), door.getNewMin(), door.getNewMax()) != null)
            return abort(door, DoorOpenResult.NOPERMISSION);

        plugin.addBlockMover(new BridgeMover(plugin, door.getWorld(), time, door, upDown, openDirection, instantOpen,
                                             plugin.getConfigLoader().getMultiplier(DoorType.DRAWBRIDGE)));

        return DoorOpenResult.SUCCESS;
    }
}
