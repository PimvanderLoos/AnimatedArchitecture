package nl.pim16aap2.bigdoors.spigot.managers;

import com.google.common.base.Preconditions;
import lombok.NonNull;
import nl.pim16aap2.bigdoors.api.restartable.IRestartableHolder;
import nl.pim16aap2.bigdoors.api.restartable.Restartable;
import nl.pim16aap2.bigdoors.spigot.config.ConfigLoaderSpigot;
import nl.pim16aap2.bigdoors.util.Util;
import nl.pim16aap2.bigdoors.util.cache.TimedCache;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
public final class HeadManager extends Restartable
{
    @Nullable
    private static HeadManager INSTANCE;

    /**
     * Timed cache of player heads.
     * <p>
     * Key: The player's UUID.
     * <p>
     * Value: The player's head as item.
     */
    @NotNull
    private final TimedCache<UUID, Optional<ItemStack>> headMap;
    @NotNull
    private final ConfigLoaderSpigot config;

    /**
     * Constructs a new {@link HeadManager}.
     *
     * @param holder The {@link IRestartableHolder} that manages this object.
     * @param config The BigDoors configuration.
     */
    private HeadManager(final @NotNull IRestartableHolder holder, final @NotNull ConfigLoaderSpigot config)
    {
        super(holder);
        this.config = config;
        headMap = TimedCache.<UUID, Optional<ItemStack>>builder()
            .duration(Duration.ofMinutes(config.headCacheTimeout())).build();
    }

    /**
     * Initializes the {@link HeadManager}. If it has already been initialized, it'll return that instance instead.
     *
     * @param holder The {@link IRestartableHolder} that manages this object.
     * @param config The BigDoors configuration.
     * @return The instance of this {@link HeadManager}.
     */
    public static @NotNull HeadManager init(final @NotNull IRestartableHolder holder,
                                            final @NotNull ConfigLoaderSpigot config)
    {
        return (INSTANCE == null) ? INSTANCE = new HeadManager(holder, config) : INSTANCE;
    }

    /**
     * Gets the instance of the {@link HeadManager} if it exists.
     *
     * @return The instance of the {@link HeadManager}.
     */
    public static @NotNull HeadManager get()
    {
        Preconditions.checkState(INSTANCE != null,
                                 "Instance has not yet been initialized. Be sure #init() has been invoked");
        return INSTANCE;
    }

    /**
     * Requests the ItemStack of a head with the texture of the player's head. This is done asynchronously because it
     * can take quite a bit of time.
     *
     * @param playerUUID  The {@link UUID} of the player whose head to get.
     * @param displayName The display name to give assign to the {@link ItemStack}.
     * @return The ItemStack of a head with the texture of the player's head if possible.
     */
    public @NotNull CompletableFuture<Optional<ItemStack>> getPlayerHead(final @NotNull UUID playerUUID,
                                                                         final @NotNull String displayName)
    {
        return CompletableFuture.supplyAsync(
            () -> headMap.computeIfAbsent(playerUUID, (p) -> createItemStack(playerUUID, displayName))
                         .flatMap(Function.identity()))
                                .exceptionally(Util::exceptionallyOptional);
    }

    private @NonNull Optional<ItemStack> createItemStack(final @NonNull UUID playerUUID,
                                                         final @NonNull String displayName)
    {
        OfflinePlayer oPlayer = Bukkit.getOfflinePlayer(playerUUID);
        ItemStack skull = new ItemStack(Material.PLAYER_HEAD, 1);
        SkullMeta smeta = (SkullMeta) skull.getItemMeta();
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
        headMap.clear();
    }

    @Override
    public void shutdown()
    {
        headMap.clear();
    }
}
