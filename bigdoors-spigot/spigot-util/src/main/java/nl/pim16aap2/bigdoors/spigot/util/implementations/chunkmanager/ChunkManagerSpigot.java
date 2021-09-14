package nl.pim16aap2.bigdoors.spigot.util.implementations.chunkmanager;

import nl.pim16aap2.bigdoors.api.IChunkManager;
import nl.pim16aap2.bigdoors.api.IPWorld;
import nl.pim16aap2.bigdoors.util.vector.Vector2Di;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Represents an implementation of {@link IChunkManager} for the Spigot platform.
 *
 * @author Pim
 */
@Singleton
public final class ChunkManagerSpigot implements IChunkManager
{
    @Inject
    public ChunkManagerSpigot()
    {
    }

    @Override
    public boolean isLoaded(IPWorld world, Vector2Di chunk)
    {
        // TODO: Implement this.
        return true;
    }

    @Override
    public ChunkLoadResult load(IPWorld world, Vector2Di chunk)
    {
        // TODO: Implement this.
        return ChunkLoadResult.SUCCESS;
    }
}
