package nl.pim16aap2.bigDoors.moveBlocks;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import com.sk89q.worldedit.util.Direction;

import nl.pim16aap2.bigDoors.BigDoors;
import nl.pim16aap2.bigDoors.Door;
import nl.pim16aap2.bigDoors.moveBlocks.Cylindrical.CylindricalEast;
import nl.pim16aap2.bigDoors.moveBlocks.Cylindrical.CylindricalMovement;
import nl.pim16aap2.bigDoors.moveBlocks.Cylindrical.CylindricalNorth;
import nl.pim16aap2.bigDoors.moveBlocks.Cylindrical.CylindricalSouth;
import nl.pim16aap2.bigDoors.moveBlocks.Cylindrical.CylindricalWest;

public class BlockMover 
{
	private BigDoors plugin;
	private CylindricalMovement moveCylindrically;
	int xMin, yMin, zMin, xMax, yMax, zMax;
	private String direction; 
	private Direction currentDirection;
	
	public BlockMover(BigDoors plugin)
	{
		this.plugin = plugin;
	}
	
	public void moveBlocks(Location turningPoint, Location pointOpposite, String direction, Direction currentDirection)
	{
		World world = turningPoint.getWorld();
		
		this.direction = direction;
		this.currentDirection = currentDirection;
		
		this.xMin = turningPoint.getBlockX() < pointOpposite.getBlockX() ? turningPoint.getBlockX() : pointOpposite.getBlockX();
		this.yMin = turningPoint.getBlockY() < pointOpposite.getBlockY() ? turningPoint.getBlockY() : pointOpposite.getBlockY();
		this.zMin = turningPoint.getBlockZ() < pointOpposite.getBlockZ() ? turningPoint.getBlockZ() : pointOpposite.getBlockZ();
		this.xMax = turningPoint.getBlockX() > pointOpposite.getBlockX() ? turningPoint.getBlockX() : pointOpposite.getBlockX();
		this.yMax = turningPoint.getBlockY() > pointOpposite.getBlockY() ? turningPoint.getBlockY() : pointOpposite.getBlockY();
		this.zMax = turningPoint.getBlockZ() > pointOpposite.getBlockZ() ? turningPoint.getBlockZ() : pointOpposite.getBlockZ();
		
		int xLen = (int) (xMax-xMin)+1;
		int yLen = (int) (yMax-yMin)+1;
		int zLen = (int) (zMax-zMin)+1;
		
		Bukkit.broadcastMessage("Turning in "+direction+" direction.");
		
		if (currentDirection == Direction.NORTH) 
		{
			moveCylindrically = new CylindricalNorth();
			
		} else if (currentDirection == Direction.EAST)
		{
			moveCylindrically = new CylindricalEast();
			
		} else if (currentDirection == Direction.SOUTH)
		{
			moveCylindrically = new CylindricalSouth();
			
		} else if (currentDirection == Direction.WEST)
		{
			moveCylindrically = new CylindricalWest();
		}
		
		// Amount of quarter circles to turn, so 4 = 1 full circle.
		int qCircles = 1;
		
		moveCylindrically.moveBlockCylindrically(plugin, world, qCircles, direction, xMin, yMin, zMin, xMax, yMax, zMax, xLen, yLen, zLen);
	}
	
	public void simpleOpener()
	{
		
	}
}
