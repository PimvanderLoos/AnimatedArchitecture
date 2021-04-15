package nl.pim16aap2.bigdoors.api.factories;

import lombok.NonNull;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.api.PPlayerData;

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
     * @param playerData The {@link PPlayerData} of the player.
     * @return A new {@link IPPlayer} object.
     */
    @NonNull IPPlayer create(@NonNull PPlayerData playerData);

    /**
     * Creates a new {@link IPPlayer}.
     *
     * @param uuid The {@link UUID} of the player.
     * @return A new {@link IPPlayer} object.
     */
    @NonNull CompletableFuture<Optional<IPPlayer>> create(@NonNull UUID uuid);
}
