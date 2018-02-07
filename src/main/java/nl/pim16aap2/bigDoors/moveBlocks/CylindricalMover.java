package nl.pim16aap2.bigDoors.moveBlocks;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.FallingBlock;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import nl.pim16aap2.bigDoors.BigDoors;
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
	private int             qCircles, xLen, zLen, dx, dz, xMin, xMax, yMin, yMax, zMin, zMax;
	private List<BlockData> savedBlocks = new ArrayList<BlockData>();
	private Location        turningPoint, pointOpposite;
	private double          speed;
	private GetNewLocation  gnl;
	private int xRotMul, zRotMul;
	
	
	public CylindricalMover(BigDoors plugin, World world, int qCircles, RotateDirection rotDirection, double speed,
			Location turningPoint, Location pointOpposite, DoorDirection currentDirection)
	{
		this.pointOpposite    = pointOpposite;
		this.turningPoint     = turningPoint;
		this.rotDirection     = rotDirection;
		this.qCircles         = qCircles;
		this.plugin           = plugin;
		this.world            = world;
		
		this.xMin    = turningPoint.getBlockX() < pointOpposite.getBlockX() ? turningPoint.getBlockX() : pointOpposite.getBlockX();
		this.yMin    = turningPoint.getBlockY() < pointOpposite.getBlockY() ? turningPoint.getBlockY() : pointOpposite.getBlockY();
		this.zMin    = turningPoint.getBlockZ() < pointOpposite.getBlockZ() ? turningPoint.getBlockZ() : pointOpposite.getBlockZ();
		this.xMax    = turningPoint.getBlockX() > pointOpposite.getBlockX() ? turningPoint.getBlockX() : pointOpposite.getBlockX();
		this.yMax    = turningPoint.getBlockY() > pointOpposite.getBlockY() ? turningPoint.getBlockY() : pointOpposite.getBlockY();
		this.zMax    = turningPoint.getBlockZ() > pointOpposite.getBlockZ() ? turningPoint.getBlockZ() : pointOpposite.getBlockZ();
		
		int xLen     = (int) (xMax - xMin) + 1;
		int zLen     = (int) (zMax - zMin) + 1;
		
		this.xLen    = xLen;
		this.zLen    = zLen;
		this.speed   = speed;

		this.dx      = pointOpposite.getBlockX() > turningPoint.getBlockX() ? 1 : -1;
		this.dz      = pointOpposite.getBlockZ() > turningPoint.getBlockZ() ? 1 : -1;
		
		// xRotMul is positive when the door starts facing the north, negative otherwise.
		this.xRotMul = currentDirection == DoorDirection.NORTH ? 1 : -1;
		// zRotMul is positive for all counter clockwise movements, except for when the door starts facing the north. Then the negative and positive are swapped.
		this.zRotMul = rotDirection == RotateDirection.CLOCKWISE && currentDirection != DoorDirection.NORTH ? -1 :
						rotDirection == RotateDirection.COUNTERCLOCKWISE && currentDirection == DoorDirection.NORTH ? -1 : 1;
		
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
					Location newStandLocation  = new Location(world, xAxis + 0.5, yAxis + 25.741, zAxis + 0.5);
					Location newFBlockLocation = new Location(world, xAxis + 0.5, yAxis + 25.020, zAxis + 0.5);
					
					Material mat = world.getBlockAt((int) xAxis, (int) yAxis, (int) zAxis).getType();
					@SuppressWarnings("deprecation")
					Byte matData = world.getBlockAt((int) xAxis, (int) yAxis, (int) zAxis).getData();

//					world.getBlockAt((int) xAxis, (int) yAxis, (int) zAxis).setType(Material.AIR);
					
					nl.pim16aap2.bigDoors.customEntities.CraftArmorStand noClipArmorStand = noClipArmorStandFactory(newStandLocation);
					
					FallingBlock fBlock = fallingBlockFactory (newFBlockLocation, mat, (byte) matData, world);
					
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
		double additionalTurn   = 0.07;
		Location center         = new Location(world, turningPoint.getBlockX() + 0.5, yMin, turningPoint.getBlockZ() + 0.5);
		double maxRad           = xLen > zLen ? xLen : zLen;

		new BukkitRunnable()
		{
			double angle        = 0.0D;
			final int tckPrCrcle= (int) (2800 / speed); // How many ticks go into 1 circle (20 ticks = 1 second).
			final double step   = ((2 * Math.PI) / tckPrCrcle);
			int eights          = 1;
			boolean replace     = false;

			@Override
			public void run()
			{
				// If the angle equals or exceeds 1.5808 rad (90 degrees) multiplied by the
				// amount of quarter circles it should turn, stop.
				if (Math.abs(angle) >= (Math.PI / 2) * qCircles + additionalTurn + 1.5)
				{
					this.cancel();
					finishBlocks();
				}
				int index = 0;
				
				// Starting at 1/8 circle, every 1/4 circle.
				if ((Math.abs(angle) / (8 * eights)) >= 0.1)
				{
					eights += 2;
					replace = true;
				}
				
				// Loop up and down first.
				double xAxis = turningPoint.getX();
				do
				{
					double zAxis = turningPoint.getZ();
					do
					{
						double radius = savedBlocks.get(index).getRadius();
//						Location loc2 = entity.get(index).getLocation();
//						double dX = Math.abs((loc2.getBlockX() + 0.5) - (turningPoint.getBlockX() + 0.5));
//						double dZ = Math.abs((loc2.getBlockZ() + 0.5) - (turningPoint.getBlockZ() + 0.5));
//						radius = Math.sqrt(dX * dX + dZ * dZ);
//						if (radius <= 2.0 && radius >= 1.0)
//							Bukkit.broadcastMessage(String.format("radius=%.3f, dX=%.2f, dZ=%.2f, loc2X=%.2f, tpX=%.2f", radius, dX, dZ, loc2.getX(), turningPoint.getX()));
						
						for (double yAxis = yMin; yAxis <= yMax; yAxis++)
						{
							angle += -(step * directionMultiplier);
							if (Math.abs(angle) <= (Math.PI / 2) * qCircles + additionalTurn)
							{
								// Get the real angle the door has opened so far. Subtract angle offset, as the angle should start at 0 for these calculations to work.
								double realAngle = Math.atan2(center.getZ() - entity.get(index).getLocation().getZ(), center.getX() - entity.get(index).getLocation().getX());
								// Set the gravity stat of the armor stand to true, so it can move again.
								entity.get(index).setGravity(true);
								
								double extraAngle = 0;
								if(		xAxis >= turningPoint.getBlockX() && zAxis >= turningPoint.getBlockZ())
									extraAngle = 0.5 * Math.PI;
								else if(	xAxis <= turningPoint.getBlockX() && zAxis >= turningPoint.getBlockZ())
									extraAngle = 0.5 * Math.PI;
								else if(	xAxis >= turningPoint.getBlockX() && zAxis <= turningPoint.getBlockZ())
									extraAngle = 1.5 * Math.PI;
								else if(	xAxis <= turningPoint.getBlockX() && zAxis <= turningPoint.getBlockZ())
									extraAngle = 1 * Math.PI;
									
								double moveAngle = (90 - Math.toDegrees(realAngle + extraAngle));
								
								if (radius != 0)
								{
									double radiusGoal = savedBlocks.get(index).getRadius();
									
									// TODO: Set desired angle and punish divergent blocks with decrease/increase in speed.
									//       Only a problem when turning doors more than 180 degrees though.
									
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
									
									Location loc2 = entity.get(index).getLocation();
									double dX = Math.abs((loc2.getBlockX() + 0.5) - (turningPoint.getBlockX() + 0.5));
									double dZ = Math.abs((loc2.getBlockZ() + 0.5) - (turningPoint.getBlockZ() + 0.5));
									radius = Math.sqrt(dX * dX + dZ * dZ);
									
									double moveAngleAddForGoal = directionMultiplier * 25 * (radiusGoal - radius);
									
//									Bukkit.broadcastMessage(String.format("mAng=%3.2f, rAng=%.2f, rad=%.2f, radG=%.2f, rd=%.2f", moveAngle, realAngle, radius, radiusGoal, (radiusGoal - radius)));
									double xRot = xRotMul * 3 * (radius / maxRad) * 0.2 * Math.sin(Math.toRadians(moveAngle + moveAngleAddForGoal));
									double zRot = zRotMul * 3 * (radius / maxRad) * 0.2 * Math.cos(Math.toRadians(moveAngle + moveAngleAddForGoal));
									entity.get(index).setVelocity(new Vector(directionMultiplier * xRot, 0.002, zRot));
								}
								else
									entity.get(index).setVelocity(new Vector(0, 0.002, 0));
//									entity.get(index).setVelocity(new Vector(0, 0.002, 0));
								
//								double xRot = directionMultiplier * Math.cos(realAngle) * radius / radDiv;
//								double zRot = directionMultiplier * Math.sin(realAngle) * radius / radDiv;
//								entity.get(index).setVelocity(new Vector(directionMultiplier * xRot, 0.002, zRot));

								Material mat = savedBlocks.get(index).getMat();

								if (replace && (mat == Material.LOG || mat == Material.LOG_2))
								{
									Location loc = savedBlocks.get(index).getFBlock().getLocation();
									Byte matData = rotateBlockData(savedBlocks.get(index).getBlockByte());
									savedBlocks.get(index).getFBlock().remove();

									FallingBlock fBlock = fallingBlockFactory (loc, mat, (byte) matData, world);
									savedBlocks.get(index).setFBlock(fBlock);
									entity.get(index).addPassenger(fBlock);
								} 
								else
									savedBlocks.get(index).getFBlock().setTicksLived(1);
							}
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
		}.runTaskTimer(plugin, 14, 1);
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
	public FallingBlock fallingBlockFactory(Location loc, Material mat, byte matData, World world)
	{
		@SuppressWarnings("deprecation")
		FallingBlock fBlock = world.spawnFallingBlock(loc, mat, (byte) matData);
		fBlock.setVelocity(new Vector(0, 0, 0));
		fBlock.setDropItem(false);
		fBlock.setGravity(false);
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

		noClipArmorStand.setVelocity(new Vector(0, 0, 0));
		noClipArmorStand.setGravity(false);
		noClipArmorStand.setCollidable(false);
		
		return noClipArmorStand;
	}
}
