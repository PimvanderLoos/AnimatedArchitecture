package nl.pim16aap2.bigDoors.handlers;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.scheduler.BukkitRunnable;

import nl.pim16aap2.bigDoors.BigDoors;

public class LoginMessageHandler implements Listener
{
    BigDoors plugin;
    String   message;

    public LoginMessageHandler(BigDoors plugin)
    {
        this.plugin  = plugin;
    }

    @EventHandler
    public void onLogin(PlayerLoginEvent event)
    {
        Player player = event.getPlayer();
        if (player.hasPermission("bigdoors.admin"))
        {
            // Slight delay so the player actually receives the message;
            new BukkitRunnable()
            {
                @Override
                public void run()
                {
                    String loginString = plugin.getLoginString();
                    if (loginString != "")
                        player.sendMessage(ChatColor.AQUA + plugin.getLoginString());
                }
            }.runTaskLater(plugin, 10);
        }
    }
}
