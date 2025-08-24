package nl.pim16aap2.animatedarchitecture.spigot.core.animation;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import nl.pim16aap2.animatedarchitecture.core.animation.RotatedPosition;
import nl.pim16aap2.animatedarchitecture.core.api.IWorld;
import nl.pim16aap2.animatedarchitecture.core.api.animatedblock.IAnimatedBlock;
import nl.pim16aap2.animatedarchitecture.core.api.animatedblock.IAnimatedBlockData;
import nl.pim16aap2.animatedarchitecture.core.api.animatedblock.IAnimatedBlockFactory;
import nl.pim16aap2.animatedarchitecture.core.util.Util;
import nl.pim16aap2.animatedarchitecture.core.util.vector.Vector3Di;
import nl.pim16aap2.animatedarchitecture.spigot.util.SpigotAdapter;
import nl.pim16aap2.animatedarchitecture.spigot.util.api.BlockAnalyzerSpigot;
import org.bukkit.Material;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.function.Consumer;

/**
 * Implementation of {@link IAnimatedBlockFactory} for the Spigot platform.
 */
@Singleton
public class AnimatedBlockDisplayFactory implements IAnimatedBlockFactory
{
    private final AnimatedBlockDisplay.IFactory assistedFactory;
    private final BlockAnalyzerSpigot blockAnalyzer;

    @Inject
    AnimatedBlockDisplayFactory(
        AnimatedBlockDisplay.IFactory assistedFactory,
        BlockAnalyzerSpigot blockAnalyzer)
    {
        this.assistedFactory = assistedFactory;
        this.blockAnalyzer = blockAnalyzer;
    }

    @Override
    public Optional<IAnimatedBlock> create(
        IWorld world,
        RotatedPosition startPosition,
        float radius,
        boolean onEdge,
        RotatedPosition finalPosition,
        @Nullable Consumer<IAnimatedBlockData> blockDataRotator)
    {
        final Vector3Di pos = startPosition.position().floor().toInteger();
        final Material mat =
            Util.requireNonNull(SpigotAdapter.getBukkitWorld(world), "BukkitWorld").getType(pos.x(), pos.y(), pos.z());

        if (!blockAnalyzer.isAllowed(mat))
            return Optional.empty();

        return Optional.of(assistedFactory.create(
            blockDataRotator,
            startPosition,
            finalPosition,
            world,
            onEdge,
            radius
        ));
    }
}
