package nl.pim16aap2.bigdoors.nms;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;

import nl.pim16aap2.bigdoors.BigDoors;

public interface FallingBlockFactory_Vall
{
    public CustomCraftFallingBlock_Vall fallingBlockFactory(BigDoors plugin, Location loc, NMSBlock_Vall block, Material mat);

    public NMSBlock_Vall nmsBlockFactory(World world, int x, int y, int z);
}