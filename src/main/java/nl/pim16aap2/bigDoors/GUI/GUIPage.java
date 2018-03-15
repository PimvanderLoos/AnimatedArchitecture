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
import nl.pim16aap2.bigDoors.util.PageType;

public class GUIPage implements Listener
{
	// TODO: Make language config file for this stuff.
	private static final String GUIName    = "Door Menu";
	private static final String GUISubName = "Door Sub-Menu";
	private static final String GUIConfirm = "Confirmation Menu";
	private static String   confirm        = "YES, I am certain";
	private static String   notConfirm     = "NO!";
	private static String   nextPage       = "Next page";
	private static String   previousPage   = "Previous page";
	private static String   newDoor        = "New Door";
	private static String   lockDoor       = "Lock Door";
	private static String   unlockDoor     = "Unlock Door";
	private static String   toggleDoor     = "Toggle Door";
	private static String   doorInfo       = "Get Door Info";
	private static String   delDoor        = "Delete Door";
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
	
	// Create a new inventory, with no owner, a size of nine, called example
	private Inventory inv;
	private ArrayList<Door> doors;
	private final long pageCount;
	@SuppressWarnings("unused")
	private BigDoors plugin;
	private PageType pageType; // Type 0 is door overview, type 1 is door submenu, type 2 is confirmation menu.
	private int page;
	private Door door;
	private static final int chestSize  = 54;
	private static final Material[] doorTypes = {	Material.DARK_OAK_DOOR_ITEM,	Material.ACACIA_DOOR_ITEM, 
			                              			Material.BIRCH_DOOR_ITEM, 	Material.IRON_DOOR, 
			                              			Material.JUNGLE_DOOR_ITEM, 	Material.WOOD_DOOR, 
			                              			Material.SPRUCE_DOOR_ITEM};
	
	public GUIPage(BigDoors plugin, Player player, int page, PageType pageType, int doorUID, int pageCount) 
	{
		this.plugin    = plugin;
		this.inv       = Bukkit.createInventory(player, chestSize, (pageType == PageType.DOORLIST ? GUIName : pageType == PageType.DOORINFO ? GUISubName : GUIConfirm));
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
		inv.setItem(0, new GUIItem(pageSwitchMat, previousPage, lore, ((pageType == PageType.DOORINFO || pageType == PageType.CONFIRMATION) && page == 0 ? 1 : page)).getItemStack());
		// If it's not on the last page, add an arrow to the next page.  If it's a sub page, there is only a single page.
		lore.clear();
		lore.add("Go to page " + (page + 2) + " out of " + pageCount);
		if ((page + 1) < pageCount && pageType == PageType.DOORLIST)
			inv.setItem(8, new GUIItem(pageSwitchMat, nextPage, lore, page + 2).getItemStack());
				
		lore.clear();
		if (pageType == PageType.DOORLIST)
		{
			lore.add("Initiate door creation process");
			inv.setItem(4, new GUIItem(newDoorMat, newDoor, lore, page + 1).getItemStack());
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
			inv.setItem(9, new GUIItem(lockDoorMat, unlockDoor, null, 1, unlockedData).getItemStack());
		else
			inv.setItem(9, new GUIItem(lockDoorMat,   lockDoor, null, 1,   lockedData).getItemStack());
		lore.add("Open / Close this door");
		inv.setItem(10, new GUIItem(toggleDoorMat, toggleDoor, lore, 1).getItemStack());
		lore.clear();
		lore.add("Get more info about this door.");
		inv.setItem(11, new GUIItem(infoMat, doorInfo, lore, 1).getItemStack());
		lore.clear();
		lore.add("Delete this door. Be careful, this cannot be undone!");
		inv.setItem(12, new GUIItem(delDoorMat, delDoor, lore, 1).getItemStack());
		
		// TODO: Add more options: Add owners, remove owners, view list of owners
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
				lore.add("Yes, I am absolutely sure I want to delete this door!");
				inv.setItem(idx, new GUIItem(confirmMat, confirm, lore, 1, confirmData).getItemStack());
			}
			else
			{
				lore.add("NO! I don't want to delete this door!");
				inv.setItem(idx, new GUIItem(confirmMat, notConfirm, lore, 1, notConfirmData).getItemStack());
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

	public static String getGUIName() 			{ return GUIName; 		}
	public static String getGUISubName() 		{ return GUISubName;		}
	public static String getGUIConfirm()	 		{ return GUIConfirm; 	}
	public static String getConfirm()	 		{ return confirm; 		}
	public static String getNotConfirm() 		{ return notConfirm; 	}
	public static String getPreviousPageString()	{ return previousPage; 	}
	public static String getNextPageString() 	{ return nextPage; 		}
	public static String getNewDoorString() 		{ return newDoor; 		}
	public static String getLockDoor() 			{ return lockDoor;		}
	public static String getUnlockDoor()			{ return unlockDoor; 	}
	public static String getToggleDoor() 		{ return toggleDoor; 	}
	public static String getDoorInfo()	 		{ return doorInfo; 		}
	public static String getDelDoor()	 		{ return delDoor; 		}
	public static int    getChestSize() 			{ return chestSize; 		}
}
