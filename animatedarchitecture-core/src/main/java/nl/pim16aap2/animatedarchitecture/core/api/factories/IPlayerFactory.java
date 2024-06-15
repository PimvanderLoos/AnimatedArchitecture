package nl.pim16aap2.animatedarchitecture.core.api.factories;

import nl.pim16aap2.animatedarchitecture.core.api.IPlayer;
import nl.pim16aap2.animatedarchitecture.core.api.PlayerData;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Represents a factory for {@link IPlayer} objects.
 */
public interface IPlayerFactory
{
    /**
     * Creates a new {@link IPlayer}.
     *
     * @param playerData
     *     The {@link PlayerData} of the player.
     * @return A new {@link IPlayer} object.
     */
    IPlayer create(PlayerData playerData);

    /**
     * Creates a new {@link IPlayer}.
     *
     * @param uuid
     *     The {@link UUID} of the player.
     * @return A new {@link IPlayer} object.
     */
    CompletableFuture<Optional<IPlayer>> create(UUID uuid);
}
