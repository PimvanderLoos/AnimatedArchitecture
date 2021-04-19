package nl.pim16aap2.bigdoors.spigot.listeners;

import lombok.NonNull;
import nl.pim16aap2.bigdoors.spigot.BigDoorsSpigot;
import nl.pim16aap2.bigdoors.util.Constants;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Represents a listener that keeps track of {@link Player}s logging in to send them any messages if needed.
 *
 * @author Pim
 */
public final class LoginMessageListener implements Listener
{
    private final @NonNull BigDoorsSpigot plugin;

    public LoginMessageListener(final @NonNull BigDoorsSpigot plugin)
    {
        this.plugin = plugin;
    }

    /**
     * Listens to {@link Player}s logging in and sends them the login message.
     *
     * @param event The {@link PlayerJoinEvent}.
     */
    @EventHandler
    public void onPlayerJoin(final PlayerJoinEvent event)
    {
        try
        {
            final Player player = event.getPlayer();
            // Normally, only send to those with permission, so they can disable it.
            // But when it's a devbuild, also send it to everyone who's OP, to make it
            // a bit harder to get around the message.
            if (player.hasPermission("bigdoors.admin") || (player.isOp() && Constants.DEV_BUILD))
                // Slight delay so the player actually receives the message;
                new BukkitRunnable()
                {
                    @Override
                    public void run()
                    {
                        final String loginString = plugin.getLoginMessage();
                        if (!loginString.isEmpty())
                            player.sendMessage(ChatColor.AQUA + plugin.getLoginMessage());
                    }
                }.runTaskLater(plugin, 120);
        }
        catch (Exception e)
        {
            plugin.getPLogger().logThrowable(e);
        }
    }
}
