package nl.pim16aap2.animatedarchitecture.spigot.util.api;

import nl.pim16aap2.animatedarchitecture.core.api.IAnimatedArchitecturePlatform;
import nl.pim16aap2.animatedarchitecture.core.api.IBlockAnalyzer;
import org.bukkit.Material;

/**
 * Specialization of the block analyzer for the Spigot platform.
 * <p>
 * An instance of this class can be obtained through {@link IAnimatedArchitecturePlatform#getBlockAnalyzer()}.
 */
public interface IBlockAnalyzerSpigot extends IBlockAnalyzer<Material>
{
}
