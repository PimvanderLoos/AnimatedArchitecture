package nl.pim16aap2.bigdoors.spigot.v1_15_R1;

import lombok.Getter;
import nl.pim16aap2.bigdoors.annotations.Initializer;
import nl.pim16aap2.bigdoors.api.IBlockAnalyzer;
import nl.pim16aap2.bigdoors.api.factories.IFallingBlockFactory;
import nl.pim16aap2.bigdoors.api.factories.IPBlockDataFactory;
import nl.pim16aap2.bigdoors.logging.IPLogger;
import nl.pim16aap2.bigdoors.spigot.util.api.BigDoorsSpigotAbstract;
import nl.pim16aap2.bigdoors.spigot.util.api.IGlowingBlockFactory;
import nl.pim16aap2.bigdoors.spigot.util.api.ISpigotPlatform;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public final class SpigotPlatform_V1_15_R1 implements ISpigotPlatform
{
    private static final String VERSION = "v1_15_R1";

    @Getter
    private IFallingBlockFactory fallingBlockFactory;

    @Getter
    private IPBlockDataFactory pBlockDataFactory;

    @Getter
    private IBlockAnalyzer blockAnalyzer;

    @Getter
    private IGlowingBlockFactory glowingBlockFactory;

    private final IPLogger logger;

    @Inject
    public SpigotPlatform_V1_15_R1(IPLogger logger)
    {
        this.logger = logger;
    }

    @Override
    public String getVersion()
    {
        return VERSION;
    }

    @Override
    @Initializer
    public void init(BigDoorsSpigotAbstract plugin)
    {
        fallingBlockFactory = new FallingBlockFactory_V1_15_R1(logger);
        pBlockDataFactory = new nl.pim16aap2.bigdoors.spigot.v1_15_R1.PBlockDataFactorySpigot_V1_15_R1();
        blockAnalyzer = new nl.pim16aap2.bigdoors.spigot.v1_15_R1.BlockAnalyzer_V1_15_R1();
        glowingBlockFactory = new GlowingBlock_V1_15_R1.Factory();
    }
}
