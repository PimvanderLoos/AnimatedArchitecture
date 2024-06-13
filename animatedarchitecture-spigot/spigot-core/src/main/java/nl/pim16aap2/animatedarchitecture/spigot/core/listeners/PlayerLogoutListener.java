package nl.pim16aap2.animatedarchitecture.spigot.core.listeners;

import lombok.extern.flogger.Flogger;
import nl.pim16aap2.animatedarchitecture.core.api.IPlayer;
import nl.pim16aap2.animatedarchitecture.core.api.restartable.RestartableHolder;
import nl.pim16aap2.animatedarchitecture.core.managers.DatabaseManager;
import nl.pim16aap2.animatedarchitecture.core.managers.DelayedCommandInputManager;
import nl.pim16aap2.animatedarchitecture.core.managers.ToolUserManager;
import nl.pim16aap2.animatedarchitecture.spigot.util.SpigotAdapter;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Represents a generic listener for players logging out.
 * <p>
 * This listener should be unconditionally enabled on normal startup.
 * <p>
 * Some of the tasks this listener performs are:
 * <ul>
 *     <li>Cancelling all command inputs for the player.</li>
 *     <li>Aborting the tool user for the player.</li>
 *     <li>Updating the player in the database.</li>
 * </ul>
 */
@Flogger
@Singleton final class PlayerLogoutListener extends AbstractListener
{
    private final DatabaseManager databaseManager;
    private final ToolUserManager toolUserManager;
    private final DelayedCommandInputManager delayedCommandInputManager;

    @Inject PlayerLogoutListener(
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
     * Listens for the {@link PlayerQuitEvent} to make sure all processes they are active in are cancelled properly.
     *
     * @param event
     *     The {@link PlayerQuitEvent}.
     */
    @EventHandler
    public void onLogout(PlayerQuitEvent event)
    {
        final var player = SpigotAdapter.wrapPlayer(event.getPlayer());
        cancelCommandInputs(player);
        abortToolUser(player);
        updatePlayer(player);
    }

    private void cancelCommandInputs(IPlayer player)
    {
        try
        {
            delayedCommandInputManager.cancelAll(player);
        }
        catch (Exception e)
        {
            log.atSevere().withCause(e).log(
                "Raised an exception while cancelling all command inputs for player %s", player);
        }
    }

    private void abortToolUser(IPlayer player)
    {
        try
        {
            toolUserManager.abortToolUser(player.getUUID());
        }
        catch (Exception e)
        {
            log.atSevere().withCause(e).log(
                "Raised an exception while aborting tool user for player %s", player);
        }
    }

    private void updatePlayer(IPlayer player)
    {
        try
        {
            databaseManager
                .updatePlayer(player)
                .exceptionally(exception ->
                {
                    log.atSevere().withCause(exception).log(
                        "Raised an exception while updating player %s", player);
                    return false;
                });
        }
        catch (Exception e)
        {
            log.atSevere().withCause(e).log(
                "Raised an exception while updating player %s", player);
        }
    }
}
