package nl.pim16aap2.bigdoors.listener;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.moveblocks.BlockMover;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Represents a listener that keeps track of chunks being unloaded.
 *
 * @author Pim
 */
public class ChunkUnloadListener implements Listener
{
    private final BigDoors plugin;
    /**
     * Checks if the ChunkUnloadEvent can be cancelled or not. In version 1.14 of Minecraft and later, that's no longer
     * the case.
     */
    private final boolean isCancellable;
    private boolean success = false;
    // <1.14 method.
    private Method isCancelled;
    // 1.14 => method.
    private Method isForceLoaded;

    public ChunkUnloadListener(final @NotNull BigDoors plugin)
    {
        this.plugin = plugin;
        isCancellable = org.bukkit.event.Cancellable.class.isAssignableFrom(ChunkUnloadEvent.class);
        init();
    }

    /**
     * Initializes the listener.
     */
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

    /**
     * Listens to chunks being unloaded and checks if it intersects with the region of any of the active {@link
     * nl.pim16aap2.bigdoors.doors.DoorBase}s.
     *
     * @param event The {@link ChunkUnloadEvent}.
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onChunkUnload(final @NotNull ChunkUnloadEvent event)
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

    /**
     * Checks if a {@link ChunkUnloadEvent} is cancelled or not.
     *
     * @param event The {@link ChunkUnloadEvent}.
     * @return The if the {@link ChunkUnloadEvent} is cancelled.
     */
    private boolean isChunkUnloadCancelled(final @NotNull ChunkUnloadEvent event)
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
