package nl.pim16aap2.bigdoors.spigot.listeners;

import nl.pim16aap2.bigdoors.spigot.BigDoorsPlugin;
import nl.pim16aap2.bigdoors.spigot.managers.UpdateManager;
import nl.pim16aap2.bigdoors.util.Constants;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Represents a listener that keeps track of {@link Player}s logging in to send them any messages if needed.
 *
 * @author Pim
 */
@Singleton
public final class LoginMessageListener extends AbstractListener
{
    private final BigDoorsPlugin plugin;
    private final @Nullable UpdateManager updateManager;

    @Inject
    public LoginMessageListener(BigDoorsPlugin plugin, @Nullable UpdateManager updateManager)
    {
        super(plugin);
        this.plugin = plugin;
        this.updateManager = updateManager;
        register();
    }

    /**
     * Listens to {@link Player}s logging in and sends them the login message.
     *
     * @param event
     *     The {@link PlayerJoinEvent}.
     */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event)
    {
        final Player player = event.getPlayer();
        if (player.hasPermission("bigdoors.admin.info"))
            // Slight delay so the player actually receives the message;
            new BukkitRunnable()
            {
                @Override
                public void run()
                {
                    final @Nullable String loginString = getLoginMessage();
                    if (loginString != null)
                        player.sendMessage(ChatColor.AQUA + loginString);
                }
            }.runTaskLater(plugin, 120);
    }

    private @Nullable String getLoginMessage()
    {
        String ret = "";
        ret += formatMessage("Error", plugin.getInitErrorMessage());
        ret += formatMessage("Warning", getDevBuildWarning());
        ret += formatMessage(null, getUpdateMessage());
        return ret.isBlank() ? null : ret;
    }

    private @Nullable String getDevBuildWarning()
    {
        if (Constants.DEV_BUILD)
            return "You are running a dev-build!";
        return null;
    }

    private @Nullable String getUpdateMessage()
    {
        if (updateManager == null)
            return null;

        if (updateManager.hasUpdateBeenDownloaded())
            return "A new update (" + updateManager.getNewestVersion() + ") has been downloaded! " +
                "Restart your server to apply the update!";
        if (updateManager.updateAvailable())
            return "A new update is available: " + updateManager.getNewestVersion() + "!";
        return null;
    }

    private String formatMessage(@Nullable String prefix, @Nullable String msg)
    {
        if (msg == null || msg.isBlank())
            return "";

        String ret = "[BigDoors] ";
        if (prefix != null && !prefix.isBlank())
            ret += prefix + ": ";
        return ret + msg + '\n';
    }
}
