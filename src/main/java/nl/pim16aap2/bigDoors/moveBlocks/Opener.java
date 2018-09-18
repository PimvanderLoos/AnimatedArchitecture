package nl.pim16aap2.bigDoors.moveBlocks;

import nl.pim16aap2.bigDoors.Door;
import nl.pim16aap2.bigDoors.util.DoorDirection;

public interface Opener
{
	public DoorDirection getCurrentDirection(Door door);
	public boolean       chunksLoaded       (Door door);
	public int           getDoorSize        (Door door);
	public boolean       openDoor           (Door door, double time);
	public boolean       openDoor           (Door door, double time, boolean instantOpen, boolean silent);
}
