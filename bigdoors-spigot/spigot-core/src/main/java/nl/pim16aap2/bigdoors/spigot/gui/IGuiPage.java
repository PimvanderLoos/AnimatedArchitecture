package nl.pim16aap2.bigdoors.spigot.gui;

import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.managers.MovableRegistry;

/**
 * Represents a Gui page.
 */
interface IGuiPage
{
    String getPageName();

    IPPlayer getInventoryHolder();

    /**
     * Represents a union type of {@link IGuiPage} and {@link MovableRegistry.IDeletionListener}.
     */
    interface IGuiMovableDeletionListener extends IGuiPage, MovableRegistry.IDeletionListener
    {
    }
}
