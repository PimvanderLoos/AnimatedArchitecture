package nl.pim16aap2.bigDoors.moveBlocks;

import nl.pim16aap2.bigDoors.Door;
import nl.pim16aap2.bigDoors.util.DoorDirection;
import nl.pim16aap2.bigDoors.util.RotateDirection;

public interface Opener
{
	public DoorDirection getCurrentDirection(Door door);
	public boolean       chunksLoaded       (Door door);
	public int           getDoorSize        (Door door);
	public boolean       openDoor           (Door door, double speed);
	public boolean       openDoor           (Door door, double speed, boolean instantOpen, boolean silent);
	public void          updateCoords       (Door door, DoorDirection currentDirection, RotateDirection rotDirection, int moved);
	public void          toggleOpen         (Door door);
}
