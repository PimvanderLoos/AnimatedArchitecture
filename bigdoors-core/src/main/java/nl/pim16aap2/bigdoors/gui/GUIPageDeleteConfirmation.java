package nl.pim16aap2.bigdoors.gui;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.commands.CommandData;
import nl.pim16aap2.bigdoors.commands.subcommands.SubCommandDelete;
import nl.pim16aap2.bigdoors.doors.DoorType;
import nl.pim16aap2.bigdoors.spigotutil.PageType;
import nl.pim16aap2.bigdoors.util.DoorAttribute;
import nl.pim16aap2.bigdoors.util.messages.Message;
import nl.pim16aap2.bigdoors.util.messages.Messages;

import java.util.ArrayList;
import java.util.List;

public class GUIPageDeleteConfirmation implements IGUIPage
{
    protected final BigDoors plugin;
    protected final GUI gui;
    protected final Messages messages;

    protected GUIPageDeleteConfirmation(final BigDoors plugin, final GUI gui)
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
        if (!plugin.getDatabaseManager()
                   .hasPermissionForAction(gui.getPlayer(), gui.getDoor().getDoorUID(), DoorAttribute.DELETE))
            return;
        ((SubCommandDelete) plugin.getCommand(CommandData.DELETE)).execute(gui.getPlayer(), gui.getDoor());
        gui.removeSelectedDoor();
    }

    protected void fillHeader()
    {
        List<String> lore = new ArrayList<>();
        lore.add(plugin.getMessages().getString(Message.GUI_DESCRIPTION_PREVIOUSPAGE,
                                                Integer.toString(gui.getPage() + 1),
                                                Integer.toString(gui.getPage()),
                                                Integer.toString(gui.getMaxPageCount())));
        gui.addItem(0, new GUIItem(GUI.PAGESWITCHMAT,
                                   plugin.getMessages().getString(Message.GUI_BUTTON_PREVIOUSPAGE), lore,
                                   Math.max(1, gui.getPage())));
        lore.clear();

        lore.add(messages.getString(Message.GUI_BUTTON_INFO));
        lore.add(messages.getString(Message.GUI_DESCRIPTION_DOORID, Long.toString(gui.getDoor().getDoorUID())));
        lore.add(messages.getString(DoorType.getMessage(gui.getDoor().getType())));
        gui.addItem(4,
                    new GUIItem(GUI.CURRDOORMAT, gui.getDoor().getName() + ": " + gui.getDoor().getDoorUID(), lore, 1));
    }

    protected void fillPage()
    {
        int mid = (GUI.CHESTSIZE - 9) / 2 + 4;
        for (int idx = 9; idx < GUI.CHESTSIZE; ++idx)
        {
            List<String> lore = new ArrayList<>();
            if (idx == mid) // Middle block.
            {
                lore.add(messages.getString(Message.GUI_DESCRIPTION_DOOR_DELETE_CONFIRM));
                gui.addItem(idx, new GUIItem(GUI.CONFIRMMAT, messages.getString(Message.GUI_BUTTON_DOOR_DELETE_CONFIRM),
                                             lore, 1));
            }
            else
            {
                lore.add(messages.getString(Message.GUI_DESCRIPTION_DOOR_DELETE_CANCEL));
                gui.addItem(idx,
                            new GUIItem(GUI.NOTCONFIRMMAT, messages.getString(Message.GUI_BUTTON_DOOR_DELETE_CANCEL),
                                        lore, 1));
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
