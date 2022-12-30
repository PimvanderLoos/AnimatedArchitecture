package nl.pim16aap2.bigDoors.handlers;

import nl.pim16aap2.bigDoors.BigDoors;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class LoginResourcePackHandler implements Listener
{
    BigDoors plugin;
    String url;

    public LoginResourcePackHandler(BigDoors plugin, String url)
    {
        this.plugin = plugin;
        this.url = url;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event)
    {
        if (url == null)
        {
            plugin.getMyLogger().warn("No resource pack set! Please contact pim16aap2!");
            return;
        }
        event.getPlayer().setResourcePack(url);
    }
}
