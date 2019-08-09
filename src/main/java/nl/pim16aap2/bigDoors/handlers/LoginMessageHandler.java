package nl.pim16aap2.bigDoors.handlers;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scheduler.BukkitRunnable;

import nl.pim16aap2.bigDoors.BigDoors;

public class LoginMessageHandler implements Listener
{
    final BigDoors plugin;

    public LoginMessageHandler(final BigDoors plugin)
    {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event)
    {
        Player player = event.getPlayer();
        // Normally, only send to those with permission, so they can disable it.
        // But when it's a devbuild, also send it to everyone who's OP, to make it
        // a bit harder to get around the message.
        if (player.hasPermission("bigdoors.admin") || player.isOp() && BigDoors.DEVBUILD)
            // Slight delay so the player actually receives the message;
            new BukkitRunnable()
            {
                @Override
                public void run()
                {
                    String loginString = plugin.getLoginMessage();
                    if (loginString != null)
                        player.sendMessage(ChatColor.AQUA + plugin.getLoginMessage());
                }
            }.runTaskLater(plugin, 60);
    }
}
