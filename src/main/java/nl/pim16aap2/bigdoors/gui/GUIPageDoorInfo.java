package nl.pim16aap2.bigdoors.gui;

import java.util.ArrayList;

import org.bukkit.entity.Player;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.Door;
import nl.pim16aap2.bigdoors.commands.subcommands.SubCommandInfo;
import nl.pim16aap2.bigdoors.commands.subcommands.SubCommandToggle;
import nl.pim16aap2.bigdoors.util.DoorAttribute;
import nl.pim16aap2.bigdoors.util.DoorType;
import nl.pim16aap2.bigdoors.util.Messages;
import nl.pim16aap2.bigdoors.util.MyBlockFace;
import nl.pim16aap2.bigdoors.util.PageType;
import nl.pim16aap2.bigdoors.util.RotateDirection;

public class GUIPageDoorInfo implements IGUIPage
{
    protected final BigDoors plugin;
    protected final GUI gui;
    protected final Messages messages;

    public GUIPageDoorInfo(final BigDoors plugin, final GUI gui)
    {
        this.plugin = plugin;
        this.gui = gui;
        messages = plugin.getMessages();
        refresh();
    }

    @Override
    public PageType getPageType()
    {
        return PageType.DOORINFO;
    }

    @Override
    public void handleInput(int interactionIDX)
    {
        if (interactionIDX == 0)
        {
            gui.setGUIPage(new GUIPageDoorList(plugin, gui));
            return;
        }
        // Only button in the header is the back button.
        if (interactionIDX < 9)
            return;

        if (!plugin.getCommander().hasPermissionForAction(gui.getPlayer(), gui.getDoor().getDoorUID(), gui.getItem(interactionIDX).getDoorAttribute()))
        {
            refresh();
            return;
        }

        Door door = gui.getDoor();
        Player player = gui.getPlayer();

        switch(gui.getItem(interactionIDX).getDoorAttribute())
        {
        case LOCK:
            door.setLock(!door.isLocked());
            plugin.getCommander().setLock(door.getDoorUID(), door.isLocked());
            refresh();
            break;
        case TOGGLE:
            ((SubCommandToggle) plugin.getCommand("bigdoors", "toggle")).execute(player, door);
            break;
        case INFO:
            ((SubCommandInfo) plugin.getCommand("bigdoors", "info")).execute(player, door);
            break;
        case DELETE:
            gui.setGUIPage(new GUIPageDeleteConfirmation(plugin, gui));
            break;
        case RELOCATEPOWERBLOCK:
            plugin.getCommander().startPowerBlockRelocator(player, door);
            gui.close();
            break;
        case DIRECTION_OPEN:
        case DIRECTION_GO:
        case DIRECTION_NSEW_LOOK:
            changeOpenDir(player, door);
            break;
        case CHANGETIMER:
            plugin.getCommander().startTimerSetter(player, door);
            gui.close();
            break;
        case BLOCKSTOMOVE:

            plugin.getCommander().startBlocksToMoveSetter(player, door);
            gui.close();
            break;
        case ADDOWNER:
            plugin.getCommander().startAddOwner(player, door);
            gui.close();
            break;
        case REMOVEOWNER:
            switchToRemoveOwner();
        }
    }

    protected void fillHeader()
    {
        ArrayList<String> lore = new ArrayList<>();
        gui.addItem(0, new GUIItem(GUI.PAGESWITCHMAT, messages.getString("GUI.PreviousPage"), lore, gui.getPage() + 1));
        lore.clear();

        lore.add(messages.getString("GUI.MoreInfoMenu") + gui.getDoor().getName());
        lore.add("This door has ID " + gui.getDoor().getDoorUID());
        lore.add(messages.getString(DoorType.getNameKey(gui.getDoor().getType())));
        gui.addItem(4, new GUIItem(GUI.CURRDOORMAT, gui.getDoor().getName() + ": " + gui.getDoor().getDoorUID(), lore, 1));
    }

    protected void fillPage()
    {
        int position = 9;
        for (DoorAttribute attr : DoorType.getAttributes(gui.getDoor().getType()))
        {
            GUIItem item = getGUIItem(gui.getDoor(), attr);
            if (item != null)
                gui.addItem(position++, item);
        }
    }

    @Override
    public void refresh()
    {
        fillHeader();
        fillPage();
    }

    private void switchToRemoveOwner()
    {
//        // Text based method
//        plugin.getCommander().startRemoveOwner(gui.getPlayer(), gui.getDoor());
//        gui.close();

        // GUI based method
        gui.setGUIPage(new GUIPageRemoveOwner(plugin, gui));
    }

    // Changes the opening direction for a door.
    private void changeOpenDir(Player player, Door door)
    {
        RotateDirection curOpenDir = door.getOpenDir();
        RotateDirection newOpenDir;

        // TODO: Use DoorAttribute here.
        if (door.getType() == DoorType.SLIDINGDOOR)
            newOpenDir = curOpenDir == RotateDirection.NONE  ? RotateDirection.NORTH :
                         curOpenDir == RotateDirection.NORTH ? RotateDirection.EAST  :
                         curOpenDir == RotateDirection.EAST  ? RotateDirection.SOUTH :
                         curOpenDir == RotateDirection.SOUTH ? RotateDirection.WEST  :
                                                               RotateDirection.NONE;
        else if (door.getType() == DoorType.ELEVATOR)
            newOpenDir = curOpenDir == RotateDirection.UP ? RotateDirection.DOWN : RotateDirection.UP;
        else
            newOpenDir = curOpenDir == RotateDirection.NONE      ? RotateDirection.CLOCKWISE :
                         curOpenDir == RotateDirection.CLOCKWISE ? RotateDirection.COUNTERCLOCKWISE :
                                                                   RotateDirection.NONE;

        plugin.getCommander().updateDoorOpenDirection(door.getDoorUID(), newOpenDir);
        int idx = gui.indexOfDoor(door);
        gui.getDoor(idx).setOpenDir(newOpenDir);
        gui.setDoor(gui.getDoor(idx));
        refresh();
    }

    private GUIItem getGUIItem(Door door, DoorAttribute atr)
    {
        // If the permission level is higher than the
        if (door.getPermission() > DoorAttribute.getPermissionLevel(atr))
            return null;

        ArrayList<String> lore = new ArrayList<>();
        String desc, loreStr;
        GUIItem ret = null;

        switch(atr)
        {
        case LOCK:
            if (door.isLocked())
                ret = new GUIItem(GUI.LOCKDOORMAT, messages.getString("GUI.UnlockDoor"),
                            null, 1);
            else
                ret = new GUIItem(GUI.UNLOCKDOORMAT, messages.getString("GUI.LockDoor"),
                            null, 1);
            break;

        case TOGGLE:
            desc = messages.getString("GUI.ToggleDoor");
            lore.add(desc);
            ret = new GUIItem(GUI.TOGGLEDOORMAT, desc, lore, 1);
            break;

        case INFO:
            desc = messages.getString("GUI.GetInfo");
            lore.add(desc);
            ret = new GUIItem(GUI.INFOMAT, desc, lore, 1);
            break;

        case DELETE:
            desc = messages.getString("GUI.DeleteDoor");
            loreStr = messages.getString("GUI.DeleteDoorLong");
            lore.add(loreStr);
            ret = new GUIItem(GUI.DELDOORMAT, desc , lore, 1);
            break;

        case RELOCATEPOWERBLOCK:
            desc = messages.getString("GUI.RelocatePowerBlock");
            loreStr = messages.getString("GUI.RelocatePowerBlockLore");
            lore.add(loreStr);
            ret = new GUIItem(GUI.RELOCATEPBMAT, desc, lore, 1);
            break;

        case CHANGETIMER:
            desc = messages.getString("GUI.ChangeTimer");
            loreStr = door.getAutoClose() > -1 ? messages.getString("GUI.ChangeTimerLore") + door.getAutoClose() + "s." :
                messages.getString("GUI.ChangeTimerLoreDisabled");
            lore.add(loreStr);
            int count = door.getAutoClose() < 1 ? 1 : door.getAutoClose();
            ret = new GUIItem(GUI.CHANGETIMEMAT, desc, lore, count);
            break;

        case DIRECTION_OPEN:
            desc = messages.getString("GUI.Direction.Name");
            loreStr = messages.getString("GUI.Direction.ThisDoorOpens") + messages.getString(RotateDirection.getNameKey(door.getOpenDir()));
            lore.add(loreStr);
            ret = new GUIItem(GUI.SETOPENDIRMAT, desc, lore, 1);
            break;

        case DIRECTION_GO:
            desc = messages.getString("GUI.Direction.Name");
            loreStr = messages.getString("GUI.Direction.ThisDoorGoes") + messages.getString(RotateDirection.getNameKey(door.getOpenDir()));
            lore.add(loreStr);
            ret = new GUIItem(GUI.SETOPENDIRMAT, desc, lore, 1);
            break;

        case DIRECTION_NSEW_LOOK:
            desc = messages.getString("GUI.Direction.Name");
            loreStr = messages.getString("GUI.Direction.ThisDoorOpens") + messages.getString(RotateDirection.getNameKey(door.getOpenDir()));
            lore.add(loreStr);
            lore.add(messages.getString("GUI.Direction.Looking") +
                    (door.getType()       == DoorType.DOOR     ? messages.getString(RotateDirection.getNameKey(RotateDirection.DOWN)) :
                     door.getLookingDir() == MyBlockFace.NORTH ? messages.getString(RotateDirection.getNameKey(RotateDirection.EAST)) :
                                                                 messages.getString(RotateDirection.getNameKey(RotateDirection.NORTH))));
            ret = new GUIItem(GUI.SETOPENDIRMAT, desc, lore, 1);
            break;

        case BLOCKSTOMOVE:
            desc = messages.getString("GUI.BLOCKSTOMOVE.Name");
            if (door.getBlocksToMove() <= 0)
                loreStr = messages.getString("GUI.BLOCKSTOMOVE.Unavailable");
            else
                loreStr = messages.getString("GUI.BLOCKSTOMOVE.Available") + " " + door.getBlocksToMove();
            lore.add(loreStr);
            ret = new GUIItem(GUI.SETBTMOVEMAT, desc, lore, 1);
            break;

        case ADDOWNER:
            desc = messages.getString("GUI.ADDOWNER");
            lore.add(desc);
            ret = new GUIItem(GUI.ADDOWNERMAT, desc, lore, 1);
            break;

        case REMOVEOWNER:
            desc = messages.getString("GUI.REMOVEOWNER");
            lore.add(desc);
            ret = new GUIItem(GUI.REMOVEOWNERMAT, desc, lore, 1);
            break;
        }
        if (ret != null)
            ret.setDoorAttribute(atr);
        return ret;
    }
}
