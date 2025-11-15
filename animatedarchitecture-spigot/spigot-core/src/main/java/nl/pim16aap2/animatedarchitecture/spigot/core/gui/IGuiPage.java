package nl.pim16aap2.animatedarchitecture.spigot.core.gui;

import de.themoep.inventorygui.GuiElement;
import de.themoep.inventorygui.StaticGuiElement;
import lombok.CustomLog;
import nl.pim16aap2.animatedarchitecture.core.api.IPlayer;
import nl.pim16aap2.animatedarchitecture.core.localization.ILocalizer;
import nl.pim16aap2.animatedarchitecture.core.managers.StructureDeletionManager;
import nl.pim16aap2.animatedarchitecture.spigot.util.implementations.WrappedPlayer;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * Represents a Gui page.
 */
interface IGuiPage
{
    /**
     * Gets the name of the page.
     * <p>
     * This is shown in the inventory title.
     *
     * @return The name of the page.
     */
    String getPageName();

    /**
     * Gets the inventory holder of this page.
     * <p>
     * This is the player that is currently viewing the page.
     *
     * @return The inventory holder of this page.
     */
    IPlayer getInventoryHolder();

    /**
     * Creates a generic error element to show in the GUI.
     *
     * @param slotChar
     *     The slot character for the element.
     * @param localizer
     *     The localizer to use for localization.
     * @param context
     *     The context to show in the lore.
     * @return The error element.
     */
    default GuiElement createErrorElement(char slotChar, ILocalizer localizer, String context)
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
     * Handles exceptional completion of a future.
     *
     * @param ex
     *     The exception that occurred.
     * @param player
     *     The player for which the exception occurred.
     * @param context
     *     The context in which the exception occurred. This is used for logging.
     *     <p>
     *     E.g. the action that was being performed when the exception occurred ("toggle", "lock", etc.).
     */
    default void handleExceptional(Throwable ex, WrappedPlayer player, String context)
    {
        player.sendGenericErrorMessage();
        LogHolder.log.atError().withCause(ex).log("Failed to handle action '%s' for player '%s'", context, player);
    }

    /**
     * Represents a union type of {@link IGuiPage} and {@link StructureDeletionManager.IDeletionListener}.
     */
    interface IGuiStructureDeletionListener extends IGuiPage, StructureDeletionManager.IDeletionListener
    {
    }

    /**
     * Logger holder class. Do not use.
     */
    @CustomLog
    final class LogHolder
    {
    }
}
