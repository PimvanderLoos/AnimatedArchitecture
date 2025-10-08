package nl.pim16aap2.animatedarchitecture.spigot.core.config;

import lombok.AllArgsConstructor;
import lombok.Getter;
import nl.pim16aap2.animatedarchitecture.core.config.IConfigSectionResult;
import nl.pim16aap2.animatedarchitecture.core.config.RedstoneSection;
import org.bukkit.Material;
import org.jspecify.annotations.Nullable;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;

import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Represents a section in the configuration file that governs redstone settings for Spigot.
 */
@AllArgsConstructor
public class RedstoneSectionSpigot extends RedstoneSection<RedstoneSectionSpigot.Result>
{
    public static final String PATH_POWERBLOCK_TYPES = "powerblock_types";

    public static final Material DEFAULT_POWERBLOCK_MATERIAL = Material.GOLD_BLOCK;
    public static final List<String> DEFAULT_POWERBLOCK_TYPES = List.of(DEFAULT_POWERBLOCK_MATERIAL.name());

    private static final MaterialParser POWER_BLOCK_TYPE_PARSER = MaterialParser.builder()
        .context("Powerblock types")
        .defaultMaterial(DEFAULT_POWERBLOCK_MATERIAL)
        .isSolid(true)
        .build();

    @Getter
    private final @Nullable Consumer<Result> resultConsumer;

    @Override
    public CommentedConfigurationNode buildInitialLimitsNode()
        throws SerializationException
    {
        return super
            .buildInitialLimitsNode()
            .act(node -> addInitialPowerBlockTypes(node.node(PATH_POWERBLOCK_TYPES)));
    }

    private void addInitialPowerBlockTypes(CommentedConfigurationNode node)
        throws SerializationException
    {
        node.set(DEFAULT_POWERBLOCK_TYPES)
            .comment("""
                Choose the type of the power block that is used to open structures using redstone.
                This is the block that will open the structure attached to it when it receives a redstone signal.
                Multiple types are allowed.
                
                A list of options can be found here: https://hub.spigotmc.org/javadocs/spigot/org/bukkit/Material.html
                
                Default: %s
                """.formatted(formatDefaultCollection(DEFAULT_POWERBLOCK_TYPES)));
    }

    @Override
    protected Result getResult(ConfigurationNode sectionNode, boolean silent)
        throws SerializationException
    {
        return new Result(
            getAllowRedstone(sectionNode),
            getPowerBlockTypes(sectionNode, silent)
        );
    }

    private boolean getAllowRedstone(ConfigurationNode sectionNode)
    {
        return sectionNode.node(PATH_ALLOW_REDSTONE).getBoolean(DEFAULT_ALLOW_REDSTONE);
    }

    private Set<Material> getPowerBlockTypes(ConfigurationNode sectionNode, boolean silent)
        throws SerializationException
    {
        return POWER_BLOCK_TYPE_PARSER.parse(sectionNode.node(PATH_POWERBLOCK_TYPES).getList(String.class), silent);
    }

    /**
     * Represents the result of the General section configuration.
     *
     * @param allowRedstone
     *     Whether structures should respond to redstone signals.
     * @param powerblockTypes
     *     The types of blocks that can be used as power blocks for structures.
     */
    public record Result(
        boolean allowRedstone,
        Set<Material> powerblockTypes
    ) implements IConfigSectionResult
    {
        /**
         * The default result used when no data is available.
         */
        public static final Result DEFAULT = new Result(
            DEFAULT_ALLOW_REDSTONE,
            Set.of(DEFAULT_POWERBLOCK_MATERIAL)
        );

        public Result
        {
            powerblockTypes = Set.copyOf(powerblockTypes);
        }
    }
}
