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

public class PortcullisCreator
{
	private boolean done = false;
	private Messages    messages;
	private Player        player;
	private BigDoors      plugin;
	private Location      engine;
	private String          name;
	private Location         one;
	private Location         two;
	
	public PortcullisCreator(BigDoors plugin, Player player, String name)
	{
		this.one      = null;
		this.two      = null;
		this.engine   = null;
		this.name     = name;
		this.plugin   = plugin;
		this.messages = plugin.getMessages();
		this.player   = player;
		Util.messagePlayer(player, messages.getString("PCC.Init"));
		if (name == null)
			Util.messagePlayer(player, ChatColor.GREEN, messages.getString("DC.GiveNameInstruc"));
		else
			giveToolToPlayer();
	}
	
	public Player getPlayer()
	{
		return this.player;
	}
	
	// Final cleanup
	public void finishUp()
	{
		if (this.one != null && this.two != null && this.name != null)
		{
			Door door = new Door(player.getUniqueId(), one.getWorld(), one.getBlockX(), one.getBlockY(), one.getBlockZ(), two.getBlockX(), two.getBlockY(), two.getBlockZ(), 
					engine.getBlockX(), engine.getBlockY(), engine.getBlockZ(), name, false, -1, false, 0, 2, null,
					engine.getBlockX(), engine.getBlockY() - 1, engine.getBlockZ());
			plugin.getCommander().addDoor(door);
			
			Util.messagePlayer(player, messages.getString("PCC.Success"));
		}
		takeToolFromPlayer();
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

	// Make sure the power point is in the middle.
	public void setEngine()
	{
		int xMid = one.getBlockX() + (two.getBlockX() - one.getBlockX()) / 2;
		int zMid = one.getBlockZ() + (two.getBlockZ() - one.getBlockZ()) / 2;
		int yMin = one.getBlockY();
		this.engine = new Location(one.getWorld(), xMid, yMin, zMid);
	}
	
	// Give a selection tool to the player.
	public void giveToolToPlayer()
	{
		ItemStack tool = new ItemStack(Material.STICK, 1);
		tool.addUnsafeEnchantment(Enchantment.LUCK, 1);
		tool.getItemMeta().addItemFlags(ItemFlag.HIDE_ENCHANTS);
		
        ItemMeta itemMeta = tool.getItemMeta();
        itemMeta.setDisplayName(messages.getString("DC.StickName"));
        String[] lore = messages.getString("PBR.StickLore").split("\n");
        itemMeta.setLore(Arrays.asList(lore));
        tool.setItemMeta(itemMeta);
        
        int heldSlot = player.getInventory().getHeldItemSlot();
        if (player.getInventory().getItem(heldSlot) == null)
        		player.getInventory().setItem(heldSlot, tool);
        else
        		player.getInventory().addItem(tool);
		
        Util.messagePlayer(player, messages.getString("PBR.StickReceived"));
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
	
	// Take any selection tools in the player's inventory from them.
	public void takeToolFromPlayer()
	{
		for (ItemStack is : player.getInventory())
			if (is != null)
				if (plugin.getTF().isTool(is))
					is.setAmount(0);
	}
	
	// Make sure the second position is not the same as the first position
	// And that the portcullis is only 1 block deep.
	public boolean isPositionValid(Location loc)
	{
		if (one == null && two == null)
			return true;	
		if (one.equals(loc))
			return false;
		
		int xDepth, zDepth;
		xDepth = Math.abs(loc.getBlockX() - one.getBlockX());
		zDepth = Math.abs(loc.getBlockZ() - one.getBlockZ());
		
		return xDepth == 0 ^ zDepth == 0;
	}
	
	// Take care of the selection points.
	public void selector(Location loc)
	{
		if (!isPositionValid(loc))
			return;
		if (this.one == null)
		{
			this.one = loc;
			Util.messagePlayer(player, messages.getString("PCC.Step1"));
		}
		else
			this.two = loc;
		
		if (this.one != null && this.two != null)
		{
			minMaxFix();
			setEngine();
			this.done = true;
		}
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
