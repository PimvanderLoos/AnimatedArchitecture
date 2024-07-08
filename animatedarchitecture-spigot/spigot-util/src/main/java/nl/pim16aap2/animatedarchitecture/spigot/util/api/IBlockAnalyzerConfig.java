package nl.pim16aap2.animatedarchitecture.spigot.util.api;

import org.bukkit.Material;

import java.util.Set;

/**
 * Represents a configuration for the block analyzer.
 */
public interface IBlockAnalyzerConfig
{
    /**
     * Gets the set of materials that are blacklisted.
     * <p>
     * Materials in this set will not be allowed to be animated regardless of any other settings.
     *
     * @return The set of materials that are blacklisted.
     */
    Set<Material> getMaterialBlacklist();
}
