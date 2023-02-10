package nl.pim16aap2.bigdoors.core.api.factories;

import nl.pim16aap2.bigdoors.core.api.IPlayer;
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
     *     The {@link IPlayer} whose structures will be accessed.
     *     <p>
     *     When this is null (default), the inventory holders own structures will be used.
     */
    void newGUI(IPlayer inventoryHolder, @Nullable IPlayer source);

    /**
     * See {@link #newGUI(IPlayer, IPlayer)}.
     */
    default void newGUI(IPlayer inventoryHolder)
    {
        newGUI(inventoryHolder, null);
    }
}
