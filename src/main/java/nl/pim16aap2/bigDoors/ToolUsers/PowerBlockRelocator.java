package nl.pim16aap2.bigDoors.ToolUsers;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import nl.pim16aap2.bigDoors.BigDoors;
import nl.pim16aap2.bigDoors.util.Util;

public class PowerBlockRelocator extends ToolUser
{
	public PowerBlockRelocator(BigDoors plugin, Player player, long doorUID)
	{
		super(plugin, player, null, null);
		this.doorUID = doorUID;
        Util.messagePlayer(player, messages.getString("PBR.Init"));
        triggerGiveTool();
	}	
	
	@Override
	protected void triggerGiveTool()
	{
		giveToolToPlayer(messages.getString("PBR.StickLore"    ).split("\n"), 
		                 messages.getString("PBR.StickReceived").split("\n"));
	}
	
	@Override
	protected void triggerFinishUp()
	{
		if (this.one != null)
		{
			plugin.getCommander().updatePowerBlockLoc(this.doorUID, this.one);
			Util.messagePlayer(player, messages.getString("PBR.Success"));
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
			Util.messagePlayer(player, messages.getString("PBR.LocationInUse"));
	}

	@Override
	protected boolean isReadyToCreateDoor()
	{
		return false;
	}
}
