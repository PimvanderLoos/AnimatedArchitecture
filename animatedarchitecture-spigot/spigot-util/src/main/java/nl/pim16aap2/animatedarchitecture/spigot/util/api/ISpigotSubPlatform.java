package nl.pim16aap2.animatedarchitecture.spigot.util.api;

/**
 * Represents a sub-platform for the Spigot platform.
 * <p>
 * Each sub-platform is a separate module for a specific server version.
 */
public interface ISpigotSubPlatform
{
    /**
     * Gets the block analyzer for this sub-platform.
     *
     * @return The block analyzer for this sub-platform.
     */
    IBlockAnalyzerSpigot getBlockAnalyzer();
}
