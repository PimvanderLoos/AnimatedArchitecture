package nl.pim16aap2.bigdoors.spigot.listeners;

import lombok.NonNull;
import nl.pim16aap2.bigdoors.spigot.BigDoorsSpigot;
import nl.pim16aap2.bigdoors.spigot.gui.GUI;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;

import java.util.Optional;

/**
 * Represents a listener that keeps track of {@link Player}s interacting with a GUI.
 *
 * @author Pim
 */
public class GUIListener implements Listener
{
    private final @NonNull BigDoorsSpigot plugin;

    public GUIListener(final @NonNull BigDoorsSpigot plugin)
    {
        this.plugin = plugin;
    }

    /**
     * Listens to players closing their inventories.
     *
     * @param event The {@link InventoryCloseEvent}.
     */
    /*
     * When changing GUI pages, the InventoryCloseEvent is fired.
     * So, before killing the GUI, make sure it wasn't just a
     * page switch / update.
     */
    @EventHandler
    public void onInventoryClose(final @NonNull InventoryCloseEvent event)
    {
        try
        {
            // Looks dumb, but InventoryCloseEvent#getPlayer() actually returns a HumanEntity, not a Player.
            // A HumanEntity could also be an NPC.
            if (!(event.getPlayer() instanceof Player))
                return;
            Player player = (Player) event.getPlayer();

            Optional<GUI> gui = plugin.getGUIUser(player);
            gui.ifPresent(
                GUI ->
                {
                    if (GUI.isRefreshing())
                        return;

                    if (GUI.isOpen())
                        GUI.close();
                }
            );
        }
        catch (Exception e)
        {
            plugin.getPLogger().logThrowable(e);
        }
    }

    /**
     * Listen to players interacting with a GUI.
     *
     * @param event The {@link InventoryClickEvent}.
     */
    @EventHandler
    public void onInventoryClick(final @NonNull InventoryClickEvent event)
    {
        try
        {
            if (!(event.getWhoClicked() instanceof Player))
                return;

            Player player = (Player) event.getWhoClicked();
            Optional<GUI> gui = plugin.getGUIUser(player);

            // TODO: Check if the GUI is custom!
            gui.ifPresent(
                GUI ->
                {
                    event.setCancelled(true);
                    GUI.handleInput(event.getRawSlot());
                });
        }
        catch (Exception e)
        {
            plugin.getPLogger().logThrowable(e);
        }
    }
}
