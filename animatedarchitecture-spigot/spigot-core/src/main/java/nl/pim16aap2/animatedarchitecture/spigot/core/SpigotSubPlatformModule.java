package nl.pim16aap2.animatedarchitecture.spigot.core;

import dagger.Module;
import dagger.Provides;
import nl.pim16aap2.animatedarchitecture.core.api.IBlockAnalyzer;
import nl.pim16aap2.animatedarchitecture.spigot.util.api.IBlockAnalyzerSpigot;
import nl.pim16aap2.animatedarchitecture.spigot.util.api.ISpigotSubPlatform;
import nl.pim16aap2.animatedarchitecture.spigot.v1_19_R3.SubPlatform_V1_19;
import nl.pim16aap2.animatedarchitecture.spigot.v1_21.SubPlatform_V1_21;
import org.bukkit.Bukkit;
import org.semver4j.Semver;

import javax.annotation.Nullable;
import javax.inject.Provider;
import javax.inject.Singleton;

/**
 * Module that provides the sub-platform for the Spigot platform based on the server version.
 * <p>
 * Additionally, it provides the objects provided by the sub-platform to the version-specific interfaces.
 */
@Module
@SuppressWarnings("PMD.AbstractClassWithoutAbstractMethod")
public abstract class SpigotSubPlatformModule
{
    @Provides
    @Singleton
    static ISpigotSubPlatform getSubPlatform(
        Provider<SubPlatform_V1_19> subPlatform_v1_19,
        Provider<SubPlatform_V1_21> subPlatform_v1_21)
    {
        final @Nullable Semver serverVersion = Semver.coerce(Bukkit.getServer().getBukkitVersion());

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
            case 19, 20 -> subPlatform_v1_19.get();

            case 21 -> subPlatform_v1_21.get();

            // The default case should always point to platform for the latest supported version as this version
            // will offer the closest match to the latest version.
            default -> subPlatform_v1_21.get();
        };
    }

    @Provides
    @Singleton
    static IBlockAnalyzerSpigot getBlockAnalyzerSpigot(ISpigotSubPlatform provider)
    {
        return provider.getBlockAnalyzer();
    }

    @Provides
    @Singleton
    static IBlockAnalyzer<?> getBlockAnalyzer(ISpigotSubPlatform provider)
    {
        return provider.getBlockAnalyzer();
    }
}
