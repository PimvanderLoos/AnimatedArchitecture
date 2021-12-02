package nl.pim16aap2.bigDoors.NMS;

import net.minecraft.core.BlockPosition;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.BlockBase.Info;
import net.minecraft.world.level.block.state.IBlockData;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_18_R1.CraftWorld;

public class FallingBlockFactory_V1_18_R1 implements FallingBlockFactory
{
    @Override
    public CustomCraftFallingBlock fallingBlockFactory(Location loc, NMSBlock block, byte matData, Material mat)
    {
        IBlockData blockData = ((NMSBlock_V1_18_R1) block).getMyBlockData();
        CustomEntityFallingBlock_V1_18_R1 fBlockNMS
            = new CustomEntityFallingBlock_V1_18_R1(loc.getWorld(), loc.getX(), loc.getY(), loc.getZ(), blockData);
        CustomCraftFallingBlock_V1_18_R1 entity = new CustomCraftFallingBlock_V1_18_R1(Bukkit.getServer(), fBlockNMS);
        entity.setCustomName("BigDoorsEntity");
        entity.setCustomNameVisible(false);
        return entity;
    }

    @Override
    public NMSBlock nmsBlockFactory(World world, int x, int y, int z)
    {
        final Info blockInfo =
            BlockBase.Info.a((BlockBase) ((CraftWorld) world).getHandle().a_(new BlockPosition(x, y, z)).b());
        return new NMSBlock_V1_18_R1(world, x, y, z, blockInfo);
    }
}
