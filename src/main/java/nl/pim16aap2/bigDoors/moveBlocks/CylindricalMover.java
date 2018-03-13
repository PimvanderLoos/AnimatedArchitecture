package nl.pim16aap2.bigDoors.moveBlocks;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import nl.pim16aap2.bigDoors.BigDoors;
import nl.pim16aap2.bigDoors.Door;
import nl.pim16aap2.bigDoors.customEntities.CustomEntityFallingBlock;
import nl.pim16aap2.bigDoors.customEntities.CustomCraftFallingBlock;
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
	private BigDoors        	plugin;
	private World           	world;
	private RotateDirection 	rotDirection;
	private DoorDirection   	currentDirection;
	@SuppressWarnings("unused")
	private int             	qCircleLimit, xLen, yLen, zLen, dx, dz, xMin, xMax, yMin, yMax, zMin, zMax;
	private List<BlockData> 	savedBlocks = new ArrayList<BlockData>();
	private Location        	turningPoint, pointOpposite;
	private double          	speed;
	private GetNewLocation  	gnl;
	private Door            	door;
	
	
	public CylindricalMover(BigDoors plugin, World world, int qCircleLimit, RotateDirection rotDirection, double speed,
			Location pointOpposite, DoorDirection currentDirection, Door door)
	{
		this.currentDirection = currentDirection;
		this.pointOpposite    = pointOpposite;
		this.turningPoint     = door.getEngine();
		this.rotDirection     = rotDirection;
		this.qCircleLimit     = qCircleLimit;
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
					Location newFBlockLocation = new Location(world, xAxis + 0.5, yAxis - 0.020, zAxis + 0.5);
//					Location newFBlockLocation = new Location(world, xAxis + 0.5, yAxis - 0.020 + yLen + 5, zAxis + 0.5);
					// Move the lowest blocks up a little, so the client won't predict they're going through the ground.
					if (yAxis == yMin)
						newFBlockLocation.setY(newFBlockLocation.getY() + .010001);
					
					Material mat = world.getBlockAt((int) xAxis, (int) yAxis, (int) zAxis).getType();
					@SuppressWarnings("deprecation")
					Byte matData = world.getBlockAt((int) xAxis, (int) yAxis, (int) zAxis).getData();

					world.getBlockAt((int) xAxis, (int) yAxis, (int) zAxis).setType(Material.AIR);
					
					CustomCraftFallingBlock fBlock = fallingBlockFactory (newFBlockLocation, mat, (byte) matData, world);
					
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
	@SuppressWarnings("deprecation")
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

					Material mat = savedBlocks.get(index).getMat();
					Byte matData = rotateBlockData(savedBlocks.get(index).getBlockByte());

					Location newPos = gnl.getNewLocation(savedBlocks, xAxis, yAxis, zAxis, index);

					savedBlocks.get(index).getFBlock().remove();
					
					world.getBlockAt(newPos).setType(mat);
					world.getBlockAt(newPos).setData(matData);

					index++;
				}
				zAxis += dz;
			}
			while (zAxis >= pointOpposite.getBlockZ() && dz == -1 || zAxis <= pointOpposite.getBlockZ() && dz == 1);
			xAxis += dx;
		}
		while (xAxis >= pointOpposite.getBlockX() && dx == -1 || xAxis <= pointOpposite.getBlockX() && dx == 1);
		savedBlocks.clear();

		// Change door availability to true, so it can be opened again.
		door.changeAvailability(true);
	}
	
	
	// Put falling blocks into their final location (but keep them as falling blocks).
	// This makes the transition from entity to block appear smoother.
	public void finishBlocks()
	{
		Bukkit.broadcastMessage("Finishing blocks now!");
		int index = 0;
		double xAxis = turningPoint.getX();
		do
		{
			double zAxis = turningPoint.getZ();
			do
			{
				for (double yAxis = yMin; yAxis <= yMax; yAxis++)
				{	
					// Get final position of the blocks.
					Location newPos = gnl.getNewLocation(savedBlocks, xAxis, yAxis, zAxis, index);
					
					newPos.setX(newPos.getX() + 0.5  );
					newPos.setY(newPos.getY()        );
					newPos.setZ(newPos.getZ() + 0.5  );
					
					// Teleport the falling blocks to their final positions.
					savedBlocks.get(index).getFBlock().teleport(newPos);
					savedBlocks.get(index).getFBlock().setVelocity(new Vector(0D, 0D, 0D));
					
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
		}.runTaskLater(plugin, 4L);
	}
	
	
	// Method that takes care of the rotation aspect.
	public void rotateEntities()
	{
		new BukkitRunnable()
		{
			int directionMultiplier = rotDirection == RotateDirection.CLOCKWISE ? 1 : -1;
			Location center         = new Location(world, turningPoint.getBlockX() + 0.5, yMin, turningPoint.getBlockZ() + 0.5);
			double   maxRad         = xLen > zLen ? xLen : zLen;
			boolean replace         = false;
			int qCircleCount        = currentDirection == DoorDirection.EAST  && rotDirection == RotateDirection.CLOCKWISE       || 
			                          currentDirection == DoorDirection.SOUTH && rotDirection == RotateDirection.COUNTERCLOCKWISE ? 0 : -1;
			int qCircleCheck        = 0;
			int indexMid            = savedBlocks.size() / 2;
			
			@Override
			public void run()
			{
				int    index           = 0;
				double realAngleMid    = Math.atan2(center.getZ() - savedBlocks.get(indexMid).getFBlock().getLocation().getZ(), center.getX() - savedBlocks.get(indexMid).getFBlock().getLocation().getX());
				double realAngleMidDeg = Math.abs((Math.toDegrees(realAngleMid) + 450) % 360 - 360); // [0;360]
				
				// This part keeps track of how many quarter circles the blocks have moved by checking if blocks have moved into a new quadrant.
				// It also adds/subtracts a little from the angle so the door is stopped just before it reaches its final position (as it moved a bit further after reaching it).
				// If it's exactly a multiple of 90, it's at a starting position, so don't count that position towards qCircleCount.
				if (realAngleMidDeg % 90 != 0)
				{
					// Add or subtract 5 degrees from the actual angle and put it on [0;360] again.
					realAngleMidDeg = ((realAngleMidDeg + -directionMultiplier * 5) + 360) % 360;
					
					// If the angle / 90 is not the same as qCircleCheck, the blocks have moved on to a new quadrant.
					if ((int) (realAngleMidDeg / 90) != qCircleCheck)
					{
						qCircleCheck = (int) (realAngleMidDeg / 90);
						++qCircleCount;
					}
				}
				
				// If the blocks are at 1/8, 3/8, 5/8 or 7/8 * 360 angle, it's time to "replace" (i.e. rotate) them.
				if (((realAngleMidDeg - 45 + 360) % 360) % 90 < 5)
					replace = true;
				
				if (qCircleCount >= qCircleLimit || !plugin.canGo())
				{
					for (int idx = 0; idx < savedBlocks.size(); ++idx)
						savedBlocks.get(idx).getFBlock().setVelocity(new Vector(0D, 0D, 0D));
					finishBlocks();
					this.cancel();
				}
				else
				{
					if (!plugin.isPaused())
					{
						double xAxis = turningPoint.getX();
						do
						{
							double zAxis = turningPoint.getZ();
							do
							{
								double radius     = savedBlocks.get(index).getRadius();
								for (double yAxis = yMin; yAxis <= yMax; yAxis++)
								{
									if (radius != 0)
									{	
										double xPos         = savedBlocks.get(index).getFBlock().getLocation().getX();
										double zPos         = savedBlocks.get(index).getFBlock().getLocation().getZ();
										
										// Get the real angle the door has opened so far. Subtract angle offset, as the angle should start at 0 for these calculations to work.
										double realAngle    = Math.atan2(center.getZ() - zPos, center.getX() - xPos);
										double realAngleDeg = Math.abs((Math.toDegrees(realAngle ) + 450) % 360 - 360); // [0;360]
										double moveAngle    = (realAngleDeg + 90) % 360;
										
										double dX           = Math.abs(xPos - (turningPoint.getBlockX() + 0.5));
										double dZ           = Math.abs(zPos - (turningPoint.getBlockZ() + 0.5));
										double realRadius   = Math.sqrt(dX * dX + dZ * dZ);
										
										double moveAngleAddForGoal = directionMultiplier * 15 * (radius - realRadius - 0.18);
	
										// Inversed zRot sign for directionMultiplier.
										double xRot = -1                   * (realRadius / maxRad) * speed * Math.sin(Math.toRadians(moveAngle + moveAngleAddForGoal));
										double zRot = -directionMultiplier * (realRadius / maxRad) * speed * Math.cos(Math.toRadians(moveAngle + moveAngleAddForGoal));
										savedBlocks.get(index).getFBlock().setVelocity(new Vector (directionMultiplier * xRot, 0.000, zRot));
									}
	
									// It is not pssible to edit falling block blockdata (client won't update it), so delete the current fBlock and replace it by one that's been rotated. 
									if (replace)
									{
										Material mat     = savedBlocks.get(index).getMat();
										if (mat == Material.LOG || mat == Material.LOG_2)
										{
											Location loc = savedBlocks.get(index).getFBlock().getLocation();
											Byte matData = rotateBlockData(savedBlocks.get(index).getBlockByte());
											Vector veloc = savedBlocks.get(index).getFBlock().getVelocity();
											savedBlocks.get(index).getFBlock().remove();
		
											CustomCraftFallingBlock fBlock = fallingBlockFactory(loc, mat, (byte) matData, world);
											savedBlocks.get(index).setFBlock(fBlock);
											savedBlocks.get(index).getFBlock().setVelocity(veloc);
										}
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
				}
			}
		}.runTaskTimer(plugin, 14, 4);
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
		CustomEntityFallingBlock fBlockNMS = new CustomEntityFallingBlock(world, mat, loc.getX(), loc.getY(), loc.getZ(), (byte) matData);
		return new CustomCraftFallingBlock(Bukkit.getServer(), fBlockNMS);
	}
}
