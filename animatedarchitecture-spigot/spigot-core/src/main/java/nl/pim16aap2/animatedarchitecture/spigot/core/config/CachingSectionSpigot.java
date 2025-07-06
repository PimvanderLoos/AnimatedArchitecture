package nl.pim16aap2.animatedarchitecture.spigot.core.config;

import nl.pim16aap2.animatedarchitecture.core.config.CachingSection;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;

/**
 * Represents a section in the configuration for caching settings specific to the Spigot implementation.
 * <p>
 * This section is used to configure caching behavior, such as the timeout for player head caches.
 */
public class CachingSectionSpigot extends CachingSection
{
    public static final String PATH_HEAD_CACHE_TIMEOUT = "head_cache_timeout";

    public static final int DEFAULT_HEAD_CACHE_TIMEOUT = 120;

    @Override
    public CommentedConfigurationNode buildInitialLimitsNode()
        throws SerializationException
    {
        return super
            .buildInitialLimitsNode()
            .act(node -> node
                .node(PATH_HEAD_CACHE_TIMEOUT)
                .comment("Amount of time (in minutes) to cache player heads.")
                .set(DEFAULT_HEAD_CACHE_TIMEOUT)
            );
    }
}
