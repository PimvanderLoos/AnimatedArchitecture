package nl.pim16aap2.bigdoors.spigot.managers;

import nl.pim16aap2.bigdoors.api.IBigDoorsPlatform;
import nl.pim16aap2.bigdoors.api.factories.IPLocationFactory;
import nl.pim16aap2.bigdoors.spigot.BigDoorsPlugin;
import nl.pim16aap2.bigdoors.spigot.util.api.IBigDoorsSpigotSubPlatform;
import nl.pim16aap2.bigdoors.spigot.util.api.ISubPlatformManagerSpigot;
import nl.pim16aap2.bigdoors.spigot.v1_15_R1.BigDoorsSpigotSubPlatform_V1_15_R1;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;

public final class SubPlatformManagerSpigot implements ISubPlatformManagerSpigot
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
    public SubPlatformManagerSpigot(BigDoorsPlugin bigDoorsPlugin, IPLocationFactory locationFactory)
    {
        Version versionTmp;
        @Nullable IBigDoorsSpigotSubPlatform spigotPlatformTmp = null;
        try
        {
            final String versionStringTmp =
                Bukkit.getServer().getClass().getPackage().getName().replace(".", ",").split(",")[3];
            versionTmp = Version.parseVersion(versionStringTmp);
            if (versionTmp != Version.UNSUPPORTED_VERSION)
                spigotPlatformTmp = versionTmp.getPlatform(locationFactory);
        }
        catch (ArrayIndexOutOfBoundsException | IllegalArgumentException e)
        {
            e.printStackTrace();
            versionTmp = Version.UNSUPPORTED_VERSION;
        }
        serverVersion = Bukkit.getServer().getClass().getPackage().getName();
        subPlatformVersion = versionTmp;
        spigotPlatform = spigotPlatformTmp;

        if (spigotPlatform != null)
            spigotPlatform.init(bigDoorsPlugin);
    }

    @Override
    public boolean isValidPlatform()
    {
        return spigotPlatform != null;
    }

    @Override
    public IBigDoorsSpigotSubPlatform getSpigotPlatform()
    {
        if (spigotPlatform == null)
            throw new IllegalStateException("No Spigot platform currently registered!");
        return spigotPlatform;
    }

    @Override
    public String getSubPlatformVersion()
    {
        return subPlatformVersion.name();
    }

    @Override
    public String getServerVersion()
    {
        return serverVersion;
    }

    private enum Version
    {
        UNSUPPORTED_VERSION
            {
                @Override
                public @Nullable IBigDoorsSpigotSubPlatform getPlatform(IPLocationFactory locationFactory)
                {
                    return null;
                }
            },
        V1_15_R1
            {
                @Override
                public IBigDoorsSpigotSubPlatform getPlatform(IPLocationFactory locationFactory)
                {
                    return new BigDoorsSpigotSubPlatform_V1_15_R1(locationFactory);
                }
            },
        ;

        /**
         * Obtains the instance of the {@link IBigDoorsSpigotSubPlatform} for this {@link Version}.
         *
         * @return The instance of the {@link IBigDoorsSpigotSubPlatform} for this {@link Version}.
         */
        public abstract @Nullable IBigDoorsSpigotSubPlatform getPlatform(IPLocationFactory locationFactory)
            throws UnsupportedOperationException;

        public static Version parseVersion(String version)
        {
            try
            {
                return Version.valueOf(version);
            }
            // No enum constant...
            catch (IllegalArgumentException e)
            {
                return Version.UNSUPPORTED_VERSION;
            }
        }
    }
}
