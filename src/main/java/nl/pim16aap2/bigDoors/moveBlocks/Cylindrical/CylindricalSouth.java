package nl.pim16aap2.bigDoors.moveBlocks.Cylindrical;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.FallingBlock;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import nl.pim16aap2.bigDoors.BigDoors;
import nl.pim16aap2.bigDoors.util.RotateDirection;

public class CylindricalSouth extends CylindricalMover implements CylindricalMovement
{
	private BigDoors plugin;
	private World world;
	private RotateDirection rotDirection;
	private List<nl.pim16aap2.bigDoors.customEntities.CraftArmorStand> entity = new ArrayList<nl.pim16aap2.bigDoors.customEntities.CraftArmorStand>();
	private List<Material> blocks = new ArrayList<Material>();
	private List<Byte> blocksData = new ArrayList<Byte>();
	private List<FallingBlock> fBlocks = new ArrayList<FallingBlock>();
	@SuppressWarnings("unused")
	private int xMin, yMin, zMin, xMax, yMax, zMax, xLen, yLen, zLen, qCircles;
	private double speed;

	@Override
	public void moveBlockCylindrically(BigDoors plugin, World world, int qCircles, RotateDirection rotDirection, double speed, int xMin, int yMin, int zMin, int xMax, int yMax, int zMax, int xLen, int yLen, int zLen)
	{
		this.rotDirection = rotDirection;
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
		this.speed = speed;

		int index = 0;

		for (double zAxis = zMin; zAxis <= zMax; zAxis++)
		{
			for (double xAxis = xMin; xAxis <= xMax; xAxis++)
			{
				for (double yAxis = yMin; yAxis <= yMax; yAxis++)
				{
					Location newStandLocation = new Location(world, xAxis + 0.5, yAxis - 0.741, zAxis + 0.5);
					Location newFBlockLocation = new Location(world, xAxis + 0.5, yAxis - 0.02, zAxis + 0.5);

					Material mat = world.getBlockAt((int) xAxis, (int) yAxis, (int) zAxis).getType();
					@SuppressWarnings("deprecation")
					Byte matData = world.getBlockAt((int) xAxis, (int) yAxis, (int) zAxis).getData();
					blocks.add(index, mat);
					blocksData.add(index, matData);

					world.getBlockAt((int) xAxis, (int) yAxis, (int) zAxis).setType(Material.AIR);
					
					
//					double radius = index / yLen;
//					Material newMat = Material.GOLD_BLOCK;
//					switch ((int)(radius))
//					{
//					case 0:
//						newMat = Material.BEDROCK;
//						break;
//					case 1:
//						newMat = Material.DIAMOND_BLOCK;
//						break;
//					case 2:
//						newMat = Material.HAY_BLOCK;
//						break;
//					case 3:
//						newMat = Material.COAL_BLOCK;
//						break;
//					case 4:
//						newMat = Material.BONE_BLOCK;
//						break;
//					case 5:
//						newMat = Material.HAY_BLOCK;
//						break;
//					case 6:
//						newMat = Material.PURPUR_BLOCK;
//						break;
//					case 7:
//						newMat = Material.EMERALD_BLOCK;
//						break;
//					case 8:
//						newMat = Material.REDSTONE_BLOCK;
//						break;
//					}
//					world.getBlockAt((int) xAxis, (int) yAxis, (int) zAxis).setType(newMat);
					
					
					nl.pim16aap2.bigDoors.customEntities.CraftArmorStand noClipArmorStand = noClipArmorStandFactory(newStandLocation);
					
					FallingBlock fBlock = fallingBlockFactory (newFBlockLocation, mat, (byte) matData, world);
					
					noClipArmorStand.addPassenger(fBlock);
					entity.add(index, noClipArmorStand);
					fBlocks.add(index, fBlock);

					
					index++;
				}
			}
		}
		rotateEntities();
	}
	
	
	public Location getNewLocation(double xPos, double yPos, double zPos, int index)
	{
		Location oldPos = new Location(world, xPos, yPos, zPos);
		Location newPos = oldPos;

		double radius = index / yLen;

		newPos.setX(oldPos.getX() + (rotDirection == RotateDirection.CLOCKWISE ? -radius : radius));
		newPos.setZ(zMin);
		newPos.setY(oldPos.getY());
		return newPos;
	}
	

	// Put the door blocks back, but change their state now.
	@SuppressWarnings("deprecation")
	public void putBlocks()
	{
		int index = 0;
		for (double zAxis = zMin; zAxis <= zMax; zAxis++)
		{
			for (double xAxis = xMin; xAxis <= xMax; xAxis++)
			{
				for (double yAxis = yMin; yAxis <= yMax; yAxis++)
				{
					/*
					 * 0-3: Vertical oak, spruce, birch, then jungle 4-7: East/west oak, spruce,
					 * birch, jungle 8-11: North/south oak, spruce, birch, jungle 12-15: Uses oak,
					 * spruce, birch, jungle bark texture on all six faces
					 */

					Material mat = blocks.get(index);
					Byte matData = rotateBlockData(blocksData.get(index));
					
					Location newPos = getNewLocation(xAxis, yAxis, zAxis, index);

					world.getBlockAt(newPos).setType(mat);
					world.getBlockAt(newPos).setData(matData);

					fBlocks.get(index).remove();
					entity.get(index).remove();

					index++;
				}
			}
		}
		// Empty the entity arrayList.
		fBlocks.clear();
		entity.clear();
	}
	
	
	public void finishBlocks()
	{
		int index = 0;
		for (double zAxis = zMin; zAxis <= zMax; zAxis++)
		{
			for (double xAxis = xMin; xAxis <= xMax; xAxis++)
			{
				for (double yAxis = yMin; yAxis <= yMax; yAxis++)
				{
					entity.get(index).setGravity(false);
					
					// Get final position of the blocks.
					Location newPos = getNewLocation(xAxis, yAxis, zAxis, index);
					newPos.setX(newPos.getX() + 0.5);
					newPos.setY(newPos.getY() - 0.741);
					newPos.setZ(newPos.getZ() + 0.5);

					// Remove falling block from entity, teleport entity, then reaatch falling block to entity.
					entity.get(index).eject();
					entity.get(index).teleport(newPos);
					entity.get(index).addPassenger(fBlocks.get(index));
					
					++index;
				}
			}
		}
		
		new BukkitRunnable()
		{
			@Override
			public void run()
			{
				putBlocks();
			}
		}.runTaskLater(plugin, 5L);
	}
	

	@Override
	public void rotateEntities()
	{
		int directionMultiplier = rotDirection == RotateDirection.CLOCKWISE ? -1 : 1;
		double divider = getDivider(zLen);
		double radDiv = divider / speed;
		double additionalTurn = 0.07;
		
		Location center = new Location(world, xMin, yMin, zMax);

		new BukkitRunnable()
		{
			double angle = 0.0D;
			final int ticksPerCircle = (int) (2800 / speed); // How many ticks go into 1 circle (20 ticks = 1 second).
			final double step = ((2 * Math.PI) / ticksPerCircle);
			int eights = 1;
			boolean replace = false;

			@Override
			public void run()
			{
				// If the angle equals or exceeds 1.5808 rad (90 degrees) multiplied by the
				// amount of quarter circles it should turn, stop.
				if (Math.abs(angle) >= 1.5708 * qCircles + additionalTurn)
				{
					this.cancel();
					finishBlocks();
//					putBlocks();
				}
				int index = 0;

				// Starting at 1/8 circle, every 1/4 circle.
				if (Math.abs(angle) / (8 * eights) >= 0.1)
				{
					eights += 2;
					replace = true;
				}
				
				// Loop up and down first.
				for (double zAxis = zMin; zAxis <= zMax; zAxis++)
				{
					for (double xAxis = xMin; xAxis <= xMax; xAxis++)
					{
						for (double yAxis = yMin; yAxis <= yMax; yAxis++)
						{
							angle -= step;
							if (Math.abs(angle) <= 1.5708 * qCircles + additionalTurn)
							{
								double realAngle = Math.abs(Math.atan2(center.getX() - entity.get(index).getLocation().getX(), center.getZ() - entity.get(index).getLocation().getZ()) - 0.11);
//								Bukkit.broadcastMessage("Angle = " + angle + ", Real angle = " + realAngle + ", index = " + index);
								
								// Set the gravity stat of the armor stand to true, so it can move again.
								entity.get(index).setGravity(true);
								// Get the radius of the current blockEntity.
//								double radius = zLen - index % zLen - 1;
								double radius = index / yLen;
								// Set the x and z accelerations.
								double xRot = Math.cos(angle) * radius / radDiv;
								double zRot = Math.sin(angle) * radius / radDiv;
								entity.get(index).setVelocity(new Vector(directionMultiplier * xRot, 0.002, zRot));

								Material mat = blocks.get(index);

								if (replace && (mat == Material.LOG || mat == Material.LOG_2))
								{
									Location loc = fBlocks.get(index).getLocation();
									Byte matData = rotateBlockData(blocksData.get(index));
									fBlocks.get(index).remove();
									
									FallingBlock fBlock = fallingBlockFactory (loc, mat, (byte) matData, world);
									fBlocks.set(index, fBlock);
									entity.get(index).addPassenger(fBlock);
								} else
								{
									fBlocks.get(index).setTicksLived(1);
								}
							}
							index++;
						}
					}
				}
				replace = false;
			}
		}.runTaskTimer(plugin, 14, 1);
	}
}
