package nl.pim16aap2.bigdoors.spigot.gui;

import dagger.assisted.Assisted;
import dagger.assisted.AssistedFactory;
import dagger.assisted.AssistedInject;
import de.themoep.inventorygui.InventoryGui;
import de.themoep.inventorygui.StaticGuiElement;
import nl.pim16aap2.bigdoors.commands.CommandFactory;
import nl.pim16aap2.bigdoors.localization.ILocalizer;
import nl.pim16aap2.bigdoors.movable.AbstractMovable;
import nl.pim16aap2.bigdoors.spigot.BigDoorsPlugin;
import nl.pim16aap2.bigdoors.spigot.util.implementations.PPlayerSpigot;
import nl.pim16aap2.bigdoors.util.movableretriever.MovableRetrieverFactory;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

class DeleteGui
{
    private static final ItemStack FILLER = new ItemStack(Material.GRAY_STAINED_GLASS_PANE, 1);

    private final BigDoorsPlugin bigDoorsPlugin;
    private final ILocalizer localizer;
    private final CommandFactory commandFactory;
    private final MovableRetrieverFactory movableRetrieverFactory;
    private final AbstractMovable movable;
    private final PPlayerSpigot inventoryHolder;
    private final MainGui mainGui;
    private final InventoryGui inventoryGui;

    @AssistedInject //
    DeleteGui(
        BigDoorsPlugin bigDoorsPlugin, ILocalizer localizer, CommandFactory commandFactory,
        MovableRetrieverFactory movableRetrieverFactory,
        @Assisted AbstractMovable movable, @Assisted PPlayerSpigot inventoryHolder, @Assisted MainGui mainGui)
    {
        this.bigDoorsPlugin = bigDoorsPlugin;
        this.localizer = localizer;
        this.commandFactory = commandFactory;
        this.movableRetrieverFactory = movableRetrieverFactory;
        this.movable = movable;
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
                                                  localizer.getMessage(movable.getMovableType().getLocalizationKey()),
                                                  movable.getName()),
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
                                 localizer.getMessage(movable.getMovableType().getLocalizationKey()))
        ));
        gui.addElement(new StaticGuiElement(
            'd',
            new ItemStack(Material.BARRIER),
            click ->
            {
                commandFactory.newDelete(inventoryHolder, movableRetrieverFactory.of(movable)).run();
                mainGui.removeMovable(movable);
                inventoryGui.close(true);
                mainGui.redraw();
                return true;
            },
            localizer.getMessage("gui.delete_page.confirm",
                                 localizer.getMessage(movable.getMovableType().getLocalizationKey()))
        ));
    }

    @AssistedFactory
    interface IFactory
    {
        DeleteGui newDeleteGui(AbstractMovable movable, PPlayerSpigot playerSpigot, MainGui mainGui);
    }
}
