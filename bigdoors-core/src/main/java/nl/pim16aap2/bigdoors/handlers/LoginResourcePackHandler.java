package nl.pim16aap2.bigdoors.handlers;

import nl.pim16aap2.bigdoors.BigDoors;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class LoginResourcePackHandler implements Listener
{
    private final BigDoors plugin;
    private final String url;

    public LoginResourcePackHandler(BigDoors plugin, String url)
    {
        this.plugin = plugin;
        this.url = url;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event)
    {
        try
        {
            event.getPlayer().setResourcePack(url);
        }
        catch (Exception e)
        {
            plugin.getPLogger().logException(e);
        }
    }
}
