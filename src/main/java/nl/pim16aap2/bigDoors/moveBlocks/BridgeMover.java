package nl.pim16aap2.bigDoors.moveBlocks;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Entity;
import org.bukkit.material.MaterialData;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import nl.pim16aap2.bigDoors.BigDoors;
import nl.pim16aap2.bigDoors.Door;
import nl.pim16aap2.bigDoors.NMS.CustomCraftFallingBlock_Vall;
import nl.pim16aap2.bigDoors.NMS.FallingBlockFactory_Vall;
import nl.pim16aap2.bigDoors.NMS.NMSBlock_Vall;
import nl.pim16aap2.bigDoors.moveBlocks.Bridge.getNewLocation.GetNewLocation;
import nl.pim16aap2.bigDoors.moveBlocks.Bridge.getNewLocation.GetNewLocationEast;
import nl.pim16aap2.bigDoors.moveBlocks.Bridge.getNewLocation.GetNewLocationNorth;
import nl.pim16aap2.bigDoors.moveBlocks.Bridge.getNewLocation.GetNewLocationSouth;
import nl.pim16aap2.bigDoors.moveBlocks.Bridge.getNewLocation.GetNewLocationWest;
import nl.pim16aap2.bigDoors.util.DoorDirection;
import nl.pim16aap2.bigDoors.util.MyBlockData;
import nl.pim16aap2.bigDoors.util.RotateDirection;
import nl.pim16aap2.bigDoors.util.Util;

public class BridgeMover implements BlockMover
{
	private World                    world;
	private BigDoors                plugin;
	private int                   tickRate;	
	private double              multiplier;
	private int                     dx, dz;
	private double                    time;
	private FallingBlockFactory_Vall  fabf;
	private boolean                     NS;
	private GetNewLocation             gnl;
	private Door                      door;
	private RotateDirection         upDown;
	private DoorDirection       engineSide;
	private double              endStepSum;
	private boolean            instantOpen;
	private Location          turningPoint;
	private double            startStepSum;
	private DoorDirection    openDirection;
	private Location         pointOpposite;
	private int             stepMultiplier;
	private int           xMin, yMin, zMin;
	private int           xMax, yMax, zMax;
	private List<MyBlockData> savedBlocks = new ArrayList<MyBlockData>();
	
	@SuppressWarnings("deprecation")
	public BridgeMover(BigDoors plugin, World world, double time, Door door, RotateDirection upDown, 
			DoorDirection openDirection, boolean instantOpen)
	{
		this.door          = door;
		this.fabf          = plugin.getFABF();
		this.world         = world;
		this.plugin        = plugin;
		this.engineSide    = door.getEngSide();
		this.NS            = engineSide == DoorDirection.NORTH || engineSide == DoorDirection.SOUTH;
		this.instantOpen   = instantOpen;
		this.openDirection = openDirection;
		this.upDown        = upDown;
				
		this.xMin       = door.getMinimum().getBlockX();
		this.yMin       = door.getMinimum().getBlockY();
		this.zMin       = door.getMinimum().getBlockZ();
		this.xMax       = door.getMaximum().getBlockX();
		this.yMax       = door.getMaximum().getBlockY();
		this.zMax       = door.getMaximum().getBlockZ();
		int xLen        = Math.abs(door.getMaximum().getBlockX() - door.getMinimum().getBlockX());
		int yLen        = Math.abs(door.getMaximum().getBlockY() - door.getMinimum().getBlockY());
		int zLen        = Math.abs(door.getMaximum().getBlockZ() - door.getMinimum().getBlockZ());
		int doorSize    = Math.max(xLen, Math.max(yLen, zLen)) + 1;
		double vars[]   = Util.calculateTimeAndTickRate(doorSize, time, plugin.getConfigLoader().getDouble("dbMultiplier"), 5.2);
		this.time       = vars[0];
		this.tickRate   = (int) vars[1];
		this.multiplier = vars[2];
		
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
		this.startStepSum   = -1;
		this.stepMultiplier = -1;
		
		// Calculate turningpoint and pointOpposite.
		switch (engineSide)
		{
		case NORTH:
			// When EngineSide is North, x goes from low to high and z goes from low to high
			this.turningPoint = new Location(world, xMin, yMin, zMin);
			this.dx =  1;
			this.dz =  1;
			
			if (upDown.equals(RotateDirection.UP))
			{
				this.pointOpposite  = new Location(world, xMax, yMin, zMax);
				this.startStepSum   =  Math.PI / 2;
				this.stepMultiplier = -1;
			}
			else
			{
				this.pointOpposite  = new Location(world, xMax, yMax, zMin);
				if (openDirection.equals(DoorDirection.NORTH))
					this.stepMultiplier = -1;
				else if (openDirection.equals(DoorDirection.SOUTH))
					this.stepMultiplier =  1;
			}
			break;
			
		case SOUTH:
			// When EngineSide is South, x goes from high to low and z goes from high to low
			this.turningPoint = new Location(world, xMax, yMin, zMax);
			this.dx = -1;
			this.dz = -1;
			
			if (upDown.equals(RotateDirection.UP))
			{
				this.pointOpposite  = new Location(world, xMin, yMin, zMin);
				this.startStepSum   = -Math.PI / 2;
				this.stepMultiplier =  1;
			}
			else
			{
				this.pointOpposite  = new Location(world, xMin, yMax, zMax);
				if (openDirection.equals(DoorDirection.NORTH))
					this.stepMultiplier = -1;
				else if (openDirection.equals(DoorDirection.SOUTH))
					this.stepMultiplier =  1;
			}
			break;
			
		case EAST:
			// When EngineSide is East, x goes from high to low and z goes from low to high
			this.turningPoint = new Location(world, xMax, yMin, zMin);
			this.dx = -1;
			this.dz =  1;
			
			if (upDown.equals(RotateDirection.UP))
			{
				this.pointOpposite  = new Location(world, xMin, yMin, zMax);
				this.startStepSum   = -Math.PI / 2;
				this.stepMultiplier =  1;
			}
			else
			{
				this.pointOpposite  = new Location(world, xMax, yMax, zMax);
				if (openDirection.equals(DoorDirection.EAST))
					this.stepMultiplier =  1;
				else if (openDirection.equals(DoorDirection.WEST))
					this.stepMultiplier = -1;
			}
			break;
			
		case WEST:
			// When EngineSide is West, x goes from low to high and z goes from high to low
			this.turningPoint = new Location(world, xMin, yMin, zMax);
			this.dx =  1;
			this.dz = -1;
			
			if (upDown.equals(RotateDirection.UP))
			{	
				this.pointOpposite  = new Location(world, xMax, yMin, zMin);
				this.startStepSum   =  Math.PI / 2;
				this.stepMultiplier = -1;
			}
			else
			{
				this.pointOpposite  = new Location(world, xMin, yMax, zMin);
				if (openDirection.equals(DoorDirection.EAST))
					this.stepMultiplier =  1;
				else if (openDirection.equals(DoorDirection.WEST))
					this.stepMultiplier = -1;
			}
			break;
		}

		this.endStepSum   = upDown.equals(RotateDirection.UP)   ? 0 : Math.PI / 2 * this.stepMultiplier;
		this.startStepSum = upDown.equals(RotateDirection.DOWN) ? 0 : this.startStepSum;
		
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
					if (upDown == RotateDirection.DOWN)
						radius = yAxis - turningPoint.getBlockY();
					
					Location newFBlockLocation = new Location(world, xAxis + 0.5, yAxis - 0.020, zAxis + 0.5);
					// Move the lowest blocks up a little, so the client won't predict they're touching through the ground, 
					// which would make them slower than the rest.
					if (yAxis == yMin)
						newFBlockLocation.setY(newFBlockLocation.getY() + .010001);
					
					Material mat  = world.getBlockAt((int) xAxis, (int) yAxis, (int) zAxis).getType();
					Byte matData  = world.getBlockAt((int) xAxis, (int) yAxis, (int) zAxis).getData();
					BlockState bs = world.getBlockAt((int) xAxis, (int) yAxis, (int) zAxis).getState();
					MaterialData materialData = bs.getData();
					NMSBlock_Vall block  = this.fabf.nmsBlockFactory(world, (int) xAxis, (int) yAxis, (int) zAxis);
					NMSBlock_Vall block2 = null;
					
					int canRotate = 0;
					Byte matByte  = 0;
					// Certain blocks cannot be used the way normal blocks can (heads, (ender) chests etc).
					if (Util.isAllowedBlock(mat))
					{
						canRotate        = Util.canRotate(mat);
						// Because I can't get the blocks to rotate properly, they are rotated here
						if (canRotate == 1 || canRotate == 2 || canRotate == 3)
						{
							Location pos = new Location(world, (int) xAxis, (int) yAxis, (int) zAxis);
							matByte      = rotateBlockData(matData);
							Block b      = world.getBlockAt(pos);					
							materialData.setData(matByte);
							
							if (plugin.is1_13())
							{
								b.setType(mat);
								BlockState bs2 = b.getState();
								bs2.setData(materialData);
								bs2.update();
								block2 = this.fabf.nmsBlockFactory(world, (int) xAxis, (int) yAxis, (int) zAxis);
							}
						}
						world.getBlockAt((int) xAxis, (int) yAxis, (int) zAxis).setType(Material.AIR);
					}
					else
					{
						mat     = Material.AIR;
						matData = 0;
					}
					
					CustomCraftFallingBlock_Vall fBlock = null;
					if (!instantOpen)
						 fBlock = fallingBlockFactory(newFBlockLocation, mat, matData, block);
						
					savedBlocks.add(index, new MyBlockData(mat, matByte, fBlock, radius, materialData, block2 == null ? block : block2, canRotate, -1));
					
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
		
		if (!instantOpen)
			rotateEntities();
		else
			putBlocks(false);
	}

	// Put the door blocks back, but change their state now.
	@SuppressWarnings("deprecation")
	@Override
	public void putBlocks(boolean onDisable)
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
					
					matByte = savedBlocks.get(index).getBlockByte();

					Location newPos = gnl.getNewLocation(savedBlocks.get(index).getRadius(), xAxis, yAxis, zAxis, index);

					if (!instantOpen)
						savedBlocks.get(index).getFBlock().remove();

					if (!savedBlocks.get(index).getMat().equals(Material.AIR))
					{
						if (plugin.is1_13())
						{						
							savedBlocks.get(index).getBlock().putBlock(newPos);
							Block b = world.getBlockAt(newPos);
							BlockState bs = b.getState();
							bs.update();
						}
						else
						{
							Block b = world.getBlockAt(newPos);					
							MaterialData matData = savedBlocks.get(index).getMatData();
							matData.setData(matByte);
							
							b.setType(mat);
							BlockState bs = b.getState();
							bs.setData(matData);
							bs.update();
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
		savedBlocks.clear();

		// Tell the door object it has been opened and what its new coordinates are.
		updateCoords(door, this.openDirection, this.upDown, -1);
		toggleOpen  (door);
		if (!onDisable)
			plugin.removeBlockMover(this);
		
		// Change door availability to true, so it can be opened again.
		// Wait for a bit if instantOpen is enabled.
		int timer = onDisable   ?  0 : 
			        instantOpen ? 40 : plugin.getConfigLoader().getInt("coolDown") * 20;
		
		if (timer > 0)
		{
			new BukkitRunnable()
			{
				@Override
				public void run()
				{
					plugin.getCommander().setDoorAvailable(door.getDoorUID());
				}
			}.runTaskLater(plugin, timer);
		}
		else
			plugin.getCommander().setDoorAvailable(door.getDoorUID());
		
		if (!onDisable)
			goAgain();
	}
	
	private void goAgain()
	{
		int autoCloseTimer = door.getAutoClose();
		if (autoCloseTimer < 0 || !door.isOpen())
			return;
		
		new BukkitRunnable()
		{
			@Override
			public void run()
			{
				plugin.getDoorOpener(door.getType()).openDoor(plugin.getCommander().getDoor(door.getDoorUID()), time, instantOpen, false);
			}
		}.runTaskLater(plugin, autoCloseTimer * 20);
	}
	
	// Method that takes care of the rotation aspect.
	private void rotateEntities()
	{
		new BukkitRunnable()
		{
			Location center   = new Location(world, turningPoint.getBlockX() + 0.5, yMin, turningPoint.getBlockZ() + 0.5);
			boolean replace   = false;
			int counter       = 0;
			int endCount      = (int) (20 / tickRate * time);
			double step       = (Math.PI / 2) / endCount * stepMultiplier;
			double stepSum    = startStepSum;
			int totalTicks    = (int) (endCount * multiplier);
			int replaceCount  = (int) (endCount / 2);
			
			@Override
			public void run()
			{
				if (counter == 0 || (counter < endCount - 45 / tickRate && counter % (6 * tickRate / 4) == 0))
					Util.playSound(door.getEngine(), "bd.drawbridge-rattling", 0.8f, 0.7f);
				
				if (!plugin.getCommander().isPaused())
					++counter;
				
				if (counter < endCount - 1)
					stepSum = startStepSum + step * counter;
				else 
					stepSum = endStepSum;
								
				replace = false;
				if (counter == replaceCount)
					replace = true;
				
				if (!plugin.getCommander().canGo() || !door.canGo() || counter > totalTicks)
				{					
					Util.playSound(door.getEngine(), "bd.thud", 2f, 0.15f);
					for (int idx = 0; idx < savedBlocks.size(); ++idx)
						savedBlocks.get(idx).getFBlock().setVelocity(new Vector(0D, 0D, 0D));
					putBlocks(false);
					this.cancel();
				}
				else
				{
					for (MyBlockData block : savedBlocks)
					{
						double radius = block.getRadius();
						if (replace)
						{
							if (block.canRot() != 0 && block.canRot() != 4)
							{
								Material mat = block.getMat();
								Location loc = block.getFBlock().getLocation();
								Byte matData = block.getBlockByte();
								Vector veloc = block.getFBlock().getVelocity();
								
								CustomCraftFallingBlock_Vall fBlock;
								// Because the block in savedBlocks is already rotated where applicable, just use that block now.
								fBlock = fallingBlockFactory(loc, mat, (byte) matData, block.getBlock());
								
								block.getFBlock().remove();
								block.setFBlock(fBlock);
								block.getFBlock().setVelocity(veloc);
							}
						}
						if (radius != 0)
						{
							double posX, posY, posZ;
							posY = center.getY() + radius * Math.cos(stepSum);
							if (!NS)
							{
								posX = center.getX() + radius * Math.sin(stepSum);
								posZ = block.getFBlock().getLocation().getZ();
							}
							else
							{
								posX = block.getFBlock().getLocation().getX();
								posZ = center.getZ() + radius * Math.sin(stepSum);
							}
							Location loc = new Location(null, posX, posY, posZ);
							Vector vec   = loc.toVector().subtract(block.getFBlock().getLocation().toVector());
							vec.multiply(0.101);
							block.getFBlock().setVelocity(vec);
						}
					}
				}
			}
		}.runTaskTimer(plugin, 14, tickRate);
	}
	
	// Rotate blocks such a logs by modifying its material data.
	private byte rotateBlockData(Byte matData)
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

	// Toggle the open status of a drawbridge.
	private void toggleOpen(Door door)
	{
		door.setOpenStatus(!door.isOpen());
	}

	// Update the coordinates of a door based on its location, direction it's pointing in and rotation direction.
	private void updateCoords(Door door, DoorDirection openDirection, RotateDirection upDown, int moved)
	{
		int xMin = door.getMinimum().getBlockX();
		int yMin = door.getMinimum().getBlockY();
		int zMin = door.getMinimum().getBlockZ();
		int xMax = door.getMaximum().getBlockX();
		int yMax = door.getMaximum().getBlockY();
		int zMax = door.getMaximum().getBlockZ();
		int xLen = xMax - xMin;
		int yLen = yMax - yMin;
		int zLen = zMax - zMin;
		Location newMax = null;
		Location newMin = null;
		DoorDirection newEngSide = door.getEngSide();
		
		switch (openDirection)
		{
		case NORTH:
			if (upDown == RotateDirection.UP)
			{
				newEngSide = DoorDirection.NORTH;
				newMin = new Location(door.getWorld(), xMin, yMin,        zMin);
				newMax = new Location(door.getWorld(), xMax, yMin + zLen, zMin);
			} 
			else
			{
				newEngSide = DoorDirection.SOUTH;
				newMin = new Location(door.getWorld(), xMin, yMin, zMin - yLen);
				newMax = new Location(door.getWorld(), xMax, yMin, zMin       );
			}
			break;
			
			
		case EAST:
			if (upDown == RotateDirection.UP)
			{
				newEngSide = DoorDirection.EAST;
				newMin = new Location(door.getWorld(), xMax, yMin,        zMin);
				newMax = new Location(door.getWorld(), xMax, yMin + xLen, zMax);
			} 
			else
			{
				newEngSide = DoorDirection.WEST;
				newMin = new Location(door.getWorld(), xMax,        yMin, zMin);
				newMax = new Location(door.getWorld(), xMax + yLen, yMin, zMax);
			}
			break;
			
			
		case SOUTH:
			if (upDown == RotateDirection.UP)
			{
				newEngSide = DoorDirection.SOUTH;
				newMin = new Location(door.getWorld(), xMin, yMin,        zMax);
				newMax = new Location(door.getWorld(), xMax, yMin + zLen, zMax);
			} 
			else
			{
				newEngSide = DoorDirection.NORTH;
				newMin = new Location(door.getWorld(), xMin, yMin, zMax       );
				newMax = new Location(door.getWorld(), xMax, yMin, zMax + yLen);
			}
			break;
			
			
		case WEST:
			if (upDown == RotateDirection.UP)
			{
				newEngSide = DoorDirection.WEST;
				newMin = new Location(door.getWorld(), xMin, yMin,        zMin);
				newMax = new Location(door.getWorld(), xMin, yMin + xLen, zMax);
			} 
			else
			{
				newEngSide = DoorDirection.EAST;
				newMin = new Location(door.getWorld(), xMin - yLen, yMin, zMin);
				newMax = new Location(door.getWorld(), xMin,        yMin, zMax);
			}
			break;
		}
		door.setMaximum(newMax);
		door.setMinimum(newMin);
		door.setEngineSide(newEngSide);

		plugin.getCommander().updateDoorCoords(door.getDoorUID(), !door.isOpen(), newMin.getBlockX(), newMin.getBlockY(), newMin.getBlockZ(), newMax.getBlockX(), newMax.getBlockY(), newMax.getBlockZ(), newEngSide);
	}

	private CustomCraftFallingBlock_Vall fallingBlockFactory(Location loc, Material mat, byte matData, NMSBlock_Vall block)
	{
		CustomCraftFallingBlock_Vall entity = this.fabf.fallingBlockFactory(loc, block, matData, mat);
		Entity bukkitEntity = (Entity) entity;
		bukkitEntity.setCustomName("BigDoorsEntity");
		bukkitEntity.setCustomNameVisible(false);
		return entity;
	}

	
	@Override
	public long getDoorUID()
	{
		return this.door.getDoorUID();
	}

	@Override
	public Door getDoor()
	{
		return this.door;
	}
}