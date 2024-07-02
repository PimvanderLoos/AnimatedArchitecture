package nl.pim16aap2.animatedarchitecture.spigot.core.util;

import lombok.Getter;
import nl.pim16aap2.animatedarchitecture.core.api.debugging.DebuggableRegistry;
import nl.pim16aap2.animatedarchitecture.core.api.debugging.IDebuggable;
import nl.pim16aap2.animatedarchitecture.spigot.util.api.IBlockAnalyzerSpigot;
import nl.pim16aap2.animatedarchitecture.spigot.v1_19_R3.BlockAnalyzer_V1_19;
import nl.pim16aap2.animatedarchitecture.spigot.v1_21.BlockAnalyzer_V1_21;
import org.bukkit.Bukkit;
import org.semver4j.Semver;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Provides the {@link IBlockAnalyzerSpigot} for the current platform.
 */
@Singleton
public final class BlockAnalyzerProvider implements IDebuggable
{
    /**
     * The block analyzer for the current version of the server.
     */
    @Getter
    private final IBlockAnalyzerSpigot blockAnalyzer;

    /**
     * The server version.
     */
    private final @Nullable Semver serverVersion;

    @Inject
    BlockAnalyzerProvider(DebuggableRegistry debuggableRegistry)
    {
        this.serverVersion = Semver.coerce(Bukkit.getServer().getBukkitVersion());
        this.blockAnalyzer = instantiateBlockAnalyzer();

        debuggableRegistry.registerDebuggable(this);
    }

    private IBlockAnalyzerSpigot instantiateBlockAnalyzer()
    {
        if (serverVersion == null)
            throw new IllegalStateException(
                "Failed to coerce server version from '" + Bukkit.getServer().getBukkitVersion() + "'.");

        if (serverVersion.isLowerThan(Semver.of(1, 19, 4)))
            throw new IllegalStateException(
                "This plugin requires at least version 1.19.4. Version '" + serverVersion + "' is not supported.");

        return switch (serverVersion.getMinor())
        {
            // 1.20 did not introduce any new blocks that were not in 1.19 as experimental entries,
            // so we can use the 1.19 analyzer.
            case 19, 20 -> new BlockAnalyzer_V1_19();

            case 21 -> new BlockAnalyzer_V1_21();

            // The default case should always point to BlockAnalyzer for the latest supported version as this version
            // will offer the closest match to the latest version.
            default -> new BlockAnalyzer_V1_21();
        };
    }

    @Override
    public String getDebugInformation()
    {
        return "Loaded block analyzer '" + blockAnalyzer.getClass().getName() +
            "' for server version '" + serverVersion + "'.";
    }
}
