package nl.pim16aap2.bigdoors.gui;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.commands.CommandData;
import nl.pim16aap2.bigdoors.commands.subcommands.SubCommandNew;
import nl.pim16aap2.bigdoors.doors.DoorType;
import nl.pim16aap2.bigdoors.spigotutil.PageType;
import nl.pim16aap2.bigdoors.util.messages.Message;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class GUIPageDoorCreation implements IGUIPage
{
    protected final BigDoors plugin;
    protected final GUI gui;
    protected final SubCommandNew subCommand;

    protected GUIPageDoorCreation(final BigDoors plugin, final GUI gui)
    {
        this.plugin = plugin;
        this.gui = gui;
        subCommand = (SubCommandNew) plugin.getCommand(CommandData.NEW);
        refresh();
    }

    @Override
    public PageType getPageType()
    {
        return PageType.DOORCREATION;
    }

    @Override
    public void handleInput(int interactionIDX)
    {
        if (interactionIDX == 0)
        {
            gui.setGUIPage(new GUIPageDoorList(plugin, gui));
            return;
        }
        GUIItem item = gui.getItem(interactionIDX);
        if (item == null)
            return;

        if (!(item.getSpecialValue() instanceof DoorType))
        {
            plugin.getPLogger().warn("Something went wrong constructing the selected GUIItem at " + interactionIDX
                                             + ":\n" + item.toString());
            return;
        }
        startCreationProcess(gui.getPlayer(), (DoorType) item.getSpecialValue());
    }

    @Override
    public void refresh()
    {
        fillHeader();
        fillPage();
    }

    private void fillHeader()
    {
        List<String> lore = new ArrayList<>();
        lore.add(plugin.getMessages().getString(Message.GUI_DESCRIPTION_PREVIOUSPAGE,
                                                Integer.toString(gui.getPage() + 1),
                                                Integer.toString(gui.getPage()),
                                                Integer.toString(gui.getMaxPageCount())));
        gui.addItem(0, new GUIItem(GUI.PAGESWITCHMAT,
                                   plugin.getMessages().getString(Message.GUI_BUTTON_PREVIOUSPAGE), lore,
                                   Math.max(1, gui.getPage())));
    }

    private void fillPage()
    {
        int position = 9;
        for (DoorType type : DoorType.values())
            if (DoorType.isEnabled(type) && SubCommandNew.hasCreationPermission(gui.getPlayer(), type))
            {
                String initMessage = plugin.getMessages().getString(Message.GUI_DESCRIPTION_INITIATION,
                                                                    plugin.getMessages()
                                                                          .getString(DoorType.getMessage(type)));
                gui.addItem(position++,
                            new GUIItem(GUI.NEWDOORMAT, initMessage, null, 1, type));
            }

    }

    private void startCreationProcess(Player player, DoorType type)
    {
        player.closeInventory();
        subCommand.execute(player, null, type);
    }
}
