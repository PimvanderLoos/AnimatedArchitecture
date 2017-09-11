package nl.pim16aap2.bigDoors.moveBlocks;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FallingBlock;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.util.Vector;

import com.sk89q.worldedit.util.Direction;

import nl.pim16aap2.bigDoors.BigDoors;

public class BlockMover 
{
	private BigDoors plugin;
	private List<Entity> entity = new ArrayList<Entity>();
	private String direction;
	private Direction currentDirection;
	
	public BlockMover(BigDoors plugin)
	{
		this.plugin = plugin;
	}
	
	@SuppressWarnings("deprecation")
	public void moveBlocks(Location turningPoint, Location pointOpposite, String direction, Direction currentDirection)
	{
		this.direction = direction;
		this.currentDirection = currentDirection;
		World world = turningPoint.getWorld();
		double xMin, yMin, zMin, xMax, yMax, zMax;

		xMin = turningPoint.getBlockX() < pointOpposite.getBlockX() ? turningPoint.getBlockX() : pointOpposite.getBlockX();
		yMin = turningPoint.getBlockY() < pointOpposite.getBlockY() ? turningPoint.getBlockY() : pointOpposite.getBlockY();
		zMin = turningPoint.getBlockZ() < pointOpposite.getBlockZ() ? turningPoint.getBlockZ() : pointOpposite.getBlockZ();
		xMax = turningPoint.getBlockX() > pointOpposite.getBlockX() ? turningPoint.getBlockX() : pointOpposite.getBlockX();
		yMax = turningPoint.getBlockY() > pointOpposite.getBlockY() ? turningPoint.getBlockY() : pointOpposite.getBlockY();
		zMax = turningPoint.getBlockZ() > pointOpposite.getBlockZ() ? turningPoint.getBlockZ() : pointOpposite.getBlockZ();
		
//		xMin = pointMin.getX();
//		yMin = pointMin.getY();
//		zMin = pointMin.getZ();
//		xMax = pointMax.getX();
//		yMax = pointMax.getY();
//		zMax = pointMax.getZ();
		int xLen = (int) (xMax-xMin)+1;
		int yLen = (int) (yMax-yMin)+1;
		int zLen = (int) (zMax-zMin)+1;
		
		Bukkit.broadcastMessage("Turning in "+direction+" direction.");
		Bukkit.broadcastMessage("xMin:"+xMin+", xMax:"+xMax+", yMin:"+yMin+", yMax:"+yMax+", zMin:"+zMin+", zMax:"+zMax);
		
		int index=0;

		// Loop up and down first.
		for (double yAxis = yMin ; yAxis <= yMax ; yAxis++) 
		{
			for (double xAxis = xMin ; xAxis <= xMax ; xAxis++) 
			{
				for (double zAxis = zMin ; zAxis <= zMax ; zAxis++) 
				{
					Location newStandLocation = new Location(world, xAxis+0.5, yAxis+6, zAxis+0.5);
					
					Material item = world.getBlockAt((int)xAxis, (int)yAxis, (int)zAxis).getType();
//					Bukkit.broadcastMessage("Item="+item);
//					item = Material.GOLD_BLOCK;
					
					ArmorStand lastStand = world.spawn(newStandLocation, ArmorStand.class);	
					lastStand.setVelocity(new Vector(0, 0.002, 0));
					lastStand.setCollidable(false);
					lastStand.setVisible(false);
					
					FallingBlock block = world.spawnFallingBlock(newStandLocation, item, (byte) 0);
					block.setVelocity(new Vector(0, 0, 0));
					block.setDropItem(false);
					lastStand.addPassenger(block);
					
					entity.add(lastStand);
					entity.add(index, lastStand);
					index++;
				}
			}
		}
		
//		int finalCount = 27; // quarter round
		int finalCount = 1600000000;
		
		int directionMultiplier = direction=="clockwise" ? 1 : -1;
		
        BukkitScheduler scheduler = plugin.getServer().getScheduler();
        scheduler.scheduleSyncRepeatingTask(plugin, new Runnable() 
        {
        		int count = 0;
            double angle = 0.0D;        
            final int ticksPerCircle = 2800; // How many ticks go into 1 cirlce (20 ticks = 1 second).
            final double step = ((2 * Math.PI) / ticksPerCircle);
            
            @Override
            public void run() 
            {
                if (count > finalCount)
                {
                		return;
                }
                int index=0;
	        		// Loop up and down first.
                for (double yAxis = yMin ; yAxis <= yMax ; yAxis++) 
	        		{
                		for (double xAxis = xMin ; xAxis <= xMax ; xAxis++)  
	        			{
                			for (double zAxis = zMin ; zAxis <= zMax ; zAxis++) 
	        				{	
	        					if (count < finalCount)
	        					{
		        					double radius = zLen-index%zLen-1;
		        					double xRot = Math.cos(angle) * radius/17;
		        					double zRot = Math.sin(angle) * radius/17;
//		        					double xRot = Math.cos(angle) * radius/14;
//		        					double zRot = Math.sin(angle) * radius/14;
//		        					xRot=0D;
//		        					zRot=0D;
		        					angle += step;
		        					entity.get(index).setVelocity(new Vector(directionMultiplier*xRot, 0.002, zRot));
	        					}  else
	        					{
		        					entity.get(index).setVelocity(new Vector(0D, 0.002, 0D));
		        					if (entity.get(index).getPassengers().size()>0)
		        					{
		        						entity.get(index).getPassengers().get(0).remove();
		        					}
		        					entity.get(index).remove();
	        					}
	        					index++;
	        				}
	        			}
	        		}
                count++;
            }
        }, 0L, 1L);
	}
}
