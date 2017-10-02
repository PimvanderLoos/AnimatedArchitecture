package nl.pim16aap2.bigDoors.moveBlocks.Cylindrical;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.FallingBlock;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.bukkit.util.Vector;

import nl.pim16aap2.bigDoors.customEntities.NoClipArmorStand;

public class CylindricalMover
{
	// Figure out the divider for the width.
	public double getDivider(int len)
	{
		double divider = 1;
		switch (len - 1)
		{
		case 1:
			divider = 38;
			break;
		case 2:
			divider = 30;
			break;
		case 3:
			divider = 23;
			break;
		case 4:
			divider = 18;
			break;
		case 5:
			divider = 15;
			break;
		case 6:
			divider = 13;
			break;
		case 7:
			divider = 5.5;
			break;
		case 8:
			divider = 10;
			break;
		case 9:
			divider = 8.8;
			break;
		case 10:
			divider = 8.2;
			break;
		case 11:
			divider = 7.3;
			break;
		case 12:
			divider = 6.8;
			break;
		case 13:
			divider = 6.4;
			break;
		case 14:
			divider = 6.0;
			break;
		case 15:
			divider = 5.6;
			break;
		case 16:
			divider = 5.2;
			break;
		case 17:
			divider = 4.9;
			break;
		case 18:
			divider = 4.7;
			break;
		case 19:
			divider = 38;
			break;
		case 20:
			divider = 38;
			break;
		case 21:
			divider = 38;
			break;
		case 22:
			divider = 38;
			break;
		case 23:
			divider = 38;
			break;
		case 24:
			divider = 38;
			break;
		case 25:
			divider = 38;
			break;
		case 26:
			divider = 38;
			break;
		}
		return divider;
	}
	
	// Rotate blocks such a logs by modifying its material data.
	public byte rotateBlockData(Byte matData)
	{
		if (matData >= 4 && matData <= 7)
		{
			matData = (byte) (matData + 4);
		} else if (matData >= 7 && matData <= 11)
		{
			matData = (byte) (matData - 4);
		}
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
