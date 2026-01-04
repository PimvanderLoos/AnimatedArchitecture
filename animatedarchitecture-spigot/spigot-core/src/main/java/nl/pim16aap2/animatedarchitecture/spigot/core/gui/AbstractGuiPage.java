package nl.pim16aap2.animatedarchitecture.spigot.core.gui;

import de.themoep.inventorygui.GuiElement;
import de.themoep.inventorygui.InventoryGui;
import de.themoep.inventorygui.StaticGuiElement;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import nl.pim16aap2.animatedarchitecture.core.api.IExecutor;
import nl.pim16aap2.animatedarchitecture.core.localization.PersonalizedLocalizer;
import nl.pim16aap2.animatedarchitecture.core.managers.StructureDeletionManager;
import nl.pim16aap2.animatedarchitecture.spigot.core.AnimatedArchitecturePlugin;
import nl.pim16aap2.animatedarchitecture.spigot.util.implementations.WrappedPlayer;
import org.bukkit.Material;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Represents the abstract base of a Gui page.
 *
 * @param <T>
 *     The type of the concrete Gui page extending this abstract class.
 */
@ToString
@EqualsAndHashCode
abstract class AbstractGuiPage<T extends AbstractGuiPage<T>>
{
    @ToString.Exclude
    protected final AnimatedArchitecturePlugin animatedArchitecturePlugin;

    /**
     * Gets the inventory holder of this page.
     * <p>
     * This is the player that is currently viewing the page.
     */
    @ToString.Include
    @Getter
    protected final WrappedPlayer inventoryHolder;

    protected final IExecutor executor;

    /**
     * The InventoryGui instance representing this page.
     */
    private @Nullable InventoryGui inventoryGui;

    /**
     * The personalized localizer of the inventory holder.
     */
    @ToString.Include
    protected final PersonalizedLocalizer localizer;

    protected AbstractGuiPage(
        AnimatedArchitecturePlugin animatedArchitecturePlugin,
        WrappedPlayer inventoryHolder,
        IExecutor executor
    )
    {
        this.animatedArchitecturePlugin = animatedArchitecturePlugin;
        this.inventoryHolder = inventoryHolder;
        this.executor = executor;
        this.localizer = inventoryHolder.getPersonalizedLocalizer();
    }

    /**
     * Creates and shows the GUI to the player.
     *
     * @return The created GUI page.
     */
    public final T createAndShowGui()
    {
        try
        {
            this.inventoryGui = this.createGui();
            setupCloseAction(this.inventoryGui);
            this.inventoryGui.show(this.inventoryHolder.getBukkitPlayer());
        }
        catch (Throwable t)
        {
            throw new RuntimeException("Failed to create and show GUI", t);
        }
        //noinspection unchecked
        return (T) this;
    }

    /**
     * Redraws the Gui.
     * <p>
     * This will update all dynamic elements and recreate the inventory.
     */
    public void redrawGui()
    {
        executor.assertMainThread();
        if (this.inventoryGui != null)
        {
            this.inventoryGui.draw(
                inventoryHolder.getBukkitPlayer(),
                true, // Update all dynamic elements
                true // Recreate the inventory
            );
        }
    }

    /**
     * Sets up the close action for cleanup when the GUI is closed.
     */
    private void setupCloseAction(InventoryGui gui)
    {
        gui.setCloseAction(close ->
        {
            onGuiClosed();
            return true;
        });
    }

    /**
     * Called when the GUI is closed by the player. Override this method to perform cleanup tasks.
     * <p>
     * For example, unregistering listeners.
     * <p>
     * Defaults to a no-op.
     */
    protected void onGuiClosed()
    {
    }

    /**
     * Creates a generic error element to be used in the GUI.
     *
     * @param slotChar
     *     The slot character where the element will be placed.
     * @param context
     *     The context or reason for the error.
     * @return The created error GuiElement.
     */
    protected GuiElement createErrorElement(char slotChar, String context)
    {
        final ItemStack itemStack = new ItemStack(Material.BARRIER);
        final var meta = itemStack.getItemMeta();
        if (meta != null)
        {
            meta.setDisplayName(localizer.getMessage("constants.error.generic"));
            meta.setLore(List.of(context));
            itemStack.setItemMeta(meta);
        }
        return new StaticGuiElement(slotChar, itemStack);
    }

    /**
     * Creates the GUI for this page.
     *
     * @return The created GUI.
     */
    protected abstract InventoryGui createGui();

    /**
     * Creates a new InventoryGui with the given parameters.
     * <p>
     * See {@link InventoryGui#InventoryGui(Plugin, InventoryHolder, String, String[], GuiElement...)} for more info.
     *
     * @param title
     *     The name of the GUI. This will be the title of the inventory.
     * @param rows
     *     How the rows are set up. Each element is getting assigned to a character. Empty/missing ones get filled with
     *     the Filler.
     * @param elements
     *     The GuiElements that the gui should have. We can also use addElement(GuiElement) later.
     */
    protected final InventoryGui newInventoryGui(
        String title,
        String[] rows,
        GuiElement... elements)
    {
        return new InventoryGui(
            this.animatedArchitecturePlugin,
            this.inventoryHolder.getBukkitPlayer(),
            title,
            rows,
            elements
        );
    }

    /**
     * Gets the GUI page that this GuiPage represents.
     *
     * @return The current open GUI.
     *
     * @throws IllegalStateException
     *     If the GUI page has not been initialized yet.
     */
    protected final InventoryGui getGuiPage()
    {
        if (this.inventoryGui == null)
        {
            throw new IllegalStateException("GUI page has not been initialized yet.");
        }
        return this.inventoryGui;
    }

    /**
     * Represents a union type of {@link AbstractGuiPage} and {@link StructureDeletionManager.IDeletionListener}.
     */
    interface IGuiStructureDeletionListener extends StructureDeletionManager.IDeletionListener
    {
        /**
         * Gets the name of the inventory holder associated with this listener.
         * <p>
         * This is used for logging purposes.
         *
         * @return The name of the inventory holder.
         */
        String getInventoryHolderAsString();
    }
}
