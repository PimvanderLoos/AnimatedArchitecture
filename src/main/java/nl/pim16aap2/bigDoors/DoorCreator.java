package nl.pim16aap2.bigDoors;

import java.util.Arrays;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import net.md_5.bungee.api.ChatColor;
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
	private String               name;
	private final BigDoors     plugin;
	private Player             player;
	private final Messages   messages;
	private boolean      done = false;
	private Location one, two, engine;
	
	public DoorCreator(BigDoors plugin, Player player, String name)
	{
		this.plugin   = plugin;
		this.messages = plugin.getMessages();
		this.player   = player;
		this.name     = name;
		this.one      = null;
		this.two      = null;
		this.engine   = null;
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
		engine.setY(one.getBlockY());
	}
	
	// Final cleanup
	public void finishUp()
	{
		minMaxFix();
		Door door = new Door(player.getUniqueId(), one.getWorld(), one.getBlockX(), one.getBlockY(), one.getBlockZ(), two.getBlockX(), two.getBlockY(), two.getBlockZ(), 
				engine.getBlockX(), engine.getBlockY(), engine.getBlockZ(), name, false, -1, false, 0);
		plugin.getCommander().addDoor(door, player);
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
		return xDepth == 0 || zDepth == 0;
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
			}
			else
				Util.messagePlayer(player, messages.getString("DC.InvalidPoint"));

		}
		else if (engine == null)
		{
			if (isEngineValid(loc))
			{
				engine = loc;
				Util.messagePlayer(player, messages.getString("DC.Step3"));
				done = true;
			}
			else
				Util.messagePlayer(player, messages.getString("DC.InvalidRotation"));
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
