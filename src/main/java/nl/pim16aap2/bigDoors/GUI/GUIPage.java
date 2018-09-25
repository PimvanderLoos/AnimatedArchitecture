package nl.pim16aap2.bigDoors.GUI;

import java.util.ArrayList;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.Inventory;

import nl.pim16aap2.bigDoors.BigDoors;
import nl.pim16aap2.bigDoors.Door;
import nl.pim16aap2.bigDoors.util.DoorDirection;
import nl.pim16aap2.bigDoors.util.DoorType;
import nl.pim16aap2.bigDoors.util.Messages;
import nl.pim16aap2.bigDoors.util.PageType;
import nl.pim16aap2.bigDoors.util.RotateDirection;
import nl.pim16aap2.bigDoors.util.XMaterial;

public class GUIPage implements Listener
{
	private static Material pageSwitchMat  = Material.ARROW;
	private static Material currDoorMat    = Material.BOOK;
	private static Material changeTimeMat  = XMaterial.CLOCK.parseMaterial();
	private static Material newDoorMat     = XMaterial.WRITABLE_BOOK.parseMaterial();
	private static Material lockDoorMat    = XMaterial.GREEN_STAINED_GLASS_PANE.parseMaterial();
	private static Material unlockDoorMat  = XMaterial.RED_STAINED_GLASS_PANE.parseMaterial();
	private static Material confirmMat     = XMaterial.RED_STAINED_GLASS_PANE.parseMaterial();
	private static Material noConfirmMat   = XMaterial.GREEN_STAINED_GLASS_PANE.parseMaterial();
	private static Material toggleDoorMat  = Material.LEVER;
	private static Material infoMat        = Material.BOOKSHELF;
	private static Material delDoorMat     = Material.BARRIER;
	private static Material relocatePBMat  = Material.LEATHER_BOOTS;
	private static Material setOpenDirMat  = Material.COMPASS;
	private static byte     lockedData     = 14;
	private static byte     unlockedData   =  5;
	private static byte     confirmData    = 14;
	private static byte     notConfirmData =  5;
	private final  Messages messages;
	
	// Create a new inventory, with no owner, a size of nine, called example
	private Inventory         inv;
	private ArrayList<Door> doors;
	private final long  pageCount;
	private PageType     pageType; // Type 0 is door overview, type 1 is door submenu, type 2 is confirmation menu.
	private int              page;
	private Door             door;
	private static final int chestSize  = 45;
	
	private static final Material[] doorTypes = 
		{
			XMaterial.OAK_DOOR.parseMaterial(),
			XMaterial.OAK_TRAPDOOR.parseMaterial(),
			XMaterial.IRON_DOOR.parseMaterial()
		};
	
	public GUIPage(BigDoors plugin, Player player, int page, PageType pageType, long doorUID, int pageCount) 
	{
		this.messages  = plugin.getMessages();
		this.inv       = Bukkit.createInventory(player, chestSize, (pageType == PageType.DOORLIST ? messages.getString("GUI.Name") : pageType == PageType.DOORINFO ? messages.getString("GUI.SubName") : messages.getString("GUI.ConfirmMenu")));
		int startIndex = page * (chestSize - 9);			// Get starting and ending indices of the door to be displayed.
		int endIndex   = (page + 1) * (chestSize - 9);
		this.doors     = pageType != PageType.DOORLIST ? null : plugin.getCommander().getDoorsInRange(player.getUniqueId().toString(), null, startIndex, endIndex);
        	this.pageCount = (long) (pageCount == -1 ? Math.ceil(plugin.getCommander().countDoors(player.getUniqueId().toString(), null) / (chestSize - 9.0)) : pageCount); // If pageCount hasn't been set, calculate it.
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
			lore.add("Initiate drawbridge creation process");
			inv.setItem(3, new GUIItem(newDoorMat, messages.getString("GUI.NewDrawbridge"), lore, page + 1).getItemStack());
			lore.clear();
			
			lore.add("Initiate door creation process");
			inv.setItem(4, new GUIItem(newDoorMat, messages.getString("GUI.NewDoor"), lore, page + 1).getItemStack());
			lore.clear();
			
			lore.add("Initiate portcullis creation process");
			inv.setItem(5, new GUIItem(newDoorMat, messages.getString("GUI.NewPortcullis"), lore, page + 1).getItemStack());
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
		lore.clear();
	}
	
	public void createDoorSubMenu()
	{
		ArrayList<String> lore = new ArrayList<String>();
		if (door.isLocked())
			inv.setItem(9, new GUIItem(lockDoorMat, messages.getString("GUI.UnlockDoor"), null, 1, unlockedData).getItemStack());
		else
			inv.setItem(9, new GUIItem(unlockDoorMat, messages.getString("GUI.LockDoor"), null, 1, lockedData).getItemStack());
		
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
		lore.clear();
		
		desc = messages.getString("GUI.RelocatePowerBlock");
		loreStr = messages.getString("GUI.RelocatePowerBlockLore");
		lore.add(loreStr);
		inv.setItem(13, new GUIItem(relocatePBMat, desc, lore, 1).getItemStack());
		lore.clear();
		
		// TODO: Add these Strings to en_US.
		desc = messages.getString("GUI.ChangeTimer");
		loreStr = door.getAutoClose() > -1 ? messages.getString("GUI.ChangeTimerLore") + door.getAutoClose() + "s." :
			messages.getString("GUI.ChangeTimerLoreDisabled");
		lore.add(loreStr);
		int count = door.getAutoClose() < 1 ? 1 : door.getAutoClose();
		inv.setItem(14, new GUIItem(changeTimeMat, desc, lore, count).getItemStack());
		lore.clear();
		
		if (door.getType() != DoorType.PORTCULLIS)
		{
			desc = messages.getString("GUI.Direction.Name");
			RotateDirection doorsOpenDir = door.getOpenDir();
			loreStr = messages.getString("GUI.Direction.ThisDoorOpens")  +
			         (doorsOpenDir == RotateDirection.NONE               ? messages.getString("GUI.Direction.Any")     :
			          doorsOpenDir == RotateDirection.CLOCKWISE          ? messages.getString("GUI.Direction.Clock")   :
			          doorsOpenDir == RotateDirection.COUNTERCLOCKWISE   ? messages.getString("GUI.Direction.Counter") : "Error");
			lore.add(loreStr);
			lore.add(messages.getString("GUI.Direction.Looking") +
			        (door.getType()       == DoorType.DOOR       ? messages.getString("GUI.Direction.Down")  :
			         door.getLookingDir() == DoorDirection.NORTH ? messages.getString("GUI.Direction.East") :
			         messages.getString("GUI.Direction.North")));
			inv.setItem(15, new GUIItem(setOpenDirMat, desc, lore, 1).getItemStack());
			lore.clear();
		}
		
	}
	
	// Fill the entire menu with NO's, but put a single "yes" in the middle.
	public void fillConfirmationMenu()
	{
		int mid = (int) ((chestSize - 9) / 2) + 4;
		for (int idx = 9; idx < chestSize; ++idx)
		{
			ArrayList<String> lore = new ArrayList<String>();
			if (idx == mid) // Middle block.
			{
				lore.add(messages.getString("GUI.ConfirmDelete"));
				inv.setItem(idx, new GUIItem(noConfirmMat, messages.getString("GUI.Confirm"), lore, 1, confirmData).getItemStack());
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
				int realIdx  = idx - 9;
				int doorType = DoorType.getValue(doors.get(realIdx).getType());
//				int randomNum = ThreadLocalRandom.current().nextInt(0, 7);
				ArrayList<String> lore = new ArrayList<String>();
				lore.add("This door has ID " + doors.get(realIdx).getDoorUID());
				inv.setItem(idx, new GUIItem(doorTypes[doorType], doors.get(realIdx).getName(), lore, 1).getItemStack());
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