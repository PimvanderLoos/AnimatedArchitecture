package nl.pim16aap2.bigdoors.spigot.listeners;

import lombok.extern.flogger.Flogger;
import nl.pim16aap2.bigdoors.api.restartable.RestartableHolder;
import nl.pim16aap2.bigdoors.spigot.config.ConfigLoaderSpigot;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Represents a listener that keeps track of {@link Player}s logging in to send them the resource pack.
 *
 * @author Pim
 */
@Singleton
@Flogger
public class LoginResourcePackListener extends AbstractListener
{
    private final ConfigLoaderSpigot config;
    private String resourcePackURL;

    @Inject
    public LoginResourcePackListener(RestartableHolder holder, ConfigLoaderSpigot config, JavaPlugin plugin)
    {
        super(holder, plugin, () -> shouldBeEnabled(config));
        this.config = config;
        resourcePackURL = config.resourcePack();
    }

    /**
     * Checks if this listener should be enabled as based on the config settings.
     *
     * @param config
     *     The config to use to determine the status of this listener.
     * @return True if this listener should be enabled.
     */
    private static boolean shouldBeEnabled(ConfigLoaderSpigot config)
    {
        return !config.resourcePack().isBlank();
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
        if (resourcePackURL.isBlank())
            return;
        try
        {
            event.getPlayer().setResourcePack(resourcePackURL);
        }
        catch (Exception e)
        {
            log.atSevere().withCause(e).log();
        }
    }

    @Override
    public void initialize()
    {
        super.initialize();
        resourcePackURL = config.resourcePack();
    }
}
