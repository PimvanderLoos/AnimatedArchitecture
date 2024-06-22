package nl.pim16aap2.animatedarchitecture.spigot.core;

import dagger.Binds;
import dagger.Module;
import dagger.Provides;
import nl.pim16aap2.animatedarchitecture.core.api.IAnimatedArchitecturePlatformProvider;
import org.bukkit.plugin.java.JavaPlugin;

import javax.inject.Named;
import javax.inject.Singleton;
import java.nio.file.Path;

@Module
public abstract class AnimatedArchitecturePluginModule
{
    @Binds
    @Singleton
    abstract JavaPlugin getPlugin(AnimatedArchitecturePlugin animatedArchitecturePlugin);

    @Binds
    @Singleton
    abstract IAnimatedArchitecturePlatformProvider getAnimatedArchitecturePlatformProvider(
        AnimatedArchitecturePlugin animatedArchitecturePlugin);

    @Provides
    @Singleton
    @Named("pluginSpigotID")
    static int provideSpigotPluginID()
    {
        return 58_669;
    }

    /**
     * The time between each server tick in milliseconds.
     */
    @Provides
    @Named("serverTickTime")
    static int provideServerTickTime()
    {
        return 50;
    }

    @Provides
    @Singleton
    @Named("pluginClassLoader")
    static ClassLoader provideClassLoader(AnimatedArchitecturePlugin plugin)
    {
        return plugin.getPluginClassLoader();
    }

    @Provides
    @Singleton
    @Named("databaseFile")
    static Path provideDatabaseFile(@Named("pluginBaseDirectory") Path pluginBaseDirectory)
    {
        return pluginBaseDirectory.resolve("structures.sqlite");
    }

    @Provides
    @Singleton
    @Named("pluginBaseDirectory")
    static Path providePluginBaseDirectory(JavaPlugin plugin)
    {
        return plugin.getDataFolder().toPath();
    }

    @Provides
    @Singleton
    @Named("localizationBaseDir")
    static Path provideLocalizationBaseDir(@Named("pluginBaseDirectory") Path pluginBaseDirectory)
    {
        return pluginBaseDirectory.resolve("localization");
    }

    @Provides
    @Singleton
    @Named("localizationBaseName")
    static String provideLocalizationBaseName()
    {
        return "translations";
    }

    @Provides
    @Singleton
    @Named("mainThreadId")
    static long getMainThreadId(AnimatedArchitecturePlugin animatedArchitecturePlugin)
    {
        return animatedArchitecturePlugin.getMainThreadId();
    }
}
