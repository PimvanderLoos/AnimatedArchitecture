package nl.pim16aap2.bigdoors.spigot.managers;

import nl.pim16aap2.bigdoors.api.IBigDoorsPlatform;
import nl.pim16aap2.bigdoors.api.factories.IPLocationFactory;
import nl.pim16aap2.bigdoors.logging.IPLogger;
import nl.pim16aap2.bigdoors.spigot.BigDoorsPlugin;
import nl.pim16aap2.bigdoors.spigot.util.api.IBigDoorsSpigotSubPlatform;
import nl.pim16aap2.bigdoors.spigot.util.api.ISubPlatformManagerSpigot;
import nl.pim16aap2.bigdoors.spigot.v1_15_R1.BigDoorsSpigotSubPlatform_V1_15_R1;
import nl.pim16aap2.bigdoors.util.Util;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;

public final class SubPlatformManagerSpigot implements ISubPlatformManagerSpigot
{
    private final @Nullable IBigDoorsSpigotSubPlatform spigotPlatform;

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
    public SubPlatformManagerSpigot(BigDoorsPlugin bigDoorsPlugin, IPLogger logger, IPLocationFactory locationFactory)
    {
        Version versionTmp;
        String versionStringTmp;
        @Nullable IBigDoorsSpigotSubPlatform spigotPlatformTmp = null;
        try
        {
            versionStringTmp = Bukkit.getServer().getClass().getPackage().getName().replace(".", ",").split(",")[3];
            versionTmp = Version.valueOf(versionStringTmp);
            if (versionTmp != Version.ERROR)
                spigotPlatformTmp = versionTmp.getPlatform(logger, locationFactory);
        }
        catch (ArrayIndexOutOfBoundsException | IllegalArgumentException e)
        {
            e.printStackTrace();
            versionTmp = Version.ERROR;
            versionStringTmp = Bukkit.getServer().getClass().getPackage().getName().replace(".", ",");
        }
        final Version version = versionTmp;
        final String versionString = versionStringTmp;
        spigotPlatform = spigotPlatformTmp;

        if (version == Version.ERROR)
            throw new RuntimeException("No platform available for version " + versionString);
        Util.requireNonNull(spigotPlatform, "Platform").init(bigDoorsPlugin);
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

    private enum Version
    {
        ERROR
            {
                @Override
                public @Nullable IBigDoorsSpigotSubPlatform getPlatform(IPLogger logger,
                                                                        IPLocationFactory locationFactory)
                {
                    return null;
                }
            },
        V1_15_R1
            {
                @Override
                public IBigDoorsSpigotSubPlatform getPlatform(IPLogger logger, IPLocationFactory locationFactory)
                {
                    return new BigDoorsSpigotSubPlatform_V1_15_R1(logger, locationFactory);
                }
            },
        ;

        /**
         * Obtains the instance of the {@link IBigDoorsSpigotSubPlatform} for this {@link Version}.
         *
         * @return The instance of the {@link IBigDoorsSpigotSubPlatform} for this {@link Version}.
         */
        public abstract @Nullable IBigDoorsSpigotSubPlatform getPlatform(IPLogger logger,
                                                                         IPLocationFactory locationFactory)
            throws UnsupportedOperationException;
    }
}
