package nl.pim16aap2.animatedarchitecture.core.config;

import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;

public class CachingSection<N extends ConfigurationNode> implements IConfigSection
{
    public static final String SECTION_TITLE = "caching";

    public static final String PATH_CACHE_TIMEOUT = "cache_timeout";

    @Override
    public String getSectionTitle()
    {
        return SECTION_TITLE;
    }

    @Override
    public CommentedConfigurationNode buildInitialLimitsNode()
        throws SerializationException
    {
        return CommentedConfigurationNode.root()
            .comment("""
                Settings related to caching.
                
                Caching is used to speed up the plugin and reduce the load on the server.
                In general, the default values should be fine for most servers.
                
                All cache settings accept the following 'magic values':
                  -1 = no caching     (not recommended!)
                   0 = infinite cache (not recommended either!)
                """)
            .act(node ->
                node.node(PATH_CACHE_TIMEOUT)
                    .comment("Amount of time (in minutes) to cache power block positions in a chunk.")
                    .set(120)
            );
    }
}
