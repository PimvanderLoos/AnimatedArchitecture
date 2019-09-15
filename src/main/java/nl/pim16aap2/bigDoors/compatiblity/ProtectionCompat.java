package nl.pim16aap2.bigDoors.compatiblity;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Enum of all ProtectionCompats that can be loaded and some methods to help
 * load them.
 *
 * @author Pim
 */
public enum ProtectionCompat
{
    TOWNY ("Towny")
    {
        /**
         * {@inheritDoc}
         */
        @Override
        public Class<? extends IProtectionCompat> getClass(final String version)
        {
            boolean newVersion = false;
            int[] lastOldVersion = {0, 94, 0, 1};

            int[] currentVersion = Arrays.stream(version.split("\\.")).mapToInt(Integer::parseInt).toArray();
            for (int idx = 0; idx < lastOldVersion.length; ++idx)
            {
                if (currentVersion[idx] > lastOldVersion[idx])
                {
                    newVersion = true;
                    break;
                }
            }
            return newVersion ? TownyNewProtectionCompat.class : TownyOldProtectionCompat.class;
        }
    },
    PLOTSQUARED ("PlotSquared")
    {
        /**
         * {@inheritDoc}
         */
        @Override
        public Class<? extends IProtectionCompat> getClass(final String version)
        {
            return version.startsWith("4.") ? PlotSquaredNewProtectionCompat.class :
                PlotSquaredOldProtectionCompat.class;
        }
    },
    WORLDGUARD ("WorldGuard")
    {
        /**
         * {@inheritDoc}
         */
        @Override
        public Class<? extends IProtectionCompat> getClass(final String version)
        {
            if (version.startsWith("7."))
                return WorldGuard7ProtectionCompat.class;
            else if (version.startsWith("6."))
                return WorldGuard6ProtectionCompat.class;
            else
                return null;
        }
    },
    GRIEFPREVENTION ("GriefPrevention")
    {
        /**
         * {@inheritDoc}
         */
        @Override
        public Class<? extends IProtectionCompat> getClass(final String version)
        {
            return GriefPreventionProtectionCompat.class;
        }
    },;

    private final String name;
    private static final Map<String, ProtectionCompat> nameMap = new HashMap<>();

    ProtectionCompat(final String name)
    {
        this.name = name;
    }

    /**
     * Get the class of the given hook for a specific version of the plugin to load
     * the compat for.
     *
     * @param version The version of the plugin to load the hook for.
     * @return The {@link IProtectionCompat} class of the compat.
     */
    public abstract Class<? extends IProtectionCompat> getClass(final String version);

    /**
     * Get the name of the plugin the given compat hooks into.
     *
     * @param compat The compat the get the name of the plugin for.
     * @return The name of the plugin the given compat hooks into.
     */
    public static String getName(final ProtectionCompat compat)
    {
        return compat.name;
    }

    /**
     * Get the compat for a plugin.
     *
     * @param name The name of the plugin to get the compat for.
     * @return The compat for a plugin.
     */
    public static ProtectionCompat getFromName(final String name)
    {
        return nameMap.get(name);
    }

    static
    {
        for (ProtectionCompat compat : ProtectionCompat.values())
            nameMap.put(ProtectionCompat.getName(compat), compat);
    }
}
