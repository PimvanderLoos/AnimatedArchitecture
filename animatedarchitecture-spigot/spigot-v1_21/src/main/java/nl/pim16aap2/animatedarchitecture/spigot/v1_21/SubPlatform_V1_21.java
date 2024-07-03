package nl.pim16aap2.animatedarchitecture.spigot.v1_21;

import nl.pim16aap2.animatedarchitecture.spigot.util.api.IBlockAnalyzerSpigot;
import nl.pim16aap2.animatedarchitecture.spigot.util.api.ISpigotSubPlatform;

/**
 * Represents a sub-platform for Spigot 1.21.
 */
public final class SubPlatform_V1_21 implements ISpigotSubPlatform
{
    private final BlockAnalyzer_V1_21 blockAnalyzer = new BlockAnalyzer_V1_21();

    @Override
    public IBlockAnalyzerSpigot getBlockAnalyzer()
    {
        return blockAnalyzer;
    }
}
