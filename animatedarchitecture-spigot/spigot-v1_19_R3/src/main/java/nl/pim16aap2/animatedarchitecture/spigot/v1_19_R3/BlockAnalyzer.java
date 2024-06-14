package nl.pim16aap2.animatedarchitecture.spigot.v1_19_R3;

import nl.pim16aap2.animatedarchitecture.core.api.ILocation;
import nl.pim16aap2.animatedarchitecture.spigot.util.SpigotAdapter;
import nl.pim16aap2.animatedarchitecture.spigot.util.api.IBlockAnalyzerSpigot;
import org.bukkit.Bukkit;
import org.bukkit.Material;

import java.util.EnumSet;
import java.util.Set;

/**
 * Represents a class that can perform analysis on blocks on the Spigot 1.19 platform.
 * <p>
 * See {@link IBlockAnalyzerSpigot} for more information.
 */
public final class BlockAnalyzer implements IBlockAnalyzerSpigot
{
    private static final Set<Material> WHITELIST = EnumSet.noneOf(Material.class);

    static
    {
        for (final Material mat : Material.values())
        {
            final MaterialStatus result = getMaterialStatus(mat);
            if (result == MaterialStatus.WHITELISTED)
                WHITELIST.add(mat);
            else if (result == MaterialStatus.UNMAPPED)
                Bukkit.getLogger().warning("Material \"" + mat.name() + "\" is not mapped! Please contact pim16aap2!");
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
        return switch (mat)
        {
            //noinspection UnstableApiUsage
            case ACACIA_DOOR,
                 ACACIA_HANGING_SIGN,
                 ACACIA_SIGN,
                 ACACIA_TRAPDOOR,
                 ACACIA_WALL_HANGING_SIGN,
                 ACACIA_WALL_SIGN,
                 AIR,
                 ARMOR_STAND,
                 BAMBOO_DOOR,
                 BAMBOO_HANGING_SIGN,
                 BAMBOO_SIGN,
                 BAMBOO_TRAPDOOR,
                 BAMBOO_WALL_HANGING_SIGN,
                 BAMBOO_WALL_SIGN,
                 BARREL,
                 BEEHIVE,
                 BEE_NEST,
                 BIRCH_DOOR,
                 BIRCH_HANGING_SIGN,
                 BIRCH_SIGN,
                 BIRCH_TRAPDOOR,
                 BIRCH_WALL_HANGING_SIGN,
                 BIRCH_WALL_SIGN,
                 BLACK_BANNER,
                 BLACK_BED,
                 BLACK_SHULKER_BOX,
                 BLACK_WALL_BANNER,
                 BLAST_FURNACE,
                 BLUE_BANNER,
                 BLUE_BED,
                 BLUE_SHULKER_BOX,
                 BLUE_WALL_BANNER,
                 BREWING_STAND,
                 BROWN_BANNER,
                 BROWN_BED,
                 BROWN_SHULKER_BOX,
                 BROWN_WALL_BANNER,
                 CARTOGRAPHY_TABLE,
                 CAULDRON,
                 CAVE_AIR,
                 CHEST,
                 COMMAND_BLOCK,
                 COMMAND_BLOCK_MINECART,
                 COMPOSTER,
                 CREEPER_HEAD,
                 CREEPER_WALL_HEAD,
                 CRIMSON_DOOR,
                 CRIMSON_HANGING_SIGN,
                 CRIMSON_SIGN,
                 CRIMSON_TRAPDOOR,
                 CRIMSON_WALL_HANGING_SIGN,
                 CRIMSON_WALL_SIGN,
                 CYAN_BANNER,
                 CYAN_BED,
                 CYAN_SHULKER_BOX,
                 CYAN_WALL_BANNER,
                 DARK_OAK_DOOR,
                 DARK_OAK_HANGING_SIGN,
                 DARK_OAK_SIGN,
                 DARK_OAK_TRAPDOOR,
                 DARK_OAK_WALL_HANGING_SIGN,
                 DARK_OAK_WALL_SIGN,
                 DRAGON_EGG,
                 DRAGON_HEAD,
                 DRAGON_WALL_HEAD,
                 DROPPER,
                 ENDER_CHEST,
                 FLETCHING_TABLE,
                 FROGSPAWN,
                 FURNACE,
                 FURNACE_MINECART,
                 GLOW_ITEM_FRAME,
                 GRAY_BANNER,
                 GRAY_BED,
                 GRAY_SHULKER_BOX,
                 GRAY_WALL_BANNER,
                 GREEN_BANNER,
                 GREEN_BED,
                 GREEN_SHULKER_BOX,
                 GREEN_WALL_BANNER,
                 GRINDSTONE,
                 HOPPER,
                 IRON_DOOR,
                 IRON_TRAPDOOR,
                 ITEM_FRAME,
                 JIGSAW,
                 JUKEBOX,
                 JUNGLE_DOOR,
                 JUNGLE_HANGING_SIGN,
                 JUNGLE_SIGN,
                 JUNGLE_TRAPDOOR,
                 JUNGLE_WALL_HANGING_SIGN,
                 JUNGLE_WALL_SIGN,
                 LAVA,
                 LECTERN,
                 LIGHT_BLUE_BANNER,
                 LIGHT_BLUE_BED,
                 LIGHT_BLUE_SHULKER_BOX,
                 LIGHT_BLUE_WALL_BANNER,
                 LIGHT_GRAY_BANNER,
                 LIGHT_GRAY_BED,
                 LIGHT_GRAY_SHULKER_BOX,
                 LIGHT_GRAY_WALL_BANNER,
                 LIME_BANNER,
                 LIME_BED,
                 LIME_SHULKER_BOX,
                 LIME_WALL_BANNER,
                 LOOM,
                 MAGENTA_BANNER,
                 MAGENTA_BED,
                 MAGENTA_SHULKER_BOX,
                 MAGENTA_WALL_BANNER,
                 MANGROVE_DOOR,
                 MANGROVE_HANGING_SIGN,
                 MANGROVE_SIGN,
                 MANGROVE_TRAPDOOR,
                 MANGROVE_WALL_HANGING_SIGN,
                 MANGROVE_WALL_SIGN,
                 OAK_DOOR,
                 OAK_HANGING_SIGN,
                 OAK_SIGN,
                 OAK_TRAPDOOR,
                 OAK_WALL_HANGING_SIGN,
                 OAK_WALL_SIGN,
                 ORANGE_BANNER,
                 ORANGE_BED,
                 ORANGE_SHULKER_BOX,
                 ORANGE_WALL_BANNER,
                 PAINTING,
                 PIGLIN_HEAD,
                 PIGLIN_WALL_HEAD,
                 PINK_BANNER,
                 PINK_BED,
                 PINK_SHULKER_BOX,
                 PINK_WALL_BANNER,
                 PISTON_HEAD,
                 PLAYER_HEAD,
                 PLAYER_WALL_HEAD,
                 PURPLE_BANNER,
                 PURPLE_BED,
                 PURPLE_SHULKER_BOX,
                 PURPLE_WALL_BANNER,
                 REDSTONE,
                 RED_BANNER,
                 RED_BED,
                 RED_SHULKER_BOX,
                 RED_WALL_BANNER,
                 SHULKER_BOX,
                 SKELETON_SKULL,
                 SKELETON_WALL_SKULL,
                 SMITHING_TABLE,
                 SPAWNER,
                 SPRUCE_DOOR,
                 SPRUCE_HANGING_SIGN,
                 SPRUCE_SIGN,
                 SPRUCE_TRAPDOOR,
                 SPRUCE_WALL_HANGING_SIGN,
                 SPRUCE_WALL_SIGN,
                 STONECUTTER,
                 STRUCTURE_BLOCK,
                 STRUCTURE_VOID,
                 TRAPPED_CHEST,
                 VOID_AIR,
                 WARPED_DOOR,
                 WARPED_HANGING_SIGN,
                 WARPED_SIGN,
                 WARPED_TRAPDOOR,
                 WARPED_WALL_HANGING_SIGN,
                 WARPED_WALL_SIGN,
                 WATER,
                 WHITE_BANNER,
                 WHITE_BED,
                 WHITE_SHULKER_BOX,
                 WHITE_WALL_BANNER,
                 WITHER_SKELETON_SKULL,
                 WITHER_SKELETON_WALL_SKULL,
                 YELLOW_BANNER,
                 YELLOW_BED,
                 YELLOW_SHULKER_BOX,
                 YELLOW_WALL_BANNER,
                 ZOMBIE_HEAD,
                 ZOMBIE_WALL_HEAD -> MaterialStatus.BLACKLISTED;
            default -> MaterialStatus.WHITELISTED;
        };
    }
}
