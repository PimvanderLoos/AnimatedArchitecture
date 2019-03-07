package nl.pim16aap2.bigDoors.waitForCommand;

import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import nl.pim16aap2.bigDoors.BigDoors;
import nl.pim16aap2.bigDoors.util.Abortable;
import nl.pim16aap2.bigDoors.util.Util;

public class WaitForSetTime implements WaitForCommand, Abortable
{
	private BigDoors plugin;
	private Player   player;
	private String  command;
	private long    doorUID;
    private BukkitTask bukkitTask;

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
    public void setTask(BukkitTask task)
    {
        bukkitTask = task;
    }

    @Override
    public BukkitTask getTask()
    {
        return bukkitTask;
    }

	@Override
	public String getCommand()
	{
		return command;
	}

	@Override
	public Player getPlayer()
	{
		return player;
	}

	@Override
	public boolean executeCommand(String[] args)
	{
		if (args.length == 1)
		{
			try
			{
				int time = Integer.parseInt(args[0]);
				plugin.getCommandHandler().setDoorOpenTime(player, doorUID, time);
				plugin.removeCommandWaiter(this);
				if (time != -1)
					Util.messagePlayer(player, plugin.getMessages().getString("GENERAL.SetCloseTimerSuccess") + time + "s.");
				else
					Util.messagePlayer(player, plugin.getMessages().getString("GENERAL.DisableCloseTimerSuccess") + time + "s.");
				return true;
			}
			catch (Exception e)
			{
				Util.messagePlayer(player, plugin.getMessages().getString("GUI.InvalidCloseTimerValue"));
			}
		}
		return false;
	}

	@Override
	public void abort(boolean onDisable)
	{
		if (!onDisable)
			plugin.removeCommandWaiter(this);
	}

    @Override
    public void abort()
    {
        abort(false);
    }
}
