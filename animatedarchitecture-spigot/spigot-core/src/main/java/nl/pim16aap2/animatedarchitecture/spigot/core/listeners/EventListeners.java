package nl.pim16aap2.animatedarchitecture.spigot.core.listeners;

import lombok.extern.flogger.Flogger;
import nl.pim16aap2.animatedarchitecture.core.api.restartable.RestartableHolder;
import nl.pim16aap2.animatedarchitecture.core.managers.DatabaseManager;
import nl.pim16aap2.animatedarchitecture.core.managers.DelayedCommandInputManager;
import nl.pim16aap2.animatedarchitecture.core.managers.ToolUserManager;
import nl.pim16aap2.animatedarchitecture.core.tooluser.ToolUser;
import nl.pim16aap2.animatedarchitecture.core.util.FutureUtil;
import nl.pim16aap2.animatedarchitecture.spigot.core.implementations.AnimatedArchitectureToolUtilSpigot;
import nl.pim16aap2.animatedarchitecture.spigot.util.SpigotAdapter;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
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
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Represents a listener that keeps track of various events.
 */
@Singleton
@Flogger
public class EventListeners extends AbstractListener
{
    private final AnimatedArchitectureToolUtilSpigot animatedArchitectureToolUtil;
    private final DatabaseManager databaseManager;
    private final ToolUserManager toolUserManager;
    private final DelayedCommandInputManager delayedCommandInputManager;

    @Inject
    EventListeners(
        JavaPlugin javaPlugin,
        AnimatedArchitectureToolUtilSpigot animatedArchitectureToolUtil,
        DatabaseManager databaseManager,
        ToolUserManager toolUserManager,
        DelayedCommandInputManager delayedCommandInputManager,
        RestartableHolder restartableHolder)
    {
        super(restartableHolder, javaPlugin);
        this.animatedArchitectureToolUtil = animatedArchitectureToolUtil;
        this.databaseManager = databaseManager;
        this.toolUserManager = toolUserManager;
        this.delayedCommandInputManager = delayedCommandInputManager;
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

        toolUserManager
            .getToolUser(event.getPlayer().getUniqueId())
            .ifPresent(toolUser ->
            {
                event.setCancelled(true);
                toolUser.handleInput(SpigotAdapter.wrapLocation(event.getClickedBlock().getLocation()));
            });
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
                .exceptionally(FutureUtil::exceptionally);
        }
        catch (Exception e)
        {
            log.atSevere().withCause(e).log("Failed to update player %s", event.getPlayer().getName());
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
            final var wrappedPlayer = SpigotAdapter.wrapPlayer(player);
            delayedCommandInputManager.cancelAll(wrappedPlayer);
            toolUserManager.abortToolUser(player.getUniqueId());
            databaseManager.updatePlayer(wrappedPlayer).exceptionally(FutureUtil::exceptionally);
        }
        catch (Exception e)
        {
            log.atSevere().withCause(e).log(
                "Encountered an error while cleaning up after player %s",
                event.getPlayer().getName()
            );
        }
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
     * Listens for players trying to drop their AnimatedArchitecture stick.
     * <p>
     * If they aren't a {@link ToolUser}, the stick is deleted, as they shouldn't have had it in the first place.
     * Otherwise, it's just cancelled.
     *
     * @param event
     *     The {@link PlayerDropItemEvent}.
     */
    @EventHandler
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
            log.atSevere().withCause(e).log("Encountered an error while handling a PlayerDropItemEvent");
        }
    }

    /**
     * Listens to players interacting with an inventory, to make sure they cannot move the AnimatedArchitecture stick to
     * another inventory.
     *
     * @param event
     *     The {@link InventoryClickEvent}.
     */
    @EventHandler
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
            log.atSevere().withCause(e).log("Encountered an error while handling an InventoryClickEvent");
        }
    }

    /**
     * Listens to players interacting with an inventory, to make sure they cannot move the AnimatedArchitecture stick to
     * another inventory.
     *
     * @param event
     *     The {@link InventoryDragEvent}.
     */
    @EventHandler
    public void inventoryDragEvent(InventoryDragEvent event)
    {
        try
        {
            event.getNewItems().forEach((key, value) ->
            {
                if (animatedArchitectureToolUtil.isTool(value))
                    event.setCancelled(true);
            });
        }
        catch (Exception e)
        {
            log.atSevere().withCause(e).log("Encountered an error while handling an InventoryDragEvent");
        }
    }

    /**
     * Listens to players attempting to move the AnimatedArchitecture stick to another inventory.
     *
     * @param event
     *     The {@link InventoryMoveItemEvent}.
     */
    @EventHandler
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
            log.atSevere().withCause(e).log("Encountered an error while handling an InventoryMoveItemEvent");
        }
    }
}
