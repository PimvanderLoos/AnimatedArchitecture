package nl.pim16aap2.bigDoors.NMS.v1_16_R3;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_16_R3.CraftWorld;

import net.minecraft.server.v1_16_R3.BlockBase;
import net.minecraft.server.v1_16_R3.BlockBase.Info;
import net.minecraft.server.v1_16_R3.BlockPosition;
import net.minecraft.server.v1_16_R3.IBlockData;
import nl.pim16aap2.bigDoors.BigDoors;
import nl.pim16aap2.bigDoors.NMS.CustomCraftFallingBlock_Vall;
import nl.pim16aap2.bigDoors.NMS.FallingBlockFactory_Vall;
import nl.pim16aap2.bigDoors.NMS.NMSBlock_Vall;

public class FallingBlockFactory_V1_16_R3 implements FallingBlockFactory_Vall
{
    // Make a falling block.
    @Override
    public CustomCraftFallingBlock_Vall fallingBlockFactory(BigDoors plugin, Location loc, NMSBlock_Vall block,
                                                            byte matData, Material mat)
    {
        IBlockData blockData = ((NMSBlock_V1_16_R3) block).getMyBlockData();
        CustomEntityFallingBlock_V1_16_R3 fBlockNMS = new CustomEntityFallingBlock_V1_16_R3(loc.getWorld(), loc.getX(),
                                                                                            loc.getY(), loc.getZ(),
                                                                                            blockData);
        CustomCraftFallingBlock_V1_16_R3 entity = new CustomCraftFallingBlock_V1_16_R3(Bukkit.getServer(), fBlockNMS);
        entity.setCustomName("BigDoorsEntity");
        entity.setCustomNameVisible(false);
        return entity;
    }

    @Override
    public NMSBlock_Vall nmsBlockFactory(World world, int x, int y, int z)
    {

        Info blockInfo = net.minecraft.server.v1_16_R3.BlockBase.Info
            .a((BlockBase) ((CraftWorld) world).getHandle().getType(new BlockPosition(x, y, z)).getBlock());
        return new NMSBlock_V1_16_R3(world, x, y, z, blockInfo);
    }
}
