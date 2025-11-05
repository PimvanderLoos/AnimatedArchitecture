package nl.pim16aap2.animatedarchitecture.spigot.core.propertyAdapter;

import nl.pim16aap2.animatedarchitecture.core.localization.ILocalizer;
import nl.pim16aap2.animatedarchitecture.core.structures.PermissionLevel;
import nl.pim16aap2.animatedarchitecture.core.structures.Structure;
import nl.pim16aap2.animatedarchitecture.spigot.util.implementations.WrappedPlayer;

/**
 * Represents a request to create a property GUI element.
 *
 * @param structure
 *     The structure containing the property.
 * @param slotChar
 *     The character representing the slot in the GUI.
 * @param player
 *     The player for whom the GUI is being created.
 * @param permissionLevel
 *     The permission level of the player for the structure containing the property.
 * @param <T>
 *     The type of the property value.
 */
public record PropertyGuiRequest<T>(
    Structure structure,
    char slotChar,
    WrappedPlayer player,
    PermissionLevel permissionLevel
)
{
    /**
     * Gets the localizer personalized for the player.
     *
     * @return The personalized localizer.
     */
    public ILocalizer localizer()
    {
        return player.getPersonalizedLocalizer();
    }
}
