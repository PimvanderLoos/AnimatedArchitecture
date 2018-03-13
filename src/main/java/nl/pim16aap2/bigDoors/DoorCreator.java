package nl.pim16aap2.bigDoors;

import java.util.Arrays;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import nl.pim16aap2.bigDoors.util.Util;

/*
 * This class represents players in the process of creating doors.
 * Objects of this class are instantiated when the createdoor command is used and they are destroyed after
 * The creation process has been completed successfully or the timer ran out. In EventHandlers this class is used 
 * To check whether a user that is left-clicking is a DoorCreator && tell this class a left-click happened.
 */

public class DoorCreator
{
	private final BigDoors plugin;
	private Player player;
	private String name;
	private Location one, two, engine;
	private boolean done = false;
	
	public DoorCreator(BigDoors plugin, Player player, String name)
	{
		this.plugin = plugin;
		this.player = player;
		this.name   = name;
		this.one    = null;
		this.two    = null;
		this.engine = null;
		
		giveToolToPlayer();
	}
	
	public Player getPlayer()
	{
		return this.player;
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
		engine.setY(one.getBlockY());
	}
	
	// Final cleanup
	public void finishUp()
	{
		minMaxFix();
		Door door = new Door(player, one.getWorld(), one.getBlockX(), one.getBlockY(), one.getBlockZ(), two.getBlockX(), two.getBlockY(), two.getBlockZ(), engine.getBlockX(), engine.getBlockY(), engine.getBlockZ(), name, false);
		plugin.getRDatabase().insert(door);
		takeToolFromPlayer();
	}
	
	// Give a selection tool to the player.
	public void giveToolToPlayer()
	{
		ItemStack tool = new ItemStack(Material.STICK, 1);
		tool.addUnsafeEnchantment(Enchantment.LUCK, 1);
		tool.getItemMeta().addItemFlags(ItemFlag.HIDE_ENCHANTS);
		
        ItemMeta itemMeta = tool.getItemMeta();
        itemMeta.setDisplayName("Big Door Creator Stick");
        itemMeta.setLore(Arrays.asList(
        		"This tool is used to create big doors", 
        		"First specify the region of the door", 
        		"Then specify the rotation point"));
        tool.setItemMeta(itemMeta);
        
		player.getInventory().addItem(tool);
		Util.messagePlayer(player, "You have been given a Big Door Creator Stick! Use it wisely!");
	}
	
	// Check if the provided itemstack is a selection tool.
	public static boolean isTool(ItemStack is)
	{	
		return 	is.getType() == Material.STICK 					&& 
				is.getEnchantmentLevel(Enchantment.LUCK) == 1 	&& 
				is.getItemMeta().getDisplayName() != null 		&& 
				is.getItemMeta().getDisplayName().toString().equals("Big Door Creator Stick");
	}
	
	// Take any selection tools in the player's inventory from them.
	public void takeToolFromPlayer()
	{
		for (ItemStack is : player.getInventory())
			if (is != null)
				if (isTool(is))
					is.setAmount(0);
	}
	
	// Check if the engine selection is valid. It should be on one of the outer pillars of the door.
	public boolean isEngineValid(Location loc)
	{
		int xDepth = Math.abs(one.getBlockX() - two.getBlockX());
		int zDepth = Math.abs(one.getBlockZ() - two.getBlockZ());

		if (xDepth == 0)
			return loc.getBlockZ() == one.getBlockZ() || loc.getBlockZ() == two.getBlockZ();
		if (zDepth == 0)
			return loc.getBlockX() == one.getBlockX() || loc.getBlockX() == two.getBlockX();
		return false;
	}
	
	// Check if the second position is valid (door is 1 deep).
	public boolean isPosTwoValid(Location loc)
	{
		int xDepth = Math.abs(one.getBlockX() - loc.getBlockX());
		int zDepth = Math.abs(one.getBlockZ() - loc.getBlockZ());
		Bukkit.broadcastMessage("xDepth = " + xDepth + ", zDepth = " + zDepth);
		return xDepth == 0 || zDepth == 0;
	}
	
	// Take care of the selection points.
	public void selector(Location loc)
	{
		if (one == null)
		{
			one = loc;
			Util.messagePlayer(player, "Step 1/3: First point selected! Please select a second point!");
		}
		else if (two == null)
		{
			if (isPosTwoValid(loc))
			{
				two = loc;
				Util.messagePlayer(player, "Step 2/3: Second point selected! Please select a rotation point!");
			}
			else
				Util.messagePlayer(player, "Invalid second point selection! Try again! Note that doors can only be one block deep!");

		}
		else if (engine == null)
		{
			if (isEngineValid(loc))
			{
				engine = loc;
				Util.messagePlayer(player, "Step 3/3: Rotation point selected! You should now be able to use your door!");
				done = true;
			}
			else
				Util.messagePlayer(player, "Invalid rotation point selection! Please select a point on the outsides of the door.");
		}
		else
		{
			done = true;
		}
	}
	
	// See if this class is done.
	public boolean isDone()
	{
		return this.done;
	}
}
