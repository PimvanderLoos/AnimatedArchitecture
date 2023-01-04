package nl.pim16aap2.bigdoors.spigot.gui;

import de.themoep.inventorygui.GuiElement;
import de.themoep.inventorygui.GuiStateElement;
import de.themoep.inventorygui.InventoryGui;
import de.themoep.inventorygui.StaticGuiElement;
import nl.pim16aap2.bigdoors.api.IConfigLoader;
import nl.pim16aap2.bigdoors.api.IPExecutor;
import nl.pim16aap2.bigdoors.api.factories.ITextFactory;
import nl.pim16aap2.bigdoors.commands.CommandFactory;
import nl.pim16aap2.bigdoors.doors.AbstractDoor;
import nl.pim16aap2.bigdoors.doors.DoorAttribute;
import nl.pim16aap2.bigdoors.localization.ILocalizer;
import nl.pim16aap2.bigdoors.spigot.util.implementations.PPlayerSpigot;
import nl.pim16aap2.bigdoors.util.doorretriever.DoorRetrieverFactory;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import javax.inject.Inject;

class AttributeButtonFactory
{
    private final ILocalizer localizer;
    private final ITextFactory textFactory;
    private final CommandFactory commandFactory;
    private final IPExecutor executor;
    private final DoorRetrieverFactory doorRetrieverFactory;
    private final DeleteGui.IFactory deleteGuiFactory;
    private final IConfigLoader config;

    @Inject //
    AttributeButtonFactory(
        ILocalizer localizer, ITextFactory textFactory, CommandFactory commandFactory, IPExecutor executor,
        DoorRetrieverFactory doorRetrieverFactory, DeleteGui.IFactory deleteGuiFactory, IConfigLoader config)
    {
        this.localizer = localizer;
        this.textFactory = textFactory;
        this.commandFactory = commandFactory;
        this.executor = executor;
        this.doorRetrieverFactory = doorRetrieverFactory;
        this.deleteGuiFactory = deleteGuiFactory;
        this.config = config;
    }

    private void lockButtonExecute(boolean newState, GuiElement.Click change, AbstractDoor door, PPlayerSpigot player)
    {
        commandFactory
            .newLock(player, doorRetrieverFactory.of(door), newState).run()
            // Force a draw with dynamic fields update to ensure the correct
            // state is displayed in case the command did not change the status.
            .thenRun(() -> executor.runOnMainThread(() -> change.getGui().draw(player.getBukkitPlayer(), true, false)));
    }

    private GuiElement lockButton(AbstractDoor door, PPlayerSpigot player, char slotChar)
    {
        final GuiStateElement element = new GuiStateElement(
            slotChar,
            () -> door.isLocked() ? "isLocked" : "isUnlocked",
            new GuiStateElement.State(
                change -> lockButtonExecute(true, change, door, player),
                "isLocked",
                new ItemStack(Material.RED_STAINED_GLASS_PANE),
                localizer.getMessage("gui.info_page.attribute.unlock",
                                     localizer.getMessage(door.getDoorType().getLocalizationKey()))
            ),
            new GuiStateElement.State(
                change -> lockButtonExecute(false, change, door, player),
                "isUnlocked",
                new ItemStack(Material.GREEN_STAINED_GLASS_PANE),
                localizer.getMessage("gui.info_page.attribute.lock",
                                     localizer.getMessage(door.getDoorType().getLocalizationKey()))
            )
        );
        element.setState(door.isLocked() ? "isLocked" : "isUnlocked");
        return element;
    }

    private GuiElement toggleButton(AbstractDoor door, PPlayerSpigot player, char slotChar)
    {
        return new StaticGuiElement(
            slotChar,
            new ItemStack(Material.LEVER),
            click ->
            {
                commandFactory.newToggle(player, config.getAnimationTime(door.getDoorType()),
                                         doorRetrieverFactory.of(door)).run();
                return true;
            },
            localizer.getMessage("gui.info_page.attribute.toggle",
                                 localizer.getMessage(door.getDoorType().getLocalizationKey()))
        );
    }

    private GuiElement switchButton(AbstractDoor door, PPlayerSpigot player, char slotChar)
    {
        return new StaticGuiElement(
            slotChar,
            new ItemStack(Material.REDSTONE_TORCH),
            click ->
            {
                // TODO: Implement this
                throw new UnsupportedOperationException("Switch hasn't been implemented yet!");
            },
            localizer.getMessage("gui.info_page.attribute.switch",
                                 localizer.getMessage(door.getDoorType().getLocalizationKey()))
        );
    }

    private GuiElement infoButton(AbstractDoor door, PPlayerSpigot player, char slotChar)
    {
        return new StaticGuiElement(
            slotChar,
            new ItemStack(Material.BOOKSHELF),
            click ->
            {
                player.sendInfo(textFactory, door.getBasicInfo());
                return true;
            },
            localizer.getMessage("gui.info_page.attribute.info",
                                 localizer.getMessage(door.getDoorType().getLocalizationKey()))
        );
    }

    private GuiElement deleteButton(
        MainGui mainGui, AbstractDoor door, PPlayerSpigot player, char slotChar)
    {
        return new StaticGuiElement(
            slotChar,
            new ItemStack(Material.BARRIER),
            click ->
            {
                deleteGuiFactory.newDeleteGui(door, player, mainGui);
                return true;
            },
            localizer.getMessage("gui.info_page.attribute.delete",
                                 localizer.getMessage(door.getDoorType().getLocalizationKey()))
        );
    }

    private GuiElement relocatePowerBlockButton(
        InventoryGui gui, AbstractDoor door, PPlayerSpigot player, char slotChar)
    {
        return new StaticGuiElement(
            slotChar,
            new ItemStack(Material.LEATHER_BOOTS),
            click ->
            {
                commandFactory.newMovePowerBlock(player, doorRetrieverFactory.of(door)).run();
                gui.close(true);
                return true;
            },
            localizer.getMessage("gui.info_page.attribute.relocate_power_block",
                                 localizer.getMessage(door.getDoorType().getLocalizationKey()))
        );
    }

    private GuiElement autoCloseTimerButton(
        InventoryGui gui, AbstractDoor door, PPlayerSpigot player, char slotChar)
    {
        return new StaticGuiElement(
            slotChar,
            new ItemStack(Material.CLOCK),
            click ->
            {
                commandFactory.getSetAutoCloseTimeDelayed().runDelayed(player, doorRetrieverFactory.of(door));
                gui.close(true);
                return true;
            },
            localizer.getMessage("gui.info_page.attribute.auto_close_timer",
                                 localizer.getMessage(door.getDoorType().getLocalizationKey()))
        );
    }

    private GuiElement openDirectionButton(
        InventoryGui gui, AbstractDoor door, PPlayerSpigot player, char slotChar)
    {
        return new StaticGuiElement(
            slotChar,
            new ItemStack(Material.COMPASS),
            click ->
            {
                commandFactory.getSetOpenDirectionDelayed().runDelayed(player, doorRetrieverFactory.of(door));
                gui.close(true);
                return true;
            },
            localizer.getMessage("gui.info_page.attribute.open_direction",
                                 localizer.getMessage(door.getDoorType().getLocalizationKey()))
        );
    }

    private GuiElement blocksToMoveButton(
        InventoryGui gui, AbstractDoor door, PPlayerSpigot player, char slotChar)
    {
        return new StaticGuiElement(
            slotChar,
            new ItemStack(Material.STICKY_PISTON),
            click ->
            {
                commandFactory.getSetBlocksToMoveDelayed().runDelayed(player, doorRetrieverFactory.of(door));
                gui.close(true);
                return true;
            },
            localizer.getMessage("gui.info_page.attribute.blocks_to_move",
                                 localizer.getMessage(door.getDoorType().getLocalizationKey()))
        );
    }

    private GuiElement addOwnerButton(
        InventoryGui gui, AbstractDoor door, PPlayerSpigot player, char slotChar)
    {
        return new StaticGuiElement(
            slotChar,
            new ItemStack(Material.PLAYER_HEAD),
            click ->
            {
                commandFactory.getAddOwnerDelayed().runDelayed(player, doorRetrieverFactory.of(door));
                gui.close(true);
                return true;
            },
            localizer.getMessage("gui.info_page.attribute.add_owner",
                                 localizer.getMessage(door.getDoorType().getLocalizationKey()))
        );
    }

    private GuiElement removeOwnerButton(
        InventoryGui gui, AbstractDoor door, PPlayerSpigot player, char slotChar)
    {
        return new StaticGuiElement(
            slotChar,
            new ItemStack(Material.SKELETON_SKULL),
            click ->
            {
                commandFactory.getRemoveOwnerDelayed().runDelayed(player, doorRetrieverFactory.of(door));
                gui.close(true);
                return true;
            },
            localizer.getMessage("gui.info_page.attribute.remove_owner",
                                 localizer.getMessage(door.getDoorType().getLocalizationKey()))
        );
    }

    /**
     * Creates a new GuiElement for the provided attribute.
     */
    public GuiElement of(
        InventoryGui gui, MainGui mainGui, DoorAttribute attribute, AbstractDoor door,
        PPlayerSpigot player, char slotChar)
    {
        return switch (attribute)
            {
                case LOCK -> this.lockButton(door, player, slotChar);
                case TOGGLE -> this.toggleButton(door, player, slotChar);
                case SWITCH -> this.switchButton(door, player, slotChar);
                case INFO -> this.infoButton(door, player, slotChar);
                case DELETE -> this.deleteButton(mainGui, door, player, slotChar);
                case RELOCATE_POWERBLOCK -> this.relocatePowerBlockButton(gui, door, player, slotChar);
                case AUTO_CLOSE_TIMER -> this.autoCloseTimerButton(gui, door, player, slotChar);
                case OPEN_DIRECTION -> this.openDirectionButton(gui, door, player, slotChar);
                case BLOCKS_TO_MOVE -> this.blocksToMoveButton(gui, door, player, slotChar);
                case ADD_OWNER -> this.addOwnerButton(gui, door, player, slotChar);
                case REMOVE_OWNER -> this.removeOwnerButton(gui, door, player, slotChar);
            };
    }
}
