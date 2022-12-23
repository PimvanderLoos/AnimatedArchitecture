package nl.pim16aap2.bigdoors.spigot.v1_19_R2;

import nl.pim16aap2.bigdoors.api.IPLocation;
import nl.pim16aap2.bigdoors.api.animatedblock.AnimationContext;
import nl.pim16aap2.bigdoors.api.animatedblock.IAnimatedBlock;
import nl.pim16aap2.bigdoors.api.factories.IAnimatedBlockFactory;
import nl.pim16aap2.bigdoors.managers.AnimatedBlockHookManager;
import nl.pim16aap2.bigdoors.spigot.util.SpigotAdapter;
import nl.pim16aap2.bigdoors.util.Constants;
import nl.pim16aap2.bigdoors.util.Util;
import nl.pim16aap2.bigdoors.util.vector.Vector3Dd;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_19_R2.util.CraftChatMessage;

import javax.inject.Singleton;
import java.util.Optional;

/**
 * v1_19_R2 implementation of {@link IAnimatedBlockFactory}.
 *
 * @author Pim
 * @see IAnimatedBlockFactory
 */
@Singleton
public final class AnimatedBlockFactory_V1_19_R2 implements IAnimatedBlockFactory
{
    private final AnimatedBlockHookManager animatedBlockHookManager;

    AnimatedBlockFactory_V1_19_R2(AnimatedBlockHookManager animatedBlockHookManager)
    {
        this.animatedBlockHookManager = animatedBlockHookManager;
    }

    @Override
    public Optional<IAnimatedBlock> create(
        IPLocation loc, float radius, float startAngle, boolean bottom, boolean onEdge, AnimationContext context,
        Vector3Dd finalPosition)
        throws Exception
    {
        final Location spigotLocation = SpigotAdapter.getBukkitLocation(loc);
        final World bukkitWorld = Util.requireNonNull(spigotLocation.getWorld(), "Spigot world from location: " + loc);
        final Material material = spigotLocation.getBlock().getType();

        if (!BlockAnalyzer_V1_19_R2.isAllowedBlockStatic(material))
            return Optional.empty();

        final double offset = bottom ? 0.010_001 : 0;
        final IPLocation spawnLoc = loc.add(0, offset - 0.020, 0);

        final var animatedBlock = new CustomEntityFallingBlock_V1_19_R2(
            loc.getWorld(), bukkitWorld, spawnLoc.getX(), spawnLoc.getY(), spawnLoc.getZ(), radius, startAngle, onEdge,
            context, animatedBlockHookManager, finalPosition);

        animatedBlock.b(CraftChatMessage.fromStringOrNull(Constants.BIGDOORS_ENTITY_NAME));
        animatedBlock.n(false);
        return Optional.of(animatedBlock);
    }
}
