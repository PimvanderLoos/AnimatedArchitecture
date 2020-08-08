package nl.pim16aap2.bigdoors.spigot.util.implementations;

import nl.pim16aap2.bigdoors.api.IChunkManager;
import nl.pim16aap2.bigdoors.api.IPWorld;
import nl.pim16aap2.bigdoors.spigot.util.SpigotAdapter;
import nl.pim16aap2.bigdoors.util.vector.IVector2DiConst;
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

    @Override
    public boolean isLoaded(final @NotNull IPWorld world, final @NotNull IVector2DiConst chunk)
    {
        return SpigotAdapter.getBukkitLocation(new PLocationSpigot(world, chunk.getX(), 64, chunk.getY())).getChunk()
                            .isLoaded();
    }

    @Override
    public ChunkLoadResult load(final @NotNull IPWorld world, final @NotNull IVector2DiConst chunk)
    {
        Chunk bukkitChunk = SpigotAdapter.getBukkitLocation(new PLocationSpigot(world, chunk.getX(), 64, chunk.getY()))
                                         .getChunk();
        if (bukkitChunk.isLoaded())
            return ChunkLoadResult.ALREADYLOADED;

        return bukkitChunk.load() ? ChunkLoadResult.SUCCESS : ChunkLoadResult.FAIL;
    }
}
