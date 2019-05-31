package nl.pim16aap2.bigdoors.managers;

import java.util.HashMap;
import java.util.UUID;

import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import nl.pim16aap2.bigdoors.BigDoors;

public class HeadManagerv2
{
    @SuppressWarnings("unused")
    private final BigDoors plugin;
    private final HashMap<UUID, ItemStack> headMap;

    public HeadManagerv2(final BigDoors plugin)
    {
        this.plugin = plugin;
        headMap = new HashMap<>();
    }

    public ItemStack getPlayerHead(UUID playerUUID, String displayName, OfflinePlayer oPlayer)
    {
        if (headMap.containsKey(playerUUID))
            return headMap.get(playerUUID);

        ItemStack skull = new ItemStack(Material.PLAYER_HEAD, 1);
        SkullMeta smeta = (SkullMeta) skull.getItemMeta();
        smeta.setOwningPlayer(oPlayer);
        smeta.setDisplayName(displayName);
        skull.setItemMeta(smeta);

        headMap.put(playerUUID, skull);
        return skull;
    }
}
