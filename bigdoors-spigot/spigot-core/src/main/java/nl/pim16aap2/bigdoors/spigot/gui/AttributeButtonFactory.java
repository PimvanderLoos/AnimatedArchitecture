package nl.pim16aap2.bigdoors.spigot.gui;

import de.themoep.inventorygui.GuiElement;
import de.themoep.inventorygui.GuiStateElement;
import de.themoep.inventorygui.InventoryGui;
import de.themoep.inventorygui.StaticGuiElement;
import nl.pim16aap2.bigdoors.api.IConfigLoader;
import nl.pim16aap2.bigdoors.api.IPExecutor;
import nl.pim16aap2.bigdoors.api.factories.ITextFactory;
import nl.pim16aap2.bigdoors.commands.CommandFactory;
import nl.pim16aap2.bigdoors.localization.ILocalizer;
import nl.pim16aap2.bigdoors.movable.AbstractMovable;
import nl.pim16aap2.bigdoors.movable.MovableAttribute;
import nl.pim16aap2.bigdoors.spigot.util.implementations.PPlayerSpigot;
import nl.pim16aap2.bigdoors.util.movableretriever.MovableRetrieverFactory;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import javax.inject.Inject;

class AttributeButtonFactory
{
    private final ILocalizer localizer;
    private final ITextFactory textFactory;
    private final CommandFactory commandFactory;
    private final IPExecutor executor;
    private final MovableRetrieverFactory movableRetrieverFactory;
    private final DeleteGui.IFactory deleteGuiFactory;
    private final IConfigLoader config;

    @Inject //
    AttributeButtonFactory(
        ILocalizer localizer, ITextFactory textFactory, CommandFactory commandFactory, IPExecutor executor,
        MovableRetrieverFactory movableRetrieverFactory, DeleteGui.IFactory deleteGuiFactory, IConfigLoader config)
    {
        this.localizer = localizer;
        this.textFactory = textFactory;
        this.commandFactory = commandFactory;
        this.executor = executor;
        this.movableRetrieverFactory = movableRetrieverFactory;
        this.deleteGuiFactory = deleteGuiFactory;
        this.config = config;
    }

    private void lockButtonExecute(
        boolean newState, GuiElement.Click change, AbstractMovable movable, PPlayerSpigot player)
    {
        commandFactory
            .newLock(player, movableRetrieverFactory.of(movable), newState).run()
            // Force a draw with dynamic fields update to ensure the correct
            // state is displayed in case the command did not change the status.
            .thenRun(() -> executor.runOnMainThread(() -> change.getGui().draw(player.getBukkitPlayer(), true, false)));
    }

    private GuiElement lockButton(AbstractMovable movable, PPlayerSpigot player, char slotChar)
    {
        final GuiStateElement element = new GuiStateElement(
            slotChar,
            () -> movable.isLocked() ? "isLocked" : "isUnlocked",
            new GuiStateElement.State(
                change -> lockButtonExecute(true, change, movable, player),
                "isLocked",
                new ItemStack(Material.RED_STAINED_GLASS_PANE),
                localizer.getMessage("gui.info_page.attribute.unlock",
                                     localizer.getMessage(movable.getMovableType().getLocalizationKey()))
            ),
            new GuiStateElement.State(
                change -> lockButtonExecute(false, change, movable, player),
                "isUnlocked",
                new ItemStack(Material.GREEN_STAINED_GLASS_PANE),
                localizer.getMessage("gui.info_page.attribute.lock",
                                     localizer.getMessage(movable.getMovableType().getLocalizationKey()))
            )
        );
        element.setState(movable.isLocked() ? "isLocked" : "isUnlocked");
        return element;
    }

    private GuiElement toggleButton(AbstractMovable movable, PPlayerSpigot player, char slotChar)
    {
        return new StaticGuiElement(
            slotChar,
            new ItemStack(Material.LEVER),
            click ->
            {
                commandFactory.newToggle(player, config.getAnimationSpeedMultiplier(movable.getMovableType()),
                                         movableRetrieverFactory.of(movable)).run();
                return true;
            },
            localizer.getMessage("gui.info_page.attribute.toggle",
                                 localizer.getMessage(movable.getMovableType().getLocalizationKey()))
        );
    }

    private GuiElement switchButton(AbstractMovable movable, PPlayerSpigot player, char slotChar)
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
                                 localizer.getMessage(movable.getMovableType().getLocalizationKey()))
        );
    }

    private GuiElement infoButton(AbstractMovable movable, PPlayerSpigot player, char slotChar)
    {
        return new StaticGuiElement(
            slotChar,
            new ItemStack(Material.BOOKSHELF),
            click ->
            {
                player.sendInfo(textFactory, movable.getBasicInfo());
                return true;
            },
            localizer.getMessage("gui.info_page.attribute.info",
                                 localizer.getMessage(movable.getMovableType().getLocalizationKey()))
        );
    }

    private GuiElement deleteButton(
        MainGui mainGui, AbstractMovable movable, PPlayerSpigot player, char slotChar)
    {
        return new StaticGuiElement(
            slotChar,
            new ItemStack(Material.BARRIER),
            click ->
            {
                deleteGuiFactory.newDeleteGui(movable, player, mainGui);
                return true;
            },
            localizer.getMessage("gui.info_page.attribute.delete",
                                 localizer.getMessage(movable.getMovableType().getLocalizationKey()))
        );
    }

    private GuiElement relocatePowerBlockButton(
        InventoryGui gui, AbstractMovable movable, PPlayerSpigot player, char slotChar)
    {
        return new StaticGuiElement(
            slotChar,
            new ItemStack(Material.LEATHER_BOOTS),
            click ->
            {
                commandFactory.newMovePowerBlock(player, movableRetrieverFactory.of(movable)).run();
                gui.close(true);
                return true;
            },
            localizer.getMessage("gui.info_page.attribute.relocate_power_block",
                                 localizer.getMessage(movable.getMovableType().getLocalizationKey()))
        );
    }

    private GuiElement autoCloseTimerButton(
        InventoryGui gui, AbstractMovable movable, PPlayerSpigot player, char slotChar)
    {
        return new StaticGuiElement(
            slotChar,
            new ItemStack(Material.CLOCK),
            click ->
            {
                commandFactory.getSetAutoCloseTimeDelayed().runDelayed(player, movableRetrieverFactory.of(movable));
                gui.close(true);
                return true;
            },
            localizer.getMessage("gui.info_page.attribute.auto_close_timer",
                                 localizer.getMessage(movable.getMovableType().getLocalizationKey()))
        );
    }

    private GuiElement openDirectionButton(
        InventoryGui gui, AbstractMovable movable, PPlayerSpigot player, char slotChar)
    {
        return new StaticGuiElement(
            slotChar,
            new ItemStack(Material.COMPASS),
            click ->
            {
                commandFactory.getSetOpenDirectionDelayed().runDelayed(player, movableRetrieverFactory.of(movable));
                gui.close(true);
                return true;
            },
            localizer.getMessage("gui.info_page.attribute.open_direction",
                                 localizer.getMessage(movable.getMovableType().getLocalizationKey()))
        );
    }

    private GuiElement blocksToMoveButton(
        InventoryGui gui, AbstractMovable movable, PPlayerSpigot player, char slotChar)
    {
        return new StaticGuiElement(
            slotChar,
            new ItemStack(Material.STICKY_PISTON),
            click ->
            {
                commandFactory.getSetBlocksToMoveDelayed().runDelayed(player, movableRetrieverFactory.of(movable));
                gui.close(true);
                return true;
            },
            localizer.getMessage("gui.info_page.attribute.blocks_to_move",
                                 localizer.getMessage(movable.getMovableType().getLocalizationKey()))
        );
    }

    private GuiElement addOwnerButton(
        InventoryGui gui, AbstractMovable movable, PPlayerSpigot player, char slotChar)
    {
        return new StaticGuiElement(
            slotChar,
            new ItemStack(Material.PLAYER_HEAD),
            click ->
            {
                commandFactory.getAddOwnerDelayed().runDelayed(player, movableRetrieverFactory.of(movable));
                gui.close(true);
                return true;
            },
            localizer.getMessage("gui.info_page.attribute.add_owner",
                                 localizer.getMessage(movable.getMovableType().getLocalizationKey()))
        );
    }

    private GuiElement removeOwnerButton(
        InventoryGui gui, AbstractMovable movable, PPlayerSpigot player, char slotChar)
    {
        return new StaticGuiElement(
            slotChar,
            new ItemStack(Material.SKELETON_SKULL),
            click ->
            {
                commandFactory.getRemoveOwnerDelayed().runDelayed(player, movableRetrieverFactory.of(movable));
                gui.close(true);
                return true;
            },
            localizer.getMessage("gui.info_page.attribute.remove_owner",
                                 localizer.getMessage(movable.getMovableType().getLocalizationKey()))
        );
    }

    /**
     * Creates a new GuiElement for the provided attribute.
     */
    public GuiElement of(
        InventoryGui gui, MainGui mainGui, MovableAttribute attribute, AbstractMovable movable,
        PPlayerSpigot player, char slotChar)
    {
        return switch (attribute)
            {
                case LOCK -> this.lockButton(movable, player, slotChar);
                case TOGGLE -> this.toggleButton(movable, player, slotChar);
                case SWITCH -> this.switchButton(movable, player, slotChar);
                case INFO -> this.infoButton(movable, player, slotChar);
                case DELETE -> this.deleteButton(mainGui, movable, player, slotChar);
                case RELOCATE_POWERBLOCK -> this.relocatePowerBlockButton(gui, movable, player, slotChar);
                case AUTO_CLOSE_TIMER -> this.autoCloseTimerButton(gui, movable, player, slotChar);
                case OPEN_DIRECTION -> this.openDirectionButton(gui, movable, player, slotChar);
                case BLOCKS_TO_MOVE -> this.blocksToMoveButton(gui, movable, player, slotChar);
                case ADD_OWNER -> this.addOwnerButton(gui, movable, player, slotChar);
                case REMOVE_OWNER -> this.removeOwnerButton(gui, movable, player, slotChar);
            };
    }
}
