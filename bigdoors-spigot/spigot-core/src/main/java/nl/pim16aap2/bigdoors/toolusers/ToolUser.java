package nl.pim16aap2.bigdoors.toolusers;

import nl.pim16aap2.bigdoors.BigDoorsSpigot;
import nl.pim16aap2.bigdoors.spigotutil.AbortableTask;
import nl.pim16aap2.bigdoors.spigotutil.SpigotUtil;
import nl.pim16aap2.bigdoors.util.messages.Message;
import nl.pim16aap2.bigdoors.util.messages.Messages;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.logging.Level;

/**
 * Represents parent class of all tool related objects.
 */
public abstract class ToolUser extends AbortableTask
{
    protected final BigDoorsSpigot plugin;
    protected final Messages messages;
    protected long doorUID;
    protected Player player;
    protected boolean done = false;
    protected boolean aborting = false;

    ToolUser(final @NotNull BigDoorsSpigot plugin, final @NotNull Player player)
    {
        this.plugin = plugin;
        messages = plugin.getMessages();
        this.player = player;
        plugin.addToolUser(this);
    }

    /**
     * Handles location input. i.e. a {@link Player} hitting a block with a tool.
     *
     * @param loc The {@link Location}
     */
    public abstract void selector(final @NotNull Location loc);

    /**
     * Finishes up (but get correct strings etc from translation file first).
     */
    protected abstract void triggerFinishUp();

    /**
     * Takes the tool away from the player and aborts the task.
     */
    protected final void finishUp()
    {
        takeToolFromPlayer();
        abort();
    }

    /**
     * Gets the message sent to the player when they receive a tool.
     *
     * @return The message sent to the player when they receive a tool.
     */
    @NotNull
    protected abstract String getToolReceivedMessage();

    /**
     * Gets the lore of the tool.
     *
     * @return The lore of the tool.
     */
    @NotNull
    protected abstract String getToolLore();

    /**
     * Gives the player the tool
     */
    protected final void giveToolToPlayer()
    {
        ItemStack tool = new ItemStack(Material.STICK, 1);
        tool.addUnsafeEnchantment(Enchantment.LUCK, 1);
        tool.getItemMeta().addItemFlags(ItemFlag.HIDE_ENCHANTS);

        ItemMeta itemMeta = tool.getItemMeta();
        itemMeta.setDisplayName(messages.getString(Message.CREATOR_GENERAL_STICKNAME));
        itemMeta.setLore(Arrays.asList(getToolLore().split("\n")));
        tool.setItemMeta(itemMeta);

        int heldSlot = player.getInventory().getHeldItemSlot();
        if (player.getInventory().getItem(heldSlot) == null)
            player.getInventory().setItem(heldSlot, tool);
        else
            player.getInventory().addItem(tool);

        SpigotUtil.messagePlayer(player, getToolReceivedMessage().split("\n"));
    }

    /**
     * Gets the {@link Player} that's using the tool.
     */
    public final @NotNull Player getPlayer()
    {
        return player;
    }

    /**
     * Take any selection tools in the player's inventory from them.
     */
    public final void takeToolFromPlayer()
    {
        player.getInventory().forEach(K ->
                                      {
                                          if (plugin.getTF().isTool(K))
                                              K.setAmount(0);
                                      });
    }

    /**
     * Changes the status of the process.
     *
     * @param bool Whether or not the process is done or not.
     */
    public final void setIsDone(final boolean bool)
    {
        done = bool;
        if (bool)
            triggerFinishUp();
    }

    /**
     * Aborts the process. When aborting while disabling the plugin, some steps are skipped.
     *
     * @param onDisable Whether or not the plugin is being disabled.
     */
    @Override
    public final void abort(final boolean onDisable)
    {
        aborting = true;
        takeToolFromPlayer();
        killTask();
        if (onDisable)
            return;
        plugin.removeToolUser(this);
        if (!done)
            plugin.getPLogger().sendMessageToTarget(player, Level.INFO,
                                                    messages.getString(Message.CREATOR_GENERAL_TIMEOUT));
    }

    /**
     * Aborts the process without notifying the player.
     */
    @Override
    public void abortSilently()
    {
        done = true;
        abort(false);
    }
}
