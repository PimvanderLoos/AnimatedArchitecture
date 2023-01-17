package nl.pim16aap2.bigdoors.spigot.managers;

import lombok.extern.flogger.Flogger;
import nl.pim16aap2.bigdoors.api.IConfigLoader;
import nl.pim16aap2.bigdoors.api.restartable.Restartable;
import nl.pim16aap2.bigdoors.api.restartable.RestartableHolder;
import nl.pim16aap2.bigdoors.spigot.BigDoorsPlugin;
import nl.pim16aap2.bigdoors.spigot.util.UpdateChecker;
import nl.pim16aap2.bigdoors.util.Constants;
import nl.pim16aap2.bigdoors.util.Util;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;
import javax.inject.Singleton;


/**
 * Class that manages all update-related stuff.
 *
 * @author Pim
 */
@Singleton
@Flogger
public final class UpdateManager extends Restartable
{
    private final JavaPlugin plugin;
    private final IConfigLoader config;
    private boolean checkForUpdates = false;
    private boolean downloadUpdates = false;
    private boolean updateDownloaded = false;

    private final UpdateChecker updater;
    private @Nullable BukkitTask updateRunner = null;

    @Inject
    public UpdateManager(
        RestartableHolder restartableHolder, BigDoorsPlugin plugin,
        IConfigLoader config, UpdateChecker updater)
    {
        super(restartableHolder);
        this.plugin = plugin;
        this.config = config;
        this.updater = updater;
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
                    log.atInfo().log("A new update is available: %s", getNewestVersion());

                if (downloadUpdates && updateAvailable && result.getAge() >= config.downloadDelay())
                {
                    updateDownloaded = updater.downloadUpdate();
                    if (updateDownloaded && updater.getLastResult() != null)
                        log.atInfo()
                           .log("Update downloaded! Restart to apply it! New version is %s, Currently running %s%s",
                                updater.getLastResult().getNewestVersion(), plugin.getDescription().getVersion(),
                                (Constants.DEV_BUILD ? " (but a DEV-build)" : "")
                           );
                    else
                        log.atInfo().log("Failed to download latest version! You can download it manually at: %s",
                                         updater.getDownloadUrl());
                }
            }).exceptionally(Util::exceptionally);
    }

    @Override
    public void initialize()
    {
        checkForUpdates = config.checkForUpdates();
        downloadUpdates = config.autoDLUpdate();

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
            shutDown();
    }

    @Override
    public void shutDown()
    {
        if (updateRunner != null)
        {
            updateRunner.cancel();
            updateRunner = null;
        }
    }
}
