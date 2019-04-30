package nl.pim16aap2.bigdoors.handlers;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.scheduler.BukkitRunnable;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.GUI.GUI;
import nl.pim16aap2.bigdoors.util.Messages;
import nl.pim16aap2.bigdoors.util.PageType;

public class GUIHandler implements Listener
{
    private final Messages messages;
    private final BigDoors   plugin;

    public GUIHandler(BigDoors plugin)
    {
        this.plugin   = plugin;
        messages = plugin.getMessages();
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event)
    {
        if (!(event.getPlayer() instanceof Player))
            return;

        Player player = (Player) event.getPlayer();

        GUI gui = plugin.getGUIUser(player);
        if (gui != null)
            // Slight delay, so that we get the "next" inventory, not the current one that is being closed.
            new BukkitRunnable()
            {
                @Override
                public void run()
                {
                    // Get the current page type. If no inventory is open (I don't think that's possible), it's notbigdoors by definition,
                    // Otherwise, check which one it is and then close the GUI.
                    if ((player.getOpenInventory() == null ? PageType.NOTBIGDOORS :
                        PageType.valueOfName(messages.getStringReverse(player.getOpenInventory().getTitle()))) == PageType.NOTBIGDOORS)
                        gui.close();
                }
            }.runTaskLater(plugin, 1);
    }

    // Check for clicks on items
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event)
    {
        if (!(event.getWhoClicked() instanceof Player))
            return;

        Player player = (Player) event.getWhoClicked();
        GUI gui = plugin.getGUIUser(player);

        if (gui == null)
            return;

        event.setCancelled(true);
        gui.handleInput(event.getRawSlot());
    }
}
