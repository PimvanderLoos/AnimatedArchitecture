package nl.pim16aap2.bigdoors.spigot.gui;

import dagger.assisted.Assisted;
import dagger.assisted.AssistedFactory;
import dagger.assisted.AssistedInject;
import de.themoep.inventorygui.InventoryGui;
import de.themoep.inventorygui.StaticGuiElement;
import nl.pim16aap2.bigdoors.api.factories.ITextFactory;
import nl.pim16aap2.bigdoors.commands.CommandFactory;
import nl.pim16aap2.bigdoors.doors.AbstractDoor;
import nl.pim16aap2.bigdoors.localization.ILocalizer;
import nl.pim16aap2.bigdoors.spigot.BigDoorsPlugin;
import nl.pim16aap2.bigdoors.spigot.util.implementations.PPlayerSpigot;
import nl.pim16aap2.bigdoors.util.doorretriever.DoorRetrieverFactory;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

class DeleteGui
{
    private static final ItemStack FILLER = new ItemStack(Material.GRAY_STAINED_GLASS_PANE, 1);

    private final CommandFactory commandFactory;
    private final BigDoorsPlugin bigDoorsPlugin;
    private final ILocalizer localizer;
    private final ITextFactory textFactory;
    private final DoorRetrieverFactory doorRetrieverFactory;
    private final AbstractDoor door;
    private final PPlayerSpigot inventoryHolder;
    private final MainGui mainGui;
    private final InventoryGui inventoryGui;

    @AssistedInject //
    DeleteGui(
        BigDoorsPlugin bigDoorsPlugin, ILocalizer localizer, ITextFactory textFactory,
        CommandFactory commandFactory, DoorRetrieverFactory doorRetrieverFactory,
        @Assisted AbstractDoor door, @Assisted PPlayerSpigot inventoryHolder, @Assisted MainGui mainGui)
    {
        this.commandFactory = commandFactory;
        this.bigDoorsPlugin = bigDoorsPlugin;
        this.localizer = localizer;
        this.textFactory = textFactory;
        this.doorRetrieverFactory = doorRetrieverFactory;
        this.door = door;
        this.inventoryHolder = inventoryHolder;
        this.mainGui = mainGui;

        this.inventoryGui = createGUI();
        showGUI();
    }


    private void showGUI()
    {
        inventoryGui.show(inventoryHolder.getBukkitPlayer());
    }

    private InventoryGui createGUI()
    {
        final String[] guiSetup = new String[]{
            "sssssssss",
            "sssssssss",
            "ssssdssss",
            "sssssssss",
            "sssssssss"
        };

        final InventoryGui gui =
            new InventoryGui(bigDoorsPlugin,
                             inventoryHolder.getBukkitPlayer(),
                             localizer.getMessage("gui.delete_page.title",
                                                  localizer.getMessage(door.getDoorType().getLocalizationKey()),
                                                  door.getName()),
                             guiSetup);
        gui.setFiller(FILLER);

        populateGUI(gui);

        return gui;
    }

    private void populateGUI(InventoryGui gui)
    {
        gui.addElement(new StaticGuiElement(
            's',
            new ItemStack(Material.GREEN_STAINED_GLASS_PANE),
            click ->
            {
                gui.close(false);
                return true;
            },
            localizer.getMessage("gui.delete_page.cancel",
                                 localizer.getMessage(door.getDoorType().getLocalizationKey()))
        ));
        gui.addElement(new StaticGuiElement(
            'd',
            new ItemStack(Material.BARRIER),
            click ->
            {
//                commandFactory.newDelete(inventoryHolder, doorRetrieverFactory.of(door)).run();
                mainGui.removeDoor(door);
                inventoryGui.close(true);
                mainGui.redraw();
                return true;
            },
            localizer.getMessage("gui.delete_page.confirm",
                                 localizer.getMessage(door.getDoorType().getLocalizationKey()))
        ));
    }

    @AssistedFactory
    interface IFactory
    {
        DeleteGui newDeleteGui(AbstractDoor door, PPlayerSpigot playerSpigot, MainGui mainGui);
    }
}
