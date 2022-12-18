/**
 *
 */
package nl.pim16aap2.bigDoors;

import nl.pim16aap2.bigDoors.UpdateChecker.UpdateReason;
import nl.pim16aap2.bigDoors.util.Util;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Arrays;

/**
 *
 * @author Pim
 */
public final class UpdateManager
{
    private final BigDoors plugin;
    private volatile boolean downloadUpdates = false;
    private volatile boolean announceUpdateCheck = true;
    private volatile boolean updateDownloaded = false;

    private final UpdateChecker updater;
    private BukkitTask updateRunner = null;

    public UpdateManager(final BigDoors plugin)
    {
        this.plugin = plugin;
        updater = UpdateChecker.init(plugin);
    }

    public void setEnabled(boolean downloadUpdates, boolean announceUpdateCheck)
    {
        this.downloadUpdates = downloadUpdates;
        this.announceUpdateCheck = announceUpdateCheck;
        initUpdater();
    }

    public boolean hasUpdateBeenDownloaded()
    {
        return updateDownloaded;
    }

    public @Nullable String getNewestVersion()
    {
        if (updater.getLastResult() == null)
            return null;
        return updater.getLastResult().getNewestVersion();
    }

    private @Nullable String getNewestVersionChangelog()
    {
        if (updater.getLastResult() == null)
            return null;
        return updater.getLastResult().getChangelog();
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

    private void announceUpdate()
    {
        final @Nullable String newestVersion = getNewestVersion();
        StringBuilder sb = new StringBuilder("A new update is available: ").append(newestVersion).append('!');

        int lineWidth = 80;
        final StringBuilder changelogBuilder = new StringBuilder();
        final @Nullable String changelog = getNewestVersionChangelog();
        if (changelog != null)
        {
            for (final String item : changelog.split("\\r\\n"))
            {
                if (item.length() > lineWidth)
                    lineWidth = item.length();
                changelogBuilder.append(item).append('\n');
            }
            changelogBuilder.append('\n');
            lineWidth = Math.min(lineWidth + 4, 160);
        }

        final String title = " [BigDoors " + newestVersion + "] ";
        final char[] starsArr = new char[(int) Math.ceil((lineWidth - title.length()) / 2.0D)];
        Arrays.fill(starsArr, '*');
        final String stars = new String(starsArr);
        final String header = stars + title + stars;

        sb.append("\n\n")
          .append(header)
          .append('\n')
          .append(changelogBuilder)
          .append("Please update:\n  https://www.spigotmc.org/resources/big-doors.58669/")
          .append('\n');

        final char[] footer = new char[header.length()];
        Arrays.fill(footer, '*');
        sb.append(footer)
          .append('\n');

        plugin.getMyLogger().info(sb.toString());
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

            announceUpdate();

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
