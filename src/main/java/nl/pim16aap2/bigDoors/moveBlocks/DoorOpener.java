package nl.pim16aap2.bigDoors.moveBlocks;

import nl.pim16aap2.bigDoors.Door;

public class DoorOpener 
{
	// Get the current angle of the door.
	public int getCurrentAngle(Door door)
	{
		int angle=0;
		int xTop = Math.max(Math.abs(door.getMaximum().getBlockX()), Math.abs(door.getMaximum().getBlockX()));
		int zTop = Math.max(Math.abs(door.getMaximum().getBlockZ()), Math.abs(door.getMaximum().getBlockZ()));
		int engineX = door.getEngine().getBlockX();
		int engineY = door.getEngine().getBlockZ();
//		if ()
		{
			
		}
			
		return angle;
	}
	
	// 
	public int getDirection(Door door)
	{
		int angle = 0;
		
		int currentAngle = getCurrentAngle(door);
		
		
		return angle;
	}
	
	// Open a door.
	public void openDoor(Door door) 
	{
		getDirection(door);
	}
}
