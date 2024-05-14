package nl.pim16aap2.animatedarchitecture.spigot.core.util;

import lombok.Getter;
import nl.pim16aap2.animatedarchitecture.spigot.util.api.IBlockAnalyzerSpigot;
import org.bukkit.Bukkit;
import org.semver4j.Semver;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Provides the {@link IBlockAnalyzerSpigot} for the current platform.
 */
@Singleton
public final class BlockAnalyzerProvider
{
    /**
     * The block analyzer for the current version of the server.
     */
    @Getter
    private final IBlockAnalyzerSpigot blockAnalyzer;

    @Inject BlockAnalyzerProvider()
    {
        this.blockAnalyzer = instantiateBlockAnalyzer();
    }

    private IBlockAnalyzerSpigot instantiateBlockAnalyzer()
    {
        final @Nullable Semver serverVersion = Semver.coerce(Bukkit.getServer().getBukkitVersion());
        if (serverVersion == null)
            throw new IllegalStateException(
                "Failed to coerce server version from '" + Bukkit.getServer().getBukkitVersion() + "'.");

        if (serverVersion.isLowerThan(Semver.of(1, 19, 3)))
            throw new IllegalStateException(
                "This plugin requires at least version 1.19.3. Version '" + serverVersion + "' is not supported.");

        return switch (serverVersion.getMinor())
        {
            // 1.20 did not introduce any new blocks that were not in 1.19 as experimental entries,
            // so we can use the 1.19 analyzer.
            case 19, 20 -> new nl.pim16aap2.animatedarchitecture.spigot.v1_19_R3.BlockAnalyzer();

            // The default case should always point to BlockAnalyzer for the latest supported version as this version
            // will offer the closest match to the latest version.
            default -> new nl.pim16aap2.animatedarchitecture.spigot.v1_19_R3.BlockAnalyzer();
        };
    }
}
