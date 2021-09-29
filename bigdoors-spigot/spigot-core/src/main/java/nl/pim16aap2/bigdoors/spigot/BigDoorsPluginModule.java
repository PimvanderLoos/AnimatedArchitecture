package nl.pim16aap2.bigdoors.spigot;

import dagger.Binds;
import dagger.Module;
import dagger.Provides;
import nl.pim16aap2.bigdoors.api.IBigDoorsPlatformProvider;
import org.bukkit.plugin.java.JavaPlugin;

import javax.inject.Named;
import javax.inject.Singleton;
import java.nio.file.Path;

@Module
public abstract class BigDoorsPluginModule
{
    @Binds
    @Singleton
    abstract JavaPlugin getPlugin(BigDoorsPlugin bigDoorsPlugin);

    @Binds
    @Singleton
    abstract IBigDoorsPlatformProvider getBigDoorsPlatformProvider(BigDoorsPlugin bigDoorsPlugin);

    @Provides
    @Singleton
    @Named("pluginSpigotID")
    static int provideSpigotPluginID()
    {
        return 58_669;
    }

    @Provides
    @Singleton
    @Named("databaseFile")
    static Path provideDatabaseFile(@Named("pluginBaseDirectory") Path pluginBaseDirectory)
    {
        return pluginBaseDirectory.resolve("doorDB.db");
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
        return pluginBaseDirectory;
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
    static long getMainThreadId(BigDoorsPlugin bigDoorsPlugin)
    {
        return bigDoorsPlugin.getMainThreadId();
    }
}
