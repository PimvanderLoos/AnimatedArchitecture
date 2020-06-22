package nl.pim16aap2.bigdoors.spigot.gui;

import nl.pim16aap2.bigdoors.doors.EDoorType;
import nl.pim16aap2.bigdoors.spigot.BigDoorsSpigot;
import nl.pim16aap2.bigdoors.spigot.commands.CommandData;
import nl.pim16aap2.bigdoors.spigot.commands.subcommands.SubCommandNew;
import nl.pim16aap2.bigdoors.spigot.util.PageType;
import nl.pim16aap2.bigdoors.spigot.util.SpigotAdapter;
import nl.pim16aap2.bigdoors.util.messages.Message;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class GUIPageDoorCreation implements IGUIPage
{
    protected final BigDoorsSpigot plugin;
    protected final GUI gui;
    protected final SubCommandNew subCommand;

    GUIPageDoorCreation(final @NotNull BigDoorsSpigot plugin, final @NotNull GUI gui)
    {
        this.plugin = plugin;
        this.gui = gui;
        subCommand = (SubCommandNew) plugin.getCommand(CommandData.NEW);
        refresh();
    }

    /** {@inheritDoc} */
    @Override
    public void kill()
    {

    }

    @Override
    @NotNull
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

        if (!(item.getSpecialValue() instanceof EDoorType))
        {
            plugin.getPLogger().warn("Something went wrong constructing the selected GUIItem at " + interactionIDX
                                         + ":\n" + item.toString());
            return;
        }
        startCreationProcess(SpigotAdapter.getBukkitPlayer(gui.getGuiHolder()), (EDoorType) item.getSpecialValue());
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
                                                Integer.toString(gui.getPage() + 2),
                                                Integer.toString(gui.getPage() + 1),
                                                Integer.toString(gui.getMaxPageCount())));
        gui.addItem(0, new GUIItem(GUI.PAGESWITCHMAT,
                                   plugin.getMessages().getString(Message.GUI_BUTTON_PREVIOUSPAGE), lore,
                                   Math.max(1, gui.getPage())));
    }

    private void fillPage()
    {
        int position = 9;
        for (EDoorType type : EDoorType.cachedValues())
            if (EDoorType.isEnabled(type) &&
                SubCommandNew.hasCreationPermission(SpigotAdapter.getBukkitPlayer(gui.getGuiHolder()), type))
            {
                String initMessage = plugin.getMessages().getString(Message.GUI_DESCRIPTION_INITIATION,
                                                                    plugin.getMessages()
                                                                          .getString(EDoorType.getMessage(type)));
                gui.addItem(position++,
                            new GUIItem(GUI.NEWDOORMAT, initMessage, null, 1, type));
            }

    }

    private void startCreationProcess(final @NotNull Player player, final @NotNull EDoorType type)
    {
        player.closeInventory();
        subCommand.execute(player, null, type);
    }
}
