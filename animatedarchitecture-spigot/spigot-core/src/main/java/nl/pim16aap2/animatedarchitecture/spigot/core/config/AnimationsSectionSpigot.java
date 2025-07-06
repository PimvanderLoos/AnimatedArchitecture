package nl.pim16aap2.animatedarchitecture.spigot.core.config;

import lombok.AllArgsConstructor;
import lombok.Getter;
import nl.pim16aap2.animatedarchitecture.core.config.AnimationsSection;
import nl.pim16aap2.animatedarchitecture.core.config.IConfigSectionResult;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureAnimationRequestBuilder;
import org.jspecify.annotations.Nullable;
import org.spongepowered.configurate.ConfigurationNode;

import java.util.function.Consumer;

/**
 * Represents a section in the configuration for animations specific to the Spigot implementation.
 * <p>
 * This section is used to configure animation settings, such as the default animation speed.
 */
@AllArgsConstructor
public class AnimationsSectionSpigot extends AnimationsSection<AnimationsSectionSpigot.Result>
{
    @Getter
    private final @Nullable Consumer<Result> resultConsumer;

    @Override
    protected Result getResult(ConfigurationNode sectionNode)
    {
        return new Result(
            getLoadChunksForToggle(sectionNode),
            getSkipAnimationsByDefault(sectionNode)
        );
    }

    private boolean getLoadChunksForToggle(ConfigurationNode node)
    {
        return node.node(PATH_LOAD_CHUNKS_FOR_TOGGLE).getBoolean(DEFAULT_LOAD_CHUNKS_FOR_TOGGLE);
    }

    private boolean getSkipAnimationsByDefault(ConfigurationNode node)
    {
        return node.node(PATH_SKIP_ANIMATIONS_BY_DEFAULT).getBoolean(DEFAULT_SKIP_ANIMATIONS_BY_DEFAULT);
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
    ) implements IConfigSectionResult {}
}
