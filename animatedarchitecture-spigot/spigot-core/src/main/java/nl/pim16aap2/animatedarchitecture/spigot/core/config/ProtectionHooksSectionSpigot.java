package nl.pim16aap2.animatedarchitecture.spigot.core.config;

import dagger.Lazy;
import lombok.AllArgsConstructor;
import lombok.CustomLog;
import lombok.Getter;
import lombok.NoArgsConstructor;
import nl.pim16aap2.animatedarchitecture.core.config.IConfigSectionResult;
import nl.pim16aap2.animatedarchitecture.core.config.ProtectionHooksSection;
import nl.pim16aap2.animatedarchitecture.spigot.core.hooks.ProtectionHookManagerSpigot;
import nl.pim16aap2.animatedarchitecture.spigot.util.hooks.IProtectionHookSpigotSpecification;
import org.jspecify.annotations.Nullable;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Represents a section in the configuration for protection hooks specific to the Spigot implementation.
 */
@CustomLog
@AllArgsConstructor
@NoArgsConstructor(force = true)
public class ProtectionHooksSectionSpigot extends ProtectionHooksSection<ProtectionHooksSectionSpigot.Result>
{
    public static final String PATH_PROTECTION_HOOK_ENABLED = "enabled";

    private final Lazy<ProtectionHookManagerSpigot> protectionHookManager;

    @Getter
    private final @Nullable Consumer<Result> resultConsumer;

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

    @Override
    protected Result getResult(ConfigurationNode sectionNode, boolean silent)
    {
        return new Result(
            Collections.unmodifiableSet(getEnabledProtectionHooks(sectionNode, silent))
        );
    }

    private Set<IProtectionHookSpigotSpecification> getEnabledProtectionHooks(
        ConfigurationNode sectionNode,
        boolean silent
    )
    {
        final Map<String, IProtectionHookSpigotSpecification> registeredHooks =
            protectionHookManager.get().getRegisteredHookDefinitions();

        final Set<IProtectionHookSpigotSpecification> enabledHooks = new HashSet<>();

        sectionNode.childrenMap()
            .forEach((hookName, hookNode) ->
            {
                if (!hookNode.node(PATH_PROTECTION_HOOK_ENABLED).getBoolean(false))
                    return;
                final var hook = registeredHooks.get((String) hookName);
                if (hook == null)
                {
                    if (!silent)
                    {
                        log.atWarn().log(
                            "Protection hook '%s' is enabled in the configuration, but it is not registered!",
                            hookName
                        );
                    }
                    return;
                }
                enabledHooks.add(hook);
            });

        return enabledHooks;
    }

    /**
     * Represents the result of the ProtectionHooksSectionSpigot configuration.
     *
     * @param enabledHooks
     *     Set of enabled protection hooks.
     */
    public record Result(
        Set<IProtectionHookSpigotSpecification> enabledHooks
    ) implements IConfigSectionResult
    {
        /**
         * The default result used when no data is available.
         */
        public static final Result DEFAULT = new Result(Set.of());

        public Result
        {
            enabledHooks = Set.copyOf(enabledHooks);
        }

        @SuppressWarnings("unused") // Used by Lombok's @Delegate
        public boolean isProtectionHookEnabled(@Nullable IProtectionHookSpigotSpecification spec)
        {
            return enabledHooks().contains(spec);
        }
    }
}
