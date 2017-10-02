package nl.pim16aap2.bigDoors.moveBlocks.Cylindrical;

import org.bukkit.World;

import nl.pim16aap2.bigDoors.BigDoors;

public interface CylindricalMovement
{
	public void moveBlockCylindrically(BigDoors plugin, World world, int qCircles, String direction, double speed,
			int xMin, int yMin, int zMin, int xMax, int yMax, int zMax, int xLen, int yLen, int zLen);

	public void rotateEntities();
}
