package nl.pim16aap2.bigDoors.toolUsers;

import java.util.logging.Level;

import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import net.md_5.bungee.api.ChatColor;
import nl.pim16aap2.bigDoors.BigDoors;
import nl.pim16aap2.bigDoors.util.DoorDirection;
import nl.pim16aap2.bigDoors.util.DoorType;
import nl.pim16aap2.bigDoors.util.Util;

/*
 * This class represents players in the process of creating doors.
 * Objects of this class are instantiated when the createdoor command is used and they are destroyed after
 * The creation process has been completed successfully or the timer ran out. In EventHandlers this class is used 
 * To check whether a user that is left-clicking is a DoorCreator && tell this class a left-click happened.
 */
public class DrawbridgeCreator extends ToolUser
{	
	public DrawbridgeCreator(BigDoors plugin, Player player, String name) 
	{
		super(plugin, player, name, DoorType.DRAWBRIDGE);
		Util.messagePlayer(player, messages.getString("CREATOR.DRAWBRIDGE.Init"));
		if (name == null)
			Util.messagePlayer(player, messages.getString("CREATOR.GENERAL.GiveNameInstruc"));
		else
			triggerGiveTool();
	}
	
	@Override
	protected void triggerGiveTool()
	{
		giveToolToPlayer(messages.getString("CREATOR.DRAWBRIDGE.StickLore"    ).split("\n"), 
		                 messages.getString("CREATOR.DRAWBRIDGE.StickReceived").split("\n"));
	}
	
	@Override
	protected void triggerFinishUp()
	{
		finishUp(messages.getString("CREATOR.DRAWBRIDGE.Success"));
	}
	
	@Override
	protected boolean isReadyToCreateDoor()
	{
		return one != null && two != null && engine != null && engineSide != null;
	}
	
	// Check if the engine selection is valid. 
	private boolean isEngineValid(Location loc)
	{
		if (loc.getBlockY() != one.getBlockY())
			return false;
		
		// For a drawbridge, the engine should be on one of the four sides (min x || max x || min z || max z).
		boolean validPos = 	loc.getBlockX() == one.getBlockX() || 
							loc.getBlockX() == two.getBlockX() || 
							loc.getBlockZ() == one.getBlockZ() || 
							loc.getBlockZ() == two.getBlockZ();
		// You cannot select the same block twice.
		if (!validPos || (this.engine != null && loc.equals(this.engine)))
			return false;
		
		// Check if there is any ambiguity. This happens when a corner was selected.
		if (this.engine == null)
		{
			// Check if a corner was selected.
			int posX = loc.getBlockX();
			int posZ = loc.getBlockZ();
			
			if (loc.equals(one) || loc.equals(two) || // "bottom left" or "top right" (on 2d grid)
			   (posX == one.getBlockX() && posZ == two.getBlockZ()) || // "top left"
			   (posX == two.getBlockX() && posZ == one.getBlockZ()))   // "bottom right"
			{
				this.engine = loc;
			}
			else 
			{
				if      (posZ == one.getBlockZ())
					this.engineSide = DoorDirection.NORTH;
				else if (posZ == two.getBlockZ())
					this.engineSide = DoorDirection.SOUTH;
				else if (posX == one.getBlockX())
					this.engineSide = DoorDirection.WEST;
				else if (posX == two.getBlockX())
					this.engineSide = DoorDirection.EAST;
				drawBridgeEngineFix();	
			}
			return true;
		}
		// If an engine point has already been selected but an engine side wasn't determined yet.
		else
		{
			if (loc.equals(this.engine))
				return false;
			
			int posXa = engine.getBlockX();
			int posZa = engine.getBlockZ();
			
			// Engine axis should be on 1 axis only.
			Vector vector = loc.toVector().subtract(this.engine.toVector());
			vector.normalize();
			
			if (Math.abs(vector.getX() + vector.getY() + vector.getZ()) != 1)
				return false;
			else
			{
				// First figure out which corner was selected.
				if      (engine.equals(one)) // NORTH / WEST Possible
				{
					if (vector.getBlockX() == 1)
						this.engineSide = DoorDirection.NORTH;
					else if (vector.getBlockZ() == 1)
						this.engineSide = DoorDirection.WEST;
				}
				else if (engine.equals(two)) // EAST / SOUTH Possible
				{
					if (vector.getBlockX() == -1)
						this.engineSide = DoorDirection.SOUTH;
					else if (vector.getBlockZ() == -1)
						this.engineSide = DoorDirection.EAST;
				}
				else if (posXa == one.getBlockX() && posZa == two.getBlockZ()) // SOUTH / WEST Possible
				{
					if (vector.getBlockX() == 1)
						this.engineSide = DoorDirection.SOUTH;
					else if (vector.getBlockZ() == -1)
						this.engineSide = DoorDirection.WEST;
				}
				else if (posXa == two.getBlockX() && posZa == one.getBlockZ()) // NORTH / EAST Possible
				{
					if (vector.getBlockX() == -1)
						this.engineSide = DoorDirection.NORTH;
					else if (vector.getBlockZ() == 1)
						this.engineSide = DoorDirection.EAST;
				}
				else
					return false;
				drawBridgeEngineFix();
			}
		}
		return this.engineSide != null;
	}

	// Make sure the power point is in the middle.
	private void drawBridgeEngineFix()
	{
		if (this.engineSide == null || this.engine == null)
			return;
		
		// Make sure the power point is in the middle.
		if (this.engineSide == DoorDirection.NORTH || this.engineSide == DoorDirection.SOUTH)
			this.engine.setX(this.one.getX() + (this.two.getX() - this.one.getX()) / 2);
		else
			this.engine.setZ(this.one.getZ() + (this.two.getZ() - this.one.getZ()) / 2);
	}
	
	// Check if the second position is valid.
	private boolean isPosTwoValid(Location loc)
	{
		int xDepth = Math.abs(one.getBlockX() - loc.getBlockX());
		int yDepth = Math.abs(one.getBlockY() - loc.getBlockY());
		int zDepth = Math.abs(one.getBlockZ() - loc.getBlockZ());
		
		int count = 0;
		count += xDepth > 0 ? 1 : 0;
		count += yDepth > 0 ? 1 : 0;
		count += zDepth > 0 ? 1 : 0;
		return count == 2;
	}
	
	// Take care of the selection points.
	public void selector(Location loc)
	{
		if (name == null)
			return;
		if (one == null)
		{
			one = loc;
			String[] message = messages.getString("CREATOR.DRAWBRIDGE.Step1").split("\n");
			Util.messagePlayer(player, message);
		}
		else if (two == null)
		{
			if (isPosTwoValid(loc))
			{
				two = loc;
				// If it's up, it's closed.
				if (Math.abs(one.getBlockY() - two.getBlockY()) > 0)
					this.isOpen = false;
				else
					this.isOpen = true;
				
				String[] message = messages.getString("CREATOR.DRAWBRIDGE.Step2").split("\n");
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
				// If the engine side was found, print finish message.
				if (this.engineSide != null)
				{
//					Util.messagePlayer(player, messages.getString("DC.StepDraw3a"));
					drawBridgeEngineFix();
					setIsDone(true);
				}
				// If the engine side could not be determined, branch out for additional information.
				else
					Util.messagePlayer(player, messages.getString("CREATOR.DRAWBRIDGE.Step3"));
			}
			else
				Util.messagePlayer(player, messages.getString("CREATOR.GENERAL.InvalidRotation"));
		}
		// If it's a draw bridge and the engine side wasn't determined yet.
		else if (this.engineSide == null)
		{
			if (isEngineValid(loc))
			{
//				Util.messagePlayer(player, messages.getString("DC.StepDraw4"));
				drawBridgeEngineFix();
				setIsDone(true);
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
		if (!this.done)
		{
			this.takeToolFromPlayer();
			plugin.removeToolUser(this);
			plugin.getMyLogger().returnToSender((CommandSender) player, Level.INFO, ChatColor.RED, messages.getString("CREATOR.GENERAL.TimeUp"));
		}
	}
}
