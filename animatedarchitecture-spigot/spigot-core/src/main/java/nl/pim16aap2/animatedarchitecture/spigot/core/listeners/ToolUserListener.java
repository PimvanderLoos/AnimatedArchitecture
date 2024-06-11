package nl.pim16aap2.animatedarchitecture.spigot.core.listeners;

import lombok.extern.flogger.Flogger;
import nl.pim16aap2.animatedarchitecture.core.api.restartable.RestartableHolder;
import nl.pim16aap2.animatedarchitecture.core.managers.ToolUserManager;
import nl.pim16aap2.animatedarchitecture.core.tooluser.ToolUser;
import nl.pim16aap2.animatedarchitecture.spigot.core.implementations.AnimatedArchitectureToolUtilSpigot;
import nl.pim16aap2.animatedarchitecture.spigot.util.SpigotAdapter;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Represents a listener for events related to {@link ToolUser}s.
 * <p>
 * For example, this listener listens to players interacting with the world to check if they are using a
 * AnimatedArchitecture tool.
 * <p>
 * It also listens to players trying to move the AnimatedArchitecture stick out of their inventory. Either by dropping
 * it or moving it to another inventory.
 */
@Singleton
@Flogger
public final class ToolUserListener extends AbstractListener
{
    private final AnimatedArchitectureToolUtilSpigot animatedArchitectureToolUtil;
    private final ToolUserManager toolUserManager;

    @Inject ToolUserListener(
        JavaPlugin javaPlugin,
        AnimatedArchitectureToolUtilSpigot animatedArchitectureToolUtil,
        ToolUserManager toolUserManager,
        RestartableHolder restartableHolder)
    {
        super(restartableHolder, javaPlugin);
        this.animatedArchitectureToolUtil = animatedArchitectureToolUtil;
        this.toolUserManager = toolUserManager;
    }

    /**
     * Checks if a player is a {@link ToolUser} or not.
     *
     * @param player
     *     The {@link Player}.
     * @return True if a player is a {@link ToolUser}.
     */
    private boolean isToolUser(@Nullable Player player)
    {
        return player != null && toolUserManager.isToolUser(player.getUniqueId());
    }

    /**
     * Listens to players interacting with the world to check if they are using a AnimatedArchitecture tool.
     *
     * @param event
     *     The {@link PlayerInteractEvent}.
     */
    @EventHandler
    public void onLeftClick(PlayerInteractEvent event)
    {
        if (event.getAction() != Action.LEFT_CLICK_BLOCK)
            return;

        if (event.getClickedBlock() == null)
            return;

        if (!animatedArchitectureToolUtil.isPlayerHoldingTool(SpigotAdapter.wrapPlayer(event.getPlayer())))
            return;

        toolUserManager.getToolUser(event.getPlayer().getUniqueId()).ifPresent(
            toolUser ->
            {
                event.setCancelled(true);
                toolUser.handleInput(SpigotAdapter.wrapLocation(event.getClickedBlock().getLocation()));
            });
    }

    /**
     * Listens for players trying to drop their AnimatedArchitecture stick.
     * <p>
     * If they aren't a {@link ToolUser}, the stick is deleted, as they shouldn't have had it in the first place.
     * Otherwise, it's just cancelled.
     *
     * @param event
     *     The {@link PlayerDropItemEvent}.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onItemDropEvent(PlayerDropItemEvent event)
    {
        try
        {
            if (animatedArchitectureToolUtil.isPlayerHoldingTool(event.getPlayer()))
            {
                if (isToolUser(event.getPlayer()))
                    event.setCancelled(true);
                else
                    event.getItemDrop().remove();
            }
        }
        catch (Exception e)
        {
            log.atSevere().withCause(e).log();
        }
    }

    /**
     * Listens to players interacting with an inventory, to make sure they cannot move the AnimatedArchitecture stick to
     * another inventory.
     *
     * @param event
     *     The {@link InventoryClickEvent}.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void inventoryClickEvent(InventoryClickEvent event)
    {
        try
        {
            final @Nullable ItemStack currentItem = event.getCurrentItem();
            if (currentItem == null || !animatedArchitectureToolUtil.isTool(currentItem))
                return;

            final @Nullable Inventory clickedInventory = event.getClickedInventory();
            if (clickedInventory == null)
                return;

            if (event.getAction().equals(InventoryAction.MOVE_TO_OTHER_INVENTORY) ||
                !clickedInventory.getType().equals(InventoryType.PLAYER))
            {
                if (event.getWhoClicked() instanceof Player player)
                {
                    if (isToolUser(player))
                        event.setCancelled(true);
                    else
                        event.getInventory().removeItem(currentItem);
                }
                event.setCancelled(true);
            }
        }
        catch (Exception e)
        {
            log.atSevere().withCause(e).log();
        }
    }

    /**
     * Listens to players interacting with an inventory, to make sure they cannot move the AnimatedArchitecture stick to
     * another inventory.
     *
     * @param event
     *     The {@link InventoryDragEvent}.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void inventoryDragEvent(InventoryDragEvent event)
    {
        try
        {
            event.getNewItems().forEach(
                (key, value) ->
                {
                    if (animatedArchitectureToolUtil.isTool(value))
                        event.setCancelled(true);
                });
        }
        catch (Exception e)
        {
            log.atSevere().withCause(e).log();
        }
    }

    /**
     * Listens to players attempting to move the AnimatedArchitecture stick to another inventory.
     *
     * @param event
     *     The {@link InventoryMoveItemEvent}.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onItemMoved(InventoryMoveItemEvent event)
    {
        try
        {
            if (!animatedArchitectureToolUtil.isTool(event.getItem()))
                return;

            final Inventory src = event.getSource();
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
            log.atSevere().withCause(e).log();
        }
    }
}
