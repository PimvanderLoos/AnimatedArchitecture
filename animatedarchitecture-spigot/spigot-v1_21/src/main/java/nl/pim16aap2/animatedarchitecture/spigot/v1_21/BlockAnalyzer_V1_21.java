package nl.pim16aap2.animatedarchitecture.spigot.v1_21;

import lombok.extern.flogger.Flogger;
import nl.pim16aap2.animatedarchitecture.core.api.ILocation;
import nl.pim16aap2.animatedarchitecture.spigot.util.SpigotAdapter;
import nl.pim16aap2.animatedarchitecture.spigot.util.api.IBlockAnalyzerSpigot;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.data.Levelled;
import org.bukkit.block.data.type.CommandBlock;
import org.bukkit.inventory.BlockInventoryHolder;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Represents a class that can perform analysis on blocks on the Spigot 1.21 platform.
 * <p>
 * See {@link IBlockAnalyzerSpigot} for more information.
 */
@Flogger
public final class BlockAnalyzer_V1_21 implements IBlockAnalyzerSpigot
{
    private static final List<Tag<Material>> BLOCKED_TAGS = List.of(
        Tag.AIR,
        Tag.ALL_SIGNS,
        Tag.BANNERS,
        Tag.BEDS,
        Tag.BEEHIVES,
        Tag.SHULKER_BOXES
    );

    private static final Set<Material> WHITELIST = EnumSet.noneOf(Material.class);

    static
    {
        for (final Material mat : Material.values())
        {
            final MaterialStatus result = getMaterialStatus(mat);
            if (result == MaterialStatus.WHITELISTED)
                WHITELIST.add(mat);
        }
    }

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
        return WHITELIST.contains(material);
    }

    private static Material materialAtLocation(ILocation location)
    {
        return SpigotAdapter.getBukkitLocation(location).getBlock().getType();
    }

    /**
     * Checks if a material is white-, grey-, or blacklisted.
     *
     * @param mat
     *     The material.
     * @return The listing status of the material.
     */
    private static MaterialStatus getMaterialStatus(Material mat)
    {
        for (final Tag<Material> tag : BLOCKED_TAGS)
            if (tag.isTagged(mat))
                return MaterialStatus.BLACKLISTED;

        if (mat.isBlock())
        {
            try
            {
                final var blockData = mat.createBlockData();
                if (blockData instanceof Levelled || blockData instanceof CommandBlock)
                    return MaterialStatus.BLACKLISTED;

                //noinspection UnstableApiUsage
                if (blockData.createBlockState() instanceof BlockInventoryHolder)
                    return MaterialStatus.BLACKLISTED;
            }
            catch (Exception e)
            {
                log.atInfo().log(
                    "Encountered error parsing material '%s': %s - %s",
                    mat.name(),
                    e.getClass().getSimpleName(),
                    e.getMessage()
                );
            }
        }

        return switch (mat)
        {
            case COMMAND_BLOCK,
                 FROGSPAWN,
                 PISTON_HEAD,
                 REDSTONE,
                 SPAWNER,
                 STRUCTURE_BLOCK,
                 STRUCTURE_VOID,
                 TRIAL_SPAWNER,
                 VAULT -> MaterialStatus.BLACKLISTED;
            default -> MaterialStatus.WHITELISTED;
        };
    }
}
