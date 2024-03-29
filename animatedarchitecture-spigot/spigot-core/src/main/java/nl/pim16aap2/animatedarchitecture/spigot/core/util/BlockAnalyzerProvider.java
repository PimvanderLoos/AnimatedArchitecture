package nl.pim16aap2.animatedarchitecture.spigot.core.util;

import lombok.Getter;
import nl.pim16aap2.animatedarchitecture.spigot.util.api.IBlockAnalyzerSpigot;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Provides the {@link IBlockAnalyzerSpigot} for the current platform.
 */
@Singleton
public final class BlockAnalyzerProvider
{
    /**
     * The block analyzer for the currently used platform.
     * <p>
     * See {@link MinecraftVersion} for notes on what 'used' actually means.
     */
    @Getter
    private final IBlockAnalyzerSpigot blockAnalyzer;

    @Inject BlockAnalyzerProvider()
    {
        this.blockAnalyzer = instantiateBlockAnalyzer();
    }

    private IBlockAnalyzerSpigot instantiateBlockAnalyzer()
    {
        return switch (MinecraftVersion.getUsedVersion())
        {
            // 1.20 did not introduce any new blocks that were not in 1.19 as experimental entries,
            // so we can use the 1.19 analyzer.
            case v1_19_R3, v1_20_R1 -> new nl.pim16aap2.animatedarchitecture.spigot.v1_19_R3.BlockAnalyzer();
        };
    }
}
