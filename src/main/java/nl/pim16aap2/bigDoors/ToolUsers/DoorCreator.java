package nl.pim16aap2.bigDoors.ToolUsers;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import nl.pim16aap2.bigDoors.BigDoors;
import nl.pim16aap2.bigDoors.util.DoorType;
import nl.pim16aap2.bigDoors.util.Util;

/*
 * This class represents players in the process of creating doors.
 * Objects of this class are instantiated when the createdoor command is used and they are destroyed after
 * The creation process has been completed successfully or the timer ran out. In EventHandlers this class is used 
 * To check whether a user that is left-clicking is a DoorCreator && tell this class a left-click happened.
 */
public class DoorCreator extends ToolUser
{	
	public DoorCreator(BigDoors plugin, Player player, String name) 
	{
		super(plugin, player, name, DoorType.DOOR);
		Util.messagePlayer(player, messages.getString("DC.Init"));
		if (name == null)
			Util.messagePlayer(player, messages.getString("DC.GiveNameInstruc"));
		else
			triggerGiveTool();
	}
	
	@Override
	protected void triggerGiveTool()
	{
		giveToolToPlayer(messages.getString("BD.StickLore"    ).split("\n"), 
		                 messages.getString("BD.StickReceived").split("\n"));
	}
	
	@Override
	protected void triggerFinishUp()
	{
		finishUp(messages.getString("BD.Success"));
	}
	
	@Override
	protected boolean isReadyToCreateDoor()
	{
		return one != null && two != null && engine != null;
	}
	
	// Check if the engine selection is valid. 
	private boolean isEngineValid(Location loc)
	{
		if (loc.getBlockY() < one.getBlockY() || loc.getBlockY() > two.getBlockY())
			return false;
		// For a regular door, the engine should be on one of the outer pillars of the door.
		int xDepth = Math.abs(one.getBlockX() - two.getBlockX());
		int zDepth = Math.abs(one.getBlockZ() - two.getBlockZ());

		if (xDepth == 0)
			return loc.getBlockZ() == one.getBlockZ() || loc.getBlockZ() == two.getBlockZ();
		if (zDepth == 0)
			return loc.getBlockX() == one.getBlockX() || loc.getBlockX() == two.getBlockX();
		return false;
	}
	
	// Check if the second position is valid (door is 1 deep).
	private boolean isPosTwoValid(Location loc)
	{
		int xDepth = Math.abs(one.getBlockX() - loc.getBlockX());
		int yDepth = Math.abs(one.getBlockY() - loc.getBlockY());
		int zDepth = Math.abs(one.getBlockZ() - loc.getBlockZ());
		
		// If the door is only 1 block high, it's a drawbridge.
		if (yDepth == 0)
			return false;
		// Check if it's only 1 deep in exactly 1 direction (single moving pillar is useless).
		return xDepth == 0 ^ zDepth == 0; 
	}
	
	// Take care of the selection points.
	@Override
	public void selector(Location loc)
	{
		if (name == null)
			return;
		if (one == null)
		{
			one = loc;
			String[] message = messages.getString("DC.Step1").split("\n");
			Util.messagePlayer(player, message);
		}
		else if (two == null)
		{
			if (isPosTwoValid(loc) && one != loc)
			{
				two = loc;
				String[] message = messages.getString("DC.Step2").split("\n");
				Util.messagePlayer(player, message);
				minMaxFix();
			}
			else
				Util.messagePlayer(player, messages.getString("DC.InvalidPoint"));

		}
		// If the engine position has not been determined yet
		else if (engine == null)
		{
			if (isEngineValid(loc))
			{
				engine = loc;
				engine.setY(one.getY());
				Util.messagePlayer(player, messages.getString("DC.StepDoor3"));
				setIsDone(true);
				engine.setY(one.getBlockY());
			}
			else
				Util.messagePlayer(player, messages.getString("DC.InvalidRotation"));
		}
		else
			setIsDone(true);
	}
}
