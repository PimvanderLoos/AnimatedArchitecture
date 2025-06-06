package nl.pim16aap2.animatedarchitecture.spigot.core.gui;

import dagger.assisted.Assisted;
import dagger.assisted.AssistedFactory;
import dagger.assisted.AssistedInject;
import de.themoep.inventorygui.GuiBackElement;
import de.themoep.inventorygui.GuiElement;
import de.themoep.inventorygui.GuiElementGroup;
import de.themoep.inventorygui.InventoryGui;
import de.themoep.inventorygui.StaticGuiElement;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.ExtensionMethod;
import lombok.extern.flogger.Flogger;
import nl.pim16aap2.animatedarchitecture.core.api.IPermissionsManager;
import nl.pim16aap2.animatedarchitecture.core.api.factories.ITextFactory;
import nl.pim16aap2.animatedarchitecture.core.commands.CommandFactory;
import nl.pim16aap2.animatedarchitecture.core.localization.PersonalizedLocalizer;
import nl.pim16aap2.animatedarchitecture.core.managers.StructureTypeManager;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureType;
import nl.pim16aap2.animatedarchitecture.core.text.TextArgument;
import nl.pim16aap2.animatedarchitecture.core.util.CompletableFutureExtensions;
import nl.pim16aap2.animatedarchitecture.spigot.core.AnimatedArchitecturePlugin;
import nl.pim16aap2.animatedarchitecture.spigot.util.implementations.WrappedPlayer;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * Gui page for creating new structures.
 */
@Flogger
@ToString(onlyExplicitlyIncluded = true)
@ExtensionMethod(CompletableFutureExtensions.class)
class CreateStructureGui implements IGuiPage
{
    private static final ItemStack FILLER = new ItemStack(Material.GRAY_STAINED_GLASS_PANE, 1);

    private final AnimatedArchitecturePlugin animatedArchitecturePlugin;
    private final StructureTypeManager structureTypeManager;
    private final InventoryGui inventoryGui;
    private final IPermissionsManager permissionsManager;
    private final CommandFactory commandFactory;

    @Getter
    @ToString.Include
    private final WrappedPlayer inventoryHolder;

    @ToString.Include
    private final PersonalizedLocalizer localizer;

    @AssistedInject
    CreateStructureGui(
        AnimatedArchitecturePlugin animatedArchitecturePlugin,
        StructureTypeManager structureTypeManager,
        IPermissionsManager permissionsManager,
        CommandFactory commandFactory,
        @Assisted WrappedPlayer inventoryHolder)
    {
        this.animatedArchitecturePlugin = animatedArchitecturePlugin;
        this.structureTypeManager = structureTypeManager;
        this.permissionsManager = permissionsManager;
        this.commandFactory = commandFactory;
        this.inventoryHolder = inventoryHolder;
        this.localizer = inventoryHolder.getPersonalizedLocalizer();

        this.inventoryGui = createGui();

        showGUI();
    }

    private InventoryGui createGui()
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
                            log.atSevere().withCause(ex).log("Failed to delete structure.");
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

    private void showGUI()
    {
        inventoryGui.show(inventoryHolder.getBukkitPlayer());
    }

    @Override
    public String getPageName()
    {
        return "CreateStructureGui";
    }

    @AssistedFactory
    interface IFactory
    {
        CreateStructureGui newCreateStructureGui(WrappedPlayer playerSpigot);
    }
}
