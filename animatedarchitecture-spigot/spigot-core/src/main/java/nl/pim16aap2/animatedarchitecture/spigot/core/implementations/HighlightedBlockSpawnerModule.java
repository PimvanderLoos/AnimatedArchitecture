package nl.pim16aap2.animatedarchitecture.spigot.core.implementations;

import dagger.Binds;
import dagger.Module;
import jakarta.inject.Singleton;
import nl.pim16aap2.animatedarchitecture.core.api.HighlightedBlockSpawner;

@Module
public interface HighlightedBlockSpawnerModule
{
    @Binds
    @Singleton
    HighlightedBlockSpawner getHighlightedBlockSpawner(HighlightedBlockSpawnerSpigot spawner);
}
