package nl.pim16aap2.animatedarchitecture.spigot.util.hooks;

import org.jetbrains.annotations.Nullable;

/**
 * Represents the specification of a {@link IProtectionHookSpigot}.
 */
public interface IProtectionHookSpigotSpecification
{
    /**
     * Get the class of the given hook for a specific version of the plugin to load the compat for.
     *
     * @param version
     *     The version of the plugin to load the hook for.
     * @return The {@link IProtectionHookSpigot} class of the compat.
     */
    @Nullable
    Class<? extends IProtectionHookSpigot> getClass(String version);

    /**
     * Get the name of the plugin the given compat hooks into.
     *
     * @return The name of the plugin the given compat hooks into.
     */
    String getName();
}
