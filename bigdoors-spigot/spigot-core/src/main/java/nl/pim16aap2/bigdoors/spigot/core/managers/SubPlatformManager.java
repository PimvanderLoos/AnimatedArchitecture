package nl.pim16aap2.bigdoors.spigot.core.managers;

import lombok.extern.flogger.Flogger;
import nl.pim16aap2.bigdoors.core.api.IBigDoorsPlatform;
import nl.pim16aap2.bigdoors.core.api.IExecutor;
import nl.pim16aap2.bigdoors.core.managers.AnimatedBlockHookManager;
import nl.pim16aap2.bigdoors.spigot.core.BigDoorsPlugin;
import nl.pim16aap2.bigdoors.spigot.util.api.IBigDoorsSpigotSubPlatform;
import nl.pim16aap2.bigdoors.spigot.v1_19_R2.BigDoorsSpigotSubPlatform;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Locale;

@Singleton
@Flogger
public final class SubPlatformManager
{
    private final @Nullable IBigDoorsSpigotSubPlatform spigotPlatform;

    private final String serverVersion;

    private final Version subPlatformVersion;

    /**
     * Instantiates the platform manager and initializes the version-specific platform with the provided Spigot
     * platform.
     *
     * @param bigDoorsPlugin
     *     The {@link IBigDoorsPlatform} for Spigot.
     * @throws InstantiationException
     *     When there is no version-specific platform available. This may happen when trying to instantiate this class
     *     on an unsupported version.
     */
    @Inject
    public SubPlatformManager(
        BigDoorsPlugin bigDoorsPlugin, AnimatedBlockHookManager animatedBlockHookManager, IExecutor executor)
    {
        serverVersion = Bukkit.getServer().getClass().getPackage().getName();

        Version versionTmp;
        @Nullable IBigDoorsSpigotSubPlatform spigotPlatformTmp = null;
        try
        {
            final String versionStringTmp = serverVersion.split("\\.")[3];
            versionTmp = Version.parseVersion(versionStringTmp);
            if (versionTmp != Version.UNSUPPORTED_VERSION)
                spigotPlatformTmp = versionTmp.getPlatform(animatedBlockHookManager, executor);
        }
        catch (ArrayIndexOutOfBoundsException | IllegalArgumentException e)
        {
            log.atSevere().withCause(e)
               .log("Failed to determine platform version for server version: '%s'", serverVersion);
            versionTmp = Version.UNSUPPORTED_VERSION;
        }
        subPlatformVersion = versionTmp;
        spigotPlatform = spigotPlatformTmp;

        if (spigotPlatform != null)
            spigotPlatform.init(bigDoorsPlugin);
    }

    /**
     * Checks if a sub-platform was registered successfully.
     *
     * @return True if a sub-platform was registered successfully.
     */
    public boolean isValidPlatform()
    {
        return spigotPlatform != null;
    }

    /**
     * Gets the currently-registered sub-platform.
     *
     * @return The sub-platform that was registered.
     *
     * @throws IllegalStateException
     *     When no platform is registered. See {@link #isValidPlatform()}.
     */
    public IBigDoorsSpigotSubPlatform getSpigotPlatform()
    {
        if (spigotPlatform == null)
            throw new IllegalStateException("No Spigot platform currently registered!");
        return spigotPlatform;
    }

    /**
     * Gets the version of the registered sub-platform.
     *
     * @return The version of the registered sub-platform.
     */
    public String getSubPlatformVersion()
    {
        return subPlatformVersion.name();
    }

    /**
     * Gets the server version.
     *
     * @return The server version.
     */
    public String getServerVersion()
    {
        return serverVersion;
    }

    private enum Version
    {
        UNSUPPORTED_VERSION
            {
                @Override
                public @Nullable IBigDoorsSpigotSubPlatform getPlatform(
                    AnimatedBlockHookManager animatedBlockHookManager, IExecutor executor)
                {
                    return null;
                }
            },
        V1_19_R2
            {
                @Override
                public IBigDoorsSpigotSubPlatform getPlatform(
                    AnimatedBlockHookManager animatedBlockHookManager, IExecutor executor)
                {
                    return new BigDoorsSpigotSubPlatform(animatedBlockHookManager, executor);
                }
            },
        ;

        /**
         * @return The instance of the {@link IBigDoorsSpigotSubPlatform} for this {@link Version}.
         */
        public abstract @Nullable IBigDoorsSpigotSubPlatform getPlatform(
            AnimatedBlockHookManager animatedBlockHookManager, IExecutor executor)
            throws UnsupportedOperationException;

        public static Version parseVersion(String version)
        {
            try
            {
                return Version.valueOf(version.toUpperCase(Locale.ENGLISH));
            }
            // No enum constant...
            catch (IllegalArgumentException e)
            {
                return Version.UNSUPPORTED_VERSION;
            }
        }
    }
}
