package nl.pim16aap2.bigdoors.gui;

import java.util.ArrayList;

import org.bukkit.entity.Player;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.Door;
import nl.pim16aap2.bigdoors.commands.CommandData;
import nl.pim16aap2.bigdoors.commands.subcommands.SubCommandNew;
import nl.pim16aap2.bigdoors.gui.GUI.SortType;
import nl.pim16aap2.bigdoors.util.DoorType;
import nl.pim16aap2.bigdoors.util.Messages;
import nl.pim16aap2.bigdoors.util.PageType;
import nl.pim16aap2.bigdoors.util.Util;

public class GUIPageDoorList implements IGUIPage
{
    protected final BigDoors plugin;
    protected final GUI gui;
    protected final Messages messages;

    public GUIPageDoorList(final BigDoors plugin, final GUI gui)
    {
        this.plugin = plugin;
        this.gui = gui;
        messages = plugin.getMessages();
        refresh();
    }

    @Override
    public PageType getPageType()
    {
        return PageType.DOORLIST;
    }

    @Override
    public void handleInput(int interactionIDX)
    {
        boolean header = Util.between(interactionIDX, 0, 8);
        if (interactionIDX == 0)
        {
            gui.setPage(gui.getPage() - 1);
            gui.update();
        }
        else if (interactionIDX == 1)
        {
            gui.setNextSortType();
            gui.update();
        }
        else if (interactionIDX == 8)
        {
            gui.setPage(gui.getPage() + 1);
            gui.update();
        }
        else if (header)
        {
            String itemName = gui.getItem(interactionIDX).getName();
            if (itemName.equals(messages.getString("GUI.NewDoor")))
                startCreationProcess(gui.getPlayer(), DoorType.DOOR);
            else if (itemName.equals(messages.getString("GUI.NewPortcullis")))
                startCreationProcess(gui.getPlayer(), DoorType.PORTCULLIS);
            else if (itemName.equals(messages.getString("GUI.NewDrawbridge")))
                startCreationProcess(gui.getPlayer(), DoorType.DRAWBRIDGE);
            else if (itemName.equals(messages.getString("GUI.NewElevator")))
                startCreationProcess(gui.getPlayer(), DoorType.ELEVATOR);
            else if (itemName.equals(messages.getString("GUI.NewSlidingDoor")))
                startCreationProcess(gui.getPlayer(), DoorType.SLIDINGDOOR);
        }
        else
        {
            gui.setDoor(gui.getItem(interactionIDX).getDoor());
            if (gui.getDoor() == null)
            {
                Util.messagePlayer(gui.getPlayer(), "An unexpected error occurred while trying to open a sub-menu for a door! Try again!");
                gui.close();
                return;
            }
            if (gui.isStillOwner())
                gui.setGUIPage(new GUIPageDoorInfo(plugin, gui));
        }
    }

    protected void addCreationBook(DoorType type, int idx, String message)
    {
        if (SubCommandNew.hasCreationPermission(gui.getPlayer(), type))
        {
            ArrayList<String> lore = new ArrayList<>();
            lore.add(messages.getString("GUI.NewObjectLong") + messages.getString(message));
            gui.addItem(idx, new GUIItem(GUI.NEWDOORMAT, messages.getString(message), lore, gui.getPage() + 1));
        }
    }

    protected void fillHeader()
    {
        int page = gui.getPage();
        ArrayList<String> lore = new ArrayList<>();
        if (page != 0)
        {
            lore.add(messages.getString("GUI.ToPage") + page + messages.getString("GUI.OutOf") + gui.getMaxPageCount());
            gui.addItem(0, new GUIItem(GUI.PAGESWITCHMAT, messages.getString("GUI.PreviousPage"), lore, page));
            lore.clear();
        }

        lore.add(messages.getString(SortType.getName(gui.getSortType())));
        gui.addItem(1, new GUIItem(GUI.TOGGLEDOORMAT, messages.getString("GUI.SORT.Change"), lore, 1));
        lore.clear();

        addCreationBook(DoorType.ELEVATOR,    2, "GUI.NewElevator"   );
        addCreationBook(DoorType.DRAWBRIDGE,  3, "GUI.NewDrawbridge" );
        addCreationBook(DoorType.DOOR,        4, "GUI.NewDoor"       );
        addCreationBook(DoorType.PORTCULLIS,  5, "GUI.NewPortcullis" );
        addCreationBook(DoorType.SLIDINGDOOR, 6, "GUI.NewSlidingDoor");

        if ((page + 1) < gui.getMaxPageCount())
        {
            lore.add(messages.getString("GUI.ToPage") + (page + 2) + messages.getString("GUI.OutOf") + gui.getMaxPageCount());
            gui.addItem(8, new GUIItem(GUI.PAGESWITCHMAT, messages.getString("GUI.NextPage"), lore, page + 2));
            lore.clear();
        }
    }

    protected void fillPage()
    {
        int offset = gui.getPage() * (GUI.CHESTSIZE - 9);
        int endCount = Math.min((GUI.CHESTSIZE - 9), (gui.getDoorsSize() - offset));
        ArrayList<String> lore = new ArrayList<>();
        for (int idx = 0; idx < endCount; ++idx)
        {
            Door currentDoor = gui.getDoor(offset + idx);
            DoorType doorType = currentDoor.getType();
            lore.add(messages.getString("GUI.DoorHasID") + currentDoor.getDoorUID());
            lore.add(messages.getString(DoorType.getNameKey(doorType)));
            GUIItem item = new GUIItem(GUI.DOORTYPES[DoorType.getValue(doorType)], currentDoor.getName(), lore, 1);
            item.setDoor(currentDoor);
            gui.addItem(idx + 9, item);
            lore.clear();
        }
    }

    private void startCreationProcess(Player player, DoorType type)
    {
        player.closeInventory();
        ((SubCommandNew) plugin.getCommand(CommandData.NEW)).execute(player, null, type);
    }

    @Override
    public void refresh()
    {
        fillHeader();
        fillPage();
    }
}
