package nl.pim16aap2.bigdoors.gui;

import java.util.ArrayList;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.commands.subcommands.SubCommandDelete;
import nl.pim16aap2.bigdoors.util.DoorAttribute;
import nl.pim16aap2.bigdoors.util.DoorType;
import nl.pim16aap2.bigdoors.util.Messages;
import nl.pim16aap2.bigdoors.util.PageType;

public class GUIPageDeleteConfirmation implements IGUIPage
{
    protected final BigDoors plugin;
    protected final GUI gui;
    protected final Messages messages;

    public GUIPageDeleteConfirmation(final BigDoors plugin, final GUI gui)
    {
        this.plugin = plugin;
        this.gui = gui;
        messages = plugin.getMessages();
        refresh();
    }

    @Override
    public PageType getPageType()
    {
        return PageType.CONFIRMATION;
    }

    @Override
    public void handleInput(int interactionIDX)
    {
        if (!gui.isStillOwner())
            return;

        int mid = (GUI.CHESTSIZE - 9) / 2 + 4;

        if (interactionIDX == mid)
        {
            deleteDoor();
            gui.setGUIPage(new GUIPageDoorList(plugin, gui));
        }
        else
            gui.setGUIPage(new GUIPageDoorInfo(plugin, gui));
    }

    private void deleteDoor()
    {
        if (!plugin.getCommander().hasPermissionForAction(gui.getPlayer(), gui.getDoor().getDoorUID(), DoorAttribute.DELETE))
            return;
        ((SubCommandDelete) plugin.getCommand("bigdoors", "delete")).execute(gui.getPlayer(), gui.getDoor());
        gui.removeSelectedDoor();
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
        int mid = (GUI.CHESTSIZE - 9) / 2 + 4;
        for (int idx = 9; idx < GUI.CHESTSIZE; ++idx)
        {
            ArrayList<String> lore = new ArrayList<>();
            if (idx == mid) // Middle block.
            {
                lore.add(messages.getString("GUI.ConfirmDelete"));
                gui.addItem(idx, new GUIItem(GUI.CONFIRMMAT, messages.getString("GUI.Confirm"), lore, 1));
            }
            else
            {
                lore.add(messages.getString("GUI.NotConfirm"));
                gui.addItem(idx, new GUIItem(GUI.NOTCONFIRMMAT, messages.getString("GUI.No"), lore, 1));
            }
        }
    }

    @Override
    public void refresh()
    {
        fillHeader();
        fillPage();
    }
}
