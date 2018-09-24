package nl.pim16aap2.bigDoors.toolUsers;

import java.util.logging.Level;

import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import net.md_5.bungee.api.ChatColor;
import nl.pim16aap2.bigDoors.BigDoors;
import nl.pim16aap2.bigDoors.Door;
import nl.pim16aap2.bigDoors.util.Abortable;
import nl.pim16aap2.bigDoors.util.Util;

public class PowerBlockInspector extends ToolUser implements Abortable
{
	public PowerBlockInspector(BigDoors plugin, Player player, long doorUID)
	{
		super(plugin, player, null, null);
		this.doorUID = doorUID;
        Util.messagePlayer(player, messages.getString("CREATOR.PBINSPECTOR.Init"));
        triggerGiveTool();
	}	
	
	@Override
	protected void triggerGiveTool()
	{
		giveToolToPlayer(messages.getString("CREATOR.PBINSPECTOR.StickLore"    ).split("\n"), 
		                 messages.getString("CREATOR.PBINSPECTOR.StickReceived").split("\n"));
	}
	
	@Override
	protected void triggerFinishUp()
	{
		takeToolFromPlayer();
	}
	
	// Take care of the selection points.
	@Override
	public void selector(Location loc)
	{
		this.done = true;
		Door door = plugin.getCommander().doorFromPowerBlockLoc(loc);
		if (door != null)
		{
			plugin.getCommandHandler().listDoorInfo(player, door);
			setIsDone(true);
		}
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
