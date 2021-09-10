package nl.pim16aap2.bigdoors.spigot.managers;

import nl.pim16aap2.bigdoors.annotations.Initializer;
import nl.pim16aap2.bigdoors.api.restartable.IRestartableHolder;
import nl.pim16aap2.bigdoors.api.restartable.Restartable;
import nl.pim16aap2.bigdoors.spigot.config.ConfigLoaderSpigot;
import nl.pim16aap2.bigdoors.util.CompletableFutureHandler;
import nl.pim16aap2.bigdoors.util.cache.TimedCache;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * Represents a manager of player heads with the texture of a certain player.
 *
 * @author Pim
 */
@Singleton
public final class HeadManager extends Restartable
{
    /**
     * Timed cache of player heads.
     * <p>
     * Key: The player's UUID.
     * <p>
     * Value: The player's head as item.
     */
    private transient TimedCache<UUID, Optional<ItemStack>> headMap;
    private final ConfigLoaderSpigot config;
    private final CompletableFutureHandler handler;

    /**
     * Constructs a new {@link HeadManager}.
     *
     * @param holder
     *     The {@link IRestartableHolder} that manages this object.
     * @param config
     *     The BigDoors configuration.
     */
    @Inject
    public HeadManager(IRestartableHolder holder, ConfigLoaderSpigot config, CompletableFutureHandler handler)
    {
        super(holder);
        this.config = config;
        this.handler = handler;
        init();
    }

    @Initializer
    private void init()
    {
        headMap = TimedCache.<UUID, Optional<ItemStack>>builder()
                            .duration(Duration.ofMinutes(config.headCacheTimeout())).build();
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
    @SuppressWarnings("unused")
    public CompletableFuture<Optional<ItemStack>> getPlayerHead(UUID playerUUID, String displayName)
    {
        return CompletableFuture.supplyAsync(
                                    () -> headMap.computeIfAbsent(playerUUID,
                                                                  (p) -> createItemStack(playerUUID, displayName))
                                                 .flatMap(Function.identity()))
                                .exceptionally(handler::exceptionallyOptional);
    }

    private Optional<ItemStack> createItemStack(UUID playerUUID, String displayName)
    {
        final OfflinePlayer oPlayer = Bukkit.getOfflinePlayer(playerUUID);
        final ItemStack skull = new ItemStack(Material.PLAYER_HEAD, 1);
        final @Nullable SkullMeta smeta = (SkullMeta) skull.getItemMeta();
        if (smeta == null)
            return Optional.empty();
        smeta.setOwningPlayer(oPlayer);
        smeta.setDisplayName(displayName);
        skull.setItemMeta(smeta);
        return Optional.of(skull);
    }


    @Override
    public void restart()
    {
        final TimedCache<UUID, Optional<ItemStack>> oldHeadMap = headMap;
        init();
        oldHeadMap.clear();
    }

    @Override
    public void shutdown()
    {
        headMap.clear();
    }
}
