package nl.pim16aap2.bigDoors.compatiblity;

import java.util.ArrayList;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.plugin.Plugin;

import nl.pim16aap2.bigDoors.BigDoors;
import nl.pim16aap2.bigDoors.util.Util;

public class ProtectionCompatManager implements Listener
{
    private final BigDoors plugin;
    private final ArrayList<ProtectionCompat> protectionCompats;

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

    private Player getPlayer(UUID playerUUID, World world)
    {
        Player player = Bukkit.getPlayer(playerUUID);
        if (player == null)
            player = plugin.getFakePlayerCreator().getFakePlayer(Bukkit.getOfflinePlayer(playerUUID), world);
        return player;
    }

    private boolean canByPass(Player player)
    {
        return player.isOp() || player.hasPermission("bigdoors.admin.bypasscompat");
    }

    public String canBreakBlock(Player player, Location loc)
    {
        if (canByPass(player))
            return null;

        for (ProtectionCompat compat : protectionCompats)
            try
            {
                if (!compat.canBreakBlock(getPlayer(player.getUniqueId(), loc.getWorld()), loc))
                    return compat.getName();
            }
            catch(Exception e)
            {
                plugin.getMyLogger().warn("Failed to use \"" + compat.getPlugin().getName() + "\"! Please send this error to pim16aap2:");
                e.printStackTrace();
                plugin.getMyLogger().logMessageToLogFile(Util.exceptionToString(e));
            }
        return null;
    }

    public String canBreakBlocksBetweenLocs(Player player, Location loc1, Location loc2)
    {
        if (canByPass(player))
            return null;

        for (ProtectionCompat compat : protectionCompats)
            try
            {
                if (!compat.canBreakBlocksBetweenLocs(getPlayer(player.getUniqueId(), loc1.getWorld()), loc1, loc2))
                    return compat.getName();
            }
            catch(Exception e)
            {
                plugin.getMyLogger().warn("Failed to use \"" + compat.getPlugin().getName() + "\"! Please send this error to pim16aap2:");
                e.printStackTrace();
                plugin.getMyLogger().logMessageToLogFile(Util.exceptionToString(e));
            }
        return null;
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
            plugin.getMyLogger().info("Successfully hooked into \"" + hook.getPlugin().getName() + "\"!");
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
        if (plugin.getConfigLoader().worldGuardHook())
            try
            {
                ProtectionCompat protectionCompat;
                String WGVersion = plugin.getServer().getPluginManager().getPlugin("WorldGuard").getDescription().getVersion();
                if (WGVersion.startsWith("7."))
                {
                    if (protectionAlreadyLoaded(WorldGuard7ProtectionCompat.class))
                        return;
                    plugin.getMyLogger().info("WorldGuard v7 detected!");
                    protectionCompat = new WorldGuard7ProtectionCompat(plugin);
                }
                else if (WGVersion.startsWith("6."))
                {
                    if (protectionAlreadyLoaded(WorldGuard6ProtectionCompat.class))
                        return;
                    plugin.getMyLogger().info("WorldGuard v6 detected!");
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
                        + "Failed to initialize WorldGuard compatibility hook!");
                    plugin.getMyLogger().logMessageToConsole(
                          "Now resuming normal startup with Worldguard Compatibility Hook disabled!");
                }
            }
            catch (NullPointerException e)
            {
                plugin.getMyLogger().logMessageToConsole("Could not find PlotSquared! Hook not enabled!");
            }
            catch (Exception e)
            {
                if (!silent)
                {
                    plugin.getMyLogger().logMessageToConsole(
                          "Failed to initialize WorldGuard compatibility hook!");
                    plugin.getMyLogger().logMessageToConsole(
                          "Now resuming normal startup with Worldguard Compatibility Hook disabled!");
                }
            }
    }

    private void loadPlotSquared(boolean silent)
    {
        silent = false;
        if (plugin.getConfigLoader().plotSquaredHook())
            try
            {
                ProtectionCompat plotSquaredCompat;
                String PSVersion = plugin.getServer().getPluginManager().getPlugin("PlotSquared").getDescription()
                    .getVersion();
                if (PSVersion.startsWith("4."))
                {
                    if (protectionAlreadyLoaded(PlotSquaredNewProtectionCompat.class))
                        return;
                    plugin.getMyLogger().info("PlotSquared v4 detected!");
                    plotSquaredCompat = new PlotSquaredNewProtectionCompat(plugin);
                }
                else
                {
                    if (protectionAlreadyLoaded(PlotSquaredOldProtectionCompat.class))
                        return;
                    plugin.getMyLogger().info("PlotSquared v3 detected!");
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
            catch (NullPointerException e)
            {
                plugin.getMyLogger().logMessageToConsole("Could not find PlotSquared! Hook not enabled!");
            }
            catch (Exception e)
            {
                if (!silent)
                {
                    plugin.getMyLogger().logMessageToConsole(
                          "Failed to initialize PlotSquared compatibility hook!");
                    plugin.getMyLogger().logMessageToConsole(
                          "Now resuming normal startup with PlotSquared Compatibility Hook disabled!");
                }
            }
    }
}
