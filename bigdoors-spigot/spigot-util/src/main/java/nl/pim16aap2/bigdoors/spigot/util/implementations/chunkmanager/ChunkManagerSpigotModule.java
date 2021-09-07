package nl.pim16aap2.bigdoors.spigot.util.implementations.chunkmanager;

import dagger.Binds;
import dagger.Module;
import nl.pim16aap2.bigdoors.api.IChunkManager;

import javax.inject.Singleton;

@Module
public interface ChunkManagerSpigotModule
{
    @Binds
    @Singleton
    IChunkManager getChunkManager(ChunkManagerSpigot factory);
}
