package nl.pim16aap2.bigdoors.spigot.util.implementations;

import nl.pim16aap2.bigdoors.api.IChunkManager;
import nl.pim16aap2.bigdoors.api.IPWorld;
import nl.pim16aap2.bigdoors.util.vector.Vector2DiConst;
import org.jetbrains.annotations.NotNull;

/**
 * Represents an implementation of {@link IChunkManager} for the Spigot platform.
 *
 * @author Pim
 */
public final class ChunkManagerSpigot implements IChunkManager
{
    private static final @NotNull ChunkManagerSpigot INSTANCE = new ChunkManagerSpigot();

    private ChunkManagerSpigot()
    {
    }

    /**
     * Gets the instance of the {@link ChunkManagerSpigot} if it exists.
     *
     * @return The instance of the {@link ChunkManagerSpigot}.
     */
    public static @NotNull ChunkManagerSpigot get()
    {
        return INSTANCE;
    }

    @Override
    public boolean isLoaded(final @NotNull IPWorld world, final @NotNull Vector2DiConst chunk)
    {
        return true;
    }

    @Override
    public @NotNull ChunkLoadResult load(final @NotNull IPWorld world, final @NotNull Vector2DiConst chunk)
    {
        return ChunkLoadResult.SUCCESS;
    }
}
