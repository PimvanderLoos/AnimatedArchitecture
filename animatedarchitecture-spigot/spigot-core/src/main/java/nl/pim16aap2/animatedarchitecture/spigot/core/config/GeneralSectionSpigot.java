package nl.pim16aap2.animatedarchitecture.spigot.core.config;

import nl.pim16aap2.animatedarchitecture.core.config.GeneralSection;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;

public class GeneralSectionSpigot extends GeneralSection
{
    public static final String PATH_POWER_BLOCK_TYPES = "power_block_types";
    public static final String PATH_MATERIAL_BLACKLIST = "material_blacklist";
    public static final String PATH_RESOURCE_PACK_ENABLED = "resource_pack_enabled";
    public static final String PATH_COMMAND_ALIASES = "command_aliases";

    public static final String[] DEFAULT_POWER_BLOCK_TYPES = new String[]{"GOLD_BLOCK"};
    public static final String[] DEFAULT_MATERIAL_BLACKLIST = new String[0];
    public static final boolean DEFAULT_RESOURCE_PACK_ENABLED = true;
    public static final String[] DEFAULT_COMMAND_ALIASES = new String[]{
        "animatedarchitecture",
        "AnimatedArchitecture",
        "aa"
    };

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
                For example, you would blacklist bedrock by adding the following line:
                  - BEDROCK
                
                A list of options can be found here: https://hub.spigotmc.org/javadocs/spigot/org/bukkit/Material.html
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
}
