package nl.pim16aap2.bigdoors.spigot.listeners;

import nl.pim16aap2.bigdoors.api.IConfigLoader;
import nl.pim16aap2.bigdoors.api.restartable.IRestartableHolder;
import nl.pim16aap2.bigdoors.api.restartable.Restartable;
import nl.pim16aap2.bigdoors.logging.IPLogger;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

/**
 * Represents a listener that keeps track of {@link Player}s logging in to send them the resource pack.
 *
 * @author Pim
 */
@Singleton
public class LoginResourcePackListener extends Restartable implements Listener
{
    private final String url;
    private final IPLogger logger;
    private final IConfigLoader config;
    private final JavaPlugin plugin;
    private boolean isRegistered = false;

    @Inject
    public LoginResourcePackListener(IRestartableHolder holder, IPLogger logger, IConfigLoader config,
                                     JavaPlugin plugin, @Named("resourcePackURL") String url)
    {
        super(holder);
        this.logger = logger;
        this.config = config;
        this.plugin = plugin;
        this.url = url;
    }

    @Override
    public void restart()
    {
        if (config.enableRedstone())
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
            logger.logThrowable(e);
        }
    }
}
