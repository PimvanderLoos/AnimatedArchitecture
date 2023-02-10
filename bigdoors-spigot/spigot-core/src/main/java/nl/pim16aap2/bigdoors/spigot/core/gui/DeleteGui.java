package nl.pim16aap2.bigdoors.spigot.core.gui;

import dagger.assisted.Assisted;
import dagger.assisted.AssistedFactory;
import dagger.assisted.AssistedInject;
import de.themoep.inventorygui.InventoryGui;
import de.themoep.inventorygui.StaticGuiElement;
import lombok.Getter;
import lombok.ToString;
import nl.pim16aap2.bigdoors.core.commands.CommandFactory;
import nl.pim16aap2.bigdoors.core.localization.ILocalizer;
import nl.pim16aap2.bigdoors.core.structures.AbstractStructure;
import nl.pim16aap2.bigdoors.core.util.structureretriever.StructureRetrieverFactory;
import nl.pim16aap2.bigdoors.spigot.core.BigDoorsPlugin;
import nl.pim16aap2.bigdoors.spigot.util.implementations.PlayerSpigot;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

@ToString(onlyExplicitlyIncluded = true)
class DeleteGui implements IGuiPage
{
    private static final ItemStack FILLER = new ItemStack(Material.GRAY_STAINED_GLASS_PANE, 1);

    private final BigDoorsPlugin bigDoorsPlugin;
    private final ILocalizer localizer;
    private final CommandFactory commandFactory;
    private final StructureRetrieverFactory structureRetrieverFactory;
    private final InventoryGui inventoryGui;

    @ToString.Include
    private final AbstractStructure structure;

    @Getter
    @ToString.Include
    private final PlayerSpigot inventoryHolder;

    @AssistedInject //
    DeleteGui(
        BigDoorsPlugin bigDoorsPlugin, ILocalizer localizer, CommandFactory commandFactory,
        StructureRetrieverFactory structureRetrieverFactory,
        @Assisted AbstractStructure structure, @Assisted PlayerSpigot inventoryHolder)
    {
        this.bigDoorsPlugin = bigDoorsPlugin;
        this.localizer = localizer;
        this.commandFactory = commandFactory;
        this.structureRetrieverFactory = structureRetrieverFactory;
        this.structure = structure;
        this.inventoryHolder = inventoryHolder;

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
                                                  localizer.getMessage(structure.getType().getLocalizationKey()),
                                                  structure.getNameAndUid()),
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
                GuiUtil.closeGuiPage(gui, inventoryHolder);
                return true;
            },
            localizer.getMessage("gui.delete_page.cancel",
                                 localizer.getMessage(structure.getType().getLocalizationKey()))
        ));
        gui.addElement(new StaticGuiElement(
            'd',
            new ItemStack(Material.BARRIER),
            click ->
            {
                commandFactory.newDelete(inventoryHolder, structureRetrieverFactory.of(structure)).run();
                GuiUtil.closeGuiPage(gui, inventoryHolder);
                return true;
            },
            localizer.getMessage("gui.delete_page.confirm",
                                 localizer.getMessage(structure.getType().getLocalizationKey()))
        ));
    }

    @Override
    public String getPageName()
    {
        return "DeleteGui";
    }

    @AssistedFactory
    interface IFactory
    {
        DeleteGui newDeleteGui(AbstractStructure structure, PlayerSpigot playerSpigot);
    }
}
