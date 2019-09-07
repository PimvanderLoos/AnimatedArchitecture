package nl.pim16aap2.bigdoors.listeners;

import nl.pim16aap2.bigdoors.BigDoorsSpigot;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

/**
 * Represents a listener that keeps track of {@link Player}s logging in to send them the resource pack.
 *
 * @author Pim
 */
public class LoginResourcePackListener implements Listener
{
    private final BigDoorsSpigot plugin;
    private final String url;

    public LoginResourcePackListener(BigDoorsSpigot plugin, String url)
    {
        this.plugin = plugin;
        this.url = url;
    }

    /**
     * Listens to {@link Player}s logging in and sends them the resource pack.
     *
     * @param event The {@link PlayerJoinEvent}.
     */
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
