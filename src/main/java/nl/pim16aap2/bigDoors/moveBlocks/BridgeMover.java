package nl.pim16aap2.bigDoors.moveBlocks;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import net.md_5.bungee.api.ChatColor;
import nl.pim16aap2.bigDoors.BigDoors;
import nl.pim16aap2.bigDoors.Door;
import nl.pim16aap2.bigDoors.customEntities.CustomCraftFallingBlock_Vall;
import nl.pim16aap2.bigDoors.customEntities.FallingBlockFactory_Vall;
import nl.pim16aap2.bigDoors.moveBlocks.Bridge.RotationEastWest;
import nl.pim16aap2.bigDoors.moveBlocks.Bridge.RotationFormulae;
import nl.pim16aap2.bigDoors.moveBlocks.Bridge.RotationNorthSouth;
import nl.pim16aap2.bigDoors.moveBlocks.Bridge.getNewLocation.GetNewLocation;
import nl.pim16aap2.bigDoors.moveBlocks.Bridge.getNewLocation.GetNewLocationEast;
import nl.pim16aap2.bigDoors.moveBlocks.Bridge.getNewLocation.GetNewLocationNorth;
import nl.pim16aap2.bigDoors.moveBlocks.Bridge.getNewLocation.GetNewLocationSouth;
import nl.pim16aap2.bigDoors.moveBlocks.Bridge.getNewLocation.GetNewLocationWest;
import nl.pim16aap2.bigDoors.util.BlockData;
import nl.pim16aap2.bigDoors.util.DoorDirection;
import nl.pim16aap2.bigDoors.util.RotateDirection;
import nl.pim16aap2.bigDoors.util.Util;

public class BridgeMover
{
	private World                   world;
	private int                   dirMulX;
	private int                   dirMulZ;
	private BigDoors               plugin;
	private int                    dx, dz;
	private double                  speed;
	private FallingBlockFactory_Vall fabf;
	private boolean                    NS;
	private GetNewLocation            gnl;
	private Door                     door;
	private RotateDirection        upDown;
	private RotationFormulae     formulae;
	private DoorDirection      engineSide;
	private Location         turningPoint;
	private Location        pointOpposite;
	private DoorDirection   openDirection;
	private int          xMin, yMin, zMin;
	private int          xMax, yMax, zMax;
	private int          xLen, yLen, zLen;
	private int       directionMultiplier;
	@SuppressWarnings("unused")
	private List<BlockData> 	savedBlocks = new ArrayList<BlockData>();
	
	public BridgeMover(BigDoors plugin, World world, double speed, Door door, RotateDirection upDown, DoorDirection openDirection)
	{
		this.door          = door;
		this.fabf          = plugin.getFABF();
		this.world         = world;
		this.plugin        = plugin;
		this.upDown        = upDown;
		this.openDirection = openDirection;
		this.engineSide    = door.getEngSide();
		this.NS            = engineSide == DoorDirection.NORTH || engineSide == DoorDirection.SOUTH;
		
		this.speed   = speed;
		
		this.xMin    = door.getMinimum().getBlockX();
		this.yMin    = door.getMinimum().getBlockY();
		this.zMin    = door.getMinimum().getBlockZ();
		
		this.xMax    = door.getMaximum().getBlockX();
		this.yMax    = door.getMaximum().getBlockY();
		this.zMax    = door.getMaximum().getBlockZ();
		
		this.xLen    = this.xMax - this.xMin;
		this.yLen    = this.yMax - this.yMin;
		this.zLen    = this.zMax - this.zMin;
		
		// TODO: Make interface for block movers for the bridge and make a class for north/south and one for east/west.
		// The formulae are a bit different (different axis) and this ought to help make it bit easier to read this shit.
		if (engineSide.equals(DoorDirection.NORTH) || engineSide.equals(DoorDirection.SOUTH))
			this.formulae = new RotationNorthSouth();
		else
			this.formulae = new RotationEastWest();
		
		// Regarding dx, dz. These variables determine whether loops get incremented (1) or decremented (-1)
		// When looking in the direction of the opposite point from the engine side, the blocks should get 
		// Processed from left to right and from the engine to the opposite. 
		
		// Calculate turningpoint and pointOpposite.
		switch (engineSide)
		{
		case NORTH:
			Bukkit.broadcastMessage(ChatColor.GREEN + "NORTH, OpenDirection = " + openDirection.toString());
			// When EngineSide is North, x goes from low to high and z goes from low to high
			this.dx      =  1;
			this.dz      =  1;
			this.turningPoint = new Location(world, xMin, yMin, zMin);
			
			if (upDown.equals(RotateDirection.UP))
			{
				Bukkit.broadcastMessage(ChatColor.RED + "GOING UP");
				directionMultiplier = -1;
				this.pointOpposite  = new Location(world, xMax, yMin, zMax);
				this.dirMulX =  1;
				this.dirMulZ =  1;
			}
			else
			{
				this.pointOpposite  = new Location(world, xMax, yMax, zMin);
				if (openDirection.equals(DoorDirection.NORTH))
				{
					Bukkit.broadcastMessage(ChatColor.RED + "GOING NORTH");
					directionMultiplier = -1;
					this.dirMulX =  1;
					this.dirMulZ =  1;
				}
				else if (openDirection.equals(DoorDirection.SOUTH))
				{
					Bukkit.broadcastMessage(ChatColor.RED + "GOING SOUTH");
					directionMultiplier =  1;
					this.dirMulX = -1;
					this.dirMulZ = -1;
				}
			}
			break;
			
		case SOUTH:
			Bukkit.broadcastMessage(ChatColor.GREEN + "SOUTH, OpenDirection = " + openDirection.toString());
			// When EngineSide is South, x goes from high to low and z goes from high to low
			this.dx      = -1;
			this.dz      = -1;
			this.dirMulX =  1;
			this.dirMulZ =  1;
			this.turningPoint = new Location(world, xMax, yMin, zMax);
			
			if (upDown.equals(RotateDirection.UP))
			{
				directionMultiplier =  1;
				this.pointOpposite = new Location(world, xMin, yMin, zMin);
			}
			else
			{
				this.pointOpposite = new Location(world, xMin, yMax, zMax);
				if (openDirection.equals(DoorDirection.NORTH))
				{
					directionMultiplier =  1;
					this.dirMulX = -1;
					this.dirMulZ = -1;
				}
				else if (openDirection.equals(DoorDirection.SOUTH))
				{
					directionMultiplier = -1;
					this.dirMulX =  1;
					this.dirMulZ =  1;
				}
			}
			break;
			
		case EAST:
			Bukkit.broadcastMessage(ChatColor.GREEN + "EAST, OpenDirection = " + openDirection.toString());
			// When EngineSide is East, x goes from high to low and z goes from low to high
			this.dx      = -1;
			this.dz      =  1;
			this.dirMulX =  1;
			this.dirMulZ =  1;
			this.turningPoint = new Location(world, xMax, yMin, zMin);
			
			if (upDown.equals(RotateDirection.UP))
			{
				directionMultiplier = -1;
				Bukkit.broadcastMessage(ChatColor.RED + "GOING UP");
				this.pointOpposite = new Location(world, xMin, yMin, zMax);
			}
			else
			{
				this.pointOpposite = new Location(world, xMax, yMax, zMax);
				if (openDirection.equals(DoorDirection.EAST))
				{
					Bukkit.broadcastMessage(ChatColor.RED + "DOWN: GOING EAST");
					directionMultiplier =  1;
					this.dirMulX = -1;
					this.dirMulZ =  1;
				}
				else if (openDirection.equals(DoorDirection.WEST))
				{
					Bukkit.broadcastMessage(ChatColor.RED + "DOWN: GOING WEST");
					directionMultiplier = -1;
					this.dirMulX = -1;
					this.dirMulZ = -1;
				}
			}
			break;
			
		case WEST:
			Bukkit.broadcastMessage(ChatColor.GREEN + "WEST, OpenDirection = " + openDirection.toString());
			// When EngineSide is West, x goes from low to high and z goes from high to low
			this.dx      =  1;
			this.dz      = -1;
			this.dirMulX =  1;
			this.dirMulZ = -1;
			this.turningPoint = new Location(world, xMin, yMin, zMax);
			
			if (upDown.equals(RotateDirection.UP))
			{
				this.pointOpposite = new Location(world, xMax, yMin, zMin);
			}
			else
			{
				this.pointOpposite = new Location(world, xMin, yMax, zMin);
			}
			break;
		}

//		Bukkit.broadcastMessage("Min = (" + xMin + ";" + yMin + ";" + zMin + ")");
//		Bukkit.broadcastMessage("Max = (" + xMax + ";" + yMax + ";" + zMax + ")");
//		
//		Bukkit.broadcastMessage("TurningPoint  = (" + 
//				this.turningPoint.getBlockX() + ";" + this.turningPoint.getBlockY() + ";" + this.turningPoint.getBlockZ() + ")");
//		Bukkit.broadcastMessage("OppositePoint = (" + 
//					this.pointOpposite.getBlockX() + ";" + this.pointOpposite.getBlockY() + ";" + this.pointOpposite.getBlockZ() + ")");

		int index = 0;
		double xAxis = turningPoint.getX();
		do
		{
			double zAxis = turningPoint.getZ();
			do
			{
				// Get the radius of this row.
				double radius = 0;
				if (upDown == RotateDirection.UP)
				{
					if (NS)
						radius = Math.abs(zAxis - turningPoint.getBlockZ());
					else
						radius = Math.abs(xAxis - turningPoint.getBlockX());
				}
		         
				for (double yAxis = yMin; yAxis <= yMax; ++yAxis)
				{
					// If it's going down, it's currently up, which means that the radius will have to be determined for every y.
					// TODO: Make separate function for this. This is getting too messy.
					if (upDown == RotateDirection.DOWN)
						radius = yAxis - turningPoint.getBlockY();
//					Bukkit.broadcastMessage("upDown = " + upDown.toString() + ", radius = " + radius + ", pos(" + xAxis + ";" + yAxis + ";" + zAxis + ")");

//					Bukkit.broadcastMessage("0: idx = " + index + ", radius = " + radius);
					
					Location newFBlockLocation = new Location(world, xAxis + 0.5, yAxis - 0.020, zAxis + 0.5);
					// Move the lowest blocks up a little, so the client won't predict they're touching through the ground, which would make them slower than the rest.
					if (yAxis == yMin)
						newFBlockLocation.setY(newFBlockLocation.getY() + .010001);
					
					Material mat = world.getBlockAt((int) xAxis, (int) yAxis, (int) zAxis).getType();
					@SuppressWarnings("deprecation")
					Byte matData = world.getBlockAt((int) xAxis, (int) yAxis, (int) zAxis).getData();
					
//					Bukkit.broadcastMessage("Block at ("+(int) xAxis+";"+(int) yAxis+";"+(int) zAxis+") is of type " + mat.toString());
					
					// Certain blocks cannot be used the way normal blocks can (heads, (ender) chests etc).
					if (Util.isAllowedBlock(mat))
						world.getBlockAt((int) xAxis, (int) yAxis, (int) zAxis).setType(Material.AIR);
					else
					{
						mat     = Material.AIR;
						matData = 0;
					}
					
					CustomCraftFallingBlock_Vall fBlock = fallingBlockFactory (newFBlockLocation, mat, (byte) matData, world);
					
					savedBlocks.add(index, new BlockData(mat, matData, fBlock, radius));
										
					index++;
				}
				zAxis += dz;
			}
			while (zAxis >= pointOpposite.getBlockZ() && dz == -1 || zAxis <= pointOpposite.getBlockZ() && dz == 1);
			xAxis += dx;
		}
		while (xAxis >= pointOpposite.getBlockX() && dx == -1 || xAxis <= pointOpposite.getBlockX() && dx == 1);
		
		switch (openDirection)
		{
		case NORTH:
			this.gnl = new GetNewLocationNorth(world, xMin, xMax, yMin, yMax, zMin, zMax, upDown, openDirection);
			break;
		case EAST:
			this.gnl = new GetNewLocationEast (world, xMin, xMax, yMin, yMax, zMin, zMax, upDown, openDirection);
			break;
		case SOUTH:
			this.gnl = new GetNewLocationSouth(world, xMin, xMax, yMin, yMax, zMin, zMax, upDown, openDirection);
			break;
		case WEST:
			this.gnl = new GetNewLocationWest (world, xMin, xMax, yMin, yMax, zMin, zMax, upDown, openDirection);
			break;
		}
		rotateEntities();
	}

	// Put the door blocks back, but change their state now.
	@SuppressWarnings("deprecation")
	public void putBlocks()
	{
		Bukkit.broadcastMessage("Putting blocks back now!");
		
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

					Location newPos = gnl.getNewLocation(savedBlocks.get(index).getRadius(), xAxis, yAxis, zAxis, index);
					
//					Bukkit.broadcastMessage("OLD = (" + (int)xAxis + ";" + (int)yAxis + ";" + (int)zAxis + ")");
//					Bukkit.broadcastMessage("NEW = (" + newPos.getBlockX() + ";" + newPos.getBlockY() + ";" + newPos.getBlockZ() + ")\n.");

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
		plugin.getCommander().setDoorAvailable(door.getDoorUID());
	}
	
	// Put falling blocks into their final location (but keep them as falling blocks).
	// This makes the transition from entity to block appear smoother.
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
					// Get final position of the blocks.
					Location newPos = gnl.getNewLocation(savedBlocks.get(index).getRadius(), xAxis, yAxis, zAxis, index);
					
					newPos.setX(newPos.getX() + 0.5);
					newPos.setY(newPos.getY()      );
					newPos.setZ(newPos.getZ() + 0.5);
					
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
//			int directionMultiplier = 0;
//			switch (openDirection)
//			{
//			case NORTH:
//				
//				break;
//				
//			case SOUTH:
//				
//				break;
//				
//			case EAST:
//				
//				break;
//				
//			case WEST:
//				
//				break;
//			}
//			int directionMultiplier = upDown == RotateDirection.UP ? -1 : 1;
			Location center         = new Location(world, turningPoint.getBlockX() + 0.5, yMin, turningPoint.getBlockZ() + 0.5);
			double   maxRad         = xLen > zLen ? xLen : zLen;
			boolean replace         = false;
			int qCircleCount        = engineSide == DoorDirection.EAST  && upDown == RotateDirection.CLOCKWISE       || 
			                          engineSide == DoorDirection.SOUTH && upDown == RotateDirection.COUNTERCLOCKWISE ? 0 : -1;
			int qCircleCheck        = 0;
			int indexMid            = savedBlocks.size() / 2;
			int counter             = 0;
			
			@Override
			public void run()
			{
				int index = 0;
				++counter;
				
				double valueA = center.getY() - savedBlocks.get(indexMid).getFBlock().getLocation().getY();
				double valueB;
				if (NS)
					valueB = center.getZ() - savedBlocks.get(indexMid).getFBlock().getLocation().getZ();
				else
					valueB = center.getX() - savedBlocks.get(indexMid).getFBlock().getLocation().getX();
				
				double realAngleMid    = Math.atan2(valueA, valueB);
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
				
				if (qCircleCount >= 1 || !plugin.getCommander().canGo() || counter > 50)
				{
					if (counter > 50)
						Bukkit.broadcastMessage(ChatColor.AQUA + "DOOR TIMED OUT!");
					for (int idx = 0; idx < savedBlocks.size(); ++idx)
						savedBlocks.get(idx).getFBlock().setVelocity(new Vector(0D, 0D, 0D));
					finishBlocks();
					this.cancel();
				}
				else
				{
					if (!plugin.getCommander().isPaused())
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
									if (upDown.equals(RotateDirection.DOWN))
										radius    = savedBlocks.get(index).getRadius();
									if (radius != 0)
									{
										double xPos         = savedBlocks.get(index).getFBlock().getLocation().getX();
										double yPos         = savedBlocks.get(index).getFBlock().getLocation().getY();
										double zPos         = savedBlocks.get(index).getFBlock().getLocation().getZ();
										
										// Get the real angle the door has opened so far. Subtract angle offset, as the angle should start at 0 for these calculations to work.
										double valueC = center.getY() - savedBlocks.get(indexMid).getFBlock().getLocation().getY();
										double valueD;
										if (NS)
											valueD = center.getZ() - savedBlocks.get(indexMid).getFBlock().getLocation().getZ();
										else
											valueD = center.getX() - savedBlocks.get(indexMid).getFBlock().getLocation().getX();
										
										double realAngle    = Math.atan2(valueC, valueD);
										double realAngleDeg = Math.abs((Math.toDegrees(realAngle ) + 450) % 360 - 360); // [0;360]
										double moveAngle    = (realAngleDeg + 90) % 360;
										
										if (!NS)
										{
											// Get the actual radius of the block (and compare that to the radius it should have (stored in the block)) later (moveAndAddForGoal).
											double dX           = Math.abs(xPos - (turningPoint.getBlockX() + 0.5));
											double dY           = Math.abs(yPos - (turningPoint.getBlockY() + 0.5));
											double realRadius   = Math.sqrt(dX * dX + dY * dY);
											
											// Additional angle added to the movement direction so that the radius remains correct for all blocks.
											// TODO: Smaller total height needs lower punishment than larger doors. Presumably because of speed difference. Fix that.
											double moveAngleAddForGoal = directionMultiplier * 15 * (radius - realRadius - 0.18);
		
											// Inversed zRot sign for directionMultiplier.
											double xRot = dirMulX * -1                   * (realRadius / maxRad) * speed * Math.sin(Math.toRadians(moveAngle + moveAngleAddForGoal));
											double yRot = dirMulZ * -directionMultiplier * (realRadius / maxRad) * speed * Math.cos(Math.toRadians(moveAngle + moveAngleAddForGoal));
											savedBlocks.get(index).getFBlock().setVelocity(new Vector (directionMultiplier * xRot, yRot, 0.000));
										}
										else
										{
											// Get the actual radius of the block (and compare that to the radius it should have (stored in the block)) later (moveAndAddForGoal).
											double dX           = Math.abs(xPos - (turningPoint.getBlockX() + 0.5));
											double dY           = Math.abs(yPos - (turningPoint.getBlockY() + 0.5));
											double realRadius   = Math.sqrt(dX * dX + dY * dY);
											
											// Additional angle added to the movement direction so that the radius remains correct for all blocks.
											// TODO: Smaller total height needs lower punishment than larger doors. Presumably because of speed difference. Fix that.
											double moveAngleAddForGoal = directionMultiplier * 15 * (radius - realRadius - 0.18);
		
											// Inversed zRot sign for directionMultiplier.
											double xRot = dirMulX * -1                   * (realRadius / maxRad) * speed * Math.sin(Math.toRadians(moveAngle + moveAngleAddForGoal));
											double yRot = dirMulZ * -directionMultiplier * (realRadius / maxRad) * speed * Math.cos(Math.toRadians(moveAngle + moveAngleAddForGoal));
											savedBlocks.get(index).getFBlock().setVelocity(new Vector (directionMultiplier * xRot, yRot, 0.000));
										}
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
		
											CustomCraftFallingBlock_Vall fBlock = fallingBlockFactory(loc, mat, (byte) matData, world);
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
	
	public CustomCraftFallingBlock_Vall fallingBlockFactory(Location loc, Material mat, byte matData, World world)
	{
		return this.fabf.fallingBlockFactory(loc, mat, matData, world);
	}
}
