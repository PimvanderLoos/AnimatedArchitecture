package nl.pim16aap2.bigDoors.moveBlocks;

import nl.pim16aap2.bigDoors.Door;
import nl.pim16aap2.bigDoors.util.DoorOpenResult;

public interface Opener
{
    public DoorOpenResult openDoor (Door door, double time);
    public DoorOpenResult openDoor (Door door, double time, boolean instantOpen, boolean silent);
}
