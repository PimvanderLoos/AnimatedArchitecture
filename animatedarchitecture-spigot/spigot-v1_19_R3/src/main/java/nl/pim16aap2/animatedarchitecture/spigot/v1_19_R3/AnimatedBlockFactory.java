package nl.pim16aap2.animatedarchitecture.spigot.v1_19_R3;

import nl.pim16aap2.animatedarchitecture.core.api.IExecutor;
import nl.pim16aap2.animatedarchitecture.core.api.IWorld;
import nl.pim16aap2.animatedarchitecture.core.api.animatedblock.AnimationContext;
import nl.pim16aap2.animatedarchitecture.core.api.animatedblock.IAnimatedBlock;
import nl.pim16aap2.animatedarchitecture.core.api.animatedblock.IAnimatedBlockFactory;
import nl.pim16aap2.animatedarchitecture.core.managers.AnimatedBlockHookManager;
import nl.pim16aap2.animatedarchitecture.core.moveblocks.Animator;
import nl.pim16aap2.animatedarchitecture.core.moveblocks.RotatedPosition;
import nl.pim16aap2.animatedarchitecture.core.util.Constants;
import nl.pim16aap2.animatedarchitecture.core.util.Util;
import nl.pim16aap2.animatedarchitecture.core.util.vector.Vector3Di;
import nl.pim16aap2.animatedarchitecture.spigot.util.SpigotAdapter;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_19_R3.util.CraftChatMessage;

import javax.inject.Singleton;
import java.util.Optional;

/**
 * v1_19_R3 implementation of {@link IAnimatedBlockFactory}.
 *
 * @author Pim
 * @see IAnimatedBlockFactory
 */
@Singleton
public final class AnimatedBlockFactory implements IAnimatedBlockFactory
{
    private final AnimatedBlockHookManager animatedBlockHookManager;
    private final IExecutor executor;

    AnimatedBlockFactory(AnimatedBlockHookManager animatedBlockHookManager, IExecutor executor)
    {
        this.animatedBlockHookManager = animatedBlockHookManager;
        this.executor = executor;
    }

    @Override
    public Optional<IAnimatedBlock> create(
        IWorld world, RotatedPosition startPosition, float radius, boolean bottom, boolean onEdge,
        AnimationContext context, RotatedPosition finalPosition, Animator.MovementMethod movementMethod)
        throws Exception
    {
        final World bukkitWorld =
            Util.requireNonNull(SpigotAdapter.getBukkitWorld(world), "Spigot world from: " + world);
        final Vector3Di pos = startPosition.position().floor().toInteger();
        final Material material = bukkitWorld.getType(pos.x(), pos.y(), pos.z());

        if (!BlockAnalyzer.isAllowedBlockStatic(material))
            return Optional.empty();

        final double offset = bottom ? 0.010_001 : 0;

        final var animatedBlock = new CustomEntityFallingBlock(
            executor, world, bukkitWorld, pos.x(), pos.y() + offset, pos.z(), radius,
            movementMethod, onEdge, context, animatedBlockHookManager, finalPosition.position());

        animatedBlock.b(CraftChatMessage.fromStringOrNull(Constants.ANIMATED_ARCHITECTURE_ENTITY_NAME));
        animatedBlock.n(false);
        return Optional.of(animatedBlock);
    }
}
