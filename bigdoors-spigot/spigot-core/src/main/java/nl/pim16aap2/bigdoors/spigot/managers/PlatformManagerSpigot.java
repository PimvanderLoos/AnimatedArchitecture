package nl.pim16aap2.bigdoors.spigot.managers;

import lombok.NonNull;
import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.spigot.BigDoorsSpigot;
import nl.pim16aap2.bigdoors.spigot.util.api.IPlatformManagerSpigot;
import nl.pim16aap2.bigdoors.spigot.util.api.ISpigotPlatform;
import nl.pim16aap2.bigdoors.spigot.v1_15_R1.SpigotPlatform_V1_15_R1;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class PlatformManagerSpigot implements IPlatformManagerSpigot
{
    @NotNull
    private static final PlatformManagerSpigot INSTANCE = new PlatformManagerSpigot();
    @Nullable
    private static final ISpigotPlatform spigotPlatform;
    @NotNull
    private static final Version spigotVersion;

    static
    {
        Version version;
        ISpigotPlatform spigotPlatformTmp = null;
        try
        {
            String versionStr = Bukkit.getServer().getClass().getPackage().getName().replace(".", ",")
                                      .split(",")[3];
            version = Version.valueOf(versionStr);
            if (version != Version.ERROR)
                spigotPlatformTmp = version.getPlatform();
        }
        catch (final ArrayIndexOutOfBoundsException | IllegalArgumentException e)
        {
            BigDoors.get().getPLogger().logThrowable(e);
            version = Version.ERROR;
            spigotPlatformTmp = null;
        }
        spigotVersion = version;
        spigotPlatform = spigotPlatformTmp;
    }

    /**
     * Gets the instance of this class.
     *
     * @return The instance of this class.
     */
    public static @NotNull PlatformManagerSpigot get()
    {
        return INSTANCE;
    }

    @Override
    public @NonNull ISpigotPlatform getSpigotPlatform()
    {
        if (spigotPlatform == null)
        {
            IllegalStateException e = new IllegalStateException("No Spigot platform currently registered!");
            BigDoors.get().getPLogger().logThrowable(e);
            throw e;
        }
        return spigotPlatform;
    }

    /**
     * Initializes the correct platform.
     *
     * @param bigDoorsSpigot The {@link BigDoorsSpigot} instance.
     * @return True if a valid platform was found for the current version.
     */
    public boolean initPlatform(final @NotNull BigDoorsSpigot bigDoorsSpigot)
    {
        if (spigotPlatform == null)
        {
            BigDoors.get().getPLogger().logThrowable(new NullPointerException("Could not load Spigot platform for " +
                                                                                  "version " + spigotVersion.name()));
            return false;
        }
        spigotPlatform.init(bigDoorsSpigot);
        return true;
    }

    private enum Version
    {
        ERROR
            {
                @Override
                public ISpigotPlatform getPlatform()
                {
                    return null;
                }
            },
        v1_15_R1
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
