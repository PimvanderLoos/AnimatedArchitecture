package nl.pim16aap2.animatedarchitecture.spigot.core.gui;

import nl.pim16aap2.animatedarchitecture.core.api.IPlayer;
import nl.pim16aap2.animatedarchitecture.core.managers.StructureDeletionManager;

/**
 * Represents a Gui page.
 */
interface IGuiPage
{
    String getPageName();

    IPlayer getInventoryHolder();

    /**
     * Represents a union type of {@link IGuiPage} and {@link StructureDeletionManager.IDeletionListener}.
     */
    interface IGuiStructureDeletionListener extends IGuiPage, StructureDeletionManager.IDeletionListener
    {
    }
}
