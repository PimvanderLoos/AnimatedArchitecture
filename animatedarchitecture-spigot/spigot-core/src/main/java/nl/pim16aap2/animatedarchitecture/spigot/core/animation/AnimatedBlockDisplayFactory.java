package nl.pim16aap2.animatedarchitecture.spigot.core.animation;

import nl.pim16aap2.animatedarchitecture.core.api.IExecutor;
import nl.pim16aap2.animatedarchitecture.core.api.IWorld;
import nl.pim16aap2.animatedarchitecture.core.api.animatedblock.IAnimatedBlock;
import nl.pim16aap2.animatedarchitecture.core.api.animatedblock.IAnimatedBlockData;
import nl.pim16aap2.animatedarchitecture.core.api.animatedblock.IAnimatedBlockFactory;
import nl.pim16aap2.animatedarchitecture.core.managers.AnimatedBlockHookManager;
import nl.pim16aap2.animatedarchitecture.core.moveblocks.RotatedPosition;
import nl.pim16aap2.animatedarchitecture.core.util.Util;
import nl.pim16aap2.animatedarchitecture.core.util.vector.Vector3Di;
import nl.pim16aap2.animatedarchitecture.spigot.util.SpigotAdapter;
import nl.pim16aap2.animatedarchitecture.spigot.util.api.IBlockAnalyzerSpigot;
import org.bukkit.Material;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Optional;
import java.util.function.Consumer;

@Singleton
public class AnimatedBlockDisplayFactory implements IAnimatedBlockFactory
{
    private final IExecutor executor;
    private final AnimatedBlockHookManager animatedBlockHookManager;
    private final IBlockAnalyzerSpigot blockAnalyzer;

    @Inject AnimatedBlockDisplayFactory(
        IExecutor executor,
        AnimatedBlockHookManager animatedBlockHookManager,
        IBlockAnalyzerSpigot blockAnalyzer)
    {
        this.executor = executor;
        this.animatedBlockHookManager = animatedBlockHookManager;
        this.blockAnalyzer = blockAnalyzer;
    }

    @Override
    public Optional<IAnimatedBlock> create(
        IWorld world, RotatedPosition startPosition, float radius, boolean onEdge, RotatedPosition finalPosition,
        @Nullable Consumer<IAnimatedBlockData> blockDataRotator)
    {
        final Vector3Di pos = startPosition.position().floor().toInteger();
        final Material mat =
            Util.requireNonNull(SpigotAdapter.getBukkitWorld(world), "BukkitWorld").getType(pos.x(), pos.y(), pos.z());

        if (!blockAnalyzer.isAllowed(mat))
            return Optional.empty();

        return Optional.of(new AnimatedBlockDisplay(
            executor,
            animatedBlockHookManager,
            blockDataRotator,
            startPosition,
            world,
            finalPosition,
            onEdge,
            radius));
    }
}
