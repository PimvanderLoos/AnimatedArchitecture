package nl.pim16aap2.animatedarchitecture.spigot.core.gui;

import com.google.common.flogger.StackSize;
import dagger.assisted.Assisted;
import dagger.assisted.AssistedFactory;
import dagger.assisted.AssistedInject;
import de.themoep.inventorygui.GuiBackElement;
import de.themoep.inventorygui.GuiElement;
import de.themoep.inventorygui.GuiElementGroup;
import de.themoep.inventorygui.InventoryGui;
import de.themoep.inventorygui.StaticGuiElement;
import lombok.AccessLevel;
import lombok.CustomLog;
import lombok.Getter;
import lombok.ToString;
import nl.pim16aap2.animatedarchitecture.core.api.IPermissionsManager;
import nl.pim16aap2.animatedarchitecture.core.localization.ILocalizer;
import nl.pim16aap2.animatedarchitecture.core.structures.IStructureConst;
import nl.pim16aap2.animatedarchitecture.core.structures.PermissionLevel;
import nl.pim16aap2.animatedarchitecture.core.structures.Structure;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureAttribute;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureOwner;
import nl.pim16aap2.animatedarchitecture.core.structures.properties.PropertyAccessLevel;
import nl.pim16aap2.animatedarchitecture.core.structures.properties.PropertyValuePair;
import nl.pim16aap2.animatedarchitecture.core.util.CollectionsUtil;
import nl.pim16aap2.animatedarchitecture.spigot.core.AnimatedArchitecturePlugin;
import nl.pim16aap2.animatedarchitecture.spigot.util.implementations.WrappedPlayer;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents the info menu for a structure.
 * <p>
 * This contains all the options for a single structure.
 */
@CustomLog
@ToString(onlyExplicitlyIncluded = true)
class InfoGui implements IGuiPage
{
    private static final ItemStack FILLER = new ItemStack(Material.GRAY_STAINED_GLASS_PANE, 1);

    private static final int MAX_PROPERTY_SLOTS = 27;

    private final AnimatedArchitecturePlugin animatedArchitecturePlugin;
    private final ILocalizer localizer;
    private final AttributeButtonFactory attributeButtonFactory;
    private final PropertyGuiAdapterRegistrySpigot adapterRegistry;

    @Getter
    private final InventoryGui inventoryGui;

    @ToString.Include
    @Getter(AccessLevel.PACKAGE)
    private final Structure structure;

    @ToString.Include
    private final List<StructureAttribute> allowedAttributes;

    @ToString.Include
    private final List<PropertyValuePair<?>> visibleProperties;

    @Getter
    @ToString.Include
    private final WrappedPlayer inventoryHolder;

    // Currently not used, but it's needed later on when we can update the elements
    // When the field in the structure changes.
    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
    private final Map<StructureAttribute, GuiElement> attributeElements;

    /**
     * Until we implement pagination for the properties, we do not show any properties at all if we cannot fit them all
     * in the GUI.
     * <p>
     * While not ideal, this is preferable to failing silently.
     */
    private final boolean canFitProperties;

    @AssistedInject
    InfoGui(
        @Assisted Structure structure,
        @Assisted WrappedPlayer inventoryHolder,
        AnimatedArchitecturePlugin animatedArchitecturePlugin,
        ILocalizer localizer,
        IPermissionsManager permissionsManager,
        AttributeButtonFactory attributeButtonFactory,
        PropertyGuiAdapterRegistrySpigot adapterRegistry)
    {
        this.animatedArchitecturePlugin = animatedArchitecturePlugin;
        this.localizer = localizer;
        this.attributeButtonFactory = attributeButtonFactory;
        this.adapterRegistry = adapterRegistry;
        this.structure = structure;
        this.inventoryHolder = inventoryHolder;

        final StructureOwner structureOwner = structure.getOwner(inventoryHolder).orElseGet(this::getDummyOwner);
        this.allowedAttributes = analyzeAttributes(structureOwner, inventoryHolder, permissionsManager);

        this.visibleProperties = structure
            .getPropertiesForOwnerFilteredByAccessLevel(structureOwner, PropertyAccessLevel.READ)
            .stream()
            .toList();

        this.canFitProperties = canFitProperties(this.structure, this.visibleProperties);

        this.attributeElements = HashMap.newHashMap(this.allowedAttributes.size());

        this.inventoryGui = createGUI();

        showGUI();
    }

    private void showGUI()
    {
        inventoryGui.show(inventoryHolder.getBukkitPlayer());
    }

    private InventoryGui createGUI()
    {
        final String[] guiRowDefinitions = createGuiRowDefinitions();

        final InventoryGui gui = new InventoryGui(
            animatedArchitecturePlugin,
            inventoryHolder.getBukkitPlayer(),
            localizer.getMessage("gui.info_page.title", structure.getNameAndUid()),
            guiRowDefinitions
        );
        gui.setFiller(FILLER);

        populateGUI(gui);

        return gui;
    }

    private String[] createGuiRowDefinitions()
    {
        final String[] baseRowDefinitions = GuiUtil.fillLinesWithChar('g', allowedAttributes.size(), "f   h    ");

        if (!this.canFitProperties)
            return baseRowDefinitions;

        final String[] propertyRowDefinitions = GuiUtil.fillLinesWithChar('i', this.visibleProperties.size());
        return CollectionsUtil.concat(baseRowDefinitions, propertyRowDefinitions);
    }

    private static boolean canFitProperties(
        IStructureConst structure,
        Collection<PropertyValuePair<?>> visibleProperties
    )
    {
        if (visibleProperties.size() < MAX_PROPERTY_SLOTS)
            return true;

        log.atError().withStackTrace(StackSize.FULL).log(
            "Failed to populate GUI with properties for structure %s. " +
                "Received %d visible properties, which exceeds %d allowed",
            structure,
            visibleProperties.size(),
            MAX_PROPERTY_SLOTS
        );
        return false;
    }

    private void populateGUI(InventoryGui gui)
    {
        addHeader(gui);
        addAttributeElements(gui);
        addPropertyElements(gui);
    }

    private void addHeader(InventoryGui gui)
    {
        gui.addElement(new StaticGuiElement(
            'h',
            new ItemStack(Material.BOOK),
            localizer.getMessage(
                "gui.info_page.header",
                localizer.getMessage(structure.getType().getLocalizationKey()),
                structure.getNameAndUid()))
        );

        gui.addElement(new GuiBackElement(
            'f',
            new ItemStack(Material.ARROW),
            localizer.getMessage("gui.info_page.back_button"))
        );
    }

    private void addAttributeElements(InventoryGui gui)
    {
        final GuiElementGroup group = new GuiElementGroup('g');
        for (final StructureAttribute attribute : allowedAttributes)
        {
            final GuiElement element = attributeButtonFactory.of(attribute, structure, inventoryHolder, 'g');
            if (element == null)
                continue;
            attributeElements.put(attribute, element);
            group.addElement(element);
        }
        group.setFiller(FILLER);
        gui.addElement(group);
    }

    private void addPropertyElements(InventoryGui gui)
    {
        if (!canFitProperties)
            return;

        final GuiElementGroup group = new GuiElementGroup('i');
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
            group.addElement(new StaticGuiElement('i', itemStack));
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

    private StructureOwner getDummyOwner()
    {
        log.atError().log("Player '%s' does not have access to structure: '%s'!", inventoryHolder, structure);
        return new StructureOwner(
            structure.getUid(),
            PermissionLevel.NO_PERMISSION,
            inventoryHolder.getPlayerData()
        );
    }

    static List<StructureAttribute> analyzeAttributes(
        StructureOwner structureOwner,
        WrappedPlayer player,
        IPermissionsManager permissionsManager)
    {
        final PermissionLevel perm = structureOwner.permission();
        return StructureAttribute
            .getValues()
            .stream()
            .filter(attr -> !attr.equals(StructureAttribute.SET_PROPERTY))
            .filter(attr -> hasAccessToAttribute(player, attr, perm, permissionsManager))
            .toList();
    }

    private static boolean hasAccessToAttribute(
        WrappedPlayer player,
        StructureAttribute attribute,
        PermissionLevel permissionLevel,
        IPermissionsManager permissionsManager)
    {
        return attribute.canAccessWith(permissionLevel) ||
            permissionsManager.hasBypassPermissionsForAttribute(player, attribute);
    }

    @Override
    public String getPageName()
    {
        return "InfoGui";
    }

    @AssistedFactory
    interface IFactory
    {
        /**
         * Creates and opens a new {@link InfoGui} for the given structure and player.
         *
         * @param structure
         *     The structure to use to populate the GUI.
         * @param playerSpigot
         *     The player to open the GUI for.
         * @return The created {@link InfoGui} instance.
         */
        @SuppressWarnings("NullableProblems")
        InfoGui newInfoGUI(Structure structure, WrappedPlayer playerSpigot);
    }
}
