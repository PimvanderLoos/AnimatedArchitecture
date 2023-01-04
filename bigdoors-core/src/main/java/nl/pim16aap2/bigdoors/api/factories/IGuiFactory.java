package nl.pim16aap2.bigdoors.api.factories;

import nl.pim16aap2.bigdoors.api.IPPlayer;
import org.jetbrains.annotations.Nullable;

/**
 * Factory for GUI instances.
 */
public interface IGuiFactory
{
    /**
     * Creates a new GUI.
     *
     * @param inventoryHolder
     *     The player for whom to create the inventory.
     * @param source
     *     The {@link IPPlayer} whose doors will be accessed.
     *     <p>
     *     When this is null (default), the inventory holders own doors will be used.
     */
    void newGUI(IPPlayer inventoryHolder, @Nullable IPPlayer source);

    /**
     * See {@link #newGUI(IPPlayer, IPPlayer)}.
     */
    default void newGUI(IPPlayer inventoryHolder)
    {
        newGUI(inventoryHolder, null);
    }
}
