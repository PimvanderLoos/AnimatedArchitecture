package nl.pim16aap2.bigdoors.spigot.core.factories.playerfactory;

import nl.pim16aap2.bigdoors.core.api.IPlayer;
import nl.pim16aap2.bigdoors.core.api.PlayerData;
import nl.pim16aap2.bigdoors.core.api.factories.IPlayerFactory;
import nl.pim16aap2.bigdoors.core.managers.DatabaseManager;
import nl.pim16aap2.bigdoors.core.util.Util;
import nl.pim16aap2.bigdoors.spigot.util.implementations.OfflinePlayerSpigot;
import nl.pim16aap2.bigdoors.spigot.util.implementations.PlayerSpigot;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Represents an implementation of {@link IPlayerFactory} for the Spigot platform.
 *
 * @author Pim
 */
@Singleton
public class PlayerFactorySpigot implements IPlayerFactory
{
    private final DatabaseManager databaseManager;

    @Inject
    public PlayerFactorySpigot(DatabaseManager databaseManager)
    {
        this.databaseManager = databaseManager;
    }

    @Override
    public IPlayer create(PlayerData playerData)
    {
        final @Nullable Player player = Bukkit.getPlayer(playerData.getUUID());
        if (player != null)
            return new PlayerSpigot(player);
        return new OfflinePlayerSpigot(playerData);
    }

    @Override
    public CompletableFuture<Optional<IPlayer>> create(UUID uuid)
    {
        final @Nullable Player player = Bukkit.getPlayer(uuid);
        if (player != null)
            return CompletableFuture.completedFuture(Optional.of(new PlayerSpigot(player)));

        return databaseManager.getPlayerData(uuid)
                              .thenApply(playerData -> playerData.<IPlayer>map(OfflinePlayerSpigot::new))
                              .exceptionally(Util::exceptionallyOptional);
    }
}
