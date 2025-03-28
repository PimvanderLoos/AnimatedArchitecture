package nl.pim16aap2.animatedarchitecture.spigot.core.gui;

import de.themoep.inventorygui.GuiElement;
import de.themoep.inventorygui.GuiStateElement;
import de.themoep.inventorygui.StaticGuiElement;
import lombok.experimental.ExtensionMethod;
import lombok.extern.flogger.Flogger;
import nl.pim16aap2.animatedarchitecture.core.animation.AnimationType;
import nl.pim16aap2.animatedarchitecture.core.api.IExecutor;
import nl.pim16aap2.animatedarchitecture.core.api.factories.ITextFactory;
import nl.pim16aap2.animatedarchitecture.core.commands.CommandFactory;
import nl.pim16aap2.animatedarchitecture.core.events.StructureActionCause;
import nl.pim16aap2.animatedarchitecture.core.events.StructureActionType;
import nl.pim16aap2.animatedarchitecture.core.localization.ILocalizer;
import nl.pim16aap2.animatedarchitecture.core.structures.Structure;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureAnimationRequestBuilder;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureAttribute;
import nl.pim16aap2.animatedarchitecture.core.structures.properties.Property;
import nl.pim16aap2.animatedarchitecture.core.structures.retriever.StructureRetrieverFactory;
import nl.pim16aap2.animatedarchitecture.core.text.TextComponent;
import nl.pim16aap2.animatedarchitecture.core.text.TextType;
import nl.pim16aap2.animatedarchitecture.core.util.CompletableFutureExtensions;
import nl.pim16aap2.animatedarchitecture.core.util.MovementDirection;
import nl.pim16aap2.animatedarchitecture.core.util.Util;
import nl.pim16aap2.animatedarchitecture.spigot.util.implementations.WrappedPlayer;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Factory for creating buttons for the different attributes of a structure.
 */
@Flogger
@ExtensionMethod(CompletableFutureExtensions.class)
class AttributeButtonFactory
{
    /**
     * The default timeout in seconds for commands.
     */
    private static final int DEFAULT_TIMEOUT_SECONDS = 10;

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
        boolean newState,
        GuiElement.Click change,
        Structure structure,
        WrappedPlayer player)
    {
        commandFactory
            .newLock(player, structureRetrieverFactory.of(structure), newState)
            .runWithRawResult(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            // Force a draw with dynamic fields update to ensure the correct
            // state is displayed in case the command did not change the status.
            .thenRun(() -> executor.runOnMainThread(() -> change.getGui().draw(player.getBukkitPlayer(), true, false)))
            .orTimeout(1, TimeUnit.SECONDS)
            .handleExceptional(ex -> handleExceptional(ex, player, "lock_button"));
    }

    private GuiElement lockButton(Structure structure, WrappedPlayer player, char slotChar)
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

    private GuiElement toggleButton(Structure structure, WrappedPlayer player, char slotChar)
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
                    .orTimeout(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    .handleExceptional(ex -> handleExceptional(ex, player, "toggle_button"));
                return true;
            },
            localizer.getMessage(
                "gui.info_page.attribute.toggle",
                localizer.getMessage(structure.getType().getLocalizationKey()))
        );
    }

    private GuiElement previewButton(Structure structure, WrappedPlayer player, char slotChar)
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
                    .orTimeout(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    .handleExceptional(ex -> handleExceptional(ex, player, "preview_button"));
                return true;
            },
            localizer.getMessage(
                "gui.info_page.attribute.preview",
                localizer.getMessage(structure.getType().getLocalizationKey()))
        );
    }

    /**
     * Handles exceptional completion of a future.
     *
     * @param ex
     *     The exception that occurred.
     * @param player
     *     The player for which the exception occurred.
     * @param context
     *     The context in which the exception occurred. This is used for logging.
     *     <p>
     *     E.g. the action that was being performed when the exception occurred ("toggle", "lock", etc.).
     */
    private void handleExceptional(Throwable ex, WrappedPlayer player, String context)
    {
        player.sendMessage(textFactory.newText().append(
            localizer.getMessage("commands.base.error.generic"),
            TextType.ERROR
        ));
        log.atSevere().withCause(ex).log("Failed to handle action '%s' for player '%s'", context, player);
    }

    private GuiElement infoButton(Structure structure, WrappedPlayer player, char slotChar)
    {
        return new StaticGuiElement(
            slotChar,
            new ItemStack(Material.BOOKSHELF),
            click ->
            {
                commandFactory
                    .newInfo(player, structureRetrieverFactory.of(structure))
                    .runWithRawResult(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    .handleExceptional(ex -> handleExceptional(ex, player, "info_button"));
                return true;
            },
            localizer.getMessage(
                "gui.info_page.attribute.info",
                localizer.getMessage(structure.getType().getLocalizationKey()))
        );
    }

    private GuiElement deleteButton(
        Structure structure, WrappedPlayer player, char slotChar)
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

    private GuiElement relocatePowerBlockButton(Structure structure, WrappedPlayer player, char slotChar)
    {
        return new StaticGuiElement(
            slotChar,
            new ItemStack(Material.LEATHER_BOOTS),
            click ->
            {
                commandFactory
                    .newMovePowerBlock(player, structureRetrieverFactory.of(structure))
                    .runWithRawResult(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    .handleExceptional(ex -> handleExceptional(ex, player, "relocate_power_block_button"));
                GuiUtil.closeAllGuis(player);
                return true;
            },
            localizer.getMessage(
                "gui.info_page.attribute.relocate_power_block",
                localizer.getMessage(structure.getType().getLocalizationKey()))
        );
    }

    private void isOpenButtonExecute(
        boolean isOpen, GuiElement.Click change, Structure structure, WrappedPlayer player)
    {
        commandFactory
            .newSetOpenStatus(player, structureRetrieverFactory.of(structure), isOpen)
            .runWithRawResult(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            // Force a draw with dynamic fields update to ensure the correct
            // state is displayed in case the command did not change the status.
            .thenRun(() -> executor.runOnMainThread(() -> change.getGui().draw(player.getBukkitPlayer(), true, false)))
            .orTimeout(1, TimeUnit.SECONDS)
            .handleExceptional(ex -> handleExceptional(ex, player, "is_open_button"));
    }

    private @Nullable GuiElement openStatusButton(Structure structure, WrappedPlayer player, char slotChar)
    {
        final @Nullable Boolean isOpen = structure.getPropertyValue(Property.OPEN_STATUS).value();
        if (isOpen == null)
            return null;

        final GuiStateElement element = new GuiStateElement(
            slotChar,
            () -> isOpen ? "isOpen" : "isClosed",
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
        element.setState(isOpen ? "isOpen" : "isClosed");
        return element;
    }

    private void setOpenDirectionLore(
        StaticGuiElement staticGuiElement, Structure structure, MovementDirection direction)
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

    private GuiElement openDirectionButton(Structure structure, WrappedPlayer player, char slotChar)
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
                    .runWithRawResult(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    .handleExceptional(ex -> handleExceptional(ex, player, "open_direction_button"));

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

        setOpenDirectionLore(staticElement, structure, structure.getOpenDirection());
        return staticElement;
    }

    private GuiElement blocksToMoveButton(Structure structure, WrappedPlayer player, char slotChar)
    {
        return new StaticGuiElement(
            slotChar,
            new ItemStack(Material.STICKY_PISTON),
            click ->
            {
                commandFactory
                    .getSetBlocksToMoveDelayed()
                    .runDelayed(player, structureRetrieverFactory.of(structure))
                    .handleExceptional(ex -> handleExceptional(ex, player, "blocks_to_move_button"));
                GuiUtil.closeAllGuis(player);
                return true;
            },
            localizer.getMessage(
                "gui.info_page.attribute.blocks_to_move",
                localizer.getMessage(structure.getType().getLocalizationKey()))
        );
    }

    private GuiElement addOwnerButton(Structure structure, WrappedPlayer player, char slotChar)
    {
        return new StaticGuiElement(
            slotChar,
            new ItemStack(Material.PLAYER_HEAD),
            click ->
            {
                commandFactory
                    .getAddOwnerDelayed()
                    .runDelayed(player, structureRetrieverFactory.of(structure))
                    .handleExceptional(ex -> handleExceptional(ex, player, "add_owner_button"));
                GuiUtil.closeAllGuis(player);
                return true;
            },
            localizer.getMessage(
                "gui.info_page.attribute.add_owner",
                localizer.getMessage(structure.getType().getLocalizationKey()))
        );
    }

    private GuiElement removeOwnerButton(Structure structure, WrappedPlayer player, char slotChar)
    {
        return new StaticGuiElement(
            slotChar,
            new ItemStack(Material.SKELETON_SKULL),
            click ->
            {
                commandFactory
                    .getRemoveOwnerDelayed()
                    .runDelayed(player, structureRetrieverFactory.of(structure))
                    .handleExceptional(ex -> handleExceptional(ex, player, "remove_owner_button"));
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
    public @Nullable GuiElement of(
        StructureAttribute attribute,
        Structure structure,
        WrappedPlayer player,
        char slotChar)
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
