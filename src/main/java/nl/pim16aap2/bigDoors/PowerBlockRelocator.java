package nl.pim16aap2.bigDoors;

import java.util.Arrays;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import nl.pim16aap2.bigDoors.util.Messages;
import nl.pim16aap2.bigDoors.util.Util;

public class PowerBlockRelocator
{
	private boolean done = false;
	private Messages    messages;
	private long         doorUID;
	private Player        player;
	private BigDoors      plugin;
	private Location         loc;
	
	public PowerBlockRelocator(BigDoors plugin, Player player, long doorUID)
	{
		this.loc      = null;
		this.plugin   = plugin;
		this.doorUID  = doorUID;
		this.messages = plugin.getMessages();
		this.player   = player;
        Util.messagePlayer(player, messages.getString("PBR.Init"));
		giveToolToPlayer();
	}
	
	public Player getPlayer()
	{
		return this.player;
	}
	
	// Final cleanup
	public void finishUp()
	{
		if (this.loc != null)
		{
			plugin.getCommander().updatePowerBlockLoc(this.doorUID, this.loc);
			Util.messagePlayer(player, messages.getString("PBR.Success"));
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
	
	// Take any selection tools in the player's inventory from them.
	public void takeToolFromPlayer()
	{
		for (ItemStack is : player.getInventory())
			if (is != null)
				if (plugin.getTF().isTool(is))
					is.setAmount(0);
	}
	
	// Take care of the selection points.
	public void selector(Location loc)
	{
		if (plugin.getCommander().isPowerBlockLocationValid(loc))	
		{
			this.done = true;
			this.loc  = loc;
		}
		else 
			Util.messagePlayer(player, messages.getString("PBR.LocationInUse"));
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
