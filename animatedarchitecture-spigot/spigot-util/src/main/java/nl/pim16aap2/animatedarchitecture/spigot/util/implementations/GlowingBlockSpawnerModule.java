package nl.pim16aap2.animatedarchitecture.spigot.util.implementations;

import dagger.Binds;
import dagger.Module;
import nl.pim16aap2.animatedarchitecture.core.api.GlowingBlockSpawner;

import javax.inject.Singleton;

@Module
public abstract class GlowingBlockSpawnerModule
{
    @Binds
    @Singleton
    public abstract GlowingBlockSpawner getGlowingBlockSpawner(GlowingBlockSpawnerSpigot spawner);
}
