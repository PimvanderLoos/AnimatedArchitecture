package nl.pim16aap2.bigdoors.spigot.v1_15_R1;

import lombok.NonNull;
import lombok.val;
import nl.pim16aap2.bigdoors.api.ICustomCraftFallingBlock;
import nl.pim16aap2.bigdoors.api.INMSBlock;
import nl.pim16aap2.bigdoors.api.IPLocationConst;
import nl.pim16aap2.bigdoors.api.factories.IFallingBlockFactory;
import nl.pim16aap2.bigdoors.spigot.util.SpigotAdapter;
import nl.pim16aap2.bigdoors.spigot.util.implementations.PWorldSpigot;
import nl.pim16aap2.bigdoors.util.Constants;
import org.bukkit.Bukkit;
import org.bukkit.World;

/**
 * V1_15_R1 implementation of {@link IFallingBlockFactory}.
 *
 * @author Pim
 * @see IFallingBlockFactory
 */
public class FallingBlockFactory_V1_15_R1 implements IFallingBlockFactory
{
    @Override
    public @NonNull ICustomCraftFallingBlock fallingBlockFactory(final @NonNull IPLocationConst loc,
                                                                 final @NonNull INMSBlock block)
        throws Exception
    {
        World bukkitWorld = SpigotAdapter.getBukkitWorld(loc.getWorld());
        if (bukkitWorld == null)
            throw new NullPointerException("Could not find bukkit world " + loc.getWorld().getWorldName());

        val fBlockNMS = new nl.pim16aap2.bigdoors.spigot.v1_15_R1
            .CustomEntityFallingBlock_V1_15_R1(bukkitWorld, loc.getX(), loc.getY(), loc.getZ(),
                                               ((NMSBlock_V1_15_R1) block).getMyBlockData());

        val ret = new nl.pim16aap2.bigdoors.spigot.v1_15_R1.CustomCraftFallingBlock_V1_15_R1(Bukkit.getServer(),
                                                                                             fBlockNMS);
        ret.setCustomName(Constants.BIGDOORS_ENTITY_NAME);
        ret.setCustomNameVisible(false);
        return ret;
    }

    @Override
    public @NonNull INMSBlock nmsBlockFactory(final @NonNull IPLocationConst loc)
        throws Exception
    {
        if (!(loc.getWorld() instanceof PWorldSpigot))
            throw new Exception("Unexpected type of spigot world: " + loc.getWorld().getClass().getName());
        return new NMSBlock_V1_15_R1((PWorldSpigot) loc.getWorld(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
    }
}
