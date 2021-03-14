package nl.pim16aap2.bigdoors.api;

import lombok.NonNull;

import java.util.Optional;

/**
 * Represents a BigDoors player.
 *
 * @author Pim
 */
public interface IPPlayer extends IPPlayerDataContainer, IMessageable
{
    /**
     * Gets the current location of this player.
     *
     * @return The current location of this player.
     */
    @NonNull Optional<IPLocation> getLocation();
}
