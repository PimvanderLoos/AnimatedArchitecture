package nl.pim16aap2.bigDoors;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

//import net.minecraft.server.v1_11_R1.*;
import nl.pim16aap2.bigDoors.BigDoors;

public class EventHandlers implements Listener
{

	@SuppressWarnings("unused")
	private final BigDoors plugin;

	public EventHandlers(BigDoors plugin)
	{
		this.plugin = plugin;
	}
	
	@EventHandler
	public void onPlayerChat(AsyncPlayerChatEvent e)
	{
		Bukkit.broadcastMessage("You just tried to send a message!");
		e.setCancelled(true);
	}

}
