package nl.pim16aap2.bigdoors.spigot.gui;

import dagger.assisted.Assisted;
import dagger.assisted.AssistedFactory;
import dagger.assisted.AssistedInject;
import de.themoep.inventorygui.GuiElement;
import de.themoep.inventorygui.GuiElementGroup;
import de.themoep.inventorygui.InventoryGui;
import de.themoep.inventorygui.StaticGuiElement;
import lombok.extern.flogger.Flogger;
import nl.pim16aap2.bigdoors.api.IPermissionsManager;
import nl.pim16aap2.bigdoors.localization.ILocalizer;
import nl.pim16aap2.bigdoors.movable.AbstractMovable;
import nl.pim16aap2.bigdoors.movable.MovableAttribute;
import nl.pim16aap2.bigdoors.movable.MovableOwner;
import nl.pim16aap2.bigdoors.movable.PermissionLevel;
import nl.pim16aap2.bigdoors.spigot.BigDoorsPlugin;
import nl.pim16aap2.bigdoors.spigot.util.implementations.PPlayerSpigot;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents the info menu for a movable.
 * <p>
 * This contains all the options for a single movable.
 */
@Flogger
class InfoGui
{
    private static final ItemStack FILLER = new ItemStack(Material.GRAY_STAINED_GLASS_PANE, 1);

    private final BigDoorsPlugin bigDoorsPlugin;
    private final ILocalizer localizer;
    private final AttributeButtonFactory attributeButtonFactory;
    private final AbstractMovable movable;
    private final PPlayerSpigot inventoryHolder;
    private final MainGui mainGui;
    private final InventoryGui inventoryGui;
    private final List<MovableAttribute> allowedAttributes;

    // Currently not used, but it's needed later on when we can update the elements
    // When the field in the movable changes.
    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
    private final Map<MovableAttribute, GuiElement> attributeElements;

    @AssistedInject //
    InfoGui(
        BigDoorsPlugin bigDoorsPlugin, ILocalizer localizer, IPermissionsManager permissionsManager,
        AttributeButtonFactory attributeButtonFactory,
        @Assisted AbstractMovable movable, @Assisted PPlayerSpigot inventoryHolder, @Assisted MainGui mainGui)
    {
        this.bigDoorsPlugin = bigDoorsPlugin;
        this.localizer = localizer;
        this.attributeButtonFactory = attributeButtonFactory;
        this.movable = movable;
        this.inventoryHolder = inventoryHolder;
        this.mainGui = mainGui;

        final MovableOwner movableOwner = movable.getOwner(inventoryHolder).orElseGet(this::getDummyOwner);
        this.allowedAttributes = analyzeAttributes(movableOwner, inventoryHolder, permissionsManager);

        this.attributeElements = new HashMap<>(this.allowedAttributes.size());

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
                             localizer.getMessage("gui.info_page.title", movable.getName()),
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
                                 localizer.getMessage(movable.getMovableType().getLocalizationKey()), movable.getName())
        ));
    }

    private void addElements(InventoryGui gui)
    {
        final GuiElementGroup group = new GuiElementGroup('g');
        for (final MovableAttribute attribute : allowedAttributes)
        {
            final GuiElement element =
                attributeButtonFactory.of(gui, mainGui, attribute, movable, inventoryHolder, 'g');
            attributeElements.put(attribute, element);
            group.addElement(element);
        }
        group.setFiller(FILLER);
        gui.addElement(group);
    }

    private MovableOwner getDummyOwner()
    {
        log.atSevere().log("Player '%s' does not have access to movable: '%s'!", inventoryHolder, movable);
        return new MovableOwner(movable.getUid(), PermissionLevel.NO_PERMISSION,
                                inventoryHolder.getPPlayerData());
    }

    static List<MovableAttribute> analyzeAttributes(
        MovableOwner movableOwner, PPlayerSpigot player, IPermissionsManager permissionsManager)
    {
        final PermissionLevel perm = movableOwner.permission();
        return MovableAttribute.getValues().stream()
                               .filter(attr -> hasAccessToAttribute(player, attr, perm, permissionsManager))
                               .filter(attr -> attr != MovableAttribute.SWITCH)
                               .toList();
    }

    private static boolean hasAccessToAttribute(
        PPlayerSpigot player, MovableAttribute attribute, PermissionLevel permissionLevel,
        IPermissionsManager permissionsManager)
    {
        return attribute.canAccessWith(permissionLevel) ||
            permissionsManager.hasBypassPermissionsForAttribute(player, attribute);
    }

    @AssistedFactory
    interface IFactory
    {
        InfoGui newInfoGUI(AbstractMovable movable, PPlayerSpigot playerSpigot, MainGui mainGui);
    }
}
