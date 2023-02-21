package nl.pim16aap2.animatedarchitecture.spigot.core.implementations;

import dagger.Binds;
import dagger.Module;
import nl.pim16aap2.animatedarchitecture.core.api.IChunkLoader;

import javax.inject.Singleton;

@Module
public interface ChunkLoaderSpigotModule
{
    @Binds
    @Singleton
    IChunkLoader chunkLoader(ChunkLoaderSpigot chunkLoaderSpigot);
}
