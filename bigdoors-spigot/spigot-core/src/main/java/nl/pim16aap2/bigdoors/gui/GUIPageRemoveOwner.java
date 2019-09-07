package nl.pim16aap2.bigdoors.gui;

import nl.pim16aap2.bigdoors.BigDoorsSpigot;
import nl.pim16aap2.bigdoors.doors.DoorType;
import nl.pim16aap2.bigdoors.spigotutil.PageType;
import nl.pim16aap2.bigdoors.util.DoorOwner;
import nl.pim16aap2.bigdoors.util.messages.Message;
import nl.pim16aap2.bigdoors.util.messages.Messages;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class GUIPageRemoveOwner implements IGUIPage
{
    protected final BigDoorsSpigot plugin;
    protected final GUI gui;
    protected final Messages messages;
    private int doorOwnerPage = 0;
    private int maxDoorOwnerPageCount = 0;

    /**
     * Used to store future player heads before they are retrieved. It is stored in an intermediate step so it can be
     * aborted on an update or something.
     */
    @NotNull
    private List<CompletableFuture<Optional<ItemStack>>> futurePlayerHeads = new ArrayList<>();

    /**
     * Used to store a list of future owners of the door before they are retrieved. It is stored in an intermediate step
     * so it can be aborted on an update or something.
     */
    @Nullable
    private CompletableFuture<List<DoorOwner>> futureDoorOwners = null;

    private List<DoorOwner> owners;

    protected GUIPageRemoveOwner(final @NotNull BigDoorsSpigot plugin, final @NotNull GUI gui)
    {
        this.plugin = plugin;
        this.gui = gui;
        messages = plugin.getMessages();
        refresh();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void kill()
    {
        for (CompletableFuture<Optional<ItemStack>> futurePlayerHead : futurePlayerHeads)
        {
            if (!futurePlayerHead.isDone())
                futurePlayerHead.cancel(true);
        }
        if (futureDoorOwners != null && !futureDoorOwners.isDone())
            futureDoorOwners.cancel(true);
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
                                                Integer.toString(gui.getPage() + 2),
                                                Integer.toString(gui.getPage() + 1),
                                                Integer.toString(gui.getMaxPageCount())));
        gui.setItem(0, new GUIItem(GUI.PAGESWITCHMAT,
                                   plugin.getMessages().getString(Message.GUI_BUTTON_PREVIOUSPAGE), lore,
                                   Math.max(1, gui.getPage())));
        lore.clear();

        if (doorOwnerPage != 0)
        {
            lore.add(plugin.getMessages().getString(Message.GUI_DESCRIPTION_PREVIOUSPAGE,
                                                    Integer.toString(doorOwnerPage + 2),
                                                    Integer.toString(doorOwnerPage),
                                                    Integer.toString(maxDoorOwnerPageCount)));
            gui.setItem(0, new GUIItem(GUI.PAGESWITCHMAT,
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
            gui.setItem(8, new GUIItem(GUI.PAGESWITCHMAT, messages.getString(Message.GUI_BUTTON_NEXTPAGE), lore,
                                       doorOwnerPage + 2));
            lore.clear();
        }

        lore.add(messages.getString(Message.GUI_DESCRIPTION_INFO, gui.getDoor().getName()));
        lore.add(messages.getString(Message.GUI_DESCRIPTION_DOORID, Long.toString(gui.getDoor().getDoorUID())));
        lore.add(messages.getString(DoorType.getMessage(gui.getDoor().getType())));
        gui.setItem(4, new GUIItem(GUI.CURRDOORMAT, gui.getDoor().getName() + ": " + gui.getDoor().getDoorUID(),
                                   lore, 1));
    }

    protected void fillPage()
    {
        int idx = 9;
        for (DoorOwner owner : owners)
        {
            if (owner.getPlayerUUID().equals(gui.getGuiHolder().getUniqueId()))
                continue;

            final int currentIDX = idx;
            // First add a regular player head without a special texture.
            gui.setItem(idx++, new GUIItem(owner));
            // Then request a player head with that player's head texture. This is a CompletableFuture, so just update
            // the player head whenever it becomes available.
            CompletableFuture<Optional<ItemStack>> futurePlayerHead = plugin.getHeadManager()
                                                                            .getPlayerHead(owner.getPlayerUUID(),
                                                                                           owner.getPlayerName());
            futurePlayerHeads.add(futurePlayerHead);
            futurePlayerHead.whenComplete(
                (result, throwable) ->
                    result.ifPresent(
                        HEAD -> BigDoorsSpigot.newMainThreadExecutor().runOnMainThread(
                            () -> gui.updateItem(currentIDX,
                                                 Optional.of(new GUIItem(HEAD, owner.getPlayerName(), null,
                                                                         owner.getPermission())))))
            );
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void refresh()
    {
        futureDoorOwners = plugin.getDatabaseManager().getDoorOwners(gui.getDoor().getDoorUID());
        futureDoorOwners.whenComplete(
            (ownerList, throwable) ->
            {
                owners = ownerList;
                owners.sort(Comparator.comparing(DoorOwner::getPlayerName));
                maxDoorOwnerPageCount =
                    owners.size() / (GUI.CHESTSIZE - 9) + ((owners.size() % (GUI.CHESTSIZE - 9)) == 0 ? 0 : 1);

                BigDoorsSpigot.newMainThreadExecutor().runOnMainThread(
                    () ->
                    {
                        fillHeader();
                        fillPage();
                    });
            });
    }

    private void removeOwner(DoorOwner owner)
    {
        plugin.getDatabaseManager().removeOwner(owner.getDoorUID(), owner.getPlayerUUID());
        owners.remove(owner);
    }
}
