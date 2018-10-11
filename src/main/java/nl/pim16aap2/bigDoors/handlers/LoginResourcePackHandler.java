package nl.pim16aap2.bigDoors.handlers;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.scheduler.BukkitRunnable;

import nl.pim16aap2.bigDoors.BigDoors;

public class LoginResourcePackHandler implements Listener 
{
	BigDoors plugin;
	String   url;
	
	public LoginResourcePackHandler(BigDoors plugin, String url)
	{
		this.plugin = plugin;
		this.url    = url;
	}
	
	@EventHandler
	public void onLogin(PlayerLoginEvent event)
	{	
		// Slight delay so the player actually fully exists.
		new BukkitRunnable() 
		{
            @Override
            public void run() 
            {
            		if (event.getPlayer() != null)
            			event.getPlayer().setResourcePack(url);
            }
		}.runTaskLater(this.plugin, 10);
	}
}
