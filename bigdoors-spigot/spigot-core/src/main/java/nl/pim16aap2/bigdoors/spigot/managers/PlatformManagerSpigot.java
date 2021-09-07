package nl.pim16aap2.bigdoors.spigot.managers;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.api.IBigDoorsPlatform;
import nl.pim16aap2.bigdoors.spigot.BigDoorsSpigot;
import nl.pim16aap2.bigdoors.spigot.util.api.IPlatformManagerSpigot;
import nl.pim16aap2.bigdoors.spigot.util.api.ISpigotPlatform;
import nl.pim16aap2.bigdoors.spigot.v1_15_R1.SpigotPlatform_V1_15_R1;
import nl.pim16aap2.bigdoors.util.Util;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.Nullable;

public final class PlatformManagerSpigot implements IPlatformManagerSpigot
{
    private static final @Nullable ISpigotPlatform SPIGOT_PLATFORM;
    private static final Version SPIGOT_VERSION;
    private static final String VERSION_STRING;

    static
    {
        Version version;
        @Nullable ISpigotPlatform spigotPlatformTmp = null;
        String versionStr;
        try
        {
            versionStr = Bukkit.getServer().getClass().getPackage().getName().replace(".", ",").split(",")[3];
            version = Version.valueOf(versionStr);
            if (version != Version.ERROR)
                spigotPlatformTmp = version.getPlatform();
        }
        catch (ArrayIndexOutOfBoundsException | IllegalArgumentException e)
        {
            BigDoors.get().getPLogger().logThrowable(e);
            version = Version.ERROR;
            versionStr = Bukkit.getServer().getClass().getPackage().getName().replace(".", ",");
        }
        SPIGOT_VERSION = version;
        SPIGOT_PLATFORM = spigotPlatformTmp;
        VERSION_STRING = versionStr;
    }

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
    public PlatformManagerSpigot(BigDoorsSpigot bigDoorsSpigot)
        throws InstantiationException
    {
        if (SPIGOT_VERSION == Version.ERROR)
            throw new InstantiationException("No platform available for version " + VERSION_STRING);
        Util.requireNonNull(SPIGOT_PLATFORM, "Platform").init(bigDoorsSpigot);
    }

    @Override
    public ISpigotPlatform getSpigotPlatform()
    {
        if (SPIGOT_PLATFORM == null)
        {
            final IllegalStateException e = new IllegalStateException("No Spigot platform currently registered!");
            BigDoors.get().getPLogger().logThrowable(e);
            throw e;
        }
        return SPIGOT_PLATFORM;
    }

    private enum Version
    {
        ERROR
            {
                @Override
                public @Nullable ISpigotPlatform getPlatform()
                {
                    return null;
                }
            },
        V1_15_R1
            {
                @Override
                public ISpigotPlatform getPlatform()
                {
                    return SpigotPlatform_V1_15_R1.get();
                }
            },
        ;

        /**
         * Obtains the instance of the {@link ISpigotPlatform} for this {@link Version}.
         *
         * @return The instance of the {@link ISpigotPlatform} for this {@link Version}.
         */
        public abstract @Nullable ISpigotPlatform getPlatform()
            throws UnsupportedOperationException;
    }
}
