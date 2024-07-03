package nl.pim16aap2.animatedarchitecture.spigot.v1_19_R3;

import lombok.extern.flogger.Flogger;
import nl.pim16aap2.animatedarchitecture.core.api.ILocation;
import nl.pim16aap2.animatedarchitecture.spigot.util.SpigotAdapter;
import nl.pim16aap2.animatedarchitecture.spigot.util.api.IBlockAnalyzerSpigot;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.data.Levelled;
import org.bukkit.block.data.type.CommandBlock;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Represents a class that can perform analysis on blocks on the Spigot 1.19 platform.
 * <p>
 * See {@link IBlockAnalyzerSpigot} for more information.
 */
@Flogger
final class BlockAnalyzer_V1_19 implements IBlockAnalyzerSpigot
{
    private static final List<Tag<Material>> BLOCKED_TAGS = List.of(
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

        if (mat.isAir())
            return MaterialStatus.BLACKLISTED;

        if (mat.isBlock())
        {
            try
            {
                final var blockData = mat.createBlockData();
                if (blockData instanceof Levelled || blockData instanceof CommandBlock)
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
            //noinspection UnstableApiUsage
            case BARREL,
                 BLAST_FURNACE,
                 BREWING_STAND,
                 CHEST,
                 CHISELED_BOOKSHELF,
                 DECORATED_POT,
                 COMMAND_BLOCK,
                 CREEPER_HEAD,
                 CREEPER_WALL_HEAD,
                 DISPENSER,
                 DRAGON_EGG,
                 DRAGON_HEAD,
                 DRAGON_WALL_HEAD,
                 DROPPER,
                 FROGSPAWN,
                 FURNACE,
                 HOPPER,
                 JUKEBOX,
                 LECTERN,
                 PIGLIN_HEAD,
                 PIGLIN_WALL_HEAD,
                 PISTON_HEAD,
                 PLAYER_HEAD,
                 PLAYER_WALL_HEAD,
                 REDSTONE,
                 SHULKER_BOX,
                 SKELETON_SKULL,
                 SKELETON_WALL_SKULL,
                 SMOKER,
                 SPAWNER,
                 STONECUTTER,
                 STRUCTURE_BLOCK,
                 STRUCTURE_VOID,
                 TRAPPED_CHEST,
                 WITHER_SKELETON_SKULL,
                 WITHER_SKELETON_WALL_SKULL,
                 ZOMBIE_HEAD,
                 ZOMBIE_WALL_HEAD -> MaterialStatus.BLACKLISTED;
            default -> MaterialStatus.WHITELISTED;
        };
    }
}
