package nl.pim16aap2.bigDoors.ToolUsers;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import nl.pim16aap2.bigDoors.BigDoors;
import nl.pim16aap2.bigDoors.Door;
import nl.pim16aap2.bigDoors.util.Util;

public class PowerBlockInspector extends ToolUser
{
	public PowerBlockInspector(BigDoors plugin, Player player, long doorUID)
	{
		super(plugin, player, null, null);
		this.doorUID = doorUID;
        Util.messagePlayer(player, messages.getString("PBI.Init"));
        triggerGiveTool();
	}	
	
	@Override
	protected void triggerGiveTool()
	{
		// TODO: Give this stuff their own text.
		giveToolToPlayer(messages.getString("PBI.StickLore"    ).split("\n"), 
		                 messages.getString("PBI.StickReceived").split("\n"));
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
}
