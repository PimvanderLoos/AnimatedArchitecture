package nl.pim16aap2.animatedarchitecture.spigot.v1_20;

import nl.pim16aap2.animatedarchitecture.spigot.util.api.BlockAnalyzerSpigot;
import nl.pim16aap2.animatedarchitecture.spigot.util.api.ISpigotSubPlatform;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Represents a sub-platform for Spigot 1.20.
 */
@Singleton
public final class SubPlatform_V1_20 implements ISpigotSubPlatform
{
    private final BlockAnalyzer_V1_20 blockAnalyzer;

    @Inject
    SubPlatform_V1_20(BlockAnalyzer_V1_20 blockAnalyzer)
    {
        this.blockAnalyzer = blockAnalyzer;
    }

    @Override
    public BlockAnalyzerSpigot getBlockAnalyzer()
    {
        return blockAnalyzer;
    }
}
