package nl.pim16aap2.animatedarchitecture.spigot.core.implementations;

import com.google.common.flogger.StackSize;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.CustomLog;
import nl.pim16aap2.animatedarchitecture.core.api.IAnimatedArchitectureToolUtil;
import nl.pim16aap2.animatedarchitecture.core.api.IPlayer;
import nl.pim16aap2.util.reflection.ReflectionBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

/**
 * Implementation of {@link IAnimatedArchitectureToolUtil} for Spigot.
 * <p>
 * This class is responsible for giving the player the Animated Architecture tool and removing it from the player.
 */
@Singleton
@CustomLog
public class AnimatedArchitectureToolUtilSpigot implements IAnimatedArchitectureToolUtil
{
    private static final Material TOOL_MATERIAL = Material.STICK;

    private static final Enchantment TOOL_ENCHANTMENT = ReflectionBuilder
        .findField()
        .inClass(Enchantment.class)
        .withName("LUCK", "LUCK_OF_THE_SEA")
        .ofType(Enchantment.class)
        .get(null);

    private final NamespacedKey animatedArchitectureToolKey;

    @Inject
    public AnimatedArchitectureToolUtilSpigot(JavaPlugin javaPlugin)
    {
        animatedArchitectureToolKey = new NamespacedKey(javaPlugin, "ANIMATED_ARCHITECTURE_TOOL");
    }

    @Override
    public void giveToPlayer(IPlayer player, String nameKey, String loreKey)
    {
        final @Nullable Player spigotPlayer = PlayerFactorySpigot.unwrapPlayer(player);
        if (spigotPlayer == null)
        {
            log.atError().withStackTrace(StackSize.FULL).log("Failed to obtain Spigot player: %s", player.getUUID());
            return;
        }

        final ItemStack tool = new ItemStack(TOOL_MATERIAL, 1);
        tool.addUnsafeEnchantment(TOOL_ENCHANTMENT, 1);

        final @Nullable ItemMeta itemMeta =
            tool.hasItemMeta() ? tool.getItemMeta() : Bukkit.getItemFactory().getItemMeta(tool.getType());
        if (itemMeta == null)
            throw new IllegalArgumentException("Tried to create tool from invalid item: " + tool);

        final var localizer = player.getPersonalizedLocalizer();
        itemMeta.getPersistentDataContainer().set(animatedArchitectureToolKey, PersistentDataType.BYTE, (byte) 1);
        itemMeta.setDisplayName(localizer.getMessage(nameKey));
        itemMeta.setLore(Arrays.asList(localizer.getMessage(loreKey).split("\n")));
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
    public void removeTool(IPlayer player)
    {
        final @Nullable Player spigotPlayer = PlayerFactorySpigot.unwrapPlayer(player);
        if (spigotPlayer == null)
        {
            log.atError().withStackTrace(StackSize.FULL).log("Failed to obtain Spigot player: '%s'", player.getUUID());
            return;
        }

        spigotPlayer.getInventory().forEach(item ->
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

        return itemMeta.getPersistentDataContainer().get(animatedArchitectureToolKey, PersistentDataType.BYTE) != null;
    }

    @Override
    public boolean isPlayerHoldingTool(IPlayer player)
    {
        final @Nullable Player spigotPlayer = PlayerFactorySpigot.unwrapPlayer(player);
        if (spigotPlayer == null)
        {
            log.atError().withStackTrace(StackSize.FULL).log("Failed to obtain Spigot player: '%s'", player.getUUID());
            return false;
        }

        return isPlayerHoldingTool(spigotPlayer);
    }

    public boolean isPlayerHoldingTool(Player player)
    {
        return isTool(player.getInventory().getItemInMainHand());
    }
}
