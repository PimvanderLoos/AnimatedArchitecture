package nl.pim16aap2.bigDoors.moveBlocks.Cylindrical;

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

public class CylindricalWest extends CylindricalMover implements CylindricalMovement
{

	private BigDoors plugin;
	private World world;
	private String direction;
	private List<nl.pim16aap2.bigDoors.customEntities.CraftArmorStand> entity = new ArrayList<nl.pim16aap2.bigDoors.customEntities.CraftArmorStand>();
	private List<Material> blocks = new ArrayList<Material>();
	private List<Byte> blocksData = new ArrayList<Byte>();
	private List<FallingBlock> fBlocks = new ArrayList<FallingBlock>();
	@SuppressWarnings("unused")
	private int xMin, yMin, zMin, xMax, yMax, zMax, xLen, yLen, zLen, qCircles;
	private double speed;

	@Override
	public void moveBlockCylindrically(BigDoors plugin, World world, int qCircles, String direction, double speed,
			int xMin, int yMin, int zMin, int xMax, int yMax, int zMax, int xLen, int yLen, int zLen)
	{
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
		this.speed = speed;

		int index = 0;

		for (double yAxis = yMin; yAxis <= yMax; yAxis++)
		{
			for (double xAxis = xMin; xAxis <= xMax; xAxis++)
			{
				for (double zAxis = zMin; zAxis <= zMax; zAxis++)
				{
					Location newStandLocation = new Location(world, xAxis + 0.5, yAxis - 0.741, zAxis + 0.5);
					Location newFBlockLocation = new Location(world, xAxis + 0.5, yAxis - 0.02, zAxis + 0.5);

					Material item = world.getBlockAt((int) xAxis, (int) yAxis, (int) zAxis).getType();
					@SuppressWarnings("deprecation")
					Byte itemData = world.getBlockAt((int) xAxis, (int) yAxis, (int) zAxis).getData();
					blocks.add(index, item);
					blocksData.add(index, itemData);

					world.getBlockAt((int) xAxis, (int) yAxis, (int) zAxis).setType(Material.AIR);

					NoClipArmorStand noClipArmorStandTemp = new NoClipArmorStand(
							(org.bukkit.craftbukkit.v1_11_R1.CraftWorld) newStandLocation.getWorld(), newStandLocation);
					((org.bukkit.craftbukkit.v1_11_R1.CraftWorld) newStandLocation.getWorld()).getHandle()
							.addEntity(noClipArmorStandTemp, SpawnReason.CUSTOM);

					noClipArmorStandTemp.setInvisible(true);
					noClipArmorStandTemp.setSmall(true);

					nl.pim16aap2.bigDoors.customEntities.CraftArmorStand noClipArmorStand = new nl.pim16aap2.bigDoors.customEntities.CraftArmorStand(
							(org.bukkit.craftbukkit.v1_11_R1.CraftServer) (Bukkit.getServer()), noClipArmorStandTemp);

					noClipArmorStand.setVelocity(new Vector(0, 0, 0));
					noClipArmorStand.setGravity(false);
					noClipArmorStand.setCollidable(false);

					@SuppressWarnings("deprecation")
					FallingBlock fBlock = world.spawnFallingBlock(newFBlockLocation, item, (byte) itemData);
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
	@SuppressWarnings("deprecation")
	public void putBlocks()
	{
		int index = 0;
		for (double yAxis = yMin; yAxis <= yMax; yAxis++)
		{
			for (double xAxis = xMin; xAxis <= xMax; xAxis++)
			{
				for (double zAxis = zMin; zAxis <= zMax; zAxis++)
				{
					/*
					 * 0-3: Vertical oak, spruce, birch, then jungle 4-7: East/west oak, spruce,
					 * birch, jungle 8-11: North/south oak, spruce, birch, jungle 12-15: Uses oak,
					 * spruce, birch, jungle bark texture on all six faces
					 */

					Material mat = blocks.get(index);
					Byte matData = blocksData.get(index);
					if (matData >= 4 && matData <= 7)
					{
						matData = (byte) (matData + 4);
					} else if (matData >= 7 && matData <= 11)
					{
						matData = (byte) (matData - 4);
					}

					Location oldPos = new Location(world, xAxis, yAxis, zAxis);
					Location newPos = oldPos;

					double radius = xLen - index % xLen - 1;

					newPos.setZ(oldPos.getZ() + (direction == "clockwise" ? -radius : radius));
					newPos.setX(xMax);
					newPos.setY(oldPos.getY());

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

	@Override
	public void rotateEntities()
	{

		int directionMultiplier = direction == "clockwise" ? -1 : 1;
		double divider = getDivider(xLen);
		double radDiv = divider / speed;
		double additionalTurn = 0.07;

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
					putBlocks();
				}
				int index = 0;

				// Starting at 1/8 circle, every 1/4 circle.
				if (Math.abs(angle) / (8 * eights) >= 0.1)
				{
					eights += 2;
					replace = true;
				}

				// Loop up and down first.
				for (double yAxis = yMin; yAxis <= yMax; yAxis++)
				{
					for (double xAxis = xMin; xAxis <= xMax; xAxis++)
					{
						for (double zAxis = zMin; zAxis <= zMax; zAxis++)
						{
							angle += -(step * directionMultiplier);
							if (Math.abs(angle) <= 1.5708 * qCircles + additionalTurn)
							{
								// Set the gravity stat of the armor stand to true, so it can move again.
								entity.get(index).setGravity(true);
								// Get the radius of the current blockEntity.
								double radius = xLen - index % xLen - 1;
								// Set the x and z accelerations.
								double xRot = -Math.sin(angle) * radius / radDiv * Math.sin(1.5808);
								double zRot = -directionMultiplier * Math.cos(angle) * radius / radDiv;
								entity.get(index).setVelocity(new Vector(directionMultiplier * xRot, 0.002, zRot));

								Material mat = blocks.get(index);

								if (replace && (mat == Material.LOG || mat == Material.LOG_2))
								{
									Location loc = fBlocks.get(index).getLocation();
									Byte matData = blocksData.get(index);

									if (matData >= 4 && matData <= 11 && (mat == Material.LOG || mat == Material.LOG_2))
									{
										matData = (byte) (matData + (matData <= 7 ? 4 : -4));
									}
									fBlocks.get(index).remove();

									@SuppressWarnings("deprecation")
									FallingBlock fBlock = world.spawnFallingBlock(loc, mat, (byte) matData);
									fBlock.setVelocity(new Vector(0, 0, 0));
									fBlock.setDropItem(false);
									fBlock.setGravity(false);
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
