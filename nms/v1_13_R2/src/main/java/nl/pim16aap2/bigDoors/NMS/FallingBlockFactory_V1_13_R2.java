package nl.pim16aap2.bigDoors.NMS;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;

import net.minecraft.server.v1_13_R2.Block;
import net.minecraft.server.v1_13_R2.IBlockData;

public class FallingBlockFactory_V1_13_R2 implements FallingBlockFactory
{
    // Make a falling block.
    @Override
    public CustomCraftFallingBlock fallingBlockFactory(Location loc, NMSBlock block, byte matData, Material mat)
    {
        IBlockData blockData = ((Block) block).getBlockData();
        CustomEntityFallingBlock_V1_13_R2 fBlockNMS = new CustomEntityFallingBlock_V1_13_R2(loc.getWorld(), loc.getX(), loc.getY(), loc.getZ(), blockData);
        CustomCraftFallingBlock_V1_13_R2 entity = new CustomCraftFallingBlock_V1_13_R2(Bukkit.getServer(), fBlockNMS);
        entity.setCustomName("BigDoorsEntity");
        entity.setCustomNameVisible(false);
        return entity;
    }

    @Override
    public NMSBlock nmsBlockFactory(World world, int x, int y, int z)
    {
        return new NMSBlock_V1_13_R2(world, x, y, z);
    }
}
