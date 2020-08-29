package nl.pim16aap2.bigdoors.spigot.util.api;

import nl.pim16aap2.bigdoors.api.IBlockAnalyzer;
import nl.pim16aap2.bigdoors.api.IGlowingBlockSpawner;
import nl.pim16aap2.bigdoors.api.factories.IFallingBlockFactory;
import nl.pim16aap2.bigdoors.api.factories.IPBlockDataFactory;
import org.jetbrains.annotations.NotNull;

public interface ISpigotPlatform
{
    /**
     * Gets the version of this platform. E.g. "v1_14_R1" for 1.14 to 1.14.4.
     *
     * @return The version.
     */
    @NotNull String getVersion();

    void init(final @NotNull BigDoorsSpigotAbstract plugin);

    @NotNull IFallingBlockFactory getFallingBlockFactory();

    @NotNull IPBlockDataFactory getPBlockDataFactory();

    @NotNull IBlockAnalyzer getBlockAnalyzer();

    @NotNull IGlowingBlockSpawner getGlowingBlockSpawner();
}
