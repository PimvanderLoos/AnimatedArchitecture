package nl.pim16aap2.bigdoors.spigot.managers;

import nl.pim16aap2.bigdoors.logging.IPLogger;
import nl.pim16aap2.bigdoors.spigot.BigDoorsSpigot;
import nl.pim16aap2.bigdoors.spigot.util.UpdateChecker;
import nl.pim16aap2.bigdoors.util.Constants;
import nl.pim16aap2.bigdoors.util.Util;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.Nullable;

import javax.inject.Singleton;

/**
 * Class that manages all update-related stuff.
 *
 * @author Pim
 */
@Singleton
public final class UpdateManager
{
    private final BigDoorsSpigot plugin;
    private final IPLogger logger;
    private boolean checkForUpdates = false;
    private boolean downloadUpdates = false;
    private boolean updateDownloaded = false;

    private final UpdateChecker updater;
    private @Nullable BukkitTask updateRunner = null;

    public UpdateManager(BigDoorsSpigot plugin, int pluginID)
    {
        this.plugin = plugin;
        logger = plugin.getPLogger();
        updater = new UpdateChecker(plugin, pluginID, plugin.getPLogger());
    }

    /**
     * Enables or disables update checking and/or downloading.
     *
     * @param newCheckForUpdates
     *     True if update checking should be enabled.
     * @param newDownloadUpdates
     *     True if update downloading should be enabled.
     */
    public void setEnabled(boolean newCheckForUpdates, boolean newDownloadUpdates)
    {
        checkForUpdates = newCheckForUpdates;
        downloadUpdates = newDownloadUpdates;
        initUpdater();
    }

    /**
     * Checks if an update has been downloaded. If so, a restart will apply the update.
     *
     * @return True if an update has been downloaded.
     */
    public boolean hasUpdateBeenDownloaded()
    {
        return updateDownloaded;
    }

    /**
     * Gets the version of the latest publicly released build.
     *
     * @return The version of the latest publicly released build.
     */
    public String getNewestVersion()
    {
        if (!checkForUpdates || updater.getLastResult() == null)
            return "ERROR";
        return updater.getLastResult().getNewestVersion();
    }

    /**
     * Checks if this plugin can be updates. This either means a newer version is available or the current dev-build has
     * been released in full.
     *
     * @return True if the plugin should be updated.
     */
    public boolean updateAvailable()
    {
        // Updates disabled, so no new updates available by definition.
        if (!checkForUpdates || updater.getLastResult() == null)
            return false;

        // There's a newer version available.
        if (updater.getLastResult().requiresUpdate())
            return true;

        // The plugin is "up-to-date", but this is a dev-build, so it must be newer.
        return Constants.DEV_BUILD && updater.getLastResult().getReason().equals(UpdateChecker.UpdateReason.UP_TO_DATE);
    }

    /**
     * Checks if any updates are available.
     */
    private void checkForUpdates()
    {
        updater.requestUpdateCheck().whenComplete(
            (result, throwable) ->
            {
                final boolean updateAvailable = updateAvailable();
                if (updateAvailable)
                    logger.info("A new update is available: " + getNewestVersion());

                if (downloadUpdates && updateAvailable && result.getAge() >= plugin.getConfigLoader().downloadDelay())
                {
                    updateDownloaded = updater.downloadUpdate();
                    if (updateDownloaded && updater.getLastResult() != null)
                        logger.info("Update downloaded! Restart to apply it! " +
                                        "New version is " + updater.getLastResult().getNewestVersion() +
                                        ", Currently running " + plugin.getDescription().getVersion() +
                                        (Constants.DEV_BUILD ? " (but a DEV-build)" : ""));
                    else
                        logger.info("Failed to download latest version! You can download it manually at: " +
                                        updater.getDownloadUrl());
                }
            }).exceptionally(Util::exceptionally);
    }

    /**
     * (Re)Initializes the updater.
     */
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
                }.runTaskTimer(plugin, 0L, 288_000L); // Run immediately, then every 4 hours.
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
