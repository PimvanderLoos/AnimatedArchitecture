package nl.pim16aap2.bigdoors.spigot.core.gui;

import dagger.assisted.Assisted;
import dagger.assisted.AssistedFactory;
import dagger.assisted.AssistedInject;
import de.themoep.inventorygui.GuiElement;
import de.themoep.inventorygui.GuiElementGroup;
import de.themoep.inventorygui.InventoryGui;
import de.themoep.inventorygui.StaticGuiElement;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.flogger.Flogger;
import nl.pim16aap2.bigdoors.core.api.IPermissionsManager;
import nl.pim16aap2.bigdoors.core.localization.ILocalizer;
import nl.pim16aap2.bigdoors.core.structures.AbstractStructure;
import nl.pim16aap2.bigdoors.core.structures.PermissionLevel;
import nl.pim16aap2.bigdoors.core.structures.StructureAttribute;
import nl.pim16aap2.bigdoors.core.structures.StructureOwner;
import nl.pim16aap2.bigdoors.spigot.core.BigDoorsPlugin;
import nl.pim16aap2.bigdoors.spigot.util.implementations.PPlayerSpigot;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents the info menu for a structure.
 * <p>
 * This contains all the options for a single structure.
 */
@Flogger
@ToString(onlyExplicitlyIncluded = true)
class InfoGui implements IGuiPage
{
    private static final ItemStack FILLER = new ItemStack(Material.GRAY_STAINED_GLASS_PANE, 1);

    private final BigDoorsPlugin bigDoorsPlugin;
    private final ILocalizer localizer;
    private final AttributeButtonFactory attributeButtonFactory;

    @Getter
    private final InventoryGui inventoryGui;

    @ToString.Include
    private final AbstractStructure structure;

    @ToString.Include
    private final List<StructureAttribute> allowedAttributes;

    @Getter
    @ToString.Include
    private final PPlayerSpigot inventoryHolder;

    // Currently not used, but it's needed later on when we can update the elements
    // When the field in the structure changes.
    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
    private final Map<StructureAttribute, GuiElement> attributeElements;

    @AssistedInject //
    InfoGui(
        BigDoorsPlugin bigDoorsPlugin, ILocalizer localizer, IPermissionsManager permissionsManager,
        AttributeButtonFactory attributeButtonFactory,
        @Assisted AbstractStructure structure, @Assisted PPlayerSpigot inventoryHolder)
    {
        this.bigDoorsPlugin = bigDoorsPlugin;
        this.localizer = localizer;
        this.attributeButtonFactory = attributeButtonFactory;
        this.structure = structure;
        this.inventoryHolder = inventoryHolder;

        final StructureOwner structureOwner = structure.getOwner(inventoryHolder).orElseGet(this::getDummyOwner);
        this.allowedAttributes = analyzeAttributes(structureOwner, inventoryHolder, permissionsManager);

        this.attributeElements = new HashMap<>((int) Math.ceil(1.25 * this.allowedAttributes.size()));

        this.inventoryGui = createGUI();

        showGUI();
    }


    private void showGUI()
    {
        inventoryGui.show(inventoryHolder.getBukkitPlayer());
    }

    private InventoryGui createGUI()
    {
        final String[] guiSetup = GuiUtil.fillLinesWithChar('g', allowedAttributes.size(), "    h    ");

        final InventoryGui gui =
            new InventoryGui(bigDoorsPlugin,
                             inventoryHolder.getBukkitPlayer(),
                             localizer.getMessage("gui.info_page.title", structure.getNameAndUid()),
                             guiSetup);
        gui.setFiller(FILLER);

        populateGUI(gui);

        return gui;
    }

    private void populateGUI(InventoryGui gui)
    {
        addHeader(gui);
        addElements(gui);
    }

    private void addHeader(InventoryGui gui)
    {
        gui.addElement(new StaticGuiElement(
            'h',
            new ItemStack(Material.BOOK),
            localizer.getMessage("gui.info_page.header",
                                 localizer.getMessage(structure.getType().getLocalizationKey()),
                                 structure.getNameAndUid())
        ));
    }

    private void addElements(InventoryGui gui)
    {
        final GuiElementGroup group = new GuiElementGroup('g');
        for (final StructureAttribute attribute : allowedAttributes)
        {
            final GuiElement element =
                attributeButtonFactory.of(attribute, structure, inventoryHolder, 'g');
            attributeElements.put(attribute, element);
            group.addElement(element);
        }
        group.setFiller(FILLER);
        gui.addElement(group);
    }

    private StructureOwner getDummyOwner()
    {
        log.atSevere().log("Player '%s' does not have access to structure: '%s'!", inventoryHolder, structure);
        return new StructureOwner(structure.getUid(), PermissionLevel.NO_PERMISSION,
                                  inventoryHolder.getPPlayerData());
    }

    static List<StructureAttribute> analyzeAttributes(
        StructureOwner structureOwner, PPlayerSpigot player, IPermissionsManager permissionsManager)
    {
        final PermissionLevel perm = structureOwner.permission();
        return StructureAttribute.getValues().stream()
                                 .filter(attr -> hasAccessToAttribute(player, attr, perm, permissionsManager))
                                 .filter(attr -> attr != StructureAttribute.SWITCH)
                                 .toList();
    }

    private static boolean hasAccessToAttribute(
        PPlayerSpigot player, StructureAttribute attribute, PermissionLevel permissionLevel,
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
        InfoGui newInfoGUI(AbstractStructure structure, PPlayerSpigot playerSpigot);
    }
}
