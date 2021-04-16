package nl.pim16aap2.bigdoors.spigot.v1_15_R1;

import lombok.Getter;
import lombok.NonNull;
import nl.pim16aap2.bigdoors.api.IBlockAnalyzer;
import nl.pim16aap2.bigdoors.api.factories.IFallingBlockFactory;
import nl.pim16aap2.bigdoors.api.factories.IPBlockDataFactory;
import nl.pim16aap2.bigdoors.spigot.util.api.BigDoorsSpigotAbstract;
import nl.pim16aap2.bigdoors.spigot.util.api.IGlowingBlockFactory;
import nl.pim16aap2.bigdoors.spigot.util.api.ISpigotPlatform;

public final class SpigotPlatform_V1_15_R1 implements ISpigotPlatform
{
    private static final @NonNull String VERSION = "v1_15_R1";
    private static final @NonNull SpigotPlatform_V1_15_R1 INSTANCE = new SpigotPlatform_V1_15_R1();

    @Getter
    private IFallingBlockFactory fallingBlockFactory;

    @Getter
    private IPBlockDataFactory pBlockDataFactory;

    @Getter
    private IBlockAnalyzer blockAnalyzer;

    @Getter
    private IGlowingBlockFactory glowingBlockFactory;

    private SpigotPlatform_V1_15_R1()
    {
    }

    @Override
    public @NonNull String getVersion()
    {
        return VERSION;
    }

    /**
     * Obtains the instance of this class.
     *
     * @return The instance of this class.
     */
    public static @NonNull SpigotPlatform_V1_15_R1 get()
    {
        return INSTANCE;
    }

    @Override
    public void init(final @NonNull BigDoorsSpigotAbstract plugin)
    {
        fallingBlockFactory = new FallingBlockFactory_V1_15_R1();
        pBlockDataFactory = new nl.pim16aap2.bigdoors.spigot.v1_15_R1.PBlockDataFactorySpigot_V1_15_R1();
        blockAnalyzer = new nl.pim16aap2.bigdoors.spigot.v1_15_R1.BlockAnalyzer_V1_15_R1();
        glowingBlockFactory = new GlowingBlock_V1_15_R1.Factory();
    }
}
