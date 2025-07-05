package nl.pim16aap2.animatedarchitecture.spigot.core.config;

import nl.pim16aap2.animatedarchitecture.core.config.CachingSection;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;

public class CachingSectionSpigot extends CachingSection
{
    public static final String PATH_HEAD_CACHE_TIMEOUT = "head_cache_timeout";

    @Override
    public CommentedConfigurationNode buildInitialLimitsNode()
        throws SerializationException
    {
        return super
            .buildInitialLimitsNode()
            .act(node -> node
                .node(PATH_HEAD_CACHE_TIMEOUT)
                .comment("Amount of time (in minutes) to cache player heads.")
                .set(120)
            );
    }
}
