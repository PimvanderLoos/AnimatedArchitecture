package nl.pim16aap2.animatedarchitecture.spigot.core.config;

import lombok.AllArgsConstructor;
import lombok.Getter;
import nl.pim16aap2.animatedarchitecture.core.config.GeneralSection;
import nl.pim16aap2.animatedarchitecture.core.config.IConfigSectionResult;
import org.bukkit.Material;
import org.jspecify.annotations.Nullable;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;

import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Represents a section in the configuration file that governs general settings for Spigot.
 */
@AllArgsConstructor
public class GeneralSectionSpigot extends GeneralSection<GeneralSectionSpigot.Result>
{
    public static final String PATH_POWER_BLOCK_TYPES = "power_block_types";
    public static final String PATH_MATERIAL_BLACKLIST = "material_blacklist";
    public static final String PATH_RESOURCE_PACK_ENABLED = "resource_pack_enabled";
    public static final String PATH_COMMAND_ALIASES = "command_aliases";

    public static final Material DEFAULT_POWER_BLOCK_MATERIAL = Material.GOLD_BLOCK;
    public static final String[] DEFAULT_POWER_BLOCK_TYPES = new String[]{DEFAULT_POWER_BLOCK_MATERIAL.name()};
    public static final String[] DEFAULT_MATERIAL_BLACKLIST = new String[0];
    public static final boolean DEFAULT_RESOURCE_PACK_ENABLED = true;
    public static final String[] DEFAULT_COMMAND_ALIASES = new String[]{
        "animatedarchitecture",
        "AnimatedArchitecture",
        "aa"
    };

    private static final MaterialParser POWER_BLOCK_TYPE_PARSER = MaterialParser.builder()
        .context("Powerblock types")
        .defaultMaterial(DEFAULT_POWER_BLOCK_MATERIAL)
        .isSolid(true)
        .build();

    private static final MaterialParser MATERIAL_BLACKLIST_PARSER = MaterialParser.builder()
        .context("Material blacklist")
        .build();

    @Getter
    private final @Nullable Consumer<Result> resultConsumer;

    @Override
    public CommentedConfigurationNode buildInitialLimitsNode()
        throws SerializationException
    {
        return super
            .buildInitialLimitsNode()
            .act(node ->
            {
                addInitialPowerBlockTypes(node.node(PATH_POWER_BLOCK_TYPES));
                addInitialMaterialBlacklist(node.node(PATH_MATERIAL_BLACKLIST));
                addInitialResourcePackEnabled(node.node(PATH_RESOURCE_PACK_ENABLED));
                addInitialCommandAliases(node.node(PATH_COMMAND_ALIASES));
            });
    }

    private void addInitialPowerBlockTypes(CommentedConfigurationNode node)
        throws SerializationException
    {
        node.set(DEFAULT_POWER_BLOCK_TYPES)
            .comment("""
                Choose the type of the power block that is used to open structures using redstone.
                This is the block that will open the structure attached to it when it receives a redstone signal.
                Multiple types are allowed.
                
                A list of options can be found here: https://hub.spigotmc.org/javadocs/spigot/org/bukkit/Material.html
                """);
    }

    private void addInitialMaterialBlacklist(CommentedConfigurationNode node)
        throws SerializationException
    {
        node.set(DEFAULT_MATERIAL_BLACKLIST)
            .comment("""
                List of blacklisted materials. Materials on this list can not be animated.
                
                Use the same list of materials as for the power blocks.
                """);
    }

    private void addInitialResourcePackEnabled(CommentedConfigurationNode node)
        throws SerializationException
    {
        node.set(DEFAULT_RESOURCE_PACK_ENABLED)
            .comment("""
                Whether the resource pack should be enabled.
                
                When enabled, players will be prompted to download the resource pack when they join the server.
                The resource pack is used to display animated blocks correctly.
                
                When disabled, players will not be prompted to download the resource pack.
                
                On 1.20, enabling this will cause conflicts with other resource packs, and you will have to merge them
                manually and disable this option.
                On 1.21 and later, you can enable this without any issues.
                """);
    }

    private void addInitialCommandAliases(CommentedConfigurationNode node)
        throws SerializationException
    {
        node.set(DEFAULT_COMMAND_ALIASES)
            .comment("""
                List of aliases for the `/animatedarchitecture` command.
                
                These aliases can be used to run the plugin commands without typing the full command name.
                For example, if you add "aa" as an alias, you can run "/aa menu" instead of
                "/animatedarchitecture menu".
                
                The first alias will be used as the main command.
                
                Aliases are case-sensitive, can not contain spaces, and should not have a leading slash.
                
                Changing this will require a server restart to take effect.
                """);
    }

    @Override
    protected Result getResult(ConfigurationNode sectionNode, boolean silent)
        throws SerializationException
    {
        return new Result(
            getAllowRedstone(sectionNode),
            getPowerBlockTypes(sectionNode, silent),
            getMaterialBlackList(sectionNode, silent),
            getResourcePackEnabled(sectionNode),
            getCommandAliases(sectionNode)
        );
    }

    private boolean getAllowRedstone(ConfigurationNode sectionNode)
    {
        return sectionNode.node(PATH_ALLOW_REDSTONE).getBoolean(DEFAULT_ALLOW_REDSTONE);
    }

    private Set<Material> getPowerBlockTypes(ConfigurationNode sectionNode, boolean silent)
        throws SerializationException
    {
        return POWER_BLOCK_TYPE_PARSER.parse(sectionNode.node(PATH_POWER_BLOCK_TYPES).getList(String.class), silent);
    }

    private Set<Material> getMaterialBlackList(ConfigurationNode sectionNode, boolean silent)
        throws SerializationException
    {
        return MATERIAL_BLACKLIST_PARSER.parse(sectionNode.node(PATH_MATERIAL_BLACKLIST).getList(String.class), silent);
    }

    private boolean getResourcePackEnabled(ConfigurationNode sectionNode)
    {
        return sectionNode.node(PATH_RESOURCE_PACK_ENABLED).getBoolean(DEFAULT_RESOURCE_PACK_ENABLED);
    }

    private List<String> getCommandAliases(ConfigurationNode sectionNode)
        throws SerializationException
    {
        return sectionNode.node(PATH_COMMAND_ALIASES).getList(String.class, List.of(DEFAULT_COMMAND_ALIASES));
    }

    /**
     * Represents the result of the General section configuration.
     *
     * @param allowRedstone
     *     Whether structures should respond to redstone signals.
     * @param powerBlockTypes
     *     The types of blocks that can be used as power blocks for structures.
     * @param materialBlacklist
     *     The list of materials that are blacklisted and cannot be animated.
     * @param resourcePackEnabled
     *     Whether the resource pack should be enabled.
     * @param commandAliases
     *     The list of command aliases for the plugin commands.
     */
    public record Result(
        boolean allowRedstone,
        Set<Material> powerBlockTypes,
        Set<Material> materialBlacklist,
        boolean resourcePackEnabled,
        List<String> commandAliases
    ) implements IConfigSectionResult
    {
        /**
         * The default result used when no data is available.
         */
        public static final Result DEFAULT = new Result(
            GeneralSection.DEFAULT_ALLOW_REDSTONE,
            Set.of(DEFAULT_POWER_BLOCK_MATERIAL),
            Set.of(),
            DEFAULT_RESOURCE_PACK_ENABLED,
            List.of(DEFAULT_COMMAND_ALIASES)
        );
    }
}
