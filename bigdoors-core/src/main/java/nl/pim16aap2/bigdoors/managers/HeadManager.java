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

import java.util.HashMap;
import java.util.Optional;
import java.util.UUID;

/**
 * Represents a manager of player heads with the texture of a certain player.
 */
public class HeadManager extends Restartable
{
    private final TimedMapCache<UUID, ItemStack> headMap;
    private final ConfigLoader config;

    public HeadManager(final @NotNull IRestartableHolder holder, final @NotNull ConfigLoader config)
    {
        super(holder);
        this.config = config;
        headMap = new TimedMapCache<>(holder, HashMap::new, config.headCacheTimeout());
    }

    /**
     * Gets the ItemStack of a head with the texture of the player's head.
     *
     * @param playerUUID  The {@link UUID} of the player whose head to get.
     * @param displayName The display name to give assign to the {@link ItemStack}.
     * @return The ItemStack of a head with the texture of the player's head if possible.
     */
    public Optional<ItemStack> getPlayerHead(final @NotNull UUID playerUUID, final @NotNull String displayName)
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
