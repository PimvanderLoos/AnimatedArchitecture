package nl.pim16aap2.bigdoors.gui;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.commands.CommandData;
import nl.pim16aap2.bigdoors.commands.subcommands.SubCommandNew;
import nl.pim16aap2.bigdoors.doors.DoorType;
import nl.pim16aap2.bigdoors.spigotutil.PageType;
import nl.pim16aap2.bigdoors.util.Messages;
import org.bukkit.entity.Player;

import java.util.ArrayList;

public class GUIPageDoorCreation implements IGUIPage
{
    protected final BigDoors plugin;
    protected final GUI gui;
    protected final Messages messages;
    protected final SubCommandNew subCommand;

    protected GUIPageDoorCreation(final BigDoors plugin, final GUI gui)
    {
        this.plugin = plugin;
        this.gui = gui;
        messages = plugin.getMessages();
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
        ArrayList<String> lore = new ArrayList<>();
        lore.add(messages.getString("GUI.ToPage") + gui.getPage() + messages.getString("GUI.OutOf")
                         + gui.getMaxPageCount());
        gui.addItem(0, new GUIItem(GUI.PAGESWITCHMAT, messages.getString("GUI.PreviousPage"), lore,
                                   Math.max(1, gui.getPage())));
    }

    private void fillPage()
    {
        int position = 9;
        for (DoorType type : DoorType.values())
            if (DoorType.isEnabled(type) && SubCommandNew.hasCreationPermission(gui.getPlayer(), type))
                gui.addItem(position++,
                            new GUIItem(GUI.NEWDOORMAT,
                                        messages.getString("GUI.DoorInitiation") + " "
                                                + messages.getString("GENERAL.DOORTYPE." + DoorType.getCodeName(type)),
                                        null, 1, type));

    }

    private void startCreationProcess(Player player, DoorType type)
    {
        player.closeInventory();
        subCommand.execute(player, null, type);
    }
}
