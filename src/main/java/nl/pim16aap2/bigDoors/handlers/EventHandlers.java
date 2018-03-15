package nl.pim16aap2.bigDoors.handlers;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import net.md_5.bungee.api.ChatColor;
import nl.pim16aap2.bigDoors.BigDoors;
import nl.pim16aap2.bigDoors.Door;
import nl.pim16aap2.bigDoors.DoorCreator;
import nl.pim16aap2.bigDoors.GUI.GUIPage;
import nl.pim16aap2.bigDoors.util.PageType;
import nl.pim16aap2.bigDoors.util.Util;

public class EventHandlers implements Listener
{
	private final BigDoors plugin;
	public final Material powerBlock = Material.BEDROCK;

	public EventHandlers(BigDoors plugin)
	{
		this.plugin = plugin;
	}
	
	// Selection event.
	@EventHandler @SuppressWarnings("deprecation")
	public void onLeftClick(PlayerInteractEvent event)
	{
		if (event.getAction() == Action.LEFT_CLICK_BLOCK)
			if (DoorCreator.isTool(event.getPlayer().getItemInHand()))
			{
				DoorCreator dc = plugin.getCommandHandler().isCreatingDoor(event.getPlayer());
				if (dc != null && dc.getName() != null)
				{
					dc.selector(event.getClickedBlock().getLocation());
					event.setCancelled(true);
				}
			}
	}

	// When redstone changes, check if there's a power block on any side of it (just not below it).
	// If so, a door has (probably) been found, so try to open it.
    @EventHandler
    public void onBlockRedstoneChange(BlockRedstoneEvent event)
    {
        try
        {
            Block block = event.getBlock();
            Location location = block.getLocation();
            if (event.getOldCurrent() != 0 && event.getNewCurrent() != 0)
                return;
            Door door = null;
            int x = location.getBlockX(), y = location.getBlockY(), z = location.getBlockZ();
            if (     location.getWorld().getBlockAt(x, y, z - 1).getType() == powerBlock) // North
            		door = plugin.getCommander().doorFromEngineLoc(x, y + 1, z - 1);
            else if (location.getWorld().getBlockAt(x + 1, y, z).getType() == powerBlock) // East
            		door = plugin.getCommander().doorFromEngineLoc(x + 1, y + 1, z);
            else if (location.getWorld().getBlockAt(x, y, z + 1).getType() == powerBlock) // South
            		door = plugin.getCommander().doorFromEngineLoc(x, y + 1, z + 1);
            else if (location.getWorld().getBlockAt(x - 1, y, z).getType() == powerBlock) // West
        			door = plugin.getCommander().doorFromEngineLoc(x - 1, y + 1, z);
            else if (location.getWorld().getBlockAt(x, y + 1, z).getType() == powerBlock) // Above
            		door = plugin.getCommander().doorFromEngineLoc(x, y + 2, z);
            else 
            		return;
            
            if (door != null && !door.isLocked())
            		plugin.getDoorOpener().openDoor(door, 0.2, true);
        }
        catch (Throwable t)
        {
			plugin.getMyLogger().logMessage("Exception thrown while handling redstone event!", true, false);
			plugin.getMyLogger().logMessage("79 " + t.getMessage());
        }
    }
    
    // Set the GUI page for the player to a specified page number on a specified pageType for a specified doorUID.
    private void setPage(Player player, Inventory inv, int page, PageType pageType, int doorUID, int pageCount)
    {
		inv.clear();
		new GUIPage(plugin, player, page, pageType, doorUID, pageCount);
    }
	
    // Start the new door creation process.
	private void startNewDoorProcess(Player player)
	{
		player.closeInventory();
		plugin.getCommandHandler().makeDoor(player, null);
	}
	
	// Retrieve the int from the end of a string.
	private int finalIntFromString(String str)
	{
		final Pattern lastIntPattern = Pattern.compile("[^0-9]+([0-9]+)$");
		Matcher matcher = lastIntPattern.matcher(str);
		if (matcher.find()) 
		{
		    String someNumberStr = matcher.group(1);
		    return Integer.parseInt(someNumberStr);
		}
		return -1;
	}
	
	// Retrieve the int from the end of a string.
	private int firstIntFromString(String str)
	{
		Matcher matcher = Pattern.compile("\\d+").matcher(str);
		if (matcher.find())
			return Integer.valueOf(matcher.group());
		return -1;
	}
	
	// Open a new menu dedicated to a door listed in the itemstack's lore.
	private void openDoorSubMenu(Player player, Inventory inv, ItemStack item)
	{
		int doorUID = getDoor(item).getDoorUID();
		openDoorSubMenu(player, inv, doorUID);
	}
	
	// Open a new menu dedicated to this door (passed as doorUID).
	private void openDoorSubMenu(Player player, Inventory inv, int doorUID)
	{
		// Set the page to the submenu for this door. Provide previous page number as page number, so the menu remembers where to go back to.
		setPage(player, inv, getCurrentPageNum(inv), PageType.DOORINFO, doorUID, getPageCount(inv));
	}
	
	private int getPageCount(Inventory inv)
	{
		return inv.getItem(0) != null ? finalIntFromString(inv.getItem(0).getItemMeta().getLore().get(inv.getItem(0).getItemMeta().getLore().size() - 1)) : 
		       inv.getItem(8) != null ? finalIntFromString(inv.getItem(8).getItemMeta().getLore().get(inv.getItem(8).getItemMeta().getLore().size() - 1)) : -1;
	}
	
	private int getPreviousPage(Inventory inv)
	{
		return inv.getItem(0) != null ? firstIntFromString(inv.getItem(0).getItemMeta().getLore().get(inv.getItem(0).getItemMeta().getLore().size() - 1)) - 1: 
		       inv.getItem(8) != null ? firstIntFromString(inv.getItem(8).getItemMeta().getLore().get(inv.getItem(8).getItemMeta().getLore().size() - 1)) - 3 : 0;
	}
	
	private int getCurrentPageNum(Inventory inv)
	{
		if (inv.getName() == GUIPage.getGUIConfirm())
			return getPreviousPage(inv) + 1;
		return inv.getItem(4) != null ? inv.getItem(4).getAmount() : 0;
	}
	
	private void toggleLock(Player player, Inventory inv)
	{
		// TODO:	 implement locking.
	}
	
	private void toggleDoor(Player player, Inventory inv)
	{
		plugin.getCommandHandler().openDoorCommand(player, getDoor(inv.getItem(4)));
	}
	
	private Door getDoor(ItemStack item)
	{
		String doorUIDString = item.getItemMeta().getLore().get(item.getItemMeta().getLore().size() - 1);
		int doorUID = finalIntFromString(doorUIDString);
		if (doorUID == -1)
		{
			plugin.getMyLogger().logMessage("149: Failed to retrieve door \"" + doorUIDString + "\"!", true, false);
			return null;
		}
		Door door = plugin.getCommander().getDoor(doorUID);
		return door;
	}
	
	private void deleteDoor(Player player, Inventory inv)
	{
		int doorUID = getDoor(inv.getItem(4)).getDoorUID();
		setPage(player, inv, 0, PageType.DOORLIST, -1, getPageCount(inv));
		plugin.getCommandHandler().delDoor(player, doorUID);
		Util.messagePlayer(player, ChatColor.RED, "The door has been deleted!");
	}
    
	// Check for clicks on items
	@EventHandler
	public void onInventoryClick(InventoryClickEvent event)
	{
		if (!(event.getWhoClicked() instanceof Player))
			return;
	
		String invName = event.getInventory().getName();
		// Type -1 = Not a BigDoors menu.
		// Type  0 = List of doors.
		// Type  1 = Door Sub-Menu.
		// Type  2 = Door deletion confirmation menu.
		PageType pageType = 	invName.equals(GUIPage.getGUIName())    ? PageType.DOORLIST     : 
							invName.equals(GUIPage.getGUISubName()) ? PageType.DOORINFO     :
							invName.equals(GUIPage.getGUIConfirm()) ? PageType.CONFIRMATION : PageType.NOTBIGDOORS;
		
		if (pageType == PageType.NOTBIGDOORS)
			return;

		event.setCancelled(true);

		Player player = (Player) event.getWhoClicked();
		ItemStack clickedItem = event.getCurrentItem();

		if (clickedItem == null || !clickedItem.hasItemMeta())
			return;

		ItemMeta meta     = clickedItem.getItemMeta();
		if (!meta.hasDisplayName())
			return;
		Inventory inv     = event.getInventory();
		int slot          = event.getRawSlot();
		String itemName   = meta.getDisplayName();
		
		if (pageType == PageType.CONFIRMATION)
		{
			if (itemName.equals(GUIPage.getConfirm()))
				deleteDoor(player, inv);
			else
				openDoorSubMenu(player, inv, inv.getItem(4));
		}
		else if (itemName.equals(GUIPage.getPreviousPageString()))
			setPage(player, inv, getPreviousPage(inv), PageType.DOORLIST, -1, getPageCount(inv));
		else if (pageType == PageType.DOORINFO)
		{
			if (itemName.equals(GUIPage.getLockDoor()) || itemName.equals(GUIPage.getUnlockDoor()))
				toggleLock(player, inv);
			else if (itemName.equals(GUIPage.getToggleDoor()))
				toggleDoor(player, inv);
			else if (itemName.equals(GUIPage.getDoorInfo()))
				Util.listDoorInfo(player, getDoor(inv.getItem(4)));
			else if (itemName.equals(GUIPage.getDelDoor()))
				setPage(player, inv, getPreviousPage(inv) + 1, PageType.CONFIRMATION, getDoor(inv.getItem(4)).getDoorUID(), getPageCount(inv));
			return;
		}
		else if (itemName.equals(GUIPage.getNewDoorString()))
			startNewDoorProcess(player);
		else if (itemName.equals(GUIPage.getNextPageString()))
			setPage(player, inv, inv.getItem(8).getAmount() - 1, PageType.DOORLIST, -1, getPageCount(inv));
		else if (slot > 8)
			openDoorSubMenu(player, inv, inv.getItem(slot));
	}
}
