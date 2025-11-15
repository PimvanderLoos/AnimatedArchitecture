package nl.pim16aap2.animatedarchitecture.spigot.core.gui;

import dagger.assisted.Assisted;
import dagger.assisted.AssistedFactory;
import dagger.assisted.AssistedInject;
import de.themoep.inventorygui.GuiBackElement;
import de.themoep.inventorygui.GuiElement;
import de.themoep.inventorygui.GuiElementGroup;
import de.themoep.inventorygui.InventoryGui;
import lombok.CustomLog;
import lombok.Getter;
import lombok.ToString;
import nl.pim16aap2.animatedarchitecture.core.localization.ILocalizer;
import nl.pim16aap2.animatedarchitecture.core.structures.PermissionLevel;
import nl.pim16aap2.animatedarchitecture.core.structures.Structure;
import nl.pim16aap2.animatedarchitecture.core.structures.properties.PropertyValuePair;
import nl.pim16aap2.animatedarchitecture.spigot.core.AnimatedArchitecturePlugin;
import nl.pim16aap2.animatedarchitecture.spigot.core.managers.PropertyGuiAdapterRegistry;
import nl.pim16aap2.animatedarchitecture.spigot.core.propertyadapters.guiadapters.AbstractPropertyGuiAdapter;
import nl.pim16aap2.animatedarchitecture.spigot.core.propertyadapters.guiadapters.PropertyGuiRequest;
import nl.pim16aap2.animatedarchitecture.spigot.util.implementations.WrappedPlayer;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Objects;

@CustomLog
@ToString(onlyExplicitlyIncluded = true)
class DeletePropertyGui implements IGuiPage
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
    private final List<PropertyValuePair<?>> deletableProperties;

    @Getter
    @ToString.Include
    private final WrappedPlayer inventoryHolder;

    @AssistedInject
    DeletePropertyGui(
        @Assisted Structure structure,
        @Assisted WrappedPlayer inventoryHolder,
        @Assisted PermissionLevel permissionLevel,
        @Assisted List<PropertyValuePair<?>> deletableProperties,
        AnimatedArchitecturePlugin animatedArchitecturePlugin,
        ILocalizer localizer,
        PropertyGuiAdapterRegistry adapterRegistry)
    {
        this.animatedArchitecturePlugin = animatedArchitecturePlugin;
        this.localizer = localizer;
        this.adapterRegistry = adapterRegistry;
        this.structure = structure;
        this.inventoryHolder = inventoryHolder;
        this.permissionLevel = permissionLevel;
        this.deletableProperties = deletableProperties;

        this.inventoryGui = createGUI();
        showGUI();
    }

    private void showGUI()
    {
        inventoryGui.show(inventoryHolder.getBukkitPlayer());
    }

    private InventoryGui createGUI()
    {
        final String[] guiSetup = GuiUtil.fillLinesWithChar('p', deletableProperties.size(), "f");

        final InventoryGui gui = new InventoryGui(
            animatedArchitecturePlugin,
            inventoryHolder.getBukkitPlayer(),
            localizer.getMessage(
                "gui.delete_property_page.title",
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
        final GuiElementGroup group = new GuiElementGroup('p');
        for (final var propertyValuePair : deletableProperties)
        {
            addPropertyGuiElement('p', group, propertyValuePair);
        }
        group.setFiller(FILLER);
        gui.addElement(group);

        gui.addElement(new GuiBackElement(
            'f',
            new ItemStack(Material.ARROW),
            localizer.getMessage("gui.delete_property_page.back_button"))
        );
    }

    private void addPropertyGuiElement(char slotChar, GuiElementGroup group, PropertyValuePair<?> propertyValuePair)
    {
        GuiElement guiElement = createPropertyElement(slotChar, propertyValuePair);
        if (guiElement == null)
        {
            guiElement = createErrorElement(slotChar, localizer, Objects.toString(propertyValuePair));
        }
        group.addElement(guiElement);
    }

    private @Nullable <T> GuiElement createPropertyElement(char slotChar, PropertyValuePair<T> propertyValuePair)
    {
        final AbstractPropertyGuiAdapter<T> adapter = adapterRegistry.getGuiAdapter(propertyValuePair.property());
        if (adapter == null)
        {
            log.atWarn().log(
                "No GUI adapter registered for property '%s'!",
                propertyValuePair.property().getFullKey()
            );
            return null;
        }

        final var request = new PropertyGuiRequest<T>(
            structure,
            slotChar,
            inventoryHolder,
            permissionLevel
        );

        return adapter.createGuiElement(request);
    }

    @Override
    public String getPageName()
    {
        return "DeletePropertyGui";
    }

    @AssistedFactory
    interface IFactory
    {
        /**
         * Creates and opens a new {@link DeletePropertyGui} for the given structure and player.
         *
         * @param structure
         *     The structure to delete properties from.
         * @param inventoryHolder
         *     The player who is deleting properties.
         * @param permissionLevel
         *     The permission level of the player for the structure.
         * @param deletableProperties
         *     The list of deletable properties.
         * @return The created {@link DeletePropertyGui} instance.
         */
        @SuppressWarnings("NullableProblems")
        DeletePropertyGui newDeletePropertyGui(
            Structure structure,
            WrappedPlayer inventoryHolder,
            PermissionLevel permissionLevel,
            List<PropertyValuePair<?>> deletableProperties
        );
    }
}
