/**
 *
 */
package nl.pim16aap2.bigDoors;

import nl.pim16aap2.bigDoors.UpdateChecker.UpdateReason;
import nl.pim16aap2.bigDoors.util.Util;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.io.IOException;

/**
 *
 * @author Pim
 */
public final class UpdateManager
{
    private final BigDoors plugin;
    private boolean downloadUpdates = false;
    private boolean announceUpdateCheck = true;
    private boolean updateDownloaded = false;

    private UpdateChecker updater;
    private BukkitTask updateRunner = null;

    public UpdateManager(final BigDoors plugin)
    {
        this.plugin = plugin;
        updater = UpdateChecker.init(plugin);
    }

    public void setEnabled(boolean newDownloadUpdates, boolean announceUpdateCheck)
    {
        downloadUpdates = newDownloadUpdates;
        this.announceUpdateCheck = announceUpdateCheck;
        initUpdater();
    }

    public boolean hasUpdateBeenDownloaded()
    {
        return updateDownloaded;
    }

    public String getNewestVersion()
    {
        if (updater.getLastResult() == null)
            return null;
        return updater.getLastResult().getNewestVersion();
    }

    public boolean updateAvailable()
    {
        // Updates disabled, so no new updates available by definition.
        if (updater.getLastResult() == null)
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
        if (announceUpdateCheck)
            plugin.getMyLogger().info("Checking for updates...");
        updater.requestUpdateCheck().whenCompleteAsync((result, throwable) ->
        {
            boolean updateAvailable = updateAvailable();
            if (!updateAvailable)
            {
                if (announceUpdateCheck)
                    plugin.getMyLogger().info("No new updates available.");
                return;
            }

            plugin.getMyLogger().info("A new update is available: " + plugin.getUpdateManager().getNewestVersion());

            if (downloadUpdates && result.getAge() >= plugin.getConfigLoader().downloadDelay())
            {
                try
                {
                    updateDownloaded = updater.downloadUpdate(result);
                }
                catch (IOException e)
                {
                    updateDownloaded = false;
                    plugin.getMyLogger().logMessageToLogFile(Util.throwableToString(e));
                }
                if (updateDownloaded)
                    plugin.getMyLogger()
                        .info("Update downloaded! Restart to apply it! " + "New version is "
                            + updater.getLastResult().getNewestVersion() + ", Currently running "
                            + plugin.getDescription().getVersion()
                            + ((BigDoors.DEVBUILD && result.getReason() == UpdateReason.UP_TO_DATE) ?
                                " (but a DEV-build)" : ""));
                else
                    plugin.getMyLogger().info("Failed to download latest version! You can download it manually at: "
                        + result.getDownloadUrl());
            }
        });
    }

    private void initUpdater()
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
            }.runTaskTimer(plugin, 0L, 864000L); // Run immediately, then every 12 hours.
        }
    }
}
