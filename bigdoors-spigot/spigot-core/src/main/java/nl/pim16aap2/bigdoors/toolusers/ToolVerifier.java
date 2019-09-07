package nl.pim16aap2.bigdoors.toolusers;

import nl.pim16aap2.bigdoors.util.IRestartableHolder;
import nl.pim16aap2.bigdoors.util.Restartable;
import nl.pim16aap2.bigdoors.util.messages.Message;
import nl.pim16aap2.bigdoors.util.messages.Messages;
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
public class ToolVerifier extends Restartable
{
    @NotNull
    private final Messages messages;
    @NotNull
    private String stickName = "";

    public ToolVerifier(final @NotNull Messages messages, final @NotNull IRestartableHolder holder)
    {
        super(holder);
        this.messages = messages;
        restart();
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
            is.getType().equals(Material.STICK) &&
            is.getEnchantmentLevel(Enchantment.LUCK) == 1 &&
            is.getItemMeta() != null &&
            is.getItemMeta().getDisplayName().equals(stickName);
    }

    /**
     * Handles a restart.
     */
    @Override
    public void restart()
    {
        stickName = messages.getString(Message.CREATOR_GENERAL_STICKNAME);
    }

    /**
     * Handles a shutdown..
     */
    @Override
    public void shutdown()
    {
    }
}
