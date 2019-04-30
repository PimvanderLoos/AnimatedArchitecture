package nl.pim16aap2.bigdoors.moveBlocks;

import nl.pim16aap2.bigdoors.Door;
import nl.pim16aap2.bigdoors.util.DoorOpenResult;

public interface Opener
{
    public DoorOpenResult openDoor (Door door, double time);
    public DoorOpenResult openDoor (Door door, double time, boolean instantOpen, boolean silent);
}
