package nl.pim16aap2.animatedarchitecture.spigot.v1_21;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import nl.pim16aap2.animatedarchitecture.spigot.util.api.BlockAnalyzerSpigot;
import nl.pim16aap2.animatedarchitecture.spigot.util.api.ISpigotSubPlatform;

/**
 * Represents a sub-platform for Spigot 1.21.
 */
@Singleton
public final class SubPlatform_V1_21 implements ISpigotSubPlatform
{
    private final BlockAnalyzer_V1_21 blockAnalyzer;

    @Inject
    SubPlatform_V1_21(BlockAnalyzer_V1_21 blockAnalyzer)
    {
        this.blockAnalyzer = blockAnalyzer;
    }

    @Override
    public BlockAnalyzerSpigot getBlockAnalyzer()
    {
        return blockAnalyzer;
    }
}
