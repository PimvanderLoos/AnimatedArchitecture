package nl.pim16aap2.bigDoors.moveBlocks;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;

import com.sk89q.worldedit.util.Direction;

import nl.pim16aap2.bigDoors.BigDoors;
import nl.pim16aap2.bigDoors.Door;

public class DoorOpener 
{
	private BigDoors plugin;
	
	public DoorOpener(BigDoors plugin)
	{
		this.plugin = plugin;
	}
	
	// Get the current angle of the door.
	public int getCurrentAngle(Door door)
	{
		Integer angle=null;
		int xMax = Math.max(Math.abs(door.getMaximum().getBlockX()), Math.abs(door.getMaximum().getBlockX()));
		int zMax = Math.max(Math.abs(door.getMaximum().getBlockZ()), Math.abs(door.getMaximum().getBlockZ()));
		int xLen = door.getMaximum().getBlockX() - door.getMinimum().getBlockX();
		int zLen = door.getMaximum().getBlockZ() - door.getMinimum().getBlockZ();
		int engineX = door.getEngine().getBlockX();
		int engineZ = door.getEngine().getBlockZ();
		
		// If the door is one block deep in the x direction.
		if (xLen == 0)
		{
			// If the engine is at the highest z value, it is facing south. 
			if (engineZ ==  zMax)
			{
				angle = -180;
			// Otherwise it is facing South.
			} else 
			{
				angle = 0;
			}
		}
		// If the door is one block deep in the z direction.
		else if (zLen == 0)
		{
			// If the engine is at the highest x value, it is facing west. NOTE: West = negative X.
			if (engineX == xMax)
			{
				angle = 90;
			// Otherwise it is facing east.
			} else 
			{
				angle = -90;
			}
		}
		return angle;
	}
	
	// Get the direction that the door will turn in.
	public Integer getAngleChange(Door door)
	{
		Integer angle = null;
		
		int currentAngle = getCurrentAngle(door);

		int engineX = door.getEngine().getBlockX();
		int engineZ = door.getEngine().getBlockZ();
		int yMin = door.getMinimum().getBlockY();
		
		Bukkit.broadcastMessage("currentAngle = "+currentAngle+". Facing: ");
		switch(currentAngle)
		{
		case 0: 
			Bukkit.broadcastMessage("South.");
			// If the block north of the base of the engine is air, the door can swing that way.
			if (door.getWorld().getBlockAt(engineX+1, yMin, engineZ).getType()==Material.AIR)
			{
				angle = -90;
			// If the block south of the base of the engine is air, the door can swing that way.
			} else if (door.getWorld().getBlockAt(engineX-1, yMin, engineZ).getType()==Material.AIR)
			{
				angle = 90;
			}
			break;
		case 90: 
			Bukkit.broadcastMessage("West.");
			// If the block east of the base of the engine is air, the door can swing that way.
			if (door.getWorld().getBlockAt(engineX, yMin, engineZ+1).getType()==Material.AIR)
			{
				angle = 0;
			// If the block west of the base of the engine is air, the door can swing that way.
			} else if (door.getWorld().getBlockAt(engineX, yMin, engineZ-1).getType()==Material.AIR)
			{
				angle = -180;
			}
			break;
		case -90: 
			Bukkit.broadcastMessage("East.");
			// If the block north of the base of the engine is air, the door can swing that way.
			if (door.getWorld().getBlockAt(engineX, yMin, engineZ+1).getType()==Material.AIR)
			{
				angle = 0;
			// If the block south of the base of the engine is air, the door can swing that way.
			} else if (door.getWorld().getBlockAt(engineX, yMin, engineZ-1).getType()==Material.AIR)
			{
				angle = -180;
			}
			break;
		case -180: 
			Bukkit.broadcastMessage("North.");
			// If the block east of the base of the engine is air, the door can swing that way.
			if (door.getWorld().getBlockAt(engineX+1, yMin, engineZ).getType()==Material.AIR)
			{
				angle = -90;
			// If the block west of the base of the engine is air, the door can swing that way.
			} else if (door.getWorld().getBlockAt(engineX-1, yMin, engineZ).getType()==Material.AIR)
			{
				angle = 90;
			}
			break;
		}
		Bukkit.broadcastMessage("Angle="+angle);
		if (angle != null) {
			Integer deltaAngle = currentAngle - angle;
			Bukkit.broadcastMessage("deltaAngle="+deltaAngle);
			return deltaAngle;
		}
		return angle;
	}
	
	// Get the current direction.
	public Direction getCurrentDirection(Door door)
	{
		Direction direction = null;
		switch(getCurrentAngle(door))
		{
		case 0: 
			direction = Direction.SOUTH;
			break;
		case 90: 
			direction = Direction.WEST;
			break;
		case -90: 
			direction = Direction.EAST;
			break;
		case -180: 
			direction = Direction.NORTH;
			break;
		}
		return direction;
	}
	
	// Get the new direction.
	public Direction getNewDirection(Door door, Direction currentDirection)
	{
		
		int engineX = door.getEngine().getBlockX();
		int engineZ = door.getEngine().getBlockZ();
		int yMin = door.getMinimum().getBlockY();
		
		Direction newDirection = null;
		
		if (currentDirection == Direction.SOUTH) {
			// If the block north of the base of the engine is air, the door can swing that way.
			if (door.getWorld().getBlockAt(engineX+1, yMin, engineZ).getType()==Material.AIR)
			{
				newDirection = Direction.EAST;
			// If the block south of the base of the engine is air, the door can swing that way.
			} else if (door.getWorld().getBlockAt(engineX-1, yMin, engineZ).getType()==Material.AIR)
			{
				newDirection = Direction.WEST;
			}
			
		} else if (currentDirection == Direction.WEST) {
			// If the block north of the base of the engine is air, the door can swing that way.
			if (door.getWorld().getBlockAt(engineX, yMin, engineZ+1).getType()==Material.AIR)
			{
				newDirection = Direction.SOUTH;
			// If the block west of the base of the engine is air, the door can swing that way.
			} else if (door.getWorld().getBlockAt(engineX, yMin, engineZ-1).getType()==Material.AIR)
			{
				newDirection = Direction.NORTH;
			}
			
		} else if (currentDirection == Direction.EAST) {
			// If the block north of the base of the engine is air, the door can swing that way.
			if (door.getWorld().getBlockAt(engineX, yMin, engineZ+1).getType()==Material.AIR)
			{
				newDirection = Direction.SOUTH;
			// If the block south of the base of the engine is air, the door can swing that way.
			} else if (door.getWorld().getBlockAt(engineX, yMin, engineZ-1).getType()==Material.AIR)
			{
				newDirection = Direction.NORTH;
			}
			
		} else if (currentDirection == Direction.NORTH) {
			// If the block north of the base of the engine is air, the door can swing that way.
			if (door.getWorld().getBlockAt(engineX+1, yMin, engineZ).getType()==Material.AIR)
			{
				newDirection = Direction.EAST;
			// If the block west of the base of the engine is air, the door can swing that way.
			} else if (door.getWorld().getBlockAt(engineX-1, yMin, engineZ).getType()==Material.AIR)
			{
				newDirection = Direction.WEST;
			}
		}
		return newDirection;
	}
	
	// Get the direction the door should turn in.
	public String getTurnDirection(Direction currentDirection, Direction newDirection)
	{
		String turnDirection = null;
		if (currentDirection == Direction.NORTH)
		{
			if (newDirection == Direction.EAST)
			{
				turnDirection = "clockwise";
			} else if (newDirection == Direction.WEST) {
				turnDirection = "counterclockwise";
			}
		} else if (currentDirection == Direction.EAST)
		{
			if (newDirection == Direction.SOUTH)
			{
				turnDirection = "clockwise";
			} else if (newDirection == Direction.NORTH) {
				turnDirection = "counterclockwise";
			}
		}else if (currentDirection == Direction.SOUTH)
		{
			if (newDirection == Direction.WEST)
			{
				turnDirection = "clockwise";
			} else if (newDirection == Direction.EAST) {
				turnDirection = "counterclockwise";
			}
		}else if (currentDirection == Direction.WEST)
		{
			if (newDirection == Direction.NORTH)
			{
				turnDirection = "clockwise";
			} else if (newDirection == Direction.SOUTH) {
				turnDirection = "counterclockwise";
			}
		}
		return turnDirection;
	}
	
	// Open a door.
	public boolean openDoor(Door door) 
	{
		Direction currentDirection = getCurrentDirection(door);
		Direction newDirection = getNewDirection(door, currentDirection);
		
		Bukkit.broadcastMessage("CurrentDirection: "+currentDirection+".\nNewDirection: "+newDirection);
		
		Integer angle = getAngleChange(door);
		if (angle != null)
		{
			BlockMover blockMover = new BlockMover(plugin);
			int xOpposite, yOpposite, zOpposite;
			// If the xMax is not the same value as the engineX, then xMax is xOpposite.
			if (door.getMaximum().getBlockX() != door.getEngine().getBlockX())
			{
				xOpposite = door.getMaximum().getBlockX();
			} else 
			{
				xOpposite = door.getMinimum().getBlockX();
			}
			// If the zMax is not the same value as the engineZ, then zMax is zOpposite.
			if (door.getMaximum().getBlockZ() != door.getEngine().getBlockZ())
			{
				zOpposite = door.getMaximum().getBlockZ();
			} else 
			{
				zOpposite = door.getMinimum().getBlockZ();
			}
			// If the yMax is not the same value as the engineY, then yMax is yOpposite.
			if (door.getMaximum().getBlockY() != door.getEngine().getBlockY())
			{
				yOpposite = door.getMaximum().getBlockY();
			} else 
			{
				yOpposite = door.getMinimum().getBlockY();
			}
			
			Location oppositePoint = new Location(door.getWorld(), xOpposite, yOpposite, zOpposite);
			
			String turnDirection = getTurnDirection(currentDirection, newDirection);
			blockMover.moveBlocks(door.getEngine(), oppositePoint, turnDirection, currentDirection);
			
			return true;
		} 
		return false;
	}
}
