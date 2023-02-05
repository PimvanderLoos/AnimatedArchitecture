package nl.pim16aap2.bigdoors.spigot.core.gui;

import nl.pim16aap2.bigdoors.core.api.IPPlayer;
import nl.pim16aap2.bigdoors.core.managers.StructureDeletionManager;

/**
 * Represents a Gui page.
 */
interface IGuiPage
{
    String getPageName();

    IPPlayer getInventoryHolder();

    /**
     * Represents a union type of {@link IGuiPage} and {@link StructureDeletionManager.IDeletionListener}.
     */
    interface IGuiStructureDeletionListener extends IGuiPage, StructureDeletionManager.IDeletionListener
    {
    }
}
