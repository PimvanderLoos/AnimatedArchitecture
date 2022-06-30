package nl.pim16aap2.bigdoors.spigot.util.implementations.glowingblocks;

import dagger.Binds;
import dagger.Module;
import nl.pim16aap2.bigdoors.api.GlowingBlockSpawner;

import javax.inject.Singleton;

@Module
public abstract class GlowingBlockSpawnerModule
{
    @Binds
    @Singleton
    public abstract GlowingBlockSpawner getGlowingBlockSpawner(GlowingBlockSpawnerSpigot spawner);
}
