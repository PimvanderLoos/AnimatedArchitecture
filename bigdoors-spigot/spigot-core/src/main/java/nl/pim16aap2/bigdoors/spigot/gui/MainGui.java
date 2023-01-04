package nl.pim16aap2.bigdoors.spigot.gui;

import dagger.assisted.Assisted;
import dagger.assisted.AssistedFactory;
import dagger.assisted.AssistedInject;
import de.themoep.inventorygui.GuiElementGroup;
import de.themoep.inventorygui.GuiPageElement;
import de.themoep.inventorygui.InventoryGui;
import de.themoep.inventorygui.StaticGuiElement;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.doors.AbstractDoor;
import nl.pim16aap2.bigdoors.localization.ILocalizer;
import nl.pim16aap2.bigdoors.spigot.BigDoorsPlugin;
import nl.pim16aap2.bigdoors.spigot.util.SpigotAdapter;
import nl.pim16aap2.bigdoors.spigot.util.implementations.PPlayerSpigot;
import nl.pim16aap2.bigdoors.util.Util;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

class MainGui
{
    private static final ItemStack FILLER = new ItemStack(Material.GRAY_STAINED_GLASS_PANE, 1);

    private final BigDoorsPlugin bigDoorsPlugin;
    private final ILocalizer localizer;
    private final InfoGui.IFactory infoGUIFactory;
    private final PPlayerSpigot inventoryHolder;
    private final Map<Long, AbstractDoor> doors;
    private InventoryGui inventoryGui;


    @AssistedInject//
    MainGui(
        BigDoorsPlugin bigDoorsPlugin, ILocalizer localizer, InfoGui.IFactory infoGUIFactory,
        @Assisted IPPlayer inventoryHolder, @Assisted List<AbstractDoor> doors)
    {
        this.bigDoorsPlugin = bigDoorsPlugin;
        this.localizer = localizer;
        this.infoGUIFactory = infoGUIFactory;
        this.inventoryHolder = Util.requireNonNull(SpigotAdapter.getPPlayerSpigot(inventoryHolder), "InventoryHolder");
        this.doors = getDoorsMap(doors);

        this.inventoryGui = createGUI();

        showGUI();
    }

    private static Map<Long, AbstractDoor> getDoorsMap(List<AbstractDoor> doors)
    {
        final Map<Long, AbstractDoor> ret = new LinkedHashMap<>(doors.size());
        doors.stream().sorted(Comparator.comparing(AbstractDoor::getName))
             .forEach(door -> ret.put(door.getDoorUID(), door));
        return ret;
    }

    private void showGUI()
    {
        inventoryGui.show(inventoryHolder.getBukkitPlayer());
    }

    private InventoryGui createGUI()
    {
        final String[] guiSetup = GuiUtil.fillLinesWithChar('g', doors.size(), "fp     nl");

        final InventoryGui gui =
            new InventoryGui(bigDoorsPlugin,
                             inventoryHolder.getBukkitPlayer(),
                             localizer.getMessage("gui.main_page.title"),
                             guiSetup);
        gui.setFiller(FILLER);

        populateGUI(gui);

        return gui;
    }

    private void populateGUI(InventoryGui gui)
    {
        addElementGroup(gui);
        addHeader(gui);
    }

    private void addElementGroup(InventoryGui gui)
    {
        final GuiElementGroup group = new GuiElementGroup('g');
        for (final AbstractDoor door : doors.values())
        {
            final StaticGuiElement guiElement = new StaticGuiElement(
                'e',
                new ItemStack(Material.OAK_DOOR),
                click ->
                {
                    infoGUIFactory.newInfoGUI(door, inventoryHolder, this);
                    return true;
                },
                door.getName());
            group.addElement(guiElement);
        }
        group.setFiller(FILLER);
        gui.addElement(group);
    }

    private void addHeader(InventoryGui gui)
    {
        gui.addElement(new GuiPageElement(
            'f', new ItemStack(Material.ARROW), GuiPageElement.PageAction.FIRST,
            localizer.getMessage("gui.main_page.nav.first_page")));

        gui.addElement(new GuiPageElement(
            'p', new ItemStack(Material.BIRCH_SIGN), GuiPageElement.PageAction.PREVIOUS,
            localizer.getMessage("gui.main_page.nav.previous_page", "%prevpage%", "%pages%")));

        gui.addElement(new GuiPageElement(
            'n', new ItemStack(Material.BIRCH_SIGN), GuiPageElement.PageAction.NEXT,
            localizer.getMessage("gui.main_page.nav.next_page", "%nextpage%", "%pages%")));

        gui.addElement(new GuiPageElement(
            'l', new ItemStack(Material.ARROW), GuiPageElement.PageAction.LAST,
            localizer.getMessage("gui.main_page.nav.last_page")));
    }

    /**
     * Redraws the main GUI.
     * <p>
     * Note that any GUIs that are already open should be closed first.
     */
    public void redraw()
    {
        inventoryGui.close(true);
        inventoryGui = createGUI();
        showGUI();
    }

    /**
     * Removes a door from the set of visible doors.
     * <p>
     * Note that this will not update the GUI on its own. You may need to use {@link #redraw()} for that.
     *
     * @param door
     *     The door to remove.
     */
    public void removeDoor(AbstractDoor door)
    {
        doors.remove(door.getDoorUID());
    }

    @AssistedFactory
    interface IFactory
    {
        /**
         * Creates a new GUI.
         *
         * @param inventoryHolder
         *     The player for whom to create the GUI.
         * @param doors
         *     The doors to show in the GUI.
         */
        MainGui newGUI(IPPlayer inventoryHolder, List<AbstractDoor> doors);
    }
}
