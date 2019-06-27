package nl.pim16aap2.bigdoors.toolusers;

import java.util.Arrays;
import java.util.logging.Level;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.util.Messages;
import nl.pim16aap2.bigdoors.spigotutil.Abortable;
import nl.pim16aap2.bigdoors.spigotutil.SpigotUtil;

public abstract class ToolUser extends Abortable
{
    protected long doorUID;
    protected final BigDoors plugin;
    protected Player player;
    protected final Messages messages;
    protected boolean done = false;
    protected boolean aborting = false;

    public ToolUser(BigDoors plugin, Player player)
    {
        this.plugin = plugin;
        messages = plugin.getMessages();
        this.player = player;
        plugin.addToolUser(this);
    }

    // Handle location input (player hitting a block).
    public abstract void selector(Location loc);

    // Give a tool to a player (but get correct strings etc from translation file
    // first).
    protected abstract void triggerGiveTool();

    // Finish up (but get correct strings etc from translation file first).
    protected abstract void triggerFinishUp();

    // Final cleanup and door creation.
    protected final void finishUp()
    {
        takeToolFromPlayer();
        this.abort();
    }

    protected final void giveToolToPlayer(String[] lore, String[] message)
    {
        ItemStack tool = new ItemStack(Material.STICK, 1);
        tool.addUnsafeEnchantment(Enchantment.LUCK, 1);
        tool.getItemMeta().addItemFlags(ItemFlag.HIDE_ENCHANTS);

        ItemMeta itemMeta = tool.getItemMeta();
        itemMeta.setDisplayName(messages.getString("CREATOR.GENERAL.StickName"));
        itemMeta.setLore(Arrays.asList(lore));
        tool.setItemMeta(itemMeta);

        int heldSlot = player.getInventory().getHeldItemSlot();
        if (player.getInventory().getItem(heldSlot) == null)
            player.getInventory().setItem(heldSlot, tool);
        else
            player.getInventory().addItem(tool);

        SpigotUtil.messagePlayer(player, message);
    }

    public final Player getPlayer()
    {
        return player;
    }

    // Take any selection tools in the player's inventory from them.
    public final void takeToolFromPlayer()
    {
        player.getInventory().forEach(K ->
        {
            if (plugin.getTF().isTool(K))
                K.setAmount(0);
        });
    }

    // See if this class is done.
    public final boolean isDone()
    {
        return done;
    }

    // Change isDone status and
    public final void setIsDone(boolean bool)
    {
        done = bool;
        if (bool)
            triggerFinishUp();
    }

    @Override
    public final void abort(boolean onDisable)
    {
        aborting = true;
        takeToolFromPlayer();
        if (onDisable)
            return;
        killTask();
        plugin.removeToolUser(this);
        if (!done)
            plugin.getMyLogger().sendMessageToTarget(player, Level.INFO,
                                                   ChatColor.RED + messages.getString("CREATOR.GENERAL.TimeUp"));
    }

    @Override
    public void abortSilently()
    {
        done = true;
        abort(false);
    }
}
