package nl.pim16aap2.animatedarchitecture.spigot.core.listeners;

import lombok.extern.flogger.Flogger;
import nl.pim16aap2.animatedarchitecture.core.api.IExecutor;
import nl.pim16aap2.animatedarchitecture.core.api.IWorld;
import nl.pim16aap2.animatedarchitecture.core.api.restartable.RestartableHolder;
import nl.pim16aap2.animatedarchitecture.core.managers.DatabaseManager;
import nl.pim16aap2.animatedarchitecture.core.managers.PowerBlockManager;
import nl.pim16aap2.animatedarchitecture.core.moveblocks.Animator;
import nl.pim16aap2.animatedarchitecture.core.moveblocks.StructureActivityManager;
import nl.pim16aap2.animatedarchitecture.core.structures.AbstractStructure;
import nl.pim16aap2.animatedarchitecture.core.util.Rectangle;
import nl.pim16aap2.animatedarchitecture.core.util.Util;
import nl.pim16aap2.animatedarchitecture.core.util.vector.Vector2Di;
import nl.pim16aap2.animatedarchitecture.core.util.vector.Vector3Di;
import nl.pim16aap2.animatedarchitecture.spigot.util.SpigotAdapter;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.plugin.java.JavaPlugin;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Represents a listener that keeps track of chunks being unloaded.
 *
 * @author Pim
 */
@Singleton
@Flogger
public class ChunkListener extends AbstractListener
{
    /**
     * Add a delay to give the chunks surrounding the structure a chance to load as well.
     */
    private static final int PROCESS_LOAD_DELAY = 40;

    private final DatabaseManager databaseManager;
    private final PowerBlockManager powerBlockManager;
    private final StructureActivityManager structureActivityManager;
    private final IExecutor executor;

    @Inject ChunkListener(
        JavaPlugin javaPlugin,
        DatabaseManager databaseManager,
        PowerBlockManager powerBlockManager,
        RestartableHolder restartableHolder,
        StructureActivityManager structureActivityManager,
        IExecutor executor)
    {
        super(restartableHolder, javaPlugin);
        this.databaseManager = databaseManager;
        this.powerBlockManager = powerBlockManager;
        this.structureActivityManager = structureActivityManager;
        this.executor = executor;
    }

    private void onChunkLoad(World world, Chunk chunk)
    {
        final CompletableFuture<List<AbstractStructure>> rotationPoints =
            databaseManager.getStructuresInChunk(chunk.getX(), chunk.getZ());

        final CompletableFuture<List<AbstractStructure>> powerBlocks =
            powerBlockManager.structuresInChunk(new Vector3Di(chunk.getX() << 4, 0, chunk.getZ() << 4),
                                                world.getName());

        Util.getAllCompletableFutureResultsFlatMap(rotationPoints, powerBlocks)
            .thenAccept(lst -> lst.forEach(AbstractStructure::onChunkLoad));
    }

    /**
     * Listens to chunks being loaded and ensures that {@link AbstractStructure#onChunkLoad()} is called for any
     * structures whose rotation point lies in the chunk that is being loaded.
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onChunkLoad(ChunkLoadEvent event)
    {
        executor.runAsyncLater(() -> onChunkLoad(event.getWorld(), event.getChunk()), PROCESS_LOAD_DELAY);
    }

    /**
     * Listens to chunks being unloaded and checks if it intersects with the region of the active
     * {@link AbstractStructure}s.
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

            final IWorld world = SpigotAdapter.wrapWorld(event.getWorld());

            // Abort all currently active BlockMovers that (might) interact with the chunk that is being unloaded.
            structureActivityManager
                .getBlockMovers()
                .filter(mover -> mover.getSnapshot().getWorld().equals(world))
                .filter(mover -> chunkInsideAnimationRange(chunkCoords, mover.getSnapshot().getAnimationRange()))
                .forEach(Animator::abort);
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
