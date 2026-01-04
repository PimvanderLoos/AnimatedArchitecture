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
import nl.pim16aap2.animatedarchitecture.core.commands.CommandFactory;
import nl.pim16aap2.animatedarchitecture.core.structures.PermissionLevel;
import nl.pim16aap2.animatedarchitecture.core.structures.Structure;
import nl.pim16aap2.animatedarchitecture.core.structures.properties.Property;
import nl.pim16aap2.animatedarchitecture.core.structures.retriever.StructureRetrieverFactory;
import nl.pim16aap2.animatedarchitecture.core.util.CompletableFutureExtensions;
import nl.pim16aap2.animatedarchitecture.core.util.Util;
import nl.pim16aap2.animatedarchitecture.core.util.functional.BooleanConsumer;
import nl.pim16aap2.animatedarchitecture.spigot.core.AnimatedArchitecturePlugin;
import nl.pim16aap2.animatedarchitecture.spigot.core.implementations.PlayerFactorySpigot;
import nl.pim16aap2.animatedarchitecture.spigot.core.managers.PropertyGuiAdapterRegistry;
import nl.pim16aap2.animatedarchitecture.spigot.core.propertyadapters.guiadapters.AbstractPropertyGuiAdapter;
import nl.pim16aap2.animatedarchitecture.spigot.core.propertyadapters.guiadapters.PropertyGuiRequest;
import nl.pim16aap2.animatedarchitecture.spigot.util.implementations.WrappedPlayer;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static nl.pim16aap2.animatedarchitecture.core.util.Constants.DEFAULT_COMMAND_TIMEOUT_SECONDS;

/**
 * Represents a GUI page for adding a property to a structure.
 * <p>
 * This page displays a list of properties that can be added to the specified structure.
 * <p>
 * When a property is selected, it is added to the structure using the appropriate command with a default value.
 */
@CustomLog
@ToString(onlyExplicitlyIncluded = true, callSuper = true)
@ExtensionMethod(CompletableFutureExtensions.class)
class AddPropertyGui extends AbstractGuiPage<AddPropertyGui>
{
    private static final ItemStack FILLER = new ItemStack(Material.GRAY_STAINED_GLASS_PANE, 1);

    private static final char CH_PROPERTY = 'p';
    private static final char CH_PREVIOUS_PAGE = 'f';

    private final PropertyGuiAdapterRegistry adapterRegistry;
    private final PermissionLevel permissionLevel;
    private final BooleanConsumer onCloseCallback;

    @ToString.Include
    private final Structure structure;

    @ToString.Include
    private final Set<Property<?>> addableProperties;

    private final StructureRetrieverFactory structureRetrieverFactory;

    private final CommandFactory commandFactory;

    @ToString.Include
    private volatile boolean propertyAdded;

    @AssistedInject
    AddPropertyGui(
        @Assisted Structure structure,
        @Assisted WrappedPlayer inventoryHolder,
        @Assisted PermissionLevel permissionLevel,
        @Assisted Set<Property<?>> addableProperties,
        @Assisted BooleanConsumer onCloseCallback,
        AnimatedArchitecturePlugin animatedArchitecturePlugin,
        IExecutor executor,
        PlayerFactorySpigot playerFactory,
        PropertyGuiAdapterRegistry adapterRegistry,
        StructureRetrieverFactory structureRetrieverFactory,
        CommandFactory commandFactory
    )
    {
        super(
            animatedArchitecturePlugin,
            Util.requireNonNull(playerFactory.wrapPlayer(inventoryHolder), "InventoryHolder"),
            executor
        );

        this.structure = structure;
        this.permissionLevel = permissionLevel;
        this.addableProperties = addableProperties;
        this.onCloseCallback = onCloseCallback;

        this.adapterRegistry = adapterRegistry;
        this.structureRetrieverFactory = structureRetrieverFactory;
        this.commandFactory = commandFactory;
    }

    @Override
    protected InventoryGui createGui()
    {
        String headerSetup = String.valueOf(CH_PREVIOUS_PAGE);
        final String[] guiSetup = GuiUtil.fillLinesWithChar(CH_PROPERTY, addableProperties.size(), headerSetup);

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
        addHeader(gui);
        addGuiElements(gui);
    }

    private void addHeader(InventoryGui gui)
    {
        gui.addElement(new GuiBackElement(
            CH_PREVIOUS_PAGE,
            new ItemStack(Material.ARROW),
            localizer.getMessage("gui.add_property_page.back_button"))
        );
    }

    private void addGuiElements(InventoryGui gui)
    {
        final GuiElementGroup group = new GuiElementGroup(CH_PROPERTY);

        final var elements = GuiUtil.getGuiElementsSortedByTitle(
            addableProperties,
            property -> createNamedGuiElement(gui, CH_PROPERTY, property)
        );
        elements.forEach(group::addElement);

        group.setFiller(FILLER);
        gui.addElement(group);
    }

    private <T> NamedGuiElement createNamedGuiElement(InventoryGui gui, char slotChar, Property<T> property)
    {
        final String title = localizer.getMessage(
            "gui.add_property_page.add_property_button",
            property.getTitle(localizer)
        );

        final AbstractPropertyGuiAdapter<T> adapter = adapterRegistry.getGuiAdapter(property);
        if (adapter == null)
        {
            log.atWarn().log(
                "No GUI adapter registered for property '%s'!",
                property.getFullKey()
            );
            return new NamedGuiElement(title, createErrorElement(slotChar, Objects.toString(property)));
        }

        final var request = new PropertyGuiRequest<T>(
            structure,
            slotChar,
            inventoryHolder,
            permissionLevel
        );

        final Material material = adapter.getAddingMaterial(request);
        return new NamedGuiElement(
            title,
            new StaticGuiElement(
                slotChar,
                new ItemStack(material),
                getClickActionForProperty(gui, adapter),
                title
            )
        );
    }

    private GuiElement.Action getClickActionForProperty(InventoryGui gui, AbstractPropertyGuiAdapter<?> adapter)
    {
        return click ->
        {
            commandFactory
                .newSetProperty(
                    inventoryHolder,
                    structureRetrieverFactory.of(structure),
                    adapter.getProperty(),
                    adapter.getProperty().getDefaultValue()
                ).runWithRawResult(DEFAULT_COMMAND_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .thenRun(() -> this.propertyAdded = true)
                .thenRun(() -> GuiUtil.closeGuiPage(gui, inventoryHolder))
                .handleExceptional(ex ->
                    {
                        inventoryHolder.sendGenericErrorMessage();
                        log.atError().withCause(ex).log(
                            "Failed to add property '%s' for player '%s' to structure '%s'.",
                            adapter.getProperty(),
                            inventoryHolder,
                            structure.getBasicInfo()
                        );
                    }
                );
            return true;
        };
    }

    @Override
    protected void onGuiClosed()
    {
        this.onCloseCallback.accept(this.propertyAdded);
    }

    /**
     * The factory interface for creating {@link AddPropertyGui} pages.
     */
    @AssistedFactory
    interface IFactory
    {
        /**
         * Creates a new {@link AddPropertyGui} for the given structure and player.
         *
         * @param structure
         *     The structure to add properties to.
         * @param inventoryHolder
         *     The player who is adding properties.
         * @param permissionLevel
         *     The permission level of the player for the structure.
         * @param addableProperties
         *     The properties that can be added.
         * @param onCloseCallback
         *     The callback to run when the GUI is closed.
         *     <p>
         *     The callback takes a boolean parameter indicating whether a property was added.
         * @return The created {@link AddPropertyGui} instance.
         */
        @SuppressWarnings("NullableProblems")
        AddPropertyGui newAddPropertyGui(
            Structure structure,
            WrappedPlayer inventoryHolder,
            PermissionLevel permissionLevel,
            Set<Property<?>> addableProperties,
            BooleanConsumer onCloseCallback
        );
    }
}
