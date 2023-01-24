package nl.pim16aap2.bigdoors.spigot.listeners;

import lombok.extern.flogger.Flogger;
import nl.pim16aap2.bigdoors.api.IPWorld;
import nl.pim16aap2.bigdoors.api.restartable.RestartableHolder;
import nl.pim16aap2.bigdoors.managers.DatabaseManager;
import nl.pim16aap2.bigdoors.managers.PowerBlockManager;
import nl.pim16aap2.bigdoors.movable.AbstractMovable;
import nl.pim16aap2.bigdoors.moveblocks.BlockMover;
import nl.pim16aap2.bigdoors.moveblocks.MovableActivityManager;
import nl.pim16aap2.bigdoors.spigot.util.SpigotAdapter;
import nl.pim16aap2.bigdoors.util.Rectangle;
import nl.pim16aap2.bigdoors.util.vector.Vector2Di;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.plugin.java.JavaPlugin;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Represents a listener that keeps track of chunks being unloaded.
 *
 * @author Pim
 */
@Singleton
@Flogger
public class ChunkListener extends AbstractListener
{
    private final DatabaseManager databaseManager;
    private final PowerBlockManager powerBlockManager;
    private final MovableActivityManager movableActivityManager;

    @Inject
    public ChunkListener(
        JavaPlugin javaPlugin, DatabaseManager databaseManager, PowerBlockManager powerBlockManager,
        RestartableHolder restartableHolder, MovableActivityManager movableActivityManager)
    {
        super(restartableHolder, javaPlugin);
        this.databaseManager = databaseManager;
        this.powerBlockManager = powerBlockManager;
        this.movableActivityManager = movableActivityManager;
        register();
    }

    /**
     * Listens to chunks being loaded and ensures that {@link AbstractMovable#onChunkLoad()} is called for any movables
     * whose rotation point lies in the chunk that is being loaded.
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onChunkLoad(ChunkLoadEvent event)
    {
        databaseManager.getMovablesInChunk(event.getChunk().getX(), event.getChunk().getZ())
                       .thenAccept(lst -> lst.forEach(AbstractMovable::onChunkLoad));
    }

    /**
     * Listens to chunks being unloaded and checks if it intersects with the region of the active
     * {@link AbstractMovable}s.
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onChunkUnload(ChunkUnloadEvent event)
    {
        final Vector2Di chunkCoords = new Vector2Di(event.getChunk().getX(), event.getChunk().getZ());
        powerBlockManager.invalidateChunk(event.getWorld().getName(), chunkCoords);
        try
        {
            if (event.getChunk().isForceLoaded())
                return;

            final IPWorld world = SpigotAdapter.wrapWorld(event.getWorld());

            // Abort all currently active BlockMovers that (might) interact with the chunk that is being unloaded.
            movableActivityManager
                .getBlockMovers()
                .filter(mover -> mover.getSnapshot().getWorld().equals(world))
                .filter(mover -> chunkInsideAnimationRange(chunkCoords, mover.getSnapshot().getAnimationRange()))
                .forEach(BlockMover::abort);
        }
        catch (Exception e)
        {
            log.atSevere().withCause(e).log();
        }
    }

    /**
     * Checks if a chunk lies inside an animation range.
     *
     * @param chunkCoordinates
     *     The coordinates of the chunk (in chunk-space).
     * @param animationRange
     *     The animation range (in world-space).
     * @return True the chunk coordinates lie inside the animation range
     */
    private boolean chunkInsideAnimationRange(Vector2Di chunkCoordinates, Rectangle animationRange)
    {
        return animationRange.updatePositions(vec -> vec.rightShift(4)).isPosInsideRectangle(chunkCoordinates);
    }
}
