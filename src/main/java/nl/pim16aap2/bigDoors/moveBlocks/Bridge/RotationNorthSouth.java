package nl.pim16aap2.bigDoors.moveBlocks.Bridge;

import org.bukkit.World;

import nl.pim16aap2.bigDoors.util.RotateDirection;

public class RotationNorthSouth implements RotationFormulae
{
	@SuppressWarnings("unused")
	private int xMin, xMax, zMin, zMax;
	private RotateDirection     rotDir;
	private World                world;
	
	public RotationNorthSouth(World world, int xMin, int xMax, int zMin, int zMax, RotateDirection rotDir)
	{
		this.rotDir = rotDir;
		this.world  = world;
		this.xMin   = xMin;
		this.xMax   = xMax;
		this.zMin   = zMin;
		this.zMax   = zMax;
	}

	public RotationNorthSouth()
	{}
}
