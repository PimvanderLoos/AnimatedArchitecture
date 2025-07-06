package nl.pim16aap2.animatedarchitecture.core.config;

import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;

public class AnimationsSection implements IConfigSection
{
    public static final String SECTION_TITLE = "animations";

    public static final String PATH_LOAD_CHUNKS_FOR_TOGGLE = "load_chunks_for_toggle";
    public static final String PATH_SKIP_ANIMATIONS_BY_DEFAULT = "skip_animations_by_default";

    public static final boolean DEFAULT_LOAD_CHUNKS_FOR_TOGGLE = true;
    public static final boolean DEFAULT_SKIP_ANIMATIONS_BY_DEFAULT = false;

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

    @Override
    public String getSectionTitle()
    {
        return SECTION_TITLE;
    }
}
