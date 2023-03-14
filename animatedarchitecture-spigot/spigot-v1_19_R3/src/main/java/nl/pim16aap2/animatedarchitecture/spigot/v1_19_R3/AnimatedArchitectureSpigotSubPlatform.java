package nl.pim16aap2.animatedarchitecture.spigot.v1_19_R3;

import lombok.Getter;
import nl.pim16aap2.animatedarchitecture.core.annotations.Initializer;
import nl.pim16aap2.animatedarchitecture.core.api.IBlockAnalyzer;
import nl.pim16aap2.animatedarchitecture.core.api.IExecutor;
import nl.pim16aap2.animatedarchitecture.core.api.animatedblock.IAnimatedBlockFactory;
import nl.pim16aap2.animatedarchitecture.core.managers.AnimatedBlockHookManager;
import nl.pim16aap2.animatedarchitecture.spigot.util.api.IAnimatedArchitectureSpigotSubPlatform;
import nl.pim16aap2.animatedarchitecture.spigot.util.api.IGlowingBlockFactory;
import org.bukkit.plugin.java.JavaPlugin;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public final class AnimatedArchitectureSpigotSubPlatform implements IAnimatedArchitectureSpigotSubPlatform
{
    private static final String VERSION = "v1_19_R3";

    @Getter
    private IAnimatedBlockFactory animatedBlockFactory;

    @Getter
    private IBlockAnalyzer blockAnalyzer;

    @Getter
    private IGlowingBlockFactory glowingBlockFactory;

    private final AnimatedBlockHookManager animatedBlockHookManager;
    private final IExecutor executor;

    @Inject
    public AnimatedArchitectureSpigotSubPlatform(AnimatedBlockHookManager animatedBlockHookManager, IExecutor executor)
    {
        this.animatedBlockHookManager = animatedBlockHookManager;
        this.executor = executor;
    }

    @Override
    public String getVersion()
    {
        return VERSION;
    }

    @Override
    @Initializer
    public void init(JavaPlugin plugin)
    {
        animatedBlockFactory = new AnimatedBlockFactory(animatedBlockHookManager, executor);
        blockAnalyzer = new BlockAnalyzer();
        glowingBlockFactory = new GlowingBlock.Factory();
    }
}
