package nl.pim16aap2.bigdoors.spigot.managers;

import lombok.extern.flogger.Flogger;
import nl.pim16aap2.bigdoors.api.IBigDoorsPlatform;
import nl.pim16aap2.bigdoors.managers.AnimatedBlockHookManager;
import nl.pim16aap2.bigdoors.spigot.BigDoorsPlugin;
import nl.pim16aap2.bigdoors.spigot.util.api.IBigDoorsSpigotSubPlatform;
import nl.pim16aap2.bigdoors.spigot.v1_15_R1.BigDoorsSpigotSubPlatform_V1_15_R1;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Locale;
import java.util.logging.Level;

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
    public SubPlatformManager(BigDoorsPlugin bigDoorsPlugin, AnimatedBlockHookManager animatedBlockHookManager)
    {
        serverVersion = Bukkit.getServer().getClass().getPackage().getName();

        Version versionTmp;
        @Nullable IBigDoorsSpigotSubPlatform spigotPlatformTmp = null;
        try
        {
            final String versionStringTmp = serverVersion.split("\\.")[3];
            versionTmp = Version.parseVersion(versionStringTmp);
            if (versionTmp != Version.UNSUPPORTED_VERSION)
                spigotPlatformTmp = versionTmp.getPlatform(animatedBlockHookManager);
        }
        catch (ArrayIndexOutOfBoundsException | IllegalArgumentException e)
        {
            log.at(Level.SEVERE).withCause(e)
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
                    AnimatedBlockHookManager animatedBlockHookManager)
                {
                    return null;
                }
            },
        V1_15_R1
            {
                @Override
                public IBigDoorsSpigotSubPlatform getPlatform(AnimatedBlockHookManager animatedBlockHookManager)
                {
                    return new BigDoorsSpigotSubPlatform_V1_15_R1(animatedBlockHookManager);
                }
            },
        ;

        /**
         * @return The instance of the {@link IBigDoorsSpigotSubPlatform} for this {@link Version}.
         */
        public abstract @Nullable IBigDoorsSpigotSubPlatform getPlatform(
            AnimatedBlockHookManager animatedBlockHookManager)
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
