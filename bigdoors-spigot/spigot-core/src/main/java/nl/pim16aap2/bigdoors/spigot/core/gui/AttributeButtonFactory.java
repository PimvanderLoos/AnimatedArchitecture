package nl.pim16aap2.bigdoors.spigot.core.gui;

import de.themoep.inventorygui.GuiElement;
import de.themoep.inventorygui.GuiStateElement;
import de.themoep.inventorygui.StaticGuiElement;
import nl.pim16aap2.bigdoors.core.api.IConfigLoader;
import nl.pim16aap2.bigdoors.core.api.IPExecutor;
import nl.pim16aap2.bigdoors.core.api.factories.ITextFactory;
import nl.pim16aap2.bigdoors.core.commands.CommandFactory;
import nl.pim16aap2.bigdoors.core.commands.Toggle;
import nl.pim16aap2.bigdoors.core.localization.ILocalizer;
import nl.pim16aap2.bigdoors.spigot.util.implementations.PPlayerSpigot;
import nl.pim16aap2.bigdoors.core.structures.AbstractStructure;
import nl.pim16aap2.bigdoors.core.structures.StructureAttribute;
import nl.pim16aap2.bigdoors.core.util.structureretriever.StructureRetrieverFactory;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import javax.inject.Inject;

class AttributeButtonFactory
{
    private final ILocalizer localizer;
    private final ITextFactory textFactory;
    private final CommandFactory commandFactory;
    private final IPExecutor executor;
    private final StructureRetrieverFactory structureRetrieverFactory;
    private final DeleteGui.IFactory deleteGuiFactory;
    private final IConfigLoader config;

    @Inject //
    AttributeButtonFactory(
        ILocalizer localizer, ITextFactory textFactory, CommandFactory commandFactory, IPExecutor executor,
        StructureRetrieverFactory structureRetrieverFactory, DeleteGui.IFactory deleteGuiFactory, IConfigLoader config)
    {
        this.localizer = localizer;
        this.textFactory = textFactory;
        this.commandFactory = commandFactory;
        this.executor = executor;
        this.structureRetrieverFactory = structureRetrieverFactory;
        this.deleteGuiFactory = deleteGuiFactory;
        this.config = config;
    }

    private void lockButtonExecute(
        boolean newState, GuiElement.Click change, AbstractStructure structure, PPlayerSpigot player)
    {
        commandFactory
            .newLock(player, structureRetrieverFactory.of(structure), newState).run()
            // Force a draw with dynamic fields update to ensure the correct
            // state is displayed in case the command did not change the status.
            .thenRun(() -> executor.runOnMainThread(() -> change.getGui().draw(player.getBukkitPlayer(), true, false)));
    }

    private GuiElement lockButton(AbstractStructure structure, PPlayerSpigot player, char slotChar)
    {
        final GuiStateElement element = new GuiStateElement(
            slotChar,
            () -> structure.isLocked() ? "isLocked" : "isUnlocked",
            new GuiStateElement.State(
                change -> lockButtonExecute(true, change, structure, player),
                "isLocked",
                new ItemStack(Material.RED_STAINED_GLASS_PANE),
                localizer.getMessage("gui.info_page.attribute.unlock",
                                     localizer.getMessage(structure.getType().getLocalizationKey()))
            ),
            new GuiStateElement.State(
                change -> lockButtonExecute(false, change, structure, player),
                "isUnlocked",
                new ItemStack(Material.GREEN_STAINED_GLASS_PANE),
                localizer.getMessage("gui.info_page.attribute.lock",
                                     localizer.getMessage(structure.getType().getLocalizationKey()))
            )
        );
        element.setState(structure.isLocked() ? "isLocked" : "isUnlocked");
        return element;
    }

    private GuiElement toggleButton(AbstractStructure structure, PPlayerSpigot player, char slotChar)
    {
        return new StaticGuiElement(
            slotChar,
            new ItemStack(Material.LEVER),
            click ->
            {
                commandFactory.newToggle(
                    player,
                    Toggle.DEFAULT_STRUCTURE_ACTION_TYPE,
                    Toggle.DEFAULT_ANIMATION_TYPE,
                    config.getAnimationSpeedMultiplier(structure.getType()),
                    structureRetrieverFactory.of(structure)).run();
                return true;
            },
            localizer.getMessage("gui.info_page.attribute.toggle",
                                 localizer.getMessage(structure.getType().getLocalizationKey()))
        );
    }

    private GuiElement switchButton(AbstractStructure structure, PPlayerSpigot player, char slotChar)
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
                                 localizer.getMessage(structure.getType().getLocalizationKey()))
        );
    }

    private GuiElement infoButton(AbstractStructure structure, PPlayerSpigot player, char slotChar)
    {
        return new StaticGuiElement(
            slotChar,
            new ItemStack(Material.BOOKSHELF),
            click ->
            {
                player.sendInfo(textFactory, structure.getBasicInfo());
                return true;
            },
            localizer.getMessage("gui.info_page.attribute.info",
                                 localizer.getMessage(structure.getType().getLocalizationKey()))
        );
    }

    private GuiElement deleteButton(
        AbstractStructure structure, PPlayerSpigot player, char slotChar)
    {
        return new StaticGuiElement(
            slotChar,
            new ItemStack(Material.BARRIER),
            click ->
            {
                deleteGuiFactory.newDeleteGui(structure, player);
                return true;
            },
            localizer.getMessage("gui.info_page.attribute.delete",
                                 localizer.getMessage(structure.getType().getLocalizationKey()))
        );
    }

    private GuiElement relocatePowerBlockButton(AbstractStructure structure, PPlayerSpigot player, char slotChar)
    {
        return new StaticGuiElement(
            slotChar,
            new ItemStack(Material.LEATHER_BOOTS),
            click ->
            {
                commandFactory.newMovePowerBlock(player, structureRetrieverFactory.of(structure)).run();
                GuiUtil.closeAllGuis(player);
                return true;
            },
            localizer.getMessage("gui.info_page.attribute.relocate_power_block",
                                 localizer.getMessage(structure.getType().getLocalizationKey()))
        );
    }

    private void isOpenButtonExecute(
        boolean isOpen, GuiElement.Click change, AbstractStructure structure, PPlayerSpigot player)
    {
        commandFactory
            .newSetOpenStatus(player, structureRetrieverFactory.of(structure), isOpen).run()
            // Force a draw with dynamic fields update to ensure the correct
            // state is displayed in case the command did not change the status.
            .thenRun(() -> executor.runOnMainThread(() -> change.getGui().draw(player.getBukkitPlayer(), true, false)));
    }

    private GuiElement openStatusButton(AbstractStructure structure, PPlayerSpigot player, char slotChar)
    {
        final GuiStateElement element = new GuiStateElement(
            slotChar,
            () -> structure.isOpen() ? "isOpen" : "isClosed",
            new GuiStateElement.State(
                change -> isOpenButtonExecute(true, change, structure, player),
                "isOpen",
                new ItemStack(Material.WARPED_DOOR),
                localizer.getMessage("gui.info_page.attribute.set_open",
                                     localizer.getMessage(structure.getType().getLocalizationKey()))
            ),
            new GuiStateElement.State(
                change -> isOpenButtonExecute(false, change, structure, player),
                "isClosed",
                new ItemStack(Material.MANGROVE_DOOR),
                localizer.getMessage("gui.info_page.attribute.set_closed",
                                     localizer.getMessage(structure.getType().getLocalizationKey()))
            )
        );
        element.setState(structure.isOpen() ? "isOpen" : "isClosed");
        return element;
    }

    private GuiElement openDirectionButton(AbstractStructure structure, PPlayerSpigot player, char slotChar)
    {
        return new StaticGuiElement(
            slotChar,
            new ItemStack(Material.COMPASS),
            click ->
            {
                commandFactory.getSetOpenDirectionDelayed().runDelayed(player, structureRetrieverFactory.of(structure));
                GuiUtil.closeAllGuis(player);
                return true;
            },
            localizer.getMessage("gui.info_page.attribute.open_direction",
                                 localizer.getMessage(structure.getType().getLocalizationKey()))
        );
    }

    private GuiElement blocksToMoveButton(AbstractStructure structure, PPlayerSpigot player, char slotChar)
    {
        return new StaticGuiElement(
            slotChar,
            new ItemStack(Material.STICKY_PISTON),
            click ->
            {
                commandFactory.getSetBlocksToMoveDelayed().runDelayed(player, structureRetrieverFactory.of(structure));
                GuiUtil.closeAllGuis(player);
                return true;
            },
            localizer.getMessage("gui.info_page.attribute.blocks_to_move",
                                 localizer.getMessage(structure.getType().getLocalizationKey()))
        );
    }

    private GuiElement addOwnerButton(AbstractStructure structure, PPlayerSpigot player, char slotChar)
    {
        return new StaticGuiElement(
            slotChar,
            new ItemStack(Material.PLAYER_HEAD),
            click ->
            {
                commandFactory.getAddOwnerDelayed().runDelayed(player, structureRetrieverFactory.of(structure));
                GuiUtil.closeAllGuis(player);
                return true;
            },
            localizer.getMessage("gui.info_page.attribute.add_owner",
                                 localizer.getMessage(structure.getType().getLocalizationKey()))
        );
    }

    private GuiElement removeOwnerButton(AbstractStructure structure, PPlayerSpigot player, char slotChar)
    {
        return new StaticGuiElement(
            slotChar,
            new ItemStack(Material.SKELETON_SKULL),
            click ->
            {
                commandFactory.getRemoveOwnerDelayed().runDelayed(player, structureRetrieverFactory.of(structure));
                GuiUtil.closeAllGuis(player);
                return true;
            },
            localizer.getMessage("gui.info_page.attribute.remove_owner",
                                 localizer.getMessage(structure.getType().getLocalizationKey()))
        );
    }

    /**
     * Creates a new GuiElement for the provided attribute.
     */
    public GuiElement of(StructureAttribute attribute, AbstractStructure structure, PPlayerSpigot player, char slotChar)
    {
        return switch (attribute)
            {
                case LOCK -> this.lockButton(structure, player, slotChar);
                case TOGGLE -> this.toggleButton(structure, player, slotChar);
                case SWITCH -> this.switchButton(structure, player, slotChar);
                case INFO -> this.infoButton(structure, player, slotChar);
                case DELETE -> this.deleteButton(structure, player, slotChar);
                case RELOCATE_POWERBLOCK -> this.relocatePowerBlockButton(structure, player, slotChar);
                case OPEN_STATUS -> this.openStatusButton(structure, player, slotChar);
                case OPEN_DIRECTION -> this.openDirectionButton(structure, player, slotChar);
                case BLOCKS_TO_MOVE -> this.blocksToMoveButton(structure, player, slotChar);
                case ADD_OWNER -> this.addOwnerButton(structure, player, slotChar);
                case REMOVE_OWNER -> this.removeOwnerButton(structure, player, slotChar);
            };
    }
}
