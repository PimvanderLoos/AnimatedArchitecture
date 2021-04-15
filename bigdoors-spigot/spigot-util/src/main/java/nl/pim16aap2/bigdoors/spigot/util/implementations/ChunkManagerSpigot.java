package nl.pim16aap2.bigdoors.spigot.util.implementations;

import lombok.NonNull;
import nl.pim16aap2.bigdoors.api.IChunkManager;
import nl.pim16aap2.bigdoors.api.IPWorld;
import nl.pim16aap2.bigdoors.util.vector.Vector2DiConst;

/**
 * Represents an implementation of {@link IChunkManager} for the Spigot platform.
 *
 * @author Pim
 */
public final class ChunkManagerSpigot implements IChunkManager
{
    @NonNull
    private static final ChunkManagerSpigot INSTANCE = new ChunkManagerSpigot();

    private ChunkManagerSpigot()
    {
    }

    /**
     * Gets the instance of the {@link ChunkManagerSpigot} if it exists.
     *
     * @return The instance of the {@link ChunkManagerSpigot}.
     */
    public static @NonNull ChunkManagerSpigot get()
    {
        return INSTANCE;
    }

    @Override
    public boolean isLoaded(final @NonNull IPWorld world, final @NonNull Vector2DiConst chunk)
    {
        return true;
    }

    @Override
    public @NonNull ChunkLoadResult load(final @NonNull IPWorld world, final @NonNull Vector2DiConst chunk)
    {
        return ChunkLoadResult.SUCCESS;
    }
}
