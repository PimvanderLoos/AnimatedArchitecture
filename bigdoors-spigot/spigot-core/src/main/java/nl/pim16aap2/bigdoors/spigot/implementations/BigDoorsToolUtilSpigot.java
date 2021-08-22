package nl.pim16aap2.bigdoors.spigot.implementations;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.api.IBigDoorsToolUtil;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.spigot.BigDoorsSpigot;
import nl.pim16aap2.bigdoors.spigot.util.SpigotAdapter;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

public class BigDoorsToolUtilSpigot implements IBigDoorsToolUtil
{
    private static final Material TOOL_MATERIAL = Material.STICK;
    private static final NamespacedKey BIG_DOORS_TOOL_KEY = new NamespacedKey(BigDoorsSpigot.get(), "BIG_DOORS_TOOL");

    @Override
    public void giveToPlayer(IPPlayer player, String name, String lore)
    {
        final @Nullable Player spigotPlayer = SpigotAdapter.getBukkitPlayer(player);
        if (spigotPlayer == null)
        {
            BigDoors.get().getPLogger().logThrowable(
                new NullPointerException("Failed to obtain Spigot player: " + player.getUUID()));
            return;
        }

        final ItemStack tool = new ItemStack(TOOL_MATERIAL, 1);
        tool.addUnsafeEnchantment(Enchantment.LUCK, 1);

        final @Nullable ItemMeta itemMeta =
            tool.hasItemMeta() ? tool.getItemMeta() : Bukkit.getItemFactory().getItemMeta(tool.getType());
        if (itemMeta == null)
            throw new IllegalArgumentException("Tried to create tool from invalid item: " + tool);
        itemMeta.getPersistentDataContainer().set(BIG_DOORS_TOOL_KEY, PersistentDataType.BYTE, (byte) 1);
        itemMeta.setDisplayName(name);
        itemMeta.setLore(Arrays.asList(lore.split("\n")));
        itemMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        tool.setItemMeta(itemMeta);

        // TODO: Make sure to give the item in their current slot, moving items if needed.
        final int heldSlot = spigotPlayer.getInventory().getHeldItemSlot();
        if (spigotPlayer.getInventory().getItem(heldSlot) == null)
            spigotPlayer.getInventory().setItem(heldSlot, tool);
        else
            spigotPlayer.getInventory().addItem(tool);
    }

    @Override
    public void removeTool(IPPlayer player)
    {
        final @Nullable Player spigotPlayer = SpigotAdapter.getBukkitPlayer(player);
        if (spigotPlayer == null)
        {
            BigDoors.get().getPLogger().logThrowable(
                new NullPointerException("Failed to obtain Spigot player: " + player.getUUID().toString()));
            return;
        }
        spigotPlayer.getInventory().forEach(
            item ->
            {
                if (isTool(item))
                    item.setAmount(0);
            });
    }

    public boolean isTool(@Nullable ItemStack item)
    {
        if (item == null || !item.getType().equals(TOOL_MATERIAL))
            return false;

        final @Nullable ItemMeta itemMeta = item.getItemMeta();
        if (itemMeta == null)
            return false;

        return itemMeta.getPersistentDataContainer().get(BIG_DOORS_TOOL_KEY, PersistentDataType.BYTE) != null;
    }

    @Override
    public boolean isPlayerHoldingTool(IPPlayer player)
    {
        final @Nullable Player spigotPlayer = SpigotAdapter.getBukkitPlayer(player);
        if (spigotPlayer == null)
        {
            BigDoors.get().getPLogger().logThrowable(
                new NullPointerException("Failed to obtain Spigot player: " + player.getUUID().toString()));
            return false;
        }

        return isPlayerHoldingTool(spigotPlayer);
    }

    public boolean isPlayerHoldingTool(Player player)
    {
        return isTool(player.getInventory().getItemInMainHand());
    }
}
