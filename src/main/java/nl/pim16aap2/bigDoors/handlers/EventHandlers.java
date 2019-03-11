package nl.pim16aap2.bigDoors.handlers;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.scheduler.BukkitRunnable;

import nl.pim16aap2.bigDoors.BigDoors;
import nl.pim16aap2.bigDoors.moveBlocks.BlockMover;
import nl.pim16aap2.bigDoors.toolUsers.ToolUser;

public class EventHandlers implements Listener
{
    private final BigDoors plugin;

    public EventHandlers(BigDoors plugin)
    {
        this.plugin = plugin;
    }

    // Selection event.
    @EventHandler
    public void onLeftClick(PlayerInteractEvent event)
    {
        if (event.getAction() == Action.LEFT_CLICK_BLOCK)
        {
            if (plugin.getTF().isTool(event.getPlayer().getItemInHand()))
            {
                ToolUser tu = plugin.getToolUser(event.getPlayer());
                if (tu != null)
                {
                    tu.selector(event.getClickedBlock().getLocation());
                    event.setCancelled(true);
                    return;
                }
            }
        }
    }

//    @EventHandler
//    public void onBlockBreakEvent(BlockBreakEvent event)
//    {
//        Block block = event.getBlock();
//        if (block.hasMetadata("BDBreakTest"))
//        {
//            event.setCancelled(true);
//            block.removeMetadata("BDBreakTest", plugin);
//        }
//    }

    // Do not allow the player to drop the door creation tool.
    @EventHandler
    public void onItemDropEvent(PlayerDropItemEvent event)
    {
        if (plugin.getTF().isTool(event.getItemDrop().getItemStack()))
            event.setCancelled(true);
    }

    // Do not allow the user to move the tool around in their inventory.
    @EventHandler
    public void onItemMoved(InventoryMoveItemEvent event)
    {
        if (plugin.getTF().isTool(event.getItem()))
            event.setCancelled(true);
    }

    // Make sure any chunks that are unloaded don't contain any moving doors.
    // In the case it does happen, cancel the event, stop the door and uncancel the event a bit later.
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onChunkUnload(ChunkUnloadEvent event)
    {
        if (event.isCancelled())
            return;
        for (BlockMover bm : plugin.getBlockMovers())
        {
            if (bm.getDoor().chunkInRange(event.getChunk()))
            {
                bm.getDoor().setCanGo(false);
                event.setCancelled(true);
                new BukkitRunnable()
                {
                    @Override
                    public void run()
                    {
                       if (!event.isCancelled())
                           event.getChunk().unload(true);
                    }
                }.runTaskLater(plugin, 10);
            }
        }
    }
}
