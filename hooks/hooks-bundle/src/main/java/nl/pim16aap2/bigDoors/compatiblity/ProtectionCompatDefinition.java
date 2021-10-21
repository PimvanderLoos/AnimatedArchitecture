package nl.pim16aap2.bigDoors.compatiblity;

import com.sun.tools.javac.util.List;
import nl.pim16aap2.bigDoors.compatibility.IProtectionCompat;
import nl.pim16aap2.bigDoors.compatibility.IProtectionCompatDefinition;

import java.util.Arrays;

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
            int[] lastOldVersion = { 0, 94, 0, 1 };

            int[] currentVersion = Arrays.stream(version.split("\\.")).mapToInt(Integer::parseInt).toArray();
            for (int idx = 0; idx < lastOldVersion.length; ++idx)
            {
                if (currentVersion[idx] == lastOldVersion[idx])
                    continue;

                return currentVersion[idx] > lastOldVersion[idx] ? TownyNewProtectionCompat.class :
                    TownyOldProtectionCompat.class;
            }
            return null;
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
            case "3.":
                return PlotSquared3ProtectionCompat.class;
            case "4.":
                return PlotSquared4ProtectionCompat.class;
            case "5.":
                return PlotSquared5ProtectionCompat.class;
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

    public static final List<IProtectionCompatDefinition> DEFAULT_COMPAT_DEFINITIONS =
        List.of(TOWNY, PLOTSQUARED, WORLDGUARD, GRIEFPREVENTION, LANDS, REDPROTECT, GRIEF_DEFENDER);

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
