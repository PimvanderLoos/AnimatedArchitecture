package nl.pim16aap2.bigdoors.gui;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.doors.DoorType;
import nl.pim16aap2.bigdoors.spigotutil.PageType;
import nl.pim16aap2.bigdoors.util.DoorOwner;
import nl.pim16aap2.bigdoors.util.messages.Message;
import nl.pim16aap2.bigdoors.util.messages.Messages;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class GUIPageRemoveOwner implements IGUIPage
{
    protected final BigDoors plugin;
    protected final GUI gui;
    protected final Messages messages;
    protected final int doorOwnerCount;
    private int doorOwnerPage = 0;
    private int maxDoorOwnerPageCount = 0;

    private List<DoorOwner> owners;

    protected GUIPageRemoveOwner(final @NotNull BigDoors plugin, final @NotNull GUI gui)
    {
        this.plugin = plugin;
        this.gui = gui;
        doorOwnerCount = plugin.getDatabaseManager().countOwnersOfDoor(gui.getDoor().getDoorUID());
        messages = plugin.getMessages();
        refresh();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PageType getPageType()
    {
        return PageType.REMOVEOWNER;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void handleInput(int interactionIDX)
    {
        if (interactionIDX == 0)
        {
            gui.setGUIPage(new GUIPageDoorInfo(plugin, gui));
            return;
        }
        else if (interactionIDX == 1)
        {
            --doorOwnerPage;
            gui.update();
        }
        else if (interactionIDX == 7)
        {
            ++doorOwnerPage;
            gui.update();
        }
        else if (interactionIDX > 8)
        {
            if (!gui.isStillOwner())
                return;
            removeOwner(gui.getItem(interactionIDX).getDoorOwner());
            if (owners.size() == 0)
            {
                gui.setGUIPage(new GUIPageDoorInfo(plugin, gui));
                return;
            }
            gui.update();
        }
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

        if (doorOwnerPage != 0)
        {
            lore.add(plugin.getMessages().getString(Message.GUI_DESCRIPTION_PREVIOUSPAGE,
                                                    Integer.toString(doorOwnerPage + 2),
                                                    Integer.toString(doorOwnerPage),
                                                    Integer.toString(maxDoorOwnerPageCount)));
            gui.addItem(0, new GUIItem(GUI.PAGESWITCHMAT,
                                       plugin.getMessages().getString(Message.GUI_BUTTON_PREVIOUSPAGE), lore,
                                       doorOwnerPage));
            lore.clear();
        }

        if ((doorOwnerPage + 1) < maxDoorOwnerPageCount)
        {
            lore.add(plugin.getMessages().getString(Message.GUI_DESCRIPTION_NEXTPAGE,
                                                    Integer.toString(doorOwnerPage + 2),
                                                    Integer.toString(doorOwnerPage),
                                                    Integer.toString(maxDoorOwnerPageCount)));
            gui.addItem(8, new GUIItem(GUI.PAGESWITCHMAT, messages.getString(Message.GUI_BUTTON_NEXTPAGE), lore,
                                       doorOwnerPage + 2));
            lore.clear();
        }

        lore.add(messages.getString(Message.GUI_DESCRIPTION_INFO, gui.getDoor().getName()));
        lore.add(messages.getString(Message.GUI_DESCRIPTION_DOORID, Long.toString(gui.getDoor().getDoorUID())));
        lore.add(messages.getString(DoorType.getMessage(gui.getDoor().getType())));
        gui.addItem(4,
                    new GUIItem(GUI.CURRDOORMAT, gui.getDoor().getName() + ": " + gui.getDoor().getDoorUID(), lore, 1));
    }

    protected void fillPage()
    {
        int idx = 9;
        for (DoorOwner owner : owners)
        {
            final int currentIDX = idx;
            // First add a regular player head without a special texture.
            gui.addItem(idx++, new GUIItem(owner));
            // Then request a player head with that player's head texture. This is a CompletableFuture, so just update
            // the player head whenever it becomes available.
            plugin.getHeadManager().getPlayerHead(owner.getPlayerUUID(), owner.getPlayerName()).whenComplete(
                (result, throwable) ->
                {
                    result.ifPresent(
                        HEAD -> gui.updateItem(currentIDX,
                                               Optional.of(new GUIItem(HEAD, owner.getPlayerName(), null,
                                                                       owner.getPermission()))));
                }
            );
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void refresh()
    {
        owners = plugin.getDatabaseManager().getDoorOwners(gui.getDoor().getDoorUID());
        Collections.sort(owners, Comparator.comparing(DoorOwner::getPlayerName));
        maxDoorOwnerPageCount =
            owners.size() / (GUI.CHESTSIZE - 9) + ((owners.size() % (GUI.CHESTSIZE - 9)) == 0 ? 0 : 1);

        fillHeader();
        fillPage();
    }

    private void removeOwner(DoorOwner owner)
    {
        plugin.getDatabaseManager().removeOwner(owner.getDoorUID(), owner.getPlayerUUID());
        owners.remove(owner);
    }
}
