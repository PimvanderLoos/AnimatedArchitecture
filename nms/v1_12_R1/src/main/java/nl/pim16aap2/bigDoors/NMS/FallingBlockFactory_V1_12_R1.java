package nl.pim16aap2.bigDoors.NMS;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;

public class FallingBlockFactory_V1_12_R1 implements FallingBlockFactory
{
    // Make a falling block.
    @Override
    public CustomCraftFallingBlock fallingBlockFactory(Location loc, NMSBlock block, byte matData, Material mat)
    {
        CustomEntityFallingBlock_V1_12_R1 fBlockNMS = new CustomEntityFallingBlock_V1_12_R1(loc.getWorld(), mat, loc.getX(), loc.getY(), loc.getZ(), matData);
        CustomCraftFallingBlock_V1_12_R1 entity = new CustomCraftFallingBlock_V1_12_R1(Bukkit.getServer(), fBlockNMS);
        entity.setCustomName("BigDoorsEntity");
        entity.setCustomNameVisible(false);
        return entity;
    }

    @Override
    public NMSBlock nmsBlockFactory(World world, int x, int y, int z)
    {
        return null;
    }
}
