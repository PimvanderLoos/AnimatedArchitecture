package nl.pim16aap2.animatedarchitecture.spigot.core.gui;

import de.themoep.inventorygui.GuiElement;
import de.themoep.inventorygui.GuiStateElement;
import de.themoep.inventorygui.StaticGuiElement;
import nl.pim16aap2.animatedarchitecture.core.animation.AnimationType;
import nl.pim16aap2.animatedarchitecture.core.api.IExecutor;
import nl.pim16aap2.animatedarchitecture.core.api.factories.ITextFactory;
import nl.pim16aap2.animatedarchitecture.core.commands.CommandFactory;
import nl.pim16aap2.animatedarchitecture.core.events.StructureActionCause;
import nl.pim16aap2.animatedarchitecture.core.events.StructureActionType;
import nl.pim16aap2.animatedarchitecture.core.localization.ILocalizer;
import nl.pim16aap2.animatedarchitecture.core.structures.AbstractStructure;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureAnimationRequestBuilder;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureAttribute;
import nl.pim16aap2.animatedarchitecture.core.structures.retriever.StructureRetrieverFactory;
import nl.pim16aap2.animatedarchitecture.core.text.TextComponent;
import nl.pim16aap2.animatedarchitecture.core.util.MovementDirection;
import nl.pim16aap2.animatedarchitecture.core.util.Util;
import nl.pim16aap2.animatedarchitecture.spigot.util.implementations.PlayerSpigot;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import javax.inject.Inject;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Factory for creating buttons for the different attributes of a structure.
 */
class AttributeButtonFactory
{
    private final ILocalizer localizer;
    private final ITextFactory textFactory;
    private final CommandFactory commandFactory;
    private final IExecutor executor;
    private final StructureRetrieverFactory structureRetrieverFactory;
    private final StructureAnimationRequestBuilder structureAnimationRequestBuilder;
    private final DeleteGui.IFactory deleteGuiFactory;

    @Inject
    AttributeButtonFactory(
        ILocalizer localizer,
        ITextFactory textFactory,
        CommandFactory commandFactory,
        IExecutor executor,
        StructureRetrieverFactory structureRetrieverFactory,
        StructureAnimationRequestBuilder structureAnimationRequestBuilder,
        DeleteGui.IFactory deleteGuiFactory)
    {
        this.localizer = localizer;
        this.textFactory = textFactory;
        this.commandFactory = commandFactory;
        this.executor = executor;
        this.structureRetrieverFactory = structureRetrieverFactory;
        this.structureAnimationRequestBuilder = structureAnimationRequestBuilder;
        this.deleteGuiFactory = deleteGuiFactory;
    }

    private void lockButtonExecute(
        boolean newState, GuiElement.Click change, AbstractStructure structure, PlayerSpigot player)
    {
        commandFactory
            .newLock(player, structureRetrieverFactory.of(structure), newState).run()
            // Force a draw with dynamic fields update to ensure the correct
            // state is displayed in case the command did not change the status.
            .thenRun(() -> executor.runOnMainThread(() -> change.getGui().draw(player.getBukkitPlayer(), true, false)))
            .exceptionally(Util::exceptionally);
    }

    private GuiElement lockButton(AbstractStructure structure, PlayerSpigot player, char slotChar)
    {
        final GuiStateElement element = new GuiStateElement(
            slotChar,
            () -> structure.isLocked() ? "isLocked" : "isUnlocked",
            new GuiStateElement.State(
                change -> lockButtonExecute(true, change, structure, player),
                "isLocked",
                new ItemStack(Material.RED_STAINED_GLASS_PANE),
                localizer.getMessage(
                    "gui.info_page.attribute.unlock",
                    localizer.getMessage(structure.getType().getLocalizationKey()))
            ),
            new GuiStateElement.State(
                change -> lockButtonExecute(false, change, structure, player),
                "isUnlocked",
                new ItemStack(Material.GREEN_STAINED_GLASS_PANE),
                localizer.getMessage(
                    "gui.info_page.attribute.lock",
                    localizer.getMessage(structure.getType().getLocalizationKey()))
            )
        );
        element.setState(structure.isLocked() ? "isLocked" : "isUnlocked");
        return element;
    }

    private GuiElement toggleButton(AbstractStructure structure, PlayerSpigot player, char slotChar)
    {
        return new StaticGuiElement(
            slotChar,
            new ItemStack(Material.LEVER),
            click ->
            {
                structureAnimationRequestBuilder
                    .builder()
                    .structure(structure)
                    .structureActionCause(StructureActionCause.PLAYER)
                    .structureActionType(StructureActionType.TOGGLE)
                    .responsible(player)
                    .build()
                    .execute()
                    .exceptionally(Util::exceptionally);
                return true;
            },
            localizer.getMessage(
                "gui.info_page.attribute.toggle",
                localizer.getMessage(structure.getType().getLocalizationKey()))
        );
    }

    private GuiElement previewButton(AbstractStructure structure, PlayerSpigot player, char slotChar)
    {
        return new StaticGuiElement(
            slotChar,
            new ItemStack(Material.ENDER_EYE),
            click ->
            {
                structureAnimationRequestBuilder
                    .builder()
                    .structure(structure)
                    .structureActionCause(StructureActionCause.PLAYER)
                    .structureActionType(StructureActionType.TOGGLE)
                    .responsible(player)
                    .animationType(AnimationType.PREVIEW)
                    .build()
                    .execute()
                    .exceptionally(Util::exceptionally);
                return true;
            },
            localizer.getMessage(
                "gui.info_page.attribute.preview",
                localizer.getMessage(structure.getType().getLocalizationKey()))
        );
    }

    private GuiElement infoButton(AbstractStructure structure, PlayerSpigot player, char slotChar)
    {
        return new StaticGuiElement(
            slotChar,
            new ItemStack(Material.BOOKSHELF),
            click ->
            {
                commandFactory
                    .newInfo(player, structureRetrieverFactory.of(structure))
                    .run()
                    .exceptionally(Util::exceptionally);
                return true;
            },
            localizer.getMessage(
                "gui.info_page.attribute.info",
                localizer.getMessage(structure.getType().getLocalizationKey()))
        );
    }

    private GuiElement deleteButton(
        AbstractStructure structure, PlayerSpigot player, char slotChar)
    {
        return new StaticGuiElement(
            slotChar,
            new ItemStack(Material.BARRIER),
            click ->
            {
                deleteGuiFactory.newDeleteGui(structure, player);
                return true;
            },
            localizer.getMessage(
                "gui.info_page.attribute.delete",
                localizer.getMessage(structure.getType().getLocalizationKey()))
        );
    }

    private GuiElement relocatePowerBlockButton(AbstractStructure structure, PlayerSpigot player, char slotChar)
    {
        return new StaticGuiElement(
            slotChar,
            new ItemStack(Material.LEATHER_BOOTS),
            click ->
            {
                commandFactory
                    .newMovePowerBlock(player, structureRetrieverFactory.of(structure))
                    .run()
                    .exceptionally(Util::exceptionally);
                GuiUtil.closeAllGuis(player);
                return true;
            },
            localizer.getMessage(
                "gui.info_page.attribute.relocate_power_block",
                localizer.getMessage(structure.getType().getLocalizationKey()))
        );
    }

    private void isOpenButtonExecute(
        boolean isOpen, GuiElement.Click change, AbstractStructure structure, PlayerSpigot player)
    {
        commandFactory
            .newSetOpenStatus(player, structureRetrieverFactory.of(structure), isOpen)
            .run()
            // Force a draw with dynamic fields update to ensure the correct
            // state is displayed in case the command did not change the status.
            .thenRun(() -> executor.runOnMainThread(() -> change.getGui().draw(player.getBukkitPlayer(), true, false)))
            .exceptionally(Util::exceptionally);
    }

    private GuiElement openStatusButton(AbstractStructure structure, PlayerSpigot player, char slotChar)
    {
        final GuiStateElement element = new GuiStateElement(
            slotChar,
            () -> structure.isOpen() ? "isOpen" : "isClosed",
            new GuiStateElement.State(
                change -> isOpenButtonExecute(true, change, structure, player),
                "isOpen",
                new ItemStack(Material.WARPED_DOOR),
                localizer.getMessage(
                    "gui.info_page.attribute.set_open",
                    localizer.getMessage(structure.getType().getLocalizationKey()))
            ),
            new GuiStateElement.State(
                change -> isOpenButtonExecute(false, change, structure, player),
                "isClosed",
                new ItemStack(Material.MANGROVE_DOOR),
                localizer.getMessage(
                    "gui.info_page.attribute.set_closed",
                    localizer.getMessage(structure.getType().getLocalizationKey()))
            )
        );
        element.setState(structure.isOpen() ? "isOpen" : "isClosed");
        return element;
    }

    private void setOpenDirectionLore(
        StaticGuiElement staticGuiElement, AbstractStructure structure, MovementDirection direction)
    {
        staticGuiElement.setText(
            localizer.getMessage(
                "gui.info_page.attribute.open_direction",
                localizer.getMessage(structure.getType().getLocalizationKey())),
            textFactory
                .newText()
                .append(
                    localizer.getMessage("gui.info_page.attribute.open_direction.lore"),
                    TextComponent.EMPTY,
                    arg -> arg.info(localizer.getMessage(direction.getLocalizationKey())))
                .toString()
        );
    }

    private GuiElement openDirectionButton(AbstractStructure structure, PlayerSpigot player, char slotChar)
    {
        final AtomicReference<StaticGuiElement> staticGuiElementRef = new AtomicReference<>();

        final var staticElement = new StaticGuiElement(
            slotChar,
            new ItemStack(Material.COMPASS),
            click ->
            {
                final var newOpenDir = structure.getCycledOpenDirection();
                commandFactory
                    .newSetOpenDirection(player, StructureRetrieverFactory.ofStructure(structure), newOpenDir)
                    .run()
                    .exceptionally(Util::exceptionally);

                setOpenDirectionLore(
                    Util.requireNonNull(staticGuiElementRef.get(), "static GUI element reference"),
                    structure,
                    newOpenDir
                );

                click.getGui().draw(player.getBukkitPlayer(), true, false);

                return true;
            }
        );
        staticGuiElementRef.set(staticElement);

        setOpenDirectionLore(staticElement, structure, structure.getOpenDir());
        return staticElement;
    }

    private GuiElement blocksToMoveButton(AbstractStructure structure, PlayerSpigot player, char slotChar)
    {
        return new StaticGuiElement(
            slotChar,
            new ItemStack(Material.STICKY_PISTON),
            click ->
            {
                commandFactory
                    .getSetBlocksToMoveDelayed()
                    .runDelayed(player, structureRetrieverFactory.of(structure))
                    .exceptionally(Util::exceptionally);
                GuiUtil.closeAllGuis(player);
                return true;
            },
            localizer.getMessage(
                "gui.info_page.attribute.blocks_to_move",
                localizer.getMessage(structure.getType().getLocalizationKey()))
        );
    }

    private GuiElement addOwnerButton(AbstractStructure structure, PlayerSpigot player, char slotChar)
    {
        return new StaticGuiElement(
            slotChar,
            new ItemStack(Material.PLAYER_HEAD),
            click ->
            {
                commandFactory
                    .getAddOwnerDelayed()
                    .runDelayed(player, structureRetrieverFactory.of(structure))
                    .exceptionally(Util::exceptionally);
                GuiUtil.closeAllGuis(player);
                return true;
            },
            localizer.getMessage(
                "gui.info_page.attribute.add_owner",
                localizer.getMessage(structure.getType().getLocalizationKey()))
        );
    }

    private GuiElement removeOwnerButton(AbstractStructure structure, PlayerSpigot player, char slotChar)
    {
        return new StaticGuiElement(
            slotChar,
            new ItemStack(Material.SKELETON_SKULL),
            click ->
            {
                commandFactory
                    .getRemoveOwnerDelayed()
                    .runDelayed(player, structureRetrieverFactory.of(structure))
                    .exceptionally(Util::exceptionally);
                GuiUtil.closeAllGuis(player);
                return true;
            },
            localizer.getMessage(
                "gui.info_page.attribute.remove_owner",
                localizer.getMessage(structure.getType().getLocalizationKey()))
        );
    }

    /**
     * Creates a new GuiElement for the provided attribute.
     */
    public GuiElement of(StructureAttribute attribute, AbstractStructure structure, PlayerSpigot player, char slotChar)
    {
        return switch (attribute)
        {
            case ADD_OWNER -> this.addOwnerButton(structure, player, slotChar);
            case BLOCKS_TO_MOVE -> this.blocksToMoveButton(structure, player, slotChar);
            case DELETE -> this.deleteButton(structure, player, slotChar);
            case INFO -> this.infoButton(structure, player, slotChar);
            case LOCK -> this.lockButton(structure, player, slotChar);
            case OPEN_DIRECTION -> this.openDirectionButton(structure, player, slotChar);
            case OPEN_STATUS -> this.openStatusButton(structure, player, slotChar);
            case PREVIEW -> this.previewButton(structure, player, slotChar);
            case RELOCATE_POWERBLOCK -> this.relocatePowerBlockButton(structure, player, slotChar);
            case REMOVE_OWNER -> this.removeOwnerButton(structure, player, slotChar);
            case SWITCH -> throw new UnsupportedOperationException("Switch attribute has not been implemented yet.");
            case TOGGLE -> this.toggleButton(structure, player, slotChar);
        };
    }
}
