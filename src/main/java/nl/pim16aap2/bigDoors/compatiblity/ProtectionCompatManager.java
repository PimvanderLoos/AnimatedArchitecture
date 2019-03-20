package nl.pim16aap2.bigDoors.compatiblity;

import java.util.ArrayList;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.plugin.Plugin;

import nl.pim16aap2.bigDoors.BigDoors;

public class ProtectionCompatManager implements Listener
{
    private final BigDoors plugin;
    private ArrayList<ProtectionCompat> protectionCompats;

    /* This class keeps track of all protection compats.
     * It allows you to test if you can break a block or all blocks
     * between 2 locations for all supported plugins.
     *
     * It first tries to load all supported plugins, but won't print any
     * errors if anything goes wrong, because Spigot cannot guarantee
     * the load order for soft-dependents. This means that this plugin could
     * be loaded BEFORE a given supported plugin.
     *
     * After trying to load all supported plugins loaded before BD, it'll listen
     * for plugin enables events, so it can also load plugins loaded after BD.
     */

    public ProtectionCompatManager(BigDoors plugin)
    {
        this.plugin = plugin;
        protectionCompats = new ArrayList<>();
        loadLoadedPlugins(true);
    }

    public void reload()
    {
        protectionCompats.clear();
        for (Plugin p : plugin.getServer().getPluginManager().getPlugins())
            loadFromPluginName(p.getName());
    }

    private void loadLoadedPlugins(boolean silent)
    {
        loadWorldGuard (silent);
        loadPlotSquared(silent);
    }

    public boolean canBreakBlock(Player player, Location loc)
    {
        for (ProtectionCompat compat : protectionCompats)
            if (!compat.canBreakBlock(player, loc))
                return false;
        return true;
    }

    public boolean canBreakBlocksBetweenLocs(Player player, Location loc1, Location loc2)
    {
        for (ProtectionCompat compat : protectionCompats)
            if (!compat.canBreakBlocksBetweenLocs(player, loc1, loc2))
                return false;
        return true;
    }

    private <T> boolean protectionAlreadyLoaded(Class<T> compatType)
    {
        for (ProtectionCompat compat : protectionCompats)
            if (compat.getClass().isInstance(compatType))
                return true;
        return false;
    }

    private void addProtectionCompat(ProtectionCompat hook)
    {
        if (hook.success())
        {
            protectionCompats.add(hook);
            plugin.getMyLogger().logMessageToConsole("Successfully hooked into \"" + hook.getPlugin().getName() + "\"!");
        }
        else
            plugin.getMyLogger().logMessageToConsole("Failed to hook into \"" + hook.getPlugin().getName() + "\"!");
    }

    @EventHandler(priority = EventPriority.MONITOR)
    protected void onPluginEnable(final PluginEnableEvent event)
    {
        loadFromPluginName(event.getPlugin().getName());
    }

    private void loadFromPluginName(String pluginName)
    {
        switch (pluginName)
        {
        case "PlotSquared":
            loadPlotSquared(false);
            break;
        case "WorldGuard":
            loadWorldGuard(false);
            break;
        }
    }

    private void loadWorldGuard(boolean silent)
    {
        silent = false;
        if (plugin.getConfigLoader().worldGuardHook() &&
            plugin.getServer().getPluginManager().getPlugin("WorldGuard") != null)
        {
            try
            {
                ProtectionCompat protectionCompat;
                String WGVersion = plugin.getServer().getPluginManager().getPlugin("WorldGuard").getDescription().getVersion();
                if (WGVersion.startsWith("7."))
                {
                    if (protectionAlreadyLoaded(WorldGuard7ProtectionCompat.class))
                        return;
                    plugin.getMyLogger().logMessageToConsoleOnly("WorldGuard v7 detected!");
                    protectionCompat = new WorldGuard7ProtectionCompat(plugin);
                }
                else if (WGVersion.startsWith("6."))
                {
                    if (protectionAlreadyLoaded(WorldGuard6ProtectionCompat.class))
                        return;
                    plugin.getMyLogger().logMessageToConsoleOnly("WorldGuard v6 detected!");
                    protectionCompat = new WorldGuard6ProtectionCompat(plugin);
                }
                else
                {
                    plugin.getMyLogger().logMessageToConsole("Version " +
                        WGVersion + " is not supported! If you believe this is in error, please contact pim16aap2.");
                    return;
                }
                addProtectionCompat(protectionCompat);
            }
            catch (NoClassDefFoundError e)
            {
                if (!silent)
                {
                    plugin.getMyLogger().logMessageToConsole("NoClassDefFoundError: "
                        + "Failed to initialize WorldGuard compatibility hook! "
                        + "Only v7 seems to be supported atm! Maybe that's the issue?");
                    plugin.getMyLogger().logMessageToConsole(
                          "Now resuming normal startup with Worldguard Compatibility Hook disabled!");
                }
            }
            catch (Exception e)
            {
                if (!silent)
                {
                    plugin.getMyLogger().logMessageToConsole(
                          "Failed to initialize WorldGuard compatibility hook!");
                    e.printStackTrace();
                    plugin.getMyLogger().logMessageToConsole(
                          "Now resuming normal startup with Worldguard Compatibility Hook disabled!");
                }
            }
        }
    }

    private void loadPlotSquared(boolean silent)
    {
        silent = false;
        if (plugin.getConfigLoader().plotSquaredHook() &&
            plugin.getServer().getPluginManager().getPlugin("PlotSquared") != null)
        {
            try
            {
                ProtectionCompat plotSquaredCompat;
                String PSVersion = plugin.getServer().getPluginManager().getPlugin("PlotSquared").getDescription()
                    .getVersion();
                if (PSVersion.startsWith("4."))
                {
                    if (protectionAlreadyLoaded(PlotSquaredNewProtectionCompat.class))
                        return;
                    plugin.getMyLogger().logMessageToConsoleOnly("PlotSquared v4 detected!");
                    plotSquaredCompat = new PlotSquaredNewProtectionCompat(plugin);
                }
                else
                {
                    if (protectionAlreadyLoaded(PlotSquaredOldProtectionCompat.class))
                        return;
                    plugin.getMyLogger().logMessageToConsoleOnly("PlotSquared v3 detected!");
                    plotSquaredCompat = new PlotSquaredOldProtectionCompat(plugin);
                }
                addProtectionCompat(plotSquaredCompat);
            }
            catch (NoClassDefFoundError e)
            {
                if (!silent)
                {
                    plugin.getMyLogger().logMessageToConsole("NoClassDefFoundError: "
                        + "Failed to initialize PlotSquared compatibility hook! Perhaps this version isn't supported? Check error.log for more info!");
                    plugin.getMyLogger().logMessageToConsole(
                          "Now resuming normal startup with PlotSquared Compatibility Hook disabled!");
                }
            }
            catch (Exception e)
            {
                if (!silent)
                {
                    plugin.getMyLogger().logMessageToConsole(
                          "Failed to initialize PlotSquared compatibility hook!");
                    e.printStackTrace();
                    plugin.getMyLogger().logMessageToConsole(
                          "Now resuming normal startup with PlotSquared Compatibility Hook disabled!");
                }
            }
        }
    }
}
