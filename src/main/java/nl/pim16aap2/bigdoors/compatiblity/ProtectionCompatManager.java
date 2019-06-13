package nl.pim16aap2.bigdoors.compatiblity;

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

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.MyException;
import nl.pim16aap2.bigdoors.util.Restartable;

public class ProtectionCompatManager extends Restartable implements Listener
{
    private static final String BYPASSPERMISSION = "bigdoors.admin.bypasscompat";

    private final ArrayList<ProtectionCompat> protectionCompats;
    private FakePlayerCreator fakePlayerCreator;

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

    public ProtectionCompatManager(final BigDoors plugin)
    {
        super(plugin);
        fakePlayerCreator = new FakePlayerCreator(plugin);
        protectionCompats = new ArrayList<>();
        loadLoadedPlugins(true);
    }

    @Override
    public void restart()
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

    private boolean canByPass(Player player)
    {
        if (player.isOp())
            return true;

         // offline players don't have permissions, so use Vault if that's the case.
        if (player.hasMetadata(FakePlayerCreator.FAKEPLAYERMETADATA))
            return player.hasPermission(BYPASSPERMISSION);
        return plugin.getVaultManager().hasPermission(player, BYPASSPERMISSION);
    }

    private Player getPlayer(UUID playerUUID, World world)
    {
        Player player = Bukkit.getPlayer(playerUUID);
        if (player == null)
            player = fakePlayerCreator.getFakePlayer(Bukkit.getOfflinePlayer(playerUUID), world);
        return player;
    }

    public String canBreakBlock(UUID playerUUID, Location loc)
    {
        Player fakePlayer = getPlayer(playerUUID, loc.getWorld());
        if (canByPass(fakePlayer))
            return null;

        for (ProtectionCompat compat : protectionCompats)
            try
            {
                if (!compat.canBreakBlock(fakePlayer, loc))
                    return compat.getName();
            }
            catch(Exception e)
            {
                plugin.getMyLogger().handleMyStackTrace(new MyException(e, "Failed to use \"" + compat.getPlugin().getName() + "\"! Please send this error to pim16aap2:"));
            }
        return null;
    }

    public String canBreakBlocksBetweenLocs(UUID playerUUID, Location loc1, Location loc2)
    {
        Player fakePlayer = getPlayer(playerUUID, loc1.getWorld());
        if (canByPass(fakePlayer))
            return null;

        for (ProtectionCompat compat : protectionCompats)
            try
            {
                if (!compat.canBreakBlocksBetweenLocs(fakePlayer, loc1, loc2))
                    return compat.getName();
            }
            catch(Exception e)
            {
                plugin.getMyLogger().handleMyStackTrace(new MyException(e, "Failed to use \"" + compat.getPlugin().getName() + "\"! Please send this error to pim16aap2:"));
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
            plugin.getMyLogger().info("Failed to hook into \"" + hook.getPlugin().getName() + "\"!");
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
                    plugin.getMyLogger().warn("Version " +
                        WGVersion + " is not supported! If you believe this is in error, please contact pim16aap2.");
                    return;
                }
                addProtectionCompat(protectionCompat);
            }
            catch (NoClassDefFoundError e)
            {
                if (!silent)
                {
                    plugin.getMyLogger().severe("NoClassDefFoundError: "
                        + "Failed to initialize WorldGuard compatibility hook!");
                    plugin.getMyLogger().info(
                          "Now resuming normal startup with Worldguard Compatibility Hook disabled!");
                }
            }
            catch (NullPointerException e)
            {
                plugin.getMyLogger().warn("Could not find WorldGuard! Hook not enabled!");
            }
            catch (Exception e)
            {
                if (!silent)
                {
                    plugin.getMyLogger().warn(
                          "Failed to initialize WorldGuard compatibility hook!");
                    plugin.getMyLogger().info(
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
                    plugin.getMyLogger().severe("NoClassDefFoundError: "
                        + "Failed to initialize PlotSquared compatibility hook! Perhaps this version isn't supported? Check error.log for more info!");
                    plugin.getMyLogger().info(
                          "Now resuming normal startup with PlotSquared Compatibility Hook disabled!");
                }
            }
            catch (NullPointerException e)
            {
                plugin.getMyLogger().warn("Could not find PlotSquared! Hook not enabled!");
            }
            catch (Exception e)
            {
                if (!silent)
                {
                    plugin.getMyLogger().warn(
                          "Failed to initialize PlotSquared compatibility hook!");
                    plugin.getMyLogger().info(
                          "Now resuming normal startup with PlotSquared Compatibility Hook disabled!");
                }
            }
    }
}
