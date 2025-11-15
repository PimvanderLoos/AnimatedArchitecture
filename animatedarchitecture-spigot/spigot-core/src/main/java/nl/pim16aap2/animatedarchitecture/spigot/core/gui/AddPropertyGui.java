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
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.ExtensionMethod;
import nl.pim16aap2.animatedarchitecture.core.commands.CommandFactory;
import nl.pim16aap2.animatedarchitecture.core.localization.ILocalizer;
import nl.pim16aap2.animatedarchitecture.core.structures.PermissionLevel;
import nl.pim16aap2.animatedarchitecture.core.structures.Structure;
import nl.pim16aap2.animatedarchitecture.core.structures.properties.Property;
import nl.pim16aap2.animatedarchitecture.core.structures.retriever.StructureRetrieverFactory;
import nl.pim16aap2.animatedarchitecture.core.util.CompletableFutureExtensions;
import nl.pim16aap2.animatedarchitecture.spigot.core.AnimatedArchitecturePlugin;
import nl.pim16aap2.animatedarchitecture.spigot.core.managers.PropertyGuiAdapterRegistry;
import nl.pim16aap2.animatedarchitecture.spigot.core.propertyadapters.guiadapters.AbstractPropertyGuiAdapter;
import nl.pim16aap2.animatedarchitecture.spigot.core.propertyadapters.guiadapters.PropertyGuiRequest;
import nl.pim16aap2.animatedarchitecture.spigot.util.implementations.WrappedPlayer;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static nl.pim16aap2.animatedarchitecture.core.util.Constants.DEFAULT_COMMAND_TIMEOUT_SECONDS;

@CustomLog
@ToString(onlyExplicitlyIncluded = true)
@ExtensionMethod(CompletableFutureExtensions.class)
class AddPropertyGui implements IGuiPage
{
    private static final ItemStack FILLER = new ItemStack(Material.GRAY_STAINED_GLASS_PANE, 1);

    private final AnimatedArchitecturePlugin animatedArchitecturePlugin;
    private final ILocalizer localizer;
    private final PropertyGuiAdapterRegistry adapterRegistry;
    private final PermissionLevel permissionLevel;

    @Getter
    private final InventoryGui inventoryGui;

    @ToString.Include
    private final Structure structure;

    @ToString.Include
    private final List<Property<?>> addableProperties;

    private final StructureRetrieverFactory structureRetrieverFactory;

    private final CommandFactory commandFactory;

    @Getter
    @ToString.Include
    private final WrappedPlayer inventoryHolder;

    @AssistedInject
    AddPropertyGui(
        @Assisted Structure structure,
        @Assisted WrappedPlayer inventoryHolder,
        @Assisted PermissionLevel permissionLevel,
        @Assisted List<Property<?>> addableProperties,
        AnimatedArchitecturePlugin animatedArchitecturePlugin,
        ILocalizer localizer,
        PropertyGuiAdapterRegistry adapterRegistry,
        StructureRetrieverFactory structureRetrieverFactory,
        CommandFactory commandFactory
    )
    {
        this.animatedArchitecturePlugin = animatedArchitecturePlugin;
        this.localizer = localizer;
        this.adapterRegistry = adapterRegistry;
        this.structure = structure;
        this.inventoryHolder = inventoryHolder;
        this.permissionLevel = permissionLevel;
        this.addableProperties = addableProperties;
        this.structureRetrieverFactory = structureRetrieverFactory;
        this.commandFactory = commandFactory;

        this.inventoryGui = createGUI();
        showGUI();
    }

    private void showGUI()
    {
        inventoryGui.show(inventoryHolder.getBukkitPlayer());
    }

    private InventoryGui createGUI()
    {
        final String[] guiSetup = GuiUtil.fillLinesWithChar('p', addableProperties.size(), "f");

        final InventoryGui gui = new InventoryGui(
            animatedArchitecturePlugin,
            inventoryHolder.getBukkitPlayer(),
            localizer.getMessage(
                "gui.add_property_page.title",
                localizer.getMessage(structure.getType().getLocalizationKey()),
                structure.getNameAndUid()
            ),
            guiSetup
        );

        gui.setFiller(FILLER);

        populateGUI(gui);

        return gui;
    }

    private void populateGUI(InventoryGui gui)
    {
        final GuiElementGroup group = new GuiElementGroup('p');
        for (final var adapter : addableProperties)
        {
            addPropertyGuiElement('p', group, adapter);
        }
        group.setFiller(FILLER);
        gui.addElement(group);

        gui.addElement(new GuiBackElement(
            'f',
            new ItemStack(Material.ARROW),
            localizer.getMessage("gui.add_property_page.back_button"))
        );
    }

    private <T> void addPropertyGuiElement(char slotChar, GuiElementGroup group, Property<T> property)
    {
        final AbstractPropertyGuiAdapter<T> adapter = adapterRegistry.getGuiAdapter(property);
        GuiElement guiElement;
        if (adapter != null)
        {
            guiElement = createPropertyElement(slotChar, adapter);
        }
        else
        {
            log.atError().log("No GUI adapter found for property: %s", property.getNamespacedKey());
            guiElement = createErrorElement(
                slotChar,
                localizer,
                "No GUI adapter found for property: " + property
            );
        }
        group.addElement(guiElement);
    }

    private <T> GuiElement createPropertyElement(char slotChar, AbstractPropertyGuiAdapter<T> adapter)
    {
        final var request = new PropertyGuiRequest<T>(
            structure,
            slotChar,
            inventoryHolder,
            permissionLevel
        );

        final Material material = adapter.getAddingMaterial(request);

        return new StaticGuiElement(
            slotChar,
            new ItemStack(material),
            click ->
            {
                commandFactory
                    .newSetProperty(
                        inventoryHolder,
                        structureRetrieverFactory.of(structure),
                        adapter.getProperty(),
                        adapter.getProperty().getDefaultValue()
                    ).runWithRawResult(DEFAULT_COMMAND_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    .handleExceptional(ex -> handleExceptional(
                        ex,
                        inventoryHolder,
                        "adding property " + adapter.getProperty())
                    );
                return true;
            },
            localizer.getMessage(
                "gui.add_property_page.add_property_button",
                adapter.getProperty().getTitle(localizer))
        );
    }

    @Override
    public String getPageName()
    {
        return "AddPropertyGui";
    }

    @AssistedFactory
    interface IFactory
    {
        /**
         * Creates and opens a new {@link AddPropertyGui} for the given structure and player.
         *
         * @param structure
         *     The structure to add properties to.
         * @param inventoryHolder
         *     The player who is adding properties.
         * @param permissionLevel
         *     The permission level of the player for the structure.
         * @param addableProperties
         *     The list of properties that can be added.
         * @return The created {@link AddPropertyGui} instance.
         */
        @SuppressWarnings("NullableProblems")
        AddPropertyGui newAddPropertyGui(
            Structure structure,
            WrappedPlayer inventoryHolder,
            PermissionLevel permissionLevel,
            List<Property<?>> addableProperties
        );
    }
}
