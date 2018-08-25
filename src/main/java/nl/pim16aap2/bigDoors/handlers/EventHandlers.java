package nl.pim16aap2.bigDoors.handlers;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import nl.pim16aap2.bigDoors.BigDoors;
import nl.pim16aap2.bigDoors.DoorCreator;

public class EventHandlers implements Listener
{
	private final BigDoors plugin;

	public EventHandlers(BigDoors plugin)
	{
		this.plugin     = plugin;
	}
	
	// Selection event.
	@EventHandler @SuppressWarnings("deprecation")
	public void onLeftClick(PlayerInteractEvent event)
	{
		if (event.getAction() == Action.LEFT_CLICK_BLOCK)
			if (plugin.getTF().isTool(event.getPlayer().getItemInHand()))
			{
				DoorCreator dc = plugin.getCommandHandler().isCreatingDoor(event.getPlayer());
				if (dc != null && dc.getName() != null)
				{
					dc.selector(event.getClickedBlock().getLocation());
					event.setCancelled(true);
				}
			}
	}
	
	// Do not allow the player to drop the door creation tool.
	@EventHandler
	public void onItemDropEvent(PlayerDropItemEvent event)
	{
		if (plugin.getTF().isTool(event.getItemDrop().getItemStack()))
			event.setCancelled(true);
	}
	
	// Do not allow the user to move the tool around in their inventory.
	@EventHandler
	public void onItemMoved(InventoryMoveItemEvent event)
	{
		if (plugin.getTF().isTool(event.getItem()))
			event.setCancelled(true);
	}
}
