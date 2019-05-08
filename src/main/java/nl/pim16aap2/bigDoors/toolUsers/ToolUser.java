package nl.pim16aap2.bigDoors.toolUsers;

import java.util.Arrays;
import java.util.logging.Level;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import nl.pim16aap2.bigDoors.BigDoors;
import nl.pim16aap2.bigDoors.Door;
import nl.pim16aap2.bigDoors.util.Abortable;
import nl.pim16aap2.bigDoors.util.DoorDirection;
import nl.pim16aap2.bigDoors.util.DoorType;
import nl.pim16aap2.bigDoors.util.Messages;
import nl.pim16aap2.bigDoors.util.RotateDirection;
import nl.pim16aap2.bigDoors.util.Util;

public abstract class ToolUser extends Abortable
{
    protected DoorType type;
    protected String name;
    protected final BigDoors plugin;
    protected Player player;
    protected long doorUID;
    protected final Messages messages;
    protected DoorDirection engineSide;
    protected boolean done = false;
    protected boolean isOpen = false;
    protected Location one, two, engine;
    protected boolean aborting = false;
    protected RotateDirection openDir = null;

    public ToolUser(BigDoors plugin, Player player, String name, DoorType type)
    {
        this.plugin = plugin;
        messages = plugin.getMessages();
        this.player = player;
        this.name = name;
        one = null;
        two = null;
        engine = null;
        engineSide = null;
        this.type = type;
        plugin.addToolUser(this);
    }

    // Handle location input (player hitting a block).
    public abstract void selector(Location loc);

    // Give a tool to a player (but get correct strings etc from translation file
    // first).
    protected abstract void triggerGiveTool();

    // Finish up (but get correct strings etc from translation file first).
    protected abstract void triggerFinishUp();

    // Check if all the variables that cannot be null are not null.
    protected abstract boolean isReadyToCreateDoor();

    // Final cleanup and door creation.
    protected final void finishUp(String message)
    {
        if (isReadyToCreateDoor() && !aborting)
        {
            World world     = one.getWorld();
            Location min    = new Location(world, one.getBlockX(), one.getBlockY(), one.getBlockZ());
            Location max    = new Location(world, two.getBlockX(), two.getBlockY(), two.getBlockZ());
            Location engine = new Location(world, this.engine.getBlockX(), this.engine.getBlockY(), this.engine.getBlockZ());
            Location powerB = new Location(world, this.engine.getBlockX(), this.engine.getBlockY() - 1, this.engine.getBlockZ());

            String canBreakBlock = plugin.canBreakBlocksBetweenLocs(player, min, max);
            if (canBreakBlock != null)
            {
                Util.messagePlayer(player, messages.getString("CREATOR.GENERAL.NoPermissionHere") + " " + canBreakBlock);
                this.abort(false);
                return;
            }

            Door door = new Door(player.getUniqueId(), world, min, max, engine, name, isOpen, -1, false,
                                 0, type, engineSide, powerB, openDir, -1);

            int doorSize = door.getBlockCount();
            int sizeLimit = Util.getMaxDoorSizeForPlayer(player);

            if (sizeLimit >= 0 && sizeLimit <= doorSize)
                Util.messagePlayer(player, messages.getString("CREATOR.GENERAL.TooManyBlocks") + " " + sizeLimit);
            else if (plugin.getEconomyManager().buyDoor(player, type, doorSize))
            {
                plugin.getCommander().addDoor(door);
                if (message != null)
                    Util.messagePlayer(player, message);
            }
        }
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

        Util.messagePlayer(player, message);
    }

    public final Player getPlayer()
    {
        return player;
    }

    public final void setName(String newName)
    {
        name = newName;
        triggerGiveTool();
    }

    public final String getName()
    {
        return name;
    }

    // Take any selection tools in the player's inventory from them.
    public final void takeToolFromPlayer()
    {
        for (ItemStack is : player.getInventory())
            if (is != null)
                if (plugin.getTF().isTool(is))
                    is.setAmount(0);
    }

    // Make sure position "one" contains the minimum values, "two" the maximum
    // values and engine min.Y;
    protected final void minMaxFix()
    {
        int minX = one.getBlockX();
        int minY = one.getBlockY();
        int minZ = one.getBlockZ();
        int maxX = two.getBlockX();
        int maxY = two.getBlockY();
        int maxZ = two.getBlockZ();

        one.setX(minX > maxX ? maxX : minX);
        one.setY(minY > maxY ? maxY : minY);
        one.setZ(minZ > maxZ ? maxZ : minZ);
        two.setX(minX < maxX ? maxX : minX);
        two.setY(minY < maxY ? maxY : minY);
        two.setZ(minZ < maxZ ? maxZ : minZ);
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
        cancelTask();
        plugin.removeToolUser(this);
        if (!done)
            // TODO: This is dumb. Casting player to CommandSender and then checking if it's
            // a player or console.
            plugin.getMyLogger().returnToSender(player, Level.INFO, ChatColor.RED,
                                                messages.getString("CREATOR.GENERAL.TimeUp"));
    }

    @Override
    public final void abort()
    {
        abort(false);
    }
}
