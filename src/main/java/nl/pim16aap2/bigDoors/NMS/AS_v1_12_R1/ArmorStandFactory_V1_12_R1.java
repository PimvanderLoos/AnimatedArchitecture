package nl.pim16aap2.bigDoors.NMS.AS_v1_12_R1;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import nl.pim16aap2.bigDoors.NMS.CustomCraftFallingBlock_Vall;
import nl.pim16aap2.bigDoors.NMS.FallingBlockFactory_Vall;
import nl.pim16aap2.bigDoors.NMS.NMSBlock_Vall;

public class ArmorStandFactory_V1_12_R1 implements FallingBlockFactory_Vall
{

	@Override
	public CustomCraftFallingBlock_Vall fallingBlockFactory(Location loc, NMSBlock_Vall block, byte matData, Material mat)
	{
		loc.setY(loc.getY() - 1);
		NoClipArmorStand_V1_12_R1 noClipArmorStandTemp = new NoClipArmorStand_V1_12_R1((org.bukkit.craftbukkit.v1_12_R1.CraftWorld) loc.getWorld(), loc);
		((org.bukkit.craftbukkit.v1_12_R1.CraftWorld) loc.getWorld()).getHandle().addEntity(noClipArmorStandTemp, SpawnReason.CUSTOM);

		noClipArmorStandTemp.setInvisible(true);
		noClipArmorStandTemp.setSmall(false);

		CraftArmorStand_V1_12_R1 noClipArmorStand = new CraftArmorStand_V1_12_R1((org.bukkit.craftbukkit.v1_12_R1.CraftServer) (Bukkit.getServer()), noClipArmorStandTemp);

		noClipArmorStand.setVelocity  (new Vector(0, 0, 0));
		noClipArmorStand.setCollidable(false);
		noClipArmorStand.setHelmet(new ItemStack(mat, 1));
		
		return (CustomCraftFallingBlock_Vall) noClipArmorStand;
	}

	@Override
	public NMSBlock_Vall nmsBlockFactory(World world, int x, int y, int z)
	{
		return null;
	}
}
