package nl.pim16aap2.bigdoors.spigot.gui;

import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.managers.MovableDeletionManager;

/**
 * Represents a Gui page.
 */
interface IGuiPage
{
    String getPageName();

    IPPlayer getInventoryHolder();

    /**
     * Represents a union type of {@link IGuiPage} and {@link MovableDeletionManager.IDeletionListener}.
     */
    interface IGuiMovableDeletionListener extends IGuiPage, MovableDeletionManager.IDeletionListener
    {
    }
}
