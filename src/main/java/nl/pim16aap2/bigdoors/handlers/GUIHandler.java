package nl.pim16aap2.bigdoors.handlers;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.gui.GUI;
import nl.pim16aap2.bigdoors.util.Messages;
import nl.pim16aap2.bigdoors.util.PageType;

public class GUIHandler implements Listener
{
    private final Messages messages;
    private final BigDoors plugin;

    public GUIHandler(final BigDoors plugin)
    {
        this.plugin = plugin;
        messages = plugin.getMessages();
    }

    // When changing GUI pages, the InventoryCloseEvent is fired.
    // So, before killing the GUI, make sure it wasn't just a
    // page switch / update.
    @EventHandler
    public void onInventoryClose(final InventoryCloseEvent event)
    {
        if (!(event.getPlayer() instanceof Player))
            return;

        Player player = (Player) event.getPlayer();

        GUI gui = plugin.getGUIUser(player);
        if (gui == null)
            return;

        if (gui.isRefreshing())
            return;

        if (gui.isOpen())
            gui.close();
    }

    // Check for clicks on items
    @EventHandler
    public void onInventoryClick(final InventoryClickEvent event)
    {
        if (!(event.getWhoClicked() instanceof Player))
            return;

        Player player = (Player) event.getWhoClicked();
        GUI gui = plugin.getGUIUser(player);

        if (gui == null)
            return;

        if (PageType.valueOfName(messages.getStringReverse(player.getOpenInventory().getTitle())) == PageType.NOTBIGDOORS)
            return;

        event.setCancelled(true);
        gui.handleInput(event.getRawSlot());
    }
}
