package nl.pim16aap2.bigDoors.moveBlocks.Cylindrical;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FallingBlock;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import nl.pim16aap2.bigDoors.BigDoors;

public class CylindricalNorth extends CylindricalMover implements CylindricalMovement {
	
	private BigDoors plugin;
	private World world;
	private String direction;
	private List<Entity> entity = new ArrayList<Entity>();
	private int xMin, yMin, zMin, xMax, yMax, zMax, xLen, yLen, zLen, qCircles;
	
	@Override
	public void moveBlockCylindrically(BigDoors plugin, World world, int qCircles, String direction, int xMin, int yMin, int zMin, int xMax, int yMax, int zMax, int xLen, int yLen, int zLen) {
		this.direction = direction;
		this.qCircles = qCircles;
		this.plugin = plugin;
		this.world = world;
		this.xMin = xMin;
		this.yMin = yMin;
		this.zMin = zMin;
		this.xMax = xMax;
		this.yMax = yMax;
		this.zMax = zMax;
		this.xLen = xLen;
		this.yLen = yLen;
		this.zLen = zLen;		
		
		int index = 0;
		
		for (double yAxis = yMin ; yAxis <= yMax ; yAxis++) 
		{
			for (double xAxis = xMin ; xAxis <= xMax ; xAxis++) 
			{
				for (double zAxis = zMin ; zAxis <= zMax ; zAxis++) 
				{
					Location newStandLocation = new Location(world, xAxis+0.5, yAxis-0.7, zAxis+0.5);
					
					Material item = world.getBlockAt((int)xAxis, (int)yAxis, (int)zAxis).getType();
					world.getBlockAt((int)xAxis, (int)yAxis, (int)zAxis).setType(Material.AIR);
					
					ArmorStand lastStand = world.spawn(newStandLocation, ArmorStand.class);	
					lastStand.setVelocity(new Vector(0, 0.000, 0));
					lastStand.setGravity(false);
					lastStand.setCollidable(false);
					lastStand.setVisible(false);
					lastStand.setSmall(true);
					
					@SuppressWarnings("deprecation")
					FallingBlock block = world.spawnFallingBlock(newStandLocation, item, (byte) 0);
					block.setVelocity(new Vector(0, 0, 0));
					block.setDropItem(false);
					block.setGravity(false);
					lastStand.addPassenger(block);
					
					entity.add(index, lastStand);
					index++;
				}
			}
		}
		rotateEntities();
	}

	// Put the door blocks back, but change their state now.
	public void putBlocks()
	{
		int index = 0;
		for (double yAxis = yMin ; yAxis <= yMax ; yAxis++) 
		{
			for (double xAxis = xMin ; xAxis <= xMax ; xAxis++) 
			{
				for (double zAxis = zMin ; zAxis <= zMax ; zAxis++)
				{
					// If the entity has any passengers.
					if (entity.get(index).getPassengers().size() > 0)
					{
						Entity fallingBlock = entity.get(index).getPassengers().get(0);
						if (fallingBlock instanceof FallingBlock)
						{
							Material mat = ((FallingBlock) fallingBlock).getMaterial();
							Location oldPos = new Location(fallingBlock.getWorld(), xAxis, yAxis, zAxis);
							Location newPos = oldPos;
							
							int radius = zLen-index%zLen-1;
							
							newPos.setX(oldPos.getX() + (direction == "clockwise" ? radius : -radius));
							newPos.setZ(zMax);
							newPos.setY(oldPos.getY());
							
							world.getBlockAt(newPos).setType(mat);
						}
						entity.get(index).getPassengers().get(0).remove();
					}
					entity.get(index).remove();
					index++;
				}
			}
		}
		// Empty the entity arrayList.
		entity.clear();
	}
	
	
	@Override
	public void rotateEntities() {
		
		int directionMultiplier = direction=="clockwise" ? 1 : -1;
		double divider = getDivider(zLen);
		double speed = 0.2;
		double radDiv = divider / speed;
		
		new BukkitRunnable()
        {
            double angle = 0.0D;
            final int ticksPerCircle = (int) (2800/speed); // How many ticks go into 1 circle (20 ticks = 1 second).
            final double step = ((2 * Math.PI) / ticksPerCircle);
            
            @Override
            public void run() 
            {
            		// If the angle equals or exceeds 1.5808 rad (90 degrees) multiplied by the amount of quarter circles it should turn, stop.
                if (Math.abs(angle) >= 1.5808 * qCircles)
                {
                		this.cancel();
                		putBlocks();
                }
                int index=0;
                
	        		// Loop up and down first.
                for (double yAxis = yMin ; yAxis <= yMax ; yAxis++) 
	        		{
                		for (double xAxis = xMin ; xAxis <= xMax ; xAxis++)  
	        			{
                			for (double zAxis = zMin ; zAxis <= zMax ; zAxis++) 
	        				{
                				angle += step;
	        					if (Math.abs(angle) <= 1.5808 * qCircles)
	        					{
		        					// Set the gravity stat of the armor stand to true, so it can move again.
	        						entity.get(index).setGravity(true);
		        					// Get the radius of the current blockEntity.
	        						double radius = zLen-index%zLen-1;
		        					// Set the x and z accelerations.
		        					double xRot = Math.cos(angle) * radius / radDiv;
		        					double zRot = Math.sin(angle) * radius / radDiv;
		        					entity.get(index).setVelocity(new Vector(directionMultiplier*xRot, 0.002, zRot));
		        					// If there's more than 0 passengers for the entity (ArmorStand), set its tickslived to 0, so the blocks on the AS's won't despawn.
		        					if (entity.get(index).getPassengers().size() > 0)
		        					{
		        						entity.get(index).getPassengers().get(0).setTicksLived(1);
		        					}
	        					}
	        					index++;
	        				}
	        			}
	        		}
            }
		}.runTaskTimer(plugin, 14, 1);
		
	}

}
