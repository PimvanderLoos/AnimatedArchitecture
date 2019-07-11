package nl.pim16aap2.bigdoors.spigot.spigot_v1_14_R1;

import nl.pim16aap2.bigdoors.api.ICustomCraftFallingBlock;
import nl.pim16aap2.bigdoors.api.IFallingBlockFactory;
import nl.pim16aap2.bigdoors.api.INMSBlock;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

/**
 * V1_14_R1 implementation of {@link IFallingBlockFactory}.
 *
 * @author Pim
 * @see IFallingBlockFactory
 */
public class FallingBlockFactory_V1_14_R1 implements IFallingBlockFactory
{
    /**
     * {@inheritDoc}
     */
    @Override
    public ICustomCraftFallingBlock fallingBlockFactory(Location loc, INMSBlock block)
    {
        CustomEntityFallingBlock_V1_14_R1 fBlockNMS = new CustomEntityFallingBlock_V1_14_R1(loc.getWorld(), loc
                .getX(), loc.getY(), loc.getZ(), ((NMSBlock_V1_14_R1) block).getMyBlockData());
        CustomCraftFallingBlock_V1_14_R1 ret = new CustomCraftFallingBlock_V1_14_R1(Bukkit.getServer(), fBlockNMS);
        ret.setCustomName("BigDoorsEntity");
        ret.setCustomNameVisible(false);
        return ret;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public INMSBlock nmsBlockFactory(World world, int x, int y, int z)
    {
        return new NMSBlock_V1_14_R1(world, x, y, z);
    }
}
