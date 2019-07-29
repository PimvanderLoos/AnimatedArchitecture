package nl.pim16aap2.bigdoors.listener;

import nl.pim16aap2.bigdoors.BigDoors;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

/**
 * Represents a listener that keeps track of {@link Player}s logging in to send them the resource pack.
 *
 * @author Pim
 */
public class LoginResourcePackHandler implements Listener
{
    private final BigDoors plugin;
    private final String url;

    public LoginResourcePackHandler(BigDoors plugin, String url)
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
