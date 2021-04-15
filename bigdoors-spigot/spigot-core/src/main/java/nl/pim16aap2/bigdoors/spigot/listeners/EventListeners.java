package nl.pim16aap2.bigdoors.spigot.listeners;

import lombok.NonNull;
import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.spigot.BigDoorsSpigot;
import nl.pim16aap2.bigdoors.spigot.util.SpigotAdapter;
import nl.pim16aap2.bigdoors.tooluser.ToolUser;
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
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.PlayerInventory;

/**
 * Represents a listener that keeps track of various events.
 *
 * @author Pim
 */
public class EventListeners implements Listener
{
    private final @NonNull BigDoorsSpigot plugin;

    public EventListeners(final @NonNull BigDoorsSpigot plugin)
    {
        this.plugin = plugin;
    }

    /**
     * Listens to players interacting with the world to check if they are using a BigDoors tool.
     *
     * @param event The {@link PlayerInteractEvent}.
     */
    @EventHandler
    public void onLeftClick(final PlayerInteractEvent event)
    {
        if (event.getAction() != Action.LEFT_CLICK_BLOCK)
            return;

        if (event.getClickedBlock() == null)
            return;

        if (!BigDoors.get().getPlatform().getBigDoorsToolUtil()
                     .isPlayerHoldingTool(SpigotAdapter.wrapPlayer(event.getPlayer())))
            return;

        BigDoors.get().getToolUserManager().getToolUser(event.getPlayer().getUniqueId()).ifPresent(
            toolUser ->
            {
                event.setCancelled(true);
                toolUser.handleInput(SpigotAdapter.wrapLocation(event.getClickedBlock().getLocation()));
            });
    }

    /**
     * Listens for the {@link PlayerJoinEvent} to make sure their latest name is updated in the database.
     *
     * @param event The {@link PlayerJoinEvent}.
     */
    @EventHandler
    public void onLogin(final PlayerJoinEvent event)
    {
        try
        {
            BigDoors.get().getDatabaseManager().updatePlayer(SpigotAdapter.wrapPlayer(event.getPlayer()));
        }
        catch (Exception e)
        {
            plugin.getPLogger().logThrowable(e);
        }
    }

    /**
     * Listens for the {@link PlayerQuitEvent} to make sure all processes they are active in are cancelled properly.
     *
     * @param event The {@link PlayerQuitEvent}.
     */
    @EventHandler
    public void onLogout(final PlayerQuitEvent event)
    {
        try
        {
            plugin.onPlayerLogout(event.getPlayer());
            BigDoors.get().getDatabaseManager().updatePlayer(SpigotAdapter.wrapPlayer(event.getPlayer()));
        }
        catch (Exception e)
        {
            plugin.getPLogger().logThrowable(e);
        }
    }

    /**
     * Checks if a player is a {@link ToolUser} or not.
     *
     * @param player The {@link Player}.
     * @return True if a player is a {@link ToolUser}.
     */
    private boolean isToolUser(final @NonNull Player player)
    {
        return BigDoors.get().getToolUserManager().isToolUser(player.getUniqueId());
    }

    /**
     * Listens for players trying to drop their BigDoors stick.
     * <p>
     * If they aren't a {@link ToolUser}, the stick is deleted, as they shouldn't have had it in the first place.
     * Otherwise, it's just cancelled.
     *
     * @param event The {@link PlayerDropItemEvent}.
     */
    @EventHandler
    public void onItemDropEvent(PlayerDropItemEvent event)
    {
        try
        {
            if (plugin.getBigDoorsToolUtil().isPlayerHoldingTool(event.getPlayer()))
            {
                if (isToolUser(event.getPlayer()))
                    event.setCancelled(true);
                else
                    event.getItemDrop().remove();
            }
        }
        catch (Exception e)
        {
            plugin.getPLogger().logThrowable(e);
        }
    }

    /**
     * Listens to players interacting with an inventory, to make sure they cannot move the BigDoors stick to another
     * inventory.
     *
     * @param event The {@link InventoryClickEvent}.
     */
    @EventHandler
    public void inventoryClickEvent(InventoryClickEvent event)
    {
        try
        {
            if (!plugin.getBigDoorsToolUtil().isTool(event.getCurrentItem()))
                return;
            if (event.getAction().equals(InventoryAction.MOVE_TO_OTHER_INVENTORY) ||
                !event.getClickedInventory().getType().equals(InventoryType.PLAYER))
            {
                if (event.getWhoClicked() instanceof Player)
                {
                    if (isToolUser((Player) event.getWhoClicked()))
                        event.setCancelled(true);
                    else
                        event.getInventory().removeItem(event.getCurrentItem());
                }
                event.setCancelled(true); // TODO: Check this.
            }
        }
        catch (Exception e)
        {
            plugin.getPLogger().logThrowable(e);
        }
    }

    /**
     * Listens to players interacting with an inventory, to make sure they cannot move the BigDoors stick to another
     * inventory.
     *
     * @param event The {@link InventoryDragEvent}.
     */
    @EventHandler
    public void inventoryDragEvent(InventoryDragEvent event)
    {
        try
        {
            event.getNewItems().forEach(
                (K, V) ->
                {
                    if (plugin.getBigDoorsToolUtil().isTool(V))
                        event.setCancelled(true);
                });
        }
        catch (Exception e)
        {
            plugin.getPLogger().logThrowable(e);
        }
    }

    /**
     * Listens to players attempting to move the BigDoors stick to another inventory.
     *
     * @param event The {@link InventoryMoveItemEvent}.
     */
    @EventHandler
    public void onItemMoved(InventoryMoveItemEvent event)
    {
        try
        {
            if (!plugin.getBigDoorsToolUtil().isTool(event.getItem()))
                return;

            Inventory src = event.getSource();
            if (src instanceof PlayerInventory && ((PlayerInventory) src).getHolder() instanceof Player)
            {
                if (isToolUser((Player) ((PlayerInventory) src).getHolder()))
                {
                    event.setCancelled(true);
                    return;
                }
            }
            src.removeItem(event.getItem());
        }
        catch (Exception e)
        {
            plugin.getPLogger().logThrowable(e);
        }
    }
}
