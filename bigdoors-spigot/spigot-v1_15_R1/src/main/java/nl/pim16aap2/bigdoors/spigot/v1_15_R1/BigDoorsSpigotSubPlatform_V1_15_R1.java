package nl.pim16aap2.bigdoors.spigot.v1_15_R1;

import lombok.Getter;
import nl.pim16aap2.bigdoors.annotations.Initializer;
import nl.pim16aap2.bigdoors.api.IBlockAnalyzer;
import nl.pim16aap2.bigdoors.api.factories.IFallingBlockFactory;
import nl.pim16aap2.bigdoors.api.factories.IPBlockDataFactory;
import nl.pim16aap2.bigdoors.api.factories.IPLocationFactory;
import nl.pim16aap2.bigdoors.logging.IPLogger;
import nl.pim16aap2.bigdoors.spigot.util.api.IBigDoorsSpigotSubPlatform;
import nl.pim16aap2.bigdoors.spigot.util.api.IGlowingBlockFactory;
import org.bukkit.plugin.java.JavaPlugin;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public final class BigDoorsSpigotSubPlatform_V1_15_R1 implements IBigDoorsSpigotSubPlatform
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
    private final IPLocationFactory locationFactory;

    @Inject
    public BigDoorsSpigotSubPlatform_V1_15_R1(IPLogger logger, IPLocationFactory locationFactory)
    {
        this.logger = logger;
        this.locationFactory = locationFactory;
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
        fallingBlockFactory = new FallingBlockFactory_V1_15_R1(logger);
        pBlockDataFactory =
            new nl.pim16aap2.bigdoors.spigot.v1_15_R1.PBlockDataFactorySpigot_V1_15_R1(locationFactory,
                                                                                       fallingBlockFactory);
        blockAnalyzer = new nl.pim16aap2.bigdoors.spigot.v1_15_R1.BlockAnalyzer_V1_15_R1();
        glowingBlockFactory = new GlowingBlock_V1_15_R1.Factory();
    }
}
