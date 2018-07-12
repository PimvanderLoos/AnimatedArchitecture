package nl.pim16aap2.bigDoors.customEntities;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;

public interface FallingBlockFactory_Vall
{
	public CustomCraftFallingBlock_Vall fallingBlockFactory(Location loc, Material mat, byte matData, World world);
}