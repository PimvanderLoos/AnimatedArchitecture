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
import nl.pim16aap2.animatedarchitecture.core.api.IExecutor;
import nl.pim16aap2.animatedarchitecture.core.api.IPermissionsManager;
import nl.pim16aap2.animatedarchitecture.core.structures.IStructureConst;
import nl.pim16aap2.animatedarchitecture.core.structures.PermissionLevel;
import nl.pim16aap2.animatedarchitecture.core.structures.Structure;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureAttribute;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureOwner;
import nl.pim16aap2.animatedarchitecture.core.structures.properties.Property;
import nl.pim16aap2.animatedarchitecture.core.structures.properties.PropertyAccessLevel;
import nl.pim16aap2.animatedarchitecture.core.structures.properties.PropertyValuePair;
import nl.pim16aap2.animatedarchitecture.core.util.CollectionsUtil;
import nl.pim16aap2.animatedarchitecture.spigot.core.AnimatedArchitecturePlugin;
import nl.pim16aap2.animatedarchitecture.spigot.core.managers.PropertyGuiAdapterRegistry;
import nl.pim16aap2.animatedarchitecture.spigot.core.propertyadapters.guiadapters.AbstractPropertyGuiAdapter;
import nl.pim16aap2.animatedarchitecture.spigot.core.propertyadapters.guiadapters.PropertyGuiRequest;
import nl.pim16aap2.animatedarchitecture.spigot.util.implementations.WrappedPlayer;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Represents the info menu for a structure.
 * <p>
 * This contains all the options for a single structure.
 */
@CustomLog
@ToString(onlyExplicitlyIncluded = true, callSuper = true)
class InfoGui extends AbstractGuiPage<InfoGui>
{
    private static final ItemStack FILLER = new ItemStack(Material.GRAY_STAINED_GLASS_PANE, 1);

    private static final int MAX_PROPERTY_SLOTS = 27;

    private static final EnumSet<StructureAttribute> EXCLUDED_ATTRIBUTES =
        EnumSet.of(
            StructureAttribute.SET_PROPERTY,
            StructureAttribute.OPEN_STATUS,
            StructureAttribute.BLOCKS_TO_MOVE
        );

    private final AttributeButtonFactory attributeButtonFactory;
    private final PropertyGuiAdapterRegistry adapterRegistry;
    private final AddPropertyGui.IFactory addPropertyGuiFactory;
    private final DeletePropertyGui.IFactory deletePropertyGuiFactory;
    private final PermissionLevel permissionLevel;

    @ToString.Include
    @Getter(AccessLevel.PACKAGE)
    private final Structure structure;

    @ToString.Include
    private final List<StructureAttribute> allowedAttributes;

    @ToString.Include
    private final List<PropertyValuePair<?>> visibleProperties;

    @ToString.Include
    private final StructureOwner structureOwner;

    @ToString.Include
    private volatile Set<Property<?>> addableProperties = Set.of();

    @ToString.Include
    private volatile Set<PropertyValuePair<?>> removableProperties = Set.of();

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
        IExecutor executor,
        IPermissionsManager permissionsManager,
        AttributeButtonFactory attributeButtonFactory,
        PropertyGuiAdapterRegistry adapterRegistry,
        AddPropertyGui.IFactory addPropertyGuiFactory,
        DeletePropertyGui.IFactory deletePropertyGuiFactory
    )
    {
        super(
            animatedArchitecturePlugin,
            inventoryHolder,
            executor
        );

        this.attributeButtonFactory = attributeButtonFactory;
        this.adapterRegistry = adapterRegistry;
        this.addPropertyGuiFactory = addPropertyGuiFactory;
        this.deletePropertyGuiFactory = deletePropertyGuiFactory;
        this.structure = structure;

        this.structureOwner = structure.getOwner(inventoryHolder).orElseGet(this::getDummyOwner);
        this.permissionLevel = structureOwner.permission();

        this.allowedAttributes = analyzeAttributes(structureOwner, inventoryHolder, permissionsManager);

        this.visibleProperties = structure
            .getPropertiesForOwnerFilteredByAccessLevel(structureOwner, PropertyAccessLevel.READ)
            .stream()
            .sorted(Comparator.comparing(p -> p.property().getFullKey()))
            .toList();

        this.refreshProperties();

        this.canFitProperties = canFitProperties(this.structure, this.visibleProperties);
    }

    @Override
    protected InventoryGui createGui()
    {
        final String[] guiRowDefinitions = createGuiRowDefinitions();

        final InventoryGui gui = newInventoryGui(
            localizer.getMessage("gui.info_page.title", structure.getNameAndUid()),
            guiRowDefinitions
        );
        gui.setFiller(FILLER);

        populateGUI(gui);

        return gui;
    }

    private String[] createGuiRowDefinitions()
    {
        String header = String.format(
            "f %s h %s  ",
            addableProperties.isEmpty() ? " " : "a",
            removableProperties.isEmpty() ? " " : "d"
        );
        final String[] baseRowDefinitions = GuiUtil.fillLinesWithChar('g', allowedAttributes.size(), header);

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
        addElements(gui, 'g', allowedAttributes, this::createAttributeElement);
        if (canFitProperties)
            addElements(gui, 'i', visibleProperties, this::createPropertyElement);
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

        if (!addableProperties.isEmpty())
        {
            gui.addElement(new StaticGuiElement(
                'a',
                new ItemStack(Material.CHEST_MINECART),
                click ->
                {
                    GuiUtil.closeGuiPage(gui, inventoryHolder);
                    addPropertyGuiFactory
                        .newAddPropertyGui(
                            structure,
                            inventoryHolder,
                            permissionLevel,
                            Collections.unmodifiableSet(addableProperties),
                            this::handlePotentialPropertiesChange
                        )
                        .createAndShowGui();
                    return true;
                },
                localizer.getMessage(
                    "gui.info_page.add_property_button",
                    localizer.getMessage(structure.getType().getLocalizationKey())
                )
            ));
        }

        if (!removableProperties.isEmpty())
        {
            gui.addElement(new StaticGuiElement(
                'd',
                new ItemStack(Material.TNT_MINECART),
                click ->
                {
                    GuiUtil.closeGuiPage(gui, inventoryHolder);
                    deletePropertyGuiFactory
                        .newDeletePropertyGui(
                            structure,
                            inventoryHolder,
                            permissionLevel,
                            removableProperties,
                            this::handlePotentialPropertiesChange
                        )
                        .createAndShowGui();
                    return true;
                },
                localizer.getMessage(
                    "gui.info_page.delete_property_button",
                    localizer.getMessage(structure.getType().getLocalizationKey())
                )
            ));
        }

        gui.addElement(new GuiBackElement(
            'f',
            new ItemStack(Material.ARROW),
            localizer.getMessage("gui.info_page.back_button"))
        );
    }

    private <T> void addElements(
        InventoryGui gui,
        char slotChar,
        Collection<T> elements,
        GuiElementMapper<T> mapper
    )
    {
        final GuiElementGroup group = new GuiElementGroup(slotChar);
        for (final var element : elements)
        {
            GuiElement guiElement = mapper.create(slotChar, element);
            if (guiElement == null)
            {
                guiElement = createErrorElement(slotChar, Objects.toString(element));
            }
            group.addElement(guiElement);
        }
        group.setFiller(FILLER);
        gui.addElement(group);
    }

    private @Nullable GuiElement createAttributeElement(char slotChar, StructureAttribute attribute)
    {
        return attributeButtonFactory.of(attribute, structure, inventoryHolder, slotChar);
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

    private StructureOwner getDummyOwner()
    {
        log.atError().log("Player '%s' does not have access to structure: '%s'!", inventoryHolder, structure);
        return new StructureOwner(
            structure.getUid(),
            PermissionLevel.NO_PERMISSION,
            inventoryHolder.getPlayerData()
        );
    }

    private void handlePotentialPropertiesChange(boolean changed)
    {
        if (!changed)
        {
            return;
        }
        refreshProperties();
        redrawGui();
    }

    private void refreshProperties()
    {
        this.addableProperties = findAddableProperties(structureOwner);
        this.removableProperties = findRemovableProperties(structureOwner);
    }

    private Set<Property<?>> findAddableProperties(StructureOwner structureOwner)
    {
        return Property.getAllRegisteredProperties()
            .stream()
            .filter(property ->
                !structure.getPropertyContainerSnapshot().getPropertyValue(property).isSet() &&
                    property.hasAccessLevel(structureOwner.permission(), PropertyAccessLevel.ADD)
            )
            .collect(Collectors.toCollection(ConcurrentHashMap::newKeySet));
    }

    private Set<PropertyValuePair<?>> findRemovableProperties(StructureOwner structureOwner)
    {
        return structure
            .getPropertiesForOwnerFilteredByAccessLevel(structureOwner, PropertyAccessLevel.REMOVE)
            .stream()
            .filter(pair -> pair.value().isSet() && !pair.value().isRequired())
            .collect(Collectors.toCollection(ConcurrentHashMap::newKeySet));
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
            .filter(attr -> !EXCLUDED_ATTRIBUTES.contains(attr))
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


    @FunctionalInterface
    private interface GuiElementMapper<T>
    {
        @Nullable GuiElement create(char slotChar, T element);
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
