package nl.pim16aap2.bigdoors.handlers;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.moveblocks.BlockMover;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkUnloadEvent;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class ChunkUnloadHandler implements Listener
{
    private final BigDoors plugin;
    private final boolean isCancellable;
    private boolean success = false;
    // <1.14 method.
    private Method isCancelled;
    // 1.14 => method.
    private Method isForceLoaded;

    public ChunkUnloadHandler(BigDoors plugin)
    {
        this.plugin = plugin;
        isCancellable = org.bukkit.event.Cancellable.class.isAssignableFrom(ChunkUnloadEvent.class);
        init();
    }

    private void init()
    {
        try
        {
            if (isCancellable)
            {
                isCancelled = org.bukkit.event.world.ChunkUnloadEvent.class.getMethod("isCancelled");
                success = true;
            }
            else
            {
                isForceLoaded = org.bukkit.Chunk.class.getMethod("isForceLoaded");
                success = true;
            }
        }
        catch (NoSuchMethodException | SecurityException e)
        {
            success = false;
            plugin.getPLogger()
                  .logException(e, "Serious error encountered! Unloading chunks with active doors IS UNSAFE!");
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChunkUnload(ChunkUnloadEvent event)
    {
        try
        {
            // If this class couldn't figure out reflection properly, give up.
            if (!success)
            {
                plugin.getPLogger().warn("ChunkUnloadHandler was not initialized properly! Please contact pim16aap2.");
                return;
            }

            // If another plugin has already cancelled this event (or, forceLoaded this chunk in 1.14), there's no need to interfere.
            if (isChunkUnloadCancelled(event))
                return;

            // Find any and all doors currently operating in the chunk that's to be unloaded.
            for (BlockMover bm : plugin.getBlockMovers())
                if (bm.getDoor().getWorld().equals(event.getWorld()) &&
                        bm.getDoor().chunkInRange(event.getChunk()))
                    // Abort currently running blockMovers.
                    bm.abort();
        }
        catch (Exception e)
        {
            plugin.getPLogger().logException(e);
        }
    }

    private boolean isChunkUnloadCancelled(ChunkUnloadEvent event)
    {
        try
        {
            if (isCancellable)
                return (boolean) isCancelled.invoke(event);
            return (boolean) isForceLoaded.invoke(event.getChunk());
        }
        catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e)
        {
            plugin.getPLogger()
                  .logException(e, "Serious error encountered! Unloading chunks with active doors IS UNSAFE!");
            return false;
        }
    }
}
