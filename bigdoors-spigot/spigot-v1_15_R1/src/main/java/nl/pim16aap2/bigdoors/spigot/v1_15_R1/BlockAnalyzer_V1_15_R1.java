package nl.pim16aap2.bigdoors.spigot.v1_15_R1;

import nl.pim16aap2.bigdoors.api.IBlockAnalyzer;
import nl.pim16aap2.bigdoors.api.IPLocation;
import nl.pim16aap2.bigdoors.spigot.util.SpigotAdapter;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;

import javax.inject.Singleton;
import java.util.EnumSet;
import java.util.Set;

/**
 * Represents a class that can perform basic analysis on blocks, such as if they're empty, blacklisted, and/or
 * rotatable.
 *
 * @author Pim
 */
@Singleton
public final class BlockAnalyzer_V1_15_R1 implements IBlockAnalyzer
{
    private static final Set<Material> WHITELIST = EnumSet.noneOf(Material.class);
    private static final Set<Material> GREYLIST = EnumSet.noneOf(Material.class);
    private static final Set<Material> BLACKLIST = EnumSet.noneOf(Material.class);

    static
    {
        for (final Material mat : Material.values())
        {
            final MaterialStatus result = getMaterialStatus(mat);
            if (result == MaterialStatus.WHITELISTED)
                WHITELIST.add(mat);
            else if (result == MaterialStatus.BLACKLISTED)
                BLACKLIST.add(mat);
            else if (result == MaterialStatus.GREYLISTED)
                GREYLIST.add(mat);
            else if (result == MaterialStatus.UNMAPPED)
            {
                Bukkit.getLogger().warning("Material \"" + mat.name() + "\" is not mapped! Please contact pim16aap2!");
                BLACKLIST.add(mat);
            }
        }
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
        if (!mat.isBlock())
            return MaterialStatus.BLACKLISTED;

        switch (mat)
        {
            //<editor-fold defaultstate="collapsed" desc="isWhitelisted">
            case ACACIA_FENCE:
            case ACACIA_FENCE_GATE:
            case ACACIA_LEAVES:
            case ACACIA_LOG:
            case ACACIA_PLANKS:
            case ACACIA_SLAB:
            case ACACIA_STAIRS:
            case ACACIA_WOOD:
            case ANDESITE:
            case ANDESITE_SLAB:
            case ANDESITE_STAIRS:
            case ANDESITE_WALL:
            case BARRIER:
            case BEACON:
            case BEDROCK:
            case BIRCH_FENCE:
            case BIRCH_FENCE_GATE:
            case BIRCH_LEAVES:
            case BIRCH_LOG:
            case BIRCH_PLANKS:
            case BIRCH_SLAB:
            case BIRCH_STAIRS:
            case BIRCH_WOOD:
            case BLACK_CONCRETE:
            case BLACK_GLAZED_TERRACOTTA:
            case BLACK_SHULKER_BOX:
            case BLACK_STAINED_GLASS:
            case BLACK_STAINED_GLASS_PANE:
            case BLACK_TERRACOTTA:
            case BLACK_WOOL:
            case BLUE_CONCRETE:
            case BLUE_GLAZED_TERRACOTTA:
            case BLUE_ICE:
            case BLUE_SHULKER_BOX:
            case BLUE_STAINED_GLASS:
            case BLUE_STAINED_GLASS_PANE:
            case BLUE_TERRACOTTA:
            case BLUE_WOOL:
            case BONE_BLOCK:
            case BOOKSHELF:
            case BRICK_SLAB:
            case BRICK_STAIRS:
            case BRICK_WALL:
            case BRICKS:
            case BROWN_CONCRETE:
            case BROWN_GLAZED_TERRACOTTA:
            case BROWN_MUSHROOM_BLOCK:
            case BROWN_SHULKER_BOX:
            case BROWN_STAINED_GLASS:
            case BROWN_STAINED_GLASS_PANE:
            case BROWN_TERRACOTTA:
            case BROWN_WOOL:
            case CAKE:
            case CAMPFIRE:
            case CARVED_PUMPKIN:
            case CHISELED_QUARTZ_BLOCK:
            case CHISELED_RED_SANDSTONE:
            case CHISELED_SANDSTONE:
            case CHISELED_STONE_BRICKS:
            case CLAY:
            case COAL_BLOCK:
            case COAL_ORE:
            case COARSE_DIRT:
            case COBBLESTONE:
            case COBBLESTONE_SLAB:
            case COBBLESTONE_STAIRS:
            case COBBLESTONE_WALL:
            case COBWEB:
            case CRACKED_STONE_BRICKS:
            case CRAFTING_TABLE:
            case CUT_RED_SANDSTONE:
            case CUT_RED_SANDSTONE_SLAB:
            case CUT_SANDSTONE:
            case CUT_SANDSTONE_SLAB:
            case CYAN_CONCRETE:
            case CYAN_GLAZED_TERRACOTTA:
            case CYAN_SHULKER_BOX:
            case CYAN_STAINED_GLASS:
            case CYAN_STAINED_GLASS_PANE:
            case CYAN_TERRACOTTA:
            case CYAN_WOOL:
            case DARK_OAK_FENCE:
            case DARK_OAK_FENCE_GATE:
            case DARK_OAK_LEAVES:
            case DARK_OAK_LOG:
            case DARK_OAK_PLANKS:
            case DARK_OAK_SLAB:
            case DARK_OAK_STAIRS:
            case DARK_OAK_WOOD:
            case DARK_PRISMARINE:
            case DARK_PRISMARINE_SLAB:
            case DARK_PRISMARINE_STAIRS:
            case DAYLIGHT_DETECTOR:
            case DIAMOND_BLOCK:
            case DIAMOND_ORE:
            case DIORITE:
            case DIORITE_SLAB:
            case DIORITE_STAIRS:
            case DIORITE_WALL:
            case DIRT:
            case DRIED_KELP_BLOCK:
            case EMERALD_BLOCK:
            case EMERALD_ORE:
            case ENCHANTING_TABLE:
            case END_PORTAL:
            case END_PORTAL_FRAME:
            case END_ROD:
            case END_STONE:
            case END_STONE_BRICK_SLAB:
            case END_STONE_BRICK_STAIRS:
            case END_STONE_BRICK_WALL:
            case END_STONE_BRICKS:
            case ENDER_CHEST:
            case FARMLAND:
            case FLOWER_POT:
            case FROSTED_ICE:
            case GLASS:
            case GLASS_PANE:
            case GLOWSTONE:
            case GOLD_BLOCK:
            case GOLD_ORE:
            case GRANITE:
            case GRANITE_SLAB:
            case GRANITE_STAIRS:
            case GRANITE_WALL:
            case GRASS:
            case GRASS_BLOCK:
            case GRASS_PATH:
            case GRAVEL:
            case GRAY_CONCRETE:
            case GRAY_GLAZED_TERRACOTTA:
            case GRAY_SHULKER_BOX:
            case GRAY_STAINED_GLASS:
            case GRAY_STAINED_GLASS_PANE:
            case GRAY_TERRACOTTA:
            case GRAY_WOOL:
            case GREEN_CONCRETE:
            case GREEN_GLAZED_TERRACOTTA:
            case GREEN_SHULKER_BOX:
            case GREEN_STAINED_GLASS:
            case GREEN_STAINED_GLASS_PANE:
            case GREEN_TERRACOTTA:
            case GREEN_WOOL:
            case HAY_BLOCK:
            case ICE:
            case INFESTED_CHISELED_STONE_BRICKS:
            case INFESTED_COBBLESTONE:
            case INFESTED_CRACKED_STONE_BRICKS:
            case INFESTED_MOSSY_STONE_BRICKS:
            case INFESTED_STONE:
            case INFESTED_STONE_BRICKS:
            case IRON_BARS:
            case IRON_BLOCK:
            case IRON_ORE:
            case JACK_O_LANTERN:
            case JUKEBOX:
            case JUNGLE_FENCE:
            case JUNGLE_FENCE_GATE:
            case JUNGLE_LEAVES:
            case JUNGLE_LOG:
            case JUNGLE_PLANKS:
            case JUNGLE_SLAB:
            case JUNGLE_STAIRS:
            case JUNGLE_WOOD:
            case LAPIS_BLOCK:
            case LAPIS_ORE:
            case LIGHT_BLUE_CONCRETE:
            case LIGHT_BLUE_GLAZED_TERRACOTTA:
            case LIGHT_BLUE_SHULKER_BOX:
            case LIGHT_BLUE_STAINED_GLASS:
            case LIGHT_BLUE_STAINED_GLASS_PANE:
            case LIGHT_BLUE_TERRACOTTA:
            case LIGHT_BLUE_WOOL:
            case LIGHT_GRAY_CONCRETE:
            case LIGHT_GRAY_GLAZED_TERRACOTTA:
            case LIGHT_GRAY_SHULKER_BOX:
            case LIGHT_GRAY_STAINED_GLASS:
            case LIGHT_GRAY_STAINED_GLASS_PANE:
            case LIGHT_GRAY_TERRACOTTA:
            case LIGHT_GRAY_WOOL:
            case LIME_CONCRETE:
            case LIME_GLAZED_TERRACOTTA:
            case LIME_STAINED_GLASS:
            case LIME_STAINED_GLASS_PANE:
            case LIME_TERRACOTTA:
            case LIME_WOOL:
            case MAGENTA_CONCRETE:
            case MAGENTA_GLAZED_TERRACOTTA:
            case MAGENTA_SHULKER_BOX:
            case MAGENTA_STAINED_GLASS:
            case MAGENTA_STAINED_GLASS_PANE:
            case MAGENTA_TERRACOTTA:
            case MAGENTA_WOOL:
            case MAGMA_BLOCK:
            case MELON:
            case MOSSY_COBBLESTONE:
            case MOSSY_COBBLESTONE_SLAB:
            case MOSSY_COBBLESTONE_STAIRS:
            case MOSSY_COBBLESTONE_WALL:
            case MOSSY_STONE_BRICK_SLAB:
            case MOSSY_STONE_BRICK_STAIRS:
            case MOSSY_STONE_BRICK_WALL:
            case MOSSY_STONE_BRICKS:
            case MYCELIUM:
            case NETHER_BRICK_FENCE:
            case NETHER_BRICK_SLAB:
            case NETHER_BRICK_STAIRS:
            case NETHER_BRICK_WALL:
            case NETHER_BRICKS:
            case NETHER_PORTAL:
            case NETHER_QUARTZ_ORE:
            case NETHER_WART_BLOCK:
            case NETHERRACK:
            case NOTE_BLOCK:
            case OAK_FENCE:
            case OAK_FENCE_GATE:
            case OAK_LEAVES:
            case OAK_LOG:
            case OAK_PLANKS:
            case OAK_SLAB:
            case OAK_STAIRS:
            case OAK_WOOD:
            case OBSERVER:
            case OBSIDIAN:
            case ORANGE_CONCRETE:
            case ORANGE_GLAZED_TERRACOTTA:
            case ORANGE_SHULKER_BOX:
            case ORANGE_STAINED_GLASS:
            case ORANGE_STAINED_GLASS_PANE:
            case ORANGE_TERRACOTTA:
            case ORANGE_WOOL:
            case PACKED_ICE:
            case PETRIFIED_OAK_SLAB:
            case PINK_CONCRETE:
            case PINK_GLAZED_TERRACOTTA:
            case PINK_SHULKER_BOX:
            case PINK_STAINED_GLASS:
            case PINK_STAINED_GLASS_PANE:
            case PINK_TERRACOTTA:
            case PINK_WOOL:
            case PISTON:
            case PODZOL:
            case POLISHED_ANDESITE:
            case POLISHED_ANDESITE_SLAB:
            case POLISHED_ANDESITE_STAIRS:
            case POLISHED_DIORITE:
            case POLISHED_DIORITE_SLAB:
            case POLISHED_DIORITE_STAIRS:
            case POLISHED_GRANITE:
            case POLISHED_GRANITE_SLAB:
            case POLISHED_GRANITE_STAIRS:
            case POTTED_ACACIA_SAPLING:
            case POTTED_ALLIUM:
            case POTTED_AZURE_BLUET:
            case POTTED_BAMBOO:
            case POTTED_BIRCH_SAPLING:
            case POTTED_BLUE_ORCHID:
            case POTTED_BROWN_MUSHROOM:
            case POTTED_CACTUS:
            case POTTED_CORNFLOWER:
            case POTTED_DANDELION:
            case POTTED_DARK_OAK_SAPLING:
            case POTTED_DEAD_BUSH:
            case POTTED_FERN:
            case POTTED_JUNGLE_SAPLING:
            case POTTED_LILY_OF_THE_VALLEY:
            case POTTED_OAK_SAPLING:
            case POTTED_ORANGE_TULIP:
            case POTTED_OXEYE_DAISY:
            case POTTED_PINK_TULIP:
            case POTTED_POPPY:
            case POTTED_RED_MUSHROOM:
            case POTTED_RED_TULIP:
            case POTTED_SPRUCE_SAPLING:
            case POTTED_WHITE_TULIP:
            case POTTED_WITHER_ROSE:
            case PRISMARINE:
            case PRISMARINE_BRICK_SLAB:
            case PRISMARINE_BRICK_STAIRS:
            case PRISMARINE_BRICKS:
            case PRISMARINE_SLAB:
            case PRISMARINE_STAIRS:
            case PRISMARINE_WALL:
            case PUMPKIN:
            case PURPLE_CONCRETE:
            case PURPLE_GLAZED_TERRACOTTA:
            case PURPLE_SHULKER_BOX:
            case PURPLE_STAINED_GLASS:
            case PURPLE_STAINED_GLASS_PANE:
            case PURPLE_TERRACOTTA:
            case PURPLE_WOOL:
            case PURPUR_BLOCK:
            case PURPUR_PILLAR:
            case PURPUR_SLAB:
            case PURPUR_STAIRS:
            case QUARTZ_BLOCK:
            case QUARTZ_PILLAR:
            case QUARTZ_SLAB:
            case QUARTZ_STAIRS:
            case RED_CARPET:
            case RED_CONCRETE:
            case RED_GLAZED_TERRACOTTA:
            case RED_MUSHROOM_BLOCK:
            case RED_NETHER_BRICK_SLAB:
            case RED_NETHER_BRICK_STAIRS:
            case RED_NETHER_BRICK_WALL:
            case RED_NETHER_BRICKS:
            case RED_SANDSTONE:
            case RED_SANDSTONE_SLAB:
            case RED_SANDSTONE_STAIRS:
            case RED_SANDSTONE_WALL:
            case RED_SHULKER_BOX:
            case RED_STAINED_GLASS:
            case RED_STAINED_GLASS_PANE:
            case RED_TERRACOTTA:
            case RED_WOOL:
            case REDSTONE_BLOCK:
            case REDSTONE_LAMP:
            case REDSTONE_ORE:
            case SANDSTONE:
            case SANDSTONE_SLAB:
            case SANDSTONE_STAIRS:
            case SANDSTONE_WALL:
            case SLIME_BLOCK:
            case SMOOTH_QUARTZ:
            case SMOOTH_QUARTZ_SLAB:
            case SMOOTH_QUARTZ_STAIRS:
            case SMOOTH_RED_SANDSTONE:
            case SMOOTH_RED_SANDSTONE_SLAB:
            case SMOOTH_RED_SANDSTONE_STAIRS:
            case SMOOTH_SANDSTONE:
            case SMOOTH_SANDSTONE_SLAB:
            case SMOOTH_SANDSTONE_STAIRS:
            case SMOOTH_STONE:
            case SMOOTH_STONE_SLAB:
            case SNOW_BLOCK:
            case SOUL_SAND:
            case SPAWNER:
            case SPONGE:
            case SPRUCE_FENCE:
            case SPRUCE_FENCE_GATE:
            case SPRUCE_LEAVES:
            case SPRUCE_LOG:
            case SPRUCE_PLANKS:
            case SPRUCE_SLAB:
            case SPRUCE_STAIRS:
            case SPRUCE_WOOD:
            case STICKY_PISTON:
            case STONE:
            case STONE_BRICK_SLAB:
            case STONE_BRICK_STAIRS:
            case STONE_BRICK_WALL:
            case STONE_BRICKS:
            case STONE_SLAB:
            case STONE_STAIRS:
            case STRIPPED_ACACIA_LOG:
            case STRIPPED_ACACIA_WOOD:
            case STRIPPED_BIRCH_LOG:
            case STRIPPED_BIRCH_WOOD:
            case STRIPPED_DARK_OAK_LOG:
            case STRIPPED_DARK_OAK_WOOD:
            case STRIPPED_JUNGLE_LOG:
            case STRIPPED_JUNGLE_WOOD:
            case STRIPPED_OAK_LOG:
            case STRIPPED_OAK_WOOD:
            case STRIPPED_SPRUCE_LOG:
            case STRIPPED_SPRUCE_WOOD:
            case TERRACOTTA:
            case TNT:
            case WET_SPONGE:
            case WHITE_CONCRETE:
            case WHITE_GLAZED_TERRACOTTA:
            case WHITE_SHULKER_BOX:
            case WHITE_STAINED_GLASS:
            case WHITE_STAINED_GLASS_PANE:
            case WHITE_TERRACOTTA:
            case WHITE_WOOL:
            case YELLOW_CONCRETE:
            case YELLOW_GLAZED_TERRACOTTA:
            case YELLOW_SHULKER_BOX:
            case YELLOW_STAINED_GLASS:
            case YELLOW_STAINED_GLASS_PANE:
            case YELLOW_TERRACOTTA:
            case YELLOW_WOOL:
                return MaterialStatus.WHITELISTED;


            //<editor-fold defaultstate="collapsed" desc="isGreylisted">
            case ACACIA_BUTTON:
            case ACACIA_DOOR:
            case ACACIA_PRESSURE_PLATE:
            case ACACIA_SAPLING:
            case ACACIA_SIGN:
            case ACACIA_TRAPDOOR:
            case ACACIA_WALL_SIGN:
            case ACTIVATOR_RAIL:
            case ALLIUM:
            case ANVIL:
            case ATTACHED_MELON_STEM:
            case ATTACHED_PUMPKIN_STEM:
            case AZURE_BLUET:
            case BAMBOO:
            case BAMBOO_SAPLING:
            case BEETROOTS:
            case BELL:
            case BIRCH_BUTTON:
            case BIRCH_DOOR:
            case BIRCH_PRESSURE_PLATE:
            case BIRCH_SAPLING:
            case BIRCH_SIGN:
            case BIRCH_TRAPDOOR:
            case BIRCH_WALL_SIGN:
            case BLACK_BANNER:
            case BLACK_BED:
            case BLACK_CARPET:
            case BLACK_CONCRETE_POWDER:
            case BLACK_WALL_BANNER:
            case BLUE_BANNER:
            case BLUE_BED:
            case BLUE_CARPET:
            case BLUE_CONCRETE_POWDER:
            case BLUE_ORCHID:
            case BLUE_WALL_BANNER:
            case BRAIN_CORAL:
            case BRAIN_CORAL_BLOCK:
            case BRAIN_CORAL_FAN:
            case BRAIN_CORAL_WALL_FAN:
            case BROWN_BANNER:
            case BROWN_BED:
            case BROWN_CARPET:
            case BROWN_CONCRETE_POWDER:
            case BROWN_MUSHROOM:
            case BROWN_WALL_BANNER:
            case BUBBLE_COLUMN:
            case BUBBLE_CORAL:
            case BUBBLE_CORAL_BLOCK:
            case BUBBLE_CORAL_FAN:
            case BUBBLE_CORAL_WALL_FAN:
            case CACTUS:
            case CARROTS:
            case CHIPPED_ANVIL:
            case CHORUS_FLOWER:
            case CHORUS_PLANT:
            case COCOA:
            case COMPARATOR:
            case CONDUIT:
            case CORNFLOWER:
            case CREEPER_HEAD:
            case CREEPER_WALL_HEAD:
            case CYAN_BANNER:
            case CYAN_BED:
            case CYAN_CARPET:
            case CYAN_CONCRETE_POWDER:
            case CYAN_WALL_BANNER:
            case DAMAGED_ANVIL:
            case DANDELION:
            case DARK_OAK_BUTTON:
            case DARK_OAK_DOOR:
            case DARK_OAK_PRESSURE_PLATE:
            case DARK_OAK_SAPLING:
            case DARK_OAK_SIGN:
            case DARK_OAK_TRAPDOOR:
            case DARK_OAK_WALL_SIGN:
            case DEAD_BRAIN_CORAL:
            case DEAD_BRAIN_CORAL_BLOCK:
            case DEAD_BRAIN_CORAL_FAN:
            case DEAD_BRAIN_CORAL_WALL_FAN:
            case DEAD_BUBBLE_CORAL:
            case DEAD_BUBBLE_CORAL_BLOCK:
            case DEAD_BUBBLE_CORAL_FAN:
            case DEAD_BUBBLE_CORAL_WALL_FAN:
            case DEAD_BUSH:
            case DEAD_FIRE_CORAL:
            case DEAD_FIRE_CORAL_BLOCK:
            case DEAD_FIRE_CORAL_FAN:
            case DEAD_FIRE_CORAL_WALL_FAN:
            case DEAD_HORN_CORAL:
            case DEAD_HORN_CORAL_BLOCK:
            case DEAD_HORN_CORAL_FAN:
            case DEAD_HORN_CORAL_WALL_FAN:
            case DEAD_TUBE_CORAL:
            case DEAD_TUBE_CORAL_BLOCK:
            case DEAD_TUBE_CORAL_FAN:
            case DEAD_TUBE_CORAL_WALL_FAN:
            case DETECTOR_RAIL:
            case DRAGON_EGG:
            case DRAGON_HEAD:
            case DRAGON_WALL_HEAD:
            case FERN:
            case FIRE_CORAL:
            case FIRE_CORAL_BLOCK:
            case FIRE_CORAL_FAN:
            case FIRE_CORAL_WALL_FAN:
            case GRAY_BANNER:
            case GRAY_BED:
            case GRAY_CARPET:
            case GRAY_CONCRETE_POWDER:
            case GRAY_WALL_BANNER:
            case GREEN_BANNER:
            case GREEN_BED:
            case GREEN_CARPET:
            case GREEN_CONCRETE_POWDER:
            case GREEN_WALL_BANNER:
            case HEAVY_WEIGHTED_PRESSURE_PLATE:
            case HORN_CORAL:
            case HORN_CORAL_BLOCK:
            case HORN_CORAL_FAN:
            case HORN_CORAL_WALL_FAN:
            case IRON_DOOR:
            case IRON_TRAPDOOR:
            case JUNGLE_BUTTON:
            case JUNGLE_DOOR:
            case JUNGLE_PRESSURE_PLATE:
            case JUNGLE_SAPLING:
            case JUNGLE_SIGN:
            case JUNGLE_TRAPDOOR:
            case JUNGLE_WALL_SIGN:
            case KELP:
            case KELP_PLANT:
            case LADDER:
            case LANTERN:
            case LARGE_FERN:
            case LEVER:
            case LIGHT_BLUE_BANNER:
            case LIGHT_BLUE_BED:
            case LIGHT_BLUE_CARPET:
            case LIGHT_BLUE_CONCRETE_POWDER:
            case LIGHT_BLUE_WALL_BANNER:
            case LIGHT_GRAY_BANNER:
            case LIGHT_GRAY_BED:
            case LIGHT_GRAY_CARPET:
            case LIGHT_GRAY_CONCRETE_POWDER:
            case LIGHT_GRAY_WALL_BANNER:
            case LIGHT_WEIGHTED_PRESSURE_PLATE:
            case LILAC:
            case LILY_OF_THE_VALLEY:
            case LILY_PAD:
            case LIME_BANNER:
            case LIME_BED:
            case LIME_CARPET:
            case LIME_CONCRETE_POWDER:
            case LIME_WALL_BANNER:
            case MAGENTA_BANNER:
            case MAGENTA_BED:
            case MAGENTA_CARPET:
            case MAGENTA_CONCRETE_POWDER:
            case MAGENTA_WALL_BANNER:
            case MELON_STEM:
            case MUSHROOM_STEM:
            case NETHER_WART:
            case OAK_BUTTON:
            case OAK_DOOR:
            case OAK_PRESSURE_PLATE:
            case OAK_SAPLING:
            case OAK_SIGN:
            case OAK_TRAPDOOR:
            case OAK_WALL_SIGN:
            case ORANGE_BANNER:
            case ORANGE_BED:
            case ORANGE_CARPET:
            case ORANGE_CONCRETE_POWDER:
            case ORANGE_TULIP:
            case ORANGE_WALL_BANNER:
            case OXEYE_DAISY:
            case PEONY:
            case PINK_BANNER:
            case PINK_BED:
            case PINK_CARPET:
            case PINK_CONCRETE_POWDER:
            case PINK_TULIP:
            case PINK_WALL_BANNER:
            case PISTON_HEAD:
            case PLAYER_HEAD:
            case PLAYER_WALL_HEAD:
            case POPPY:
            case POTATOES:
            case POWERED_RAIL:
            case PUMPKIN_STEM:
            case PURPLE_BANNER:
            case PURPLE_BED:
            case PURPLE_CARPET:
            case PURPLE_CONCRETE_POWDER:
            case PURPLE_WALL_BANNER:
            case RAIL:
            case RED_BANNER:
            case RED_BED:
            case RED_CONCRETE_POWDER:
            case RED_MUSHROOM:
            case RED_SAND:
            case RED_TULIP:
            case RED_WALL_BANNER:
            case REDSTONE_TORCH:
            case REDSTONE_WALL_TORCH:
            case REDSTONE_WIRE:
            case REPEATER:
            case ROSE_BUSH:
            case SAND:
            case SCAFFOLDING:
            case SEA_LANTERN:
            case SEA_PICKLE:
            case SEAGRASS:
            case SKELETON_SKULL:
            case SKELETON_WALL_SKULL:
            case SNOW:
            case SPRUCE_BUTTON:
            case SPRUCE_DOOR:
            case SPRUCE_PRESSURE_PLATE:
            case SPRUCE_SAPLING:
            case SPRUCE_SIGN:
            case SPRUCE_TRAPDOOR:
            case SPRUCE_WALL_SIGN:
            case STONE_BUTTON:
            case STONE_PRESSURE_PLATE:
            case STRUCTURE_BLOCK:
            case STRUCTURE_VOID:
            case SUGAR_CANE:
            case SUNFLOWER:
            case SWEET_BERRY_BUSH:
            case TALL_GRASS:
            case TALL_SEAGRASS:
            case TORCH:
            case TRIPWIRE:
            case TRIPWIRE_HOOK:
            case TUBE_CORAL:
            case TUBE_CORAL_BLOCK:
            case TUBE_CORAL_FAN:
            case TUBE_CORAL_WALL_FAN:
            case TURTLE_EGG:
            case VINE:
            case WALL_TORCH:
            case WHEAT:
            case WHITE_BANNER:
            case WHITE_BED:
            case WHITE_CARPET:
            case WHITE_CONCRETE_POWDER:
            case WHITE_TULIP:
            case WHITE_WALL_BANNER:
            case WITHER_ROSE:
            case WITHER_SKELETON_SKULL:
            case WITHER_SKELETON_WALL_SKULL:
            case YELLOW_BANNER:
            case YELLOW_BED:
            case YELLOW_CARPET:
            case YELLOW_CONCRETE_POWDER:
            case YELLOW_WALL_BANNER:
            case ZOMBIE_HEAD:
            case ZOMBIE_WALL_HEAD:
                return MaterialStatus.GREYLISTED;


            //<editor-fold defaultstate="collapsed" desc="isBlacklisted">
            case AIR:
            case BARREL:
            case BLAST_FURNACE:
            case BREWING_STAND:
            case CARTOGRAPHY_TABLE:
            case CAULDRON:
            case CAVE_AIR:
            case CHAIN_COMMAND_BLOCK:
            case CHEST:
            case COMMAND_BLOCK:
            case COMPOSTER:
            case DISPENSER:
            case DROPPER:
            case END_GATEWAY:
            case FIRE:
            case FLETCHING_TABLE:
            case FURNACE:
            case GRINDSTONE:
            case HOPPER:
            case JIGSAW:
            case LAVA:
            case LECTERN:
            case LIME_SHULKER_BOX:
            case LOOM:
            case MOVING_PISTON:
            case REPEATING_COMMAND_BLOCK:
            case SHULKER_BOX:
            case SMITHING_TABLE:
            case SMOKER:
            case STONECUTTER:
            case TRAPPED_CHEST:
            case VOID_AIR:
            case WATER:
                return MaterialStatus.BLACKLISTED;
            default:
                return MaterialStatus.UNMAPPED;
        }
    }

    /**
     * Gets the material of a block at an {@link IPLocation}.
     *
     * @param location
     *     The location.
     * @return The material of the block at the location.
     */
    private static Material getMaterial(IPLocation location)
    {
        return SpigotAdapter.getBukkitLocation(location).getBlock().getType();
    }

    /**
     * See {@link #placeOnSecondPass(IPLocation)}.
     */
    public static boolean placeOnSecondPassStatic(Material mat)
    {
        return GREYLIST.contains(mat);
    }

    /**
     * See {@link #isAirOrLiquid(IPLocation)}.
     */
    public static boolean isAirOrLiquidStatic(Block block)
    {
        // Empty means it's air.
        return block.isLiquid() || block.isEmpty();
    }

    /**
     * See {@link #isAllowedBlock(IPLocation)}.
     */
    public static boolean isAllowedBlockStatic(Material mat)
    {
        return WHITELIST.contains(mat);
    }

    /**
     * See {@link #placeOnSecondPass(IPLocation)}.
     */
    public static boolean placeOnSecondPassStatic(IPLocation location)
    {
        return placeOnSecondPassStatic(getMaterial(location));
    }

    /**
     * See {@link #isAirOrLiquid(IPLocation)}.
     */
    public static boolean isAirOrLiquidStatic(IPLocation location)
    {
        final Block block = SpigotAdapter.getBukkitLocation(location).getBlock();
        // Empty means it's air.
        return isAirOrLiquidStatic(block);
    }

    /**
     * See {@link #isAllowedBlock(IPLocation)}.
     */
    public static boolean isAllowedBlockStatic(IPLocation location)
    {
        return isAllowedBlockStatic(getMaterial(location));
    }

    @Override
    public boolean placeOnSecondPass(IPLocation location)
    {
        return placeOnSecondPassStatic(location);
    }

    @Override
    public boolean isAirOrLiquid(IPLocation location)
    {
        return isAirOrLiquidStatic(location);
    }

    @Override
    public boolean isAllowedBlock(IPLocation location)
    {
        return isAllowedBlockStatic(location);
    }
}
