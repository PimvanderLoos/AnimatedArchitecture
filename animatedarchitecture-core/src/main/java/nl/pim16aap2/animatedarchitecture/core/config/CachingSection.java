package nl.pim16aap2.animatedarchitecture.core.config;

import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;

/**
 * Represents a section in the configuration file that governs caching settings.
 *
 * @param <T>
 *     the type of result this section produces.
 */
public abstract class CachingSection<T extends IConfigSectionResult> extends ConfigSection<T>
{
    public static final String SECTION_TITLE = "caching";

    public static final String PATH_CACHE_TIMEOUT = "cache_timeout";

    public static final int DEFAULT_CACHE_TIMEOUT = 120;

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
                    .set(DEFAULT_CACHE_TIMEOUT)
            );
    }
}
