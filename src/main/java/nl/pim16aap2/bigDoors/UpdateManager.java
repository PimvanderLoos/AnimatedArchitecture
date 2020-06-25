/**
 *
 */
package nl.pim16aap2.bigDoors;

import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import nl.pim16aap2.bigDoors.UpdateChecker.UpdateReason;

/**
 *
 * @author Pim
 */
public final class UpdateManager
{
    private final BigDoors plugin;
    private boolean checkForUpdates = false;
    private boolean downloadUpdates = false;
    private boolean updateDownloaded = false;

    private UpdateChecker updater;
    private BukkitTask updateRunner = null;

    public UpdateManager(final BigDoors plugin, final int pluginID)
    {
        this.plugin = plugin;
        updater = UpdateChecker.init(plugin, pluginID);
    }

    public void setEnabled(final boolean newCheckForUpdates, final boolean newDownloadUpdates)
    {
        checkForUpdates = newCheckForUpdates;
        downloadUpdates = newDownloadUpdates;
        initUpdater();
    }

    public boolean hasUpdateBeenDownloaded()
    {
        return updateDownloaded;
    }

    public String getNewestVersion()
    {
        if (!checkForUpdates || updater.getLastResult() == null)
            return null;
        return updater.getLastResult().getNewestVersion();
    }

    public boolean updateAvailable()
    {
        // Updates disabled, so no new updates available by definition.
        if (!checkForUpdates || updater.getLastResult() == null)
            return false;

        // There's a newer version available.
        if (updater.getLastResult().requiresUpdate())
            return true;

        // The plugin is "up-to-date", but this is a dev-build, so it must be newer.
        if (BigDoors.DEVBUILD && updater.getLastResult().getReason().equals(UpdateReason.UP_TO_DATE))
            return true;

        return false;
    }

    public void checkForUpdates()
    {
        updater.requestUpdateCheck().whenComplete((result, throwable) ->
        {
            boolean updateAvailable = updateAvailable();
            if (updateAvailable)
                plugin.getMyLogger().info("A new update is available: " + plugin.getUpdateManager().getNewestVersion());

            if (downloadUpdates && updateAvailable && result.getAge() >= plugin.getConfigLoader().downloadDelay())
            {
                updateDownloaded = updater.downloadUpdate();
                if (updateDownloaded)
                    plugin.getMyLogger()
                        .info("Update downloaded! Restart to apply it! " + "New version is "
                            + updater.getLastResult().getNewestVersion() + ", Currently running "
                            + plugin.getDescription().getVersion() + (BigDoors.DEVBUILD ? " (but a DEV-build)" : ""));
                else
                    plugin.getMyLogger().info("Failed to download latest version! You can download it manually at: "
                        + updater.getDownloadUrl());
            }
        });
    }

    private void initUpdater()
    {
        if (checkForUpdates)
        {
            // Run the UpdateChecker regularly.
            if (updateRunner == null)
            {
                updateRunner = new BukkitRunnable()
                {
                    @Override
                    public void run()
                    {
                        checkForUpdates();
                    }
                }.runTaskTimer(plugin, 0L, 288000L); // Run immediately, then every 4 hours.
            }
        }
        else
        {
            if (updateRunner != null)
            {
                updateRunner.cancel();
                updateRunner = null;
            }
        }
    }
}
