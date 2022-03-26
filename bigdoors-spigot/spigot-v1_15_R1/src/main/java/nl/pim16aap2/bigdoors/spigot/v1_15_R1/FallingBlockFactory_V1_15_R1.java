package nl.pim16aap2.bigdoors.spigot.v1_15_R1;

import nl.pim16aap2.bigdoors.api.IAnimatedBlock;
import nl.pim16aap2.bigdoors.api.INMSBlock;
import nl.pim16aap2.bigdoors.api.IPLocation;
import nl.pim16aap2.bigdoors.api.factories.IFallingBlockFactory;
import nl.pim16aap2.bigdoors.spigot.util.SpigotAdapter;
import nl.pim16aap2.bigdoors.spigot.util.implementations.PWorldSpigot;
import nl.pim16aap2.bigdoors.util.Constants;
import nl.pim16aap2.bigdoors.util.Util;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_15_R1.util.CraftChatMessage;

import javax.inject.Singleton;

/**
 * V1_15_R1 implementation of {@link IFallingBlockFactory}.
 *
 * @author Pim
 * @see IFallingBlockFactory
 */
@Singleton
public final class FallingBlockFactory_V1_15_R1 implements IFallingBlockFactory
{
    @Override
    public IAnimatedBlock fallingBlockFactory(IPLocation loc, INMSBlock block)
        throws Exception
    {
        final World bukkitWorld = Util.requireNonNull(SpigotAdapter.getBukkitWorld(loc.getWorld()),
                                                      "Spigot world from location: " + loc);

        final CustomEntityFallingBlock_V1_15_R1 animatedBlock = new nl.pim16aap2.bigdoors.spigot.v1_15_R1
            .CustomEntityFallingBlock_V1_15_R1(loc.getWorld(), bukkitWorld, loc.getX(), loc.getY(), loc.getZ(),
                                               ((NMSBlock_V1_15_R1) block).getMyBlockData());

        animatedBlock.setCustomName(CraftChatMessage.fromStringOrNull(Constants.BIGDOORS_ENTITY_NAME));
        animatedBlock.setCustomNameVisible(false);
        return animatedBlock;
    }

    @Override
    public INMSBlock nmsBlockFactory(IPLocation loc)
        throws Exception
    {
        if (!(loc.getWorld() instanceof PWorldSpigot))
            throw new Exception("Unexpected type of spigot world: " + loc.getWorld().getClass().getName());
        return new NMSBlock_V1_15_R1((PWorldSpigot) loc.getWorld(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
    }
}
