package nl.pim16aap2.bigDoors.waitForCommand;

import org.bukkit.entity.Player;

import com.mysql.jdbc.Messages;

import nl.pim16aap2.bigDoors.BigDoors;
import nl.pim16aap2.bigDoors.util.Abortable;
import nl.pim16aap2.bigDoors.util.Util;

public class WaitForSetTime implements WaitForCommand, Abortable
{
	private BigDoors plugin;
	private Player   player;
	private String  command;
	private long    doorUID;
	
	public WaitForSetTime(BigDoors plugin, Player player, String command, long doorUID)
	{
		this.player  = player;
		this.plugin  = plugin;
		this.command = command;
		this.doorUID = doorUID;
		Util.messagePlayer(player, plugin.getMessages().getString("GUI.SetTimeInit"));
		plugin.addCommandWaiter(this);
	}
	
	@Override
	public String getCommand()
	{
		return this.command;
	}
	
	@Override
	public Player getPlayer()
	{
		return this.player;
	}
	
	@Override
	public boolean executeCommand(String[] args)
	{
		if (args.length == 1)
		{
			try
			{
				int time = Integer.parseInt(args[0]);
				plugin.getCommandHandler().setDoorOpenTime(this.player, this.doorUID, time);
				plugin.removeCommandWaiter(this);
				return true;
			}
			catch (Exception e) 
			{
				Util.messagePlayer(player, Messages.getString("GUI.InvalidCloseTimerValue"));
			}
		}
		return false;
	}
	
	@Override
	public void abort()
	{
		plugin.removeCommandWaiter(this);
	}
}
