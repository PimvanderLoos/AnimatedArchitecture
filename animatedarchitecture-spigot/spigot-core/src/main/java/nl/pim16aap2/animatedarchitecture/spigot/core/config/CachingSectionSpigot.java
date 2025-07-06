package nl.pim16aap2.animatedarchitecture.spigot.core.config;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import nl.pim16aap2.animatedarchitecture.core.config.CachingSection;
import nl.pim16aap2.animatedarchitecture.core.config.IConfigSectionResult;
import org.jspecify.annotations.Nullable;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;

import java.util.function.Consumer;

/**
 * Represents a section in the configuration for caching settings specific to the Spigot implementation.
 * <p>
 * This section is used to configure caching behavior, such as the timeout for player head caches.
 */
@AllArgsConstructor
@NoArgsConstructor(force = true)
public class CachingSectionSpigot extends CachingSection<CachingSectionSpigot.Result>
{
    public static final String PATH_HEAD_CACHE_TIMEOUT = "head_cache_timeout";

    public static final int DEFAULT_HEAD_CACHE_TIMEOUT = 120;

    @Getter
    private final @Nullable Consumer<Result> resultConsumer;

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

    @Override
    protected Result getResult(ConfigurationNode sectionNode, boolean silent)
    {
        return new Result(
            getCacheTimeout(sectionNode),
            getHeadCacheTimeout(sectionNode)
        );
    }

    private int getCacheTimeout(ConfigurationNode sectionNode)
    {
        return sectionNode.node(PATH_POWERBLOCK_CACHE_TIMEOUT).getInt(DEFAULT_POWERBLOCK_CACHE_TIMEOUT);
    }

    private int getHeadCacheTimeout(ConfigurationNode sectionNode)
    {
        return sectionNode.node(PATH_HEAD_CACHE_TIMEOUT).getInt(DEFAULT_HEAD_CACHE_TIMEOUT);
    }

    /**
     * Represents the result of the caching configuration section.
     *
     * @param powerblockCacheTimeout
     *     The timeout for caching powerblocks (in minutes).
     * @param headCacheTimeout
     *     The timeout for player head caching (in minutes).
     */
    public record Result(
        int powerblockCacheTimeout,
        int headCacheTimeout
    ) implements IConfigSectionResult
    {
        /**
         * The default result used when no data is available.
         */
        public static final Result DEFAULT = new Result(
            CachingSection.DEFAULT_POWERBLOCK_CACHE_TIMEOUT,
            CachingSectionSpigot.DEFAULT_HEAD_CACHE_TIMEOUT
        );
    }
}
