package nl.pim16aap2.bigDoors.moveBlocks;

//import java.util.ArrayList;
//import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Location;
//import org.bukkit.Material;
import org.bukkit.World;
//import org.bukkit.entity.ArmorStand;
//import org.bukkit.entity.Entity;
//import org.bukkit.entity.FallingBlock;
//import org.bukkit.scheduler.BukkitRunnable;
//import org.bukkit.util.Vector;

import com.sk89q.worldedit.util.Direction;

import nl.pim16aap2.bigDoors.BigDoors;
import nl.pim16aap2.bigDoors.Door;
import nl.pim16aap2.bigDoors.moveBlocks.Cylindrical.CylindricalEast;
import nl.pim16aap2.bigDoors.moveBlocks.Cylindrical.CylindricalMovement;
import nl.pim16aap2.bigDoors.moveBlocks.Cylindrical.CylindricalNorth;
import nl.pim16aap2.bigDoors.moveBlocks.Cylindrical.CylindricalSouth;
import nl.pim16aap2.bigDoors.moveBlocks.Cylindrical.CylindricalSouth;
import nl.pim16aap2.bigDoors.moveBlocks.Cylindrical.CylindricalWest;

public class BlockMover 
{
	private BigDoors plugin;
//	private List<Entity> entity = new ArrayList<Entity>();
	private CylindricalMovement moveCylindrically;
	int xMin, yMin, zMin, xMax, yMax, zMax;
	private String direction; 
	private Direction currentDirection;
	
	public BlockMover(BigDoors plugin)
	{
		this.plugin = plugin;
	}
	
//	@SuppressWarnings("deprecation")
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
		Bukkit.broadcastMessage("xMin:"+xMin+", xMax:"+xMax+", yMin:"+yMin+", yMax:"+yMax+", zMin:"+zMin+", zMax:"+zMax);
		
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
		
		Bukkit.broadcastMessage("qCircles = "+qCircles);
		
		moveCylindrically.moveBlockCylindrically(plugin, world, qCircles, direction, xMin, yMin, zMin, xMax, yMax, zMax, xLen, yLen, zLen);
	}
	
	public void simpleOpener()
	{
		
	}
}


//		int index=0;
//
//		// Loop up and down first.
//		for (double yAxis = yMin ; yAxis <= yMax ; yAxis++) 
//		{
//			if (currentDirection == Direction.NORTH) 
//			{
//				for (double xAxis = xMin ; xAxis <= xMax ; xAxis++) 
//				{
//					for (double zAxis = zMin ; zAxis <= zMax ; zAxis++) 
//					{
//						Location newStandLocation = new Location(world, xAxis+0.5, yAxis+6, zAxis+0.5);
//						
//						Material item = world.getBlockAt((int)xAxis, (int)yAxis, (int)zAxis).getType();
//						
//						ArmorStand lastStand = world.spawn(newStandLocation, ArmorStand.class);	
//						lastStand.setVelocity(new Vector(0, 0.002, 0));
//						lastStand.setCollidable(false);
//						lastStand.setVisible(false);
//						
//						FallingBlock block = world.spawnFallingBlock(newStandLocation, item, (byte) 0);
//						block.setVelocity(new Vector(0, 0, 0));
//						block.setDropItem(false);
//						lastStand.addPassenger(block);
//						
//						entity.add(lastStand);
//						entity.add(index, lastStand);
//						index++;
//					}
//				}
//			} else if (currentDirection == Direction.SOUTH)
//	        	{
//				for (double xAxis = xMin ; xAxis <= xMax ; xAxis++) 
//				{
//					for (double zAxis = zMax ; zAxis >= zMin ; zAxis--) 
//					{
//						Location newStandLocation = new Location(world, xAxis+0.5, yAxis+6, zAxis+0.5);
//						
//						Material item = world.getBlockAt((int)xAxis, (int)yAxis, (int)zAxis).getType();
//						
//						ArmorStand lastStand = world.spawn(newStandLocation, ArmorStand.class);	
//						lastStand.setVelocity(new Vector(0, 0.002, 0));
//						lastStand.setCollidable(false);
//						lastStand.setVisible(false);
//						
//						FallingBlock block = world.spawnFallingBlock(newStandLocation, item, (byte) 0);
//						block.setVelocity(new Vector(0, 0, 0));
//						block.setDropItem(false);
//						lastStand.addPassenger(block);
//						
//						entity.add(lastStand);
//						entity.add(index, lastStand);
//						index++;
//					}
//				}
//	        	} else if (currentDirection == Direction.EAST)
//	        	{
//        			for (double xAxis = xMax ; xAxis >= xMin ; xAxis--) 
//				{
//					for (double zAxis = zMin ; zAxis <= zMax ; zAxis++) 
//					{
//						Location newStandLocation = new Location(world, xAxis+0.5, yAxis+6, zAxis+0.5);
//						
//						Material item = world.getBlockAt((int)xAxis, (int)yAxis, (int)zAxis).getType();
//						
//						ArmorStand lastStand = world.spawn(newStandLocation, ArmorStand.class);	
//						lastStand.setVelocity(new Vector(0, 0.002, 0));
//						lastStand.setCollidable(false);
//						lastStand.setVisible(false);
//						
//						FallingBlock block = world.spawnFallingBlock(newStandLocation, item, (byte) 0);
//						block.setVelocity(new Vector(0, 0, 0));
//						block.setDropItem(false);
//						lastStand.addPassenger(block);
//						
//						entity.add(lastStand);
//						entity.add(index, lastStand);
//						index++;
//					}
//				}
//	        	} else if (currentDirection == Direction.WEST)
//	        	{
//        			for (double xAxis = xMin ; xAxis <= xMax ; xAxis++) 
//				{
//					for (double zAxis = zMin ; zAxis <= zMax ; zAxis++)
//					{
//						Location newStandLocation = new Location(world, xAxis+0.5, yAxis+6, zAxis+0.5);
//						
//						Material item = world.getBlockAt((int)xAxis, (int)yAxis, (int)zAxis).getType();
//						
//						ArmorStand lastStand = world.spawn(newStandLocation, ArmorStand.class);	
//						lastStand.setVelocity(new Vector(0, 0.002, 0));
//						lastStand.setCollidable(false);
//						lastStand.setVisible(false);
//						
//						FallingBlock block = world.spawnFallingBlock(newStandLocation, item, (byte) 0);
//						block.setVelocity(new Vector(0, 0, 0));
//						block.setDropItem(false);
//						lastStand.addPassenger(block);
//						
//						entity.add(lastStand);
//						entity.add(index, lastStand);
//						index++;
//					}
//				}
//	        	} else {
//	        		Bukkit.broadcastMessage("Invalid current direction!");
//	        		return;
//	        	}
//		}
//		
////		int finalCount = 27; // quarter round
//		int finalCount = 600; // The falling blocks will disappear after 600 ticks
//		
//		int directionMultiplier = direction=="clockwise" ? 1 : -1;
//		
////        BukkitScheduler scheduler = plugin.getServer().getScheduler();
////        scheduler.scheduleSyncRepeatingTask(plugin, new BukkitRunnable() 
//		new BukkitRunnable()
//        {
//        		int count = 0;
//            double angle = 0.0D;
//            final int ticksPerCircle = 2800; // How many ticks go into 1 cirlce (20 ticks = 1 second).
//            final double step = ((2 * Math.PI) / ticksPerCircle);
//            
//            @Override
//            public void run() 
//            {
//                if (count > finalCount)
//                {
//                		cancel();
//                }
//                int index=0;
//	        		// Loop up and down first.
//                for (double yAxis = yMin ; yAxis <= yMax ; yAxis++) 
//	        		{
////					if (count%26==0) {
////    					// Only loop this once every number of cycli.
////						Bukkit.broadcastMessage("Angle = "+angle+", count = "+count);
////					}
//	                	if (currentDirection == Direction.NORTH) 
//	                	{
//	                		for (double xAxis = xMin ; xAxis <= xMax ; xAxis++)  
//		        			{
//	                			for (double zAxis = zMin ; zAxis <= zMax ; zAxis++) 
//		        				{	
//		        					if (count < finalCount)
//		        					{
//			        					double radius = zLen-index%zLen-1;
//			        					double xRot = Math.cos(angle) * radius/17;
//			        					double zRot = Math.sin(angle) * radius/17;
//			        					angle += step;
//		        						entity.get(index).setVelocity(new Vector(directionMultiplier*xRot, 0.002, zRot));
//		        					}  else
//		        					{
//			        					entity.get(index).setVelocity(new Vector(0D, 0.002, 0D));
//			        					if (entity.get(index).getPassengers().size()>0)
//			        					{
//			        						entity.get(index).getPassengers().get(0).remove();
//			        					}
//			        					entity.get(index).remove();
//		        					}
//		        					index++;
//		        				}
//		        			}
//	                	} else if (currentDirection == Direction.SOUTH)
//	                	{
//	                		for (double xAxis = xMin ; xAxis <= xMax ; xAxis++)  
//		        			{
//                				for (double zAxis = zMax ; zAxis >= zMin ; zAxis--) 
//                				{	
//		        					if (count < finalCount)
//		        					{
//			        					double radius = zLen-index%zLen-1;
//			        					double xRot = Math.cos(angle) * radius/17;
//			        					double zRot = Math.sin(angle) * radius/17;
//			        					angle -= step;
//			        					entity.get(index).setVelocity(new Vector(-directionMultiplier*xRot, 0.002, zRot));
//		        					}  else
//		        					{
//			        					entity.get(index).setVelocity(new Vector(0D, 0.002, 0D));
//			        					if (entity.get(index).getPassengers().size()>0)
//			        					{
//			        						entity.get(index).getPassengers().get(0).remove();
//			        					}
//			        					entity.get(index).remove();
//		        					}
//		        					index++;
//		        				}
//		        			}
//	                	} else if (currentDirection == Direction.EAST)
//	                	{
////	                		for (double xAxis = xMin ; xAxis <= xMax ; xAxis++)  
////		        			{
////	                			for (double zAxis = zMin ; zAxis <= zMax ; zAxis++) 
//	                		for (double zAxis = zMin ; zAxis <= zMax ; zAxis++)
//	                		{
//	                			for (double xAxis = xMin ; xAxis <= xMax ; xAxis++)
//	                			{	
//		        					if (count < finalCount)
//		        					{
//			        					double radius = xLen-index%xLen-1;
//			        					double xRot = 0;
//			        					double zRot = 0;
////			        					angle += step;
//			        					if (direction == "clockwise") 
//			        					{
//				        					xRot = Math.sin(angle) * radius/17 * Math.sin(1.5708);
//				        					zRot = Math.cos(angle) * radius/17;
//			        						angle -= step;
//			        					} else if (direction == "counterclockwise") 
//			        					{
//				        					xRot = Math.sin(angle) * radius/17 * Math.sin(1.5708);
//				        					zRot = -Math.cos(angle) * radius/17;
//			        						angle += step;
//			        					}
//			        					entity.get(index).setVelocity(new Vector(directionMultiplier*xRot, 0.002, zRot));
//		        					}  else
//		        					{
//			        					entity.get(index).setVelocity(new Vector(0D, 0.002, 0D));
//			        					if (entity.get(index).getPassengers().size()>0)
//			        					{
//			        						entity.get(index).getPassengers().get(0).remove();
//			        					}
//			        					entity.get(index).remove();
//		        					}
//		        					index++;
//		        				}
//		        			}
//	                	} else if (currentDirection == Direction.WEST)
//	                	{
////	                		for (double xAxis = xMin ; xAxis <= xMax ; xAxis++)   
////		        			{
////	                			for (double zAxis = zMin ; zAxis <= zMax ; zAxis++) 
//	                		for (double zAxis = zMin ; zAxis <= zMax ; zAxis++) 
//	                		{
//	                			for (double xAxis = xMax ; xAxis >= xMin ; xAxis--)
//		        				{	
//		        					if (count < finalCount)
//		        					{
//			        					double radius = xLen-index%xLen-1;
////			        					double xRot = -Math.sin(angle) * radius/17 * Math.sin(1.5708);
////			        					double zRot = Math.cos(angle) * radius/17;
////			        					angle -= step;
//////			        					angle = step*count; // ??????
//			        					double xRot = 0;
//			        					double zRot = 0;
////			        					angle += step;
//			        					if (direction == "clockwise") 
//			        					{
//                                       // directionMultipler = 1;
//				        					xRot = -Math.sin(angle) * radius/17 * Math.sin(1.5708);
//				        					zRot = -Math.cos(angle) * radius/17;
//			        						angle -= step;
//			        					} else if (direction == "counterclockwise") 
//			        					{
//                                       // directionMultipler = -1;
//				        					xRot = -Math.sin(angle) * radius/17 * Math.sin(1.5708);
//				        					zRot = Math.cos(angle) * radius/17;
//			        						angle += step;
//			        					}
//			        					entity.get(index).setVelocity(new Vector(directionMultiplier*xRot, 0.002, zRot));
//		        					}  else
//		        					{
//			        					entity.get(index).setVelocity(new Vector(0D, 0.002, 0D));
//			        					if (entity.get(index).getPassengers().size()>0)
//			        					{
//			        						entity.get(index).getPassengers().get(0).remove();
//			        					}
//			        					entity.get(index).remove();
//		        					}
//		        					index++;
//		        				}
//		        			}
//	                	} else {
//	                		Bukkit.broadcastMessage("Invalid current direction!");
//	                	}
//	        		}
//                count++;
//            }
////        }, 0L, 1L);
//		}.runTaskTimer(plugin, 1, 1);
//	}
//}
