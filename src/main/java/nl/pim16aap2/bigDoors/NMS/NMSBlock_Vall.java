package nl.pim16aap2.bigDoors.NMS;

import org.bukkit.Location;

import nl.pim16aap2.bigDoors.util.RotateDirection;

public interface NMSBlock_Vall
{
	public void   rotateBlock(RotateDirection rotDir);
	public void   putBlock(Location loc);
	public void   rotateBlockUpDown(boolean NS);
	public void   rotateCylindrical(RotateDirection rotDir);
	public String toString();
}
