package nl.pim16aap2.bigDoors.toolUsers;

import java.util.logging.Level;

import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import net.md_5.bungee.api.ChatColor;
import nl.pim16aap2.bigDoors.BigDoors;
import nl.pim16aap2.bigDoors.util.Abortable;
import nl.pim16aap2.bigDoors.util.DoorType;
import nl.pim16aap2.bigDoors.util.Util;

/*
 * This class represents players in the process of creating doors.
 * Objects of this class are instantiated when the createdoor command is used and they are destroyed after
 * The creation process has been completed successfully or the timer ran out. In EventHandlers this class is used 
 * To check whether a user that is left-clicking is a DoorCreator && tell this class a left-click happened.
 */
public class DoorCreator extends ToolUser implements Abortable
{	
	public DoorCreator(BigDoors plugin, Player player, String name) 
	{
		super(plugin, player, name, DoorType.DOOR);
		Util.messagePlayer(player, messages.getString("CREATOR.DOOR.Init"));
		if (name == null)
			Util.messagePlayer(player, messages.getString("CREATOR.GENERAL.GiveNameInstruc"));
		else
			triggerGiveTool();
	}
	
	@Override
	protected void triggerGiveTool()
	{
		giveToolToPlayer(messages.getString("CREATOR.DOOR.StickLore"    ).split("\n"), 
		                 messages.getString("CREATOR.DOOR.StickReceived").split("\n"));
	}
	
	@Override
	protected void triggerFinishUp()
	{
		finishUp(messages.getString("CREATOR.DOOR.Success"));
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
			String[] message = messages.getString("CREATOR.DOOR.Step1").split("\n");
			Util.messagePlayer(player, message);
		}
		else if (two == null)
		{
			if (isPosTwoValid(loc) && one != loc)
			{
				two = loc;
				String[] message = messages.getString("CREATOR.DOOR.Step2").split("\n");
				Util.messagePlayer(player, message);
				minMaxFix();
			}
			else
				Util.messagePlayer(player, messages.getString("CREATOR.GENERAL.InvalidPoint"));

		}
		// If the engine position has not been determined yet
		else if (engine == null)
		{
			if (isEngineValid(loc))
			{
				engine = loc;
				engine.setY(one.getY());
//				Util.messagePlayer(player, messages.getString("CREATOR.DOOR.StepDoor3"));
				setIsDone(true);
				engine.setY(one.getBlockY());
			}
			else
				Util.messagePlayer(player, messages.getString("CREATOR.GENERAL.InvalidRotation"));
		}
		else
			setIsDone(true);
	}
	
	@Override
	public void abort()
	{
		this.takeToolFromPlayer();
		plugin.removeToolUser(this);
		plugin.getMyLogger().returnToSender((CommandSender) player, Level.INFO, ChatColor.RED, messages.getString("CREATOR.GENERAL.TimeUp"));
	}
}
