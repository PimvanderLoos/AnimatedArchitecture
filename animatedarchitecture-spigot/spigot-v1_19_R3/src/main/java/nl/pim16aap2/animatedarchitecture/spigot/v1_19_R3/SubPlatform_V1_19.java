package nl.pim16aap2.animatedarchitecture.spigot.v1_19_R3;

import nl.pim16aap2.animatedarchitecture.spigot.util.api.BlockAnalyzerSpigot;
import nl.pim16aap2.animatedarchitecture.spigot.util.api.ISpigotSubPlatform;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Represents a sub-platform for Spigot 1.19.
 */
@Singleton
public final class SubPlatform_V1_19 implements ISpigotSubPlatform
{
    private final BlockAnalyzer_V1_19 blockAnalyzer;

    @Inject
    SubPlatform_V1_19(BlockAnalyzer_V1_19 blockAnalyzer)
    {
        this.blockAnalyzer = blockAnalyzer;
    }

    @Override
    public BlockAnalyzerSpigot getBlockAnalyzer()
    {
        return blockAnalyzer;
    }
}
