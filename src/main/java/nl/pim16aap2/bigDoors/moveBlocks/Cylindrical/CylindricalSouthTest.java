package nl.pim16aap2.bigDoors.moveBlocks.Cylindrical;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FallingBlock;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import nl.pim16aap2.bigDoors.BigDoors;
import nl.pim16aap2.bigDoors.customEntities.NoClipArmorStand;

public class CylindricalSouthTest extends CylindricalMover implements CylindricalMovement {
	
	private BigDoors plugin;
	private World world;
	private String direction;
//	private List<NoClipArmorStand> entity = new ArrayList<NoClipArmorStand>();
	private List<nl.pim16aap2.bigDoors.customEntities.CraftArmorStand> entity = new ArrayList<nl.pim16aap2.bigDoors.customEntities.CraftArmorStand>();
	private List<Material> blocks = new ArrayList<Material>();
	private List<FallingBlock> fBlocks = new ArrayList<FallingBlock>();
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
				for (double zAxis = zMax ; zAxis >= zMin ; zAxis--) 
				{
//					Location newStandLocation = new Location(world, xAxis+0.5, yAxis-1.48, zAxis+0.5);
					Location newStandLocation = new Location(world, xAxis+0.5, yAxis-0.7, zAxis+0.5);
					
					Material item = world.getBlockAt((int)xAxis, (int)yAxis, (int)zAxis).getType();
					blocks.add(index, item);
					
					world.getBlockAt((int)xAxis, (int)yAxis, (int)zAxis).setType(Material.AIR);
					
					NoClipArmorStand noClipArmorStandTemp = new NoClipArmorStand((org.bukkit.craftbukkit.v1_11_R1.CraftWorld)newStandLocation.getWorld(), newStandLocation);
					((org.bukkit.craftbukkit.v1_11_R1.CraftWorld)newStandLocation.getWorld()).getHandle().addEntity(noClipArmorStandTemp, SpawnReason.CUSTOM);

					noClipArmorStandTemp.setInvisible(true);
					noClipArmorStandTemp.setSmall(true);
					
					nl.pim16aap2.bigDoors.customEntities.CraftArmorStand noClipArmorStand = new nl.pim16aap2.bigDoors.customEntities.CraftArmorStand((org.bukkit.craftbukkit.v1_11_R1.CraftServer) (Bukkit.getServer()), noClipArmorStandTemp);
					
					noClipArmorStand.setVelocity(new Vector(0, 1.002, 0));
					noClipArmorStand.setGravity(false);
					noClipArmorStand.setCollidable(false);
//					//lastStand.setBodyPose(EulerAngle pose); !!!!
					
					@SuppressWarnings("deprecation")
					FallingBlock fBlock = world.spawnFallingBlock(newStandLocation, item, (byte) 0);
					fBlock.setVelocity(new Vector(0, 0, 0));
					fBlock.setDropItem(false);
					fBlock.setGravity(false);	
					
					noClipArmorStand.addPassenger(fBlock);	
					entity.add(index, noClipArmorStand);
					fBlocks.add(fBlock);
					
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
				for (double zAxis = zMax ; zAxis >= zMin ; zAxis--) 
				{
//					// If the entity has any passengers.
//					if (entity.get(index).getPassengers().size() > 0)
//					{
//						Entity fallingBlock = entity.get(index).getPassengers().get(0);
//						if (fallingBlock instanceof FallingBlock)
//						{
//							Material mat = ((FallingBlock) fallingBlock).getMaterial();
//							Location oldPos = new Location(fallingBlock.getWorld(), xAxis, yAxis, zAxis);
//							Location newPos = oldPos;
//							
//							int radius = zLen-index%zLen-1;
//							
//							newPos.setX(oldPos.getX() + (direction == "clockwise" ? -radius : radius));
//							newPos.setZ(zMin);
//							newPos.setY(oldPos.getY());
//							
//							world.getBlockAt(newPos).setType(mat);
//						}
//						entity.get(index).getPassengers().clear();
//					} else {
//						Bukkit.broadcastMessage("No passengers!");
//					}
//					entity.get(index).remove();
					
					Material mat = blocks.get(index);
					Location oldPos = new Location(world, xAxis, yAxis, zAxis);
					Location newPos = oldPos;
					
					int radius = zLen-index%zLen-1;
					
					newPos.setX(oldPos.getX() + (direction == "clockwise" ? -radius : radius));
					newPos.setZ(zMin);
					newPos.setY(oldPos.getY());
					
					world.getBlockAt(newPos).setType(mat);
					
					fBlocks.get(index).remove();
					
//					entity.get(index).getPassengers().clear();
//					entity.remove(index);
					index++;
				}
			}
		}
		// Empty the entity arrayList.
		entity.clear();
	}

	@Override
	public void rotateEntities() {
		
		int directionMultiplier = direction=="clockwise" ? -1 : 1;
		double divider = getDivider(zLen);
		double speed = 0.2;
		double radDiv = divider / speed;
//		qCircles *= 20;
		
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
	        				for (double zAxis = zMax ; zAxis >= zMin ; zAxis--) 
	        				{
	        					angle -= step;
	        					if (Math.abs(angle) <= 1.5808 * qCircles)
	        					{
		        					// Set the gravity stat of the armor stand to true, so it can move again.
//	        						entity.get(index).setNoGravity(false);
	        						entity.get(index).setGravity(true);
		        					// Get the radius of the current blockEntity.
	        						double radius = zLen-index%zLen-1;
		        					// Set the x and z accelerations.
		        					double xRot = Math.cos(angle) * radius / radDiv;
		        					double zRot = Math.sin(angle) * radius / radDiv;
		        					entity.get(index).setVelocity(new Vector(directionMultiplier*xRot, 0.002, zRot));
		        					
		        					fBlocks.get(index).setTicksLived(1);
		        					
//		        					// If there's more than 0 passengers for the entity (ArmorStand), set its tickslived to 0, so the blocks on the AS's won't despawn.
//		        					if (entity.get(index).getPassengers().size() > 0)
//		        					{
//		        						entity.get(index).getPassengers().get(0).setTicksLived(1);
//		        					}
	        					}
	        					index++;
	        				}
	        			}
	        		}
            }
		}.runTaskTimer(plugin, 14, 1);
	}
}
