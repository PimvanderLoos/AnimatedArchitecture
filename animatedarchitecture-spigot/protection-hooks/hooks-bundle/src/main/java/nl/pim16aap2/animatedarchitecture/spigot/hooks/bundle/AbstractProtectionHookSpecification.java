package nl.pim16aap2.animatedarchitecture.spigot.hooks.bundle;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.extern.flogger.Flogger;
import nl.pim16aap2.animatedarchitecture.spigot.hooks.griefdefender2.GriefDefender2ProtectionHook;
import nl.pim16aap2.animatedarchitecture.spigot.hooks.griefprevention.GriefPreventionProtectionHook;
import nl.pim16aap2.animatedarchitecture.spigot.hooks.lands.LandsProtectionHook;
import nl.pim16aap2.animatedarchitecture.spigot.hooks.plotsquared6.PlotSquared6ProtectionHook;
import nl.pim16aap2.animatedarchitecture.spigot.hooks.plotsquared7.PlotSquared7ProtectionHook;
import nl.pim16aap2.animatedarchitecture.spigot.hooks.redprotect.RedProtectProtectionHook;
import nl.pim16aap2.animatedarchitecture.spigot.hooks.towny.TownyProtectionHook;
import nl.pim16aap2.animatedarchitecture.spigot.hooks.worldguard7.WorldGuard7ProtectionHook;
import nl.pim16aap2.animatedarchitecture.spigot.util.hooks.IProtectionHookSpigot;
import nl.pim16aap2.animatedarchitecture.spigot.util.hooks.IProtectionHookSpigotSpecification;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Represents a definition of a protection hook.
 */
@SuppressWarnings("unused")
@Flogger
@ToString
@EqualsAndHashCode
public abstract class AbstractProtectionHookSpecification implements IProtectionHookSpigotSpecification
{
    public static final IProtectionHookSpigotSpecification TOWNY = new AbstractProtectionHookSpecification("Towny")
    {
        @Override
        public Class<? extends IProtectionHookSpigot> getClass(final String version)
        {
            return TownyProtectionHook.class;
        }
    };

    public static final IProtectionHookSpigotSpecification PLOT_SQUARED = new AbstractProtectionHookSpecification(
        "PlotSquared")
    {
        @Override
        public @Nullable Class<? extends IProtectionHookSpigot> getClass(final String version)
        {
            if (version.length() < 2)
                return null;

            final String majorVersion = version.substring(0, 2);
            return switch (majorVersion)
            {
                case "6." -> PlotSquared6ProtectionHook.class;
                case "7." -> PlotSquared7ProtectionHook.class;
                default ->
                {
                    logUnsupportedVersion("PlotSquared", version);
                    yield null;
                }
            };
        }
    };

    public static final IProtectionHookSpigotSpecification WORLD_GUARD = new AbstractProtectionHookSpecification(
        "WorldGuard")
    {
        @Override
        public @Nullable Class<? extends IProtectionHookSpigot> getClass(final String version)
        {
            if (version.length() < 2)
                return null;

            if (version.startsWith("7."))
                return WorldGuard7ProtectionHook.class;
            else
            {
                logUnsupportedVersion("WorldGuard", version);
                return null;
            }
        }
    };

    public static final IProtectionHookSpigotSpecification GRIEF_PREVENTION = new AbstractProtectionHookSpecification(
        "GriefPrevention")
    {
        @Override
        public Class<? extends IProtectionHookSpigot> getClass(final String version)
        {
            return GriefPreventionProtectionHook.class;
        }
    };

    public static final IProtectionHookSpigotSpecification LANDS = new AbstractProtectionHookSpecification("Lands")
    {
        @Override
        public Class<? extends IProtectionHookSpigot> getClass(final String version)
        {
            return LandsProtectionHook.class;
        }
    };

    public static final IProtectionHookSpigotSpecification RED_PROTECT = new AbstractProtectionHookSpecification(
        "RedProtect")
    {
        @Override
        public Class<? extends IProtectionHookSpigot> getClass(final String version)
        {
            return RedProtectProtectionHook.class;
        }
    };

    public static final IProtectionHookSpigotSpecification GRIEF_DEFENDER = new AbstractProtectionHookSpecification(
        "GriefDefender")
    {
        @Override
        public @Nullable Class<? extends IProtectionHookSpigot> getClass(String version)
        {
            if (version.startsWith("2"))
                return GriefDefender2ProtectionHook.class;
            else
            {
                logUnsupportedVersion("GriefDefender", version);
                return null;
            }
        }
    };

    /**
     * The set of default protection hook definitions.
     * <p>
     * More can be added by external plugins if needed.
     */
    public static final List<IProtectionHookSpigotSpecification> DEFAULT_HOOK_DEFINITIONS =
        List.of(TOWNY, PLOT_SQUARED, WORLD_GUARD, GRIEF_PREVENTION, LANDS, RED_PROTECT, GRIEF_DEFENDER);

    private final String name;

    public AbstractProtectionHookSpecification(String name)
    {
        this.name = name;
    }

    private static void logUnsupportedVersion(String hook, String version)
    {
        log.atSevere().log("No hook exists for '%s' version '%s'", hook, version);
    }

    @Override
    public String getName()
    {
        return name;
    }
}
