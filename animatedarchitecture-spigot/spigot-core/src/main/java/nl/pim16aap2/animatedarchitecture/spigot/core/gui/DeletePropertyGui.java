package nl.pim16aap2.animatedarchitecture.spigot.core.gui;

import dagger.assisted.Assisted;
import dagger.assisted.AssistedFactory;
import dagger.assisted.AssistedInject;
import de.themoep.inventorygui.GuiBackElement;
import de.themoep.inventorygui.GuiElementGroup;
import de.themoep.inventorygui.InventoryGui;
import de.themoep.inventorygui.StaticGuiElement;
import lombok.CustomLog;
import lombok.ToString;
import nl.pim16aap2.animatedarchitecture.core.api.IExecutor;
import nl.pim16aap2.animatedarchitecture.core.structures.PermissionLevel;
import nl.pim16aap2.animatedarchitecture.core.structures.Structure;
import nl.pim16aap2.animatedarchitecture.core.structures.properties.PropertyValuePair;
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

/**
 * Gui page for deleting a property from a structure.
 * <p>
 * This page displays a list of properties that can be deleted from the specified structure.
 * <p>
 * When a property is selected, a confirmation page is opened to confirm the deletion.
 */
@CustomLog
@ToString(onlyExplicitlyIncluded = true, callSuper = true)
class DeletePropertyGui extends AbstractGuiPage<DeletePropertyGui>
{
    private static final ItemStack FILLER = new ItemStack(Material.GRAY_STAINED_GLASS_PANE, 1);

    private static final char CH_PROPERTY = 'p';
    private static final char CH_PREVIOUS_PAGE = 'f';

    private final ConfirmDeletePropertyGui.IFactory deletePropertyGuiFactory;
    private final PropertyGuiAdapterRegistry adapterRegistry;
    private final PermissionLevel permissionLevel;
    private final BooleanConsumer onCloseCallback;

    @ToString.Include
    private final Structure structure;

    @ToString.Include
    private final Set<PropertyValuePair<?>> deletableProperties;

    @ToString.Include
    private volatile boolean propertyDeleted;

    @AssistedInject
    DeletePropertyGui(
        @Assisted Structure structure,
        @Assisted WrappedPlayer inventoryHolder,
        @Assisted PermissionLevel permissionLevel,
        @Assisted Set<PropertyValuePair<?>> deletableProperties,
        @Assisted BooleanConsumer onCloseCallback,
        AnimatedArchitecturePlugin animatedArchitecturePlugin,
        ConfirmDeletePropertyGui.IFactory deletePropertyGuiFactory,
        PropertyGuiAdapterRegistry adapterRegistry,
        IExecutor executor,
        PlayerFactorySpigot playerFactory
    )
    {
        super(
            animatedArchitecturePlugin,
            Util.requireNonNull(playerFactory.wrapPlayer(inventoryHolder), "InventoryHolder"),
            executor
        );

        this.structure = structure;
        this.permissionLevel = permissionLevel;
        this.deletableProperties = deletableProperties;
        this.onCloseCallback = onCloseCallback;

        this.deletePropertyGuiFactory = deletePropertyGuiFactory;
        this.adapterRegistry = adapterRegistry;
    }

    @Override
    protected InventoryGui createGui()
    {
        final String headerSetup = String.valueOf(CH_PREVIOUS_PAGE);
        final String[] guiSetup = GuiUtil.fillLinesWithChar(CH_PROPERTY, deletableProperties.size(), headerSetup);

        final InventoryGui gui = super.newInventoryGui(
            localizer.getMessage(
                "gui.deletable_properties_page.title",
                localizer.getMessage(structure.getType().getLocalizationKey()),
                structure.getNameAndUid()),
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
            localizer.getMessage("gui.deletable_properties_page.back_button"))
        );
    }

    private void addGuiElements(InventoryGui gui)
    {
        final GuiElementGroup group = new GuiElementGroup(CH_PROPERTY);

        final var elements = GuiUtil.getGuiElementsSortedByTitle(
            deletableProperties,
            pair -> createNamedGuiElement(CH_PROPERTY, pair)
        );
        for (final var element : elements)
        {
            group.addElement(element);
        }

        group.setFiller(FILLER);
        gui.addElement(group);
    }

    private <T> NamedGuiElement createNamedGuiElement(char slotChar, PropertyValuePair<T> propertyValuePair)
    {
        final String title = localizer.getMessage(
            "gui.deletable_properties_page.button",
            propertyValuePair.property().getTitle(localizer)
        );

        final AbstractPropertyGuiAdapter<T> adapter = adapterRegistry.getGuiAdapter(propertyValuePair.property());
        if (adapter == null)
        {
            log.atWarn().log(
                "No GUI adapter registered for property '%s'!",
                propertyValuePair.property().getFullKey()
            );
            return new NamedGuiElement(title, createErrorElement(slotChar, Objects.toString(propertyValuePair)));
        }

        final var request = new PropertyGuiRequest(
            structure,
            slotChar,
            inventoryHolder,
            permissionLevel
        );

        final Material mat = adapter.getRemovingMaterial(request);
        return new NamedGuiElement(
            title,
            new StaticGuiElement(
                slotChar,
                new ItemStack(mat),
                click ->
                {
                    deletePropertyGuiFactory.newDeletePropertyGui(
                        propertyValuePair.property(),
                        inventoryHolder,
                        structure,
                        () -> this.propertyDeleted = true
                    );
                    return true;
                },
                title
            )
        );
    }

    @Override
    protected void onGuiClosed()
    {
        this.onCloseCallback.accept(this.propertyDeleted);
    }

    /**
     * The factory interface for creating {@link DeletePropertyGui} pages.
     */
    @AssistedFactory
    interface IFactory
    {
        /**
         * Creates a new {@link DeletePropertyGui} for the given structure and player.
         *
         * @param structure
         *     The structure to delete properties from.
         * @param inventoryHolder
         *     The player who is deleting properties.
         * @param permissionLevel
         *     The permission level of the player for the structure.
         * @param deletableProperties
         *     The deletable properties.
         * @param onCloseCallback
         *     The callback to run when the GUI is closed.
         *     <p>
         *     The callback takes a boolean parameter indicating whether a property was removed.
         * @return The created {@link DeletePropertyGui} instance.
         */
        @SuppressWarnings("NullableProblems")
        DeletePropertyGui newDeletePropertyGui(
            Structure structure,
            WrappedPlayer inventoryHolder,
            PermissionLevel permissionLevel,
            Set<PropertyValuePair<?>> deletableProperties,
            BooleanConsumer onCloseCallback
        );
    }
}
