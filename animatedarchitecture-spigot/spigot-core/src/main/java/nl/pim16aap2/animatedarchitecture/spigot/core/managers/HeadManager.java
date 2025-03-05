package nl.pim16aap2.animatedarchitecture.spigot.core.managers;

import lombok.extern.flogger.Flogger;
import nl.pim16aap2.animatedarchitecture.core.api.IExecutor;
import nl.pim16aap2.animatedarchitecture.core.api.restartable.Restartable;
import nl.pim16aap2.animatedarchitecture.core.api.restartable.RestartableHolder;
import nl.pim16aap2.animatedarchitecture.core.data.cache.timed.TimedCache;
import nl.pim16aap2.animatedarchitecture.spigot.core.config.ConfigSpigot;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Represents a manager of player heads with the texture of a certain player.
 */
@Singleton
@Flogger
public final class HeadManager extends Restartable
{
    /**
     * Timed cache of player heads.
     * <p>
     * Key: The player's UUID.
     * <p>
     * Value: The player's head as item.
     */
    private @Nullable TimedCache<UUID, Optional<ItemStack>> headMap;

    private final IExecutor executor;
    private final ConfigSpigot config;

    /**
     * Constructs a new {@link HeadManager}.
     *
     * @param holder
     *     The {@link RestartableHolder} that manages this object.
     * @param config
     *     The AnimatedArchitecture configuration.
     */
    @Inject
    public HeadManager(RestartableHolder holder, IExecutor executor, ConfigSpigot config)
    {
        super(holder);
        this.executor = executor;
        this.config = config;
    }

    /**
     * Requests the ItemStack of a head with the texture of the player's head. This is done asynchronously because it
     * can take quite a bit of time.
     *
     * @param playerUUID
     *     The {@link UUID} of the player whose head to get.
     * @param displayName
     *     The display name to give assign to the {@link ItemStack}.
     * @return The ItemStack of a head with the texture of the player's head if possible.
     */
    public CompletableFuture<Optional<ItemStack>> getPlayerHead(UUID playerUUID, String displayName)
    {
        final var headMap0 = headMap;
        if (headMap0 == null)
        {
            log.atSevere().log("Trying to retrieve player head while head map is not initialized!");
            return CompletableFuture.completedFuture(Optional.empty());
        }

        return CompletableFuture
            .supplyAsync(() -> Objects.requireNonNull(
                    headMap0.computeIfAbsent(playerUUID, (p) -> createItemStack(playerUUID, displayName))),
                executor.getVirtualExecutor())
            .exceptionally(ex ->
            {
                log.atSevere().withCause(ex).log(
                    "Failed to get player head for player with UUID: %s and display name: %s",
                    playerUUID,
                    displayName
                );
                return Optional.empty();
            });
    }

    private Optional<ItemStack> createItemStack(UUID playerUUID, String displayName)
    {
        final OfflinePlayer oPlayer = Bukkit.getOfflinePlayer(playerUUID);
        final ItemStack skull = new ItemStack(Material.PLAYER_HEAD, 1);
        final @Nullable SkullMeta sMeta = (SkullMeta) skull.getItemMeta();
        if (sMeta == null)
            return Optional.empty();

        sMeta.setOwningPlayer(oPlayer);
        sMeta.setDisplayName(displayName);
        skull.setItemMeta(sMeta);
        return Optional.of(skull);
    }

    @Override
    public void initialize()
    {
        headMap = TimedCache.<UUID, Optional<ItemStack>>builder()
            .timeOut(Duration.ofMinutes(config.headCacheTimeout()))
            .cleanup(Duration.ofMinutes(Math.max(1, config.headCacheTimeout())))
            .softReference(true)
            .build();
    }

    @Override
    public void shutDown()
    {
        if (headMap != null)
            headMap.shutDown();
        headMap = null;
    }
}
