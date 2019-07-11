package nl.pim16aap2.bigdoors.compatiblity;

import nl.pim16aap2.bigdoors.config.ConfigLoader;

import java.util.HashMap;
import java.util.Map;

/**
 * Enum of all ProtectionCompats that can be loaded and some methods to help load them.
 *
 * @author Pim
 */
public enum ProtectionCompat
{
    PLOTSQUARED("PlotSquared")
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

                /**
                 * {@inheritDoc}
                 */
                @Override
                public boolean isEnabled(ConfigLoader config)
                {
                    return config.plotSquaredHook();
                }
            },
    WORLDGUARD("WorldGuard")
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

                /**
                 * {@inheritDoc}
                 */
                @Override
                public boolean isEnabled(ConfigLoader config)
                {
                    return config.worldGuardHook();
                }
            },
    GRIEFPREVENTION("GriefPrevention")
            {
                /**
                 * {@inheritDoc}
                 */
                @Override
                public Class<? extends IProtectionCompat> getClass(final String version)
                {
                    return GriefPreventionProtectionCompat.class;
                }

                /**
                 * {@inheritDoc}
                 */
                @Override
                public boolean isEnabled(ConfigLoader config)
                {
                    return config.griefPreventionHook();
                }
            },
    ;

    private static final Map<String, ProtectionCompat> nameMap = new HashMap<>();

    static
    {
        for (ProtectionCompat compat : ProtectionCompat.values())
            nameMap.put(ProtectionCompat.getName(compat), compat);
    }

    private final String name;

    ProtectionCompat(final String name)
    {
        this.name = name;
    }

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
        if (nameMap.containsKey(name))
            return nameMap.get(name);
        return null;
    }

    /**
     * Get the function in the config that determines if the compat is enabled in the config.
     *
     * @param config The config loader to query for if this compat is enabled.
     * @return The function in the config that determines if the compat is enabled in the config.
     */
    public abstract boolean isEnabled(ConfigLoader config);

    /**
     * Get the class of the given hook for a specific version of the plugin to load the compat for.
     *
     * @param version The version of the plugin to load the hook for.
     * @return The {@link IProtectionCompat} class of the compat.
     */
    public abstract Class<? extends IProtectionCompat> getClass(final String version);
}
