package nl.pim16aap2.bigDoors;

import java.util.Arrays;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.Vector;

import net.md_5.bungee.api.ChatColor;
import nl.pim16aap2.bigDoors.util.DoorDirection;
import nl.pim16aap2.bigDoors.util.Messages;
import nl.pim16aap2.bigDoors.util.Util;

/*
 * This class represents players in the process of creating doors.
 * Objects of this class are instantiated when the createdoor command is used and they are destroyed after
 * The creation process has been completed successfully or the timer ran out. In EventHandlers this class is used 
 * To check whether a user that is left-clicking is a DoorCreator && tell this class a left-click happened.
 */
public class DoorCreator
{
	private String                 name;
	private final BigDoors       plugin;
	private Player               player;
	private final Messages     messages;
	private int                type = 0;
	private DoorDirection    EngineSide;
	private boolean        done = false;
	private Location   one, two, engine;
	
	public DoorCreator(BigDoors plugin, Player player, String name)
	{
		this.plugin     = plugin;
		this.messages   = plugin.getMessages();
		this.player     = player;
		this.name       = name;
		this.one        = null;
		this.two        = null;
		this.engine     = null;
		this.EngineSide = null;
		Util.messagePlayer(player, ChatColor.GREEN, messages.getString("DC.Init"));
		if (name == null)
			Util.messagePlayer(player, ChatColor.GREEN, messages.getString("DC.GiveNameInstruc"));
		else
			giveToolToPlayer();
	}
	
	public Player getPlayer()
	{
		return this.player;
	}
	
	public void setName(String newName)
	{
		name = newName;
		giveToolToPlayer();
	}
	
	public String getName()
	{
		return this.name;
	}
	
	// Make sure position "one" contains the minimum values, "two" the maximum values and engine min.Y;
	public void minMaxFix()
	{
		int minX = one.getBlockX();
		int minY = one.getBlockY();
		int minZ = one.getBlockZ();
		int maxX = two.getBlockX();
		int maxY = two.getBlockY();
		int maxZ = two.getBlockZ();

		one.setX(minX > maxX ? maxX : minX);
		one.setY(minY > maxY ? maxY : minY);
		one.setZ(minZ > maxZ ? maxZ : minZ);
		two.setX(minX < maxX ? maxX : minX);
		two.setY(minY < maxY ? maxY : minY);
		two.setZ(minZ < maxZ ? maxZ : minZ);
	}
	
	// Final cleanup
	public void finishUp()
	{
//		if (this.EngineSide != null)
//			this.player.sendMessage(ChatColor.DARK_PURPLE + "Engine side = " + this.EngineSide.toString());
//		else
//			this.player.sendMessage(ChatColor.DARK_PURPLE + "Engine side is null");
		if (one != null && two != null && engine != null && (EngineSide != null || type != 1))
		{		
			Door door = new Door(player.getUniqueId(), one.getWorld(), one.getBlockX(), one.getBlockY(), one.getBlockZ(), two.getBlockX(), two.getBlockY(), two.getBlockZ(), 
					engine.getBlockX(), engine.getBlockY(), engine.getBlockZ(), name, false, -1, false, 0, type, EngineSide);
			plugin.getCommander().addDoor(door);
		}
		takeToolFromPlayer();
	}
	
	// Give a selection tool to the player.
	public void giveToolToPlayer()
	{
		ItemStack tool = new ItemStack(Material.STICK, 1);
		tool.addUnsafeEnchantment(Enchantment.LUCK, 1);
		tool.getItemMeta().addItemFlags(ItemFlag.HIDE_ENCHANTS);
		
        ItemMeta itemMeta = tool.getItemMeta();
        itemMeta.setDisplayName(messages.getString("DC.StickName"));
        String[] lore = messages.getString("DC.StickLore").split("\n");
        itemMeta.setLore(Arrays.asList(lore));
        tool.setItemMeta(itemMeta);
        
        int heldSlot = player.getInventory().getHeldItemSlot();
        if (player.getInventory().getItem(heldSlot) == null)
        		player.getInventory().setItem(heldSlot, tool);
        else
        		player.getInventory().addItem(tool);
		
		String[] message = messages.getString("DC.StickReceived").split("\n");
		Util.messagePlayer(player, message);
	}
	
	// Take any selection tools in the player's inventory from them.
	public void takeToolFromPlayer()
	{
		for (ItemStack is : player.getInventory())
			if (is != null)
				if (plugin.getTF().isTool(is))
					is.setAmount(0);
	}
	
	// Check if the engine selection is valid. 
	public boolean isEngineValid(Location loc)
	{
		if (type == 0)
		{
			// For a regular door, the engine should be on one of the outer pillars of the door.
			int xDepth = Math.abs(one.getBlockX() - two.getBlockX());
			int zDepth = Math.abs(one.getBlockZ() - two.getBlockZ());
	
			if (xDepth == 0)
				return loc.getBlockZ() == one.getBlockZ() || loc.getBlockZ() == two.getBlockZ();
			if (zDepth == 0)
				return loc.getBlockX() == one.getBlockX() || loc.getBlockX() == two.getBlockX();
		}
		else if (type == 1)
		{
			// For a drawbridge, the engine should be on one of the four sides (min x || max x || min z || max z).
			boolean validPos = loc.getBlockX() == one.getBlockX() || loc.getBlockX() == two.getBlockX() || loc.getBlockZ() == one.getBlockZ() || loc.getBlockZ() == two.getBlockZ();
			// You cannot select the same block twice.
			if (!validPos || (this.engine != null && loc.equals(this.engine)))
				return false;
			
			// Then check if there is any ambiguity. This happens when a corner was selected.
			if (this.engine == null)
			{
				// Check if a corner was selected.
				int posX = loc.getBlockX();
				int posZ = loc.getBlockZ();
				
				if (loc.equals(one) || loc.equals(two) || // "bottom left" or "top right" (on 2d grid on paper)
				   (posX == one.getBlockX() && posZ == two.getBlockZ()) || // "top left"
				   (posX == two.getBlockX() && posZ == one.getBlockZ()))   // "bottom right"
				{
					this.engine = loc;
					this.player.sendMessage(ChatColor.DARK_PURPLE + "Found a corner!");
				}
				else 
				{
					if      (posZ == one.getBlockZ())
						this.EngineSide = DoorDirection.NORTH;
					else if (posZ == two.getBlockZ())
						this.EngineSide = DoorDirection.SOUTH;
					else if (posX == one.getBlockX())
						this.EngineSide = DoorDirection.WEST;
					else if (posX == two.getBlockX())
						this.EngineSide = DoorDirection.EAST;
				}
				return true;
			}
			// If an engine point has already been selected
			else
			{				
				int posXa = engine.getBlockX();
				int posZa = engine.getBlockZ();
				
				// This part is commented out not because it doesn't work, but because it's more complicated and testing proved 
				// that it was slower than the vector method as well. Keeping it in case I need it again somewhere for some reason
				// Because it was a right bitch to build.
//				int posXb = loc.getBlockX();
//				int posZb = loc.getBlockZ();
//				this.player.sendMessage(ChatColor.DARK_GREEN + "Going to figure out which engine side to use!");
//				// First figure out which corner was selected.
//				if      (engine.equals(one)) // NORTH / WEST Possible
//				{
//					this.player.sendMessage(ChatColor.RED + "Side = 0");
//					if      (Util.between(posXb, one.getBlockX() + 1, two.getBlockX()) && posZb == two.getBlockZ()) // East side (from engine point)
//						this.EngineSide = DoorDirection.NORTH;
//					else if (Util.between(posZb, one.getBlockZ() + 1, two.getBlockZ()) && posXb == one.getBlockX()) // South side
//						this.EngineSide = DoorDirection.WEST;
//				}
//				else if (engine.equals(two)) // EAST / SOUTH Possible
//				{
//					this.player.sendMessage(ChatColor.RED + "Side = 1");
//					if      (Util.between(posZb, one.getBlockZ(), two.getBlockZ() - 1) && posXb == two.getBlockX()) // North side
//						this.EngineSide = DoorDirection.EAST;
//					else if (Util.between(posXb, one.getBlockX(), two.getBlockX() - 1) && posZb == two.getBlockZ()) // West side
//						this.EngineSide = DoorDirection.SOUTH;
//				}
//				else if (posXa == one.getBlockX() && posZa == two.getBlockZ()) // SOUTH / WEST Possible
//				{
//					this.player.sendMessage(ChatColor.RED + "Side = 2");
//					if      (Util.between(posZb, one.getBlockZ(), two.getBlockZ() - 1) && posXb == one.getBlockX()) // North side
//						this.EngineSide = DoorDirection.WEST;
//					else if (Util.between(posXb, one.getBlockX() + 1, two.getBlockX()) && posZb == two.getBlockZ()) // East side
//						this.EngineSide = DoorDirection.SOUTH;
//				}
//				else if (posXa == two.getBlockX() && posZa == one.getBlockZ()) // NORTH / EAST Possible
//				{
//					this.player.sendMessage(ChatColor.RED + "Side = 3");
//					if      (Util.between(posZb, one.getBlockZ() + 1, two.getBlockZ()) && posXb == two.getBlockX()) // South side
//						this.EngineSide = DoorDirection.EAST;
//					else if (Util.between(posXb, one.getBlockX(), two.getBlockX() - 1) && posZb == one.getBlockZ()) // West side
//						this.EngineSide = DoorDirection.NORTH;
//				}
//				else
//				{
//					this.player.sendMessage(ChatColor.RED + "Side = 4");
//					return false;
//				}

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
							this.EngineSide = DoorDirection.NORTH;
						else if (vector.getBlockZ() == 1)
							this.EngineSide = DoorDirection.WEST;
					}
					else if (engine.equals(two)) // EAST / SOUTH Possible
					{
						if (vector.getBlockX() == -1)
							this.EngineSide = DoorDirection.SOUTH;
						else if (vector.getBlockZ() == -1)
							this.EngineSide = DoorDirection.EAST;
					}
					else if (posXa == one.getBlockX() && posZa == two.getBlockZ()) // SOUTH / WEST Possible
					{
						if (vector.getBlockX() == 1)
							this.EngineSide = DoorDirection.SOUTH;
						else if (vector.getBlockZ() == -1)
							this.EngineSide = DoorDirection.WEST;
					}
					else if (posXa == two.getBlockX() && posZa == one.getBlockZ()) // NORTH / EAST Possible
					{
						if (vector.getBlockX() == -1)
							this.EngineSide = DoorDirection.NORTH;
						else if (vector.getBlockZ() == 1)
							this.EngineSide = DoorDirection.EAST;
					}
					else
						return false;
				}
			}
			
			return this.EngineSide != null;
		}
		return false;
	}
	
	// Check if the second position is valid (door is 1 deep).
	public boolean isPosTwoValid(Location loc)
	{
		int xDepth = Math.abs(one.getBlockX() - loc.getBlockX());
		int yDepth = Math.abs(one.getBlockY() - loc.getBlockY());
		int zDepth = Math.abs(one.getBlockZ() - loc.getBlockZ());
		
		// If the door is only 1 block high, it's a drawbridge.
		if (yDepth == 0)
			this.type = 1;
		// If the door is more than 1 block high, it's a type 0 (default door, default type).
		// Then check if it's only 1 deep in exactly 1 direction (single moving pillar is useless).
		return xDepth == 0 ^ zDepth == 0 || this.type == 1; 
	}
	
	// Take care of the selection points.
	public void selector(Location loc)
	{
		if (one == null)
		{
			one = loc;
			String[] message = messages.getString("DC.Step1").split("\n");
			Util.messagePlayer(player, message);
		}
		else if (two == null)
		{
			if (isPosTwoValid(loc))
			{
				two = loc;
				String[] message = messages.getString("DC.Step2").split("\n");
				Util.messagePlayer(player, message);
				minMaxFix();
			}
			else
				Util.messagePlayer(player, messages.getString("DC.InvalidPoint"));

		}
		// If the engine position has been determined yet
		else if (engine == null)
		{
			if (isEngineValid(loc))
			{
				engine = loc;
				if (type == 0)
				{
					Util.messagePlayer(player, messages.getString("DC.StepDoor3"));
					done = true;
					if (type == 0)
						engine.setY(one.getBlockY());
				}
				else
				{
					// If the engine side was found, print finish message.
					if (this.EngineSide != null)
					{
						Util.messagePlayer(player, messages.getString("DC.StepDraw3a"));
						done = true;
					}
					// If the engine side could not be determined, branch out for additional information.
					else
						Util.messagePlayer(player, messages.getString("DC.StepDraw3b"));
				}
			}
			else
				Util.messagePlayer(player, messages.getString("DC.InvalidRotation"));
		}
		// If it's a draw bridge and the engine side wasn't determined yet.
		else if (type == 1 && this.EngineSide == null)
		{
			if (isEngineValid(loc))
			{
				Util.messagePlayer(player, messages.getString("DC.StepDraw4"));
				done = true;
			}
			else
				Util.messagePlayer(player, messages.getString("DC.InvalidRotation"));
		}
		else
			done = true;
	}
	
	// See if this class is done.
	public boolean isDone()
	{
		return this.done;
	}
	
	// Change isDone status.
	public void setIsDone(boolean bool)
	{
		this.done = bool;
	}
}
