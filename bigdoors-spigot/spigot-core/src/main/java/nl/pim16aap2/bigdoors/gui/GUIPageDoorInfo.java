package nl.pim16aap2.bigdoors.gui;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.BigDoorsSpigot;
import nl.pim16aap2.bigdoors.commands.CommandData;
import nl.pim16aap2.bigdoors.commands.subcommands.SubCommandInfo;
import nl.pim16aap2.bigdoors.commands.subcommands.SubCommandToggle;
import nl.pim16aap2.bigdoors.doors.AbstractDoorBase;
import nl.pim16aap2.bigdoors.doors.DoorType;
import nl.pim16aap2.bigdoors.managers.DatabaseManager;
import nl.pim16aap2.bigdoors.spigotutil.PageType;
import nl.pim16aap2.bigdoors.spigotutil.SpigotAdapter;
import nl.pim16aap2.bigdoors.util.DoorAttribute;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import nl.pim16aap2.bigdoors.util.messages.Message;
import nl.pim16aap2.bigdoors.util.messages.Messages;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

public class GUIPageDoorInfo implements IGUIPage
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

    protected GUIPageDoorInfo(final BigDoorsSpigot plugin, final GUI gui)
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
        if (futurePermissionCheck != null && !futurePermissionCheck.isDone())
            futurePermissionCheck.cancel(true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PageType getPageType()
    {
        return PageType.DOORINFO;
    }

    /**
     * Handles user input for an action that they have the required level of ownership for for this door in the database
     * manager.
     *
     * @param door           The door.
     * @param player         The player.
     * @param guiItem        The GUIItem pressed.
     * @param interactionIDX The index of the GUIItem the player interacted with.
     */
    private void handleAllowedInput(final @NotNull AbstractDoorBase door, final @NotNull Player player,
                                    final @NotNull GUIItem guiItem, final int interactionIDX)
    {
        switch (guiItem.getDoorAttribute().get())
        {
            case LOCK:
                door.setLock(!door.isLocked());
                BigDoors.get().getDatabaseManager().setLock(door.getDoorUID(), door.isLocked());
                gui.updateItem(interactionIDX, createGUIItemOfAttribute(door, DoorAttribute.LOCK));
                break;
            case TOGGLE:
                ((SubCommandToggle) plugin.getCommand(CommandData.TOGGLE)).execute(player, door);
                break;
            case INFO:
                ((SubCommandInfo) plugin.getCommand(CommandData.INFO)).execute(player, door);
                break;
            case DELETE:
                gui.setGUIPage(new GUIPageDeleteConfirmation(plugin, gui));
                break;
            case RELOCATEPOWERBLOCK:
                plugin.getAbortableTaskManager().startPowerBlockRelocator(player, door);
                gui.close();
                break;
            case DIRECTION_ROTATE_VERTICAL2:
            case DIRECTION_STRAIGHT_HORIZONTAL:
            case DIRECTION_STRAIGHT_VERTICAL:
            case DIRECTION_ROTATE_HORIZONTAL:
            case DIRECTION_ROTATE_VERTICAL:
                changeOpenDir(door, interactionIDX);
                break;
            case CHANGETIMER:
                plugin.getAbortableTaskManager().startTimerSetter(player, door);
                gui.close();
                break;
            case BLOCKSTOMOVE:
                plugin.getAbortableTaskManager().startBlocksToMoveSetter(player, door);
                gui.close();
                break;
            case ADDOWNER:
                plugin.getAbortableTaskManager().startAddOwner(player, door);
                gui.close();
                break;
            case REMOVEOWNER:
                switchToRemoveOwner();
        }
    }

    /**
     * {@inheritDoc}
     */
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

        GUIItem guiItem = gui.getItem(interactionIDX);
        if (!guiItem.getDoorAttribute().isPresent())
            return;

        futurePermissionCheck = DatabaseManager.get()
                                               .hasPermissionForAction(gui.getGuiHolder(),
                                                                       gui.getDoor().getDoorUID(),
                                                                       guiItem.getDoorAttribute().get());
        futurePermissionCheck.whenComplete(
            (isAllowed, throwable) ->
            {
                if (!isAllowed)
                {
                    gui.update();
                    return;
                }
                BigDoors.get().getPlatform().newPExecutor().runOnMainThread(
                    () -> handleAllowedInput(gui.getDoor(), SpigotAdapter.getBukkitPlayer(gui.getGuiHolder()), guiItem,
                                             interactionIDX));
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

        lore.add(messages.getString(Message.GUI_DESCRIPTION_INFO, gui.getDoor().getName()));
        lore.add(messages.getString(Message.GUI_DESCRIPTION_DOORID, Long.toString(gui.getDoor().getDoorUID())));
        lore.add(messages.getString(DoorType.getMessage(gui.getDoor().getType())));
        gui.addItem(4, new GUIItem(GUI.CURRDOORMAT, gui.getDoor().getName() + ": " + gui.getDoor().getDoorUID(),
                                   lore, 1));
    }

    protected void fillPage()
    {
        final AtomicInteger position = new AtomicInteger(8);
        for (DoorAttribute attr : DoorType.getAttributes(gui.getDoor().getType()))
            createGUIItemOfAttribute(gui.getDoor(), attr).ifPresent(I -> gui.addItem(position.addAndGet(1), I));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void refresh()
    {
        fillHeader();
        fillPage();
    }

    private void switchToRemoveOwner()
    {
//        // Text based method
//        plugin.getCommander().startRemoveOwner(gui.getOfflinePlayer(), gui.getDoor());
//        gui.close();

        // GUI based method
        gui.setGUIPage(new GUIPageRemoveOwner(plugin, gui));
    }

    // Changes the opening direction for a door.
    private void changeOpenDir(final @NotNull AbstractDoorBase door, final int index)
    {
        DoorAttribute[] attributes = DoorType.getAttributes(door.getType());
        DoorAttribute openTypeAttribute = null;

        outerLoop:
        for (int idx = 0; idx != attributes.length; ++idx)
        {
            switch (attributes[idx])
            {
                case DIRECTION_ROTATE_HORIZONTAL:
                    openTypeAttribute = DoorAttribute.DIRECTION_ROTATE_HORIZONTAL;
                    break outerLoop;
                case DIRECTION_ROTATE_VERTICAL:
                    openTypeAttribute = DoorAttribute.DIRECTION_ROTATE_VERTICAL;
                    break outerLoop;

                case DIRECTION_ROTATE_VERTICAL2:
                    openTypeAttribute = DoorAttribute.DIRECTION_ROTATE_VERTICAL2;
                    break outerLoop;
                case DIRECTION_STRAIGHT_HORIZONTAL:
                    openTypeAttribute = DoorAttribute.DIRECTION_STRAIGHT_HORIZONTAL;
                    break outerLoop;
                case DIRECTION_STRAIGHT_VERTICAL:
                    openTypeAttribute = DoorAttribute.DIRECTION_STRAIGHT_VERTICAL;
                    break outerLoop;
                default:
                    break;
            }
        }
        RotateDirection newOpenDir = door.cycleOpenDirection();

        BigDoors.get().getDatabaseManager().updateDoorOpenDirection(door.getDoorUID(), newOpenDir);
        int idx = gui.indexOfDoor(door);
        gui.getDoor(idx).setOpenDir(newOpenDir);
        gui.setDoor(gui.getDoor(idx));
        // TODO: Check this.
        gui.updateItem(index, createGUIItemOfAttribute(door, openTypeAttribute));
    }

    @NotNull
    private Optional<GUIItem> createGUIItemOfAttribute(final @NotNull AbstractDoorBase door,
                                                       final @NotNull DoorAttribute atr)
    {
        // If the permission level is higher than the max permission of this action.
        if (door.getPermission() > DoorAttribute.getPermissionLevel(atr))
            return Optional.empty();

        List<String> lore = new ArrayList<>();
        String desc;
        GUIItem ret = null;

        switch (atr)
        {
            case LOCK:
                if (door.isLocked())
                    ret = new GUIItem(GUI.LOCKDOORMAT, messages.getString(Message.GUI_BUTTON_LOCK), null, 1);
                else
                    ret = new GUIItem(GUI.UNLOCKDOORMAT, messages.getString(Message.GUI_BUTTON_UNLOCK), null, 1);
                break;

            case TOGGLE:
                desc = messages.getString(Message.GUI_BUTTON_TOGGLE);
                ret = new GUIItem(GUI.TOGGLEDOORMAT, desc, lore, 1);
                break;

            case INFO:
                desc = messages.getString(Message.GUI_BUTTON_INFO);
                ret = new GUIItem(GUI.INFOMAT, desc, lore, 1);
                break;

            case DELETE:
                desc = messages.getString(Message.GUI_BUTTON_DOOR_DELETE);
                lore.add(messages.getString(Message.GUI_DESCRIPTION_DOOR_DELETE));
                ret = new GUIItem(GUI.DELDOORMAT, desc, lore, 1);
                break;

            case RELOCATEPOWERBLOCK:
                desc = messages.getString(Message.GUI_BUTTON_RELOCATEPB);
                ret = new GUIItem(GUI.RELOCATEPBMAT, desc, lore, 1);
                break;

            case CHANGETIMER:
                desc = messages.getString(Message.GUI_BUTTON_TIMER);
                lore.add(door.getAutoClose() > -1 ?
                         messages.getString(Message.GUI_DESCRIPTION_TIMER_SET, Integer.toString(door.getAutoClose())) :
                         messages.getString(Message.GUI_DESCRIPTION_TIMER_NOTSET));
                int count = Math.max(1, door.getAutoClose());
                ret = new GUIItem(GUI.CHANGETIMEMAT, desc, lore, count);
                break;

            case DIRECTION_ROTATE_VERTICAL2:
            case DIRECTION_STRAIGHT_HORIZONTAL:
            case DIRECTION_STRAIGHT_VERTICAL:
            case DIRECTION_ROTATE_VERTICAL:
                desc = messages.getString(Message.GUI_BUTTON_DIRECTION);
                lore.add(messages.getString(Message.GUI_DESCRIPTION_OPENDIRECTION,
                                            messages.getString(RotateDirection.getMessage(
                                                door.getOpenDir()))));
                ret = new GUIItem(GUI.SETOPENDIRMAT, desc, lore, 1);
                break;

            case DIRECTION_ROTATE_HORIZONTAL:
                desc = messages.getString(Message.GUI_BUTTON_DIRECTION);
                lore.add(messages.getString(Message.GUI_DESCRIPTION_OPENDIRECTION_RELATIVE,
                                            messages.getString(
                                                RotateDirection.getMessage(door.getOpenDir())),
                                            messages.getString(RotateDirection.getMessage(RotateDirection.DOWN))));
                ret = new GUIItem(GUI.SETOPENDIRMAT, desc, lore, 1);
                break;

            case BLOCKSTOMOVE:
                desc = messages.getString(Message.GUI_BUTTON_BLOCKSTOMOVE);
                lore.add(messages.getString(Message.GUI_DESCRIPTION_BLOCKSTOMOVE,
                                            Integer.toString(door.getBlocksToMove())));
                ret = new GUIItem(GUI.SETBTMOVEMAT, desc, lore, 1);
                break;

            case ADDOWNER:
                desc = messages.getString(Message.GUI_BUTTON_OWNER_ADD);
                ret = new GUIItem(GUI.ADDOWNERMAT, desc, lore, 1);
                break;

            case REMOVEOWNER:
                desc = messages.getString(Message.GUI_BUTTON_OWNER_DELETE);
                ret = new GUIItem(GUI.REMOVEOWNERMAT, desc, lore, 1);
                break;
            default:
                break;
        }
        if (ret != null)
            ret.setDoorAttribute(atr);
        return Optional.ofNullable(ret);
    }
}
