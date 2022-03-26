package nl.pim16aap2.bigdoors.spigot.v1_15_R1;

import nl.pim16aap2.bigdoors.api.IPLocation;
import nl.pim16aap2.bigdoors.api.animatedblock.IAnimatedBlock;
import nl.pim16aap2.bigdoors.api.factories.IAnimatedBlockFactory;
import nl.pim16aap2.bigdoors.spigot.util.SpigotAdapter;
import nl.pim16aap2.bigdoors.util.Constants;
import nl.pim16aap2.bigdoors.util.Util;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_15_R1.util.CraftChatMessage;

import javax.inject.Singleton;

/**
 * V1_15_R1 implementation of {@link IAnimatedBlockFactory}.
 *
 * @author Pim
 * @see IAnimatedBlockFactory
 */
@Singleton
public final class AnimatedBlockFactory_V1_15_R1 implements IAnimatedBlockFactory
{
    AnimatedBlockFactory_V1_15_R1()
    {
    }

    @Override
    public IAnimatedBlock fallingBlockFactory(IPLocation loc)
        throws Exception
    {
        final World bukkitWorld = Util.requireNonNull(SpigotAdapter.getBukkitWorld(loc.getWorld()),
                                                      "Spigot world from location: " + loc);

        final var animatedBlock = new nl.pim16aap2.bigdoors.spigot.v1_15_R1
            .CustomEntityFallingBlock_V1_15_R1(loc.getWorld(), bukkitWorld, loc.getBlockX(), loc.getBlockY(),
                                               loc.getBlockZ());

        animatedBlock.setCustomName(CraftChatMessage.fromStringOrNull(Constants.BIGDOORS_ENTITY_NAME));
        animatedBlock.setCustomNameVisible(false);
        return animatedBlock;
    }
}
