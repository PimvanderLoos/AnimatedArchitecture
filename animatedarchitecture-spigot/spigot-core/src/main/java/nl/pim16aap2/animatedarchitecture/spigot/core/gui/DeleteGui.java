package nl.pim16aap2.animatedarchitecture.spigot.core.gui;

import dagger.assisted.Assisted;
import dagger.assisted.AssistedFactory;
import dagger.assisted.AssistedInject;
import de.themoep.inventorygui.InventoryGui;
import de.themoep.inventorygui.StaticGuiElement;
import lombok.CustomLog;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.ExtensionMethod;
import nl.pim16aap2.animatedarchitecture.core.commands.CommandFactory;
import nl.pim16aap2.animatedarchitecture.core.localization.ILocalizer;
import nl.pim16aap2.animatedarchitecture.core.structures.Structure;
import nl.pim16aap2.animatedarchitecture.core.structures.retriever.StructureRetrieverFactory;
import nl.pim16aap2.animatedarchitecture.core.util.CompletableFutureExtensions;
import nl.pim16aap2.animatedarchitecture.spigot.core.AnimatedArchitecturePlugin;
import nl.pim16aap2.animatedarchitecture.spigot.util.implementations.WrappedPlayer;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

@CustomLog
@ToString(onlyExplicitlyIncluded = true)
@ExtensionMethod(CompletableFutureExtensions.class)
class DeleteGui implements IGuiPage
{
    private static final ItemStack FILLER = new ItemStack(Material.GRAY_STAINED_GLASS_PANE, 1);

    private final AnimatedArchitecturePlugin animatedArchitecturePlugin;
    private final ILocalizer localizer;
    private final CommandFactory commandFactory;
    private final StructureRetrieverFactory structureRetrieverFactory;
    private final InventoryGui inventoryGui;

    @ToString.Include
    private final Structure structure;

    @Getter
    @ToString.Include
    private final WrappedPlayer inventoryHolder;

    @AssistedInject
    DeleteGui(
        AnimatedArchitecturePlugin animatedArchitecturePlugin,
        ILocalizer localizer,
        CommandFactory commandFactory,
        StructureRetrieverFactory structureRetrieverFactory,
        @Assisted Structure structure,
        @Assisted WrappedPlayer inventoryHolder)
    {
        this.animatedArchitecturePlugin = animatedArchitecturePlugin;
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

        final InventoryGui gui = new InventoryGui(
            animatedArchitecturePlugin,
            inventoryHolder.getBukkitPlayer(),
            localizer.getMessage(
                "gui.delete_page.title",
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
            localizer.getMessage(
                "gui.delete_page.cancel",
                localizer.getMessage(structure.getType().getLocalizationKey()))
        ));
        gui.addElement(new StaticGuiElement(
            'd',
            new ItemStack(Material.BARRIER),
            click ->
            {
                commandFactory
                    .newDelete(inventoryHolder, structureRetrieverFactory.of(structure))
                    .run()
                    .handleExceptional(ex ->
                    {
                        inventoryHolder.sendError("constants.error.generic");
                        log.atError().withCause(ex).log("Failed to delete structure.");
                    });
                GuiUtil.closeGuiPage(gui, inventoryHolder);
                return true;
            },
            localizer.getMessage(
                "gui.delete_page.confirm",
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
        DeleteGui newDeleteGui(Structure structure, WrappedPlayer playerSpigot);
    }
}
