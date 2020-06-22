package nl.pim16aap2.bigdoors.spigot.gui;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.spigot.BigDoorsSpigot;
import nl.pim16aap2.bigdoors.spigot.commands.CommandData;
import nl.pim16aap2.bigdoors.spigot.commands.subcommands.SubCommandDelete;
import nl.pim16aap2.bigdoors.spigot.util.PageType;
import nl.pim16aap2.bigdoors.spigot.util.SpigotAdapter;
import nl.pim16aap2.bigdoors.util.DoorAttribute;
import nl.pim16aap2.bigdoors.util.messages.Message;
import nl.pim16aap2.bigdoors.util.messages.Messages;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class GUIPageDeleteConfirmation implements IGUIPage
{
    protected final BigDoorsSpigot plugin;
    protected final GUI gui;
    protected final Messages messages;

    /**
     * Used to store whether or not a player has access to door removal for this door. It is stored in an intermediate
     * step so it can be aborted on an update or something.
     */
    @Nullable
    private CompletableFuture<Boolean> futurePermissionCheck = null;

    protected GUIPageDeleteConfirmation(final BigDoorsSpigot plugin, final GUI gui)
    {
        this.plugin = plugin;
        this.gui = gui;
        messages = plugin.getMessages();
        refresh();
    }

    /** {@inheritDoc} */
    @Override
    public void kill()
    {
        if (futurePermissionCheck != null && !futurePermissionCheck.isDone())
            futurePermissionCheck.cancel(true);
    }

    /** {@inheritDoc} */
    @Override
    public PageType getPageType()
    {
        return PageType.CONFIRMATION;
    }

    /** {@inheritDoc} */
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
        futurePermissionCheck = BigDoors.get().getDatabaseManager()
                                        .hasPermissionForAction(gui.getGuiHolder(), gui.getDoor().getDoorUID(),
                                                                DoorAttribute.DELETE);
        futurePermissionCheck.whenComplete(
            (isAllowed, throwable) ->
            {
                if (!isAllowed)
                    return;
                BigDoors.get().getPlatform().newPExecutor().runOnMainThread(
                    () ->
                    {
                        ((SubCommandDelete) plugin.getCommand(CommandData.DELETE))
                            .execute(SpigotAdapter.getBukkitPlayer(gui.getGuiHolder()), gui.getDoor());
                        gui.removeSelectedDoor();
                    });
            });

    }

    protected void fillHeader()
    {
        List<String> lore = new ArrayList<>();
        lore.add(plugin.getMessages().getString(Message.GUI_DESCRIPTION_PREVIOUSPAGE,
                                                Integer.toString(gui.getPage() + 2),
                                                Integer.toString(gui.getPage() + 1),
                                                Integer.toString(gui.getMaxPageCount())));
        gui.addItem(0, new GUIItem(GUI.PAGESWITCHMAT,
                                   plugin.getMessages().getString(Message.GUI_BUTTON_PREVIOUSPAGE), lore,
                                   Math.max(1, gui.getPage())));
        lore.clear();

        lore.add(messages.getString(Message.GUI_BUTTON_INFO));
        lore.add(messages.getString(Message.GUI_DESCRIPTION_DOORID, Long.toString(gui.getDoor().getDoorUID())));
//        lore.add(messages.getString(EDoorType.getMessage(gui.getDoor().getType())));
        lore.add(gui.getDoor().getDoorType().getTranslationName());
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

    /** {@inheritDoc} */
    @Override
    public void refresh()
    {
        fillHeader();
        fillPage();
    }
}
