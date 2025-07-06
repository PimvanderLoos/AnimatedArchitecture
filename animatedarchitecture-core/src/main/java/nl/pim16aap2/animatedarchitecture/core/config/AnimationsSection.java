package nl.pim16aap2.animatedarchitecture.core.config;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureAnimationRequestBuilder;
import org.jspecify.annotations.Nullable;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;

import java.util.function.Consumer;

/**
 * Represents a section in the configuration file that governs animation options.
 */
@AllArgsConstructor
@NoArgsConstructor(force = true)
public class AnimationsSection implements IConfigSection
{
    public static final String SECTION_TITLE = "animations";

    public static final String PATH_LOAD_CHUNKS_FOR_TOGGLE = "load_chunks_for_toggle";
    public static final String PATH_SKIP_ANIMATIONS_BY_DEFAULT = "skip_animations_by_default";

    public static final boolean DEFAULT_LOAD_CHUNKS_FOR_TOGGLE = true;
    public static final boolean DEFAULT_SKIP_ANIMATIONS_BY_DEFAULT = false;

    private final @Nullable Consumer<Result> resultConsumer;

    private Result buildResult(CommentedConfigurationNode root)
    {
        final var node = getSection(root);
        return new Result(
            getLoadChunksForToggle(node),
            getSkipAnimationsByDefault(node)
        );
    }

    @Override
    public void applyResults(CommentedConfigurationNode root)
    {
        if (resultConsumer == null)
            return;

        resultConsumer.accept(buildResult(root));
    }

    @Override
    public CommentedConfigurationNode buildInitialLimitsNode()
        throws SerializationException
    {
        return CommentedConfigurationNode.root()
            .comment("""
                Settings related to animations.
                
                These settings control how animations are handled in the plugin.
                """)
            .act(node ->
            {
                addInitialLoadChunksForToggle(node.node(PATH_LOAD_CHUNKS_FOR_TOGGLE));
                addInitialSkipAnimationsByDefault(node.node(PATH_SKIP_ANIMATIONS_BY_DEFAULT));
            });
    }

    private void addInitialLoadChunksForToggle(CommentedConfigurationNode node)
        throws SerializationException
    {
        node.set(DEFAULT_LOAD_CHUNKS_FOR_TOGGLE)
            .comment("""
                Whether to load chunks when toggling a structure.
                
                This is relevant when toggling a far-away structure that is in a part of the world that is not loaded.
                
                When enabled, the plugin will try to load all chunks the structure will interact with before toggling.
                If more than 1 chunk needs to be loaded, the structure will skip its animation to avoid spawning
                a bunch of entities no one can see anyway.
                
                When disabled, structures will not be toggled if more than 1 chunk needs to be loaded.
                """);
    }

    private void addInitialSkipAnimationsByDefault(CommentedConfigurationNode node)
        throws SerializationException
    {
        node.set(DEFAULT_SKIP_ANIMATIONS_BY_DEFAULT)
            .comment("""
                Whether to skip all animations by default.
                
                When enabled, toggling a structure will simply teleport the blocks to their destination without any
                animations. For structures that don't have a destination, any toggle request will be ignored.
                
                This only determines the default value, which can be overridden when explicitly enabled
                in the toggle request.
                """);
    }

    private boolean getLoadChunksForToggle(ConfigurationNode node)
    {
        return node.node(PATH_LOAD_CHUNKS_FOR_TOGGLE).getBoolean(DEFAULT_LOAD_CHUNKS_FOR_TOGGLE);
    }

    private boolean getSkipAnimationsByDefault(ConfigurationNode node)
    {
        return node.node(PATH_SKIP_ANIMATIONS_BY_DEFAULT).getBoolean(DEFAULT_SKIP_ANIMATIONS_BY_DEFAULT);
    }

    @Override
    public String getSectionTitle()
    {
        return SECTION_TITLE;
    }

    /**
     * Represents the result of this section after it was parsed.
     *
     * @param loadChunksForToggle
     *     Whether to load chunks when toggling a structure.
     * @param skipAnimationsByDefault
     *     Whether to skip animations by default.
     *     <p>
     *     This can be overridden by {@link StructureAnimationRequestBuilder.IBuilder#skipAnimation(boolean)}.
     */
    public record Result(
        boolean loadChunksForToggle,
        boolean skipAnimationsByDefault
    ) implements IConfigSectionResult
    {}
}
