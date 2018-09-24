package nl.pim16aap2.bigDoors.toolUsers;

import java.util.logging.Level;

import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import net.md_5.bungee.api.ChatColor;
import nl.pim16aap2.bigDoors.BigDoors;
import nl.pim16aap2.bigDoors.util.Abortable;
import nl.pim16aap2.bigDoors.util.Util;

public class PowerBlockRelocator extends ToolUser implements Abortable
{
	public PowerBlockRelocator(BigDoors plugin, Player player, long doorUID)
	{
		super(plugin, player, null, null);
		this.doorUID = doorUID;
        Util.messagePlayer(player, messages.getString("CREATOR.PBRELOCATOR.Init"));
        triggerGiveTool();
	}	
	
	@Override
	protected void triggerGiveTool()
	{
		giveToolToPlayer(messages.getString("CREATOR.PBRELOCATOR.StickLore"    ).split("\n"), 
		                 messages.getString("CREATOR.PBRELOCATOR.StickReceived").split("\n"));
	}
	
	@Override
	protected void triggerFinishUp()
	{
		if (this.one != null)
		{
			plugin.getCommander().updatePowerBlockLoc(this.doorUID, this.one);
			Util.messagePlayer(player, messages.getString("CREATOR.PBRELOCATOR.Success"));
		}
		takeToolFromPlayer();
	}
	
	// Take care of the selection points.
	@Override
	public void selector(Location loc)
	{
		if (plugin.getCommander().isPowerBlockLocationValid(loc))	
		{
			this.done = true;
			this.one  = loc;
			setIsDone(true);
		}
		else 
			Util.messagePlayer(player, messages.getString("CREATOR.PBRELOCATOR.LocationInUse"));
	}

	@Override
	protected boolean isReadyToCreateDoor()
	{
		return false;
	}
	
	@Override
	public void abort()
	{
		this.takeToolFromPlayer();
		plugin.removeToolUser(this);
		plugin.getMyLogger().returnToSender((CommandSender) player, Level.INFO, ChatColor.RED, messages.getString("CREATOR.GENERAL.TimeUp"));
	}
}
