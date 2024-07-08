package nl.pim16aap2.animatedarchitecture.spigot.util.api;

import lombok.extern.flogger.Flogger;
import nl.pim16aap2.animatedarchitecture.core.api.IAnimatedArchitecturePlatform;
import nl.pim16aap2.animatedarchitecture.core.api.IBlockAnalyzer;
import nl.pim16aap2.animatedarchitecture.core.api.ILocation;
import nl.pim16aap2.animatedarchitecture.core.api.restartable.IRestartable;
import nl.pim16aap2.animatedarchitecture.core.api.restartable.RestartableHolder;
import nl.pim16aap2.animatedarchitecture.spigot.util.SpigotAdapter;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Specialization of the block analyzer for the Spigot platform.
 * <p>
 * An instance of this class can be obtained through {@link IAnimatedArchitecturePlatform#getBlockAnalyzer()}.
 */
@Flogger
public abstract class BlockAnalyzerSpigot implements IBlockAnalyzer<Material>, IRestartable
{
    /**
     * The configuration for the block analyzer.
     * <p>
     * This is used to obtain the blacklist with materials that should not be animated.
     */
    private final IBlockAnalyzerConfig config;

    /**
     * The set of materials that are whitelisted.
     * <p>
     * Materials in this set will be allowed to be animated.
     */
    private volatile Set<Material> whitelist = EnumSet.noneOf(Material.class);

    protected BlockAnalyzerSpigot(
        IBlockAnalyzerConfig config,
        RestartableHolder restartableHolder)
    {
        this.config = config;

        restartableHolder.registerRestartable(this);
    }

    /**
     * Updates {@link #whitelist} based on the current configuration.
     */
    private void updateWhitelist()
    {
        final Set<Material> blacklist = config.getMaterialBlacklist();

        whitelist = Stream
            .of(Material.values())
            .filter(mat -> !blacklist.contains(mat))
            .filter(mat -> getDefaultMaterialStatus(mat) == MaterialStatus.WHITELISTED)
            .collect(Collectors.toCollection(HashSet::new));
    }

    /**
     * Safely creates a {@link BlockData} object for a material.
     * <p>
     * If an error occurs while creating the {@link BlockData} object, this method will log the error and return
     * {@code null}.
     * <p>
     * See {@link Material#createBlockData()}.
     *
     * @param material
     *     The material to create the {@link BlockData} object for.
     * @return The created {@link BlockData} object or {@code null} if an error occurred.
     */
    protected @Nullable BlockData safeCreateBlockData(Material material)
    {
        try
        {
            return material.createBlockData();
        }
        catch (Exception e)
        {
            log.atInfo().log(
                "Encountered error parsing material '%s': %s - %s",
                material.name(),
                e.getClass().getSimpleName(),
                e.getMessage()
            );
        }
        return null;
    }

    /**
     * Gets the default material status for a material.
     * <p>
     * The default material status can be overridden by the blacklist provided by the configuration.
     *
     * @param material
     *     The material to get the default material status for.
     * @return The default material status for the material.
     */
    protected abstract MaterialStatus getDefaultMaterialStatus(Material material);

    @Override
    public boolean isAirOrLiquid(Material material)
    {
        return material.isAir() || material.equals(Material.WATER) || material.equals(Material.LAVA);
    }

    @Override
    public boolean isAirOrLiquid(ILocation location)
    {
        return isAirOrLiquid(materialAtLocation(location));
    }

    @Override
    public boolean isAllowed(Material material)
    {
        return whitelist.contains(material);
    }

    private static Material materialAtLocation(ILocation location)
    {
        return SpigotAdapter.getBukkitLocation(location).getBlock().getType();
    }

    @Override
    public final void initialize()
    {
        updateWhitelist();
    }
}
