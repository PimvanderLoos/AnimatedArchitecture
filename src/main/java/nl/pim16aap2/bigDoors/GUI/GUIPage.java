package nl.pim16aap2.bigDoors.GUI;

import java.util.ArrayList;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.Inventory;

import nl.pim16aap2.bigDoors.BigDoors;
import nl.pim16aap2.bigDoors.Door;
import nl.pim16aap2.bigDoors.util.DoorAttribute;
import nl.pim16aap2.bigDoors.util.DoorDirection;
import nl.pim16aap2.bigDoors.util.DoorType;
import nl.pim16aap2.bigDoors.util.Messages;
import nl.pim16aap2.bigDoors.util.PageType;
import nl.pim16aap2.bigDoors.util.RotateDirection;
import nl.pim16aap2.bigDoors.util.XMaterial;

public class GUIPage implements Listener
{
    private static final Material PAGESWITCHMAT  = Material.ARROW;
    private static final Material CURRDOORMAT    = Material.BOOK;
    private static final Material CHANGETIMEMAT  = XMaterial.CLOCK.parseMaterial();
    private static final Material NEWDOORMAT     = XMaterial.WRITABLE_BOOK.parseMaterial();
    private static final Material LOCKDOORMAT    = XMaterial.GREEN_STAINED_GLASS_PANE.parseMaterial();
    private static final Material UNLOCKDOORMAT  = XMaterial.RED_STAINED_GLASS_PANE.parseMaterial();
    private static final Material CONFIRMMAT     = XMaterial.RED_STAINED_GLASS_PANE.parseMaterial();
    private static final Material NOTCONFIRMMAT  = XMaterial.GREEN_STAINED_GLASS_PANE.parseMaterial();
    private static final Material TOGGLEDOORMAT  = Material.LEVER;
    private static final Material INFOMAT        = Material.BOOKSHELF;
    private static final Material DELDOORMAT     = Material.BARRIER;
    private static final Material RELOCATEPBMAT  = Material.LEATHER_BOOTS;
    private static final Material SETOPENDIRMAT  = Material.COMPASS;
    private static final Material SETBTMOVEMAT   = XMaterial.STICKY_PISTON.parseMaterial();
    private static final Material ADDOWNERMAT    = Material.SKULL_ITEM;
    private static final Material REMOVEOWNERMAT = Material.SKULL_ITEM;
    private static final byte     LOCKEDDATA     = 14;
    private static final byte     UNLOCKEDDATA   =  5;
    private static final byte     CONFIRMDATA    = 14;
    private static final byte     NOTCONFIRMDATA =  5;
    private static final byte     PLAYERHEADDATA =  3;
    private static final byte     SKULLDATA      =  0;
    private final  Messages messages;

    // Create a new inventory, with no owner, a size of nine, called example
    private Inventory         inv;
    private ArrayList<Door> doors;
    private final long  pageCount;
    private PageType     pageType;
    private int              page;
    private Door             door;
    private static final int CHESTSIZE = 45;

    private static final Material[] DOORTYPES =
    {
        XMaterial.OAK_DOOR.parseMaterial(),
        XMaterial.OAK_TRAPDOOR.parseMaterial(),
        XMaterial.IRON_DOOR.parseMaterial(),
        XMaterial.OAK_BOAT.parseMaterial(),
        XMaterial.GLASS_PANE.parseMaterial(),
        XMaterial.PURPLE_CARPET.parseMaterial()
    };

    public GUIPage(BigDoors plugin, Player player, int page, PageType pageType, long doorUID, int pageCount)
    {
        messages = plugin.getMessages();
        inv = Bukkit.createInventory(player, CHESTSIZE,
                  (pageType == PageType.DOORLIST ? messages.getString("GUI.Name") :
                   pageType == PageType.DOORINFO ? messages.getString("GUI.SubName") :
                   messages.getString("GUI.ConfirmMenu")));
        int startIndex =  page * (CHESTSIZE - 9);            // Get starting and ending indices of the door to be displayed.
        int endIndex   = (page + 1) * (CHESTSIZE - 9);

        doors = pageType != PageType.DOORLIST ? null :
            plugin.getCommander().getDoorsInRange(player.getUniqueId().toString(), null, startIndex, endIndex);

        this.pageCount = (long) (pageCount == -1 ?
                          Math.ceil(plugin.getCommander().countDoors(player.getUniqueId().toString(), null) /
                                    (CHESTSIZE - 9.0)) : pageCount); // If pageCount hasn't been set, calculate it.
        this.pageType  = pageType;
        this.page      = page;
        door = doorUID != -1 ? plugin.getCommander().getDoor(player.getUniqueId(), doorUID) : null;
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
        inv.setItem(0, new GUIItem(PAGESWITCHMAT, messages.getString("GUI.PreviousPage"),
                                   lore, ((pageType == PageType.DOORINFO ||
                                           pageType == PageType.CONFIRMATION) &&
                                          page == 0 ? 1 : page)).getItemStack());
        // If it's not on the last page, add an arrow to the next page.  If it's a sub page, there is only a single page.
        lore.clear();

        lore.add("Go to page " + (page + 2) + " out of " + pageCount);
        if ((page + 1) < pageCount && pageType == PageType.DOORLIST)
            inv.setItem(8, new GUIItem(PAGESWITCHMAT, messages.getString("GUI.NextPage"),
                                       lore, page + 2).getItemStack());
        lore.clear();

        if (pageType == PageType.DOORLIST)
        {
            lore.add(messages.getString("GUI.NewObjectLong") + messages.getString("GUI.NewElevator"));
            inv.setItem(2, new GUIItem(NEWDOORMAT, messages.getString("GUI.NewElevator"),
                                       lore, page + 1).getItemStack());
            lore.clear();

            lore.add(messages.getString("GUI.NewObjectLong") + messages.getString("GUI.NewDrawbridge"));
            inv.setItem(3, new GUIItem(NEWDOORMAT, messages.getString("GUI.NewDrawbridge"),
                                       lore, page + 1).getItemStack());
            lore.clear();

            lore.add(messages.getString("GUI.NewObjectLong") + messages.getString("GUI.NewDoor"));
            inv.setItem(4, new GUIItem(NEWDOORMAT, messages.getString("GUI.NewDoor"),
                                       lore, page + 1).getItemStack());
            lore.clear();

            lore.add(messages.getString("GUI.NewObjectLong") + messages.getString("GUI.NewPortcullis"));
            inv.setItem(5, new GUIItem(NEWDOORMAT, messages.getString("GUI.NewPortcullis"),
                                       lore, page + 1).getItemStack());
            lore.clear();

            lore.add(messages.getString("GUI.NewObjectLong") + messages.getString("GUI.NewSlidingDoor"));
            inv.setItem(6, new GUIItem(NEWDOORMAT, messages.getString("GUI.NewSlidingDoor"),
                                       lore, page + 1).getItemStack());
            lore.clear();
        }
        else if (pageType == PageType.DOORINFO)
        {
            lore.add("Expanded menu for door " + door.getName());
            lore.add("This door has ID " + door.getDoorUID());
            lore.add(messages.getString(DoorType.getNameKey(door.getType())));
            inv.setItem(4, new GUIItem(CURRDOORMAT, door.getName() + ": "
                                        + door.getDoorUID(), lore, 1).getItemStack());
        }
        else if (pageType == PageType.CONFIRMATION)
        {
            lore.add("Expanded menu for door " + door.getName());
            lore.add("This door has ID " + door.getDoorUID());
            lore.add(messages.getString(DoorType.getNameKey(door.getType())));
            inv.setItem(4, new GUIItem(CURRDOORMAT, door.getName() + ": "
                                        + door.getDoorUID(), lore, 1).getItemStack());
        }
        lore.clear();
    }

    public void createDoorSubMenu()
    {
        int position = 9;
        for (DoorAttribute attr : DoorType.getAttributes(door.getType()))
        {
            GUIItem item = getGUIItem(door, attr);
            if (item != null)
                inv.setItem(position++, item.getItemStack());
        }
    }

    // Fill the entire menu with NO's, but put a single "yes" in the middle.
    public void fillConfirmationMenu()
    {
        int mid = (CHESTSIZE - 9) / 2 + 4;
        for (int idx = 9; idx < CHESTSIZE; ++idx)
        {
            ArrayList<String> lore = new ArrayList<String>();
            if (idx == mid) // Middle block.
            {
                lore.add(messages.getString("GUI.ConfirmDelete"));
                inv.setItem(idx, new GUIItem(NOTCONFIRMMAT, messages.getString("GUI.Confirm"), lore, 1, CONFIRMDATA).getItemStack());
            }
            else
            {
                lore.add(messages.getString("GUI.NotConfirm"));
                inv.setItem(idx, new GUIItem(CONFIRMMAT, messages.getString("GUI.No"), lore, 1, NOTCONFIRMDATA).getItemStack());
            }
        }
    }

    public void fillInventory(Player player)
    {
        createHeader();
        if (pageType == PageType.DOORLIST)
            for (int idx = 9; idx - 9 != doors.size() && idx < CHESTSIZE; ++idx)
            {
                int realIdx  = idx - 9;
                try
                {
                    DoorType doorType = doors.get(realIdx).getType();
                    ArrayList<String> lore = new ArrayList<String>();
                    lore.add(messages.getString("GUI.DoorHasID") + doors.get(realIdx).getDoorUID());
                    lore.add(messages.getString(DoorType.getNameKey(doorType)));
                    inv.setItem(idx, new GUIItem(DOORTYPES[DoorType.getValue(doorType)], doors.get(realIdx).getName(), lore, 1).getItemStack());
                }
                catch (Exception e)
                {
//                    Util.broadcastMessage("Failed to put door \"" + doors.get(realIdx) + "\" (" + doors.get(realIdx) +
//                                          ") in the GUI. Type = " + doors.get(realIdx).getType());
                    // No need to catch it. This is thrown because newer (dev) versions have more door types.
                }
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

    public static int getChestSize() {  return CHESTSIZE;  }


    private GUIItem getGUIItem(Door door, DoorAttribute atr)
    {
        // If the permission level is higher than the
        if (door.getPermission() > DoorAttribute.getPermissionLevel(atr))
            return null;

        ArrayList<String> lore = new ArrayList<String>();
        String desc, loreStr;
        GUIItem ret = null;

        switch(atr)
        {
        case LOCK:
            if (door.isLocked())
                ret = new GUIItem(LOCKDOORMAT, messages.getString("GUI.UnlockDoor"),
                            null, 1, UNLOCKEDDATA);
            else
                ret = new GUIItem(UNLOCKDOORMAT, messages.getString("GUI.LockDoor"),
                            null, 1, LOCKEDDATA);
            break;

        case TOGGLE:
            desc = messages.getString("GUI.ToggleDoor");
            lore.add(desc);
            ret = new GUIItem(TOGGLEDOORMAT, desc, lore, 1);
            break;

        case INFO:
            desc = messages.getString("GUI.GetInfo");
            lore.add(desc);
            ret = new GUIItem(INFOMAT, desc, lore, 1);
            break;

        case DELETE:
            desc = messages.getString("GUI.DeleteDoor");
            loreStr = messages.getString("GUI.DeleteDoorLong");
            lore.add(loreStr);
            ret = new GUIItem(DELDOORMAT, desc , lore, 1);
            break;

        case RELOCATEPOWERBLOCK:
            desc = messages.getString("GUI.RelocatePowerBlock");
            loreStr = messages.getString("GUI.RelocatePowerBlockLore");
            lore.add(loreStr);
            ret = new GUIItem(RELOCATEPBMAT, desc, lore, 1);
            break;

        case CHANGETIMER:
            desc = messages.getString("GUI.ChangeTimer");
            loreStr = door.getAutoClose() > -1 ? messages.getString("GUI.ChangeTimerLore") + door.getAutoClose() + "s." :
                messages.getString("GUI.ChangeTimerLoreDisabled");
            lore.add(loreStr);
            int count = door.getAutoClose() < 1 ? 1 : door.getAutoClose();
            ret = new GUIItem(CHANGETIMEMAT, desc, lore, count);
            break;

        case DIRECTION_OPEN:
            desc = messages.getString("GUI.Direction.Name");
            loreStr = messages.getString("GUI.Direction.ThisDoorOpens") + messages.getString(RotateDirection.getNameKey(door.getOpenDir()));
            lore.add(loreStr);
            ret = new GUIItem(SETOPENDIRMAT, desc, lore, 1);
            break;

        case DIRECTION_GO:
            desc = messages.getString("GUI.Direction.Name");
            loreStr = messages.getString("GUI.Direction.ThisDoorGoes") + messages.getString(RotateDirection.getNameKey(door.getOpenDir()));
            lore.add(loreStr);
            ret = new GUIItem(SETOPENDIRMAT, desc, lore, 1);
            break;

        case DIRECTION_NSEW_LOOK:
            desc = messages.getString("GUI.Direction.Name");
            loreStr = messages.getString("GUI.Direction.ThisDoorOpens") + messages.getString(RotateDirection.getNameKey(door.getOpenDir()));
            lore.add(loreStr);
            lore.add(messages.getString("GUI.Direction.Looking") +
                    (door.getType()       == DoorType.DOOR       ? messages.getString(RotateDirection.getNameKey(RotateDirection.DOWN)) :
                     door.getLookingDir() == DoorDirection.NORTH ? messages.getString(RotateDirection.getNameKey(RotateDirection.EAST)) :
                                                                   messages.getString(RotateDirection.getNameKey(RotateDirection.NORTH))));
            ret = new GUIItem(SETOPENDIRMAT, desc, lore, 1);
            break;

        case BLOCKSTOMOVE:
            desc = messages.getString("GUI.BLOCKSTOMOVE.Name");
            if (door.getBlocksToMove() <= 0)
                loreStr = messages.getString("GUI.BLOCKSTOMOVE.Unavailable");
            else
                loreStr = messages.getString("GUI.BLOCKSTOMOVE.Available") + " " + door.getBlocksToMove();
            lore.add(loreStr);
            ret = new GUIItem(SETBTMOVEMAT, desc, lore, 1);
            break;

        case ADDOWNER:
            desc = messages.getString("GUI.ADDOWNER");
            lore.add(desc);
            ret = new GUIItem(ADDOWNERMAT, desc, lore, 1, PLAYERHEADDATA);
            break;

        case REMOVEOWNER:
            desc = messages.getString("GUI.REMOVEOWNER");
            lore.add(desc);
            ret = new GUIItem(REMOVEOWNERMAT, desc, lore, 1, SKULLDATA);
            break;
        }
        return ret;
    }
}