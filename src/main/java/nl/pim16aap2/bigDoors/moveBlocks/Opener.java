package nl.pim16aap2.bigDoors.moveBlocks;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import nl.pim16aap2.bigDoors.BigDoors;
import nl.pim16aap2.bigDoors.Door;
import nl.pim16aap2.bigDoors.util.DoorOpenResult;
import nl.pim16aap2.bigDoors.util.RotateDirection;
import nl.pim16aap2.bigDoors.util.Util;

public interface Opener
{
    public DoorOpenResult openDoor (Door door, double time);
    public DoorOpenResult openDoor (Door door, double time, boolean instantOpen, boolean silent);

    RotateDirection getRotateDirection(Door door);
    boolean isRotateDirectionValid(Door door);

    default int getSizeLimit(final Door door)
    {
        int globalLimit = BigDoors.get().getConfigLoader().maxDoorSize();
        Player player = Bukkit.getPlayer(door.getPlayerUUID());
        int personalLimit = player == null ? -1 : Util.getMaxDoorSizeForPlayer(player);

        return Util.getLowestPositiveNumber(personalLimit, globalLimit);
    }
}
