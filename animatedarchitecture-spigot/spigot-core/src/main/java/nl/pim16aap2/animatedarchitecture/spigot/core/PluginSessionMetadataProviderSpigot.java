package nl.pim16aap2.animatedarchitecture.spigot.core;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import nl.pim16aap2.animatedarchitecture.core.animation.recovery.PluginSessionMetadata;
import nl.pim16aap2.animatedarchitecture.core.animation.recovery.IPluginSessionMetadataProvider;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Provides Spigot runtime metadata for plugin session tracking.
 */
@Singleton
public final class PluginSessionMetadataProviderSpigot implements IPluginSessionMetadataProvider
{
    private final JavaPlugin plugin;

    /**
     * Creates a Spigot plugin session metadata provider.
     *
     * @param plugin
     *     The owning JavaPlugin.
     */
    @Inject
    public PluginSessionMetadataProviderSpigot(JavaPlugin plugin)
    {
        this.plugin = plugin;
    }

    @Override
    public PluginSessionMetadata getMetadata()
    {
        return new PluginSessionMetadata(
            plugin.getDescription().getVersion(),
            Bukkit.getVersion(),
            Bukkit.getBukkitVersion(),
            Bukkit.getName()
        );
    }
}
