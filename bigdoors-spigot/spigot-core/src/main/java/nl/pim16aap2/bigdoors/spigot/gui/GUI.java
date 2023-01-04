package nl.pim16aap2.bigdoors.spigot.gui;

import dagger.assisted.Assisted;
import dagger.assisted.AssistedFactory;
import dagger.assisted.AssistedInject;
import de.themoep.inventorygui.GuiElementGroup;
import de.themoep.inventorygui.GuiPageElement;
import de.themoep.inventorygui.InventoryGui;
import de.themoep.inventorygui.StaticGuiElement;
import nl.pim16aap2.bigdoors.api.IPExecutor;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.api.factories.ITextFactory;
import nl.pim16aap2.bigdoors.doors.AbstractDoor;
import nl.pim16aap2.bigdoors.localization.ILocalizer;
import nl.pim16aap2.bigdoors.spigot.BigDoorsPlugin;
import nl.pim16aap2.bigdoors.spigot.util.SpigotAdapter;
import nl.pim16aap2.bigdoors.spigot.util.implementations.PPlayerSpigot;
import nl.pim16aap2.bigdoors.text.TextType;
import nl.pim16aap2.bigdoors.util.Util;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class GUI
{
    private static final ItemStack FILLER = new ItemStack(Material.GRAY_STAINED_GLASS_PANE, 1);

    private final BigDoorsPlugin bigDoorsPlugin;
    private final ILocalizer localizer;
    private final ITextFactory textFactory;
    private final IPExecutor executor;
    private final PPlayerSpigot inventoryHolder;
    private final GUIData guiData;
    private final InventoryGui inventoryGui;

    @AssistedInject//
    GUI(
        BigDoorsPlugin bigDoorsPlugin, ILocalizer localizer, ITextFactory textFactory, IPExecutor executor,
        @Assisted IPPlayer inventoryHolder, @Assisted GUIData guiData)
    {
        this.bigDoorsPlugin = bigDoorsPlugin;
        this.localizer = localizer;
        this.textFactory = textFactory;
        this.executor = executor;
        this.inventoryHolder = Util.requireNonNull(SpigotAdapter.getPPlayerSpigot(inventoryHolder), "InventoryHolder");
        this.guiData = guiData;

        this.inventoryGui = createGUI();
        showGUI();
    }

    private void showGUI()
    {
        executor.runOnMainThread(() -> inventoryGui.show(inventoryHolder.getBukkitPlayer()));
    }

    private InventoryGui createGUI()
    {
        final String[] guiSetup = {
            "fp     nl",
            "ggggggggg",
            "ggggggggg",
            "ggggggggg",
            "ggggggggg",
            };

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

    private void addElementGroup(de.themoep.inventorygui.InventoryGui gui)
    {
        final GuiElementGroup group = new GuiElementGroup('g');
        for (final AbstractDoor door : guiData.getDoors())
        {
            final StaticGuiElement guiElement = new StaticGuiElement(
                'e',
                new ItemStack(Material.OAK_DOOR),
                click ->
                {
                    inventoryHolder.sendMessage(textFactory.newText()
                                                           .append("CLICKED ON DOOR: \n", TextType.INFO)
                                                           .append(door.getBasicInfo(), TextType.HIGHLIGHT));
                    return true;
                },
                door.getName());
            group.addElement(guiElement);
        }
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

    @AssistedFactory
    interface IFactory
    {
        /**
         * Creates a new GUI.
         *
         * @param inventoryHolder
         *     The player for whom to create the inventory.
         * @param guiData
         *     The {@link GUIData} to use.
         */
        GUI newGUI(IPPlayer inventoryHolder, GUIData guiData);
    }
}
