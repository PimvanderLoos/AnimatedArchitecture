package nl.pim16aap2.animatedarchitecture.spigot.core.gui;

import nl.pim16aap2.animatedarchitecture.core.api.IPlayer;
import nl.pim16aap2.animatedarchitecture.core.managers.StructureDeletionManager;

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
     * Represents a union type of {@link IGuiPage} and {@link StructureDeletionManager.IDeletionListener}.
     */
    interface IGuiStructureDeletionListener extends IGuiPage, StructureDeletionManager.IDeletionListener
    {
    }
}
