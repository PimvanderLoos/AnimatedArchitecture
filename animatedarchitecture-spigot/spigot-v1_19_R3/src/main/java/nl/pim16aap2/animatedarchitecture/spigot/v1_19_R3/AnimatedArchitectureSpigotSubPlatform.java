package nl.pim16aap2.animatedarchitecture.spigot.v1_19_R3;

import lombok.Getter;
import nl.pim16aap2.animatedarchitecture.core.annotations.Initializer;
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
    private IGlowingBlockFactory glowingBlockFactory;

    @Inject
    public AnimatedArchitectureSpigotSubPlatform()
    {
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
        glowingBlockFactory = new GlowingBlock.Factory();
    }
}
