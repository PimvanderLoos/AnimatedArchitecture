package nl.pim16aap2.bigdoors.toolusers;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents an object that checks if items are actually BigDoors creator tools.
 *
 * @author Pim
 */
public class ToolVerifier
{
    private String toolName;

    public ToolVerifier(final @NotNull String str)
    {
        toolName = str;
    }

    /**
     * Checks if a provided {@link ItemStack} is a BigDoors creator tool
     *
     * @param is The {@link ItemStack}.
     * @return True if the item is a BigDoors creator tool
     */
    public boolean isTool(final @Nullable ItemStack is)
    {
        return is != null &&
            is.getType() == Material.STICK &&
            is.getEnchantmentLevel(Enchantment.LUCK) == 1 &&
            is.getItemMeta().getDisplayName() != null &&
            is.getItemMeta().getDisplayName().equals(toolName);
    }
}
