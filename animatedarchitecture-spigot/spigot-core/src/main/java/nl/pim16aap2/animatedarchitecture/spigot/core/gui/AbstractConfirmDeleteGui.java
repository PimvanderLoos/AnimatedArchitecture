package nl.pim16aap2.animatedarchitecture.spigot.core.gui;

import de.themoep.inventorygui.InventoryGui;
import de.themoep.inventorygui.StaticGuiElement;
import lombok.CustomLog;
import lombok.ToString;
import lombok.experimental.ExtensionMethod;
import nl.pim16aap2.animatedarchitecture.core.api.IExecutor;
import nl.pim16aap2.animatedarchitecture.core.commands.CommandFactory;
import nl.pim16aap2.animatedarchitecture.core.util.CompletableFutureExtensions;
import nl.pim16aap2.animatedarchitecture.spigot.core.AnimatedArchitecturePlugin;
import nl.pim16aap2.animatedarchitecture.spigot.util.implementations.WrappedPlayer;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

/**
 * Abstract GUI page for deleting an item of type T.
 * <p>
 * For example, deleting a property or a structure, or deleting an entire structure altogether.
 *
 * @param <T>
 *     The type of item to delete.
 */
@CustomLog
@ToString(onlyExplicitlyIncluded = true, callSuper = true)
@ExtensionMethod(CompletableFutureExtensions.class)
abstract class AbstractConfirmDeleteGui<T, SELF extends AbstractConfirmDeleteGui<T, SELF>> extends AbstractGuiPage<SELF>
{
    private static final ItemStack FILLER = new ItemStack(Material.GRAY_STAINED_GLASS_PANE, 1);

    private static final char CH_CANCEL = 's';
    private static final char CH_DELETE = 'd';

    private static final String[] GUI_SETUP = new String[]{
        String.valueOf(CH_CANCEL).repeat(9),
        String.valueOf(CH_CANCEL).repeat(9),
        String.valueOf(CH_CANCEL).repeat(4) + CH_DELETE + String.valueOf(CH_CANCEL).repeat(4),
        String.valueOf(CH_CANCEL).repeat(9),
        String.valueOf(CH_CANCEL).repeat(9)
    };

    protected final CommandFactory commandFactory;

    /**
     * The item that is the subject of deletion.
     */
    protected final T itemToDelete;

    AbstractConfirmDeleteGui(
        AnimatedArchitecturePlugin animatedArchitecturePlugin,
        CommandFactory commandFactory,
        IExecutor executor,
        T itemToDelete,
        WrappedPlayer inventoryHolder
    )
    {
        super(animatedArchitecturePlugin, inventoryHolder, executor);
        this.commandFactory = commandFactory;
        this.itemToDelete = itemToDelete;
    }

    @Override
    protected final InventoryGui createGui()
    {
        final InventoryGui gui = newInventoryGui(
            getDeletePageConfirmationTitle(),
            GUI_SETUP
        );

        gui.setFiller(FILLER);

        populateGUI(gui);

        return gui;
    }

    /**
     * Get the title of the delete confirmation page where the user has to indicate that they are sure they want to
     * delete the object.
     *
     * @return The localized title of the delete confirmation page.
     */
    protected abstract String getDeletePageConfirmationTitle();

    /**
     * Get the title of the delete confirmation button.
     *
     * @return The localized title of the delete confirmation button.
     */
    protected abstract String getDeleteItemConfirmationTitle();

    /**
     * Get the title of the delete cancellation button.
     *
     * @return The localized title of the delete cancellation button.
     */
    protected abstract String getDeleteItemCancelTitle();

    /**
     * Called when the user has confirmed that they want to delete the item.
     */
    protected abstract void onDeleteConfirmed();

    private void populateGUI(InventoryGui gui)
    {
        gui.addElement(new StaticGuiElement(
            CH_CANCEL,
            new ItemStack(Material.GREEN_STAINED_GLASS_PANE),
            click ->
            {
                GuiUtil.closeGuiPage(gui, inventoryHolder);
                return true;
            },
            getDeleteItemCancelTitle()
        ));

        gui.addElement(new StaticGuiElement(
            CH_DELETE,
            new ItemStack(Material.BARRIER),
            click ->
            {
                onDeleteConfirmed();
                GuiUtil.closeGuiPage(gui, inventoryHolder);
                return true;
            },
            getDeleteItemConfirmationTitle()
        ));
    }
}
