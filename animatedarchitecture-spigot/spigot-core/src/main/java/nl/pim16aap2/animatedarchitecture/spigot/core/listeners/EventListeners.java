package nl.pim16aap2.animatedarchitecture.spigot.core.listeners;

import lombok.extern.flogger.Flogger;
import nl.pim16aap2.animatedarchitecture.core.api.restartable.RestartableHolder;
import nl.pim16aap2.animatedarchitecture.core.managers.DatabaseManager;
import nl.pim16aap2.animatedarchitecture.core.managers.DelayedCommandInputManager;
import nl.pim16aap2.animatedarchitecture.core.managers.ToolUserManager;
import nl.pim16aap2.animatedarchitecture.core.util.Util;
import nl.pim16aap2.animatedarchitecture.spigot.util.SpigotAdapter;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Represents a listener that keeps track of various events.
 */
@Singleton
@Flogger
public class EventListeners extends AbstractListener
{
    private final DatabaseManager databaseManager;
    private final ToolUserManager toolUserManager;
    private final DelayedCommandInputManager delayedCommandInputManager;

    @Inject EventListeners(
        JavaPlugin javaPlugin,
        DatabaseManager databaseManager,
        ToolUserManager toolUserManager,
        DelayedCommandInputManager delayedCommandInputManager,
        RestartableHolder restartableHolder)
    {
        super(restartableHolder, javaPlugin);
        this.databaseManager = databaseManager;
        this.toolUserManager = toolUserManager;
        this.delayedCommandInputManager = delayedCommandInputManager;
    }

    /**
     * Listens for the {@link PlayerJoinEvent} to make sure their latest name is updated in the database.
     *
     * @param event
     *     The {@link PlayerJoinEvent}.
     */
    @EventHandler
    public void onLogin(PlayerJoinEvent event)
    {
        try
        {
            databaseManager
                .updatePlayer(SpigotAdapter.wrapPlayer(event.getPlayer()))
                .exceptionally(Util::exceptionally);
        }
        catch (Exception e)
        {
            log.atSevere().withCause(e).log();
        }
    }

    /**
     * Listens for the {@link PlayerQuitEvent} to make sure all processes they are active in are cancelled properly.
     *
     * @param event
     *     The {@link PlayerQuitEvent}.
     */
    @EventHandler
    public void onLogout(PlayerQuitEvent event)
    {
        try
        {
            final Player player = event.getPlayer();
            delayedCommandInputManager.cancelAll(SpigotAdapter.wrapPlayer(player));
            toolUserManager.abortToolUser(player.getUniqueId());
            databaseManager.updatePlayer(SpigotAdapter.wrapPlayer(player)).exceptionally(Util::exceptionally);
        }
        catch (Exception e)
        {
            log.atSevere().withCause(e).log();
        }
    }
}
