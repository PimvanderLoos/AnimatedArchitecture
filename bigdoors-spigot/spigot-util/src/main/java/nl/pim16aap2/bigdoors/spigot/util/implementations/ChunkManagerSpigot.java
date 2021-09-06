package nl.pim16aap2.bigdoors.spigot.util.implementations;

import nl.pim16aap2.bigdoors.api.IChunkManager;
import nl.pim16aap2.bigdoors.api.IPWorld;
import nl.pim16aap2.bigdoors.util.vector.Vector2Di;

import javax.inject.Singleton;

/**
 * Represents an implementation of {@link IChunkManager} for the Spigot platform.
 *
 * @author Pim
 */
@Singleton
public final class ChunkManagerSpigot implements IChunkManager
{
    private static final ChunkManagerSpigot INSTANCE = new ChunkManagerSpigot();

    private ChunkManagerSpigot()
    {
    }

    /**
     * Gets the instance of the {@link ChunkManagerSpigot} if it exists.
     *
     * @return The instance of the {@link ChunkManagerSpigot}.
     */
    public static ChunkManagerSpigot get()
    {
        return INSTANCE;
    }

    @Override
    public boolean isLoaded(IPWorld world, Vector2Di chunk)
    {
        return true;
    }

    @Override
    public ChunkLoadResult load(IPWorld world, Vector2Di chunk)
    {
        return ChunkLoadResult.SUCCESS;
    }
}
