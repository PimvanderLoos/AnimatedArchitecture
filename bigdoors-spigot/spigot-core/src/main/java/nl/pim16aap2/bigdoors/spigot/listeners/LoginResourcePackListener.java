package nl.pim16aap2.bigdoors.spigot.listeners;

import nl.pim16aap2.bigdoors.api.restartable.Restartable;
import nl.pim16aap2.bigdoors.spigot.BigDoorsSpigot;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a listener that keeps track of {@link Player}s logging in to send them the resource pack.
 *
 * @author Pim
 */
public class LoginResourcePackListener extends Restartable implements Listener
{
    private static @Nullable LoginResourcePackListener INSTANCE;
    private final BigDoorsSpigot plugin;
    private final String url;
    private boolean isRegistered = false;

    private LoginResourcePackListener(BigDoorsSpigot plugin, String url)
    {
        super(plugin);
        this.plugin = plugin;
        this.url = url;
    }

    /**
     * Initializes the {@link LoginResourcePackListener}. If it has already been initialized, it'll return that instance
     * instead.
     *
     * @param plugin
     *     The {@link BigDoorsSpigot} plugin.
     * @param url
     *     The URL of the resource pack.
     * @return The instance of this {@link LoginResourcePackListener}.
     */
    public static LoginResourcePackListener init(BigDoorsSpigot plugin, String url)
    {
        return (INSTANCE == null) ? INSTANCE = new LoginResourcePackListener(plugin, url) : INSTANCE;
    }

    @Override
    public void restart()
    {
        if (plugin.getConfigLoader().enableRedstone())
            register();
        else
            unregister();
    }

    /**
     * Registers this listener if it isn't already registered.
     */
    private void register()
    {
        if (isRegistered)
            return;
        Bukkit.getPluginManager().registerEvents(this, plugin);
        isRegistered = true;
    }

    /**
     * Unregisters this listener if it isn't already unregistered.
     */
    private void unregister()
    {
        if (!isRegistered)
            return;
        HandlerList.unregisterAll(this);
        isRegistered = false;
    }

    @Override
    public void shutdown()
    {
        unregister();
    }

    /**
     * Listens to {@link Player}s logging in and sends them the resource pack.
     *
     * @param event
     *     The {@link PlayerJoinEvent}.
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
            plugin.getPLogger().logThrowable(e);
        }
    }
}
