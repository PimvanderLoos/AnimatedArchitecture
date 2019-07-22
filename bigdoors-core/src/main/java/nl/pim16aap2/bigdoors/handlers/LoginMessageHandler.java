package nl.pim16aap2.bigdoors.handlers;

import nl.pim16aap2.bigdoors.BigDoors;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scheduler.BukkitRunnable;

public class LoginMessageHandler implements Listener
{
    BigDoors plugin;

    public LoginMessageHandler(BigDoors plugin)
    {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event)
    {
        try
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
                        String loginString = plugin.getLoginString();
                        if (loginString != null && !loginString.isEmpty())
                            player.sendMessage(ChatColor.AQUA + plugin.getLoginString());
                    }
                }.runTaskLater(plugin, 60);
        }
        catch (Exception e)
        {
            plugin.getPLogger().logException(e);
        }
    }
}
