package nl.pim16aap2.animatedarchitecture.spigot.core.listeners;

import lombok.extern.flogger.Flogger;
import nl.pim16aap2.animatedarchitecture.core.api.restartable.RestartableHolder;
import nl.pim16aap2.animatedarchitecture.core.managers.DatabaseManager;
import nl.pim16aap2.animatedarchitecture.core.util.Util;
import nl.pim16aap2.animatedarchitecture.spigot.util.SpigotAdapter;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Represents a generic listener for players logging.
 * <p>
 * Unlike the other login listeners, this one should be unconditionally enabled on normal startup.
 * <p>
 * Some of the tasks this listener performs are:
 * <ul>
 *     <li>Updating the player's information in the database.</li>
 * </ul>
 */
@Flogger
@Singleton final class PlayerLoginListener extends AbstractListener
{
    private final DatabaseManager databaseManager;

    @Inject PlayerLoginListener(
        JavaPlugin javaPlugin,
        DatabaseManager databaseManager,
        RestartableHolder restartableHolder)
    {
        super(restartableHolder, javaPlugin);
        this.databaseManager = databaseManager;
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
}
