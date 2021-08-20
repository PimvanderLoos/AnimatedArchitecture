package nl.pim16aap2.bigdoors.spigot.util.api;

import nl.pim16aap2.bigdoors.api.IBlockAnalyzer;
import nl.pim16aap2.bigdoors.api.factories.IFallingBlockFactory;
import nl.pim16aap2.bigdoors.api.factories.IPBlockDataFactory;

public interface ISpigotPlatform
{
    /**
     * Gets the version of this platform. E.g. "v1_14_R1" for 1.14 to 1.14.4.
     *
     * @return The version.
     */
    String getVersion();

    void init(BigDoorsSpigotAbstract plugin);

    IFallingBlockFactory getFallingBlockFactory();

    IPBlockDataFactory getPBlockDataFactory();

    IBlockAnalyzer getBlockAnalyzer();

    IGlowingBlockFactory getGlowingBlockFactory();
}
