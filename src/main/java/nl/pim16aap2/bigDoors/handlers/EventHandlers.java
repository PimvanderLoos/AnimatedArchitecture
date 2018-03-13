package nl.pim16aap2.bigDoors.handlers;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

import nl.pim16aap2.bigDoors.BigDoors;
import nl.pim16aap2.bigDoors.DoorCreator;

public class EventHandlers implements Listener
{
	private final BigDoors plugin;

	public EventHandlers(BigDoors plugin)
	{
		this.plugin = plugin;
	}
	
	public DoorCreator isCreatingDoor(Player player)
	{
		for (DoorCreator dc : plugin.getDoorCreators())
			if (dc.getPlayer() == player)
				return dc;
		return null;
	}
	
	// Selection event.
	@EventHandler @SuppressWarnings("deprecation")
	public void onLeftClick(PlayerInteractEvent event)
	{
		if (event.getAction() == Action.LEFT_CLICK_BLOCK)
			if (DoorCreator.isTool(event.getPlayer().getItemInHand()))
			{
				DoorCreator dc = isCreatingDoor(event.getPlayer());
				if (dc != null)
				{
					dc.selector(event.getClickedBlock().getLocation());
					event.setCancelled(true);
				}
			}
	}
}
