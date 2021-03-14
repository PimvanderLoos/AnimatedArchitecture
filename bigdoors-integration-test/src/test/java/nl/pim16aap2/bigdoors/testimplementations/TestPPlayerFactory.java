package nl.pim16aap2.bigdoors.testimplementations;

import lombok.NonNull;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.api.PPlayerData;
import nl.pim16aap2.bigdoors.api.factories.IPPlayerFactory;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class TestPPlayerFactory implements IPPlayerFactory
{
    @Override
    public @NonNull IPPlayer create(@NonNull PPlayerData playerData)
    {
        return new TestPPlayer(playerData);
    }

    @Override
    public @NonNull CompletableFuture<Optional<IPPlayer>> create(@NonNull UUID uuid)
    {
        return CompletableFuture.completedFuture(Optional.empty());
    }
}
