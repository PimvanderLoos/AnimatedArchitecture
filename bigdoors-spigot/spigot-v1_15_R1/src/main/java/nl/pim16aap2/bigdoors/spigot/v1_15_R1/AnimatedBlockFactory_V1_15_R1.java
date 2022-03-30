package nl.pim16aap2.bigdoors.spigot.v1_15_R1;

import nl.pim16aap2.bigdoors.api.IPLocation;
import nl.pim16aap2.bigdoors.api.animatedblock.IAnimatedBlock;
import nl.pim16aap2.bigdoors.api.factories.IAnimatedBlockFactory;
import nl.pim16aap2.bigdoors.spigot.util.SpigotAdapter;
import nl.pim16aap2.bigdoors.util.Constants;
import nl.pim16aap2.bigdoors.util.Util;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_15_R1.util.CraftChatMessage;

import javax.inject.Singleton;
import java.util.Optional;

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
    public Optional<IAnimatedBlock> create(IPLocation loc, float radius, float startAngle, boolean bottom)
        throws Exception
    {
        final Location spigotLocation = SpigotAdapter.getBukkitLocation(loc);
        final World bukkitWorld = Util.requireNonNull(spigotLocation.getWorld(), "Spigot world from location: " + loc);
        final Material material = spigotLocation.getBlock().getType();

        if (!BlockAnalyzer_V1_15_R1.isAllowedBlockStatic(material))
            return Optional.empty();

        final double offset = bottom ? 0.010_001 : 0;
        final IPLocation spawnLoc = loc.add(loc.getBlockX() + 0.5,
                                            loc.getBlockY() - 0.020 + offset,
                                            loc.getBlockZ() + 0.5);

        final boolean placementDeferred = BlockAnalyzer_V1_15_R1.placeOnSecondPassStatic(material);

        final var animatedBlock = new nl.pim16aap2.bigdoors.spigot.v1_15_R1
            .CustomEntityFallingBlock_V1_15_R1(loc.getWorld(), bukkitWorld, spawnLoc.getBlockX(), spawnLoc.getBlockY(),
                                               spawnLoc.getBlockZ(), radius, startAngle, placementDeferred);

        animatedBlock.setCustomName(CraftChatMessage.fromStringOrNull(Constants.BIGDOORS_ENTITY_NAME));
        animatedBlock.setCustomNameVisible(false);
        return Optional.of(animatedBlock);
    }
}
