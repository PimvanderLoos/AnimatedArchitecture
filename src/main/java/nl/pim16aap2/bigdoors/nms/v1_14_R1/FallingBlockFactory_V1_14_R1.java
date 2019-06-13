package nl.pim16aap2.bigdoors.nms.v1_14_R1;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.nms.CustomCraftFallingBlock_Vall;
import nl.pim16aap2.bigdoors.nms.FallingBlockFactory_Vall;
import nl.pim16aap2.bigdoors.nms.NMSBlock_Vall;

public class FallingBlockFactory_V1_14_R1 implements FallingBlockFactory_Vall
{
    // Make a falling block.
    @Override
    public CustomCraftFallingBlock_Vall fallingBlockFactory(BigDoors plugin, Location loc, NMSBlock_Vall block)
    {
        CustomEntityFallingBlock_V1_14_R1 fBlockNMS = new CustomEntityFallingBlock_V1_14_R1(plugin, loc.getWorld(), loc
            .getX(), loc.getY(), loc.getZ(), ((NMSBlock_V1_14_R1) block).getMyBlockData());
        CustomCraftFallingBlock_V1_14_R1 ret = new CustomCraftFallingBlock_V1_14_R1(Bukkit.getServer(), fBlockNMS);
        ret.setCustomName("BigDoorsEntity");
        ret.setCustomNameVisible(false);
        return ret;
    }

    @Override
    public NMSBlock_Vall nmsBlockFactory(World world, int x, int y, int z)
    {
        return new NMSBlock_V1_14_R1(world, x, y, z);
    }
}
