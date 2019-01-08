package nl.pim16aap2.bigDoors.moveBlocks;

import nl.pim16aap2.bigDoors.Door;

public interface Opener
{
	public boolean openDoor (Door door, double time);
	public boolean openDoor (Door door, double time, boolean instantOpen, boolean silent);
}
