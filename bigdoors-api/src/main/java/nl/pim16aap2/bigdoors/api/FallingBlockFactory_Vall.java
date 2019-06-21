package nl.pim16aap2.bigdoors.api;

import org.bukkit.Location;
import org.bukkit.World;

public interface FallingBlockFactory_Vall
{
    public CustomCraftFallingBlock_Vall fallingBlockFactory(Location loc, NMSBlock_Vall block);

    public NMSBlock_Vall nmsBlockFactory(World world, int x, int y, int z);
}