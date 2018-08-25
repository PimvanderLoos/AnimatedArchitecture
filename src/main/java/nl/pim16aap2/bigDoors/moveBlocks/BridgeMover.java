package nl.pim16aap2.bigDoors.moveBlocks;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.material.MaterialData;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import nl.pim16aap2.bigDoors.BigDoors;
import nl.pim16aap2.bigDoors.Door;
import nl.pim16aap2.bigDoors.customEntities.CustomCraftFallingBlock_Vall;
import nl.pim16aap2.bigDoors.customEntities.FallingBlockFactory_Vall;
//import nl.pim16aap2.bigDoors.moveBlocks.Bridge.RotationEastWest;
//import nl.pim16aap2.bigDoors.moveBlocks.Bridge.RotationFormulae;
//import nl.pim16aap2.bigDoors.moveBlocks.Bridge.RotationNorthSouth;
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
	private int                   doorLen;
	private int                  indexEnd;
	private int                  indexMid;
//	private RotationFormulae     formulae;
	private DoorDirection      engineSide;
	private Location         turningPoint;
	private Location        pointOpposite;
	private DoorDirection   openDirection;
	private int           angleOffset = 2;
	private int          xMin, yMin, zMin;
	private int          xMax, yMax, zMax;
	private int          xLen, yLen, zLen;
	private int       directionMultiplier;
	private int     finishA, startA, endA;
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
		
//		Bukkit.broadcastMessage("EngineSide=" + door.getEngSide().toString() + ", OpenDir=" + openDirection.toString() + ", upDown=" + upDown.toString());
		
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
		{
//			this.formulae = new RotationNorthSouth();
			this.doorLen  = zLen > yLen ? zLen : yLen;
		}
		else
		{
//			this.formulae = new RotationEastWest();
			this.doorLen  = xLen > yLen ? xLen : yLen;
		}
		
		this.speed = doorLen < 3  ? speed / 3   : 
		             doorLen < 5  ? speed / 2   : 
		             doorLen < 10 ? speed / 1.5 :
		             doorLen > 20 ? speed * 1.0 : speed;
				
		// Regarding dx, dz. These variables determine whether loops get incremented (1) or decremented (-1)
		// When looking in the direction of the opposite point from the engine side, the blocks should get 
		// Processed from left to right and from the engine to the opposite. 

		/* Pointing:   Degrees:
		 * UP            0 or 360
		 * EAST         90
		 * WEST        270
		 * NORTH       270
		 * SOUTH        90
		 */
		
		// Calculate turningpoint and pointOpposite.
		switch (engineSide)
		{
		case NORTH:
			// When EngineSide is North, x goes from low to high and z goes from low to high
			this.dx      =  1;
			this.dz      =  1;
			this.turningPoint = new Location(world, xMin, yMin, zMin);
			this.startA  = 0;
			this.endA    = 0;
			this.finishA = endA - angleOffset;
			
			if (upDown.equals(RotateDirection.UP))
			{
				this.startA  = 90;
				this.endA    =  0;
				this.finishA = 360 - angleOffset  * 3;
				directionMultiplier = -1;
				this.pointOpposite  = new Location(world, xMax, yMin, zMax);
				this.dirMulX = -1;
				this.dirMulZ = -1;
			}
			else
			{
				this.pointOpposite  = new Location(world, xMax, yMax, zMin);
				if (openDirection.equals(DoorDirection.NORTH))
				{
					this.startA  = 360;
					this.endA    = 270;
					this.finishA = endA - angleOffset * 3;
					directionMultiplier = -1;
					this.dirMulX = -1;
					this.dirMulZ = -1;
				}
				else if (openDirection.equals(DoorDirection.SOUTH))
				{
					this.startA  =  0;
					this.endA    = 90;
					this.finishA = endA + angleOffset * 3;
					directionMultiplier =  1;
					this.dirMulX = -1;
					this.dirMulZ = -1;
				}
			}
			break;
			
		case SOUTH:
			// When EngineSide is South, x goes from high to low and z goes from high to low
			this.dx      = -1;
			this.dz      = -1;
			this.dirMulX = -1;
			this.dirMulZ = -1;
			this.turningPoint = new Location(world, xMax, yMin, zMax);
			this.startA  = 0;
			this.endA    = 0;
			this.finishA = endA - angleOffset;
			
			if (upDown.equals(RotateDirection.UP))
			{
				this.startA  = 270;
				this.endA    = 360;
				this.finishA = angleOffset * 3;
				directionMultiplier =  1;
				this.pointOpposite  = new Location(world, xMin, yMin, zMin);
			}
			else
			{
				this.pointOpposite = new Location(world, xMin, yMax, zMax);
				if (openDirection.equals(DoorDirection.NORTH))
				{
					this.startA  = 360;
					this.endA    = 270;
					this.finishA = endA - angleOffset * 3;
					directionMultiplier = -1;
					this.dirMulX = -1;
					this.dirMulZ = -1;
				}
				else if (openDirection.equals(DoorDirection.SOUTH))
				{
					this.startA  =  0;
					this.endA    = 90;
					this.finishA = endA + angleOffset * 3;
					directionMultiplier =  1;
					this.dirMulX = -1;
					this.dirMulZ = -1;
				}
			}
			break;
			
		case EAST:
			// When EngineSide is East, x goes from high to low and z goes from low to high
			this.dx      = -1;
			this.dz      =  1;
			this.dirMulX =  1;
			this.dirMulZ =  1;
			this.turningPoint = new Location(world, xMax, yMin, zMin);
			
			if (upDown.equals(RotateDirection.UP))
			{
				this.startA  = 270;
				this.endA    = 360;
				this.finishA = angleOffset * 3;
				directionMultiplier = -1;
				this.pointOpposite  = new Location(world, xMin, yMin, zMax);
			}
			else
			{
				this.pointOpposite = new Location(world, xMax, yMax, zMax);
				if (openDirection.equals(DoorDirection.EAST))
				{
					this.startA  =  0;
					this.endA    = 90;
					this.finishA = endA + angleOffset * 3;
					directionMultiplier =  1;
					this.dirMulX = -1;
					this.dirMulZ = -1;
				}
				else if (openDirection.equals(DoorDirection.WEST))
				{
					this.startA  = 360;
					this.endA    = 270;
					this.finishA = endA - angleOffset * 3;
					directionMultiplier = -1;
					this.dirMulX = -1;
					this.dirMulZ = -1;
				}
			}
			break;
			
		case WEST:
			// When EngineSide is West, x goes from low to high and z goes from high to low
			this.dx      =  1;
			this.dz      = -1;
			this.dirMulX =  1;
			this.dirMulZ =  1;
			this.turningPoint = new Location(world, xMin, yMin, zMax);
			
			if (upDown.equals(RotateDirection.UP))
			{
				this.startA  = 90;
				this.endA    =  0;
				this.finishA = 360 - angleOffset * 3;
				directionMultiplier =  1;
				this.pointOpposite  = new Location(world, xMax, yMin, zMin);
			}
			else
			{
				this.pointOpposite = new Location(world, xMin, yMax, zMin);
				if (openDirection.equals(DoorDirection.EAST))
				{
					this.startA  =  0;
					this.endA    = 90;
					this.finishA = endA + angleOffset * 3;
					directionMultiplier =  1;
					this.dirMulX = -1;
					this.dirMulZ = -1;
				}
				else if (openDirection.equals(DoorDirection.WEST))
				{
					this.startA  = 360;
					this.endA    = 270;
					this.finishA = endA - angleOffset * 3;
					directionMultiplier = -1;
					this.dirMulX = -1;
					this.dirMulZ = -1;
				}
			}
			break;
		}

		// The mid values indicate the middle block of the door
		int xAxisMid = (int) (xMin + (xMax - xMin) / 2);
		int yAxisMid = (int) (yMin + (yMax - yMin) / 2);
		int zAxisMid = (int) (zMin + (zMax - zMin) / 2);
		// The end values indicate the end position. Basically pointopposite,
		// But this way you don't have to extract the values from that point many times over.
		int xAxisEnd = pointOpposite.getBlockX();
		int yAxisEnd = yMax;
		int zAxisEnd = pointOpposite.getBlockZ();
		
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
					// TODO: Make separate function for this. This is getting too messy and the performance is suffering.
					if (upDown == RotateDirection.DOWN)
						radius = yAxis - turningPoint.getBlockY();
					
					Location newFBlockLocation = new Location(world, xAxis + 0.5, yAxis - 0.020, zAxis + 0.5);
					// Move the lowest blocks up a little, so the client won't predict they're touching through the ground, which would make them slower than the rest.
					if (yAxis == yMin)
						newFBlockLocation.setY(newFBlockLocation.getY() + .010001);
					
					if (xAxis == xAxisMid && yAxis == yAxisMid && zAxis == zAxisMid)
					{
						this.indexMid = index;
//						world.getBlockAt((int) xAxis, (int) yAxis, (int) zAxis).setType(Material.EMERALD_BLOCK);
					}

					if (xAxis == xAxisEnd && yAxis == yAxisEnd && zAxis == zAxisEnd)
					{
						this.indexEnd = index;
//						world.getBlockAt((int) xAxis, (int) yAxis, (int) zAxis).setType(Material.DIAMOND_BLOCK);
					}
					
					Material mat = world.getBlockAt((int) xAxis, (int) yAxis, (int) zAxis).getType();
					@SuppressWarnings("deprecation")
					Byte matData = world.getBlockAt((int) xAxis, (int) yAxis, (int) zAxis).getData();
					BlockState bs = world.getBlockAt((int) xAxis, (int) yAxis, (int) zAxis).getState();
					MaterialData materialData = bs.getData();
					
					// Certain blocks cannot be used the way normal blocks can (heads, (ender) chests etc).
					if (Util.isAllowedBlock(mat))
						world.getBlockAt((int) xAxis, (int) yAxis, (int) zAxis).setType(Material.AIR);
					else
					{
						mat     = Material.AIR;
						matData = 0;
					}
					
					CustomCraftFallingBlock_Vall fBlock = fallingBlockFactory (newFBlockLocation, mat, (byte) matData, world);
					
					savedBlocks.add(index, new BlockData(mat, matData, fBlock, radius, materialData));
										
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
	
	// Check if a block can (should) be rotated.
	public boolean canRotate(Material mat)
	{	// Logs, stairs and glass panes can rotate, the rest can't.
		return 	mat.equals(Material.LOG)               || mat.equals(Material.LOG_2)              || mat.equals(Material.ACACIA_STAIRS)        ||
				mat.equals(Material.BIRCH_WOOD_STAIRS) || mat.equals(Material.BRICK_STAIRS)       || mat.equals(Material.COBBLESTONE_STAIRS)   || 
				mat.equals(Material.DARK_OAK_STAIRS)   || mat.equals(Material.JUNGLE_WOOD_STAIRS) || mat.equals(Material.NETHER_BRICK_STAIRS)  || 
				mat.equals(Material.PURPUR_STAIRS)     || mat.equals(Material.QUARTZ_STAIRS)      || mat.equals(Material.RED_SANDSTONE_STAIRS) || 
				mat.equals(Material.SANDSTONE_STAIRS)  || mat.equals(Material.SMOOTH_STAIRS)      || mat.equals(Material.SPRUCE_WOOD_STAIRS)   || 
				mat.equals(Material.WOOD_STAIRS)       || mat.equals(Material.STAINED_GLASS_PANE) || mat.equals(Material.THIN_GLASS);
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
					Byte matByte;
					
					if (canRotate(mat))
						matByte = rotateBlockData(savedBlocks.get(index).getBlockByte());
					else
						matByte = savedBlocks.get(index).getMatData().getData();

					Location newPos = gnl.getNewLocation(savedBlocks.get(index).getRadius(), xAxis, yAxis, zAxis, index);

					savedBlocks.get(index).getFBlock().remove();

					Block b = world.getBlockAt(newPos);					
					MaterialData matData = savedBlocks.get(index).getMatData();
					matData.setData(matByte);
					
					b.setType(mat);
					BlockState bs = b.getState();
					bs.setData(matData);
					bs.update();

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
	
	double getAngleDeg(int index, Location loc)
	{
		double valueA = loc.getY() - savedBlocks.get(index).getFBlock().getLocation().getY();
		double valueB;
		if (NS)
			valueB    = loc.getZ() - savedBlocks.get(index).getFBlock().getLocation().getZ();
		else
			valueB    = loc.getX() - savedBlocks.get(index).getFBlock().getLocation().getX();
		
		double realAngle    = Math.atan2(valueA, valueB);
		double realAngleDeg = Math.abs((Math.toDegrees(realAngle) + 450) % 360 - 360); // [0;360]
		return realAngleDeg;
	}
	
	// Method that takes care of the rotation aspect.
	public void rotateEntities()
	{
		new BukkitRunnable()
		{
			RotateDirection angleDir = startA > endA ? RotateDirection.DOWN : RotateDirection.UP;
			Location center          = new Location(world, turningPoint.getBlockX() + 0.5, yMin, turningPoint.getBlockZ() + 0.5);
			double   maxRad          = xLen > zLen ? xLen : zLen;
			boolean replace          = false;
			@SuppressWarnings("unused")
			int qCircleCount         = 	engineSide == DoorDirection.EAST  && upDown == RotateDirection.DOWN && openDirection == DoorDirection.EAST  ?  0 : 
										engineSide == DoorDirection.EAST  ? -1 : 
										engineSide == DoorDirection.WEST  && upDown == RotateDirection.DOWN && openDirection == DoorDirection.WEST  ? -1 :
										engineSide == DoorDirection.WEST  ?  0 :
										engineSide == DoorDirection.NORTH && upDown == RotateDirection.UP   ? -2 :
										engineSide == DoorDirection.NORTH && upDown == RotateDirection.DOWN && openDirection == DoorDirection.NORTH ? -1 :
										engineSide == DoorDirection.NORTH && upDown == RotateDirection.DOWN ?  0 :
										engineSide == DoorDirection.SOUTH && upDown == RotateDirection.UP   ? -2 :
										engineSide == DoorDirection.SOUTH && upDown == RotateDirection.DOWN && openDirection == DoorDirection.SOUTH ?  0 :
										engineSide == DoorDirection.SOUTH && upDown == RotateDirection.DOWN ? -1 : -1;
			int qCircleCheck         = 0;
			@SuppressWarnings("unused")
			int counter              = 0;
			
			@Override
			public void run()
			{
				int index = 0;
				++counter;
				double realAngleMidDeg = getAngleDeg(indexMid, center);
				
				// This part keeps track of how many quarter circles the blocks have moved by checking if blocks have moved into a new quadrant.
				// It also adds/subtracts a little from the angle so the door is stopped just before it reaches its final position (as it moved a bit further after reaching it).
				// If it's exactly a multiple of 90, it's at a starting position, so don't count that position towards qCircleCount.
				if (realAngleMidDeg % 90 != 0)
				{
					// Add or subtract 5 degrees from the actual angle and put it on [0;360] again.
					realAngleMidDeg = ((realAngleMidDeg + -directionMultiplier * 0) + 360) % 360;
					
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
				
//				if (qCircleCount >= 1 || !plugin.getCommander().canGo() || counter > 800   || Math.abs(realAngleMidDeg - finishA) < 0.1 ||
//						(
//							((endA > startA && realAngleMidDeg > finishA && counter > 2)   || 
//							 (endA < startA && realAngleMidDeg < finishA && counter > 2))  &&
//							!(startA == 0  && realAngleMidDeg != 360)   && !(startA == 360 && realAngleMidDeg != 0)
//						))
				
				// TODO: Make end point dynamic. Measure the change in angle per tick, then use that for the angleOffset.
				if (!plugin.getCommander().canGo() ||
					Math.abs(realAngleMidDeg - endA)    < angleOffset     || Math.abs(realAngleMidDeg - endA)    > (360 - angleOffset  ) ||
					Math.abs(realAngleMidDeg - finishA) < angleOffset * 4 || Math.abs(realAngleMidDeg - finishA) > (360 - angleOffset * 4))
				{
					for (int idx = 0; idx < savedBlocks.size(); ++idx)
						savedBlocks.get(idx).getFBlock().setVelocity(new Vector(0D, 0D, 0D));
					finishBlocks();
					this.cancel();
				}
				else
				{
//					Bukkit.broadcastMessage(String.format("Distance to end = %.2f or %.2f", Math.abs(realAngleMidDeg - endA), Math.abs(realAngleMidDeg - endA)));
					double realRadiusEnd, totRadDiff, totradDiffPunishment;
					if (!NS)
					{
						double dXE    = Math.abs(savedBlocks.get(indexEnd).getFBlock().getLocation().getX() - (turningPoint.getBlockX() + 0.5));
						double dYE    = Math.abs(savedBlocks.get(indexEnd).getFBlock().getLocation().getY() - (turningPoint.getBlockY() + 0.5));
						realRadiusEnd = Math.sqrt(dXE * dXE + dYE * dYE);
//						if (upDown.equals(RotateDirection.DOWN))
//							totRadDiff    = doorLen - realRadiusEnd;
//						else
							totRadDiff    = realRadiusEnd - doorLen;
					}
					else
					{
						double dZE    = Math.abs(savedBlocks.get(indexEnd).getFBlock().getLocation().getZ() - (turningPoint.getBlockX() + 0.5));
						double dYE    = Math.abs(savedBlocks.get(indexEnd).getFBlock().getLocation().getY() - (turningPoint.getBlockY() + 0.5));
						realRadiusEnd = Math.sqrt(dZE * dZE + dYE * dYE);
						if (upDown.equals(RotateDirection.DOWN))
							totRadDiff    = doorLen - realRadiusEnd;
						else
							totRadDiff    = realRadiusEnd - doorLen;
					}
//					double totRadDiff     = doorLen - realRadiusEnd;
					totradDiffPunishment  = totRadDiff;
					totradDiffPunishment *= 3.77;
					totradDiffPunishment  = totradDiffPunishment > 2 ? 2 : totradDiffPunishment;
					totradDiffPunishment += 1;
//					totradDiffPunishment  = 3;

					if (NS)
						totradDiffPunishment = 3;
//					if (!NS && upDown.equals(RotateDirection.DOWN))
//						totradDiffPunishment = 13;
					
					double speedMul = 1;
					if (Math.abs(realAngleMidDeg - endA) < 15 || 
						doorLen < 6  && Math.abs(realAngleMidDeg - endA) < 30 ||
						doorLen < 10 && Math.abs(realAngleMidDeg - endA) < 15)
					{
						speedMul = 0.70;
						if (doorLen < 10)
						{
							speedMul = 0.60;
							if (upDown.equals(RotateDirection.DOWN))
								speedMul = 0.27;
						}
						totradDiffPunishment *= 1.8;
						if (doorLen < 10)
							totradDiffPunishment *= 4;
						if (!NS && upDown.equals(RotateDirection.DOWN))
						{
//							totradDiffPunishment = 4;
						}
					}
					else if (Math.abs(realAngleMidDeg - endA) > 60)
					{
//						if (!NS && upDown.equals(RotateDirection.DOWN))
//							totradDiffPunishment = 3;
					}
					if (totRadDiff > 0 && Math.abs(realAngleMidDeg - endA) < 50)
					{
						if (doorLen < 10)
							totradDiffPunishment = 14;
					}
					
					totradDiffPunishment  = totradDiffPunishment < 0.2 ? 0.2 : totradDiffPunishment;
//					Bukkit.broadcastMessage(String.format("RealRad=%.2f, Rad=%.2f, totRadDiff=%.2f, punish=%.2f", realRadiusEnd, savedBlocks.get(indexEnd).getRadius(), totRadDiff, totradDiffPunishment));
					
					double xAxis = turningPoint.getX();
					do
					{
						double zAxis = turningPoint.getZ();
						do
						{
							double radius     = savedBlocks.get(index).getRadius();
							for (double yAxis = turningPoint.getY(); yAxis <= pointOpposite.getBlockY(); ++yAxis)
							{
								// When doors are paused, make sure all blocks stop moving.
								// Set pausedBlocks to false, so the timer won't have to loop over all blocks until unpaused.
								if (plugin.getCommander().isPaused())
									savedBlocks.get(index).getFBlock().setVelocity(new Vector (0.0, 0.0, 0.0));
								else
								{	
									if (upDown.equals(RotateDirection.DOWN))
										radius    = savedBlocks.get(index).getRadius();
									if (radius != 0)
									{
										// TODO: Fuck qCircles and shit. Just use start angle and goal angle.
										// Then also use those two variable to determine if a block is lagging behind
										// Or moving a bit too enthusiastically. This knowledge can then be used to
										// Change acceleration of those blocks to make them fall in line with the rest.
										/* Pointing:   Degrees:
										 * UP            0 or 360
										 * EAST         90
										 * WEST        270
										 */
										
										realAngleMidDeg = getAngleDeg(indexMid, center);										
										
										double xPos     = savedBlocks.get(index).getFBlock().getLocation().getX();
										double yPos     = savedBlocks.get(index).getFBlock().getLocation().getY();
										double zPos     = savedBlocks.get(index).getFBlock().getLocation().getZ();
										
										// Get the real angle the door has opened so far. Subtract angle offset, as the angle should start at 0 for these calculations to work.
										double realAngleDeg = getAngleDeg(index, center);
										
										double moveAngle    = (realAngleDeg + 90) % 360;
										
										if (!NS)
										{
											// Get the actual radius of the block (and compare that to the radius it should have (stored in the block)) later (moveAndAddForGoal).
											double dX           = Math.abs(xPos - (turningPoint.getBlockX() + 0.5));
											double dY           = Math.abs(yPos - (turningPoint.getBlockY() + 0.5));
											double realRadius   = Math.sqrt(dX * dX + dY * dY);
											realRadius -= 0.08;
											if (upDown.equals(RotateDirection.UP))
												realRadius -= 0.08;
																						
											// Additional angle added to the movement direction so that the radius remains correct for all blocks.
											// TODO: Smaller total width needs lower punishment than larger doors. Presumable because of speed difference. Fix that.
											double moveAngleAddForGoal = totradDiffPunishment * -directionMultiplier * 10 * (radius - realRadius - 0.12);
		
											double speedBoost = 0;
											double behind     = 0;
											if (angleDir.equals(RotateDirection.UP))
												behind = realAngleMidDeg - realAngleDeg;
											else
												behind = realAngleDeg - realAngleMidDeg;
											// If behind is more than (-)180 ahead, it's not that far ahead, but just behind (or the other way around).
											behind = behind >  180 ? behind - 360 : behind;
											behind = behind < -180 ? behind + 360 : behind;
											
											double scaled = behind / 4;
											scaled = scaled > 1 ? 1 : scaled;
											speedBoost = 1 + scaled;
											
											speedBoost = speedBoost == 0 ? 1 : speedBoost;
											
											// Inversed zRot sign for directionMultiplier.
											double xRot = speedBoost * dirMulX * -1                   * (realRadius / maxRad) * speed * speedMul * Math.sin(Math.toRadians(moveAngle + moveAngleAddForGoal));
											double yRot = speedBoost * dirMulZ * -directionMultiplier * (realRadius / maxRad) * speed * speedMul * Math.cos(Math.toRadians(moveAngle + moveAngleAddForGoal));
											savedBlocks.get(index).getFBlock().setVelocity(new Vector (directionMultiplier * xRot, yRot, 0.000));
										}
										else
										{
											// Get the actual radius of the block (and compare that to the radius it should have (stored in the block)) later (moveAndAddForGoal).
											double dZ           = Math.abs(zPos - (turningPoint.getBlockZ() + 0.5));
											double dY           = Math.abs(yPos - (turningPoint.getBlockY() + 0.5));
											double realRadius   = Math.sqrt(dZ * dZ + dY * dY);
											realRadius -= 0.08;
											if (upDown.equals(RotateDirection.UP))
												realRadius -= 0.08;
											
											// Additional angle added to the movement direction so that the radius remains correct for all blocks.
											// TODO: Smaller total width needs lower punishment than larger doors. Presumable because of speed difference. Fix that.
											double moveAngleAddForGoal = -directionMultiplier * 10 * (radius - realRadius - 0.66);
											
											double speedBoost = 0;
											double behind     = 0;
											if (angleDir.equals(RotateDirection.UP))
												behind = realAngleMidDeg - realAngleDeg;
											else
												behind = realAngleDeg - realAngleMidDeg;
											// If behind is more than (-)180 ahead, it's not that far ahead, but just behind (or the other way around).
											behind = behind >  180 ? behind - 360 : behind;
											behind = behind < -180 ? behind + 360 : behind;
											
											double scaled = behind / 4;
											scaled = scaled > 1 ? 1 : scaled;
											speedBoost = 1 + scaled;
											
											speedBoost = speedBoost == 0 ? 1 : speedBoost;
											
											// Inversed zRot sign for directionMultiplier.
											double zRot = speedBoost * dirMulX * -1                   * (realRadius / maxRad) * speed * speedMul * Math.sin(Math.toRadians(moveAngle + moveAngleAddForGoal));
											double yRot = speedBoost * dirMulZ * -directionMultiplier * (realRadius / maxRad) * speed * speedMul * Math.cos(Math.toRadians(moveAngle + moveAngleAddForGoal));
											savedBlocks.get(index).getFBlock().setVelocity(new Vector (0.000, yRot, directionMultiplier * zRot));
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
		}.runTaskTimer(plugin, 14, 4);
	}
	
	// Rotate blocks such a logs by modifying its material data.
	public byte rotateBlockData(Byte matData)
	{
		if (!NS)
		{
			if (matData >= 0 && matData < 4)
				return (byte) (matData + 4);
			if (matData >= 4 && matData < 7)
				return (byte) (matData - 4);
			return matData;
		}
		else
		{
			if (matData >= 0 && matData < 4)
				return (byte) (matData + 8);
			if (matData >= 8 && matData < 12)
				return (byte) (matData - 8);
			return matData;
		}

	}
	
	public CustomCraftFallingBlock_Vall fallingBlockFactory(Location loc, Material mat, byte matData, World world)
	{
		return this.fabf.fallingBlockFactory(loc, mat, matData, world);
	}
}
