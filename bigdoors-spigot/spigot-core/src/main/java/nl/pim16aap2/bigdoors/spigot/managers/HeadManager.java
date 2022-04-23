package nl.pim16aap2.bigdoors.spigot.managers;

import lombok.extern.flogger.Flogger;
import nl.pim16aap2.bigdoors.api.restartable.Restartable;
import nl.pim16aap2.bigdoors.api.restartable.RestartableHolder;
import nl.pim16aap2.bigdoors.spigot.config.ConfigLoaderSpigot;
import nl.pim16aap2.bigdoors.util.Util;
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
import java.util.logging.Level;

/**
 * Represents a manager of player heads with the texture of a certain player.
 *
 * @author Pim
 */
@Singleton @Flogger
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
    private final ConfigLoaderSpigot config;

    /**
     * Constructs a new {@link HeadManager}.
     *
     * @param holder
     *     The {@link RestartableHolder} that manages this object.
     * @param config
     *     The BigDoors configuration.
     */
    @Inject
    public HeadManager(RestartableHolder holder, ConfigLoaderSpigot config)
    {
        super(holder);
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
    @SuppressWarnings("unused")
    public CompletableFuture<Optional<ItemStack>> getPlayerHead(UUID playerUUID, String displayName)
    {
        if (headMap == null)
        {
            log.at(Level.SEVERE).log("Trying to retrieve player head while head map is not initialized!");
            return CompletableFuture.completedFuture(Optional.empty());
        }
        // Satisfy NullAway that headMap won't be null.
        final var headMapNN = headMap;

        return CompletableFuture
            .supplyAsync(() -> headMapNN.computeIfAbsent(playerUUID,
                                                         (p) -> createItemStack(playerUUID, displayName)))
            .exceptionally(Util::exceptionallyOptional);
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
                            .duration(Duration.ofMinutes(config.headCacheTimeout()))
                            .cleanup(Duration.ofMinutes(Math.max(1, config.headCacheTimeout())))
                            .softReference(true).build();
    }

    @Override
    public void shutDown()
    {
        if (headMap != null)
            headMap.shutDown();
        headMap = null;
    }
}
