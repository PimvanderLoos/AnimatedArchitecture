package nl.pim16aap2.animatedarchitecture.spigot.core.config;

import dagger.Lazy;
import lombok.AllArgsConstructor;
import nl.pim16aap2.animatedarchitecture.core.config.ProtectionHooksSection;
import nl.pim16aap2.animatedarchitecture.spigot.core.hooks.ProtectionHookManagerSpigot;
import nl.pim16aap2.animatedarchitecture.spigot.util.hooks.IProtectionHookSpigotSpecification;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;

import java.util.Map;

/**
 * Represents a section in the configuration for protection hooks specific to the Spigot implementation.
 */
@AllArgsConstructor
public class ProtectionHooksSectionSpigot extends ProtectionHooksSection
{
    public static final String PATH_PROTECTION_HOOK_ENABLED = "enabled";

    private final Lazy<ProtectionHookManagerSpigot> protectionHookManager;

    private void writeProtectionHookToNode(
        CommentedConfigurationNode hooksNode,
        String hookName
    )
        throws SerializationException
    {
        final var hookNode = hooksNode.node(hookName, PATH_PROTECTION_HOOK_ENABLED);

        // If the node already exists, we should not overwrite it.
        if (!hookNode.virtual())
            return;

        hookNode.set(true);
    }

    private void writeProtectionHooksToNode(
        CommentedConfigurationNode hooksNode,
        Map<String, IProtectionHookSpigotSpecification> hooks)
    {
        hooks.keySet()
            .stream()
            .sorted()
            .forEach(hookName ->
            {
                try
                {
                    writeProtectionHookToNode(hooksNode, hookName);
                }
                catch (SerializationException exception)
                {
                    throw new RuntimeException(
                        String.format("Failed to write protection hook '%s' to configuration node.", hookName),
                        exception
                    );
                }
            });
    }

    @Override
    public void populateDynamicData(CommentedConfigurationNode root)
    {
        getSection(root).act(node ->
            writeProtectionHooksToNode(node, protectionHookManager.get().getRegisteredHookDefinitions()));
    }
}
