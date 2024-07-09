package nl.pim16aap2.animatedarchitecture.spigot.core;

import dagger.Module;
import dagger.Provides;
import nl.pim16aap2.animatedarchitecture.core.api.IBlockAnalyzer;
import nl.pim16aap2.animatedarchitecture.spigot.util.api.BlockAnalyzerSpigot;
import nl.pim16aap2.animatedarchitecture.spigot.util.api.ISpigotSubPlatform;
import nl.pim16aap2.animatedarchitecture.spigot.v1_20.SubPlatform_V1_20;
import nl.pim16aap2.animatedarchitecture.spigot.v1_21.SubPlatform_V1_21;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.Nullable;
import org.semver4j.Semver;

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
    private static final Semver MINIMUM_SUPPORTED_VERSION = Semver.of(1, 20, 0);

    @Provides
    @Singleton
    static ISpigotSubPlatform getSubPlatform(
        Provider<SubPlatform_V1_20> subPlatform_v1_20,
        Provider<SubPlatform_V1_21> subPlatform_v1_21)
    {

        @Nullable Semver serverVersion = Semver.coerce(Bukkit.getServer().getBukkitVersion());
        if (serverVersion == null)
            throw new IllegalStateException(
                "Failed to coerce server version from '" + Bukkit.getServer().getBukkitVersion() + "'.");

        // E.g. 1.20.6-R0.1-SNAPSHOT -> 1.20.6
        serverVersion = serverVersion.withClearedPreReleaseAndBuild();

        if (serverVersion.isLowerThan(MINIMUM_SUPPORTED_VERSION))
            throw new IllegalStateException(
                "This plugin requires at least version " + MINIMUM_SUPPORTED_VERSION +
                    "! Version " + serverVersion + " is not supported.");

        return switch (serverVersion.getMinor())
        {
            case 20 -> subPlatform_v1_20.get();
            case 21 -> subPlatform_v1_21.get();

            // The default case should always point to platform for the latest supported version as this version
            // will offer the closest match to the latest version.
            default -> subPlatform_v1_21.get();
        };
    }

    @Provides
    @Singleton
    static BlockAnalyzerSpigot getBlockAnalyzerSpigot(ISpigotSubPlatform provider)
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
