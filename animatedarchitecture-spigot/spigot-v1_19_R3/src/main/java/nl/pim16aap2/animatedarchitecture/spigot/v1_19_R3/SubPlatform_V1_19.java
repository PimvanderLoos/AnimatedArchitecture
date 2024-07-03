package nl.pim16aap2.animatedarchitecture.spigot.v1_19_R3;

import nl.pim16aap2.animatedarchitecture.spigot.util.api.IBlockAnalyzerSpigot;
import nl.pim16aap2.animatedarchitecture.spigot.util.api.ISpigotSubPlatform;

/**
 * Represents a sub-platform for Spigot 1.19.
 */
public final class SubPlatform_V1_19 implements ISpigotSubPlatform
{
    private final BlockAnalyzer_V1_19 blockAnalyzer = new BlockAnalyzer_V1_19();

    @Override
    public IBlockAnalyzerSpigot getBlockAnalyzer()
    {
        return blockAnalyzer;
    }
}
