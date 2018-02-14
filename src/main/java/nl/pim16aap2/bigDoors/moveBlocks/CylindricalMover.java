package nl.pim16aap2.bigDoors.moveBlocks;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import net.md_5.bungee.api.ChatColor;
import nl.pim16aap2.bigDoors.BigDoors;
import nl.pim16aap2.bigDoors.Door;
import nl.pim16aap2.bigDoors.customEntities.CustomEntityFallingBlock;
import nl.pim16aap2.bigDoors.customEntities.CustomCraftFallingBlock;
import nl.pim16aap2.bigDoors.customEntities.NoClipArmorStand;
import nl.pim16aap2.bigDoors.moveBlocks.Cylindrical.getNewLocation.GetNewLocation;
import nl.pim16aap2.bigDoors.moveBlocks.Cylindrical.getNewLocation.GetNewLocationEast;
import nl.pim16aap2.bigDoors.moveBlocks.Cylindrical.getNewLocation.GetNewLocationNorth;
import nl.pim16aap2.bigDoors.moveBlocks.Cylindrical.getNewLocation.GetNewLocationSouth;
import nl.pim16aap2.bigDoors.moveBlocks.Cylindrical.getNewLocation.GetNewLocationWest;
import nl.pim16aap2.bigDoors.util.BlockData;
import nl.pim16aap2.bigDoors.util.DoorDirection;
import nl.pim16aap2.bigDoors.util.RotateDirection;

public class CylindricalMover
{
	private BigDoors        plugin;
	private World           world;
	private RotateDirection rotDirection;
	private List<nl.pim16aap2.bigDoors.customEntities.CraftArmorStand> entity = new ArrayList<nl.pim16aap2.bigDoors.customEntities.CraftArmorStand>();
	private int             qCircles, xLen, yLen, zLen, dx, dz, xMin, xMax, yMin, yMax, zMin, zMax;
	private List<BlockData> savedBlocks = new ArrayList<BlockData>();
	private Location        turningPoint, pointOpposite;
	private double          speed;
	private GetNewLocation  gnl;
	private Door            door;
	
	
	public CylindricalMover(BigDoors plugin, World world, int qCircles, RotateDirection rotDirection, double speed,
			Location pointOpposite, DoorDirection currentDirection, Door door)
	{
		this.pointOpposite    = pointOpposite;
		this.turningPoint     = door.getEngine();
		this.rotDirection     = rotDirection;
		this.qCircles         = qCircles * 8;
		this.plugin           = plugin;
		this.world            = world;
		this.door             = door;
		
		this.xMin    = turningPoint.getBlockX() < pointOpposite.getBlockX() ? turningPoint.getBlockX() : pointOpposite.getBlockX();
		this.yMin    = turningPoint.getBlockY() < pointOpposite.getBlockY() ? turningPoint.getBlockY() : pointOpposite.getBlockY();
		this.zMin    = turningPoint.getBlockZ() < pointOpposite.getBlockZ() ? turningPoint.getBlockZ() : pointOpposite.getBlockZ();
		this.xMax    = turningPoint.getBlockX() > pointOpposite.getBlockX() ? turningPoint.getBlockX() : pointOpposite.getBlockX();
		this.yMax    = turningPoint.getBlockY() > pointOpposite.getBlockY() ? turningPoint.getBlockY() : pointOpposite.getBlockY();
		this.zMax    = turningPoint.getBlockZ() > pointOpposite.getBlockZ() ? turningPoint.getBlockZ() : pointOpposite.getBlockZ();
		
		int xLen     = (int) (xMax - xMin) + 1;
		int yLen     = (int) (yMax - yMin) + 1;
		int zLen     = (int) (zMax - zMin) + 1;
		
		this.xLen    = xLen;
		this.yLen    = yLen;
		this.zLen    = zLen;
		this.speed   = speed;

		this.dx      = pointOpposite.getBlockX() > turningPoint.getBlockX() ? 1 : -1;
		this.dz      = pointOpposite.getBlockZ() > turningPoint.getBlockZ() ? 1 : -1;

//		// xRotMul is positive when the door starts facing the north, negative otherwise.
//		this.xRotMul = currentDirection == DoorDirection.NORTH ? 1 : -1;
//		// zRotMul is positive for all counter clockwise movements, except for when the door starts facing the north. Then the negative and positive are swapped.
//		this.zRotMul = rotDirection == RotateDirection.CLOCKWISE        && currentDirection != DoorDirection.NORTH ? -1 :
//		               rotDirection == RotateDirection.COUNTERCLOCKWISE && currentDirection == DoorDirection.NORTH ? -1 : 1;

//		this.zRotMul = rotDirection == RotateDirection.CLOCKWISE ? -1 : 1;
				
		/*
		 * ClockWise East : -xRot, -zRot,  mAAFG  Y
		 * CounterCl East : -xRot,  zRot, -mAAFG  Y
		 * ClockWise West : -xRot, -zRot,  mAAFG  Y
		 * CounterCl West : -xRot,  zRot, -mAAFG  Y
		 * ClockWise North:  xRot,  zRot,  mAAFG  Y
		 * CounterCl North:  xRot, -zRot, -mAAFG  Y
		 * ClockWise South: -xRot, -zRot,  mAAFG  Y
		 * CounterCl South: -xRot,  zRot, -mAAFG  Y
		 */
		
		int index = 0;
		double xAxis = turningPoint.getX();
		do
		{
			double zAxis = turningPoint.getZ();
			do
			{
				// Get the radius of this pillar.
				double radius = Math.abs(xAxis - turningPoint.getBlockX()) > Math.abs(zAxis - turningPoint.getBlockZ()) ?
				                Math.abs(xAxis - turningPoint.getBlockX()) : Math.abs(zAxis - turningPoint.getBlockZ());
								
				for (double yAxis = yMin; yAxis <= yMax; yAxis++)
				{
//					Location newStandLocation  = new Location(world, xAxis + 0.5, yAxis - 0.741, zAxis + 0.5);
//					Location newFBlockLocation = new Location(world, xAxis + 0.5, yAxis - 0.020, zAxis + 0.5);
					Location newStandLocation  = new Location(world, xAxis + 0.5, yAxis + 35.741, zAxis + 0.5);
					Location newFBlockLocation = new Location(world, xAxis + 0.5, yAxis + 35.020, zAxis + 0.5);
					
					Material mat = world.getBlockAt((int) xAxis, (int) yAxis, (int) zAxis).getType();
					@SuppressWarnings("deprecation")
					Byte matData = world.getBlockAt((int) xAxis, (int) yAxis, (int) zAxis).getData();

//					world.getBlockAt((int) xAxis, (int) yAxis, (int) zAxis).setType(Material.AIR);
					
					nl.pim16aap2.bigDoors.customEntities.CraftArmorStand noClipArmorStand = noClipArmorStandFactory(newStandLocation);
					
					CustomCraftFallingBlock fBlock = fallingBlockFactory (newFBlockLocation, mat, (byte) matData, world);
					
					noClipArmorStand.addPassenger(fBlock);
					entity.add(index, noClipArmorStand);
					
					savedBlocks.add(index, new BlockData(mat, matData, fBlock, radius));

					index++;
				}
				zAxis += dz;
			}
			while (zAxis >= pointOpposite.getBlockZ() && dz == -1 || zAxis <= pointOpposite.getBlockZ() && dz == 1);
			xAxis += dx;
		}
		while (xAxis >= pointOpposite.getBlockX() && dx == -1 || xAxis <= pointOpposite.getBlockX() && dx == 1);

		GetNewLocation  gnln = new GetNewLocationNorth(world, xMin, xMax, zMin, zMax, rotDirection);
		GetNewLocation  gnle = new GetNewLocationEast (world, xMin, xMax, zMin, zMax, rotDirection);
		GetNewLocation  gnls = new GetNewLocationSouth(world, xMin, xMax, zMin, zMax, rotDirection);
		GetNewLocation  gnlw = new GetNewLocationWest (world, xMin, xMax, zMin, zMax, rotDirection);
		
		// Basically set a pointer to the correct GetNewLocation function.
		switch (currentDirection)
		{
		case NORTH:
			this.gnl = gnln;
			break;
		case EAST:
			this.gnl = gnle;
			break;
		case SOUTH:
			this.gnl = gnls;
			break;
		case WEST:
			this.gnl = gnlw;
			break;
		}		
//		// This part should be used instead of the 21 lines above (including the definitions/declarations of gnln etc),
//		// But this part below doesn't work for live development... sigh...
//		switch (currentDirection)
//		{
//		case NORTH:
//			this.gnl = new GetNewLocationNorth(world, xMin, xMax, zMin, zMax, rotDirection);
//			break;
//		case EAST:
//			this.gnl = new GetNewLocationEast (world, xMin, xMax, zMin, zMax, rotDirection);
//			break;
//		case SOUTH:
//			this.gnl = new GetNewLocationSouth(world, xMin, xMax, zMin, zMax, rotDirection);
//			break;
//		case WEST:
//			this.gnl = new GetNewLocationWest (world, xMin, xMax, zMin, zMax, rotDirection);
//			break;
//		}
		
		rotateEntities();
	}

	// Put the door blocks back, but change their state now.
//	@SuppressWarnings("deprecation")
	public void putBlocks()
	{
		int index = 0;
		double xAxis = turningPoint.getX();
		do
		{
			double zAxis = turningPoint.getZ();
			do
			{
				for (double yAxis = yMin; yAxis <= yMax; yAxis++)
				{
					/*
					 * 0-3: Vertical oak, spruce, birch, then jungle 4-7: East/west oak, spruce,
					 * birch, jungle 8-11: North/south oak, spruce, birch, jungle 12-15: Uses oak,
					 * spruce, birch, jungle bark texture on all six faces
					 */

//					Material mat = savedBlocks.get(index).getMat();
//					Byte matData = rotateBlockData(savedBlocks.get(index).getBlockByte());
//
//					Location newPos = gnl.getNewLocation(savedBlocks, xAxis, yAxis, zAxis, index);
//
//					world.getBlockAt(newPos).setType(mat);
//					world.getBlockAt(newPos).setData(matData);

					savedBlocks.get(index).getFBlock().remove();
					entity.get(index).remove();

					index++;
				}
				zAxis += dz;
			}
			while (zAxis >= pointOpposite.getBlockZ() && dz == -1 || zAxis <= pointOpposite.getBlockZ() && dz == 1);
			xAxis += dx;
		}
		while (xAxis >= pointOpposite.getBlockX() && dx == -1 || xAxis <= pointOpposite.getBlockX() && dx == 1);
		// Empty the entity arrayList.
		savedBlocks.clear();
		entity.clear();
		
		// Change door availability to true, so it can be opened again.
		door.changeAvailability(true);
	}
	
	
	public void finishBlocks()
	{
		int index = 0;
		double xAxis = turningPoint.getX();
		do
		{
			double zAxis = turningPoint.getZ();
			do
			{
				for (double yAxis = yMin; yAxis <= yMax; yAxis++)
				{
					entity.get(index).setGravity(false);
					
					// Get final position of the blocks.
					Location newPos = gnl.getNewLocation(savedBlocks, xAxis, yAxis, zAxis, index);
					
					newPos.setX(newPos.getX() + 0.5  );
					newPos.setY(newPos.getY() - 0.741);
					newPos.setZ(newPos.getZ() + 0.5  );

					// Remove falling block from entity, teleport entity, then reaatch falling block to entity.
					entity.get(index).eject();
					entity.get(index).teleport(newPos);
					entity.get(index).addPassenger(savedBlocks.get(index).getFBlock());
					
					++index;
				}
				zAxis += dz;
			}
			while (zAxis >= pointOpposite.getBlockZ() && dz == -1 || zAxis <= pointOpposite.getBlockZ() && dz == 1);
			xAxis += dx;
		}
		while (xAxis >= pointOpposite.getBlockX() && dx == -1 || xAxis <= pointOpposite.getBlockX() && dx == 1);
		
		new BukkitRunnable()
		{
			@Override
			public void run()
			{
				putBlocks();
			}
		}.runTaskLater(plugin, 5L);
	}
	
	
	
	
	public void rotateEntities()
	{
		int directionMultiplier = rotDirection == RotateDirection.CLOCKWISE ? 1 : -1;
		Location center         = new Location(world, turningPoint.getBlockX() + 0.5, yMin, turningPoint.getBlockZ() + 0.5);
		double maxRad           = xLen > zLen ? xLen : zLen;

		new BukkitRunnable()
		{
			double angle        = 0.0D;
			final int tckPrCrcle= (int) (2800 / speed); // How many ticks go into 1 circle (20 ticks = 1 second).
			final double step   = ((2 * Math.PI) / tckPrCrcle);
			int eights          = 1;
			boolean replace     = false;
			int qCircleCount    = 0;
			
			@Override
			public void run()
			{
				int index = 0;
				
				// Starting at 1/8 circle, every 1/4 circle.
				if ((Math.abs(angle) / (8 * eights)) >= 0.1)
				{
					eights += 2;
					replace = true;
				}
				
				if (!plugin.isPaused())
				{
					double xAxis = turningPoint.getX();
					do
					{
						angle += -(step * directionMultiplier);
						double zAxis = turningPoint.getZ();
						do
						{
							double radius     = savedBlocks.get(index).getRadius();
							
							for (double yAxis = yMin; yAxis <= yMax; yAxis++)
							{	
								double xPos   = entity.get(index).getLocation().getX();
								double zPos   = entity.get(index).getLocation().getZ();
								
								// Get the real angle the door has opened so far. Subtract angle offset, as the angle should start at 0 for these calculations to work.
								double realAngle    = Math.atan2(center.getZ() - zPos, center.getX() - xPos);
								double realAngleDeg = Math.abs((Math.toDegrees(realAngle ) + 450) % 360 - 360); // [0;360]
								double moveAngle    = (realAngleDeg + 90) % 360;
								
								if (radius != 0)
								{	
									double dX       = Math.abs(xPos - (turningPoint.getBlockX() + 0.5));
									double dZ       = Math.abs(zPos - (turningPoint.getBlockZ() + 0.5));
									double realRad  = Math.sqrt(dX * dX + dZ * dZ);
									
									double moveAngleAddForGoal = directionMultiplier * 15 * (radius - realRad + 0.3);

									// Inversed zRot sign for directionMultiplier.
									double xRot = -1 *                       (realRad / maxRad)   * speed * Math.sin(Math.toRadians(moveAngle + moveAngleAddForGoal));
									double zRot = -1 * directionMultiplier * (realRad / maxRad)   * speed * Math.cos(Math.toRadians(moveAngle + moveAngleAddForGoal));
									entity.get(index).setVelocity(new Vector (directionMultiplier * xRot, 0.000, zRot));
								}

//								Material mat = savedBlocks.get(index).getMat();
//								if (replace && (mat == Material.LOG || mat == Material.LOG_2))
//								{
//									Location loc = savedBlocks.get(index).getFBlock().getLocation();
//									Byte matData = rotateBlockData(savedBlocks.get(index).getBlockByte());
//									savedBlocks.get(index).getFBlock().remove();
//
//									CustomCraftFallingBlock fBlock = fallingBlockFactory(loc, mat, (byte) matData, world);
//									savedBlocks.get(index).setFBlock(fBlock);
//									entity.get(index).addPassenger(fBlock);
//								} 
								
								index++;
							}
							zAxis += dz;
						}
						while (zAxis >= pointOpposite.getBlockZ() && dz == -1 || zAxis <= pointOpposite.getBlockZ() && dz == 1);
						xAxis += dx;
					}
					while (xAxis >= pointOpposite.getBlockX() && dx == -1 || xAxis <= pointOpposite.getBlockX() && dx == 1);
					replace = false;
				}

				if (qCircleCount >= 2 || !plugin.canGo())
				{
					Bukkit.broadcastMessage("Stop! qCirlces = " + qCircleCount);
					this.cancel();
					finishBlocks();
				}
			}
		}.runTaskTimer(plugin, 14, 5);
	}
	
	
	
	// Rotate blocks such a logs by modifying its material data.
	public byte rotateBlockData(Byte matData)
	{
		if (matData >= 4 && matData <= 7)
			matData = (byte) (matData + 4);
		else if (matData >= 7 && matData <= 11)
			matData = (byte) (matData - 4);
		return matData;
	}
	
	// Make a falling block.
	public CustomCraftFallingBlock fallingBlockFactory(Location loc, Material mat, byte matData, World world)
	{
		CustomEntityFallingBlock fBlockNMS = new CustomEntityFallingBlock((org.bukkit.craftbukkit.v1_11_R1.CraftWorld) world, mat, loc.getX(), loc.getY(), loc.getZ(), (byte) matData);
		((org.bukkit.craftbukkit.v1_11_R1.CraftWorld) loc.getWorld()).getHandle().addEntity(fBlockNMS, SpawnReason.CUSTOM);
		CustomCraftFallingBlock fBlock = new CustomCraftFallingBlock(Bukkit.getServer(), fBlockNMS);
		fBlock.setVelocity(new Vector(0, 0, 0));
		fBlock.setDropItem(false);
		return fBlock;
	}
	
	// Make a no clip armorstand.
	public nl.pim16aap2.bigDoors.customEntities.CraftArmorStand noClipArmorStandFactory(Location newStandLocation)
	{
		NoClipArmorStand noClipArmorStandTemp = new NoClipArmorStand((org.bukkit.craftbukkit.v1_11_R1.CraftWorld) newStandLocation.getWorld(), newStandLocation);
		((org.bukkit.craftbukkit.v1_11_R1.CraftWorld) newStandLocation.getWorld()).getHandle().addEntity(noClipArmorStandTemp, SpawnReason.CUSTOM);

		noClipArmorStandTemp.setInvisible(true);
		noClipArmorStandTemp.setSmall(true);

		nl.pim16aap2.bigDoors.customEntities.CraftArmorStand noClipArmorStand = new nl.pim16aap2.bigDoors.customEntities.CraftArmorStand((org.bukkit.craftbukkit.v1_11_R1.CraftServer) (Bukkit.getServer()), noClipArmorStandTemp);

		noClipArmorStand.setVelocity  (new Vector(0, 0, 0));
		noClipArmorStand.setCollidable(false);
		
		return noClipArmorStand;
	}
}
