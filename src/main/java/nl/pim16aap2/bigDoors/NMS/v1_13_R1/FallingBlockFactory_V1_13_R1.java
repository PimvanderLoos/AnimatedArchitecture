package nl.pim16aap2.bigDoors.NMS.v1_13_R1;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;

import net.minecraft.server.v1_13_R1.IBlockData;
import nl.pim16aap2.bigDoors.NMS.CustomCraftFallingBlock_Vall;
import nl.pim16aap2.bigDoors.NMS.FallingBlockFactory_Vall;
import nl.pim16aap2.bigDoors.NMS.NMSBlock_Vall;

public class FallingBlockFactory_V1_13_R1 implements FallingBlockFactory_Vall
{
	// Make a falling block.
	public CustomCraftFallingBlock_Vall fallingBlockFactory(Location loc, NMSBlock_Vall block, byte matData, Material mat)
	{
		IBlockData blockData = block.getBlockData();
		CustomEntityFallingBlock_V1_13_R1 fBlockNMS = new CustomEntityFallingBlock_V1_13_R1(loc.getWorld(), loc.getX(), loc.getY(), loc.getZ(), blockData);
		return new CustomCraftFallingBlock_V1_13_R1(Bukkit.getServer(), fBlockNMS);
	}

	@Override
	public NMSBlock_Vall nmsBlockFactory(World world, int x, int y, int z)
	{
		return new NMSBlock_V1_13_R1(world, x, y, z);
	}
}
