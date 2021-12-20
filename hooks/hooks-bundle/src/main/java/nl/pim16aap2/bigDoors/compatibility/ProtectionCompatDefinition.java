package nl.pim16aap2.bigDoors.compatibility;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Represents a definition of a protection compat.
 *
 * @author Pim
 */
public abstract class ProtectionCompatDefinition implements IProtectionCompatDefinition
{
    public static final IProtectionCompatDefinition TOWNY = new ProtectionCompatDefinition("Towny")
    {
        @Override
        public Class<? extends IProtectionCompat> getClass(final String version)
        {
            return TownyProtectionCompat.class;
        }
    };

    public static final IProtectionCompatDefinition PLOTSQUARED = new ProtectionCompatDefinition("PlotSquared")
    {
        @Override
        public Class<? extends IProtectionCompat> getClass(final String version)
        {
            if (version.length() < 2)
                return null;

            final String majorVersion = version.substring(0, 2);
            switch (majorVersion)
            {
            case "20": // Version 3 can sometimes be reported as "20.03.28", apparently?
            case "3.":
                return PlotSquared3ProtectionCompat.class;
            case "4.":
                return PlotSquared4ProtectionCompat.class;
            case "5.":
                return PlotSquared5ProtectionCompat.class;
            case "6.":
                return PlotSquared6ProtectionCompat.class;
            default:
                return null;
            }
        }
    };

    public static final IProtectionCompatDefinition WORLDGUARD = new ProtectionCompatDefinition("WorldGuard")
    {
        @Override
        public Class<? extends IProtectionCompat> getClass(final String version)
        {
            if (version.length() < 2)
                return null;

            if (version.startsWith("7."))
                return WorldGuard7ProtectionCompat.class;
            else if (version.startsWith("6."))
                return WorldGuard6ProtectionCompat.class;
            else
                return null;
        }
    };

    public static final IProtectionCompatDefinition GRIEFPREVENTION = new ProtectionCompatDefinition("GriefPrevention")
    {
        @Override
        public Class<? extends IProtectionCompat> getClass(final String version)
        {
            return GriefPreventionProtectionCompat.class;
        }
    };

    public static final IProtectionCompatDefinition LANDS = new ProtectionCompatDefinition("Lands")
    {
        @Override
        public Class<? extends IProtectionCompat> getClass(final String version)
        {
            return LandsProtectionCompat.class;
        }
    };

    public static final IProtectionCompatDefinition REDPROTECT = new ProtectionCompatDefinition("RedProtect")
    {
        @Override
        public Class<? extends IProtectionCompat> getClass(final String version)
        {
            return RedProtectProtectionCompat.class;
        }
    };

    public static final IProtectionCompatDefinition GRIEF_DEFENDER = new ProtectionCompatDefinition("GriefDefender")
    {
        @Override
        public Class<? extends IProtectionCompat> getClass(String version)
        {
            return GriefDefenderProtectionCompat.class;
        }
    };

    public static final List<IProtectionCompatDefinition> DEFAULT_COMPAT_DEFINITIONS = Collections.unmodifiableList(
        Arrays.asList(TOWNY, PLOTSQUARED, WORLDGUARD, GRIEFPREVENTION, LANDS, REDPROTECT, GRIEF_DEFENDER));

    private final String name;

    private ProtectionCompatDefinition(final String name)
    {
        this.name = name;
    }

    @Override
    public String getName()
    {
        return name;
    }
}
