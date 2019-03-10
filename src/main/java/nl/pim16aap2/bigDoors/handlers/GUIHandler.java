package nl.pim16aap2.bigDoors.handlers;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import nl.pim16aap2.bigDoors.BigDoors;
import nl.pim16aap2.bigDoors.Door;
import nl.pim16aap2.bigDoors.GUI.GUIPage;
import nl.pim16aap2.bigDoors.util.DoorType;
import nl.pim16aap2.bigDoors.util.Messages;
import nl.pim16aap2.bigDoors.util.PageType;
import nl.pim16aap2.bigDoors.util.RotateDirection;

public class GUIHandler implements Listener
{
    private final Messages messages;
    private final BigDoors   plugin;
    
    public GUIHandler(BigDoors plugin)
    {
        this.plugin   = plugin;
        this.messages = plugin.getMessages();
    }
    
    // Set the GUI page for the player to a specified page number on a specified pageType for a specified doorUID.
    private void setPage(Player player, Inventory inv, int page, PageType pageType, long doorUID, int pageCount)
    {
        inv.clear();
        new GUIPage(plugin, player, page, pageType, doorUID, pageCount);
    }
    
    private void startCreationProcess(Player player, DoorType type)
    {
        player.closeInventory();
        plugin.getCommandHandler().startCreator(player, null, type);
    }
    
    // Changes the opening direction for a door.
    private void changeOpenDir(Player player, Door door, Inventory inv)
    {
        RotateDirection curOpenDir = door.getOpenDir();
        RotateDirection newOpenDir;
        if (door.getType() == DoorType.SLIDINGDOOR)
        {
            newOpenDir = curOpenDir == RotateDirection.NONE  ? RotateDirection.NORTH :
                         curOpenDir == RotateDirection.NORTH ? RotateDirection.EAST  :
                         curOpenDir == RotateDirection.EAST  ? RotateDirection.SOUTH :
                         curOpenDir == RotateDirection.SOUTH ? RotateDirection.WEST  :
                                                               RotateDirection.NONE;
        }
        else
        {
            newOpenDir = curOpenDir == RotateDirection.NONE      ? RotateDirection.CLOCKWISE :
                         curOpenDir == RotateDirection.CLOCKWISE ? RotateDirection.COUNTERCLOCKWISE :
                                                                   RotateDirection.NONE;
        }
        plugin.getCommander().updateDoorOpenDirection(door.getDoorUID(), newOpenDir);
        setPage(player, inv, getPreviousPage(inv) + 1, PageType.DOORINFO, door.getDoorUID(), getPageCount(inv));
    }
    
    // Retrieve the int from the end of a string.
    private long finalLongFromString(String str)
    {
        final Pattern lastIntPattern = Pattern.compile("[^0-9]+([0-9]+)$");
        Matcher matcher = lastIntPattern.matcher(str);
        if (matcher.find()) 
        {
            String someNumberStr = matcher.group(1);
            return Long.parseLong(someNumberStr);
        }
        return -1;
    }
    
    // Retrieve the long from the end of a string.
    private long firstLongFromString(String str)
    {
        Matcher matcher = Pattern.compile("\\d+").matcher(str);
        if (matcher.find())
            return Long.valueOf(matcher.group());
        return -1;
    }
    
    // Open a new menu dedicated to a door listed in the itemstack's lore.
    private void openDoorSubMenu(Player player, Inventory inv, ItemStack item)
    {
        if (player == null || inv == null || item == null)
            return;
        Door door = getDoor(item);
        if (door == null)
            return;
        long doorUID = door.getDoorUID();
        openDoorSubMenu(player, inv, doorUID);
    }
    
    // Open a new menu dedicated to this door (passed as doorUID).
    private void openDoorSubMenu(Player player, Inventory inv, long doorUID)
    {
        if (player == null || inv == null)
            return;
        // Set the page to the submenu for this door. Provide previous page number as page number, so the menu remembers where to go back to.
        setPage(player, inv, getCurrentPageNum(inv), PageType.DOORINFO, doorUID, getPageCount(inv));
    }
    
    private int getPageCount(Inventory inv)
    {
        return (int) (inv.getItem(0) != null ? finalLongFromString(inv.getItem(0).getItemMeta().getLore().get(inv.getItem(0).getItemMeta().getLore().size() - 1)) : 
                      inv.getItem(8) != null ? finalLongFromString(inv.getItem(8).getItemMeta().getLore().get(inv.getItem(8).getItemMeta().getLore().size() - 1)) : -1);
    }
    
    private int getPreviousPage(Inventory inv)
    {
        return (int) (inv.getItem(0) != null ? firstLongFromString(inv.getItem(0).getItemMeta().getLore().get(inv.getItem(0).getItemMeta().getLore().size() - 1)) - 1: 
                      inv.getItem(8) != null ? firstLongFromString(inv.getItem(8).getItemMeta().getLore().get(inv.getItem(8).getItemMeta().getLore().size() - 1)) - 3 : 0);
    }
    
    private int getCurrentPageNum(Inventory inv)
    {
        if (inv.getName() == messages.getString("GUI.ConfirmMenu"))
            return getPreviousPage(inv) + 1;
        return inv.getItem(4) != null ? inv.getItem(4).getAmount() : 0;
    }
    
    private void toggleLock(Player player, Inventory inv)
    {
        Door door = getDoor(inv.getItem(4));
        long doorID = door.getDoorUID();
        door.setLock(true);
        plugin.getCommandHandler().lockDoorCommand(player, plugin.getCommander().getDoor(doorID));
        openDoorSubMenu(player, inv, getDoor(inv.getItem(4)).getDoorUID());
    }
    
    private void toggleDoor(Player player, Inventory inv)
    {
        plugin.getCommandHandler().openDoorCommand(player, getDoor(inv.getItem(4)));
    }
    
    private Door getDoor(ItemStack item)
    {
        List<String> lore = item.getItemMeta().getLore();
        
        long doorUID = -1;
        long foundID = -1;

        // Check the end of every line, to give myself some freedom in regards to formatting.
        for (String str : lore)
        {
            foundID = finalLongFromString(str);
            doorUID = foundID != -1 ? foundID : doorUID;
        }
            
        if (doorUID == -1)
        {
            plugin.getMyLogger().logMessage("149: Failed to retrieve door \"" + lore.get(0) + "\"!", true, false);
            return null;
        }
                
        Door door = plugin.getCommander().getDoor(doorUID);
        return door;
    }
    
    private void deleteDoor(Player player, Inventory inv)
    {
        long doorUID = getDoor(inv.getItem(4)).getDoorUID();
        plugin.getCommandHandler().delDoor(player, doorUID);
        setPage(player, inv, 0, PageType.DOORLIST, -1, getPageCount(inv));
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
        PageType pageType = invName.equals(messages.getString("GUI.Name"))        ? PageType.DOORLIST : 
                            invName.equals(messages.getString("GUI.SubName"))     ? PageType.DOORINFO :
                            invName.equals(messages.getString("GUI.ConfirmMenu")) ? PageType.CONFIRMATION : PageType.NOTBIGDOORS;
                
        if (pageType == PageType.NOTBIGDOORS)
            return;

        event.setCancelled(true);

        Player player = (Player) event.getWhoClicked();
        ItemStack clickedItem = event.getCurrentItem();

        if (clickedItem == null || !clickedItem.hasItemMeta())
            return;

        ItemMeta meta   = clickedItem.getItemMeta();
        if (!meta.hasDisplayName())
            return;
        Inventory inv   = event.getInventory();
        int slot        = event.getRawSlot();
        String itemName = meta.getDisplayName();

        if (pageType == PageType.CONFIRMATION)
        {
            if (itemName.equals(messages.getString("GUI.Confirm")))
                deleteDoor(player, inv);
            else
                openDoorSubMenu(player, inv, inv.getItem(4));
        }
        else if (pageType == PageType.DOORINFO)
        {
            Door door = getDoor(inv.getItem(4));
            if (itemName.equals(messages.getString("GUI.LockDoor")) || itemName.equals(messages.getString("GUI.UnlockDoor")))
                toggleLock(player, inv);
            else if (itemName.equals(messages.getString("GUI.ToggleDoor")))
                toggleDoor(player, inv);
            else if (itemName.equals(messages.getString("GUI.GetInfo")))
                plugin.getCommandHandler().listDoorInfo(player, door);
            else if (itemName.equals(messages.getString("GUI.DeleteDoor")))
                setPage(player, inv, getPreviousPage(inv) + 1, PageType.CONFIRMATION, door.getDoorUID(), getPageCount(inv));
            else if (itemName.equals(messages.getString("GUI.RelocatePowerBlock")))
            {
                player.closeInventory();
                plugin.getCommandHandler().startPowerBlockRelocator(player, door.getDoorUID());
            }
            else if (itemName.equals(messages.getString("GUI.Direction.Name")))
                this.changeOpenDir(player, door, inv);
            else if (itemName.equals(messages.getString("GUI.ChangeTimer")))
            {
                plugin.getCommandHandler().startTimerSetter(player, door.getDoorUID());
                player.closeInventory();
            }
            else if (itemName.equals(messages.getString("GUI.BLOCKSTOMOVE.Name")))
            {
                plugin.getCommandHandler().startBlocksToMoveSetter(player, door.getDoorUID());
                player.closeInventory();
            }
            return;
        }
        else if (itemName.equals(messages.getString("GUI.NewDoor")))
            startCreationProcess(player, DoorType.DOOR);
        else if (itemName.equals(messages.getString("GUI.NewPortcullis")))
            startCreationProcess(player, DoorType.PORTCULLIS);
        else if (itemName.equals(messages.getString("GUI.NewDrawbridge")))
            startCreationProcess(player, DoorType.DRAWBRIDGE);
        else if (itemName.equals(messages.getString("GUI.NewElevator")))
            startCreationProcess(player, DoorType.ELEVATOR);
        else if (itemName.equals(messages.getString("GUI.NewSlidingDoor")))
            startCreationProcess(player, DoorType.SLIDINGDOOR);
        else if (itemName.equals(messages.getString("GUI.NextPage")))
            setPage(player, inv, inv.getItem(8).getAmount() - 1, PageType.DOORLIST, -1, getPageCount(inv));
        else if (itemName.equals(messages.getString("GUI.PreviousPage")))
            setPage(player, inv, getPreviousPage(inv), PageType.DOORLIST, -1, getPageCount(inv));
        else if (slot > 8)
            openDoorSubMenu(player, inv, inv.getItem(slot));
    }
}
