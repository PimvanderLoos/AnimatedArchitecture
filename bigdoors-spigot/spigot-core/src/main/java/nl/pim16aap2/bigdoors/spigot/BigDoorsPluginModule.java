package nl.pim16aap2.bigdoors.spigot;

import dagger.Binds;
import dagger.Module;
import dagger.Provides;
import org.bukkit.plugin.java.JavaPlugin;

import javax.inject.Named;
import javax.inject.Singleton;
import java.io.File;
import java.nio.file.Path;

@Module
public abstract class BigDoorsPluginModule
{
    @Binds
    @Singleton
    abstract JavaPlugin getPlugin(BigDoorsPlugin bigDoorsPlugin);

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
    static File provideDatabaseFile(JavaPlugin plugin)
    {
        return new File(plugin.getDataFolder(), "doorDB.db");
    }

    @Provides
    @Singleton
    @Named("pluginBaseDirectory")
    static File providePluginBaseDirectory(JavaPlugin plugin)
    {
        return plugin.getDataFolder();
    }

    @Provides
    @Singleton
    @Named("logFile")
    static File provideLogFile(JavaPlugin plugin)
    {
        return new File(plugin.getDataFolder(), "log.txt");
    }

    @Provides
    @Singleton
    @Named("localizationBaseDir")
    static Path provideLocalizationBaseDir(JavaPlugin plugin)
    {
        return plugin.getDataFolder().toPath().resolve("");
    }

    @Provides
    @Singleton
    @Named("localizationBaseName")
    static String provideLocalizationBaseName()
    {
        return "translations";
    }
}
