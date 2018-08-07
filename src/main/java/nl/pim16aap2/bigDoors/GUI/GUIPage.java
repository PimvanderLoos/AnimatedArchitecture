package nl.pim16aap2.bigDoors.GUI;

import java.util.ArrayList;
import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.Inventory;

import nl.pim16aap2.bigDoors.BigDoors;
import nl.pim16aap2.bigDoors.Door;
import nl.pim16aap2.bigDoors.util.Messages;
import nl.pim16aap2.bigDoors.util.PageType;

public class GUIPage implements Listener
{
	private static Material pageSwitchMat  = Material.ARROW;
	private static Material currDoorMat    = Material.BOOK;
	private static Material newDoorMat     = Material.BOOK_AND_QUILL;
	private static Material lockDoorMat    = Material.STAINED_GLASS_PANE;
	private static Material confirmMat     = Material.STAINED_GLASS_PANE;
	private static Material toggleDoorMat  = Material.LEVER;
	private static Material infoMat        = Material.BOOKSHELF;
	private static Material delDoorMat     = Material.BARRIER;
	private static byte     lockedData     = 14;
	private static byte     unlockedData   =  5;
	private static byte     confirmData    = 14;
	private static byte     notConfirmData =  5;
	private final Messages  messages;
	
	// Create a new inventory, with no owner, a size of nine, called example
	private Inventory         inv;
	private ArrayList<Door> doors;
	private final long  pageCount;
	private PageType     pageType; // Type 0 is door overview, type 1 is door submenu, type 2 is confirmation menu.
	private int              page;
	private Door             door;
	private static final int chestSize  = 54;
	private static final Material[] doorTypes = {Material.DARK_OAK_DOOR_ITEM,	Material.ACACIA_DOOR_ITEM, 
			                              		Material.BIRCH_DOOR_ITEM, 	Material.IRON_DOOR, 
			                              		Material.JUNGLE_DOOR_ITEM, 	Material.WOOD_DOOR, 
			                              		Material.SPRUCE_DOOR_ITEM};
	
	public GUIPage(BigDoors plugin, Player player, int page, PageType pageType, long doorUID, int pageCount) 
	{
		this.messages  = plugin.getMessages();
		this.inv       = Bukkit.createInventory(player, chestSize, (pageType == PageType.DOORLIST ? messages.getString("GUI.Name") : pageType == PageType.DOORINFO ? messages.getString("GUI.SubName") : messages.getString("GUI.ConfirmMenu")));
		int startIndex = page * (chestSize - 9);			// Get starting and ending indices of the door to be displayed.
		int endIndex   = (page + 1) * (chestSize - 9);
		this.doors     = pageType != PageType.DOORLIST ? null : plugin.getCommander().getDoorsInRange(player.getUniqueId().toString(), null, startIndex, endIndex);
        	this.pageCount = pageCount == -1 ? Math.round(plugin.getCommander().countDoors(player.getUniqueId().toString(), null) / (chestSize - 9.0)) : pageCount; // If pageCount hasn't been set, calculate it.
        	this.pageType  = pageType;
        	this.page      = page;
        	this.door      = doorUID != -1 ? plugin.getCommander().getDoor(doorUID) : null;
        	fillInventory(player);
    }
	
	public GUIPage(BigDoors plugin, Player player)
	{
		this(plugin, player, 0, PageType.DOORLIST, -1, -1);
	}

	// Populate the top row with general options.
	public void createHeader()
	{
		ArrayList<String> lore = new ArrayList<String>();
		lore.add("Go to page " + page + " out of " + pageCount);
		// If it's not on the first page, add an arrow to the first page.
		inv.setItem(0, new GUIItem(pageSwitchMat, messages.getString("GUI.PreviousPage"), lore, ((pageType == PageType.DOORINFO || pageType == PageType.CONFIRMATION) && page == 0 ? 1 : page)).getItemStack());
		// If it's not on the last page, add an arrow to the next page.  If it's a sub page, there is only a single page.
		lore.clear();
		lore.add("Go to page " + (page + 2) + " out of " + pageCount);
		if ((page + 1) < pageCount && pageType == PageType.DOORLIST)
			inv.setItem(8, new GUIItem(pageSwitchMat, messages.getString("GUI.NextPage"), lore, page + 2).getItemStack());
				
		lore.clear();
		if (pageType == PageType.DOORLIST)
		{
			lore.add("Initiate door creation process");
			inv.setItem(4, new GUIItem(newDoorMat, messages.getString("GUI.NewDoor"), lore, page + 1).getItemStack());
		}
		else if (pageType == PageType.DOORINFO)
		{
			lore.add("Expanded menu for door " + door.getName());
			lore.add("This door has ID " + door.getDoorUID());
			inv.setItem(4, new GUIItem(currDoorMat, door.getName() + ": " + door.getDoorUID(), lore, 1).getItemStack());
		}
		else if (pageType == PageType.CONFIRMATION)
		{
			lore.add("Expanded menu for door " + door.getName());
			lore.add("This door has ID " + door.getDoorUID());
			inv.setItem(4, new GUIItem(currDoorMat, door.getName() + ": " + door.getDoorUID(), lore, 1).getItemStack());
		}
	}
	
	public void createDoorSubMenu()
	{
		ArrayList<String> lore = new ArrayList<String>();
		if (door.isLocked())
			inv.setItem(9, new GUIItem(lockDoorMat, messages.getString("GUI.UnlockDoor"), null, 1, unlockedData).getItemStack());
		else
			inv.setItem(9, new GUIItem(lockDoorMat, messages.getString("GUI.LockDoor"), null, 1,   lockedData).getItemStack());
		String desc = messages.getString("GUI.ToggleDoor");
		lore.add(desc);
		inv.setItem(10, new GUIItem(toggleDoorMat, desc, lore, 1).getItemStack());
		lore.clear();
		desc = messages.getString("GUI.GetInfo");
		lore.add(desc);
		inv.setItem(11, new GUIItem(infoMat, desc, lore, 1).getItemStack());
		lore.clear();
		desc = messages.getString("GUI.DeleteDoor");
		String loreStr = messages.getString("GUI.DeleteDoorLong");
		lore.add(loreStr);
		inv.setItem(12, new GUIItem(delDoorMat, desc , lore, 1).getItemStack());
		
		// TODO: LT: Add more options: Add owners, remove owners, view list of owners
	}
	
	// Fill the entire menu with NO's, but put a single "yes" in the middle.
	public void fillConfirmationMenu()
	{
		for (int idx = 9; idx < chestSize; ++idx)
		{
			ArrayList<String> lore = new ArrayList<String>();
			int mid = (chestSize - 9) / 2 + 9;
			if (idx == mid) // Middle
			{
				lore.add(messages.getString("GUI.ConfirmDelete"));
				inv.setItem(idx, new GUIItem(confirmMat, messages.getString("GUI.Confirm"), lore, 1, confirmData).getItemStack());
			}
			else
			{
				lore.add(messages.getString("GUI.NotConfirm"));
				inv.setItem(idx, new GUIItem(confirmMat, messages.getString("GUI.No"), lore, 1, notConfirmData).getItemStack());
			}
		}
	}
	
	public void fillInventory(Player player)
	{
		createHeader();
		if (pageType == PageType.DOORLIST)
			for (int idx = 9; idx - 9 != doors.size() && idx < chestSize; ++idx)
			{
				int realIdx = idx - 9;
				int randomNum = ThreadLocalRandom.current().nextInt(0, 7);
				ArrayList<String> lore = new ArrayList<String>();
				lore.add("This door has ID " + doors.get(realIdx).getDoorUID());
				inv.setItem(idx, new GUIItem(doorTypes[randomNum], doors.get(realIdx).getName(), lore, 1).getItemStack());
			}
		else if (pageType == PageType.DOORINFO)
			createDoorSubMenu();
		else 
			fillConfirmationMenu();
		player.openInventory(inv);
	}

	// You can open the inventory with this
	public void openInventory(Player player)
	{
		player.openInventory(inv);
		return;
	}

	public static int getChestSize() { return chestSize;	}
}