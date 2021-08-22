package nl.pim16aap2.bigdoors.spigot.listeners;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.annotations.Initializer;
import nl.pim16aap2.bigdoors.api.IPWorld;
import nl.pim16aap2.bigdoors.doors.DoorBase;
import nl.pim16aap2.bigdoors.spigot.BigDoorsSpigot;
import nl.pim16aap2.bigdoors.spigot.util.SpigotAdapter;
import nl.pim16aap2.bigdoors.util.Util;
import nl.pim16aap2.bigdoors.util.vector.Vector2Di;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Represents a listener that keeps track of chunks being unloaded.
 *
 * @author Pim
 */
public class ChunkListener implements Listener
{
    private final BigDoorsSpigot plugin;
    /**
     * Checks if the ChunkUnloadEvent can be cancelled or not. In version 1.14 of Minecraft and later, that's no longer
     * the case.
     */
    private final boolean isCancellable;
    private boolean success = false;
    // <1.14 method.
    private @Nullable Method isCancelled;
    // 1.14 => method.
    private @Nullable Method isForceLoaded;

    public ChunkListener(BigDoorsSpigot plugin)
    {
        this.plugin = plugin;
        isCancellable = org.bukkit.event.Cancellable.class.isAssignableFrom(ChunkUnloadEvent.class);
        init();
    }

    /**
     * Initializes the listener.
     */
    @Initializer
    private void init()
    {
        try
        {
            if (isCancellable)
                //noinspection JavaReflectionMemberAccess
                isCancelled = org.bukkit.event.world.ChunkUnloadEvent.class.getMethod("isCancelled");
            else
                isForceLoaded = org.bukkit.Chunk.class.getMethod("isForceLoaded");
            success = true;
        }
        catch (NoSuchMethodException | SecurityException e)
        {
            success = false;
            plugin.getPLogger()
                  .logThrowable(e, "Serious error encountered! Unloading chunks with active doors IS UNSAFE!");
        }
    }

    /**
     * Listens to chunks being loaded and checks if they contain doors that move perpetually (doors, clocks, etc).
     *
     * @param event
     *     The {@link ChunkLoadEvent}.
     */
    @SuppressWarnings("CommentedOutCode")
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onChunkLoad(ChunkLoadEvent event)
    {
        long chunkHash = Util.simpleChunkHashFromChunkCoordinates(event.getChunk().getX(), event.getChunk().getZ());
        BigDoors.get().getDatabaseManager().getDoorsInChunk(chunkHash).whenComplete(
            (doors, throwable) ->
                doors.forEach(doorUID -> BigDoors.get().getDatabaseManager().getDoor(doorUID).whenComplete(
                    (optionalDoor, throwable2) ->
                        optionalDoor.ifPresent(
                            door ->
                            {
                                // TODO: (re?)Implement this
//                                if (door instanceof IPerpetualMover && door.isPowerBlockActive())
//                                    BigDoors.get().getDoorOpener()
//                                            .animateDoorAsync(door, DoorActionCause.PERPETUALMOVEMENT, null, 0,
//                                                              false, DoorActionType.TOGGLE);
                            })
                )));
    }

    /**
     * Listens to chunks being unloaded and checks if it intersects with the region of any of the active {@link
     * DoorBase}s.
     *
     * @param event
     *     The {@link ChunkUnloadEvent}.
     */
    @SuppressWarnings("CommentedOutCode")
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onChunkUnload(ChunkUnloadEvent event)
    {
        BigDoors.get().getPlatform().getPowerBlockManager()
                .invalidateChunk(event.getWorld().getName(), new Vector2Di(event.getChunk().getX(),
                                                                           event.getChunk().getZ()));
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

            IPWorld world = SpigotAdapter.wrapWorld(event.getWorld());
            Vector2Di chunkCoords = new Vector2Di(event.getChunk().getX(), event.getChunk().getZ());

//            // Abort all currently active BlockMovers that (might) interact with the chunk that is being unloaded.
//            BigDoors.get().getDoorActivityManager().getBlockMovers()
//                    .filter(BM -> BM.getDoor().chunkInRange(world, chunkCoords))
//                    .forEach(BlockMover::abort);
        }
        catch (Exception e)
        {
            plugin.getPLogger().logThrowable(e);
        }
    }

    /**
     * Checks if a {@link ChunkUnloadEvent} is cancelled or not.
     *
     * @param event
     *     The {@link ChunkUnloadEvent}.
     * @return The if the {@link ChunkUnloadEvent} is cancelled.
     */
    private boolean isChunkUnloadCancelled(ChunkUnloadEvent event)
    {
        try
        {
            if (isCancelled != null)
                return (boolean) isCancelled.invoke(event);
            else if (isForceLoaded != null)
                return (boolean) isForceLoaded.invoke(event.getChunk());
            else
                BigDoors.get().getPLogger().warn("Both isCancelled and isForceLoaded are unavailable!" +
                                                     "Chunk management is now unreliable!");
        }
        catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e)
        {
            plugin.getPLogger()
                  .logThrowable(e, "Serious error encountered! Unloading chunks with active doors IS UNSAFE!");
            return false;
        }
        return false;
    }
}
