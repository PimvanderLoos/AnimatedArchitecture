package nl.pim16aap2.bigdoors.spigotutil.implementations;

import nl.pim16aap2.bigdoors.api.IChunkManager;
import nl.pim16aap2.bigdoors.api.IPWorld;
import nl.pim16aap2.bigdoors.spigotutil.SpigotAdapter;
import nl.pim16aap2.bigdoors.util.vector.Vector2Di;
import org.bukkit.Chunk;
import org.jetbrains.annotations.NotNull;

/**
 * Represents an implementation of {@link IChunkManager} for the Spigot platform.
 *
 * @author Pim
 */
public final class ChunkManagerSpigot implements IChunkManager
{
    @NotNull
    private static final ChunkManagerSpigot instance = new ChunkManagerSpigot();

    private ChunkManagerSpigot()
    {
    }

    /**
     * Gets the instance of the {@link ChunkManagerSpigot} if it exists.
     *
     * @return The instance of the {@link ChunkManagerSpigot}.
     */
    @NotNull
    public static ChunkManagerSpigot get()
    {
        return instance;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isLoaded(final @NotNull IPWorld world, final @NotNull Vector2Di chunk)
    {
        return SpigotAdapter.getBukkitLocation(new PLocationSpigot(world, chunk.getX(), 64, chunk.getY())).getChunk()
                            .isLoaded();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ChunkLoadResult load(final @NotNull IPWorld world, final @NotNull Vector2Di chunk)
    {
        Chunk bukkitChunk = SpigotAdapter.getBukkitLocation(new PLocationSpigot(world, chunk.getX(), 64, chunk.getY()))
                                         .getChunk();
        if (bukkitChunk.isLoaded())
            return ChunkLoadResult.ALREADYLOADED;

        return bukkitChunk.load() ? ChunkLoadResult.SUCCESS : ChunkLoadResult.FAIL;
    }
}
