package nl.pim16aap2.bigDoors.customEntities.v1_11_R1;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;

import nl.pim16aap2.bigDoors.customEntities.CustomCraftFallingBlock_Vall;
import nl.pim16aap2.bigDoors.customEntities.FallingBlockFactory_Vall;

public class FallingBlockFactory_V1_11_R1 implements FallingBlockFactory_Vall
{
	// Make a falling block.
	public CustomCraftFallingBlock_Vall fallingBlockFactory(Location loc, Material mat, byte matData, World world)
	{
		CustomEntityFallingBlock_V1_11_R1 fBlockNMS = new CustomEntityFallingBlock_V1_11_R1(world, mat, loc.getX(), loc.getY(), loc.getZ(), (byte) matData);
		return new CustomCraftFallingBlock_V1_11_R1(Bukkit.getServer(), fBlockNMS);
	}
}
