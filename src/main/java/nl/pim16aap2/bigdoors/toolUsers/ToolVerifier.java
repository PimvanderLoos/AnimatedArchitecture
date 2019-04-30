package nl.pim16aap2.bigdoors.toolUsers;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;

public class ToolVerifier
{
    private String toolName;

    public ToolVerifier(String str)
    {
        toolName = str;
    }

    // Check if the provided itemstack is a selection tool.
    public boolean isTool(ItemStack is)
    {
        return  is.getType() == Material.STICK                &&
                is.getEnchantmentLevel(Enchantment.LUCK) == 1 &&
                is.getItemMeta().getDisplayName() != null     &&
                is.getItemMeta().getDisplayName().toString().equals(toolName);
    }
}
