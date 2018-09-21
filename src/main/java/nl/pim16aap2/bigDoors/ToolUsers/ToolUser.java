package nl.pim16aap2.bigDoors.ToolUsers;

import java.util.Arrays;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import nl.pim16aap2.bigDoors.BigDoors;
import nl.pim16aap2.bigDoors.Door;
import nl.pim16aap2.bigDoors.util.DoorDirection;
import nl.pim16aap2.bigDoors.util.DoorType;
import nl.pim16aap2.bigDoors.util.Messages;
import nl.pim16aap2.bigDoors.util.Util;

public abstract class ToolUser
{
	protected DoorType             type;
	protected String               name;
	protected final BigDoors     plugin;
	protected Player             player;
	protected long              doorUID;
	protected final Messages   messages;
	protected DoorDirection  engineSide;
	protected boolean      done = false;
	protected boolean    isOpen = false;
	protected Location one, two, engine;
	
	public ToolUser(BigDoors plugin, Player player, String name, DoorType type)
	{
		this.plugin     = plugin;
		this.messages   = plugin.getMessages();
		this.player     = player;
		this.name       = name;
		this.one        = null;
		this.two        = null;
		this.engine     = null;
		this.engineSide = null;
		this.type       = type;
	}
	
	// Handle location input (player hitting a block).
	public    abstract void    selector(Location loc);
	// Give a tool to a player (but get correct strings etc from translation file first).
	protected abstract void    triggerGiveTool();
	// Finish up (but get correct strings etc from translation file first).
	protected abstract void    triggerFinishUp();
	// Check if all the variables that cannot be null are not null.
	protected abstract boolean isReadyToCreateDoor();
	
	// Final cleanup
	protected void finishUp(String message)
	{
		if (isReadyToCreateDoor())
		{
			World world     = one.getWorld();
			Location min    = new Location(world, one.getBlockX(), one.getBlockY(), one.getBlockZ());
			Location max    = new Location(world, two.getBlockX(), two.getBlockY(), two.getBlockZ());
			Location engine = new Location(world, this.engine.getBlockX(), this.engine.getBlockY(), this.engine.getBlockZ());
			Location powerB = new Location(world, this.engine.getBlockX(), this.engine.getBlockY() - 1, this.engine.getBlockZ());
			
			Door door = new Door(player.getUniqueId(), world, min, max, engine, name, isOpen, -1, false, 
					0, this.type, engineSide, powerB, null);
			plugin.getCommander().addDoor(door);
			
			Util.messagePlayer(player, message);
		}
		takeToolFromPlayer();
	}
	
	protected void giveToolToPlayer(String[] lore, String[] message)
	{
		ItemStack tool = new ItemStack(Material.STICK, 1);
		tool.addUnsafeEnchantment(Enchantment.LUCK, 1);
		tool.getItemMeta().addItemFlags(ItemFlag.HIDE_ENCHANTS);
		
        ItemMeta itemMeta = tool.getItemMeta();
        itemMeta.setDisplayName(messages.getString("DC.StickName"));
        itemMeta.setLore(Arrays.asList(lore));
        tool.setItemMeta(itemMeta);
        
        int heldSlot = player.getInventory().getHeldItemSlot();
        if (player.getInventory().getItem(heldSlot) == null)
        		player.getInventory().setItem(heldSlot, tool);
        else
        		player.getInventory().addItem(tool);
		
		Util.messagePlayer(player, message);
	}
	
	public Player getPlayer()
	{
		return this.player;
	}

	public void setName(String newName)
	{
		name = newName;
		triggerGiveTool();
	}
	
	// Take any selection tools in the player's inventory from them.
	public void takeToolFromPlayer()
	{
		for (ItemStack is : player.getInventory())
			if (is != null)
				if (plugin.getTF().isTool(is))
					is.setAmount(0);
	}
	
	public String getName()
	{
		return this.name;
	}
	
	// Make sure position "one" contains the minimum values, "two" the maximum values and engine min.Y;
	protected void minMaxFix()
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
	
	// See if this class is done.
	public boolean isDone()
	{
		return this.done;
	}
	
	// Change isDone status and 
	public void setIsDone(boolean bool)
	{
		this.done = bool;
		if (bool)
		{
			triggerFinishUp();
			plugin.getToolUsers().remove(this);
		}
	}
}
