package nl.pim16aap2.animatedarchitecture.spigot.core.gui;

import dagger.assisted.Assisted;
import dagger.assisted.AssistedFactory;
import dagger.assisted.AssistedInject;
import de.themoep.inventorygui.GuiBackElement;
import de.themoep.inventorygui.GuiElement;
import de.themoep.inventorygui.GuiElementGroup;
import de.themoep.inventorygui.InventoryGui;
import de.themoep.inventorygui.StaticGuiElement;
import lombok.CustomLog;
import lombok.ToString;
import lombok.experimental.ExtensionMethod;
import nl.pim16aap2.animatedarchitecture.core.api.IExecutor;
import nl.pim16aap2.animatedarchitecture.core.api.IPermissionsManager;
import nl.pim16aap2.animatedarchitecture.core.api.factories.ITextFactory;
import nl.pim16aap2.animatedarchitecture.core.commands.CommandFactory;
import nl.pim16aap2.animatedarchitecture.core.managers.StructureTypeManager;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureType;
import nl.pim16aap2.animatedarchitecture.core.text.TextArgument;
import nl.pim16aap2.animatedarchitecture.core.util.CompletableFutureExtensions;
import nl.pim16aap2.animatedarchitecture.core.util.Util;
import nl.pim16aap2.animatedarchitecture.spigot.core.AnimatedArchitecturePlugin;
import nl.pim16aap2.animatedarchitecture.spigot.core.implementations.PlayerFactorySpigot;
import nl.pim16aap2.animatedarchitecture.spigot.util.implementations.WrappedPlayer;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * Gui page for creating new structures.
 */
@CustomLog
@ToString(onlyExplicitlyIncluded = true, callSuper = true)
@ExtensionMethod(CompletableFutureExtensions.class)
class CreateStructureGui extends AbstractGuiPage<CreateStructureGui>
{
    private static final ItemStack FILLER = new ItemStack(Material.GRAY_STAINED_GLASS_PANE, 1);

    private final StructureTypeManager structureTypeManager;
    private final IPermissionsManager permissionsManager;
    private final CommandFactory commandFactory;

    @AssistedInject
    CreateStructureGui(
        @Assisted WrappedPlayer inventoryHolder,
        AnimatedArchitecturePlugin animatedArchitecturePlugin,
        StructureTypeManager structureTypeManager,
        IPermissionsManager permissionsManager,
        CommandFactory commandFactory,
        IExecutor executor,
        PlayerFactorySpigot playerFactory
    )
    {
        super(
            animatedArchitecturePlugin,
            Util.requireNonNull(playerFactory.wrapPlayer(inventoryHolder), "InventoryHolder"),
            executor
        );

        this.structureTypeManager = structureTypeManager;
        this.permissionsManager = permissionsManager;
        this.commandFactory = commandFactory;
    }

    @Override
    protected InventoryGui createGui()
    {
        final var types = structureTypeManager
            .getEnabledStructureTypes()
            .stream()
            .filter(type -> permissionsManager.hasPermissionToCreateStructure(inventoryHolder, type))
            .toList();

        final String[] guiSetup = GuiUtil.fillLinesWithChar('g', types.size(), "f        ");

        final InventoryGui gui = new InventoryGui(
            animatedArchitecturePlugin,
            inventoryHolder.getBukkitPlayer(),
            localizer.getMessage("gui.new_structure_page.title"),
            guiSetup
        );

        gui.setFiller(FILLER);

        populateGUI(gui, types);

        return gui;
    }

    private void populateGUI(InventoryGui gui, List<StructureType> types)
    {
        addHeader(gui);
        addElements(gui, types);
    }

    private void addHeader(InventoryGui gui)
    {
        gui.addElement(new GuiBackElement(
            'f',
            new ItemStack(Material.ARROW),
            localizer.getMessage("gui.new_structure_page.back_button"))
        );
    }

    private void addElements(InventoryGui gui, List<StructureType> types)
    {
        final GuiElementGroup group = new GuiElementGroup('g');
        for (final var type : types)
        {
            final GuiElement element = new StaticGuiElement(
                'g',
                new ItemStack(Material.WRITABLE_BOOK),
                click ->
                {
                    commandFactory
                        .newNewStructure(inventoryHolder, type)
                        .run()
                        .handleExceptional(ex ->
                        {
                            inventoryHolder.sendError("constants.error.generic");
                            log.atError().withCause(ex).log("Failed to delete structure.");
                        });
                    GuiUtil.closeAllGuis(inventoryHolder);
                    return true;
                },
                ITextFactory.getSimpleTextFactory()
                    .newText(inventoryHolder.getPersonalizedLocalizer())
                    .append(
                        localizer.getMessage("gui.new_structure_page.button.name"),
                        new TextArgument(localizer.getMessage(type.getLocalizationKey())))
                    .toString()
            );
            group.addElement(element);
        }
        group.setFiller(FILLER);
        gui.addElement(group);
    }

    /**
     * The factory interface for creating {@link CreateStructureGui} instances.
     */
    @AssistedFactory
    interface IFactory
    {
        /**
         * Creates and opens a new {@link CreateStructureGui} page for the given player.
         *
         * @param playerSpigot
         *     The player to open the GUI for.
         * @return The created {@link CreateStructureGui} instance.
         */
        @SuppressWarnings("NullableProblems")
        CreateStructureGui newCreateStructureGui(WrappedPlayer playerSpigot);
    }
}
