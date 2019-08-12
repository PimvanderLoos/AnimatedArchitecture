package nl.pim16aap2.bigdoors.managers;

import nl.pim16aap2.bigdoors.config.ConfigLoader;
import nl.pim16aap2.bigdoors.util.IRestartableHolder;
import nl.pim16aap2.bigdoors.util.Restartable;
import nl.pim16aap2.bigdoors.util.TimedMapCache;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Represents a manager of player heads with the texture of a certain player.
 *
 * @author Pim
 */
public final class HeadManager extends Restartable
{
    private static HeadManager instance;

    /**
     * Timed cache of player heads.
     * <p>
     * Key: The player's UUID.
     * <p>
     * Value: The player's head as item.
     */
    private final TimedMapCache<UUID, ItemStack> headMap;
    private final ConfigLoader config;

    /**
     * Constructs a new {@link HeadManager}.
     *
     * @param holder The {@link IRestartableHolder} that manages this object.
     * @param config The BigDoors configuration.
     */
    private HeadManager(final @NotNull IRestartableHolder holder, final @NotNull ConfigLoader config)
    {
        super(holder);
        this.config = config;
        headMap = new TimedMapCache<>(holder, ConcurrentHashMap::new, config.headCacheTimeout());
    }

    /**
     * Initializes the {@link HeadManager}. If it has already been initialized, it'll return that instance instead.
     *
     * @param holder The {@link IRestartableHolder} that manages this object.
     * @param config The BigDoors configuration.
     * @return The instance of this {@link HeadManager}.
     */
    @NotNull
    public static HeadManager init(final @NotNull IRestartableHolder holder, final @NotNull ConfigLoader config)
    {
        return (instance == null) ? instance = new HeadManager(holder, config) : instance;
    }

    /**
     * Gets the instance of the {@link HeadManager} if it exists.
     *
     * @return The instance of the {@link HeadManager}.
     */
    @Nullable
    public static HeadManager get()
    {
        return instance;
    }

    /**
     * Requests the ItemStack of a head with the texture of the player's head. This is done asynchronously because it
     * can take quite a bit of time.
     *
     * @param playerUUID  The {@link UUID} of the player whose head to get.
     * @param displayName The display name to give assign to the {@link ItemStack}.
     * @return The ItemStack of a head with the texture of the player's head if possible.
     */
    public CompletableFuture<Optional<ItemStack>> getPlayerHead(final @NotNull UUID playerUUID,
                                                                final @NotNull String displayName)
    {
        return CompletableFuture.supplyAsync(
            () ->
            {
                if (headMap.containsKey(playerUUID))
                    return Optional.of(headMap.get(playerUUID));

                OfflinePlayer oPlayer = Bukkit.getOfflinePlayer(playerUUID);
                ItemStack skull = new ItemStack(Material.PLAYER_HEAD, 1);
                SkullMeta smeta = (SkullMeta) skull.getItemMeta();
                if (smeta == null)
                    return Optional.empty();
                smeta.setOwningPlayer(oPlayer);
                smeta.setDisplayName(displayName);
                skull.setItemMeta(smeta);

                return Optional.ofNullable(headMap.put(oPlayer.getUniqueId(), skull));
            });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void restart()
    {
        headMap.reInit(config.headCacheTimeout());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void shutdown()
    {
        headMap.shutdown();
    }
}
