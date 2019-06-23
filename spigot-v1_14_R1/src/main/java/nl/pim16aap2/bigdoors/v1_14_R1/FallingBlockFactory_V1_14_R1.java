package nl.pim16aap2.bigdoors.v1_14_R1;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import nl.pim16aap2.bigdoors.api.CustomCraftFallingBlock_Vall;
import nl.pim16aap2.bigdoors.api.FallingBlockFactory_Vall;
import nl.pim16aap2.bigdoors.api.NMSBlock_Vall;

/**
 * V1_14_R1 implementation of {@link FallingBlockFactory_Vall}.
 *
 * @author Pim
 * @see FallingBlockFactory_Vall
 */
public class FallingBlockFactory_V1_14_R1 implements FallingBlockFactory_Vall
{
    /**
     * {@inheritDoc}
     */
    @Override
    public CustomCraftFallingBlock_Vall fallingBlockFactory(Location loc, NMSBlock_Vall block)
    {
        CustomEntityFallingBlock_V1_14_R1 fBlockNMS = new CustomEntityFallingBlock_V1_14_R1(loc.getWorld(), loc
            .getX(), loc.getY(), loc.getZ(), ((nl.pim16aap2.bigdoors.v1_14_R1.NMSBlock_V1_14_R1) block).getMyBlockData());
        CustomCraftFallingBlock_V1_14_R1 ret = new CustomCraftFallingBlock_V1_14_R1(Bukkit.getServer(), fBlockNMS);
        ret.setCustomName("BigDoorsEntity");
        ret.setCustomNameVisible(false);
        return ret;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NMSBlock_Vall nmsBlockFactory(World world, int x, int y, int z)
    {
        return new nl.pim16aap2.bigdoors.v1_14_R1.NMSBlock_V1_14_R1(world, x, y, z);
    }
}
