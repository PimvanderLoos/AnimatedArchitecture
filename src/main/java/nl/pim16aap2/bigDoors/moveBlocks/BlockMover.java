package nl.pim16aap2.bigDoors.moveBlocks;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import nl.pim16aap2.bigDoors.BigDoors;
import nl.pim16aap2.bigDoors.moveBlocks.Cylindrical.CylindricalEast;
import nl.pim16aap2.bigDoors.moveBlocks.Cylindrical.CylindricalMovement;
import nl.pim16aap2.bigDoors.moveBlocks.Cylindrical.CylindricalNorth;
import nl.pim16aap2.bigDoors.moveBlocks.Cylindrical.CylindricalSouth;
import nl.pim16aap2.bigDoors.moveBlocks.Cylindrical.CylindricalWest;
import nl.pim16aap2.bigDoors.util.DoorDirection;
import nl.pim16aap2.bigDoors.util.RotateDirection;

public class BlockMover
{
	private BigDoors plugin;
	private CylindricalMovement moveCylindrically;
	int xMin, yMin, zMin, xMax, yMax, zMax;
	@SuppressWarnings("unused")
	private RotateDirection rotDirection;
	@SuppressWarnings("unused")
	private DoorDirection currentDirection;

	private double speed;

	public BlockMover(BigDoors plugin, double speed)
	{
		this.plugin = plugin;
		this.speed = speed;
	}

	public void moveBlocks(Location turningPoint, Location pointOpposite, RotateDirection rotDirection, DoorDirection currentDirection)
	{
		World world = turningPoint.getWorld();

		this.rotDirection = rotDirection;
		this.currentDirection = currentDirection;

		this.xMin = turningPoint.getBlockX() < pointOpposite.getBlockX() ? turningPoint.getBlockX() : pointOpposite.getBlockX();
		this.yMin = turningPoint.getBlockY() < pointOpposite.getBlockY() ? turningPoint.getBlockY() : pointOpposite.getBlockY();
		this.zMin = turningPoint.getBlockZ() < pointOpposite.getBlockZ() ? turningPoint.getBlockZ() : pointOpposite.getBlockZ();
		this.xMax = turningPoint.getBlockX() > pointOpposite.getBlockX() ? turningPoint.getBlockX() : pointOpposite.getBlockX();
		this.yMax = turningPoint.getBlockY() > pointOpposite.getBlockY() ? turningPoint.getBlockY() : pointOpposite.getBlockY();
		this.zMax = turningPoint.getBlockZ() > pointOpposite.getBlockZ() ? turningPoint.getBlockZ() : pointOpposite.getBlockZ();

		int xLen = (int) (xMax - xMin) + 1;
		int yLen = (int) (yMax - yMin) + 1;
		int zLen = (int) (zMax - zMin) + 1;

		Bukkit.broadcastMessage("Turning in " + rotDirection + " direction.");

		switch(currentDirection)
		{
		case NORTH:
			moveCylindrically = new CylindricalNorth();
			break;
		case EAST:
			moveCylindrically = new CylindricalEast();
			break;
		case SOUTH:
			moveCylindrically = new CylindricalSouth();
			break;
		case WEST:
			moveCylindrically = new CylindricalWest();
			break;
		}

		// Amount of quarter circles to turn, so 4 = 1 full circle.
		int qCircles = 1;

		moveCylindrically.moveBlockCylindrically(plugin, world, qCircles, rotDirection, speed, xMin, yMin, zMin, xMax,
				yMax, zMax, xLen, yLen, zLen);
	}

	public void simpleOpener()
	{

	}
}
