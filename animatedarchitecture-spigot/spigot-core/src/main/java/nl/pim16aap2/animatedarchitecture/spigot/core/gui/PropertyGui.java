package nl.pim16aap2.animatedarchitecture.spigot.core.gui;

import dagger.assisted.Assisted;
import dagger.assisted.AssistedFactory;
import dagger.assisted.AssistedInject;
import de.themoep.inventorygui.GuiBackElement;
import de.themoep.inventorygui.GuiElementGroup;
import de.themoep.inventorygui.InventoryGui;
import de.themoep.inventorygui.StaticGuiElement;
import lombok.AccessLevel;
import lombok.CustomLog;
import lombok.Getter;
import lombok.ToString;
import nl.pim16aap2.animatedarchitecture.core.localization.ILocalizer;
import nl.pim16aap2.animatedarchitecture.core.structures.Structure;
import nl.pim16aap2.animatedarchitecture.core.structures.properties.PropertyAccessLevel;
import nl.pim16aap2.animatedarchitecture.core.structures.properties.PropertyValuePair;
import nl.pim16aap2.animatedarchitecture.spigot.core.AnimatedArchitecturePlugin;
import nl.pim16aap2.animatedarchitecture.spigot.util.implementations.WrappedPlayer;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * GUI for viewing structure properties.
 */
@CustomLog
@ToString(onlyExplicitlyIncluded = true)
public final class PropertyGui implements IGuiPage
{
    private static final ItemStack FILLER = new ItemStack(Material.GRAY_STAINED_GLASS_PANE, 1);

    private final AnimatedArchitecturePlugin animatedArchitecturePlugin;
    private final ILocalizer localizer;
    private final PropertyGuiAdapterRegistrySpigot adapterRegistry;

    @Getter
    private final InventoryGui inventoryGui;

    @ToString.Include
    @Getter(AccessLevel.PACKAGE)
    private final Structure structure;

    @Getter
    @ToString.Include
    private final WrappedPlayer inventoryHolder;

    @ToString.Include
    private final List<PropertyValuePair<?>> visibleProperties;

    @AssistedInject
    PropertyGui(
        @Assisted Structure structure,
        @Assisted WrappedPlayer inventoryHolder,
        AnimatedArchitecturePlugin animatedArchitecturePlugin,
        ILocalizer localizer,
        PropertyGuiAdapterRegistrySpigot adapterRegistry)
    {
        this.animatedArchitecturePlugin = animatedArchitecturePlugin;
        this.localizer = localizer;
        this.adapterRegistry = adapterRegistry;
        this.structure = structure;
        this.inventoryHolder = inventoryHolder;

        this.visibleProperties = structure
            .getPropertiesForOwnerFilteredByAccessLevel(inventoryHolder.getUUID(), PropertyAccessLevel.READ)
            .stream()
            .toList();

        this.inventoryGui = createGUI();

        showGUI();
    }

    private void showGUI()
    {
        inventoryGui.show(inventoryHolder.getBukkitPlayer());
    }

    private InventoryGui createGUI()
    {
        final String[] guiSetup = GuiUtil.fillLinesWithChar('p', visibleProperties.size(), "f   h    ");

        final InventoryGui gui = new InventoryGui(
            animatedArchitecturePlugin,
            inventoryHolder.getBukkitPlayer(),
            localizer.getMessage("gui.property_page.title", structure.getNameAndUid()),
            guiSetup
        );
        gui.setFiller(FILLER);

        populateGUI(gui);

        return gui;
    }

    private void populateGUI(InventoryGui gui)
    {
        addHeader(gui);
        addPropertyElements(gui);
    }

    private void addHeader(InventoryGui gui)
    {
        gui.addElement(new StaticGuiElement(
            'h',
            new ItemStack(Material.WRITABLE_BOOK),
            localizer.getMessage(
                "gui.property_page.header",
                structure.getNameAndUid()))
        );

        gui.addElement(new GuiBackElement(
            'f',
            new ItemStack(Material.ARROW),
            localizer.getMessage("gui.property_page.back_button"))
        );
    }

    private void addPropertyElements(InventoryGui gui)
    {
        final GuiElementGroup group = new GuiElementGroup('p');

        for (final PropertyValuePair<?> propertyValuePair : visibleProperties)
        {
            final var adapter = adapterRegistry.getAdapter(propertyValuePair.property());
            if (adapter == null)
            {
                log.atWarn().log(
                    "No GUI adapter registered for property '%s'. Skipping.",
                    propertyValuePair.property().getFullKey()
                );
                continue;
            }

            final ItemStack itemStack = createPropertyItemStack(adapter, propertyValuePair);
            group.addElement(new StaticGuiElement('p', itemStack));
        }

        group.setFiller(FILLER);
        gui.addElement(group);
    }

    @SuppressWarnings("unchecked")
    private <T> ItemStack createPropertyItemStack(
        IPropertyGuiAdapterSpigot<?> adapter,
        PropertyValuePair<T> propertyValuePair)
    {
        return ((IPropertyGuiAdapterSpigot<T>) adapter).createItemStack(
            propertyValuePair.value(),
            inventoryHolder
        );
    }

    @Override
    public String getPageName()
    {
        return "PropertyGui";
    }

    @AssistedFactory
    interface IFactory
    {
        PropertyGui newPropertyGUI(Structure structure, WrappedPlayer playerSpigot);
    }
}
