package nl.pim16aap2.bigdoors.core.api.factories;

import nl.pim16aap2.bigdoors.core.api.IPPlayer;
import nl.pim16aap2.bigdoors.core.api.PPlayerData;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Represents a factory for {@link IPPlayer} objects.
 *
 * @author Pim
 */
public interface IPPlayerFactory
{
    /**
     * Creates a new {@link IPPlayer}.
     *
     * @param playerData
     *     The {@link PPlayerData} of the player.
     * @return A new {@link IPPlayer} object.
     */
    IPPlayer create(PPlayerData playerData);

    /**
     * Creates a new {@link IPPlayer}.
     *
     * @param uuid
     *     The {@link UUID} of the player.
     * @return A new {@link IPPlayer} object.
     */
    CompletableFuture<Optional<IPPlayer>> create(UUID uuid);
}
