package nl.pim16aap2.bigdoors.handlers;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.toolusers.ToolUser;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.PlayerInventory;

public class EventHandlers implements Listener
{
    private final BigDoors plugin;

    public EventHandlers(final BigDoors plugin)
    {
        this.plugin = plugin;
    }

    // Selection event.
    @EventHandler
    public void onLeftClick(final PlayerInteractEvent event)
    {
        try
        {
            if (event.getAction() == Action.LEFT_CLICK_BLOCK &&
                    plugin.getTF().isTool(event.getPlayer().getInventory().getItemInMainHand()))
            {
                ToolUser tu = plugin.getToolUser(event.getPlayer());
                if (tu == null)
                    return;
                tu.selector(event.getClickedBlock().getLocation());
                event.setCancelled(true);
            }
        }
        catch (Exception e)
        {
            plugin.getPLogger().logException(e);
        }
    }

    @EventHandler
    public void onLogin(final PlayerLoginEvent event)
    {
        try
        {
            plugin.getDatabaseManager().updatePlayer(event.getPlayer());
        }
        catch (Exception e)
        {
            plugin.getPLogger().logException(e);
        }
    }

    @EventHandler
    public void onLogout(final PlayerQuitEvent event)
    {
        try
        {
            ToolUser tu = plugin.getToolUser(event.getPlayer());
            if (tu != null)
                tu.abort();
        }
        catch (Exception e)
        {
            plugin.getPLogger().logException(e);
        }
    }

    // Do not allow the player to drop the door creation tool.
    // If they aren't a tool user, remove the item instead;
    // They shouldn't have it in the first place.
    @EventHandler
    public void onItemDropEvent(PlayerDropItemEvent event)
    {
        try
        {
            if (plugin.getTF().isTool(event.getItemDrop().getItemStack()))
            {
                ToolUser tu = plugin.getToolUser(event.getPlayer());
                if (tu == null)
                    event.getItemDrop().remove();
                else
                    event.setCancelled(true);
            }
        }
        catch (Exception e)
        {
            plugin.getPLogger().logException(e);
        }
    }

    // Do not allow players to transfer selection tools to other inventories
    // such as chests.
    @EventHandler
    public void inventoryClickEvent(InventoryClickEvent event)
    {
        try
        {
            if (!plugin.getTF().isTool(event.getCurrentItem()))
                return;
            if (event.getAction().equals(
                    InventoryAction.MOVE_TO_OTHER_INVENTORY) || !event.getClickedInventory().getType().equals(
                    InventoryType.PLAYER))
            {
                if (event.getWhoClicked() instanceof Player)
                {
                    ToolUser tu = plugin.getToolUser((Player) event.getWhoClicked());
                    if (tu == null)
                        event.getInventory().removeItem(event.getCurrentItem());
                    else
                        event.setCancelled(true);
                }
                event.setCancelled(true);
            }
        }
        catch (Exception e)
        {
            plugin.getPLogger().logException(e);
        }
    }

    // Do not allow players to transfer selection tools to other inventories
    // such as chests.
    @EventHandler
    public void inventoryDragEvent(InventoryDragEvent event)
    {
        try
        {
            event.getNewItems().forEach((K, V) ->
                                        {
                                            if (plugin.getTF().isTool(V))
                                                event.setCancelled(true);
                                        });
        }
        catch (Exception e)
        {
            plugin.getPLogger().logException(e);
        }
    }

    // Do not allow moving selection tools
    // If a player is trying to move it, cancel the move if they need the tool
    // (i.e. they're a toolUser), or delete it if not. If they somehow managed to
    // put it in a hopper or something, just delete the tool.
    @EventHandler
    public void onItemMoved(InventoryMoveItemEvent event)
    {
        try
        {
            if (!plugin.getTF().isTool(event.getItem()))
                return;

            Inventory src = event.getSource();
            if (src instanceof PlayerInventory && ((PlayerInventory) src).getHolder() instanceof Player)
            {
                ToolUser tu = plugin.getToolUser((Player) ((PlayerInventory) src).getHolder());
                if (tu != null)
                {
                    event.setCancelled(true);
                    return;
                }
            }
            src.removeItem(event.getItem());
        }
        catch (Exception e)
        {
            plugin.getPLogger().logException(e);
        }
    }
}
