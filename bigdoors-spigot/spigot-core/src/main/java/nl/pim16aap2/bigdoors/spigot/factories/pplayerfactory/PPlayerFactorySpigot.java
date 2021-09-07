package nl.pim16aap2.bigdoors.spigot.factories.pplayerfactory;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.api.PPlayerData;
import nl.pim16aap2.bigdoors.api.factories.IPPlayerFactory;
import nl.pim16aap2.bigdoors.spigot.util.implementations.OfflinePPlayerSpigot;
import nl.pim16aap2.bigdoors.spigot.util.implementations.PPlayerSpigot;
import nl.pim16aap2.bigdoors.util.Util;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Represents an implementation of {@link IPPlayerFactory} for the Spigot platform.
 *
 * @author Pim
 */
@Singleton
public class PPlayerFactorySpigot implements IPPlayerFactory
{
    @Inject
    public PPlayerFactorySpigot()
    {
    }

    @Override
    public IPPlayer create(PPlayerData playerData)
    {
        final @Nullable Player player = Bukkit.getPlayer(playerData.getUUID());
        if (player != null)
            return new PPlayerSpigot(player);
        return new OfflinePPlayerSpigot(playerData);
    }

    @Override
    public CompletableFuture<Optional<IPPlayer>> create(UUID uuid)
    {
        final @Nullable Player player = Bukkit.getPlayer(uuid);
        if (player != null)
            return CompletableFuture.completedFuture(Optional.of(new PPlayerSpigot(player)));

        return BigDoors.get().getDatabaseManager().getPlayerData(uuid)
                       .thenApply(playerData -> playerData.<IPPlayer>map(OfflinePPlayerSpigot::new))
                       .exceptionally(Util::exceptionallyOptional);
    }
}
