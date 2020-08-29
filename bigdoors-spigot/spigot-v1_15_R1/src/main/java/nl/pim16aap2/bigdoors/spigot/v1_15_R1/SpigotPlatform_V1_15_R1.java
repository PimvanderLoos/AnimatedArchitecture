package nl.pim16aap2.bigdoors.spigot.v1_15_R1;

import lombok.Getter;
import nl.pim16aap2.bigdoors.api.IBlockAnalyzer;
import nl.pim16aap2.bigdoors.api.IGlowingBlockSpawner;
import nl.pim16aap2.bigdoors.api.factories.IFallingBlockFactory;
import nl.pim16aap2.bigdoors.api.factories.IPBlockDataFactory;
import nl.pim16aap2.bigdoors.spigot.util.api.BigDoorsSpigotAbstract;
import nl.pim16aap2.bigdoors.spigot.util.api.ISpigotPlatform;
import nl.pim16aap2.bigdoors.util.PLogger;
import org.jetbrains.annotations.NotNull;

public final class SpigotPlatform_V1_15_R1 implements ISpigotPlatform
{
    @NotNull
    private static final String VERSION = "v1_15_R1";
    @NotNull
    private static final SpigotPlatform_V1_15_R1 instance = new SpigotPlatform_V1_15_R1();

    @Getter(onMethod = @__({@Override}))
    private IFallingBlockFactory fallingBlockFactory;

    @Getter(onMethod = @__({@Override}))
    private IPBlockDataFactory pBlockDataFactory;

    @Getter(onMethod = @__({@Override}))
    private IBlockAnalyzer blockAnalyzer;

    @Getter(onMethod = @__({@Override}))
    private IGlowingBlockSpawner glowingBlockSpawner;

    private SpigotPlatform_V1_15_R1()
    {
    }

    @Override
    public @NotNull String getVersion()
    {
        return VERSION;
    }

    /**
     * Obtains the instance of this class.
     *
     * @return The instance of this class.
     */
    public static @NotNull SpigotPlatform_V1_15_R1 get()
    {
        return instance;
    }

    @Override
    public void init(final @NotNull BigDoorsSpigotAbstract plugin)
    {
        glowingBlockSpawner = GlowingBlockSpawner_V1_15_R1.init(plugin, PLogger.get());
        fallingBlockFactory = new FallingBlockFactory_V1_15_R1();
        pBlockDataFactory = new nl.pim16aap2.bigdoors.spigot.v1_15_R1.PBlockDataFactorySpigot_V1_15_R1();
        blockAnalyzer = new nl.pim16aap2.bigdoors.spigot.v1_15_R1.BlockAnalyzer_V1_15_R1();
    }
}
