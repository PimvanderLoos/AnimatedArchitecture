package nl.pim16aap2.bigdoors.spigot.managers;

import nl.pim16aap2.bigdoors.api.IBigDoorsPlatform;
import nl.pim16aap2.bigdoors.logging.IPLogger;
import nl.pim16aap2.bigdoors.spigot.BigDoorsSpigot;
import nl.pim16aap2.bigdoors.spigot.util.api.IPlatformManagerSpigot;
import nl.pim16aap2.bigdoors.spigot.util.api.ISpigotPlatform;
import nl.pim16aap2.bigdoors.spigot.v1_15_R1.SpigotPlatform_V1_15_R1;
import nl.pim16aap2.bigdoors.util.Util;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;

public final class PlatformManagerSpigot implements IPlatformManagerSpigot
{
    private final @Nullable ISpigotPlatform spigotPlatform;
    private final Version version;
    private final String versionString;

    /**
     * Instantiates the platform manager and initializes the version-specific platform with the provided Spigot
     * platform.
     *
     * @param bigDoorsSpigot
     *     The {@link IBigDoorsPlatform} for Spigot.
     * @throws InstantiationException
     *     When there is no version-specific platform available. This may happen when trying to instantiate this class
     *     on an unsupported version.
     */
    @Inject
    public PlatformManagerSpigot(BigDoorsSpigot bigDoorsSpigot, IPLogger logger)
    {
        Version versionTmp;
        String versionStringTmp;
        @Nullable ISpigotPlatform spigotPlatformTmp = null;
        try
        {
            versionStringTmp = Bukkit.getServer().getClass().getPackage().getName().replace(".", ",").split(",")[3];
            versionTmp = Version.valueOf(versionStringTmp);
            if (versionTmp != Version.ERROR)
                spigotPlatformTmp = versionTmp.getPlatform(logger);
        }
        catch (ArrayIndexOutOfBoundsException | IllegalArgumentException e)
        {
            e.printStackTrace();
            versionTmp = Version.ERROR;
            versionStringTmp = Bukkit.getServer().getClass().getPackage().getName().replace(".", ",");
        }
        version = versionTmp;
        versionString = versionStringTmp;
        spigotPlatform = spigotPlatformTmp;

        if (version == Version.ERROR)
            throw new RuntimeException("No platform available for version " + versionString);
        Util.requireNonNull(spigotPlatform, "Platform").init(bigDoorsSpigot);
    }

    @Override
    public boolean isValidPlatform()
    {
        return spigotPlatform != null;
    }

    @Override
    public ISpigotPlatform getSpigotPlatform()
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
                public @Nullable ISpigotPlatform getPlatform(IPLogger logger)
                {
                    return null;
                }
            },
        V1_15_R1
            {
                @Override
                public ISpigotPlatform getPlatform(IPLogger logger)
                {
                    return new SpigotPlatform_V1_15_R1(logger);
                }
            },
        ;

        /**
         * Obtains the instance of the {@link ISpigotPlatform} for this {@link Version}.
         *
         * @return The instance of the {@link ISpigotPlatform} for this {@link Version}.
         */
        public abstract @Nullable ISpigotPlatform getPlatform(IPLogger logger)
            throws UnsupportedOperationException;
    }
}
