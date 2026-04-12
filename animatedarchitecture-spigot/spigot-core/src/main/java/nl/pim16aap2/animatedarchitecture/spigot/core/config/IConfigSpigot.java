package nl.pim16aap2.animatedarchitecture.spigot.core.config;


import nl.pim16aap2.animatedarchitecture.core.config.IConfig;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureType;
import nl.pim16aap2.animatedarchitecture.spigot.util.api.IBlockAnalyzerConfig;
import nl.pim16aap2.animatedarchitecture.spigot.util.hooks.IProtectionHookSpigotSpecification;
import org.bukkit.Material;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Set;

/**
 * Represents the configuration of the plugin for the Spigot platform.
 */
public interface IConfigSpigot extends IConfig, IBlockAnalyzerConfig
{
    /**
     * Gets the configured set of powerblock types.
     *
     * @return A set of powerblock types.
     */
    Set<Material> powerblockTypes();

    /**
     * Checks if the resource pack is enabled.
     *
     * @return true if the resource pack is enabled, false otherwise.
     */
    boolean resourcePackEnabled();

    /**
     * Gets the material used for the GUI of a specific structure type.
     *
     * @param type
     *     The structure type for which to get the GUI material.
     * @return The material used for the GUI of the specified structure type.
     */
    Material guiMaterial(StructureType type);

    /**
     * Gets the list of command aliases.
     * <p>
     * The first alias in the list is considered the primary command.
     *
     * @return A list of command aliases.
     */
    List<String> commandAliases();

    /**
     * Gets the timeout for the head cache.
     *
     * @return The timeout in seconds for the head cache in seconds.
     */
    int headCacheTimeout();

    /**
     * Checks if a specific protection hook is enabled.
     *
     * @param spec
     *     The specification of the protection hook to check.
     * @return true if the hook is enabled, false otherwise.
     */
    boolean isProtectionHookEnabled(@Nullable IProtectionHookSpigotSpecification spec);
}
