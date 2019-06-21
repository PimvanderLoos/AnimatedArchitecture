package nl.pim16aap2.bigdoors.handlers;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.moveblocks.BlockMover;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkUnloadEvent;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class ChunkUnloadHandler implements Listener
{
    private final BigDoors plugin;
    private final boolean is1_14;
    private boolean success = false;

    public ChunkUnloadHandler(BigDoors plugin)
    {
        this.plugin = plugin;
        is1_14 = Bukkit.getServer().getClass().getPackage().getName().replace(".",  ",").split(",")[3].startsWith("v1_14");
        init();
    }

    // <1.14 method.
    private Method isCancelled;

    // 1.14 method.
    private Method isForceLoaded;

    private void init()
    {
        try
        {
            if (is1_14)
            {
                isForceLoaded = org.bukkit.Chunk.class.getMethod("isForceLoaded");
                success = true;
            }
            else
            {
                isCancelled = org.bukkit.event.world.ChunkUnloadEvent.class.getMethod("isCancelled");
                success = true;
            }
        }
        catch (NoSuchMethodException | SecurityException e)
        {
            success = false;
            plugin.getMyLogger().logException(e, "Serious error encountered! Unloading chunks with active doors IS UNSAFE!");
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChunkUnload(ChunkUnloadEvent event)
    {
        // If this class couldn't figure out reflection properly, give up.
        if (!success)
        {
            plugin.getMyLogger().warn("ChunkUnloadHandler was not initialized properly! Please contact pim16aap2.");
            return;
        }

        // If another plugin has already cancelled this event (or, forceLoaded this chunk in 1.14), there's no need to interfere.
        if (isChunkUnloadCancelled(event))
            return;

        // Find any and all doors currently operating in the chunk that's to be unloaded.
        for (BlockMover bm : plugin.getBlockMovers())
            if (bm.getDoor().chunkInRange(event.getChunk()))
                // Abort currently running blockMovers.
                bm.abort();
    }

    private boolean isChunkUnloadCancelled(ChunkUnloadEvent event)
    {
        try
        {
            if (is1_14)
                return (boolean) isForceLoaded.invoke(event.getChunk());
            return (boolean) isCancelled.invoke(event);
        }
        catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e)
        {
            plugin.getMyLogger().logException(e, "Serious error encountered! Unloading chunks with active doors IS UNSAFE!");
            return false;
        }
    }
}
